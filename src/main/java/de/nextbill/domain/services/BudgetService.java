/*
 * NextBill server application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.domain.services;

import de.nextbill.domain.dtos.BudgetDTO;
import de.nextbill.domain.dtos.InvoiceCategoryDTO;
import de.nextbill.domain.dtos.PaymentPersonDTO;
import de.nextbill.domain.dtos.SearchDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.BudgetRepetitionType;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.InvoiceCostDistributionItem;
import de.nextbill.domain.repositories.*;
import de.nextbill.domain.utils.BeanMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    @Autowired
    private BudgetCategoryRepository budgetCategoryRepository;

    @Autowired
    private BudgetRecipientRepository budgetRecipientRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private PaymentPersonService paymentPersonService;

    @Autowired
    private InvoiceCategoryService invoiceCategoryService;

    @Autowired
    private InvoiceCategoryRepository invoiceCategoryRepository;

    @Autowired
    private SearchService searchService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private CostDistributionHelper costDistributionHelper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    public BudgetDTO mapToDTO(Budget budget){

        BeanMapper beanMapper = new BeanMapper();
        BudgetDTO budgetDTO = beanMapper.map(budget, BudgetDTO.class);

        List<BudgetRecipient> budgetRecipients = budgetRecipientRepository.findAllByBudget(budget);
        List<PaymentPersonDTO> paymentPersonDTOS = new ArrayList<>();
        for (BudgetRecipient budgetRecipient : budgetRecipients) {
            PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(budgetRecipient.getPayerId() , budgetRecipient.getPaymentPersonTypeEnum());
            paymentPersonDTOS.add(paymentPersonService.mapEntityToDTO(paymentPerson));
        }
        budgetDTO.setPayerDTOS(paymentPersonDTOS);

        List<BudgetCategory> budgetCategories = budgetCategoryRepository.findAllByBudget(budget);
        List<InvoiceCategoryDTO> invoiceCategoryDTOS = new ArrayList<>();
        for (BudgetCategory budgetCategory : budgetCategories) {
            invoiceCategoryDTOS.add(invoiceCategoryService.mapToDTO(budgetCategory.getInvoiceCategory()));
        }
        budgetDTO.setInvoiceCategoryDTOS(invoiceCategoryDTOS);

        return budgetDTO;
    }

    public Budget saveBudget(AppUser currentUser, BudgetDTO budgetDTO){

        BeanMapper beanMapper = new BeanMapper();
        Budget budget = beanMapper.map(budgetDTO, Budget.class);

        if (budget.getBudgetId() == null){
            budget.setBudgetId(UUID.randomUUID());
        }
        budget.setAppUser(currentUser);
        budget = budgetRepository.save(budget);

        for (BudgetRecipient budgetRecipient : budgetRecipientRepository.findAllByBudget(budget)){
            budgetRecipientRepository.deleteById(budgetRecipient.getBudgetRecipientId());
        }
        for (PaymentPersonDTO paymentPersonDTO : budgetDTO.getPayerDTOS()) {

            BudgetRecipient budgetRecipient = new BudgetRecipient();
            budgetRecipient.setBudgetRecipientId(UUID.randomUUID());

            PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(paymentPersonDTO.getId() , paymentPersonDTO.getPaymentPersonTypeEnum());
            if (paymentPerson instanceof UserContact){
                AppUser appUserContact = ((UserContact) paymentPerson).getAppUserContact();
                if (appUserContact != null){
                    budgetRecipient.setPayerId(appUserContact.getAppUserId());
                    budgetRecipient.setPaymentPersonTypeEnum(appUserContact.getPaymentPersonEnum());
                }else{
                    budgetRecipient.setPayerId(paymentPerson.getPaymentPersonId());
                    budgetRecipient.setPaymentPersonTypeEnum(paymentPerson.getPaymentPersonEnum());
                }
            }else{
                budgetRecipient.setPayerId(paymentPersonDTO.getPaymentPersonId());
                budgetRecipient.setPaymentPersonTypeEnum(paymentPersonDTO.getPaymentPersonTypeEnum());
            }

            budgetRecipient.setBudget(budget);
            budgetRecipientRepository.save(budgetRecipient);
        }

        for (BudgetCategory budgetCategory : budgetCategoryRepository.findAllByBudget(budget)){
            budgetCategoryRepository.deleteById(budgetCategory.getBudgetCategoryId());
        }
        for (InvoiceCategoryDTO invoiceCategoryDTO : budgetDTO.getInvoiceCategoryDTOS()) {

            BudgetCategory budgetCategory = new BudgetCategory();
            budgetCategory.setBudgetCategoryId(UUID.randomUUID());

            budgetCategory.setBudget(budget);

            InvoiceCategory invoiceCategory = invoiceCategoryRepository.findById(invoiceCategoryDTO.getInvoiceCategoryId()).orElse(null);
            budgetCategory.setInvoiceCategory(invoiceCategory);
            budgetCategoryRepository.save(budgetCategory);

        }

        budget.setFilterText(createFilterText(budget));
        calculateAndSaveCurrentSumForBudget(budget);
        budget = budgetRepository.save(budget);

        return budget;
    }

    public void deleteBudget(Budget budget){

        for (BudgetRecipient budgetRecipient : budgetRecipientRepository.findAllByBudget(budget)){
            budgetRecipientRepository.deleteById(budgetRecipient.getBudgetRecipientId());
        }

        for (BudgetCategory budgetCategory : budgetCategoryRepository.findAllByBudget(budget)){
            budgetCategoryRepository.deleteById(budgetCategory.getBudgetCategoryId());
        }

        budgetRepository.delete(budget);
    }

    public String createFilterText(Budget budget){
        List<String> filters = new ArrayList<>();

        if (budget.getSpecialType() != null && budget.getSpecialType()){
            filters.add("Nur Sonderfälle");
        }

        List<String> filterPersons = new ArrayList<>();
        for (BudgetRecipient budgetRecipient : budgetRecipientRepository.findAllByBudget(budget)){
            PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(budgetRecipient.getPayerId() , budgetRecipient.getPaymentPersonTypeEnum());
            filterPersons.add(paymentPerson.getPaymentPersonName());
        }
        if (!filterPersons.isEmpty()){
            filters.add("Kostenträger: " + StringUtils.join(filterPersons, ", "));
        }

        List<String> filterCategories = new ArrayList<>();
        for (BudgetCategory budgetCategory : budgetCategoryRepository.findAllByBudget(budget)){
            filterCategories.add(budgetCategory.getInvoiceCategory().getInvoiceCategoryName());
        }
        if (!filterCategories.isEmpty()){
            filters.add("Kategorien: " + StringUtils.join(filterCategories, ", "));
        }

        if (budget.getRemarks() != null && !budget.getRemarks().equals("")){
            filters.add("Bemerkung: "+ budget.getRemarks());
        }

        if (budget.getPaymentTypeEnum() != null){
            filters.add("Zahlungsart: "+ budget.getPaymentTypeEnum().getDisplayName());
        }

        return "Filter: " + StringUtils.join(filters, "; ");
    }

    public void calculateAndSaveCurrentSumForBudget(Budget budget){

        Date startDate = null;

        Date endDate = new Date();
        if (BudgetRepetitionType.MONTH.equals(budget.getBudgetRepetitionType())){
            Date nowDate = new Date();
            LocalDate startLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDateTmp = startLocalDateTmp.withDayOfMonth(1);
            LocalDateTime startLocalDateTimeTmp = startLocalDateTmp.atStartOfDay();
            startDate = Date.from(startLocalDateTimeTmp.atZone(ZoneId.systemDefault()).toInstant());
        }else if (BudgetRepetitionType.WEEK.equals(budget.getBudgetRepetitionType())){
            Date nowDate = new Date();
            LocalDate startLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDateTmp = startLocalDateTmp.minusDays(startLocalDateTmp.getDayOfWeek().getValue() - 1);
            LocalDateTime startLocalDateTimeTmp = startLocalDateTmp.atStartOfDay();
            startDate = Date.from(startLocalDateTimeTmp.atZone(ZoneId.systemDefault()).toInstant());
        }

        Set<String> userIds = new HashSet<>();
        for (BudgetRecipient budgetRecipient : budgetRecipientRepository.findAllByBudget(budget)) {
            userIds.add(budgetRecipient.getPayerId().toString());
            PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(budgetRecipient.getPayerId() , budgetRecipient.getPaymentPersonTypeEnum());
            if (paymentPerson instanceof UserContact){
                AppUser appUserContact = ((UserContact) paymentPerson).getAppUserContact();
                if (appUserContact != null){
                    userIds.add(appUserContact.getAppUserId().toString());
                    userIds.addAll(userContactRepository.findAllByAppUserContactAndBasicStatusEnum(appUserContact, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).map(UUID::toString).collect(Collectors.toList()));
                }else{
                    userIds.add(paymentPerson.getPaymentPersonId().toString());
                }
            }else if (paymentPerson instanceof AppUser) {
                userIds.addAll(userContactRepository.findAllByAppUserContactAndBasicStatusEnum((AppUser) paymentPerson, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).map(UUID::toString).collect(Collectors.toList()));
            }
        }

        List<UUID> budgetCategoryIds = budgetCategoryRepository.findAllByBudget(budget).stream().map(BudgetCategory::getInvoiceCategory).map(InvoiceCategory::getInvoiceCategoryId).collect(Collectors.toList());

        SearchDTO searchDTO = new SearchDTO();
        searchDTO.setCostPayers(userIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        searchDTO.setInvoiceCategoryDTOs(budgetCategoryIds);
        if (budget.getSpecialType() != null && budget.getSpecialType()){
            searchDTO.setSpecialType(budget.getSpecialType());
        }
        searchDTO.setRemarks(budget.getRemarks());
        searchDTO.setPaymentTypeEnum(budget.getPaymentTypeEnum());
        searchDTO.setStartDate(startDate);
        searchDTO.setEndDate(endDate);

        AppUser appUser = budget.getAppUser();
        String currentUserId = appUser.getAppUserId().toString();

        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(appUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());
        List<UUID> allReadyListInvoiceIds = invoiceRepository.findAllIdsForReadyList(currentUserId, userContactIds);

        List<InvoiceCostDistributionItem> invoiceCostDistributionItems = searchService.search(false, searchDTO, currentUserId, allReadyListInvoiceIds);

        Map<UUID, List<UUID>> userContactMap = new HashMap<>();
        BigDecimal valueSum = new BigDecimal(0);
        for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
            PaymentPerson paymentPerson = null;
            if (invoiceCostDistributionItem.getCostDistributionItem() != null){
                paymentPerson = paymentPersonService.findPaymentPerson(invoiceCostDistributionItem.getCostDistributionItem().getPayerId(), invoiceCostDistributionItem.getCostDistributionItem().getPayerTypeEnum());

                BigDecimal sum = costDistributionHelper.invoiceCostForPaymentPerson(invoiceCostDistributionItem.getInvoice(), paymentPerson, null, userContactMap);
                valueSum = valueSum.add(sum);
            }
        }

        budget.setCurrentSum(valueSum.multiply(new BigDecimal(-1)));
        budgetRepository.save(budget);

//        if (budget.getCurrentSum().compareTo(budget.getSum()) >= 0){
//            BasicDataDTO basicDataDTO = messagingService.createBudgetMessage(budget.getAppUser(), budget);
//            MessageDTO messageDTO = messagingService.createMessageDTOFromJson(basicDataDTO.getValue());
//            firebaseService.sendTextMessage(budget.getAppUser(), FirebaseMessageType.MESSAGE_BUDGET, messageDTO.getSubject(), messageDTO.getMessage());
//        }

    }

    @Async
    public void updateBudgetsIfNecessary(Invoice invoice) {

        if (invoice == null){
            return;
        }

        Set<String> appUsers = new HashSet<>();

        List<PaymentPerson> paymentPeople = new ArrayList<>();

        if (invoice.getPaymentRecipientId() != null) {
            PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(invoice.getPaymentRecipientId() , invoice.getPaymentRecipientTypeEnum());
            if (paymentPerson != null){
                paymentPeople.add(paymentPerson);
            }
        }

        if (invoice.getPayerId() != null) {
            PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(invoice.getPayerId() , invoice.getPayerTypeEnum());
            if (paymentPerson != null){
                paymentPeople.add(paymentPerson);
            }
        }

        AppUser createdUser = appUserRepository.findOneByAppUserId(UUID.fromString(invoice.getCreatedBy()));
        paymentPeople.add(createdUser);

        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(invoice);
        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(costDistributionItem.getPayerId() , costDistributionItem.getPayerTypeEnum());
            if (paymentPerson != null){
                paymentPeople.add(paymentPerson);
            }
        }

        for (PaymentPerson paymentPerson : paymentPeople) {
            if (paymentPerson instanceof AppUser){
                appUsers.add(paymentPerson.getPaymentPersonId().toString());
            }else if (paymentPerson instanceof UserContact) {
                UserContact userContact = (UserContact) paymentPerson;
                AppUser createdAppUserContact = userContact.getAppUser();
                appUsers.add(createdAppUserContact.getAppUserId().toString());

                AppUser appUserContact = userContact.getAppUserContact();
                if (appUserContact != null){
                    appUsers.add(appUserContact.getAppUserId().toString());
                }
            }
        }

        for (String appUserId : appUsers) {
            AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(appUserId));

            List<Budget> budgets = findAllBudgetsToUpdate(appUser, invoice, costDistributionItems);

            for (Budget budget : budgets) {
                calculateAndSaveCurrentSumForBudget(budget);
            }
        }
    }

    private List<Budget> findAllBudgetsToUpdate(AppUser currentUser, Invoice invoice, List<CostDistributionItem> costDistributionItems) {

        List<Budget> resultBudgets = new ArrayList<>();

        List<Budget> budgets = budgetRepository.findAllByAppUser(currentUser);

        for (Budget budget : budgets) {

            boolean isValid = true;

            if (budget.getSpecialType() != null && budget.getSpecialType()){
                if (invoice.getSpecialType() == null || !invoice.getSpecialType()){
                    isValid = false;
                }
            }

            if (budget.getRemarks() != null && !budget.getRemarks().equals("")){
                if (invoice.getRemarks() == null || !invoice.getRemarks().contains(budget.getRemarks())){
                    isValid = false;
                }
            }

            Set<String> costPayerIds = new HashSet<>();
            for (BudgetRecipient budgetRecipient : budgetRecipientRepository.findAllByBudget(budget)) {
                costPayerIds.add(budgetRecipient.getPayerId().toString());
                PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(budgetRecipient.getPayerId() , budgetRecipient.getPaymentPersonTypeEnum());
                if (paymentPerson instanceof UserContact){
                    AppUser appUserContact = ((UserContact) paymentPerson).getAppUserContact();
                    if (appUserContact != null){
                        costPayerIds.add(appUserContact.getAppUserId().toString());
                        costPayerIds.addAll(userContactRepository.findAllByAppUserContactAndBasicStatusEnum(appUserContact, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).map(UUID::toString).collect(Collectors.toList()));
                    }else{
                        costPayerIds.add(paymentPerson.getPaymentPersonId().toString());
                    }
                }else if (paymentPerson instanceof AppUser) {
                    costPayerIds.addAll(userContactRepository.findAllByAppUserContactAndBasicStatusEnum((AppUser) paymentPerson, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).map(UUID::toString).collect(Collectors.toList()));
                }
            }
            if (!costPayerIds.isEmpty()){
                boolean hasCostPayer = false;

                for (CostDistributionItem costDistributionItem : costDistributionItems) {
                    if (costPayerIds.contains(costDistributionItem.getCostPayerId().toString())){
                        hasCostPayer = true;
                        break;
                    }

                    PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(costDistributionItem.getPayerId() , costDistributionItem.getPayerTypeEnum());
                    if (paymentPerson instanceof UserContact){
                        AppUser appUserContact = ((UserContact) paymentPerson).getAppUserContact();
                        if (appUserContact != null && costPayerIds.contains(appUserContact.getAppUserId().toString())){
                            hasCostPayer = true;
                            break;
                        }
                    }

                }

                if (!hasCostPayer){
                    isValid = false;
                }
            }

            List<UUID> categoryIds = budgetCategoryRepository.findAllByBudget(budget).stream().map(BudgetCategory::getInvoiceCategory).map(InvoiceCategory::getInvoiceCategoryId).collect(Collectors.toList());
            if (!categoryIds.isEmpty()){
                if (invoice.getInvoiceCategory() == null || !categoryIds.contains(invoice.getInvoiceCategory().getInvoiceCategoryId())){
                    isValid = false;
                }
            }

            if (budget.getPaymentTypeEnum() != null){
                if (invoice.getPaymentTypeEnum() == null || !invoice.getPaymentTypeEnum().equals(budget.getPaymentTypeEnum())){
                    isValid = false;
                }
            }

            if (isValid){
                resultBudgets.add(budget);
            }
        }

        return resultBudgets;
    }

}
