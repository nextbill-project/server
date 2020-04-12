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
import de.nextbill.domain.enums.RepetitionTypeEnum;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.CostDistributionItem;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.pojos.InvoiceCostDistributionItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportService {

    @Autowired
    private PaymentPersonService paymentPersonService;

    public ByteArrayOutputStream exportToCsv(List<InvoiceCostDistributionItem> invoiceCostDistributionItems) throws IOException {

        List<String> csvLines = new ArrayList<>();

        List<String> titleValues = new ArrayList<>();
        titleValues.add("Zahlungsempfänger");
        titleValues.add("Geldgeber");
        titleValues.add("Gesamtsumme");
        titleValues.add("Anteil");
        titleValues.add("Rechnungsdatum");
        titleValues.add("Kategorie");
        titleValues.add("Sonderfall");
        titleValues.add("Herkunft");
        titleValues.add("Wiederholungs-Typ");
        titleValues.add("Beschreibung");

        csvLines.add(StringUtils.join(titleValues, ","));

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
            List<String> resultValues = new ArrayList<>();

            Invoice invoice = invoiceCostDistributionItem.getInvoice();
            CostDistributionItem costDistributionItem = invoiceCostDistributionItem.getCostDistributionItem();

            PaymentPerson paymentRecipient = paymentPersonService.findPaymentPerson(invoice.getPaymentRecipientId(), invoice.getPaymentRecipientTypeEnum());
            PaymentPerson payer = paymentPersonService.findPaymentPerson(invoice.getPayerId(), invoice.getPayerTypeEnum());

            String paymentRecipientName = paymentRecipient != null ? paymentRecipient.getPaymentPersonName() : "";
            resultValues.add(paymentRecipientName);

            String payerName = payer != null ? payer.getPaymentPersonName() : "";
            resultValues.add(payerName);

            resultValues.add(invoice.getSumOfInvoice().setScale(2, RoundingMode.HALF_EVEN).toString());
            resultValues.add(costDistributionItem.getMoneyValue().setScale(2, RoundingMode.HALF_EVEN).toString());

            resultValues.add(sdf.format(invoice.getDateOfInvoice()));

            String invoiceCategoryName = invoice.getInvoiceCategory() != null ? invoice.getInvoiceCategory().getInvoiceCategoryName() : "";
            resultValues.add(invoiceCategoryName);

            String isSpecialType = invoice.getSpecialType() != null ? invoice.getSpecialType().toString() : "false";
            resultValues.add(isSpecialType);

            String invoiceSourceName = invoice.getInvoiceSource() != null ? invoice.getInvoiceSource().name() : InvoiceSource.MANUAL.name();
            resultValues.add(invoiceSourceName);

            String repetitionTypeName = invoice.getRepetitionTypeEnum() != null ? invoice.getRepetitionTypeEnum().name() : RepetitionTypeEnum.ONCE.name();
            resultValues.add(repetitionTypeName);

            resultValues.add((invoice.getRemarks() != null ? invoice.getRemarks() + " " : "") + (costDistributionItem.getRemarks() != null ? costDistributionItem.getRemarks() : ""));

            csvLines.add(StringUtils.join(resultValues, ","));
        }

        String resultString = StringUtils.join(csvLines, "\n");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8));
        out.write(resultString);
        out.close();

        return byteArrayOutputStream;

    }
}
