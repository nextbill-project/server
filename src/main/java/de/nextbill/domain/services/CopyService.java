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

import de.nextbill.domain.enums.InvoiceSource;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.model.CostDistributionItem;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.repositories.CostDistributionItemRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class CopyService {

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CostDistributionItemService costDistributionItemService;

    @Autowired
    private InvoiceHelperService invoiceHelperService;

    @Autowired
    private BudgetService budgetService;

    @Transactional
    public Invoice copyInvoiceForStandingOrder(Invoice invoiceTemplate, LocalDate newDateOfInvoice, UUID futureInvoiceId, Boolean isPaid, InvoiceSource invoiceSource){

        BeanMapper beanMapper = new BeanMapper();
        Invoice newInvoice = beanMapper.map(invoiceTemplate, Invoice.class);
        UUID invoiceId = futureInvoiceId;
        if (invoiceId == null){
            invoiceId = UUID.randomUUID();
        }
        newInvoice.setInvoiceId(invoiceId);

        newInvoice.setInvoiceImage(null);
        newInvoice.setSpecialType(invoiceTemplate.getSpecialType());
        newInvoice.setInvoiceSource(invoiceSource);
        newInvoice.setRepetitionTypeEnum(invoiceTemplate.getRepetitionTypeEnum());
        newInvoice.setInvoiceStatusEnum(InvoiceStatusEnum.CHECK);
        newInvoice.setInvoiceCategory(invoiceTemplate.getInvoiceCategory());
        if (newDateOfInvoice != null){
            newInvoice.setDateOfInvoice(Date.from(newDateOfInvoice.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        }
        if (isPaid != null && isPaid == true){
            newInvoice.setCostPaid(newInvoice.getMoneyValue());
        }else if (isPaid != null && isPaid == false){
            newInvoice.setCostPaid(new BigDecimal(0));
        }
        newInvoice.setCreatedBy(invoiceTemplate.getCreatedBy());
        invoiceHelperService.autoSetCorrectionStatus(newInvoice);

        newInvoice = invoiceRepository.save(newInvoice);
        newInvoice.setCreatedBy(invoiceTemplate.getCreatedBy());
        newInvoice = invoiceRepository.save(newInvoice);

        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findByInvoice(invoiceTemplate);
        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            BeanMapper beanMapper2 = new BeanMapper();
            CostDistributionItem newCostDistributionItem = beanMapper2.map(costDistributionItem, CostDistributionItem.class);
            newCostDistributionItem.setCostDistributionItemId(UUID.randomUUID());
            newCostDistributionItem.setBillings(null);
            newCostDistributionItem.setInvoice(newInvoice);
            costDistributionItemService.autoSetCorrectionStatus(newCostDistributionItem);

            if (isPaid != null && isPaid == true){
                newCostDistributionItem.setCostPaid(costDistributionItem.getMoneyValue());
            }else if (isPaid != null && isPaid == false){
                newCostDistributionItem.setCostPaid(new BigDecimal(0));
            }

            costDistributionItemRepository.save(newCostDistributionItem);
        }

        budgetService.updateBudgetsIfNecessary(newInvoice);

        return newInvoice;
    }

}
