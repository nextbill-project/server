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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.nextbill.domain.dtos.BasicDataDTO;
import de.nextbill.domain.dtos.MessageDTO;
import de.nextbill.domain.enums.*;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.*;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.BasicDataRepository;
import de.nextbill.domain.repositories.BudgetRepository;
import de.nextbill.domain.repositories.UserContactRepository;
import de.nextbill.domain.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessagingService {

    @Autowired
    private PaymentPersonService paymentPersonService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private BasicDataRepository basicDataRepository;

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private BillingHelperService billingHelperService;

    @Autowired
    private BudgetRepository budgetRepository;

    public String createMessageDTOAsJson(String subject, String message, MessageType messageType){
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setMessage(message);
        messageDTO.setSubject(subject);
        messageDTO.setMessageType(messageType);
        Gson gson = new GsonBuilder().setDateFormat("dd MMM yyyy HH:mm:ss").create();
        String jsonString = gson.toJson(messageDTO);

        return jsonString;
    }

    public MessageDTO createMessageDTOFromJson(String jsonString){
        Type listType = new TypeToken<MessageDTO>() {}.getType();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        MessageDTO messageDTO = gson.fromJson(jsonString, listType);
        return messageDTO;
    }

    public BasicData createCheckReminderMessage(AppUser appUser, AppUser creatorUser){
        BasicData basicData= new BasicData();
        basicData.setBasicDataId(UUID.randomUUID());
        basicData.setAppUser(appUser);
        basicData.setBasicDataType(BasicDataType.BILLING_MESSAGE);
        basicData.setObject1Class(AppUser.class.getSimpleName());
        basicData.setObject1Id(creatorUser.getAppUserId().toString());

        basicData.setNumberValue(new BigDecimal((new Date()).getTime()));

        String messageDto = createMessageDTOAsJson("Rechnungen prüfen",  creatorUser.getPaymentPersonName() + " fordert Sie auf ausstehende Rechnungen zu prüfen, damit eine Abrechnung erstellt werden kann.", MessageType.BILLING);
        basicData.setValue(messageDto);

        basicDataRepository.save(basicData);

        return basicData;
    }

    public BasicData createBillingAbortedAutomaticallyMessage(AppUser appUser, AppUser creatorUser){
        BasicData basicData= new BasicData();
        basicData.setBasicDataId(UUID.randomUUID());
        basicData.setAppUser(appUser);
        basicData.setBasicDataType(BasicDataType.BILLING_MESSAGE);
        basicData.setObject1Class(AppUser.class.getSimpleName());
        basicData.setObject1Id(creatorUser.getAppUserId().toString());

        basicData.setNumberValue(new BigDecimal((new Date()).getTime()));

        String messageDto = createMessageDTOAsJson("Abrechnung abgebrochen",  "Durch eine Änderung in einer Abrechnung mit Benutzer " + creatorUser.getPaymentPersonName() + " ist der Gesamtbetrag 0 oder negativ geworden, die Abrechnung wurde daher automatisch storniert.", MessageType.BILLING);
        basicData.setValue(messageDto);

        basicDataRepository.save(basicData);

        return basicData;
    }

    public BasicDataDTO createOpenBillingMessage(AppUser appUser, Billing billing){
        BasicDataDTO basicDataDTO = new BasicDataDTO();
        basicDataDTO.setBasicDataId(UUID.randomUUID());
        basicDataDTO.setAppUserId(appUser.getAppUserId());
        basicDataDTO.setBasicDataType(BasicDataType.MESSAGE);
        basicDataDTO.setBasicDataSubType(BasicDataSubType.WAIT_FOR_PAYMENT);
        basicDataDTO.setObject1Class(Billing.class.getSimpleName());
        basicDataDTO.setObject1Id(billing.getBillingId().toString());

        basicDataDTO.setNumberValue(new BigDecimal((new Date()).getTime()));
        String costToPayString = bigDecimalToString(billing.getSumToPay());

        PaymentPerson payer = null;
        if (billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true){
            payer = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());
        }else{
            payer = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        }

        String messageDto = createMessageDTOAsJson("Warten auf Bezahlung",  payer.getPaymentPersonName() + " hat eine Abrechnung zu bezahlen. Der Betrag ist "+ costToPayString + " €", MessageType.WAIT_FOR_PAYMENT);
        basicDataDTO.setValue(messageDto);

        return basicDataDTO;
    }

    public BasicDataDTO createSendMoneyMessage(AppUser appUser, Billing billing){
        BasicDataDTO basicDataDTO = new BasicDataDTO();
        basicDataDTO.setBasicDataId(UUID.randomUUID());
        basicDataDTO.setAppUserId(appUser.getAppUserId());
        basicDataDTO.setBasicDataType(BasicDataType.MESSAGE);
        basicDataDTO.setBasicDataSubType(BasicDataSubType.TO_PAY);
        basicDataDTO.setObject1Class(Billing.class.getSimpleName());
        basicDataDTO.setObject1Id(billing.getBillingId().toString());

        basicDataDTO.setNumberValue(new BigDecimal((new Date()).getTime()));
        String costToPayString = bigDecimalToString(billing.getSumToPay());

        PaymentPerson payer = null;
        if (billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true){
            payer = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        }else{
            payer = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());
        }

        String messageDto = createMessageDTOAsJson("Offene Abrechnung", "Sie haben mit " + payer.getPaymentPersonName() + " noch eine nicht beglichene Abrechnung. Bitte bezahlen Sie den Betrag von "+ costToPayString + " €", MessageType.TO_PAY);
        basicDataDTO.setValue(messageDto);

        return basicDataDTO;
    }

    public BasicDataDTO createMoneyReceivedMessage(AppUser appUser, Billing billing){
        BasicDataDTO basicDataDTO = new BasicDataDTO();
        basicDataDTO.setBasicDataId(UUID.randomUUID());
        basicDataDTO.setAppUserId(appUser.getAppUserId());
        basicDataDTO.setBasicDataType(BasicDataType.MESSAGE);
        basicDataDTO.setObject1Class(Billing.class.getSimpleName());
        basicDataDTO.setObject1Id(billing.getBillingId().toString());

        basicDataDTO.setNumberValue(new BigDecimal((new Date()).getTime()));
        String costToPayString = bigDecimalToString(billing.getSumToPay());

        PaymentPerson payer = null;
        if (billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true){
            payer = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());
        }else{
            payer = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        }

        String messageDto = createMessageDTOAsJson("Abrechnung bezahlt", "Der Benutzer " + payer.getPaymentPersonName() + " hat eine Abrechnung mit \"Zahlung verschickt\" markiert. Bitte bestätigen Sie den Erhalt der Zahlung von "+ costToPayString + " €", MessageType.PAID);
        basicDataDTO.setValue(messageDto);

        return basicDataDTO;
    }

    public BasicDataDTO createPaymentConfirmedMessage(AppUser appUser, Billing billing){
        BasicDataDTO basicDataDTO = new BasicDataDTO();
        basicDataDTO.setBasicDataId(UUID.randomUUID());
        basicDataDTO.setAppUserId(appUser.getAppUserId());
        basicDataDTO.setBasicDataType(BasicDataType.MESSAGE);
        basicDataDTO.setObject1Class(Billing.class.getSimpleName());
        basicDataDTO.setObject1Id(billing.getBillingId().toString());

        PaymentPerson paymentReceiver = null;
        if (billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true){
            paymentReceiver = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        }else{
            paymentReceiver = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());
        }

        basicDataDTO.setNumberValue(new BigDecimal((new Date()).getTime()));
        String costToPayString = bigDecimalToString(billing.getSumToPay());

        String messageDto = createMessageDTOAsJson("Zahlung bestätigt", "Der Benutzer " + paymentReceiver.getPaymentPersonName() + " hat eine Rechnung mit \"Zahlung bestätigt\" markiert. Der Betrag war "+ costToPayString + " €", MessageType.PAYMENT_CONFIRMED);
        basicDataDTO.setValue(messageDto);

        return basicDataDTO;
    }

    public BasicDataDTO createPaymentCompletedMessage(AppUser appUser, Billing billing){
        BasicDataDTO basicDataDTO = new BasicDataDTO();
        basicDataDTO.setBasicDataId(UUID.randomUUID());
        basicDataDTO.setAppUserId(appUser.getAppUserId());
        basicDataDTO.setBasicDataType(BasicDataType.MESSAGE);
        basicDataDTO.setObject1Class(Billing.class.getSimpleName());
        basicDataDTO.setObject1Id(billing.getBillingId().toString());

        basicDataDTO.setNumberValue(new BigDecimal((new Date()).getTime()));
        String costToPayString = bigDecimalToString(billing.getSumToPay());

        PaymentPerson paymentReceiver = null;
        if (appUser.getAppUserId().toString().equals(billing.getCreatedBy())) {
            if (billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true){
                paymentReceiver = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());
            }else{
                paymentReceiver = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
            }
        } else {
            if (billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true){
                paymentReceiver = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
            }else{
                paymentReceiver = paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());
            }
        }

        String messageDto = createMessageDTOAsJson("Abgeschlossen", "Die Abrechnung mit " + paymentReceiver.getPaymentPersonName() + " wurde vollständig beglichen. Der Betrag war "+ costToPayString + " €", MessageType.FINISHED);
        basicDataDTO.setValue(messageDto);

        return basicDataDTO;
    }

    public BasicDataDTO createPaymentArchivedMessage(AppUser appUser, Billing billing){
        BasicDataDTO basicDataDTO = new BasicDataDTO();
        basicDataDTO.setBasicDataId(UUID.randomUUID());
        basicDataDTO.setAppUserId(appUser.getAppUserId());
        basicDataDTO.setBasicDataType(BasicDataType.MESSAGE);
        basicDataDTO.setObject1Class(Billing.class.getSimpleName());
        basicDataDTO.setObject1Id(billing.getBillingId().toString());

        basicDataDTO.setNumberValue(new BigDecimal((new Date()).getTime()));
        String costToPayString = bigDecimalToString(billing.getSumToPay());

        String messageDto = createMessageDTOAsJson("Archiviert", "Die Abrechnung wurde archiviert. Der Betrag war "+ costToPayString + " €", MessageType.ARCHIVED);
        basicDataDTO.setValue(messageDto);

        return basicDataDTO;
    }

    public BasicDataDTO createPaidCompletedMessage(AppUser appUser, Billing billing){
        BasicDataDTO basicDataDTO = new BasicDataDTO();
        basicDataDTO.setBasicDataId(UUID.randomUUID());
        basicDataDTO.setAppUserId(appUser.getAppUserId());
        basicDataDTO.setBasicDataType(BasicDataType.MESSAGE);
        basicDataDTO.setObject1Class(Billing.class.getSimpleName());
        basicDataDTO.setObject1Id(billing.getBillingId().toString());

        basicDataDTO.setNumberValue(new BigDecimal((new Date()).getTime()));
        String costToPayString = bigDecimalToString(billing.getSumToPay());

        String messageDto = createMessageDTOAsJson("Zahlung getätigt", "Sie haben die Abrechnung beglichen. Der Betrag war "+ costToPayString + " €", MessageType.PAID_AND_WAIT);
        basicDataDTO.setValue(messageDto);

        return basicDataDTO;
    }

    public BasicData createMistakeMessage(String message, Invoice invoice, AppUser appUserFrom){
        BasicData basicData = new BasicData();
        basicData.setBasicDataId(UUID.randomUUID());

        AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(invoice.getCreatedBy()));
        basicData.setAppUser(createdBy);
        basicData.setBasicDataType(BasicDataType.MISTAKE_MESSAGE);
        basicData.setObject1Class(Invoice.class.getSimpleName());
        basicData.setObject1Id(invoice.getInvoiceId().toString());
        basicData.setObject2Class(AppUser.class.getSimpleName());
        basicData.setObject2Id(appUserFrom.getAppUserId().toString());

        basicData.setNumberValue(new BigDecimal((new Date()).getTime()));

        String messageDto = createMessageDTOAsJson("Fehler in Rechnung", "Dem Benutzer " + appUserFrom.getPaymentPersonName() + " ist in einer Rechnung ein Fehler aufgefallen. Er/Sie gab folgende Beschreibung an: " + message, MessageType.MISTAKE);
        basicData.setValue(messageDto);

        basicDataRepository.save(basicData);

        return basicData;
    }

    public BasicData createMistakeSolvedMessage(Invoice invoice, AppUser appUserFrom){
        BasicData basicData = new BasicData();
        basicData.setBasicDataId(UUID.randomUUID());
        basicData.setAppUser(appUserFrom);
        basicData.setBasicDataType(BasicDataType.MISTAKE_MESSAGE);
        basicData.setObject1Class(Invoice.class.getSimpleName());
        basicData.setObject1Id(invoice.getInvoiceId().toString());

        basicData.setNumberValue(new BigDecimal((new Date()).getTime()));

        AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(invoice.getCreatedBy()));

        String messageDto = createMessageDTOAsJson("Fehler in Rechnung gelöst", "Der Benutzer " + createdBy.getPaymentPersonName() + " hat angegeben, dass er das Problem gelöst hat.", MessageType.MISTAKE);
        basicData.setValue(messageDto);

        basicDataRepository.save(basicData);

        return basicData;
    }

    public BasicData createInvoiceChangedMessage(AppUser appUserTo, Invoice invoice, BillingStatusEnum billingStatusEnum){
        BasicData basicData = new BasicData();
        basicData.setBasicDataId(UUID.randomUUID());
        basicData.setAppUser(appUserTo);
        basicData.setBasicDataType(BasicDataType.CHANGED_MESSAGE);
        basicData.setObject1Class(Invoice.class.getSimpleName());
        basicData.setObject1Id(invoice.getInvoiceId().toString());

        basicData.setNumberValue(new BigDecimal((new Date()).getTime()));

        AppUser createdBy = appUserRepository.findOneByAppUserId(UUID.fromString(invoice.getCreatedBy()));

        String messageDto = "";
        if (BillingStatusEnum.PAID.equals(billingStatusEnum)){
            messageDto = createMessageDTOAsJson("Rechnung wurde geändert", "Der Benutzer " + createdBy.getPaymentPersonName() + " hat seine Rechnung geändert. Bitte übeprüfen Sie sie. Da Sie schon die Zahlung getätigt haben, warten Sie bitte die Bestätigung ab und zahlen Sie dann die Differenzsumme bzw. lassen Sie sich diese erstatten.", MessageType.MISTAKE);
        }else {
            messageDto = createMessageDTOAsJson("Rechnung wurde geändert", "Der Benutzer " + createdBy.getPaymentPersonName() + " hat seine Rechnung geändert. Bitte übeprüfen Sie sie.", MessageType.MISTAKE);
        }
        basicData.setValue(messageDto);

        basicDataRepository.save(basicData);

        return basicData;
    }

    public BasicDataDTO createBudgetMessage(AppUser appUser, Budget budget){
        BasicDataDTO basicDataDTO = new BasicDataDTO();
        basicDataDTO.setBasicDataId(UUID.randomUUID());
        basicDataDTO.setAppUserId(appUser.getAppUserId());
        basicDataDTO.setBasicDataType(BasicDataType.BUDGET_MESSAGE);
        basicDataDTO.setObject1Class(Budget.class.getSimpleName());
        basicDataDTO.setObject1Id(budget.getBudgetId().toString());

        basicDataDTO.setNumberValue(new BigDecimal((new Date()).getTime()));
        String budgetExceeded = bigDecimalToString(budget.getCurrentSum().subtract(budget.getSum()));

        String messageDto = createMessageDTOAsJson("Budget überschritten", "Ein Budget wurde um "+ budgetExceeded + " € überschritten. Budget-Konfiguration war: " + budget.getFilterText() + ".", MessageType.BUDGET);
        basicDataDTO.setValue(messageDto);

        return basicDataDTO;
    }

    public BasicDataDTO createBillingMessageForAppUser(AppUser currentUser, Billing billing){

        String appUserId = currentUser.getAppUserId().toString();

        List<UserContact> appUserContacts = userContactRepository.findAllByAppUserContact(currentUser);
        List<UUID> appUserContactIds = appUserContacts.stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        if ((billing.getCostPayerTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) && billing.getBillingStatusEnum().equals(BillingStatusEnum.TO_PAY) && billing.getIsNormalPayment() == true && appUserContactIds.contains(billing.getCostPayerId()) ||
                (billing.getCreatedBy().equals(appUserId) && billing.getBillingStatusEnum().equals(BillingStatusEnum.TO_PAY)  && billing.getIsNormalPayment() == false))){
            return createSendMoneyMessage(currentUser, billing);
        }else if ((billing.getCostPayerTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) && billing.getBillingStatusEnum().equals(BillingStatusEnum.TO_PAY) && billing.getIsNormalPayment() == false && appUserContactIds.contains(billing.getCostPayerId()) ||
                (billing.getCreatedBy().equals(appUserId) && billing.getBillingStatusEnum().equals(BillingStatusEnum.TO_PAY)  && billing.getIsNormalPayment() == true))){
            return createOpenBillingMessage(currentUser, billing);
        }else if ((billing.getCostPayerTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) && billing.getBillingStatusEnum().equals(BillingStatusEnum.PAID) && billing.getIsNormalPayment() == false && appUserContactIds.contains(billing.getCostPayerId()) ||
                (billing.getCreatedBy().equals(appUserId) && billing.getBillingStatusEnum().equals(BillingStatusEnum.PAID)  && billing.getIsNormalPayment() == true))){
            return createMoneyReceivedMessage(currentUser, billing);
        }else if ((billing.getCostPayerTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) && billing.getBillingStatusEnum().equals(BillingStatusEnum.PAYMENT_CONFIRMED) && billing.getIsNormalPayment() == true && appUserContactIds.contains(billing.getCostPayerId()) ||
                (billing.getCreatedBy().equals(appUserId) && billing.getBillingStatusEnum().equals(BillingStatusEnum.PAYMENT_CONFIRMED)  && billing.getIsNormalPayment() == false))){
            return createPaymentConfirmedMessage(currentUser, billing);
        }else if (billing.getCostPayerTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) && (billing.getBillingStatusEnum().equals(BillingStatusEnum.PAYMENT_CONFIRMED) || billing.getBillingStatusEnum().equals(BillingStatusEnum.FINISHED))){
            return createPaymentCompletedMessage(currentUser, billing);
        }else if (billing.getCostPayerTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) && (billing.getBillingStatusEnum().equals(BillingStatusEnum.PAID))){
            return createPaidCompletedMessage(currentUser, billing);
        }else if (billing.getCostPayerTypeEnum().equals(PaymentPersonTypeEnum.CONTACT) && (billing.getBillingStatusEnum().equals(BillingStatusEnum.ARCHIVED))){
            return createPaymentArchivedMessage(currentUser, billing);
        }

        return null;
    }

    public List<BasicDataDTO> createBasicDataMessages(AppUser currentUser) {

        BeanMapper beanMapper = new BeanMapper();

        List<BasicData> basicDatas = new ArrayList<>();
        basicDatas.addAll(basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.MISTAKE_MESSAGE));
        basicDatas.addAll(basicDataRepository.findAllByAppUserAndBasicDataType(currentUser, BasicDataType.CHANGED_MESSAGE));
        List<BasicDataDTO> basicDataDTOs = new ArrayList<>();
        for (BasicData basicData : basicDatas) {
            BasicDataDTO basicDataDTO = beanMapper.map(basicData, BasicDataDTO.class);
            basicDataDTO.setAppUserId(currentUser.getAppUserId());
            basicDataDTOs.add(basicDataDTO);
        }

        List<Billing> billings = billingHelperService.billingsForAppUser(currentUser);
        basicDataDTOs.addAll(createAllBillingMessagesForAppUser(currentUser, billings));
        List<Budget> budgets = budgetRepository.findAllByAppUser(currentUser);
        basicDataDTOs.addAll(createAllBudgetMessagesForAppUser(currentUser, budgets));

        return basicDataDTOs;
    }

    public List<BasicDataDTO> createAllBillingMessagesForAppUser(AppUser currentUser, List<Billing> billings){

        List<BasicDataDTO> basicDataDTOs = new ArrayList<>();

        for (Billing billing : billings) {
            BasicDataDTO basicDataDTO = createBillingMessageForAppUser(currentUser, billing);
            if (basicDataDTO != null) {
                basicDataDTOs.add(basicDataDTO);
            }
        }

        return basicDataDTOs;
    }

    public List<BasicDataDTO> createAllBudgetMessagesForAppUser(AppUser currentUser, List<Budget> budgets){

        List<BasicDataDTO> basicDataDTOs = new ArrayList<>();

        for (Budget budget : budgets) {
            if (budget.getCurrentSum().compareTo(budget.getSum()) >= 0){
                basicDataDTOs.add(createBudgetMessage(currentUser, budget));
            }
        }

        return basicDataDTOs;
    }

    public static String bigDecimalToString(BigDecimal currentDecimal){
        NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.GERMAN);
        numberFormatter.setGroupingUsed(false);
        numberFormatter.setMaximumFractionDigits(2);
        numberFormatter.setMinimumFractionDigits(2);

        String number = numberFormatter.format(currentDecimal);
        return number;
    }

    @Async
    public void sendBillingMessagesAsync(Billing billing){

        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(billing.getCreatedBy()));
        UserContact costPayer = (UserContact) paymentPersonService.findPaymentPerson(billing.getCostPayerId(), billing.getCostPayerTypeEnum());

        if ((billing.getIsNormalPayment() == null || billing.getIsNormalPayment() == true)){
            BasicDataDTO basicDataDTO = createOpenBillingMessage(currentUser, billing);
            MessageDTO messageDTO = createMessageDTOFromJson(basicDataDTO.getValue());
            firebaseService.sendTextMessage(currentUser, FirebaseMessageType.MESSAGE_PAYMENT, messageDTO.getSubject(), messageDTO.getMessage());

            if (costPayer.getAppUserContact() != null){
                basicDataDTO = createSendMoneyMessage(costPayer.getAppUserContact(), billing);
                messageDTO = createMessageDTOFromJson(basicDataDTO.getValue());
                firebaseService.sendTextMessage(costPayer.getAppUserContact(), FirebaseMessageType.MESSAGE_PAYMENT, messageDTO.getSubject(), messageDTO.getMessage());
            }
        }else{
            BasicDataDTO basicDataDTO = createSendMoneyMessage(currentUser, billing);
            MessageDTO messageDTO = createMessageDTOFromJson(basicDataDTO.getValue());
            firebaseService.sendTextMessage(currentUser, FirebaseMessageType.MESSAGE_PAYMENT, messageDTO.getSubject(), messageDTO.getMessage());

            if (costPayer.getAppUserContact() != null){
                basicDataDTO = createOpenBillingMessage(costPayer.getAppUserContact(), billing);
                messageDTO = createMessageDTOFromJson(basicDataDTO.getValue());
                firebaseService.sendTextMessage(costPayer.getAppUserContact(), FirebaseMessageType.MESSAGE_PAYMENT, messageDTO.getSubject(), messageDTO.getMessage());
            }
        }
    }

}
