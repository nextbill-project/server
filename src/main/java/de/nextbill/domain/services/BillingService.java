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

import de.nextbill.commons.mailmanager.model.Mail;
import de.nextbill.commons.mailmanager.model.MailRecipient;
import de.nextbill.commons.mailmanager.service.MailService;
import de.nextbill.domain.comparators.BillingListProcessItemComparator;
import de.nextbill.domain.comparators.BillingListProcessItemDTOComparator;
import de.nextbill.domain.comparators.GroupedBillingListItemsDtoComparator;
import de.nextbill.domain.dtos.*;
import de.nextbill.domain.enums.*;
import de.nextbill.domain.interfaces.PaymentItem;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.BillingListItem;
import de.nextbill.domain.pojos.BillingListProcessItem;
import de.nextbill.domain.pojos.PaymentPersonPojo;
import de.nextbill.domain.repositories.*;
import de.nextbill.domain.utils.BeanMapper;
import de.nextbill.domain.utils.views.MappingView;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import java.io.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingService {

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PaymentPersonService paymentPersonService;

    @Autowired
    private PaymentItemService paymentItemService;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private AutoFillHelperService autoFillHelperService;

    @Autowired
    private MailService mailService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private BillingHelperService billingHelperService;

    @Autowired
    private InvoiceHelperService invoiceHelperService;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private PathService pathService;

    @Autowired
    private SettingsService settingsService;

    public void markBillingItemsAsPaid(Billing billing) {

        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByBillings(billing);

        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            BigDecimal costPaid = new BigDecimal(0);
            if (costDistributionItem.getMoneyValue() != null){
                costPaid = costDistributionItem.getMoneyValue();
            }
            costDistributionItem.setCostPaid(costPaid);
            costDistributionItemRepository.save(costDistributionItem);

            Invoice invoice = costDistributionItem.getInvoice();
            if (invoice != null){
                invoiceRepository.save(invoice);
            }
        }

        List<Invoice> invoices = invoiceRepository.findByBillings(billing);

        for (Invoice invoice : invoices) {
            BigDecimal costPaid = new BigDecimal(0);
            if (invoice.getSumOfInvoice() != null){
                costPaid = invoice.getSumOfInvoice();
            }
            invoice.setCostPaid(costPaid);
            invoiceRepository.save(invoice);
        }
    }

    public Billing createBilling(AppUser loggedInUser, BillingConfigDTO billingConfigDTO) throws IOException, MessagingException {

        List<UUID> userContactIds = userContactRepository.findAllByAppUserContact(loggedInUser).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        AppUser currentUser = null;
        if (billingConfigDTO.getUserPayer() == null){
            currentUser = loggedInUser;
        }else{
            currentUser = appUserRepository.findById(UUID.fromString(billingConfigDTO.getUserPayer())).orElse(null);
        }

        UserContact costPayer = userContactRepository.findById(UUID.fromString(billingConfigDTO.getUserSelection())).orElse(null);
        if (costPayer == null){
            AppUser costPayerUser = appUserRepository.findById(UUID.fromString(billingConfigDTO.getUserSelection())).orElse(null);
            currentUser = costPayerUser;
            costPayer = userContactRepository.findOneByAppUserAndAppUserContact(costPayerUser, loggedInUser);
        }else{
            if (userContactIds.contains(costPayer.getPaymentPersonId())){
                currentUser = costPayer.getAppUser();
            }
        }

        if (billingConfigDTO.getCostPayerMail() != null){
            costPayer.setEmail(billingConfigDTO.getCostPayerMail());
            userContactRepository.save(costPayer);
        }

        BillingListItem billingListItem = sumToBeDebtAndChecked(currentUser, costPayer, billingConfigDTO.isUsePaidInvoices(), billingConfigDTO.getStartDate(), billingConfigDTO.getEndDate());
        billingListItem.setInvoicePayer(PaymentPersonPojo.fromIPaymentPerson(currentUser));
        billingListItem.setCostPayer(PaymentPersonPojo.fromIPaymentPerson(costPayer));

        BigDecimal sumTotal = billingListItem.getSumToBePaid();
        sumTotal = sumTotal.multiply(new BigDecimal(-1));

        Billing billing = new Billing();
        billing.setBillingId(UUID.randomUUID());
        billing.setCostPayer(costPayer);
        billing.setSumPaid(new BigDecimal(0));
        billing = billingRepository.save(billing);
        billing.setCreatedBy(currentUser.getAppUserId().toString());
        billing = billingRepository.save(billing);

        addPaymentItemsToBilling(billingListItem, billing);

        if (sumTotal.compareTo(new BigDecimal(0)) == -1){
            billing.setSumToPay(sumTotal.multiply(new BigDecimal(-1)));
            billing.setIsNormalPayment(true);
        }else{
            billing.setSumToPay(sumTotal);
            billing.setIsNormalPayment(false);
        }

        createBillingMailsAsync(billingConfigDTO, billing, billingListItem);
        messagingService.sendBillingMessagesAsync(billing);

        if (billingConfigDTO.isMarkAsPaid()) {
            billing.setBillingStatusEnum(BillingStatusEnum.FINISHED);
            billing.setSumPaid(billing.getSumToPay());
            billingRepository.save(billing);

            markBillingItemsAsPaid(billing);
        }else if (costPayer.getAppUserContact() == null && costPayer.getEmail() != null){
            billing.setBillingStatusEnum(BillingStatusEnum.TO_PAY);
            billingRepository.save(billing);
        }else{
            billing.setBillingStatusEnum(BillingStatusEnum.TO_PAY);
            billingRepository.save(billing);
        }

        return billing;
    }

    public Invoice createCompensation(AppUser loggedInUser, Billing billing){

        BigDecimal sumPaid = billing.getSumPaid();
        BigDecimal currentSumToPay = sumToPay(billing);

        BigDecimal difference = sumPaid.subtract(currentSumToPay);

        AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());

        boolean normalDirection = true;
        if (!createdBy.getAppUserId().equals(loggedInUser.getAppUserId())){
            normalDirection = false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMM yyyy");

        Invoice invoice = null;
        if (difference.compareTo(new BigDecimal(0)) == 1){
            if (billing.getIsNormalPayment()){
                invoice = sendMoneyToPaymentPerson(createdBy, paymentPerson, difference, "Erstattung für die Abrechnung vom " + sdf.format(billing.getCreatedDate()) + " aufgrund zwischenzeitiger Änderung der Gesamtsumme.", normalDirection);
            }else{
                invoice = sendMoneyToPaymentPerson(paymentPerson, createdBy, difference, "Erstattung für die Abrechnung vom " + sdf.format(billing.getCreatedDate()) + " aufgrund zwischenzeitiger Änderung der Gesamtsumme.", !normalDirection);
            }
        }else if (difference.compareTo(new BigDecimal(0)) == -1){
            if (billing.getIsNormalPayment()){
                invoice = sendMoneyToPaymentPerson(paymentPerson, createdBy, difference, "Ausgleich für die Abrechnung vom " + sdf.format(billing.getCreatedDate()) + " aufgrund zwischenzeitiger Änderung der Gesamtsumme.", !normalDirection);
            }else{
                invoice =  sendMoneyToPaymentPerson(createdBy, paymentPerson, difference, "Ausgleich für die Abrechnung vom " + sdf.format(billing.getCreatedDate()) + " aufgrund zwischenzeitiger Änderung der Gesamtsumme.", normalDirection);
            }
        }

        markBillingItemsAsPaid(billing);

        BigDecimal newCurrentSumToPay = currentSumToPay;

        if (currentSumToPay.compareTo(new BigDecimal(0)) < 0){
            billing.setIsNormalPayment(!billing.getIsNormalPayment());
            newCurrentSumToPay = newCurrentSumToPay.multiply(new BigDecimal(-1));
        }

        billing.setSumPaid(newCurrentSumToPay);
        billing.setSumToPay(newCurrentSumToPay);
        billing.setBillingStatusEnum(BillingStatusEnum.FINISHED);

        billingRepository.save(billing);

        return invoice;
    }

    /**
     * Creates a billing list item with invoices 'open' (= must be paid)
     *
     * @param currentUser
     * @param paymentPerson
     * @param isReversePayment
     * @return a pojo with not paid invoices
     */
    private BillingListItem startCreateBillingListItem(AppUser currentUser, PaymentPerson paymentPerson, Boolean isReversePayment){

        UserContact inputUserContact;

        if (paymentPerson.getPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER)){
            AppUser appUser = (AppUser) paymentPerson;
            List<UserContact> userContactAppUsersTmp = userContactRepository.findAllByAppUserAndAppUserContactAndBasicStatusEnum(appUser, currentUser, BasicStatusEnum.OK);
            if (userContactAppUsersTmp.isEmpty()) {
                return null;
            }
            inputUserContact = userContactAppUsersTmp.get(0);
        }else{
            inputUserContact = (UserContact) paymentPerson;
        }

        if (inputUserContact == null) {
            return null;
        }

        BillingListItem sumToDebtBillingItem = sumToBeDebtAndChecked(currentUser, inputUserContact, false, null, null);
        BigDecimal sumToDebt = sumToDebtBillingItem.getSumToBePaid();
        BigDecimal sumToCheck = sumToDebtBillingItem.getSumToBeChecked();

        if (sumToDebt.compareTo(new BigDecimal(0)) != 0 || sumToCheck.compareTo(new BigDecimal(0)) != 0){
            return createBillingListItem(currentUser, inputUserContact, sumToDebtBillingItem, sumToDebt, sumToCheck, isReversePayment);
        }

        return null;
    }

    private BillingListItem createBillingListItem(AppUser appUser, UserContact tmpUserContact, BillingListItem sumToDebtBillingItem, BigDecimal sumToDebt, BigDecimal sumToCheck, boolean isReversePayment) {
        BillingListItem billingListItem = null;
        if (sumToDebt.compareTo(new BigDecimal(0)) < 0){
            billingListItem = new BillingListItem();
            billingListItem.setInvoicePayer(PaymentPersonPojo.fromIPaymentPerson(appUser));
            billingListItem.setSumToBePaid(sumToDebt.multiply(new BigDecimal(-1)));
            billingListItem.setCostPayer(PaymentPersonPojo.fromIPaymentPerson(tmpUserContact));
            billingListItem.setInvoiceCategoriesOfCostPayer(sumToDebtBillingItem.getInvoiceCategoriesOfInvoicePayer());
            billingListItem.setInvoiceCategoriesOfInvoicePayer(sumToDebtBillingItem.getInvoiceCategoriesOfCostPayer());
            BigDecimal costsFromCostPayerResult = sumToDebtBillingItem.getCostsFromCostPayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromCostPayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromCostPayer();
            billingListItem.setCostsFromInvoicePayer(costsFromCostPayerResult);
            BigDecimal costsFromInvoicePayerResult = sumToDebtBillingItem.getCostsFromInvoicePayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromInvoicePayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromInvoicePayer();
            billingListItem.setCostsFromCostPayer(costsFromInvoicePayerResult);
        }else{
            billingListItem = new BillingListItem();
            billingListItem.setInvoicePayer(PaymentPersonPojo.fromIPaymentPerson(tmpUserContact));
            billingListItem.setSumToBePaid(sumToDebt);
            billingListItem.setCostPayer(PaymentPersonPojo.fromIPaymentPerson(appUser));
            billingListItem.setInvoiceCategoriesOfCostPayer(sumToDebtBillingItem.getInvoiceCategoriesOfCostPayer());
            billingListItem.setInvoiceCategoriesOfInvoicePayer(sumToDebtBillingItem.getInvoiceCategoriesOfInvoicePayer());
            BigDecimal costsFromInvoicePayerResult = sumToDebtBillingItem.getCostsFromInvoicePayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromInvoicePayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromInvoicePayer();
            billingListItem.setCostsFromInvoicePayer(costsFromInvoicePayerResult);
            BigDecimal costsFromCostPayerResult = sumToDebtBillingItem.getCostsFromCostPayer().compareTo(new BigDecimal(0)) < 0 ? sumToDebtBillingItem.getCostsFromCostPayer().multiply(new BigDecimal(-1)) : sumToDebtBillingItem.getCostsFromCostPayer();
            billingListItem.setCostsFromCostPayer(costsFromCostPayerResult);
        }

        if (sumToCheck.compareTo(new BigDecimal(0)) != 0){
            billingListItem.setSumToBeChecked(sumToCheck);
        }

        if (isReversePayment){
            billingListItem.setCostItemsForCostPayer(sumToDebtBillingItem.getCostItemsForInvoicePayer());
            billingListItem.setCostItemsForInvoicePayer(sumToDebtBillingItem.getCostItemsForCostPayer());
        }else{
            billingListItem.setCostItemsForCostPayer(sumToDebtBillingItem.getCostItemsForCostPayer());
            billingListItem.setCostItemsForInvoicePayer(sumToDebtBillingItem.getCostItemsForInvoicePayer());
        }

        return billingListItem;
    }

    public List<GroupedBillingListItemsDTO> groupedBillingListProcessItems(AppUser billingAppUser, boolean useArchived, boolean addProcessBillingListItems){

        Map<String, GroupedBillingListItemsDTO> groupedBillingListItemsDTOMap = new HashMap<>();

        List<UserContact> appUserContacts = userContactRepository.findAllByAppUser(billingAppUser);

        for (UserContact appUserContact : appUserContacts) {
            GroupedBillingListItemsDTO groupedBillingListItemsDTO = createGroupedBillingListItemsDTO(billingAppUser, useArchived, appUserContact, false, addProcessBillingListItems);
            if (groupedBillingListItemsDTO != null){

                AppUser appUser = appUserContact.getAppUserContact();
                if (appUser != null){
                    groupedBillingListItemsDTOMap.put(appUser.getAppUserId().toString(), groupedBillingListItemsDTO);
                }else{
                    groupedBillingListItemsDTOMap.put(appUserContact.getUserContactId().toString(), groupedBillingListItemsDTO);
                }
            }
        }

        List<UserContact> appUserAsAppUserContact = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(billingAppUser, BasicStatusEnum.OK);

        for (UserContact userContact : appUserAsAppUserContact) {

            AppUser appUser = userContact.getAppUser();
            if (appUser == null){
                continue;
            }
            GroupedBillingListItemsDTO groupedBillingListItemsDTO = createGroupedBillingListItemsDTO(appUser, useArchived, userContact, true, addProcessBillingListItems);
            if (groupedBillingListItemsDTO != null){

                GroupedBillingListItemsDTO groupedBillingListItemsDTOofMap = groupedBillingListItemsDTOMap.get(appUser.getAppUserId().toString());
                if (groupedBillingListItemsDTOofMap != null){

                    if (groupedBillingListItemsDTOofMap.getBillingListItemDTO() != null) {
                        groupedBillingListItemsDTO.setBillingListItemDTO(groupedBillingListItemsDTOofMap.getBillingListItemDTO());
                    }

                    if (groupedBillingListItemsDTOofMap.getBillingListProcessItemDTOs() != null && !groupedBillingListItemsDTOofMap.getBillingListProcessItemDTOs().isEmpty()) {
                        Map<String, BillingListProcessItemDTO> billingListProcessItemDTOMap = new HashMap<>();
                        if (groupedBillingListItemsDTO.getBillingListProcessItemDTOs() != null){
                            for (BillingListProcessItemDTO billingListProcessItemDTO : groupedBillingListItemsDTO.getBillingListProcessItemDTOs()) {
                                billingListProcessItemDTOMap.put(billingListProcessItemDTO.getBillingId().toString(), billingListProcessItemDTO);
                            }
                        }
                        for (BillingListProcessItemDTO billingListProcessItemDTO : groupedBillingListItemsDTOofMap.getBillingListProcessItemDTOs()) {
                            billingListProcessItemDTOMap.put(billingListProcessItemDTO.getBillingId().toString(), billingListProcessItemDTO);
                        }

                        List<BillingListProcessItemDTO> billingListProcessItemDTOS = new ArrayList<>(billingListProcessItemDTOMap.values());

                        Collections.sort(billingListProcessItemDTOS, new BillingListProcessItemDTOComparator());

                        groupedBillingListItemsDTO.setBillingListProcessItemDTOs(billingListProcessItemDTOS);
                    }

                }

                groupedBillingListItemsDTOMap.put(appUser.getAppUserId().toString(), groupedBillingListItemsDTO);
            }
        }

        List<GroupedBillingListItemsDTO> paymentPersonPojoListGrouped = new ArrayList<>(groupedBillingListItemsDTOMap.values());

        paymentPersonPojoListGrouped = paymentPersonPojoListGrouped.stream().sorted(new GroupedBillingListItemsDtoComparator()).collect(Collectors.toList());

        return paymentPersonPojoListGrouped;
    }

    private GroupedBillingListItemsDTO createGroupedBillingListItemsDTO(AppUser billingAppUser, boolean useArchived, UserContact appUserContact, boolean isReversed, boolean addProcessBillingListItems) {
        GroupedBillingListItemsDTO groupedBillingListItemsDTO = new GroupedBillingListItemsDTO();

        boolean isUsed = false;

        if (addProcessBillingListItems) {
            List<BillingListProcessItemDTO> listBillingListProcessItemDTOs = listBillingListProcessItemDTOs(billingAppUser, appUserContact.getUserContactId(), useArchived, isReversed);
            if (!listBillingListProcessItemDTOs.isEmpty()){
                groupedBillingListItemsDTO.setBillingListProcessItemDTOs(listBillingListProcessItemDTOs);
                isUsed = true;
            }
        }

        if (!useArchived){
            BillingListItemDTO billingListItem = createBillingListItemDTO(billingAppUser, appUserContact);
            if (billingListItem != null){
                groupedBillingListItemsDTO.setBillingListItemDTO(billingListItem);
                isUsed = true;
            }
        }

        if (isUsed){
            if (isReversed) {
                groupedBillingListItemsDTO.setCostPayer(PaymentPersonPojo.fromIPaymentPerson(billingAppUser));
            }else{
                groupedBillingListItemsDTO.setCostPayer(PaymentPersonPojo.fromIPaymentPerson(appUserContact));
            }

            return groupedBillingListItemsDTO;
        }

        return null;
    }

    private BillingListItemDTO createBillingListItemDTO(AppUser billingAppUser, UserContact userContactInput){

        BillingListItem billingListItem = startCreateBillingListItem(billingAppUser, userContactInput, false);

        if (billingListItem == null){
            return null;
        }

        PaymentPersonPojo paymentPersonPojo = null;
        if ((billingListItem.getCostPayer().getPayerId().toString().equals(billingAppUser.getAppUserId().toString())) ||
                (userContactInput.getUserContactId().toString().equals(billingListItem.getCostPayer().getPayerId().toString()))){
            paymentPersonPojo = billingListItem.getInvoicePayer();
        }else{
            paymentPersonPojo = billingListItem.getCostPayer();
        }

        UUID costPayerId = paymentPersonPojo.getPayerId();

        boolean billingIsUsable = (billingListItem.getInvoicePayer().getPayerId().equals(costPayerId) || billingListItem.getCostPayer().getPayerId().equals(costPayerId));

        if(billingIsUsable){
            if ((billingListItem.getSumToBePaid() != null && billingListItem.getSumToBePaid().compareTo(new BigDecimal(0)) != 0) ||
                    (billingListItem.getSumToBeChecked() != null && billingListItem.getSumToBeChecked().compareTo(new BigDecimal(0)) != 0)){
                BillingListItemDTO billingListItemDTO = convertBillingListItemToDTO(billingListItem);

                List<InvoiceDTO> invoiceDTOS = paymentItemService.invoiceDTOsForPaymentItems(billingListItem.getCostItemsForCostPayer());
                List<InvoiceDTO> invoicesForInvoicePayerDTOS = paymentItemService.invoiceDTOsForPaymentItems(billingListItem.getCostItemsForInvoicePayer());

                if (billingAppUser.getAppUserId().equals(billingListItemDTO.getInvoicePayer().getPayerId())){
                    billingListItemDTO.setInvoicesForCostPayer(invoicesForInvoicePayerDTOS);
                    billingListItemDTO.setInvoicesForInvoicePayer(invoiceDTOS);
                }else{
                    billingListItemDTO.setInvoicesForCostPayer(invoiceDTOS);
                    billingListItemDTO.setInvoicesForInvoicePayer(invoicesForInvoicePayerDTOS);
                }

                return billingListItemDTO;
            }
        }

        return null;
    }


    /**
     * List of billings of user with its process state
     *
     * @param billingAppUser
     * @param userContactInput
     * @param useArchived
     * @param isReversed
     * @return list of billings
     */
    public List<BillingListProcessItemDTO> listBillingListProcessItemDTOs(AppUser billingAppUser, UUID userContactInput, boolean useArchived, boolean isReversed){

        List<BillingListProcessItemDTO> billingListProcessItemDTOsResults = new ArrayList<>();

        List<Billing> billings = billingHelperService.billingsForAppUser(billingAppUser, userContactInput,true, useArchived);

        List<UUID> costPayerIds = new ArrayList<>();
        List<UUID> costPayerIdsAlreadyFound = new ArrayList<>();
        List<PaymentPersonPojo> paymentPersonBeen = new ArrayList<>();

        for (Billing billing : billings) {

            PaymentPersonPojo costPayer = PaymentPersonPojo.fromIPaymentPerson(paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum()));
            if (costPayer.getPayerId().toString().equals(userContactInput.toString())){
                AppUser billingCreatedBy = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
                UserContact userContact = userContactRepository.findOneByAppUserAndAppUserContact(billingAppUser, billingCreatedBy);

                if (userContact != null){
                    costPayer =  PaymentPersonPojo.fromIPaymentPerson(userContact);
                }else{
                    UserContact userContactExternal = userContactRepository.findById(costPayer.getPayerId()).orElse(null);
                    AppUser appUser = userContactExternal.getAppUser();
                    if (appUser != null){
                        costPayer =  PaymentPersonPojo.fromIPaymentPerson(appUser);
                        if (!costPayerIdsAlreadyFound.contains(userContactExternal.getPaymentPersonId())){
                            costPayerIds.add(userContactExternal.getPaymentPersonId());
                            costPayerIdsAlreadyFound.add(userContactExternal.getPaymentPersonId());
                            costPayerIdsAlreadyFound.add(costPayer.getPaymentPersonId());
                            paymentPersonBeen.add(costPayer);
                        }
                    }
                }
            }

            if (!costPayerIdsAlreadyFound.contains(costPayer.getPaymentPersonId())){
                costPayerIds.add(costPayer.getPaymentPersonId());
                costPayerIdsAlreadyFound.add(costPayer.getPaymentPersonId());
                paymentPersonBeen.add(costPayer);
            }
        }

        for (int i = 0; i < costPayerIds.size(); i++) {
            UUID costPayerId = costPayerIds.get(i);
            List<BillingListProcessItem> billingListItemsForPayer = new ArrayList<>();

            for (Billing billing : billings) {

                if (BillingStatusEnum.ARCHIVED_DELETED.equals(billing.getBillingStatusEnum())){
                    continue;
                }

                if (useArchived){
                    if (!BillingStatusEnum.ARCHIVED.equals(billing.getBillingStatusEnum())){
                        continue;
                    }
                }else{
                    if (BillingStatusEnum.ARCHIVED.equals(billing.getBillingStatusEnum())){
                        continue;
                    }
                }

                UserContact costPayer = userContactRepository.findById(billing.getCostPayerId()).orElse(null);
                UserContact userContact = userContactRepository.findOneByAppUserAndAppUserContact(billingAppUser,costPayer.getAppUser());

                if (billing.getCostPayerId().equals(costPayerId) || (userContact != null && userContact.getUserContactId().equals(costPayerId))){
                    BeanMapper beanMapper = new BeanMapper();
                    BillingListProcessItem billingListProcessItem = beanMapper.map(billing, BillingListProcessItem.class);

                    AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
                    billingListProcessItem.setInvoicePayer(PaymentPersonPojo.fromIPaymentPerson(appUser));
                    billingListProcessItem.setCostPayer(PaymentPersonPojo.fromIPaymentPerson(paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum())));

                    BasicDataDTO basicDataDTO = null;
                    if (isReversed){
                        basicDataDTO = messagingService.createBillingMessageForAppUser(costPayer.getAppUserContact(), billing);
                    }else{
                        basicDataDTO = messagingService.createBillingMessageForAppUser(billingAppUser, billing);
                    }

                    if (basicDataDTO != null) {
                        MessageDTO messageDTO = messagingService.createMessageDTOFromJson(basicDataDTO.getValue());
                        billingListProcessItem.setMessageType(messageDTO.getMessageType());
                        billingListProcessItem.setSubject(messageDTO.getSubject());

                        billingListItemsForPayer.add(billingListProcessItem);
                    }

                }
            }


            Collections.sort(billingListItemsForPayer, new BillingListProcessItemComparator());

            if (!billingListItemsForPayer.isEmpty()){
                List<BillingListProcessItemDTO> billingListProcessItemDTOS = convertBillingListProcessItemsToDTOs(billingListItemsForPayer);
                billingListProcessItemDTOsResults.addAll(billingListProcessItemDTOS);
            }
        }

        return billingListProcessItemDTOsResults;
    }

    public BillingListItemDTO convertBillingListItemToDTO(BillingListItem billingListItem){
        BeanMapper beanMapper = new BeanMapper();
        BillingListItemDTO billingListItemDTO = beanMapper.map(billingListItem, BillingListItemDTO.class);
        return billingListItemDTO;
    }

    public List<BillingListItemDTO> convertBillingListItemsToDTOs(List<BillingListItem> billingListItems){
        List<BillingListItemDTO> billingListItemDTOs = new ArrayList<>();
        for (BillingListItem billingListItem : billingListItems) {
            billingListItemDTOs.add(convertBillingListItemToDTO(billingListItem));
        }
        return billingListItemDTOs;
    }

    public BillingListProcessItemDTO convertBillingListItemToDTO(BillingListProcessItem billingListProcessItem){
        BeanMapper beanMapper = new BeanMapper();
        BillingListProcessItemDTO billingListProcessItemDTO = beanMapper.map(billingListProcessItem, BillingListProcessItemDTO.class);
        return billingListProcessItemDTO;
    }

    public List<BillingListProcessItemDTO> convertBillingListProcessItemsToDTOs(List<BillingListProcessItem> billingListProcessItem){
        List<BillingListProcessItemDTO> billingListProcessItemDTOs = new ArrayList<>();
        for (BillingListProcessItem billingListProcessItemTmp : billingListProcessItem) {
            billingListProcessItemDTOs.add(convertBillingListItemToDTO(billingListProcessItemTmp));
        }
        return billingListProcessItemDTOs;
    }

    public BillingListItem sumToBeDebtAndChecked(AppUser currentUser, UserContact costPayer, boolean isUsePaidInvoices, Date startDate, Date endDate){

        BillingListItem billingListItem = new BillingListItem();

//        *************************************************
//        *************** SumToBe DEBT *****************
//        *************************************************

        List<PaymentItem> sumTotalItemsExpense = costDistributionItemRepository.findCostDistributionItems(MainFunctionEnum.EXPENSE, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.READY, isUsePaidInvoices);
        sumTotalItemsExpense.addAll(costDistributionItemRepository.findDirectTransactionsCostDistributionItems(MainFunctionEnum.INCOME, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.READY, isUsePaidInvoices));

        BigDecimal sumTotalNormal = new BigDecimal(0);
        BigDecimal sumTotalNormalExpense = new BigDecimal(0);
        for (PaymentItem costDistributionItem : sumTotalItemsExpense) {
            BigDecimal costPaid = (costDistributionItem.getCostPaid() != null ? costDistributionItem.getCostPaid() : new BigDecimal(0));
            sumTotalNormal = sumTotalNormal.add(costDistributionItem.getMoneyValue().subtract(costPaid));
            sumTotalNormalExpense = sumTotalNormalExpense.add(costDistributionItem.getMoneyValue().subtract(costPaid));
        }

        List<PaymentItem> sumTotalItemsIncome = costDistributionItemRepository.findCostDistributionItems(MainFunctionEnum.INCOME, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.READY, isUsePaidInvoices);
        sumTotalItemsIncome.addAll(costDistributionItemRepository.findDirectTransactionsCostDistributionItems(MainFunctionEnum.EXPENSE, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.READY, isUsePaidInvoices));

        BigDecimal sumTotalNormalIncome = new BigDecimal(0);
        for (PaymentItem costDistributionItem : sumTotalItemsIncome) {
            BigDecimal costPaid = (costDistributionItem.getCostPaid() != null ? costDistributionItem.getCostPaid() : new BigDecimal(0));
            sumTotalNormal = sumTotalNormal.subtract(costDistributionItem.getMoneyValue().subtract(costPaid));
            sumTotalNormalIncome = sumTotalNormalIncome.add(costDistributionItem.getMoneyValue().subtract(costPaid));
        }

        BigDecimal sumTotalReverse = new BigDecimal(0);
        BigDecimal sumTotalReverseExpense = new BigDecimal(0);

        List<PaymentItem> sumTotalReverseItemsExpense = new ArrayList<>();
        List<PaymentItem> sumTotalReverseItemsIncome = new ArrayList<>();

        BigDecimal sumTotalReverseIncome = new BigDecimal(0);

        AppUser contactAsAppUser = costPayer.getAppUserContact();
        if (contactAsAppUser != null){
            UserContact newCostPayerContact = userContactRepository.findOneByAppUserAndAppUserContact(contactAsAppUser, currentUser);

            if (newCostPayerContact != null){

                sumTotalReverseItemsExpense = costDistributionItemRepository.findCostDistributionItems(MainFunctionEnum.EXPENSE, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.READY, isUsePaidInvoices);
                sumTotalReverseItemsExpense.addAll(costDistributionItemRepository.findDirectTransactionsCostDistributionItems(MainFunctionEnum.INCOME, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.READY, isUsePaidInvoices));

                for (PaymentItem costDistributionItem : sumTotalReverseItemsExpense) {
                    BigDecimal costPaid = (costDistributionItem.getCostPaid() != null ? costDistributionItem.getCostPaid() : new BigDecimal(0));
                    sumTotalReverse = sumTotalReverse.add(costDistributionItem.getMoneyValue().subtract(costPaid));
                    sumTotalReverseExpense = sumTotalReverseExpense.add(costDistributionItem.getMoneyValue().subtract(costPaid));
                }

                sumTotalReverseItemsIncome = costDistributionItemRepository.findCostDistributionItems(MainFunctionEnum.INCOME, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.READY, isUsePaidInvoices);
                sumTotalReverseItemsIncome.addAll(costDistributionItemRepository.findDirectTransactionsCostDistributionItems(MainFunctionEnum.EXPENSE, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.READY, isUsePaidInvoices));

                for (PaymentItem costDistributionItem : sumTotalReverseItemsIncome) {
                    BigDecimal costPaid = (costDistributionItem.getCostPaid() != null ? costDistributionItem.getCostPaid() : new BigDecimal(0));
                    sumTotalReverse = sumTotalReverse.subtract(costDistributionItem.getMoneyValue().subtract(costPaid));
                    sumTotalReverseIncome = sumTotalReverseIncome.add(costDistributionItem.getMoneyValue().subtract(costPaid));
                }
            }

        }

        BigDecimal sumTotal = sumTotalNormal.subtract(sumTotalReverse);

//        *************************************************
//        *************** SumToBe CHECKED *****************
//        *************************************************

        BigDecimal sumCheckedTotalNormal = new BigDecimal(0);

        List<PaymentItem> sumCheckedTotalItemsExpense = costDistributionItemRepository.findCostDistributionItems(MainFunctionEnum.EXPENSE, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.CHECK, isUsePaidInvoices);
        sumCheckedTotalItemsExpense.addAll(costDistributionItemRepository.findDirectTransactionsCostDistributionItems(MainFunctionEnum.INCOME, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.CHECK, isUsePaidInvoices));

        for (PaymentItem costDistributionItem : sumCheckedTotalItemsExpense) {
            BigDecimal costPaid = (costDistributionItem.getCostPaid() != null ? costDistributionItem.getCostPaid() : new BigDecimal(0));
            sumCheckedTotalNormal = sumCheckedTotalNormal.add(costDistributionItem.getMoneyValue().subtract(costPaid));
        }

        List<PaymentItem> sumCheckedTotalItemsIncome = costDistributionItemRepository.findCostDistributionItems(MainFunctionEnum.INCOME, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.CHECK, isUsePaidInvoices);
        sumCheckedTotalItemsIncome.addAll(costDistributionItemRepository.findDirectTransactionsCostDistributionItems(MainFunctionEnum.EXPENSE, currentUser.getPaymentPersonId(), currentUser.getPaymentPersonEnum(), costPayer.getPaymentPersonId(), costPayer.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.CHECK, isUsePaidInvoices));

        for (PaymentItem costDistributionItem : sumCheckedTotalItemsIncome) {
            BigDecimal costPaid = (costDistributionItem.getCostPaid() != null ? costDistributionItem.getCostPaid() : new BigDecimal(0));
            sumCheckedTotalNormal = sumCheckedTotalNormal.subtract(costDistributionItem.getMoneyValue().subtract(costPaid));
        }

        BigDecimal sumCheckedTotalReverse = new BigDecimal(0);

//        List<IPaymentItem> sumCheckedTotalReverseItemsExpense = new ArrayList<>();
//        List<IPaymentItem> sumCheckedTotalReverseItemsIncome = new ArrayList<>();
//
//        if (contactAsAppUser != null){
//            UserContact newCostPayerContact = userContactRepository.findOneByAppUserAndAppUserContact(contactAsAppUser, currentUser);
//
//            if (newCostPayerContact != null){
//
//                sumCheckedTotalReverseItemsExpense = costDistributionItemRepository.findCostDistributionItems(MainFunctionEnum.EXPENSE, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.CHECK, isUsePaidInvoices);
//                sumCheckedTotalReverseItemsExpense.addAll(costDistributionItemRepository.findDirectTransactionsCostDistributionItems(MainFunctionEnum.INCOME, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.CHECK, isUsePaidInvoices));
//
//                for (IPaymentItem costDistributionItem : sumCheckedTotalReverseItemsExpense) {
//                    BigDecimal costPaid = (costDistributionItem.getCostPaid() != null ? costDistributionItem.getCostPaid() : new BigDecimal(0));
//                    sumCheckedTotalReverse = sumCheckedTotalReverse.add(costDistributionItem.getMoneyValue().subtract(costPaid));
//                }
//
//                sumCheckedTotalReverseItemsIncome = costDistributionItemRepository.findCostDistributionItems(MainFunctionEnum.INCOME, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.CHECK, isUsePaidInvoices);
//                sumCheckedTotalReverseItemsIncome.addAll(costDistributionItemRepository.findDirectTransactionsCostDistributionItems(MainFunctionEnum.EXPENSE, contactAsAppUser.getPaymentPersonId(), contactAsAppUser.getPaymentPersonEnum(), newCostPayerContact.getPaymentPersonId(), newCostPayerContact.getPaymentPersonEnum(), startDate, endDate, InvoiceStatusEnum.READY, CorrectionStatus.CHECK, isUsePaidInvoices));
//
//                for (IPaymentItem costDistributionItem : sumCheckedTotalReverseItemsIncome) {
//                    BigDecimal costPaid = (costDistributionItem.getCostPaid() != null ? costDistributionItem.getCostPaid() : new BigDecimal(0));
//                    sumCheckedTotalReverse = sumCheckedTotalReverse.subtract(costDistributionItem.getMoneyValue().subtract(costPaid));
//                }
//            }
//
//        }

        BigDecimal sumCheckedTotal = sumCheckedTotalNormal.subtract(sumCheckedTotalReverse);


//        ************************************ RESULTS **************************************
        billingListItem.setSumToBePaid(sumTotal);
        billingListItem.setSumToBeChecked(sumCheckedTotal);
        billingListItem.setCostsFromInvoicePayer(sumTotalNormalIncome.add(sumTotalReverseExpense));
        billingListItem.setCostsFromCostPayer(sumTotalNormalExpense.add(sumTotalReverseIncome));

        billingListItem.setSumTotalItemsIncome(sumTotalItemsIncome);
        billingListItem.setSumTotalReverseItemsExpense(sumTotalReverseItemsExpense);

        List<InvoiceCategory> invoiceCategoriesInvoicePayer = categoriesForPaymentItems(billingListItem.getCostItemsForInvoicePayer());

        billingListItem.setSumTotalItemsExpense(sumTotalItemsExpense);
        billingListItem.setSumTotalReverseItemsIncome(sumTotalReverseItemsIncome);

        List<InvoiceCategory> invoiceCategoriesCostPayer = categoriesForPaymentItems(billingListItem.getCostItemsForCostPayer());

        billingListItem.setInvoiceCategoriesOfInvoicePayer(invoiceCategoriesInvoicePayer);
        billingListItem.setInvoiceCategoriesOfCostPayer(invoiceCategoriesCostPayer);

        return billingListItem;
    }

    public void addPaymentItemsToBilling(BillingListItem billingListItem, Billing billing){

        for (PaymentItem costDistributionItem : billingListItem.getCostItemsForCostPayer()) {
            if (!costDistributionItem.getBillings().contains(billing)){
                costDistributionItem.getBillings().add(billing);
            }
            paymentItemService.savePaymentItem(costDistributionItem);
        }

        for (PaymentItem costDistributionItem : billingListItem.getCostItemsForInvoicePayer()) {
            if (!costDistributionItem.getBillings().contains(billing)){
                costDistributionItem.getBillings().add(billing);
            }
            paymentItemService.savePaymentItem(costDistributionItem);
        }
    }

    public List<InvoiceCategory> categoriesForPaymentItems(List<PaymentItem> costDistributionItems){
        List<InvoiceCategory> invoiceCategories = new ArrayList<>();
        List<UUID> invoiceCategoriesIds = new ArrayList<>();

        for (PaymentItem costDistributionItem : costDistributionItems) {

            if (costDistributionItem instanceof CostDistributionItem){

                Invoice invoice = ((CostDistributionItem) costDistributionItem).getInvoice();
                InvoiceCategory invoiceCategory = invoice.getInvoiceCategory();
                if (invoiceCategory != null){
                    if (!invoiceCategoriesIds.contains(invoiceCategory.getInvoiceCategoryId())){
                        invoiceCategories.add(invoiceCategory);
                        invoiceCategoriesIds.add(invoiceCategory.getInvoiceCategoryId());
                    }
                }
            }else if (costDistributionItem instanceof Invoice){

                Invoice invoice = (Invoice) costDistributionItem;
                InvoiceCategory invoiceCategory = invoice.getInvoiceCategory();
                if (invoiceCategory != null){
                    if (!invoiceCategoriesIds.contains(invoiceCategory.getInvoiceCategoryId())){
                        invoiceCategories.add(invoiceCategory);
                        invoiceCategoriesIds.add(invoiceCategory.getInvoiceCategoryId());
                    }
                }
            }
        }

        return invoiceCategories;
    }

    public void refreshBillingsIfNecessary(PaymentItem paymentItem, Double costPaidNewTmp, Double costPaidOldTmp){
        List<Billing> billings = new ArrayList<>();
        if (paymentItem instanceof CostDistributionItem){
            CostDistributionItem costDistributionItem = (CostDistributionItem) paymentItem;
            billings = billingRepository.findByCostDistributionItem(costDistributionItem);
        }else if (paymentItem instanceof Invoice){
            Invoice invoice = (Invoice) paymentItem;
            billings.addAll(billingRepository.findByInvoice(invoice));

            List<CostDistributionItem> costDistributionItemsOfInvoice = costDistributionItemRepository.findByInvoice(invoice);
            for (CostDistributionItem costDistributionItem : costDistributionItemsOfInvoice) {
                billings.addAll(billingRepository.findByCostDistributionItem(costDistributionItem));
            }
        }

        Double costPaidOld = costPaidOldTmp != null ? costPaidOldTmp : 0;
        Double costPaidNew = costPaidNewTmp != null ? costPaidNewTmp : 0;
        if (!costPaidOld.equals(costPaidNew)){
            if (costPaidOld > 0 && costPaidNew == 0){
//                Entfernung aus Abrechnung (falls abgeschlossen)
                for (Billing billing : billings) {

                    boolean hasFound = removeFromBillingIfNecessary(billing, paymentItem);
                    Billing newBilling = billingRepository.save(billing);

                    if (hasFound){
                        createBillingReport(newBilling);
                    }
                }

            }else if (costPaidOld == 0 && costPaidNew > 0){
//                Can be ignored because:
//                - Is billing not paid, it is not necessary
//                - Is billing paid, it is all done. The difference can be balanced by 'executeBalance' procedure
            }
        }

        for (Billing billing : billings) {
            BigDecimal sumToPay = sumToPay(billing, false);
            billing.setSumToPay(sumToPay);

            BigDecimal sumToPayRest = sumToPay(billing, true);
            billing.setSumPaid(sumToPay.subtract(sumToPayRest));

            boolean hasFound = abortBillingIfNecessary(billing, sumToPay);
            Billing newBilling = billingRepository.save(billing);

            if (hasFound){
                createBillingReport(newBilling);
            }
        }
    }

    public Boolean abortBillingIfNecessary(Billing billing, BigDecimal sumToBePaid){
        if (billing.getBillingStatusEnum() == null) {
            return false;
        }

        if (billing.getBillingStatusEnum().equals(BillingStatusEnum.TO_PAY) && billing.getSumPaid() != null && billing.getSumPaid().compareTo(new BigDecimal(0)) == 0){
            if (sumToBePaid.compareTo(new BigDecimal(0)) <= 0){
                billing.setBillingStatusEnum(BillingStatusEnum.ABORTED);

                AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));

                UserContact userContact = userContactRepository.findById(billing.getCostPayerId()).orElse(null);
                if (userContact != null){
                    AppUser appUser = userContact.getAppUserContact();
                    if (appUser != null){

                        if (billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true){
                            BasicData basicData = messagingService.createBillingAbortedAutomaticallyMessage(appUser, createdBy);
                            MessageDTO messageDTO = messagingService.createMessageDTOFromJson(basicData.getValue());
                            firebaseService.sendTextMessage(appUser, FirebaseMessageType.MESSAGE_PAYMENT, messageDTO.getSubject(), messageDTO.getMessage());
                        }else{
                            BasicData basicData = messagingService.createBillingAbortedAutomaticallyMessage(createdBy,appUser);
                            MessageDTO messageDTO = messagingService.createMessageDTOFromJson(basicData.getValue());
                            firebaseService.sendTextMessage(createdBy, FirebaseMessageType.MESSAGE_PAYMENT, messageDTO.getSubject(), messageDTO.getMessage());
                        }

                    }
                }
            }
            return true;
        }

        return false;
    }

    public Boolean removeFromBillingIfNecessary(Billing billing, PaymentItem paymentItem){
        if (billing.getBillingStatusEnum() == null) {
            return false;
        }

        BigDecimal costPaid = paymentItem.getCostPaid() != null ? paymentItem.getCostPaid() : new BigDecimal(0);
        BigDecimal moneyValue = paymentItem.getMoneyValue() != null ? paymentItem.getMoneyValue() : new BigDecimal(0);

        BigDecimal paymentValue = moneyValue.subtract(costPaid);

        if (billing.getBillingStatusEnum().equals(BillingStatusEnum.PAID) ||
                billing.getBillingStatusEnum().equals(BillingStatusEnum.FINISHED) ||
                billing.getBillingStatusEnum().equals(BillingStatusEnum.PAYMENT_CONFIRMED) ||
                billing.getBillingStatusEnum().equals(BillingStatusEnum.ARCHIVED)){
            Set<Billing> billingsNew = paymentItem.getBillings().stream().filter(t -> !t.getBillingId().toString().equals(billing.getBillingId().toString())).collect(Collectors.toSet());
            paymentItem.setBillings(billingsNew);
            paymentItemService.savePaymentItem(paymentItem);

            AppUser appUserOfBilling = appUserRepository.findById(UUID.fromString(billing.getCreatedBy())).orElse(null);

            Invoice invoice = null;
            CostDistributionItem costDistributionItem = null;
            if (paymentItem instanceof Invoice){
                invoice = (Invoice) paymentItem;
            }else{
                costDistributionItem = (CostDistributionItem) paymentItem;
                invoice = costDistributionItem.getInvoice();
            }
            MainFunctionEnum mainFunctionEnum = invoiceHelperService.recognizeMainFunctionType(invoice, appUserOfBilling);

            if (MainFunctionEnum.EXPENSE.equals(mainFunctionEnum)){
                billing.setSumPaid(billing.getSumPaid().subtract(paymentValue));
            }

            billingRepository.save(billing);

            return true;
        }

        return false;
    }

    @Async
    public void refreshBillingsIfNecessaryAsync(PaymentItem paymentItem, Double costPaidNew, Double costPaidOld){
        refreshBillingsIfNecessary(paymentItem, costPaidNew, costPaidOld);
    }

    public BigDecimal sumToPay(Billing billing){
        return sumToPay(billing, false);
    }

    public BigDecimal sumToPay(Billing billing, boolean useRestDebtValue){
        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByBillings(billing);
        List<Invoice> invoices = invoiceRepository.findByBillings(billing);

        List<PaymentItem> paymentItems = new ArrayList<>();
        paymentItems.addAll(costDistributionItems);
        paymentItems.addAll(invoices);

        AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));

        PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());

        List<InvoiceDTO> invoicesForInvoicePayer = filterByInvoicesForPayerTypeAndGetDTO(paymentItems, createdBy, paymentPerson, createdBy, false, false);
        List<InvoiceDTO> invoicesForCostPayer = filterByInvoicesForPayerTypeAndGetDTO(paymentItems, createdBy, paymentPerson, createdBy, true, false);

        BigDecimal sumOfInvoicePayer = new BigDecimal(0);
        for (InvoiceDTO invoiceDTO : invoicesForInvoicePayer) {
            if (useRestDebtValue){
                sumOfInvoicePayer = sumOfInvoicePayer.add(invoiceDTO.getRestDebtValue());
            }else{
                sumOfInvoicePayer = sumOfInvoicePayer.add(invoiceDTO.getDebtValue());
            }

        }

        BigDecimal sumOfCostPayer = new BigDecimal(0);
        for (InvoiceDTO invoiceDTO : invoicesForCostPayer) {
            if (useRestDebtValue){
                sumOfCostPayer = sumOfCostPayer.add(invoiceDTO.getRestDebtValue());
            }else{
                sumOfCostPayer = sumOfCostPayer.add(invoiceDTO.getDebtValue());
            }

        }

        Boolean isNormalPayment = billing.getIsNormalPayment();

        BigDecimal sumToBePaid;
        if (isNormalPayment){
            sumToBePaid = sumOfInvoicePayer.subtract(sumOfCostPayer);
        }else{
            sumToBePaid = sumOfCostPayer.subtract(sumOfInvoicePayer);
        }

        return sumToBePaid;
    }

    public BillingDTO mapToDTO(Billing billing, AppUser currentUser){
        BillingDTO billingDTO = new BillingDTO();

        AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        PaymentPerson costPayer = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());

        billingDTO.setBillingId(billing.getBillingId());
        billingDTO.setBillingStatusEnum(billing.getBillingStatusEnum());
        billingDTO.setInvoicePayerDTO(paymentPersonService.mapEntityToDTO(createdBy));
        billingDTO.setCostPayerDTO(paymentPersonService.mapEntityToDTO(costPayer));
        billingDTO.setSumToPay(billing.getSumToPay());
        billingDTO.setSumPaid(billing.getSumPaid());
        billingDTO.setIsNormalPayment(billing.getIsNormalPayment());
        billingDTO.setCreatedDate(billing.getCreatedDate());

        if (billing.getIsNormalPayment()){
            if (StringUtils.isNotEmpty(createdBy.getPaypalName())){
                billingDTO.setPaypalMeUrl("https://paypal.me/" + createdBy.getPaypalName() + "/" + billing.getSumToPay().doubleValue());
            }
        }else{
            if (costPayer instanceof UserContact){
                AppUser appUser = ((UserContact) costPayer).getAppUserContact();
                if (appUser != null &&  StringUtils.isNotEmpty(appUser.getPaypalName())){
                    billingDTO.setPaypalMeUrl("https://paypal.me/" + appUser.getPaypalName() + "/" + billing.getSumToPay().doubleValue());
                }
            }
        }

        if (currentUser != null){
            billingDTO.setCompensationPossible(compensationPossible(billing, currentUser));
        }

        BasicDataDTO basicDataDTO = messagingService.createBillingMessageForAppUser(currentUser, billing);
        if (basicDataDTO != null){
            MessageDTO messageDTO = messagingService.createMessageDTOFromJson(basicDataDTO.getValue());
            billingDTO.setMessageType(messageDTO.getMessageType());
            billingDTO.setSubject(messageDTO.getSubject());
        }else{
            billingDTO.setMessageType(MessageType.BILLING);
            billingDTO.setSubject("Abgebrochen");
        }

        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByBillings(billing);
        List<Invoice> invoices = invoiceRepository.findByBillings(billing);

        List<PaymentItem> paymentItems = new ArrayList<>();
        paymentItems.addAll(costDistributionItems);
        paymentItems.addAll(invoices);

        PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());

        List<InvoiceDTO> invoicesForInvoicePayer = filterByInvoicesForPayerTypeAndGetDTO(paymentItems, createdBy, paymentPerson, currentUser, false, true);
        List<InvoiceDTO> invoicesForCostPayer = filterByInvoicesForPayerTypeAndGetDTO(paymentItems, createdBy, paymentPerson, currentUser, true, true);

        billingDTO.setInvoicesForInvoicePayer(invoicesForInvoicePayer);
        billingDTO.setInvoicesForCostPayer(invoicesForCostPayer);

        return billingDTO;
    }

    public List<InvoiceDTO> filterByInvoicesForPayerTypeAndGetDTO(List<PaymentItem> paymentItems, AppUser createdBy, PaymentPerson costPayer, AppUser currentUser, Boolean isCostPayer, Boolean deepMapping){
        List<InvoiceDTO> filteredInvoiceDTOs = new ArrayList<>();

        for (PaymentItem paymentItem : paymentItems) {

            Invoice invoice = null;
            CostDistributionItem costDistributionItem = null;
            if (paymentItem instanceof Invoice){
                invoice = (Invoice) paymentItem;
            }else{
                costDistributionItem = (CostDistributionItem) paymentItem;
                invoice = costDistributionItem.getInvoice();
            }
            MainFunctionEnum mainFunctionEnum = invoiceHelperService.recognizeMainFunctionType(invoice);
            InvoiceWorkflowMode invoiceWorkflowMode = invoiceHelperService.analyzeWorkflowMode(invoice, createdBy);

            boolean isValid = isPaymentItemValid(isCostPayer, paymentItem, mainFunctionEnum, invoiceWorkflowMode);
            if (isValid){
                AppUser appUser = (costPayer instanceof UserContact && ((UserContact) costPayer).getAppUserContact() != null) ? ((UserContact) costPayer).getAppUserContact() : null;

                if (appUser != null){
                    InvoiceWorkflowMode invoiceWorkflowModeCostPayer = invoiceHelperService.analyzeWorkflowMode(invoice, appUser);
                    if (paymentItem instanceof CostDistributionItem){
                        isValid = isPaymentItemValid((!isCostPayer), costDistributionItem, mainFunctionEnum, invoiceWorkflowModeCostPayer);
                    }else{
                        isValid = isPaymentItemValid((!isCostPayer), invoice, mainFunctionEnum, invoiceWorkflowModeCostPayer);
                    }
                }
            }
            if (isValid){
                InvoiceWorkflowMode invoiceWorkflowModeForMapping = invoiceHelperService.analyzeWorkflowMode(invoice, currentUser);
                InvoiceDTO invoiceDTO = addDebtValueToDto(currentUser, deepMapping, paymentItem, invoice, invoiceWorkflowModeForMapping);
                filteredInvoiceDTOs.add(invoiceDTO);
            }
        }

        return filteredInvoiceDTOs;
    }

    public Billing changeBillingStatus(Billing billing, BillingStatusEnum billingStatusEnum){

        AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));

        AppUser costPayer = null;
        UserContact userContact = userContactRepository.findById(billing.getCostPayerId()).orElse(null);
        if (userContact != null){
            costPayer = userContact.getAppUserContact();
        }

        MessageDTO messageDTO = null;
        if (BillingStatusEnum.PAID.equals(billingStatusEnum)){

            BigDecimal currentSumPaid = billing.getSumToPay();

            billing.setSumPaid(currentSumPaid);
            billing.setBillingStatusEnum(BillingStatusEnum.PAID);

            if (billing.getSumPaid().compareTo(billing.getSumToPay()) >= 0 && costPayer == null){
                billing.setSumPaid(billing.getSumToPay());
                billing.setBillingStatusEnum(BillingStatusEnum.FINISHED);
                markBillingItemsAsPaid(billing);
            }

            billingRepository.save(billing);

            BasicDataDTO basicDataDTO = messagingService.createMoneyReceivedMessage(createdBy, billing);
            messageDTO = messagingService.createMessageDTOFromJson(basicDataDTO.getValue());

            if (billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true){
                firebaseService.sendTextMessage(createdBy, FirebaseMessageType.MESSAGE_PAYMENT, messageDTO.getSubject(), messageDTO.getMessage());
            }else{

            }
        }else if (BillingStatusEnum.PAYMENT_CONFIRMED.equals(billingStatusEnum)){

            billing.setSumPaid(billing.getSumToPay());

            if (costPayer != null){
                billing.setBillingStatusEnum(BillingStatusEnum.PAYMENT_CONFIRMED);
            }else{
                billing.setBillingStatusEnum(BillingStatusEnum.FINISHED);
            }

            markBillingItemsAsPaid(billing);

            billingRepository.save(billing);

            BasicDataDTO basicDataDTO = messagingService.createPaymentConfirmedMessage(createdBy, billing);
            messageDTO = messagingService.createMessageDTOFromJson(basicDataDTO.getValue());

            if (billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true){
                if (costPayer != null){
                    firebaseService.sendTextMessage(costPayer, FirebaseMessageType.MESSAGE_PAYMENT, messageDTO.getSubject(), messageDTO.getMessage());
                }
            }else{
                firebaseService.sendTextMessage(createdBy, FirebaseMessageType.MESSAGE_PAYMENT, messageDTO.getSubject(), messageDTO.getMessage());
            }
        }else if (BillingStatusEnum.FINISHED.equals(billingStatusEnum)){
            billing.setBillingStatusEnum(BillingStatusEnum.FINISHED);
            billingRepository.save(billing);
        }

        return billing;
    }

    public List<PaymentItem> filterByInvoicesForPayerType(List<PaymentItem> paymentItems, AppUser billingCreatedBy, PaymentPerson costPayer, Boolean isCostPayer){
        List<PaymentItem> filteredInvoiceDTOs = new ArrayList<>();

        for (PaymentItem paymentItem : paymentItems) {

            Invoice invoice = null;
            CostDistributionItem costDistributionItem = null;
            if (paymentItem instanceof Invoice){
                invoice = (Invoice) paymentItem;
            }else{
                costDistributionItem = (CostDistributionItem) paymentItem;
                invoice = costDistributionItem.getInvoice();
            }
            MainFunctionEnum mainFunctionEnum = invoiceHelperService.recognizeMainFunctionType(invoice);
            InvoiceWorkflowMode invoiceWorkflowMode = invoiceHelperService.analyzeWorkflowMode(invoice, billingCreatedBy);

            boolean isValid = isPaymentItemValid(isCostPayer, paymentItem, mainFunctionEnum, invoiceWorkflowMode);
            if (isValid){
                AppUser appUser = (costPayer instanceof UserContact && ((UserContact) costPayer).getAppUserContact() != null) ? ((UserContact) costPayer).getAppUserContact() : null;

                if (appUser != null) {
                    InvoiceWorkflowMode invoiceWorkflowModeCostPayer = invoiceHelperService.analyzeWorkflowMode(invoice, appUser);
                    if (paymentItem instanceof CostDistributionItem){
                        isValid = isPaymentItemValid(!isCostPayer, costDistributionItem, mainFunctionEnum, invoiceWorkflowModeCostPayer);
                    }else{
                        isValid = isPaymentItemValid(isCostPayer, invoice, mainFunctionEnum, invoiceWorkflowModeCostPayer);
                    }
                }
            }
            if (isValid){
                filteredInvoiceDTOs.add(paymentItem);
            }
        }

        return filteredInvoiceDTOs;
    }

    public boolean isPaymentItemValid(Boolean invoicePayer, PaymentItem paymentItem, MainFunctionEnum mainFunctionEnum, InvoiceWorkflowMode invoiceWorkflowMode) {
        boolean isValid = false;
        if (paymentItem instanceof Invoice){

            if (MainFunctionEnum.INCOME.equals(mainFunctionEnum)){
                if (InvoiceWorkflowMode.CREATED_USER_READY_MODE.equals(invoiceWorkflowMode) && !invoicePayer){
                    isValid = true;
                }else if (InvoiceWorkflowMode.EXTERNAL_USER_READY_MODE.equals(invoiceWorkflowMode) && invoicePayer){
                    isValid = true;
                }
            }else if (MainFunctionEnum.EXPENSE.equals(mainFunctionEnum)){
                if (InvoiceWorkflowMode.CREATED_USER_READY_MODE.equals(invoiceWorkflowMode) && invoicePayer){
                    isValid = true;
                }else if (InvoiceWorkflowMode.EXTERNAL_USER_READY_MODE.equals(invoiceWorkflowMode) && !invoicePayer){
                    isValid = true;
                }
            }
        }else{
            if (MainFunctionEnum.INCOME.equals(mainFunctionEnum)){
                if (InvoiceWorkflowMode.CREATED_USER_READY_MODE.equals(invoiceWorkflowMode) && invoicePayer){
                    isValid = true;
                }else if (InvoiceWorkflowMode.EXTERNAL_USER_READY_MODE.equals(invoiceWorkflowMode) && !invoicePayer){
                    isValid = true;
                }
            }else if (MainFunctionEnum.EXPENSE.equals(mainFunctionEnum)){
                if (InvoiceWorkflowMode.CREATED_USER_READY_MODE.equals(invoiceWorkflowMode) && !invoicePayer){
                    isValid = true;
                }else if (InvoiceWorkflowMode.EXTERNAL_USER_READY_MODE.equals(invoiceWorkflowMode) && invoicePayer){
                    isValid = true;
                }
            }
        }
        return isValid;
    }

    public InvoiceDTO addDebtValueToDto(AppUser currentUser, Boolean deepMapping, PaymentItem paymentItem, Invoice invoice, InvoiceWorkflowMode invoiceWorkflowMode) {
        InvoiceDTO invoiceDTO = new InvoiceDTO();
        if (deepMapping){
            invoiceDTO = invoiceHelperService.mapToDTO(invoice, invoiceWorkflowMode,currentUser, MappingView.Summary.class);
        }else{
            invoiceDTO.setInvoiceId(invoice.getInvoiceId());
        }
        BigDecimal costPaid = paymentItem.getCostPaid() != null ? paymentItem.getCostPaid() : new BigDecimal(0);
        BigDecimal moneyValue = paymentItem.getMoneyValue() != null ? paymentItem.getMoneyValue() : new BigDecimal(0);

        invoiceDTO.setDebtValue(moneyValue);
        invoiceDTO.setRestDebtValue(moneyValue.subtract(costPaid));

        return invoiceDTO;
    }

    public boolean compensationPossible(Billing billing, AppUser currentUser){

        BigDecimal currentSumToPay = billing.getSumToPay();
        BigDecimal sumPaid = billing.getSumPaid() != null ? billing.getSumPaid() : new BigDecimal(0);

        if (sumPaid.compareTo(new BigDecimal(0)) == 0){
            return false;
        }

        BigDecimal plusInPayment = sumPaid.subtract(currentSumToPay);

        AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());

        if (plusInPayment.compareTo(new BigDecimal(0)) != 0){

            if (currentUser.getAppUserId().equals(createdBy.getAppUserId())){
                return true;
            }else{
                if (paymentPerson instanceof UserContact){
                    UserContact userContact = (UserContact) paymentPerson;

                    AppUser appUserContact = userContact.getAppUserContact();
                    if (appUserContact != null){
                        UserContact userContactOfCurrentUser = userContactRepository.findOneByAppUserAndAppUserContact(appUserContact, createdBy);
                        if (userContactOfCurrentUser != null){
                            return true;
                        }
                    }
                }
            }

        }else{
            return false;
        }

        return false;
    }

    public Invoice sendMoneyToPaymentPerson(PaymentPerson appUserFromInput, PaymentPerson appUserToInput, BigDecimal sum, String remarks, boolean fromIsCreator){

        Invoice invoice = new Invoice();
        invoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setSpecialType(true);

        if (remarks == null){
            invoice.setRemarks("");
        }else{
            invoice.setRemarks(remarks.trim());
        }
        BigDecimal tmpSum = (sum.compareTo(new BigDecimal(0)) == -1 ? sum.multiply(new BigDecimal(-1)) : sum);
        invoice.setSumOfInvoice(tmpSum);
        invoice.setDateOfInvoice(new Date());
        invoice.setInvoiceSource(InvoiceSource.MANUAL);
        invoice.setRepetitionTypeEnum(RepetitionTypeEnum.ONCE);

        UserContact costPayer = null;
        AppUser appUserFrom = null;
        AppUser appUserTo = null;
        if (fromIsCreator){

            if (appUserFromInput instanceof AppUser){
                appUserFrom = (AppUser) appUserFromInput;
            }else if (appUserFromInput instanceof UserContact){
                UserContact userContact = (UserContact) appUserFromInput;
                AppUser appUserContact = userContact.getAppUserContact();
                if (appUserContact != null){
                    appUserFrom = appUserContact;
                }
            }

            if (appUserFrom == null){
                return null;
            }

            invoice.setPayerId(appUserFrom.getAppUserId());
            invoice.setPayerTypeEnum(PaymentPersonTypeEnum.USER);

            if (appUserToInput instanceof UserContact){

                UserContact resultContact = null;

                UserContact tmpCostPayer = (UserContact) appUserToInput;
                AppUser costPayerUser = tmpCostPayer.getAppUser();
                if (costPayerUser != null && costPayerUser.getAppUserId().equals(appUserFrom.getAppUserId())){
                    resultContact = tmpCostPayer;
                }else{
                    AppUser appUserContactOfCostPayer = tmpCostPayer.getAppUserContact();
                    if (appUserContactOfCostPayer != null){
                        resultContact = userContactRepository.findOneByAppUserAndAppUserContact(appUserFrom, appUserContactOfCostPayer);
                    }
                }

                if (resultContact != null){
                    invoice.setPaymentRecipientId(resultContact.getPaymentPersonId());
                    invoice.setPaymentRecipientTypeEnum(resultContact.getPaymentPersonEnum());
                    if (PaymentPersonTypeEnum.USER.equals(resultContact.getVirtualPaymentPersonEnum())){
                        invoice.setCorrectionStatus(CorrectionStatus.CHECK);
                    }else{
                        invoice.setCorrectionStatus(CorrectionStatus.READY);
                    }
                    invoice.setCostPaid(new BigDecimal(0));

                    costPayer = resultContact;
                }
            }else if (appUserToInput instanceof AppUser){
                AppUser appUser = (AppUser) appUserToInput;

                costPayer = userContactRepository.findOneByAppUserAndAppUserContact(appUserFrom, appUser);
                if (costPayer != null){
                    invoice.setPaymentRecipientId(costPayer.getPaymentPersonId());
                    invoice.setPaymentRecipientTypeEnum(costPayer.getPaymentPersonEnum());
                    invoice.setCorrectionStatus(CorrectionStatus.CHECK);
                    invoice.setCostPaid(new BigDecimal(0));
                }
            }else{
                invoice.setPaymentRecipientId(appUserToInput.getPaymentPersonId());
                invoice.setPaymentRecipientTypeEnum(appUserToInput.getPaymentPersonEnum());
                invoice.setCorrectionStatus(CorrectionStatus.IGNORE);
                invoice.setCostPaid(new BigDecimal(0));
            }

        }else{

            if (appUserToInput instanceof AppUser){
                appUserTo = (AppUser) appUserToInput;
            }else if (appUserToInput instanceof UserContact){
                UserContact userContact = (UserContact) appUserToInput;
                AppUser appUserContact = userContact.getAppUserContact();
                if (appUserContact != null){
                    appUserTo = appUserContact;
                }
            }

            if (appUserTo == null){
                return null;
            }

            invoice.setPaymentRecipientId(appUserTo.getAppUserId());
            invoice.setPaymentRecipientTypeEnum(PaymentPersonTypeEnum.USER);

            if (appUserFromInput instanceof UserContact){
                UserContact resultContact = null;

                UserContact tmpCostPayer = (UserContact) appUserFromInput;
                AppUser costPayerUser = tmpCostPayer.getAppUser();
                if (costPayerUser != null && costPayerUser.getAppUserId().equals(appUserTo.getAppUserId())){
                    resultContact = tmpCostPayer;
                }else{
                    AppUser appUserContactOfCostPayer = tmpCostPayer.getAppUserContact();
                    if (appUserContactOfCostPayer != null){
                        resultContact = userContactRepository.findOneByAppUserAndAppUserContact(appUserTo, appUserContactOfCostPayer);
                    }
                }

                if (resultContact != null){
                    invoice.setPayerId(resultContact.getPaymentPersonId());
                    invoice.setPayerTypeEnum(resultContact.getPaymentPersonEnum());
                    if (PaymentPersonTypeEnum.USER.equals(resultContact.getVirtualPaymentPersonEnum())){
                        invoice.setCorrectionStatus(CorrectionStatus.CHECK);
                    }else{
                        invoice.setCorrectionStatus(CorrectionStatus.READY);
                    }
                    invoice.setCostPaid(new BigDecimal(0));

                    costPayer = resultContact;
                }

            }else if (appUserFromInput instanceof AppUser){
                AppUser appUser = (AppUser) appUserFromInput;

                UserContact userContact = userContactRepository.findOneByAppUserAndAppUserContact(appUserTo, appUser);
                if (userContact != null){
                    invoice.setPayerId(userContact.getPaymentPersonId());
                    invoice.setPayerTypeEnum(userContact.getPaymentPersonEnum());
                    invoice.setCorrectionStatus(CorrectionStatus.CHECK);
                    invoice.setCostPaid(new BigDecimal(0));
                }
            }else{
                invoice.setPayerId(appUserFromInput.getPaymentPersonId());
                invoice.setPayerTypeEnum(appUserFromInput.getPaymentPersonEnum());
                invoice.setCorrectionStatus(CorrectionStatus.IGNORE);
                invoice.setCostPaid(new BigDecimal(0));
            }

        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        if (fromIsCreator){
            savedInvoice.setCreatedBy(appUserFrom.getAppUserId().toString());
        }else{
            savedInvoice.setCreatedBy(appUserTo.getAppUserId().toString());
        }
        savedInvoice = invoiceRepository.save(savedInvoice);

        if (fromIsCreator){
            autoFillHelperService.generateDefaultCostDistributionItem(invoice, appUserFrom);
        }else{
            autoFillHelperService.generateDefaultCostDistributionItem(invoice, appUserTo);
        }

        budgetService.updateBudgetsIfNecessary(savedInvoice);

        return savedInvoice;
    }

    @Async
    public void createBillingReport(Billing billing){
        BillingListItem billingListItem = startCreateBillingListItem(billing);
        createAndSaveBillingReport(billingListItem, billing);
    }

    public File createAndSaveBillingReport(BillingListItem billingListItem, Billing billing){

        ByteArrayOutputStream outputStream = reportService.createBillingReport(billingListItem);

        File tempFile = null;
        if (outputStream != null){
            tempFile = null;
            try {
                tempFile = pathService.getBillingsPath("Rechnung_" + billing.getBillingId().toString() + ".pdf");
                try(OutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                    outputStream.writeTo(fileOutputStream);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return tempFile;
    }

    @Async
    public void createBillingMailsAsync(BillingConfigDTO reportConfig, Billing billing, BillingListItem billingListItem) throws IOException, MessagingException {

        File tempFile = createAndSaveBillingReport(billingListItem, billing);

        NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.GERMAN);
        numberFormatter.setMaximumFractionDigits(2);
        numberFormatter.setMinimumFractionDigits(2);

        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        UserContact costPayer = (UserContact) paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());

        BigDecimal sumTotal = billingListItem.getSumToBePaid();
        try {

            Map<String, Object> viewModel = new HashMap<String, Object>();
            viewModel.put("userName", currentUser.getAppUserName());
            viewModel.put("sumTotal", numberFormatter.format(sumTotal) + " EUR");
            if (sumTotal.compareTo(new BigDecimal(0)) == -1){
                viewModel.put("direction", "debts");
            }else{
                viewModel.put("direction", "credit");
            }
            viewModel.put("sumTotal", numberFormatter.format(sumTotal) + " EUR");

            Settings settings = settingsService.getCurrentSettings();

            if (reportConfig.isSendMailCostPayer()){
                Mail mail = Mail.builder()
                        .isMessageTextHtml(true)
                        .build();

                Map<String, Object> viewModelTmp = new HashMap<String, Object>();
                viewModelTmp.putAll(viewModel);

                if (tempFile != null){
                    ArrayList<File> files = new ArrayList<>();
                    files.add(tempFile);
                    mail.setAttachments(files);
                }

                if ((billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true)){
                    mail.getRecipients().add(MailRecipient.builder()
                            .address(costPayer.getEmail()).name(costPayer.getContactName()).build());

                    AppUser appUser = costPayer.getAppUserContact();
                    if (appUser != null){
                        viewModelTmp.put("url", settings.getDomainUrl() + "/#/billings/" + billing.getBillingId().toString());
                    }
                    mail.setSubject("Neue Abrechnung von " + currentUser.getAppUserName());
                }else{
                    mail.getRecipients().add(MailRecipient.builder()
                            .address(currentUser.getEmail()).name(currentUser.getAppUserName()).build());
                    viewModelTmp.put("url", settings.getDomainUrl() + "/#/billings/" + billing.getBillingId().toString());
                    mail.setSubject("Neue Abrechnung von " + costPayer.getPaymentPersonName());
                }

                mail.setMessageTextHtml(mailService.generateMessageTemplate("newBilling", viewModelTmp));
                mailService.sendMail(mail);
            }

            if (reportConfig.isSendMailInvoicePayer()){
                Mail mail = Mail.builder()
                        .isMessageTextHtml(true)
                        .subject("Ihre erstellte Abrechnung")
                        .build();

                if (tempFile != null){
                    ArrayList<File> files = new ArrayList<>();
                    files.add(tempFile);
                    mail.setAttachments(files);
                }

                Map<String, Object> viewModelTmp = new HashMap<String, Object>();
                viewModelTmp.putAll(viewModel);

                mail.getRecipients().add(MailRecipient.builder()
                        .address(currentUser.getEmail()).name(currentUser.getAppUserName()).build());

                viewModelTmp.put("url", settings.getDomainUrl() + "/#/billings/" + billing.getBillingId().toString());
                mail.setMessageTextHtml(mailService.generateMessageTemplate("newBillingCreator", viewModelTmp));

                mailService.sendMail(mail);
            }

        } catch (SendFailedException e) {
            e.printStackTrace();
        }
    }

    public BillingListItem startCreateBillingListItem(Billing billing){

        AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        UserContact costPayer = (UserContact) paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());

        List<PaymentItem> paymentItems = new ArrayList<>();
        paymentItems.addAll(costDistributionItemRepository.findByBillings(billing));
        paymentItems.addAll(invoiceRepository.findByBillings(billing));

        PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());

        List<PaymentItem> filteredPaymentItemsInvoicePayer = filterByInvoicesForPayerType(paymentItems, createdBy, paymentPerson, false);
        List<PaymentItem> filteredPaymentItemsCostPayer = filterByInvoicesForPayerType(paymentItems, createdBy, paymentPerson, true);

        BillingListItem billingListItem = new BillingListItem();
        billingListItem.setCostItemsForInvoicePayer(filteredPaymentItemsInvoicePayer);
        billingListItem.setCostItemsForCostPayer(filteredPaymentItemsCostPayer);

        billingListItem.setInvoicePayer(PaymentPersonPojo.fromIPaymentPerson(createdBy));
        billingListItem.setCostPayer(PaymentPersonPojo.fromIPaymentPerson(costPayer));

        billingListItem.setSumToBePaid(billing.getSumToPay());

        return billingListItem;
    }

}
