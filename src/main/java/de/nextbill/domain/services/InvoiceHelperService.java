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

import de.nextbill.domain.dtos.ArticleDTO;
import de.nextbill.domain.dtos.InvoiceDTO;
import de.nextbill.domain.enums.*;
import de.nextbill.domain.interfaces.PaymentItem;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.DatabaseChange;
import de.nextbill.domain.repositories.*;
import de.nextbill.domain.utils.views.MappingView;
import de.nextbill.domain.utils.views.ViewHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InvoiceHelperService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private PaymentPersonService paymentPersonService;

    @Autowired
    private InvoiceCategoryService invoiceCategoryService;

    @Autowired
    private StandingOrderRepository standingOrderRepository;

    @Autowired
    private CostDistributionHelper costDistributionHelper;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceFailureRepository invoiceFailureRepository;

    public MainFunctionEnum recognizeMainFunctionType(Invoice invoice){
        AppUser creator = appUserRepository.findOneByAppUserId(UUID.fromString(invoice.getCreatedBy()));
        if (invoice.getPayerId() != null && invoice.getPayerId().equals(creator.getAppUserId())){
            return MainFunctionEnum.EXPENSE;
        }else if (invoice.getPaymentRecipientId() != null && invoice.getPaymentRecipientId().equals(creator.getAppUserId())){
            return MainFunctionEnum.INCOME;
        }

        return MainFunctionEnum.EXPENSE;
    }

    public MainFunctionEnum recognizeMainFunctionType(Invoice invoice, AppUser perspectiveOfUser){
        MainFunctionEnum mainFunctionEnum = recognizeMainFunctionType(invoice);

        List<UserContact> appUserContacts = userContactRepository.findAllByAppUserContact(perspectiveOfUser);
        List<UUID> appUserContactIds = appUserContacts.stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        if (invoice.getPaymentRecipientId() != null && appUserContactIds.contains(invoice.getPaymentRecipientId())) {
            mainFunctionEnum = MainFunctionEnum.INCOME;
        }else if (invoice.getPayerId() != null && appUserContactIds.contains(invoice.getPayerId())){
            mainFunctionEnum = MainFunctionEnum.EXPENSE;
        }

//        if (isReverseInvoice(invoice, perspectiveOfUser, mainFunctionEnum)){
//            mainFunctionEnum = MainFunctionEnum.EXPENSE.equals(mainFunctionEnum) ? MainFunctionEnum.INCOME : MainFunctionEnum.EXPENSE;
//        }

        return mainFunctionEnum;
    }

    public void autoSetCorrectionStatus(Invoice invoice) {
        MainFunctionEnum mainFunctionEnum = recognizeMainFunctionType(invoice);
        PaymentPerson paymentPerson = null;
        if (MainFunctionEnum.INCOME.equals(mainFunctionEnum)){
            if (invoice.getPayerId() != null){
                paymentPerson = paymentPersonService.findPaymentPerson(invoice.getPayerId(), invoice.getPayerTypeEnum());
            }
        }else{
            if (invoice.getPaymentRecipientId() != null){
                paymentPerson = paymentPersonService.findPaymentPerson(invoice.getPaymentRecipientId(), invoice.getPaymentRecipientTypeEnum());
            }
        }
        if (paymentPerson != null){
            recognizeAndSetCorrectionStatus(invoice, paymentPerson);
        }
    }

    public void recognizeAndSetCorrectionStatus(PaymentItem paymentItem, PaymentPerson paymentPerson) {
        if (paymentPerson.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER)) {
            paymentItem.setCorrectionStatus(CorrectionStatus.CHECK);
        }else if (paymentPerson.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT)){
            paymentItem.setCorrectionStatus(CorrectionStatus.READY);
        }else{
            paymentItem.setCorrectionStatus(CorrectionStatus.IGNORE);
        }
    }

    public Map<UUID, StandingOrder> findInvoiceStandingOrdersByDataChangeBeans(List<DatabaseChange> databaseChangeBeen) {

        List<Invoice> invoices = new ArrayList<>();

        List<Object> objects = databaseChangeBeen.stream().filter(t->t.getClassType().equals(Invoice.class)).map(DatabaseChange::getObject).collect(Collectors.toList());
        for (Object object : objects) {
            invoices.add((Invoice) object);
        }

        return findInvoiceStandingOrders(invoices);
    }

    public Map<UUID, StandingOrder> findInvoiceStandingOrders(List<Invoice> invoices) {

        Map<UUID, StandingOrder> invoiceStandingOrderMap = new HashMap<>();
        if (!invoices.isEmpty()){
            List<Object[]> standingOrderInvoices = invoiceRepository.findOneByStandingOrderInvoice(invoices);
            standingOrderInvoices.addAll(invoiceRepository.findOneByStandingOrderInvoiceTemplate(invoices));

            for (Object[] standingOrderInvoice : standingOrderInvoices) {
                invoiceStandingOrderMap.put(((Invoice) standingOrderInvoice[0]).getInvoiceId(), (StandingOrder) standingOrderInvoice[1]);
            }
        }

        return invoiceStandingOrderMap;
    }

    public InvoiceDTO mapToDTO(Invoice invoice, InvoiceWorkflowMode invoiceWorkflowMode, Boolean mobileView, AppUser currentUser, Map<UUID, StandingOrder> invoiceStandingOrderMap, Class<? extends MappingView.OnlyNameAndId> view, Map<UUID, List<UUID>> userContactIdsMap){

        if (currentUser == null){
            String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
            currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));
        }

        invoice = repairInvoice(invoice);

        InvoiceDTO invoiceDTO = InvoiceDTO.generateDTO(invoice);

        if (ViewHelper.fieldValidForView(InvoiceDTO.class, "createdByDTO", view)) {
            if (invoice.getCreatedBy() != null){
                invoiceDTO.setCreatedById(UUID.fromString(invoice.getCreatedBy()));
                if (mobileView == null || mobileView == false){
                    AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(invoice.getCreatedBy()));
                    invoiceDTO.setCreatedByDTO(paymentPersonService.mapEntityToDTO(createdBy));
                }
            }
        }

        if (ViewHelper.fieldValidForView(InvoiceDTO.class, "invoiceImageId", view)) {
            InvoiceImage invoiceImage = invoice.getInvoiceImage();
            if (invoiceImage != null){
                invoiceDTO.setInvoiceImageId(invoiceImage.getInvoiceImageId());
                invoiceDTO.setRotateImage(invoiceImage.getRotate());
            }
        }

        if (ViewHelper.fieldValidForView(InvoiceDTO.class, "invoiceCategoryDTO", view)) {
            if (invoice.getInvoiceCategory() != null){
                invoiceDTO.setInvoiceCategoryDTO(invoiceCategoryService.mapToDTO(invoice.getInvoiceCategory(), currentUser));
            }
        }

        if (ViewHelper.fieldValidForView(InvoiceDTO.class, "standingOrderInvoiceTemplateId", view)) {
            if (invoiceStandingOrderMap != null){
                StandingOrder standingOrder = invoiceStandingOrderMap.get(invoice.getInvoiceId());
                if (standingOrder != null){
                    invoiceDTO.setStandingOrderInvoiceTemplateId(standingOrder.getInvoiceTemplate().getInvoiceId());
                    if (standingOrder.getInvoiceTemplate() != null && invoice.getInvoiceId().equals(standingOrder.getInvoiceTemplate().getInvoiceId())){
                        invoiceDTO.setStandingOrderStartDate(standingOrder.getStartDate());
                    }
                }
            }else{
                StandingOrder standingOrder = standingOrderRepository.findOneByStandingOrderInvoice(invoice);
                if (standingOrder != null){
                    invoiceDTO.setStandingOrderInvoiceTemplateId(standingOrder.getInvoiceTemplate().getInvoiceId());
                }else{
                    standingOrder = standingOrderRepository.findOneByStandingOrderInvoiceTemplate(invoice);
                    if (standingOrder != null){
                        invoiceDTO.setStandingOrderInvoiceTemplateId(standingOrder.getInvoiceTemplate().getInvoiceId());
                        invoiceDTO.setStandingOrderStartDate(standingOrder.getStartDate());
                    }
                }
            }
        }

        if (ViewHelper.fieldValidForView(InvoiceDTO.class, "articleDTOs", view)) {
            if (invoice.getArticleDTOsAsJson() != null){
                invoiceDTO.setArticleDTOs(convertToArticleDTOs(invoice.getArticleDTOsAsJson()));
            }
        }

        if (ViewHelper.fieldValidForView(InvoiceDTO.class, "invoiceFailureMessage", view)) {
            List<InvoiceFailure> invoiceFailures = invoiceFailureRepository.findByInvoice(invoice);
            if (!invoiceFailures.isEmpty()){
                List<String> errorMessages = invoiceFailures.stream().map(InvoiceFailure::getMessage).collect(Collectors.toList());
                invoiceDTO.setInvoiceFailureMessage(StringUtils.join(errorMessages, "\n"));
            }
        }

        if (mobileView == null || mobileView == false){

            if (ViewHelper.fieldValidForView(InvoiceDTO.class, "invoiceWorkflowMode", view)) {
                if (invoiceWorkflowMode == null){
                    invoiceDTO.setInvoiceWorkflowMode(analyzeWorkflowMode(invoice, currentUser));
                }else{
                    invoiceDTO.setInvoiceWorkflowMode(invoiceWorkflowMode);
                }
            }

            if (ViewHelper.fieldValidForView(InvoiceDTO.class, "mainFunctionEnum", view)) {
                invoiceDTO.setMainFunctionEnum(recognizeMainFunctionType(invoice, currentUser));
            }

            if (ViewHelper.fieldValidForView(InvoiceDTO.class, "reverseInvoice", view)) {
                invoiceDTO.setReverseInvoice(isReverseInvoice(invoice, currentUser, invoiceDTO.getMainFunctionEnum()));
            }

            if (ViewHelper.fieldValidForView(InvoiceDTO.class, "payerDTO", view)) {
                if (invoice.getPayerId() != null){
                    invoiceDTO.setPayerDTO(paymentPersonService.findPaymentPersonAndGetDTO(invoice.getPayerId(), invoice.getPayerTypeEnum()));
                }
            }

            if (ViewHelper.fieldValidForView(InvoiceDTO.class, "paymentRecipientDTO", view)) {
                if (invoice.getPaymentRecipientId() != null){
                    invoiceDTO.setPaymentRecipientDTO(paymentPersonService.findPaymentPersonAndGetDTO(invoice.getPaymentRecipientId(), invoice.getPaymentRecipientTypeEnum()));
                }
            }

            if (ViewHelper.fieldValidForView(InvoiceDTO.class, "moneyValue", view)) {
                BigDecimal invoiceCost = costDistributionHelper.invoiceCostForPaymentPerson(invoice, currentUser, userContactIdsMap);
                invoiceDTO.setMoneyValue(invoiceCost);
            }

        }

        return invoiceDTO;
    }

    public Invoice repairInvoice(Invoice invoice){
        if (invoice.getInvoiceSource() == null){
            if (invoice.getRepetitionTypeEnum() != null && invoice.getRepetitionTypeEnum().equals(RepetitionTypeEnum.MONTHLY)){
                invoice.setInvoiceSource(InvoiceSource.STANDING_ORDER);
            }else{
                invoice.setInvoiceSource(InvoiceSource.MANUAL);
            }

            invoiceRepository.save(invoice);
        }
        return invoice;
    }

    public InvoiceDTO mapToDTO(Invoice invoice, InvoiceWorkflowMode invoiceWorkflowMode, AppUser currentUser, Class<? extends MappingView.OnlyNameAndId> view){
        return mapToDTO(invoice,invoiceWorkflowMode, false, currentUser, null, view, null);
    }

    public InvoiceDTO mapToDTO(Invoice invoice, Boolean mobileView, AppUser currentUser, Class<? extends MappingView.OnlyNameAndId> view){
        return mapToDTO(invoice, null, mobileView, currentUser, null, view, null);
    }

    public InvoiceDTO mapToDTO(Invoice invoice, Boolean mobileView, AppUser currentUser, Class<? extends MappingView.OnlyNameAndId> view, Map<UUID, List<UUID>> userContactIdsMap){
        return mapToDTO(invoice, null, mobileView, currentUser, null, view, userContactIdsMap);
    }

    public InvoiceDTO mapToDTO(Invoice invoice, Boolean mobileView, AppUser currentUser, Map<UUID, StandingOrder> invoiceStandingOrderMap, Class<? extends MappingView.OnlyNameAndId> view, Map<UUID, List<UUID>> userContactIdsMap){
        return mapToDTO(invoice, null, mobileView, currentUser, invoiceStandingOrderMap, view, userContactIdsMap);
    }

    public InvoiceDTO mapToDTO(Invoice invoice, Boolean mobileView){
        return mapToDTO(invoice, null, mobileView, null, null, MappingView.Detail.class, null);
    }

    public InvoiceDTO mapToDTO(Invoice invoice, Boolean mobileView, AppUser currentUser){
        return mapToDTO(invoice, null, mobileView, currentUser, null, MappingView.Detail.class, null);
    }

    public InvoiceDTO mapToDTO(Invoice invoice, Boolean mobileView, AppUser currentUser, Map<UUID, StandingOrder> invoiceStandingOrderMap){
        return mapToDTO(invoice, null, mobileView, currentUser, invoiceStandingOrderMap, MappingView.Detail.class, null);
    }

    public InvoiceDTO mapToDTO(Invoice invoice, Boolean mobileView, Class<? extends MappingView.OnlyNameAndId> view){
        return mapToDTO(invoice, null, mobileView, null, null, view, null);
    }

    public boolean isReverseInvoice(Invoice invoice, AppUser currentUser, MainFunctionEnum mainFunctionEnum){
        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getAppUser).map(AppUser::getAppUserId).collect(Collectors.toList());

        if (MainFunctionEnum.EXPENSE.equals(mainFunctionEnum)){
            if (invoice.getPaymentRecipientId() != null && userContactIds.contains(invoice.getPaymentRecipientId())) return true;
        }else{
            if (invoice.getPayerId() != null && userContactIds.contains(invoice.getPayerId())) return true;
        }

        return false;
    }

    public InvoiceWorkflowMode analyzeWorkflowMode(Invoice invoice, AppUser currentUser){

        if (isCreatedUserCheckMode(invoice, currentUser)){
            return InvoiceWorkflowMode.CREATED_USER_CHECK_MODE;
        }else if (isCreatedUserReadyMode(invoice, currentUser)){
            return InvoiceWorkflowMode.CREATED_USER_READY_MODE;
        }else if (isExternalUserCheckMode(invoice, currentUser)){
            return InvoiceWorkflowMode.EXTERNAL_USER_CHECK_MODE;
        }else if (isExternalUserReadyMode(invoice, currentUser)){
            return InvoiceWorkflowMode.EXTERNAL_USER_READY_MODE;
        }

        return null;
    }

    public boolean isCreatedUserCheckMode(Invoice currentInvoice, AppUser currentUser){

        if (currentUser.getAppUserId().equals(UUID.fromString(currentInvoice.getCreatedBy()))){
            if (InvoiceStatusEnum.CHECK.equals(currentInvoice.getInvoiceStatusEnum()) || InvoiceStatusEnum.ANALYZING.equals(currentInvoice.getInvoiceStatusEnum())) {
                return true;
            }
        }
        return false;
    }

    public boolean isCreatedUserReadyMode(Invoice currentInvoice, AppUser currentUser){

        if (currentUser.getAppUserId().equals(UUID.fromString(currentInvoice.getCreatedBy()))){
            if (InvoiceStatusEnum.READY.equals(currentInvoice.getInvoiceStatusEnum())) {
                return true;
            }
        }
        return false;
    }

    public boolean isExternalUserCheckMode(Invoice currentInvoice, AppUser currentUser){

        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        if ((currentInvoice.getCorrectionStatus() == null || currentInvoice.getCorrectionStatus().equals(CorrectionStatus.CHECK)  || currentInvoice.getCorrectionStatus().equals(CorrectionStatus.PROBLEM))
                && ( (currentInvoice.getPayerId() != null && userContactIds.contains(currentInvoice.getPayerId())) ||  (currentInvoice.getPaymentRecipientId() != null && userContactIds.contains(currentInvoice.getPaymentRecipientId())) )){
            return true;
        }

        List<CostDistributionItem> costDistributionItems = null;
        if (currentInvoice.getCostDistributionItems() == null || currentInvoice.getCostDistributionItems().isEmpty()){
            costDistributionItems = costDistributionItemRepository.findByInvoice(currentInvoice);
        }else{
            costDistributionItems = currentInvoice.getCostDistributionItems();
        }

        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            if ((costDistributionItem.getCorrectionStatus() == null || costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.CHECK)  || costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.PROBLEM))
                    && (costDistributionItem.getPayerId() != null && userContactIds.contains(costDistributionItem.getPayerId()))){
                return true;
            }
        }
        return false;
    }

    public boolean isExternalUserReadyMode(Invoice currentInvoice, AppUser currentUser){

        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        if ((currentInvoice.getCorrectionStatus() != null && currentInvoice.getCorrectionStatus().equals(CorrectionStatus.READY))
                && ( (currentInvoice.getPayerId() != null && userContactIds.contains(currentInvoice.getPayerId())) ||  (currentInvoice.getPaymentRecipientId() != null && userContactIds.contains(currentInvoice.getPaymentRecipientId())) )){
            return true;
        }

        List<CostDistributionItem> costDistributionItems = null;
        if (currentInvoice.getCostDistributionItems() == null || currentInvoice.getCostDistributionItems().isEmpty()){
            costDistributionItems = costDistributionItemRepository.findByInvoice(currentInvoice);
        }else{
            costDistributionItems = currentInvoice.getCostDistributionItems();
        }

        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            if ((costDistributionItem.getCorrectionStatus() != null && costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.READY))
                    && ( (costDistributionItem.getPayerId() != null && userContactIds.contains(costDistributionItem.getPayerId())))){
                return true;
            }
        }
        return false;
    }

    public List<ArticleDTO> convertToArticleDTOs(String dbString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return (List<ArticleDTO>) objectMapper.readValue(dbString, List.class);
        } catch (IOException e) {
            log.warn("Could not convert dbString to ArticleDTOs!", e);
        }
        return new ArrayList<>();
    }

    public String convertArticleDTOsToJson(List<ArticleDTO> articleDTOs) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(articleDTOs);
        } catch (IOException e) {
            log.warn("Could not convert ArticleDTOs to dbString!", e);
        }
        return null;
    }

}
