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
import de.nextbill.domain.enums.CorrectionStatus;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.interfaces.PaymentItem;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.CostDistributionItem;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.repositories.CostDistributionItemRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.utils.views.MappingView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentItemService {

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceHelperService invoiceHelperService;

    public void savePaymentItem(PaymentItem paymentItem){
        if (paymentItem instanceof CostDistributionItem){
            costDistributionItemRepository.save((CostDistributionItem) paymentItem);
        }else if (paymentItem instanceof Invoice){
            invoiceRepository.save((Invoice) paymentItem);
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

    public List<InvoiceDTO> invoiceDTOsForPaymentItems(List<PaymentItem> paymentItems){
        List<InvoiceDTO> invoiceDTOS = new ArrayList<>();
        List<UUID> invoiceIds = new ArrayList<>();

        for (PaymentItem paymentItem : paymentItems) {

            if (paymentItem instanceof CostDistributionItem){
                CostDistributionItem costDistributionItem = (CostDistributionItem) paymentItem;
                Invoice invoice = costDistributionItem.getInvoice();

                generateDtoWithDebtValue(invoiceDTOS, invoiceIds, paymentItem, invoice);
            }else if (paymentItem instanceof Invoice){
                Invoice invoice = (Invoice) paymentItem;

                generateDtoWithDebtValue(invoiceDTOS, invoiceIds, paymentItem, invoice);
            }
        }

        return invoiceDTOS;
    }

    private void generateDtoWithDebtValue(List<InvoiceDTO> invoiceDTOs, List<UUID> invoiceIds, PaymentItem paymentItem, Invoice invoice) {
        if (invoice != null){
            if (!invoiceIds.contains(invoice.getInvoiceId())){
                invoiceIds.add(invoice.getInvoiceId());

                InvoiceDTO invoiceDTO = invoiceHelperService.mapToDTO(invoice, false, MappingView.Summary.class);

                BigDecimal costPaid = paymentItem.getCostPaid() != null ? paymentItem.getCostPaid() : new BigDecimal(0);
                BigDecimal moneyValue = paymentItem.getMoneyValue() != null ? paymentItem.getMoneyValue() : new BigDecimal(0);

                invoiceDTO.setDebtValue(moneyValue);
                invoiceDTO.setRestDebtValue(moneyValue.subtract(costPaid));

                invoiceDTOs.add(invoiceDTO);
            }else{
                Optional<InvoiceDTO> invoiceDTO = invoiceDTOs.stream().filter(t-> t.getInvoiceId().equals(invoice.getInvoiceId())).findAny();

                if (invoiceDTO.isPresent()){
                    BigDecimal currentValue = invoiceDTO.get().getDebtValue();

                    BigDecimal costPaid = paymentItem.getCostPaid() != null ? paymentItem.getCostPaid() : new BigDecimal(0);
                    BigDecimal moneyValue = paymentItem.getMoneyValue() != null ? paymentItem.getMoneyValue() : new BigDecimal(0);

                    invoiceDTO.get().setDebtValue(currentValue.add(moneyValue));
                    invoiceDTO.get().setRestDebtValue(currentValue.add(moneyValue.subtract(costPaid)));
                }
            }
        }
    }

}
