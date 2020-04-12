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

import de.nextbill.domain.dtos.InvoiceDTO;
import de.nextbill.domain.dtos.MessageDTO;
import de.nextbill.domain.enums.*;
import de.nextbill.domain.interfaces.PaymentItem;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.repositories.*;
import de.nextbill.domain.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private InvoiceFailureRepository invoiceFailureRepository;

    @Autowired
    private PaymentPersonService paymentPersonService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private PaymentItemService paymentItemService;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private InvoiceHelperService invoiceHelperService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private InvoiceImageRepository invoiceImageRepository;

    @Autowired
    private BusinessPartnerService businessPartnerService;

    @Autowired
    private InvoiceCategoryRepository invoiceCategoryRepository;

    @Autowired
    private BillingService billingService;

    @Autowired
    private CostDistributionItemService costDistributionItemService;

    public Invoice configureCorrectionStatusAttributes(Invoice invoiceFound, Invoice invoice){
        PaymentPerson payer = null;
        PaymentPerson paymentRecipient = null;
        if (invoiceFound.getPayerId() != null && invoice.getPayerId() != null && !invoiceFound.getPayerId().equals(invoice.getPayerId())){
            payer = paymentPersonService.findPaymentPerson(invoice.getPayerId(), invoice.getPayerTypeEnum());
        }
        if (invoiceFound.getPaymentRecipientId() != null && invoice.getPaymentRecipientId() != null && !invoiceFound.getPaymentRecipientId().equals(invoice.getPaymentRecipientId())){
            paymentRecipient = paymentPersonService.findPaymentPerson(invoice.getPaymentRecipientId(), invoice.getPaymentRecipientTypeEnum());
        }
        if ((payer != null && payer.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER)) ||
                (paymentRecipient != null && paymentRecipient.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.USER))) {
            invoice.setCorrectionStatus(CorrectionStatus.CHECK);
        }else if ((payer != null && payer.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT)) ||
                (paymentRecipient != null && paymentRecipient.getVirtualPaymentPersonEnum().equals(PaymentPersonTypeEnum.CONTACT))){
            invoice.setCorrectionStatus(CorrectionStatus.READY);
        }else if (payer != null || paymentRecipient != null){
            invoice.setCorrectionStatus(CorrectionStatus.IGNORE);
        }

        return invoice;
    }

    public List<PaymentItem> paymentItemsForCorrectionMode(Invoice currentInvoice, boolean isCheck){

        List<PaymentItem> paymentItems = new ArrayList<>();
        if (currentInvoice != null){

            String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
            AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

            List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

            List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(currentInvoice);

            if (isCheck){
                if ((currentInvoice.getCorrectionStatus() == null || currentInvoice.getCorrectionStatus().equals(CorrectionStatus.CHECK)  || currentInvoice.getCorrectionStatus().equals(CorrectionStatus.PROBLEM))
                        && ( (currentInvoice.getPayerId() != null && userContactIds.contains(currentInvoice.getPayerId())) ||  (currentInvoice.getPaymentRecipientId() != null && userContactIds.contains(currentInvoice.getPaymentRecipientId())) )){
                    paymentItems.add(currentInvoice);
                }

                for (CostDistributionItem costDistributionItem : costDistributionItems) {
                    if ((costDistributionItem.getCorrectionStatus() == null || costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.CHECK)  || costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.PROBLEM))
                            && (costDistributionItem.getPayerId() != null && userContactIds.contains(costDistributionItem.getPayerId()))){
                        paymentItems.add(costDistributionItem);
                    }
                }
            }else{
                if ((currentInvoice.getCorrectionStatus() != null && currentInvoice.getCorrectionStatus().equals(CorrectionStatus.READY))
                        && ( (currentInvoice.getPayerId() != null && userContactIds.contains(currentInvoice.getPayerId())) ||  (currentInvoice.getPaymentRecipientId() != null && userContactIds.contains(currentInvoice.getPaymentRecipientId())) )){
                    paymentItems.add(currentInvoice);
                }

                for (CostDistributionItem costDistributionItem : costDistributionItems) {
                    if ((costDistributionItem.getCorrectionStatus() != null && costDistributionItem.getCorrectionStatus().equals(CorrectionStatus.READY))){
                        paymentItems.add(costDistributionItem);
                    }
                }
            }

        }

        return paymentItems;
    }

    public void setCorrectionStatus(Invoice newInvoice, CorrectionStatus correctionStatus, AppUser currentUser){

        if (CorrectionStatus.CHECK.equals(correctionStatus)){

            if (invoiceHelperService.isCreatedUserReadyMode(newInvoice, currentUser)){
                newInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
                invoiceRepository.save(newInvoice);
            }else if (invoiceHelperService.isExternalUserReadyMode(newInvoice, currentUser)){
                List<PaymentItem> paymentItems2 = paymentItemsForCorrectionMode(newInvoice, false);
                for (PaymentItem paymentItem : paymentItems2) {
                    paymentItem.setCorrectionStatus(CorrectionStatus.CHECK);
                    paymentItemService.savePaymentItem(paymentItem);
                }
            }
        }else{
            if (invoiceHelperService.isCreatedUserCheckMode(newInvoice, currentUser)){
                newInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.READY);
                invoiceRepository.save(newInvoice);
                List<InvoiceFailure> invoiceFailures = invoiceFailureRepository.findByInvoice(newInvoice);
                invoiceFailureRepository.deleteAll(invoiceFailures);
            }else if (invoiceHelperService.isExternalUserCheckMode(newInvoice, currentUser)){
                List<PaymentItem> paymentItems2 = paymentItemsForCorrectionMode(newInvoice, true);
                for (PaymentItem paymentItem : paymentItems2) {
                    paymentItem.setCorrectionStatus(CorrectionStatus.READY);
                    paymentItemService.savePaymentItem(paymentItem);
                }
            }
        }

    }

    public Invoice createInvoice(AppUser currentUser, InvoiceDTO invoiceDTO) {
        Invoice invoice = null;

        if (invoiceDTO.getInvoiceStatusEnum() == null){
            invoiceDTO.setInvoiceStatusEnum(InvoiceStatusEnum.READY);
        }

        if (invoiceDTO.getInvoiceId() == null){
            invoiceDTO.setInvoiceId(UUID.randomUUID());
        }

        BeanMapper beanMapper = new BeanMapper();
        invoice= beanMapper.map(invoiceDTO, Invoice.class);

        if (invoice.getPaymentTypeEnum() == null) {
            invoice.setPaymentTypeEnum(PaymentTypeEnum.NOT_DEFINED);
        }

        if (invoiceDTO.getArticleDTOs() != null){
            invoice.setArticleDTOsAsJson(invoiceHelperService.convertArticleDTOsToJson(invoiceDTO.getArticleDTOs()));
        }else{
            invoice.setArticleDTOsAsJson(null);
        }

        if (invoiceDTO.getRepetitionTypeEnum() == null){
            invoice.setRepetitionTypeEnum(RepetitionTypeEnum.ONCE);
        }

        if (invoiceDTO.getInvoiceImageId() != null){
            invoice.setInvoiceImage(invoiceImageRepository.findById(invoiceDTO.getInvoiceImageId()).orElse(null));
        }

        if (invoiceDTO.getInvoiceCategoryDTO() != null){
            invoice.setInvoiceCategory(invoiceCategoryRepository.findById(invoiceDTO.getInvoiceCategoryDTO().getInvoiceCategoryId()).orElse(null));
        }

        invoice.setCreatedBy(currentUser.getAppUserId().toString());

        if (invoice.getCorrectionStatus() == null){
            invoiceHelperService.autoSetCorrectionStatus(invoice);
        }

        Invoice newInvoice = invoiceRepository.save(invoice);

        businessPartnerService.refreshBusinessPartnerMetrics(invoice);

        budgetService.updateBudgetsIfNecessary(invoice);

        return newInvoice;
    }

    public Invoice updateInvoice(AppUser currentUser, InvoiceDTO invoiceDTO, Optional<CorrectionStatus> correctionStatus) {
        Double costPaidNew = invoiceDTO.getCostPaid() != null ? invoiceDTO.getCostPaid().doubleValue() : 0;

        boolean sendChangedInvoiceMessageIfNecessary = false;

        Invoice oldInvoice = invoiceRepository.findById(invoiceDTO.getInvoiceId()).orElse(null);
        if (oldInvoice != null){
            sendChangedInvoiceMessageIfNecessary = true;
        }

        BigDecimal oldCostPaid = oldInvoice != null && oldInvoice.getCostPaid() != null ? oldInvoice.getCostPaid() : new BigDecimal(0);

        BeanMapper beanMapper = new BeanMapper();
        Invoice invoice = beanMapper.map(invoiceDTO, Invoice.class);

        if (oldInvoice != null) {
            invoice.setOcrFullText(oldInvoice.getOcrFullText());
        }

        if (invoice.getPaymentTypeEnum() == null) {
            invoice.setPaymentTypeEnum(PaymentTypeEnum.NOT_DEFINED);
        }

        if (invoiceDTO.getInvoiceImageId() != null){
            invoice.setInvoiceImage(invoiceImageRepository.findById(invoiceDTO.getInvoiceImageId()).orElse(null));
        }else{
            invoice.setInvoiceImage(null);
        }

        if (invoiceDTO.getArticleDTOs() != null){
            invoice.setArticleDTOsAsJson(invoiceHelperService.convertArticleDTOsToJson(invoiceDTO.getArticleDTOs()));
        }else{
            invoice.setArticleDTOsAsJson(null);
        }

        if (oldInvoice != null && invoice.getScansioResultData() == null && oldInvoice.getScansioResultData() != null){
            invoice.setScansioResultData(oldInvoice.getScansioResultData());
        }

        if (invoiceDTO.getInvoiceCategoryDTO() != null){
            invoice.setInvoiceCategory(invoiceCategoryRepository.findById(invoiceDTO.getInvoiceCategoryDTO().getInvoiceCategoryId()).orElse(null));
        }else{
            invoice.setInvoiceCategory(null);
        }

        Invoice invoiceFound = invoiceRepository.findById(invoiceDTO.getInvoiceId()).orElse(null);
        if (invoiceFound != null){
            invoice.setCreatedBy(invoiceFound.getCreatedBy());
            invoice.setBillings(invoiceFound.getBillings());
        }else{
            invoice.setCreatedBy(currentUser.getAppUserId().toString());
        }

        if (invoice.getCorrectionStatus() == null){
            invoiceHelperService.autoSetCorrectionStatus(invoice);
        }

        if (invoiceDTO.getCostDistributionItemDTOs() != null){
            costDistributionItemService.updateCostDistributionItemsOfInvoice(invoice, invoiceDTO.getCostDistributionItemDTOs());
        }

        Invoice newInvoice = invoiceRepository.save(invoice);

        List<InvoiceFailure> invoiceFailures = invoiceFailureRepository.findByInvoice(newInvoice);
        invoiceFailureRepository.deleteAll(invoiceFailures);

        if (correctionStatus.isPresent()){
            setCorrectionStatus(newInvoice, correctionStatus.get(), currentUser);
        }

        billingService.refreshBillingsIfNecessaryAsync(newInvoice, costPaidNew, oldCostPaid.doubleValue());

        if (sendChangedInvoiceMessageIfNecessary){
            sendChangedInvoiceMessageIfNecessary(newInvoice.getInvoiceId(), currentUser);
        }

        businessPartnerService.refreshBusinessPartnerMetrics(invoice);

        budgetService.updateBudgetsIfNecessary(invoice);

        return newInvoice;
    }

    @Async
    public void sendChangedInvoiceMessageIfNecessary(UUID invoiceId, AppUser currentUser){

        Invoice invoiceInput = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoiceInput == null){
            throw new RuntimeException("No invoice for invoiceId found!");
        }

        if (InvoiceStatusEnum.READY.equals(invoiceInput.getInvoiceStatusEnum())){

            List<UUID> currentUserIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());
            currentUserIds.add(currentUser.getAppUserId());

            List<PaymentItem> paymentItems = new ArrayList<>();
            if (CorrectionStatus.READY.equals(invoiceInput.getCorrectionStatus())){
                paymentItems.add(invoiceInput);
            }

            List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(invoiceInput);
            for (CostDistributionItem costDistributionItem : costDistributionItems) {
                if (CorrectionStatus.READY.equals(costDistributionItem.getCorrectionStatus())){
                    paymentItems.add(costDistributionItem);
                }
            }

            for (PaymentItem paymentItem : paymentItems) {

                PaymentPerson paymentPerson = null;
                if (paymentItem instanceof Invoice){

                    Invoice invoice = (Invoice) paymentItem;
                    MainFunctionEnum mainFunctionEnum = invoiceHelperService.recognizeMainFunctionType(invoice);
                    if (MainFunctionEnum.INCOME.equals(mainFunctionEnum)){
                        if (invoice.getPayerId() != null){
                            paymentPerson = paymentPersonService.findPaymentPerson(invoice.getPayerId(), invoice.getPayerTypeEnum());
                        }
                    }else{
                        if (invoice.getPaymentRecipientId() != null){
                            paymentPerson = paymentPersonService.findPaymentPerson(invoice.getPaymentRecipientId(), invoice.getPaymentRecipientTypeEnum());
                        }
                    }
                }else if (paymentItem instanceof CostDistributionItem){
                    paymentPerson = paymentPersonService.findPaymentPerson(paymentItem.getCostPayerId(), paymentItem.getPayerTypeEnum());
                }

                List<UUID> idsOfPaymentPerson = new ArrayList<>();
                idsOfPaymentPerson.add(paymentPerson.getPaymentPersonId());

                AppUser appUserContact = null;

                if (PaymentPersonTypeEnum.USER.equals(paymentPerson.getVirtualPaymentPersonEnum())){
                    appUserContact = ((UserContact) paymentPerson).getAppUserContact();
                    idsOfPaymentPerson.addAll(userContactRepository.findAllByAppUserContactAndBasicStatusEnum(appUserContact, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList()));
                }

                Set<Billing> billings = paymentItem.getBillings();
                if (billings != null && billings.size() > 0){
                    for (Billing billing : billings) {

                        if (!BillingStatusEnum.ABORTED.equals(billing.getBillingStatusEnum()) &&
                                !BillingStatusEnum.ARCHIVED.equals(billing.getBillingStatusEnum()) &&
                                !BillingStatusEnum.ARCHIVED_DELETED.equals(billing.getBillingStatusEnum()) &&
                                !BillingStatusEnum.TO_PAY.equals(billing.getBillingStatusEnum())){

                            UUID createdById = UUID.fromString(billing.getCreatedBy());
                            if (idsOfPaymentPerson.contains(createdById) || idsOfPaymentPerson.contains(billing.getCostPayerId())){
                                if (appUserContact != null){
                                    BasicData basicData = messagingService.createInvoiceChangedMessage(appUserContact, invoiceInput, billing.getBillingStatusEnum());
                                    MessageDTO messageDTO = messagingService.createMessageDTOFromJson(basicData.getValue());
                                    firebaseService.sendTextMessage(appUserContact, FirebaseMessageType.MESSAGE_INVOICE_CHANGED, messageDTO.getSubject(), messageDTO.getMessage());
                                }else{
                                    if (PaymentPersonTypeEnum.CONTACT.equals(paymentPerson.getVirtualPaymentPersonEnum())){
//                                    TODO: Send Message per E-Mail
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Transactional
    public void deleteInvoice(Invoice invoice, boolean deleteCostDistributionItems, boolean deleteComplete){

        invoice.setInvoiceStatusEnum(InvoiceStatusEnum.DELETED);
        invoice.setStandingOrder(null);
        invoice = invoiceRepository.save(invoice);

        List<Invoice> invoiceToDelete = new ArrayList<Invoice>();
        invoiceToDelete.add(invoice);

        for (InvoiceFailure invoiceFailure : invoiceFailureRepository.findByInvoiceIn(invoiceToDelete)) {
            invoiceFailureRepository.delete(invoiceFailure);
        }

        if (deleteCostDistributionItems){
            List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(invoice);
            for (CostDistributionItem costDistributionItem : costDistributionItems) {
                costDistributionItemRepository.delete(costDistributionItem);
            }
        }

        if (deleteComplete){
            invoiceRepository.delete(invoice);
        }


    }
}
