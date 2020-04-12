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

import de.nextbill.domain.comparators.ReportItemComparator;
import de.nextbill.domain.enums.MainFunctionEnum;
import de.nextbill.domain.interfaces.PaymentItem;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.CostDistributionItem;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.UserContact;
import de.nextbill.domain.pojos.BillingListItem;
import de.nextbill.domain.pojos.ReportItem;
import de.nextbill.domain.repositories.UserContactRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ReportService {

    @Autowired
    private PaymentPersonService paymentPersonService;

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private InvoiceHelperService invoiceHelperService;

    public ByteArrayOutputStream createReport(List<?> beans, Map<String, Object> parameters) throws JRException, IOException {
        JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(beans);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InputStream targetStream = new ClassPathResource("billings/billingPdf.jasper").getInputStream();
        JasperPrint printFileName = JasperFillManager.fillReport(targetStream, parameters, beanColDataSource);
        if (printFileName != null) {
            JasperExportManager.exportReportToPdfStream(printFileName, outputStream);
        }

        return outputStream;
    }

    public ByteArrayOutputStream createBillingReport(BillingListItem billingListItem){

        AppUser invoicePayer = (AppUser) paymentPersonService.findPaymentPerson(billingListItem.getInvoicePayer().getPayerId(), billingListItem.getInvoicePayer().getPaymentPersonEnum());
        UserContact costPayer = (UserContact) paymentPersonService.findPaymentPerson(billingListItem.getCostPayer().getPayerId(), billingListItem.getCostPayer().getPaymentPersonEnum());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("creditorString", invoicePayer.getAppUserName());

        List<ReportItem> reportItemList = createReportItems(billingListItem.getSumTotalItemsExpense(), invoicePayer, costPayer);
        reportItemList.addAll(createReportItems(billingListItem.getSumTotalItemsIncome(), costPayer, invoicePayer, true));

        AppUser contactAsAppUser = costPayer.getAppUserContact();
        if (contactAsAppUser != null){
            parameters.put("debitorString", contactAsAppUser.getAppUserName());
            UserContact newCostPayerContact = userContactRepository.findOneByAppUserAndAppUserContact(contactAsAppUser, invoicePayer);
            if (newCostPayerContact != null){

                List<ReportItem> reportItemListReverse = createReportItems(billingListItem.getSumTotalReverseItemsExpense(), contactAsAppUser, newCostPayerContact);
                reportItemListReverse.addAll(createReportItems(billingListItem.getSumTotalReverseItemsIncome(), newCostPayerContact, contactAsAppUser, true));

                reportItemList.addAll(reportItemListReverse);
            }
        }else{
            parameters.put("debitorString", costPayer.getContactName());
        }

        BigDecimal sumTotal = billingListItem.getSumToBePaid();

        if (sumTotal.compareTo(new BigDecimal(0)) == -1){
            sumTotal = billingListItem.getSumToBePaid().multiply(new BigDecimal(-1));
            parameters.put("isReverse", false);
        }else{
            parameters.put("isReverse", true);
        }
        parameters.put("sumTotal", sumTotal);

        try {
            Collections.sort(reportItemList, new ReportItemComparator());
            outputStream = createReport(reportItemList, parameters);
        } catch (JRException | IOException e) {
            e.printStackTrace();
        }

        return outputStream;
    }

    @Transactional
    public List<ReportItem> createReportItems(List<PaymentItem> costDistributionItems, PaymentPerson invoicePayerPerson, PaymentPerson costPayerPerson) {
        return createReportItems(costDistributionItems, invoicePayerPerson, costPayerPerson, false);
    }

    @Transactional
    public List<ReportItem> createReportItems(List<PaymentItem> costDistributionItems, PaymentPerson invoicePayerPerson, PaymentPerson costPayerPerson, boolean isIncome) {

        List<ReportItem> resultReportItems = new ArrayList<>();
        String creditorDebitor = "";
        creditorDebitor = "Was " + costPayerPerson.getPaymentPersonName() + " an "+ invoicePayerPerson.getPaymentPersonName() + " zahlen muss:";


        for (PaymentItem paymentItem : costDistributionItems) {
            ReportItem reportItem = new ReportItem();
//            if (isIncome){
//                reportItem.setPartCost(costDistributionItem.getMoneyValue().multiply(new BigDecimal(-1)));
//            }else{
                reportItem.setPartCost(paymentItem.getMoneyValue());
//            }

            MainFunctionEnum mainFunctionEnum = null;
            Invoice invoice = null;
            if (paymentItem instanceof CostDistributionItem){
                reportItem.setDescription(((CostDistributionItem) paymentItem).getRemarks() != null ? ((CostDistributionItem) paymentItem).getRemarks() : "");
                invoice = ((CostDistributionItem) paymentItem).getInvoice();
            }else if (paymentItem instanceof Invoice){
                reportItem.setDescription(((Invoice) paymentItem).getRemarks() != null ? ((Invoice) paymentItem).getRemarks() : "");
                invoice = (Invoice) paymentItem;
            }
            mainFunctionEnum = invoiceHelperService.recognizeMainFunctionType(invoice);

            reportItem.setTotalCost(invoice.getSumOfInvoice());
            reportItem.setCreditorDebitor(creditorDebitor);
            reportItem.setInvoiceReceiptDate(invoice.getDateOfInvoice());

            PaymentPerson paymentPerson = null;
            if (MainFunctionEnum.EXPENSE.equals(mainFunctionEnum)){
                paymentPerson = paymentPersonService.findPaymentPerson(invoice.getPaymentRecipientId(), invoice.getPaymentRecipientTypeEnum());
            }else{
                paymentPerson = paymentPersonService.findPaymentPerson(invoice.getPayerId(), invoice.getPayerTypeEnum());
            }
            if (paymentPerson != null){
                reportItem.setPaymentRecipient(paymentPerson.getPaymentPersonName());
            }

            resultReportItems.add(reportItem);
        }

        return resultReportItems;
    }
}
