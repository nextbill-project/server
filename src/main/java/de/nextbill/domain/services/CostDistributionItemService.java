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
import de.nextbill.domain.dtos.CostDistributionItemDTO;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.repositories.CostDistributionItemRepository;
import de.nextbill.domain.repositories.CostDistributionRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.utils.BeanMapper;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class CostDistributionItemService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CostDistributionRepository costDistributionRepository;

    @Autowired
    private PaymentPersonService paymentPersonService;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private PaymentItemService paymentItemService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private MessagingService messagingService;

    public CostDistributionItemDTO mapToDTO(CostDistributionItem costDistributionItem){
        return mapToDTO(costDistributionItem, null);
    }

    public CostDistributionItemDTO mapToDTO(CostDistributionItem costDistributionItem, Boolean mobileView){
        BeanMapper beanMapper = new BeanMapper();
        CostDistributionItemDTO costDistributionItemDTO = beanMapper.map(costDistributionItem, CostDistributionItemDTO.class);

        Invoice invoice = costDistributionItem.getInvoice();
        if (invoice != null){
            costDistributionItemDTO.setInvoiceId(invoice.getInvoiceId());
        }

        CostDistribution costDistribution = costDistributionItem.getCostDistribution();
        if (costDistribution != null){
            costDistributionItemDTO.setCostDistributionId(costDistribution.getCostDistributionId());
        }

        if (costDistributionItem.getPayerId() != null){
            if (mobileView == null || mobileView == false){
                costDistributionItemDTO.setPayerDTO(paymentPersonService.findPaymentPersonAndGetDTO(costDistributionItem.getPayerId(), costDistributionItem.getPaymentPersonTypeEnum()));
            }
        }

        if (costDistributionItem.getArticleDTOsAsJson() != null){
            costDistributionItemDTO.setArticleDTOs(convertToArticleDTOs(costDistributionItem.getArticleDTOsAsJson()));
        }

        return costDistributionItemDTO;
    }

    public CostDistributionItem mapToEntity(CostDistributionItemDTO costDistributionItemDTO){

        CostDistributionItem costDistributionItem = null;

        BeanMapper beanMapper = new BeanMapper();
        costDistributionItem= beanMapper.map(costDistributionItemDTO, CostDistributionItem.class);

        if (costDistributionItemDTO.getInvoiceId() != null){
            costDistributionItem.setInvoice(invoiceRepository.findById(costDistributionItemDTO.getInvoiceId()).orElse(null));
        }else{
            costDistributionItem.setInvoice(null);
        }

        if (costDistributionItemDTO.getCostDistributionId() != null){
            costDistributionItem.setCostDistribution(costDistributionRepository.findById(costDistributionItemDTO.getCostDistributionId()).orElse(null));
        }else{
            costDistributionItem.setCostDistribution(null);
        }

        if (costDistributionItemDTO.getArticleDTOs() != null){
            costDistributionItem.setArticleDTOsAsJson(convertArticleDTOsToJson(costDistributionItemDTO.getArticleDTOs()));
        }else{
            costDistributionItem.setArticleDTOsAsJson(null);
        }

        CostDistributionItem oldCostDistributionItem = costDistributionItemRepository.findById(costDistributionItemDTO.getCostDistributionItemId()).orElse(null);
        if (oldCostDistributionItem != null){
            costDistributionItem.setBillings(oldCostDistributionItem.getBillings());
        }

        return costDistributionItem;
    }

    public List<CostDistributionItem> updateCostDistributionItemsOfInvoice(Invoice invoice, List<CostDistributionItemDTO> costDistributionItemDTOs){

        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(invoice);
        List<CostDistributionItem> costDistributionItemsAdded = new ArrayList<>();

        List<BigDecimal> costPaidValues = new ArrayList<>();

        for (CostDistributionItemDTO costDistributionItemDTO : costDistributionItemDTOs) {
            CostDistributionItem costDistributionItem = mapToEntity(costDistributionItemDTO);
            if (costDistributionItem.getCostPaid() == null){
                costDistributionItem.setCostPaid(new BigDecimal(0));
            }

            CostDistributionItem costDistributionItemDb = null;
            if (costDistributionItemDTO.getCostDistributionItemId() != null){
                costDistributionItemDb = costDistributionItemRepository.findById(costDistributionItemDTO.getCostDistributionItemId()).orElse(null);
            }
            BigDecimal costPaid = costDistributionItemDb != null && costDistributionItemDb.getCostPaid() != null ? costDistributionItemDb.getCostPaid() : new BigDecimal(0);
            costPaidValues.add(costPaid);

            PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(costDistributionItem.getCostPayerId(), costDistributionItem.getPaymentPersonTypeEnum());
            paymentItemService.recognizeAndSetCorrectionStatus(costDistributionItem, paymentPerson);

            CostDistributionItem costDistributionItemSaved = costDistributionItemRepository.save(costDistributionItem);

            costDistributionItemsAdded.add(costDistributionItemSaved);
        }

        List<CostDistributionItem> costDistributionItemsToDelete = new ArrayList<>();
        costDistributionItemsToDelete.addAll(costDistributionItems);
        costDistributionItemsToDelete.removeAll(costDistributionItemsAdded);


        costDistributionItemRepository.deleteAll(costDistributionItemsToDelete);
        invoice.setLastModifiedAt(new Date());
        invoiceRepository.save(invoice);

        for (CostDistributionItem costDistributionItem : costDistributionItemsToDelete) {
            PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(costDistributionItem.getCostPayerId(), costDistributionItem.getPaymentPersonTypeEnum());
            AppUser appUser = null;
            if (paymentPerson instanceof AppUser) {
                appUser = (AppUser) paymentPerson;
            }else if (paymentPerson instanceof UserContact) {
                appUser = ((UserContact) paymentPerson).getAppUserContact();
            }
            if (appUser != null && !appUser.getAppUserId().toString().equals(invoice.getCreatedBy())) {
                messagingService.createInternalDataInvoiceDeletedMessage(appUser, invoice);
            }
        }

        Integer counter = 0;
        for (CostDistributionItem costDistributionItem : costDistributionItemsAdded) {
            billingService.refreshBillingsIfNecessaryAsync(costDistributionItem, costDistributionItemDTOs.get(counter).getCostPaid(), costPaidValues.get(counter).doubleValue());
            counter++;
        }

        return costDistributionItemsAdded;
    }

    public void autoSetCorrectionStatus(CostDistributionItem costDistributionItem) {
        PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(costDistributionItem.getCostPayerId(), costDistributionItem.getPaymentPersonTypeEnum());
        paymentItemService.recognizeAndSetCorrectionStatus(costDistributionItem, paymentPerson);
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
