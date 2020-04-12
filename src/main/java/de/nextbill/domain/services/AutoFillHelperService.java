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

import de.nextbill.domain.enums.*;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AutoFillHelperService {

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private BasicDataRepository basicDataRepository;

    @Autowired
    private CostDistributionRepository costDistributionRepository;

    @Autowired
    private InvoiceCategoryRepository invoiceCategoryRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private InvoiceCategoryService invoiceCategoryService;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    public void generateCostDistributionItem(Invoice invoice, CostDistributionItem costDistributionItem, List<CostDistributionItem> costDistributionItems, Integer currentPosition){
        CostDistributionItem newCostDistributionItem = new CostDistributionItem();
        newCostDistributionItem.setInvoice(invoice);
        newCostDistributionItem.setCostDistributionItemId(UUID.randomUUID());
        newCostDistributionItem.setCostDistributionItemTypeEnum(costDistributionItem.getCostDistributionItemTypeEnum());
        newCostDistributionItem.setPayerId(costDistributionItem.getPayerId());
        newCostDistributionItem.setPaymentPersonTypeEnum(costDistributionItem.getPaymentPersonTypeEnum());
        newCostDistributionItem.setPosition(currentPosition);
        newCostDistributionItem.setValue(costDistributionItem.getValue());

        BigDecimal sumOfInvoice = invoice.getSumOfInvoice();
        if (sumOfInvoice == null){
            sumOfInvoice = new BigDecimal(0);
        }
        BigDecimal moneyValue = CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(costDistributionItem, costDistributionItems, sumOfInvoice);
        newCostDistributionItem.setMoneyValue(moneyValue);
        costDistributionItemRepository.save(newCostDistributionItem);
    }

    public void generateDefaultCostDistributionItem(Invoice invoice){
        generateDefaultCostDistributionItem(invoice, null);
    }

    public void generateDefaultCostDistributionItem(Invoice invoice, PaymentPerson paymentPerson){
        CostDistributionItem costDistributionItem = new CostDistributionItem();
        costDistributionItem.setInvoice(invoice);
        costDistributionItem.setCostDistributionItemId(UUID.randomUUID());
        costDistributionItem.setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum.QUOTA);

        if (paymentPerson != null){
            costDistributionItem.setPayerId(paymentPerson.getPaymentPersonId());
            costDistributionItem.setPaymentPersonTypeEnum(paymentPerson.getPaymentPersonEnum());
        }else{
            costDistributionItem.setPayerId(invoice.getPayerId());
            costDistributionItem.setPaymentPersonTypeEnum(invoice.getPayerTypeEnum());
        }

        costDistributionItem.setPosition(0);
        costDistributionItem.setValue(new BigDecimal(1));
        BigDecimal sumOfInvoice = invoice.getSumOfInvoice();
        if (sumOfInvoice == null){
            sumOfInvoice = new BigDecimal(0);
        }
        costDistributionItem.setMoneyValue(sumOfInvoice);
        costDistributionItemRepository.save(costDistributionItem);
    }

    public List<CostDistributionItem> findBestCostDistributionItems(Invoice invoice, AppUser appUser){
        List<CostDistributionItem> resultCostDistributionitems = new ArrayList<>();

        Integer highestCount = 0;
        CostDistribution resultCostDistribution = null;

        List<Invoice> relevantInvoices = invoiceRepository.findByPaymentRecipientIdAndPaymentRecipientTypeEnumAndInvoiceStatusEnumAndCreatedBy(invoice.getPaymentRecipientId(),invoice.getPaymentRecipientTypeEnum(),InvoiceStatusEnum.READY,appUser);
        List<CostDistribution> relevantCostDistributions = costDistributionRepository.findByCreatedByAndBasicStatusEnum(appUser.getAppUserId().toString(), BasicStatusEnum.OK);

        Map<CostDistribution, Integer> ranking = rankingOfCostDistributionsInFilteredInvoices(relevantCostDistributions, relevantInvoices);
        Set<CostDistribution> costDistributions = ranking.keySet();
        for (CostDistribution costDistribution : costDistributions) {

            Integer currentCount = ranking.get(costDistribution);
            if (currentCount > highestCount){
                highestCount = currentCount;
                resultCostDistribution = costDistribution;
            }

        }

        if (highestCount >= 2){
            List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByCostDistribution(resultCostDistribution);
            return costDistributionItems;
        }

        return resultCostDistributionitems;
    }

    public Map<CostDistribution, Integer> rankingOfCostDistributionsInFilteredInvoices(List<CostDistribution> costDistributions, List<Invoice> filteredInvoices){

        Map<CostDistribution, Integer> ranking = new HashMap<CostDistribution, Integer>();

        for (CostDistribution costDistribution : costDistributions) {
            List<CostDistributionItem> costDistributionItemsForCostDistribution = costDistributionItemRepository.findByCostDistribution(costDistribution);

            Integer count = 0;
            for (Invoice invoice : filteredInvoices) {
                List<CostDistributionItem> costDistributionItemsForInvoice = costDistributionItemRepository.findByInvoice(invoice);

                if (areCostDistributionItemListsEqual(costDistributionItemsForCostDistribution, costDistributionItemsForInvoice)){
                    count++;
                }
            }

            ranking.put(costDistribution, count);

        }

        return ranking;
    }

    public boolean areCostDistributionItemListsEqual(List<CostDistributionItem> costDistributionItems1, List<CostDistributionItem> costDistributionItems2){

        for (CostDistributionItem costDistributionItem1 : costDistributionItems1) {

            boolean equalCostDistributionItemFound = false;
            for (CostDistributionItem costDistributionItem2 : costDistributionItems2) {
                if (areCostDistributionItemsEqual(costDistributionItem1, costDistributionItem2)){
                    equalCostDistributionItemFound = true;
                    break;
                }
            }

            if (!equalCostDistributionItemFound){
                return false;
            }

        }

        return true;
    }

    public boolean areCostDistributionItemsEqual(CostDistributionItem costDistributionItem1, CostDistributionItem costDistributionItem2){
        if (costDistributionItem1.getCostDistributionItemTypeEnum() == null && costDistributionItem2.getCostDistributionItemTypeEnum() != null){
            return false;
        }else if (costDistributionItem1.getCostDistributionItemTypeEnum() != null && costDistributionItem2.getCostDistributionItemTypeEnum() == null){
            return false;
        }else if (costDistributionItem1.getCostDistributionItemTypeEnum() != null && costDistributionItem2.getCostDistributionItemTypeEnum() != null && !costDistributionItem1.getCostDistributionItemTypeEnum().equals(costDistributionItem2.getCostDistributionItemTypeEnum())){
            return false;
        }

        if (costDistributionItem1.getPaymentPersonTypeEnum() == null && costDistributionItem2.getPaymentPersonTypeEnum() != null){
            return false;
        }else if (costDistributionItem1.getPaymentPersonTypeEnum() != null && costDistributionItem2.getPaymentPersonTypeEnum() == null){
            return false;
        }else if (costDistributionItem1.getPaymentPersonTypeEnum() != null && costDistributionItem2.getPaymentPersonTypeEnum() != null && !costDistributionItem1.getPaymentPersonTypeEnum().equals(costDistributionItem2.getPaymentPersonTypeEnum())){
            return false;
        }

        if (costDistributionItem1.getPayerId() == null && costDistributionItem2.getPayerId() != null){
            return false;
        }else if (costDistributionItem1.getPayerId() != null && costDistributionItem2.getPayerId() == null){
            return false;
        }else if (costDistributionItem1.getPayerId() != null && costDistributionItem2.getPayerId() != null && !costDistributionItem1.getPayerId().equals(costDistributionItem2.getPayerId())){
            return false;
        }

        if (costDistributionItem1.getValue() == null && costDistributionItem2.getValue() != null){
            return false;
        }else if (costDistributionItem1.getValue() != null && costDistributionItem2.getValue() == null){
            return false;
        }else if (costDistributionItem1.getValue() != null && costDistributionItem2.getValue() != null && !costDistributionItem1.getValue().equals(costDistributionItem2.getValue())){
            return false;
        }

        return true;
    }

    public InvoiceCategory findCategoryForBusinessPartner(AppUser currentUser, BusinessPartner businessPartner, Invoice invoice, String locationName, boolean useGoogleSearch){

        if (currentUser == null){

            return null;
        }
        BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(currentUser, BasicDataType.STATISTIC, BasicDataSubType.CATEGORY_FOR_BUSINESS_PARTNER,BusinessPartner.class.getSimpleName(), businessPartner.getBusinessPartnerId().toString() );

        if (basicData != null){
            InvoiceCategory invoiceCategory = invoiceCategoryRepository.findById(UUID.fromString(basicData.getObject2Id())).orElse(null);

            if (invoiceCategory != null){
                return invoiceCategory;
            }
        }

        InvoiceCategory invoiceCategoryBusinessPartner = businessPartner.getInvoiceCategory();
        if (invoiceCategoryBusinessPartner != null){
            return invoiceCategoryBusinessPartner;
        }

        InvoiceCategory invoiceCategory = invoiceCategoryService.requestExternalServicesForInvoiceCategory(businessPartner, invoice);

        if (invoiceCategory != null){
            businessPartner.setInvoiceCategory(invoiceCategory);
            businessPartnerRepository.save(businessPartner);
        }

        return invoiceCategory;
    }

    public void refreshMoneyValues(Invoice invoice){
        List<CostDistributionItem> newCostDistributionItems = costDistributionItemRepository.findByInvoice(invoice);
        for (CostDistributionItem newCostDistributionItem : newCostDistributionItems) {
            BigDecimal moneyValue = CostDistributionHelper.calculateAmountForCostDistributionItemPrecise(newCostDistributionItem, newCostDistributionItems, invoice.getSumOfInvoice());
            newCostDistributionItem.setMoneyValue(moneyValue);
            costDistributionItemRepository.save(newCostDistributionItem);
        }
    }

    public CostDistributionItem prepareItemForCostDistribution(CostDistributionItem costDistributionItem, CostDistribution costDistribution){
        CostDistributionItem costDistributionItemNew = new CostDistributionItem();
        costDistributionItemNew.setCostDistributionItemId(UUID.randomUUID());
        costDistributionItemNew.setPaymentPersonTypeEnum(costDistributionItem.getPaymentPersonTypeEnum());
        costDistributionItemNew.setValue(costDistributionItem.getValue());
        costDistributionItemNew.setPayerId(costDistributionItem.getPayerId());
        if (costDistribution != null){
            costDistributionItemNew.setCostDistribution(costDistribution);
        }
        costDistributionItemNew.setCostDistributionItemTypeEnum(costDistributionItem.getCostDistributionItemTypeEnum());
        costDistributionItemNew.setPosition(costDistributionItem.getPosition());

        return costDistributionItemNew;
    }

    public Boolean findSpecialTypeForCategory(AppUser currentUser, InvoiceCategory invoiceCategory){
        if (currentUser == null){
            return null;
        }

        BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(currentUser, BasicDataType.STATISTIC, BasicDataSubType.SPECIAL_TYPE_FOR_CATEGORY, InvoiceCategory.class.getSimpleName(), invoiceCategory.getInvoiceCategoryId().toString());

        if (basicData != null){
            String value = basicData.getObject2Id();
            Boolean booleanValue = Boolean.valueOf(value);
            return booleanValue;
        }

        return null;
    }

    public InvoiceCategory findCategoryForBusinessPartner(PaymentPerson businessPartner){
        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));
        if (currentUser == null){
            return null;
        }

        BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(currentUser, BasicDataType.STATISTIC, BasicDataSubType.CATEGORY_FOR_BUSINESS_PARTNER, businessPartner.getClass().getSimpleName(), businessPartner.getPaymentPersonId().toString());

        if (basicData != null){
            InvoiceCategory invoiceCategory = invoiceCategoryRepository.findById(UUID.fromString(basicData.getObject2Id())).orElse(null);
            return invoiceCategory;
        }

        return null;
    }

    public RepetitionTypeEnum findRepetitionTypeForCategory(AppUser currentUser, InvoiceCategory invoiceCategory){
        if (currentUser == null){
            return null;
        }

        BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(currentUser, BasicDataType.STATISTIC, BasicDataSubType.REPETITION_TYPE_FOR_CATEGORY, InvoiceCategory.class.getSimpleName(), invoiceCategory.getInvoiceCategoryId().toString());

        if (basicData != null){
            RepetitionTypeEnum value = RepetitionTypeEnum.valueOf(basicData.getObject2Id());
            return value;
        }

        return null;
    }

    public CostDistribution findCostDistributionForCategory(AppUser currentUser, InvoiceCategory invoiceCategory){
        if (currentUser == null){
            return null;
        }

        BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(currentUser, BasicDataType.STATISTIC,BasicDataSubType.COST_DISTRIBUTION_FOR_CATEGORY, InvoiceCategory.class.getSimpleName(), invoiceCategory.getInvoiceCategoryId().toString());

        if (basicData != null){

            CostDistribution costDistribution = costDistributionRepository.findById(UUID.fromString(basicData.getObject2Id())).orElse(null);
            return costDistribution;
        }

        return null;
    }

    public CostDistribution generateCostDistributionForCategory(Invoice invoice, AppUser currentUser, InvoiceCategory invoiceCategory){

        if (currentUser == null){
            return null;
        }

        List<CostDistributionItem> newCostDistributionItems = costDistributionItemRepository.findByInvoice(invoice);
        costDistributionItemRepository.deleteAll(newCostDistributionItems);
        invoice.setLastModifiedAt(new Date());
        invoice.setCostDistributionItems(new ArrayList());
        invoiceRepository.save(invoice);

        BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(currentUser, BasicDataType.STATISTIC,BasicDataSubType.COST_DISTRIBUTION_FOR_CATEGORY, InvoiceCategory.class.getSimpleName(), invoiceCategory.getInvoiceCategoryId().toString());

        if (basicData != null){
            CostDistribution costDistribution = costDistributionRepository.findById(UUID.fromString(basicData.getObject2Id())).orElse(null);

            if (costDistribution != null){

                List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByCostDistribution(costDistribution);

                Integer currentPosition = 0;
                for (CostDistributionItem costDistributionItem : costDistributionItems) {
                    generateCostDistributionItem(invoice, costDistributionItem, costDistributionItems, currentPosition);
                    currentPosition++;
                }


                return costDistribution;
            }else{
                generateDefaultCostDistributionItem(invoice);
            }
        }else{
            generateDefaultCostDistributionItem(invoice);
        }

        return null;
    }
}
