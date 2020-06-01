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

import de.nextbill.domain.dtos.BasicDataDTO;
import de.nextbill.domain.enums.BasicDataType;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.DatabaseChange;
import de.nextbill.domain.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MobileDeviceService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BillingHelperService billingHelperService;

    @Autowired
    private BasicDataRepository basicDataRepository;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private CostDistributionRepository costDistributionRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceCategoryRepository invoiceCategoryRepository;

    @Autowired
    private InvoiceFailureRepository invoiceFailureRepository;

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private AuditService auditService;

    public List<DatabaseChange> allRelevantData(AppUser currentUser){

        List<AppUser> appUsers = appUserRepository.findAll();

        List<BasicData> basicDatas = basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.STATISTIC);
        basicDatas.addAll(basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.MISTAKE_MESSAGE));
        basicDatas.addAll(basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.CHANGED_MESSAGE));

        List<BusinessPartner> businessPartners = businessPartnerRepository.findAll();
        List<UUID> businessPartnerIds = businessPartners.stream().map(BusinessPartner::getBusinessPartnerId).collect(Collectors.toList());

        List<CostDistribution> costDistributions = costDistributionRepository.findByCreatedByAndBasicStatusEnum(currentUser.getAppUserId().toString(), BasicStatusEnum.OK);

        List<Invoice> invoices = new ArrayList<>();

        invoices.addAll(invoiceRepository.findWithUserAndNotStatus(currentUser.getAppUserId().toString(), InvoiceStatusEnum.DELETED, PageRequest.of( 0, Integer.MAX_VALUE, Sort.Direction.ASC, "dateOfInvoice" )).getContent());
        List<CostDistributionItem> tmpCostDistributionItems = costDistributionItemRepository.findByPayerAndInvoiceNotNullAndNoStandingOrder(currentUser);

        findInvoicesAndBusinessPartnersToAdd(businessPartners, businessPartnerIds, invoices, tmpCostDistributionItems);

        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoiceIn(invoices);
        List<CostDistributionItem> costDistributionItems2 = costDistributionItemRepository.findByCostDistributionIn(costDistributions);
        costDistributionItems.addAll(costDistributionItems2);

        List<InvoiceFailure> invoiceFailures = invoiceFailureRepository.findByInvoiceIn(invoices);

        List<UserContact> userContacts = userContactRepository.findAllByAppUser(currentUser);
        List<UserContact> directUserContacts = userContactRepository.findAllByAppUserContact(currentUser);
        userContacts.addAll(directUserContacts);

        List<InvoiceCategory> invoiceCategorys = invoiceCategoryRepository.findAllByAppUserIsNullOrAppUser(currentUser);
        Set<UUID> invoiceCategoryIds = invoiceCategorys.stream().map(InvoiceCategory::getInvoiceCategoryId).collect(Collectors.toSet());

        Set<UUID> userContactIds = userContacts.stream().map(UserContact::getUserContactId).collect(Collectors.toSet());
        findUserContactsToAdd(costDistributionItems, currentUser, userContacts, userContactIds);

        userContactIds = userContacts.stream().map(UserContact::getUserContactId).collect(Collectors.toSet());
        findUserContactsAndInvoiceCategoriesToAdd(invoices, invoiceCategoryIds, userContacts, userContactIds);

        List<InvoiceCategory> resultInvoiceCategories = invoiceCategoryRepository.findAllById(invoiceCategoryIds);

        Set<UserContact> setForRemovingDuplicates = new HashSet<>(userContacts);
        List<UserContact> resultUserContacts = new ArrayList<>(setForRemovingDuplicates);

        List<DatabaseChange> databaseChangeBeen = new ArrayList<>();

        List<Billing> billings = billingHelperService.billingsForAppUser(currentUser);
        List<BasicDataDTO> basicDataDTOs = messagingService.createAllBillingMessagesForAppUser(currentUser, billings);

        List<Budget> budgets = budgetRepository.findAllByAppUser(currentUser);
        basicDataDTOs.addAll(messagingService.createAllBudgetMessagesForAppUser(currentUser, budgets));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(basicDataDTOs));

        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(invoices));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(basicDatas));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(businessPartners));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(resultUserContacts));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(resultInvoiceCategories));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(invoiceFailures));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(costDistributionItems));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(costDistributions));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(appUsers));

        return databaseChangeBeen;
    }

    public List<DatabaseChange> allRelevantPatchData(AppUser currentUser, Date mofifiedSinceDate){

        List<AppUser> appUsers = appUserRepository.findAll();

        List<BasicData> basicDatas = basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.STATISTIC);
        basicDatas.addAll(basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.MISTAKE_MESSAGE));
        basicDatas.addAll(basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.CHANGED_MESSAGE));
        basicDatas.addAll(basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.INTERNAL_DATA));

        List<BusinessPartner> businessPartners = businessPartnerRepository.findAllByLastModifiedAtAfter(mofifiedSinceDate);
        List<UUID> businessPartnerIds = businessPartners.stream().map(BusinessPartner::getBusinessPartnerId).collect(Collectors.toList());

        List<CostDistribution> costDistributions = costDistributionRepository.findByCreatedBy(currentUser.getAppUserId().toString());
        List<CostDistribution> costDistributionsOk = costDistributionRepository.findByCreatedByAndBasicStatusEnum(currentUser.getAppUserId().toString(), BasicStatusEnum.OK);

        List<Invoice> invoices = new ArrayList<>();
        invoices.addAll(invoiceRepository.findWithUser(currentUser.getAppUserId().toString(), mofifiedSinceDate, PageRequest.of( 0, Integer.MAX_VALUE, Sort.Direction.ASC, "dateOfInvoice" )).getContent());

        List<CostDistributionItem> tmpCostDistributionItems = costDistributionItemRepository.findByPayerAndInvoiceNotNullAndNoStandingOrder(currentUser, mofifiedSinceDate);
        findInvoicesAndBusinessPartnersToAdd(businessPartners, businessPartnerIds, invoices, tmpCostDistributionItems);
        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoiceIn(invoices);
        List<CostDistributionItem> costDistributionItems2 = costDistributionItemRepository.findByCostDistributionIn(costDistributionsOk);
        costDistributionItems.addAll(costDistributionItems2);

        List<InvoiceCategory> invoiceCategorys = invoiceCategoryRepository.findAllByAppUserIsNullOrAppUser(currentUser);
        Set<UUID> invoiceCategoryIds = invoiceCategorys.stream().map(InvoiceCategory::getInvoiceCategoryId).collect(Collectors.toSet());

        List<UserContact> userContacts = userContactRepository.findAllByAppUser(currentUser);
        List<UserContact> directUserContacts = userContactRepository.findAllByAppUserContact(currentUser);
        userContacts.addAll(directUserContacts);

        Set<UUID> userContactIds = userContacts.stream().map(UserContact::getUserContactId).collect(Collectors.toSet());
        findUserContactsToAdd(costDistributionItems, currentUser, userContacts, userContactIds);

        userContactIds = userContacts.stream().map(UserContact::getUserContactId).collect(Collectors.toSet());
        findUserContactsAndInvoiceCategoriesToAdd(invoices, invoiceCategoryIds, userContacts, userContactIds);

        Set<UserContact> setForRemovingDuplicates = new HashSet<>(userContacts);
        List<UserContact> resultUserContacts = new ArrayList<>(setForRemovingDuplicates);

        List<DatabaseChange> databaseChangeBeen = new ArrayList<>();
        List<UUID> objectIds;

        List<Billing> billings = billingHelperService.billingsForAppUser(currentUser);
        List<BasicDataDTO> basicDataDTOs = messagingService.createAllBillingMessagesForAppUser(currentUser, billings);

        List<Budget> budgets = budgetRepository.findAllByAppUser(currentUser);
        basicDataDTOs.addAll(messagingService.createAllBudgetMessagesForAppUser(currentUser, budgets));
        databaseChangeBeen.addAll(auditService.convertToDatabaseChangeDTOs(basicDataDTOs));

        objectIds = basicDatas.stream().map(BasicData::getBasicDataId).collect(Collectors.toList());
        databaseChangeBeen.addAll(auditService.findDatabaseChange(BasicData.class, objectIds, mofifiedSinceDate, basicDataRepository));

        objectIds = invoices.stream().map(Invoice::getInvoiceId).collect(Collectors.toList());
        databaseChangeBeen.addAll(auditService.findDatabaseChange(Invoice.class, objectIds, mofifiedSinceDate, invoiceRepository));

        objectIds = businessPartners.stream().map(BusinessPartner::getBusinessPartnerId).collect(Collectors.toList());
        databaseChangeBeen.addAll(auditService.findDatabaseChange(BusinessPartner.class, objectIds, mofifiedSinceDate, businessPartnerRepository));

        objectIds = resultUserContacts.stream().map(UserContact::getUserContactId).collect(Collectors.toList());
        databaseChangeBeen.addAll(auditService.findDatabaseChange(UserContact.class, objectIds, mofifiedSinceDate, userContactRepository));

        objectIds = new ArrayList<>(invoiceCategoryIds);
        databaseChangeBeen.addAll(auditService.findDatabaseChange(InvoiceCategory.class, objectIds, mofifiedSinceDate, invoiceCategoryRepository));

        objectIds = costDistributionItems.stream().map(CostDistributionItem::getCostDistributionItemId).collect(Collectors.toList());
        databaseChangeBeen.addAll(auditService.findDatabaseChange(CostDistributionItem.class, objectIds, mofifiedSinceDate, costDistributionItemRepository));

        objectIds = costDistributions.stream().map(CostDistribution::getCostDistributionId).collect(Collectors.toList());
        databaseChangeBeen.addAll(auditService.findDatabaseChange(CostDistribution.class, objectIds, mofifiedSinceDate, costDistributionRepository));

        objectIds = appUsers.stream().map(AppUser::getAppUserId).collect(Collectors.toList());
        databaseChangeBeen.addAll(auditService.findDatabaseChange(AppUser.class, objectIds, mofifiedSinceDate, appUserRepository));

        return databaseChangeBeen;
    }

    private void findUserContactsAndInvoiceCategoriesToAdd(List<Invoice> invoices, Set<UUID> invoiceCategoryIds, List<UserContact> userContacts, Set<UUID> userContactIds) {
        Set<UUID> userContactIdsToAdd = new HashSet<>();
        for (Invoice invoiceTmp : invoices) {
            if (PaymentPersonTypeEnum.CONTACT.equals(invoiceTmp.getPaymentRecipientTypeEnum())){
                if (invoiceTmp.getPaymentRecipientId() != null && !userContactIds.contains(invoiceTmp.getPaymentRecipientId())){
                    userContactIdsToAdd.add(invoiceTmp.getPaymentRecipientId());
                }
            }
            if (PaymentPersonTypeEnum.CONTACT.equals(invoiceTmp.getPayerTypeEnum())){
                if (invoiceTmp.getPayerId() != null && !userContactIds.contains(invoiceTmp.getPayerId())){
                    userContactIdsToAdd.add(invoiceTmp.getPayerId());
                }
            }
            if (invoiceTmp.getInvoiceCategory() != null){
                invoiceCategoryIds.add(invoiceTmp.getInvoiceCategory().getInvoiceCategoryId());
            }
        }
        userContacts.addAll(userContactRepository.findAllById(userContactIdsToAdd));
    }

    private void findUserContactsToAdd(List<CostDistributionItem> costDistributionItemsInput, AppUser currentUser, List<UserContact> userContacts, Set<UUID> userContactIds) {
        List<CostDistributionItem> costDistributionItems = new ArrayList<>();
        costDistributionItems.addAll(costDistributionItemsInput);
        costDistributionItems.addAll(costDistributionItemRepository.findByPayerAndInvoiceNotNullAndNoStandingOrderAndPaymentPersonTypeEnum(currentUser, PaymentPersonTypeEnum.CONTACT));

        Set<UUID> userContactIdsToAdd = new HashSet<>();
        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            if (costDistributionItem.getPayerId() != null && !userContactIds.contains(costDistributionItem.getPayerId())){
                userContactIdsToAdd.add(costDistributionItem.getPayerId());
            }
        }
        userContacts.addAll(userContactRepository.findAllById(userContactIdsToAdd));
    }

    private void findInvoicesAndBusinessPartnersToAdd(List<BusinessPartner> businessPartners, List<UUID> businessPartnerIds, List<Invoice> invoices, List<CostDistributionItem> tmpCostDistributionItems) {
        Set<UUID> businessPartnerIdsToAdd = new HashSet<>();
        List<UUID> invoiceIds = invoices.stream().map(Invoice::getInvoiceId).collect(Collectors.toList());

        for (CostDistributionItem costDistributionItem : tmpCostDistributionItems) {
            Invoice invoice = costDistributionItem.getInvoice();
            if (!invoiceIds.contains(invoice.getInvoiceId())){
                invoices.add(invoice);
            }

            UUID businessPartnerIdToAdd = null;
            if (invoice.getPayerId() != null && invoice.getPayerTypeEnum() != null && invoice.getPayerTypeEnum().equals(PaymentPersonTypeEnum.BUSINESS_PARTNER) && !businessPartnerIds.contains(invoice.getPayerId())){
                businessPartnerIdToAdd = invoice.getPayerId();
            }else if (invoice.getPaymentRecipientId() != null && invoice.getPaymentRecipientTypeEnum() != null && invoice.getPaymentRecipientTypeEnum().equals(PaymentPersonTypeEnum.BUSINESS_PARTNER) &&!businessPartnerIds.contains(invoice.getPaymentRecipientId())){
                businessPartnerIdToAdd = invoice.getPaymentRecipientId();
            }else if (costDistributionItem.getPayerId() != null && costDistributionItem.getPayerTypeEnum() != null && costDistributionItem.getPayerTypeEnum().equals(PaymentPersonTypeEnum.BUSINESS_PARTNER) &&!businessPartnerIds.contains(costDistributionItem.getPayerId())){
                businessPartnerIdToAdd = costDistributionItem.getPayerId();
            }

            if (businessPartnerIdToAdd != null){
                businessPartnerIdsToAdd.add(businessPartnerIdToAdd);
            }
        }
        if (!businessPartnerIdsToAdd.isEmpty()){
            businessPartners.addAll(businessPartnerRepository.findAllById(businessPartnerIdsToAdd));
        }
    }

}
