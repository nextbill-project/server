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

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPAQueryBase;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.expr.BooleanExpression;
import de.nextbill.domain.dtos.InvoiceDTO;
import de.nextbill.domain.dtos.InvoiceListDTO;
import de.nextbill.domain.dtos.SearchDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.enums.PaymentTypeEnum;
import de.nextbill.domain.enums.RepetitionTypeEnum;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.InvoiceCostDistributionItem;
import de.nextbill.domain.repositories.*;
import de.nextbill.domain.utils.views.MappingView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static de.nextbill.domain.model.QAppUser.appUser;
import static de.nextbill.domain.model.QCostDistributionItem.costDistributionItem;
import static de.nextbill.domain.model.QInvoice.invoice;
import static de.nextbill.domain.model.QStandingOrder.standingOrder;

@Service
public class SearchService {

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private AutoFillHelperService autoFillHelperService;

    @Autowired
    private InvoiceHelperService invoiceHelperService;

    @Autowired
    private CostDistributionRepository costDistributionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AppUserRepository appUserRepository;

    public List<InvoiceCostDistributionItem> search(boolean distinctInvoices, SearchDTO searchDTO){
        return search(distinctInvoices, searchDTO, null, null);
    }

    public List<InvoiceCostDistributionItem> search(boolean distinctInvoices, SearchDTO searchDTO, List<UUID> validInvoiceIds){
        return search(distinctInvoices, searchDTO, null, validInvoiceIds);
    }

    public List<InvoiceCostDistributionItem> search(boolean distinctInvoices, SearchDTO searchDTO, String loggedInUserName, List<UUID> validInvoiceIds){
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        CostDistribution costDistribution = null;
        if (searchDTO.getCostDistributionId() != null){
            costDistribution = costDistributionRepository.getOne(searchDTO.getCostDistributionId());
        }

        if (loggedInUserName == null){
            loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        List<UUID> invoiceCategories = new ArrayList<>();
        if (searchDTO.getInvoiceCategoryDTOs() != null && !searchDTO.getInvoiceCategoryDTOs().isEmpty() ){
            invoiceCategories.addAll(searchDTO.getInvoiceCategoryDTOs());
        }else if (searchDTO.getInvoiceCategoryDTO() != null){
            invoiceCategories.add(searchDTO.getInvoiceCategoryDTO().getInvoiceCategoryId());
        }

        List<UUID> costPayers = new ArrayList<>();
        if (searchDTO.getCostPayers() != null && !searchDTO.getCostPayers().isEmpty() ){
            costPayers.addAll(searchDTO.getCostPayers());
        }else if (searchDTO.getCostPayer() != null){
            costPayers.add(searchDTO.getCostPayer().getPayerId());
        }

        return search(searchDTO.getIncomeMainFunctionEnum(), searchDTO.getExpenseMainFunctionEnum(), searchDTO.getPaymentTypeEnum(), searchDTO.getStandingOrder(), distinctInvoices,searchDTO.getPayerPerson(),searchDTO.getPaymentRecipientPerson(), costPayers, searchDTO.getSpecialType(),searchDTO.getRepetitionTypeEnum(), costDistribution,invoiceCategories,searchDTO.getStartDate(), searchDTO.getEndDate(), booleanBuilder, loggedInUserName, validInvoiceIds, true, searchDTO.getRemarks(), searchDTO.getFullText());
    }

    public List<InvoiceCostDistributionItem> search(Boolean incomeMainFunctionEnum, Boolean expenseMainFunctionEnum, PaymentTypeEnum paymentTypeEnum, Boolean standingOrder, boolean distinctInvoice, PaymentPerson payerPerson, PaymentPerson paymentRecipientPerson, List<UUID> costPayers, Boolean isSpecialType, RepetitionTypeEnum repetitionTypeEnum,
                                                    CostDistribution costDistribution, List<UUID> invoiceCategories, Date startDate, Date endDate, BooleanBuilder whereClause, String loggedInUserName, List<UUID> allInvoiceIdsForUserInput, boolean useEqualDate, String remarks, String fullText){
        List<InvoiceCostDistributionItem> invoiceCostDistributionItems = new ArrayList<>();

        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        List<UUID> allInvoiceIdsForUser = new ArrayList<>();
        if (allInvoiceIdsForUserInput == null){
            allInvoiceIdsForUser = allInvoicesForUser(currentUser, startDate, endDate);
        }else{
            allInvoiceIdsForUser.addAll(allInvoiceIdsForUserInput);
        }

        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        JPAQuery jpaQueryFactory = new JPAQuery(entityManager);

        JPAQueryBase jpaQuery = jpaQueryFactory.from(costDistributionItem, appUser).leftJoin(costDistributionItem.invoice(), invoice);

        whereClause = buildQuery(whereClause, currentUser, userContactIds, incomeMainFunctionEnum, expenseMainFunctionEnum, paymentTypeEnum, standingOrder, payerPerson, paymentRecipientPerson, costPayers, isSpecialType, repetitionTypeEnum, invoiceCategories, startDate, endDate, allInvoiceIdsForUserInput, useEqualDate, remarks, fullText);
//
        jpaQuery.where(whereClause);
        List<CostDistributionItem> costDistributionItems = jpaQuery.list(costDistributionItem);

        Set<UUID> distinctInvoices = new HashSet<>();
        for (CostDistributionItem costDistributionItem : costDistributionItems) {

            Invoice invoice1 = costDistributionItem.getInvoice();

            if ((distinctInvoice && invoice1 != null && distinctInvoices.contains(invoice1.getInvoiceId())) || (!allInvoiceIdsForUser.contains(invoice1.getInvoiceId()))){
                continue;
            }

            InvoiceCostDistributionItem invoiceCostDistributionItem = new InvoiceCostDistributionItem();
            invoiceCostDistributionItem.setCostDistributionItem(costDistributionItem);

            invoiceCostDistributionItem.setInvoice(invoice1);
            invoiceCostDistributionItems.add(invoiceCostDistributionItem);

            distinctInvoices.add(invoice1.getInvoiceId());
        }

        JPAQuery jpaQueryFactory2 = new JPAQuery(entityManager);
        JPAQueryBase jpaQuery2 = jpaQueryFactory2.from(costDistributionItem, invoice, appUser);

        jpaQuery2.where(whereClause);
        List<Invoice> invoices = jpaQuery2.list(invoice);

        for (Invoice invoice : invoices) {
            if (!distinctInvoices.contains(invoice.getInvoiceId()) && (allInvoiceIdsForUser.contains(invoice.getInvoiceId()))){
                InvoiceCostDistributionItem invoiceCostDistributionItem = new InvoiceCostDistributionItem();
                invoiceCostDistributionItem.setInvoice(invoice);

                invoiceCostDistributionItems.add(invoiceCostDistributionItem);

                distinctInvoices.add(invoice.getInvoiceId());
            }
        }

        if (costDistribution != null){
            List<InvoiceCostDistributionItem> resultInvoiceCostDistributionItems = new ArrayList<>();

            List<CostDistributionItem> costDistributionItemsForCostDistribution = costDistributionItemRepository.findByCostDistribution(costDistribution);

            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {

                Invoice invoice = invoiceCostDistributionItem.getInvoice();

                List<CostDistributionItem> costDistributionItemsForInvoice = null;
                if (invoice.getCostDistributionItems() == null || invoice.getCostDistributionItems().isEmpty()){
                    costDistributionItemsForInvoice = costDistributionItemRepository.findByInvoice(invoice);
                }else{
                    costDistributionItemsForInvoice = invoice.getCostDistributionItems();
                }

                boolean isCostDistribution = autoFillHelperService.areCostDistributionItemListsEqual(costDistributionItemsForInvoice, costDistributionItemsForCostDistribution);
                if (isCostDistribution){
                    resultInvoiceCostDistributionItems.add(invoiceCostDistributionItem);
                }
            }

            return resultInvoiceCostDistributionItems;

        }

        return invoiceCostDistributionItems;
    }

    public List<UUID> allInvoicesForUser(AppUser currentUser, Date startDate, Date endDate){

        Date nowDate = new Date();

        if (startDate == null){
            LocalDate startLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDateTmp = startLocalDateTmp.minusYears(100);
            startLocalDateTmp = startLocalDateTmp.withDayOfMonth(1);
            startDate = Date.from(startLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        }

        if (endDate == null){
            LocalDate endLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endLocalDateTmp = endLocalDateTmp.plusYears(100);
            endLocalDateTmp = endLocalDateTmp.withDayOfMonth(endLocalDateTmp.lengthOfMonth());
            endDate = Date.from(endLocalDateTmp.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
        }

        List<Invoice> invoices = new ArrayList<>();

        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        if (userContactIds.isEmpty()){
            userContactIds.add(UUID.randomUUID());
        }

        invoices.addAll(invoiceRepository.findAllForCheckList(currentUser.getAppUserId().toString(), userContactIds, PageRequest.of( 0, Integer.MAX_VALUE, Sort.Direction.ASC , "dateOfInvoice")).getContent());

        invoices.addAll(invoiceRepository.findWithUserAndNotStatusAndIsInvoiceTemplate(currentUser.getAppUserId().toString(), PageRequest.of( 0, Integer.MAX_VALUE, Sort.Direction.ASC, "dateOfInvoice" )).getContent());

        List<UUID> invoiceIds = invoices.stream().map(Invoice::getInvoiceId).distinct().collect(Collectors.toList());

        invoiceIds.addAll(invoiceRepository.findAllIdsForReadyList(currentUser.getAppUserId().toString(), userContactIds));

        return invoiceIds;
    }

    public BooleanBuilder buildQuery(BooleanBuilder whereClause, AppUser currentUser, List<UUID> currentUserContacts, Boolean incomeMainFunctionEnum, Boolean expenseMainFunctionEnum, PaymentTypeEnum paymentTypeEnum, Boolean useStandingOrder, PaymentPerson payerPerson, PaymentPerson paymentRecipientPerson, List<UUID> costPayers, Boolean isSpecialType, RepetitionTypeEnum repetitionTypeEnum,
                                     List<UUID> invoiceCategories, Date startDate, Date endDate, List<UUID> allInvoiceIdsForUser, boolean useEqualDate, String remarks, String fullText){

        BooleanExpression booleanExpression = invoice.createdBy.eq(appUser.appUserId.stringValue());
        whereClause.and(booleanExpression);

        if (incomeMainFunctionEnum != null && expenseMainFunctionEnum != null){

            BooleanExpression totalBooleanExpression = null;

            if (incomeMainFunctionEnum != null && incomeMainFunctionEnum){

                BooleanExpression possibleIncome = invoice.paymentRecipientId.eq(currentUser.getAppUserId());
                totalBooleanExpression = possibleIncome;

                possibleIncome = invoice.paymentRecipientId.in(currentUserContacts);
                totalBooleanExpression = totalBooleanExpression.or(possibleIncome);

                possibleIncome = costDistributionItem.payerId.eq(currentUser.getAppUserId()).and(appUser.appUserId.eq(invoice.paymentRecipientId));
                totalBooleanExpression = totalBooleanExpression.or(possibleIncome);

                possibleIncome = costDistributionItem.payerId.in(currentUserContacts).and(appUser.appUserId.eq(invoice.paymentRecipientId));
                totalBooleanExpression = totalBooleanExpression.or(possibleIncome);
            }

            if (expenseMainFunctionEnum != null && expenseMainFunctionEnum){

                BooleanExpression possibleExpense = invoice.payerId.eq(currentUser.getAppUserId());
                if (totalBooleanExpression != null){
                    totalBooleanExpression = totalBooleanExpression.or(possibleExpense);
                }else{
                    totalBooleanExpression = possibleExpense;
                }

                possibleExpense = invoice.payerId.in(currentUserContacts);
                totalBooleanExpression = totalBooleanExpression.or(possibleExpense);

                possibleExpense = costDistributionItem.payerId.eq(currentUser.getAppUserId()).and(appUser.appUserId.eq(invoice.payerId));
                totalBooleanExpression = totalBooleanExpression.or(possibleExpense);

                possibleExpense = costDistributionItem.payerId.in(currentUserContacts).and(appUser.appUserId.eq(invoice.payerId));
                totalBooleanExpression = totalBooleanExpression.or(possibleExpense);

            }

            whereClause.and(totalBooleanExpression);
        }

        booleanExpression = costDistributionItem.invoice().invoiceId.eq(invoice.invoiceId);
        whereClause.and(booleanExpression);

        if (StringUtils.isNotBlank(remarks)){
            BooleanExpression booleanExpression2 = invoice.remarks.likeIgnoreCase("%" + remarks + "%").or(costDistributionItem.remarks.likeIgnoreCase("%" + remarks + "%"));
            whereClause.and(booleanExpression2);
        }

        if (StringUtils.isNotBlank(fullText)){
            BooleanExpression booleanExpression2 = invoice.ocrFullText.likeIgnoreCase("%" + fullText + "%");
            whereClause.and(booleanExpression2);
        }

        if (allInvoiceIdsForUser != null){
            BooleanExpression booleanExpression2 = invoice.invoiceId.in(allInvoiceIdsForUser);
            whereClause.and(booleanExpression2);
        }

        if (payerPerson != null){
            BooleanExpression booleanExpression2 = invoice.payerId.eq(payerPerson.getPaymentPersonId());
            whereClause.and(booleanExpression2);
        }

        if (paymentRecipientPerson != null){
            BooleanExpression booleanExpression2 = invoice.paymentRecipientId.eq(paymentRecipientPerson.getPaymentPersonId());
            whereClause.and(booleanExpression2);
        }

        if (costPayers != null && !costPayers.isEmpty()){
            BooleanExpression booleanExpression2 = costDistributionItem.payerId.in(costPayers);
            whereClause.and(booleanExpression2);
        }

        if (isSpecialType != null){
            if (isSpecialType){
                BooleanExpression booleanExpression2 = invoice.specialType.isTrue();
                whereClause.and(booleanExpression2);
            }else{
                BooleanExpression booleanExpression2 = invoice.specialType.isNull().or(invoice.specialType.isFalse());
                whereClause.and(booleanExpression2);
            }
        }

        if (repetitionTypeEnum != null){
            BooleanExpression booleanExpression2 = invoice.repetitionTypeEnum.eq(repetitionTypeEnum);
            whereClause.and(booleanExpression2);
        }

        if (paymentTypeEnum != null){
            BooleanExpression booleanExpression2 = invoice.paymentTypeEnum.eq(paymentTypeEnum);
            whereClause.and(booleanExpression2);
        }

        if (invoiceCategories != null && !invoiceCategories.isEmpty()){
            BooleanExpression booleanExpression2 = invoice.invoiceCategory().invoiceCategoryId.in(invoiceCategories);
            whereClause.and(booleanExpression2);
        }

        if (startDate != null){
            if (useEqualDate){
                BooleanExpression booleanExpression2 = invoice.dateOfInvoice.after(startDate).or(invoice.dateOfInvoice.eq(startDate));
                whereClause.and(booleanExpression2);
            }else{
                BooleanExpression booleanExpression2 = invoice.dateOfInvoice.after(startDate);
                whereClause.and(booleanExpression2);
            }
        }

        if (endDate != null){
            if (useEqualDate){
                BooleanExpression booleanExpression2 = invoice.dateOfInvoice.before(endDate).or(invoice.dateOfInvoice.eq(endDate));
                whereClause.and(booleanExpression2);
            }else{
                BooleanExpression booleanExpression2 = invoice.dateOfInvoice.before(endDate);
                whereClause.and(booleanExpression2);
            }
        }

        BooleanExpression booleanExpression3 = costDistributionItem.invoice().invoiceStatusEnum.ne(InvoiceStatusEnum.DELETED);
        whereClause.and(booleanExpression3);

        if (useStandingOrder != null){
            QInvoice standingOrderInvoiceTemplate = QStandingOrder.standingOrder.invoiceTemplate();
            JPASubQuery sub = new JPASubQuery();
            sub.from(invoice, standingOrder).leftJoin(standingOrderInvoiceTemplate, invoice).where(invoice.eq(standingOrderInvoiceTemplate));

            if (useStandingOrder == false){
                booleanExpression3 = invoice.neAny(sub.list(invoice));
                whereClause.and(booleanExpression3);
            }else{
                booleanExpression3 = invoice.eqAny(sub.list(invoice));
                whereClause.and(booleanExpression3);
            }
        }

        booleanExpression3 = costDistributionItem.invoice().isNotNull();
        whereClause.and(booleanExpression3);

        return whereClause;
    }

    public List<Invoice> invoicesInInvoiceCostDistributionItems(List<InvoiceCostDistributionItem> invoiceCostDistributionItems){
        List<Invoice> invoices = new ArrayList<>();
        for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
            invoices.add(invoiceCostDistributionItem.getInvoice());
        }

        return invoices;
    }

    public InvoiceListDTO searchForInvoices(AppUser currentUser, SearchDTO searchDTOInput, Optional<Boolean> onlyStandingOrder, Optional<InvoiceStatusEnum> invoiceStatusEnum, Optional<Integer> pageNumber){

        List<Invoice> invoices = new ArrayList<>();
        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        SearchDTO searchDTO = new SearchDTO();
        if (searchDTOInput != null){
            searchDTO = searchDTOInput;
        }

        if (onlyStandingOrder.isPresent() && onlyStandingOrder.get()) {
            searchDTO.setStandingOrder(true);

            List<UUID> allInvoiceTemplates = invoiceRepository.findWithUserAndNotStatusAndIsInvoiceTemplate(currentUser.getAppUserId().toString(), PageRequest.of( 0, Integer.MAX_VALUE, Sort.Direction.ASC, "dateOfInvoice" )).getContent()
                    .stream().map(Invoice::getInvoiceId).collect(Collectors.toList());

            List<InvoiceCostDistributionItem> invoiceCostDistributionItems = search(true, searchDTO, allInvoiceTemplates);
            invoices.addAll(invoicesInInvoiceCostDistributionItems(invoiceCostDistributionItems));

        }else if (invoiceStatusEnum != null && invoiceStatusEnum.isPresent()){
            searchDTO.setStandingOrder(false);

            if (invoiceStatusEnum.get().equals(InvoiceStatusEnum.CHECK)){
                List<UUID> allCheckListInvoiceIds = invoiceRepository.findAllForCheckList(currentUser.getAppUserId().toString(), userContactIds, PageRequest.of( 0, Integer.MAX_VALUE, Sort.Direction.DESC , "dateOfInvoice")).getContent()
                        .stream().map(Invoice::getInvoiceId).collect(Collectors.toList());

                List<InvoiceCostDistributionItem> invoiceCostDistributionItems = search(true, searchDTO, allCheckListInvoiceIds);
                invoices.addAll(invoicesInInvoiceCostDistributionItems(invoiceCostDistributionItems).stream().filter(t -> allCheckListInvoiceIds.contains(t.getInvoiceId())).collect(Collectors.toList()));
            }else{
                Date startDateOfBean = searchDTO.getStartDate();

                Date newestDate = null;
                if (startDateOfBean != null) {
                    newestDate = invoiceRepository.findNewestDateForReadyList(currentUser.getAppUserId().toString(), userContactIds,startDateOfBean);
                }else{
                    newestDate = (Date) invoiceRepository.findNewestDateForReadyList(currentUser.getAppUserId().toString(), userContactIds);
                }

                Integer currentAmountMonths = pageNumber.orElse(0);
                Integer amountMonth = pageNumber.orElse(0);
                for (int i = 0; i < amountMonth; i++) {
                    currentAmountMonths = i+1;

                    if (newestDate != null) {
                        Date endOfMonthOfNewestDate = firstDayOfMonth(newestDate);
                        newestDate = invoiceRepository.findNewestDateForReadyList(currentUser.getAppUserId().toString(), userContactIds, endOfMonthOfNewestDate);
                    }
                }

                if (newestDate != null) {
                    searchDTO.setStartDate( firstDayOfMonth(newestDate) );
                    searchDTO.setEndDate( lastDayOfMonth(newestDate) );

                    List<UUID> allReadyListInvoiceIds = invoiceRepository.findAllIdsForReadyList(currentUser.getAppUserId().toString(), userContactIds);
                    invoices.addAll(invoicesInInvoiceCostDistributionItems(search(true, searchDTO, allReadyListInvoiceIds)));
                }

                List<InvoiceDTO> invoiceDTOs = new ArrayList<>();

                for (Invoice invoice : invoices) {
                    invoiceDTOs.add(invoiceHelperService.mapToDTO(invoice,false, currentUser, MappingView.Summary.class));
                }
                InvoiceListDTO invoiceListDTO = new InvoiceListDTO();
                invoiceListDTO.setInvoiceDTOs(invoiceDTOs);
                invoiceListDTO.setCurrentPage(currentAmountMonths);

                return invoiceListDTO;
            }
        }else{

            List<InvoiceCostDistributionItem> invoiceCostDistributionItems = search(true, searchDTO);
            invoices.addAll(invoicesInInvoiceCostDistributionItems(invoiceCostDistributionItems));

//            invoices.addAll(invoiceService.allInvoicesForUser(currentUser));
        }

        Map<UUID, List<UUID>> userContactIdsMap = new HashMap<>();
        List<InvoiceDTO> invoiceDTOs = new ArrayList<>();
        for (Invoice invoice : invoices) {
            invoiceDTOs.add(invoiceHelperService.mapToDTO(invoice,false, currentUser, MappingView.Summary.class, userContactIdsMap));
        }

        InvoiceListDTO invoiceListDTO = new InvoiceListDTO();
        invoiceListDTO.setInvoiceDTOs(invoiceDTOs);

        return invoiceListDTO;
    }

    private Date firstDayOfMonth(Date date) {
        LocalDate startLocalDateTmp = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate();
        startLocalDateTmp = startLocalDateTmp.withDayOfMonth(1);
        return Date.from(startLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    private Date lastDayOfMonth(Date date) {
        LocalDate endLocalDateTmp = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate();
        endLocalDateTmp = endLocalDateTmp.withDayOfMonth(endLocalDateTmp.lengthOfMonth());
        return Date.from(endLocalDateTmp.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
    }

    public List<InvoiceCostDistributionItem> searchForInvoices(AppUser currentUser, SearchDTO searchDTO, InvoiceStatusEnum invoiceStatusEnum){

        List<InvoiceCostDistributionItem> invoicesCostDistributionItems = new ArrayList<>();
        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        searchDTO.setStandingOrder(false);

        if (invoiceStatusEnum.equals(InvoiceStatusEnum.CHECK)){
            List<UUID> allCheckListInvoiceIds = invoiceRepository.findAllForCheckList(currentUser.getAppUserId().toString(), userContactIds, PageRequest.of( 0, Integer.MAX_VALUE, Sort.Direction.DESC , "dateOfInvoice")).getContent()
                    .stream().map(Invoice::getInvoiceId).collect(Collectors.toList());

            List<InvoiceCostDistributionItem> invoiceCostDistributionItems = search(true, searchDTO, allCheckListInvoiceIds);
            invoicesCostDistributionItems.addAll(invoiceCostDistributionItems.stream().filter(t -> allCheckListInvoiceIds.contains(t.getInvoice().getInvoiceId())).collect(Collectors.toList()));
        }else{
            List<UUID> allReadyListInvoiceIds = invoiceRepository.findAllIdsForReadyList(currentUser.getAppUserId().toString(), userContactIds);

            invoicesCostDistributionItems.addAll(search(true, searchDTO, allReadyListInvoiceIds));
        }

        return invoicesCostDistributionItems;
    }

}
