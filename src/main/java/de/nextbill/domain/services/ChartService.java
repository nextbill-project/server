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

import de.nextbill.domain.comparators.FloatComparator;
import de.nextbill.domain.dtos.DiagrammDataChartCoordinateDTO;
import de.nextbill.domain.dtos.DiagrammDataChartDTO;
import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.DiagramType;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.InvoiceCategory;
import de.nextbill.domain.model.UserContact;
import de.nextbill.domain.pojos.DiagrammDataChart;
import de.nextbill.domain.pojos.InvoiceCostDistributionItem;
import de.nextbill.domain.pojos.PaymentPersonPojo;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.InvoiceCategoryRepository;
import de.nextbill.domain.repositories.UserContactRepository;
import de.nextbill.domain.utils.BeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChartService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CostDistributionHelper costDistributionHelper;

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private InvoiceCategoryRepository invoiceCategoryRepository;

    @Autowired
    private PaymentPersonService paymentPersonService;

    public DiagrammDataChart chartForTimeRange(List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanz im Monatsvergleich");
        diagrammDataChart.setXAxeDisplayName("Monat");
        diagrammDataChart.setYAxeDisplayName("Bilanz");
        diagrammDataChart.setDiagramType(DiagramType.MONTH);
        diagrammDataChart.setUseAbsolutValues(useAbsolutValues);

        List<BigDecimal> valuesforMonth = balanceForEachMonth(invoiceCostDistributionItems);

        Map<Integer, String> numberMonthMap = new HashMap<>();
        numberMonthMap.put(1, "Jan");
        numberMonthMap.put(2, "Feb");
        numberMonthMap.put(3, "Mär");
        numberMonthMap.put(4, "Apr");
        numberMonthMap.put(5, "Mai");
        numberMonthMap.put(6, "Jun");
        numberMonthMap.put(7, "Jul");
        numberMonthMap.put(8, "Aug");
        numberMonthMap.put(9, "Sep");
        numberMonthMap.put(10, "Okt");
        numberMonthMap.put(11, "Nov");
        numberMonthMap.put(12, "Dez");

        Integer counter = valuesforMonth.size()-1;
        List<DiagrammDataChartCoordinateDTO> diagrammDataChartCoordinateDTOList = new ArrayList<>();
        for (BigDecimal monthValue : valuesforMonth) {

            DiagrammDataChartCoordinateDTO diagrammDataChartCoordinateDTO = new DiagrammDataChartCoordinateDTO();
            diagrammDataChartCoordinateDTO.setXValueObject(counter);
            if (useAbsolutValues){
                diagrammDataChartCoordinateDTO.setYValue(monthValue.abs());
            }else{
                diagrammDataChartCoordinateDTO.setYValue(monthValue);
            }

            diagrammDataChartCoordinateDTOList.add(diagrammDataChartCoordinateDTO);
            counter--;
        }

//        Collections.reverse(diagrammDataChartCoordinateDTOList);

        for (int i = 0; i < diagrammDataChartCoordinateDTOList.size(); i++) {

            Integer month = (Integer) diagrammDataChartCoordinateDTOList.get(i).getXValueObject();

            Calendar monthBefore = Calendar.getInstance();
            monthBefore.add(Calendar.MONTH, (month * -1));

            Integer monthNumberBefore = monthBefore.get(Calendar.MONTH);

            diagrammDataChartCoordinateDTOList.get(i).setXValue(new BigDecimal(monthNumberBefore+1));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateDTOList);
        diagrammDataChart.setSortByYValues(false);

        return diagrammDataChart;
    }

    public DiagrammDataChart chartForCategories(List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanzvergleich von Kategorien");
        diagrammDataChart.setXAxeDisplayName("Kategorie");
        diagrammDataChart.setYAxeDisplayName("Bilanz");
        diagrammDataChart.setDiagramType(DiagramType.CATEGORY);
        diagrammDataChart.setUseAbsolutValues(useAbsolutValues);

        Map<InvoiceCategory, BigDecimal> valuesforMonth = valuesForEachCategory(invoiceCostDistributionItems);

        Map<BigDecimal, String> xValues = new HashMap<>();

        List<DiagrammDataChartCoordinateDTO> diagrammDataChartCoordinateDTOList = new ArrayList<>();
        for (InvoiceCategory invoiceCategory : valuesforMonth.keySet()) {

            DiagrammDataChartCoordinateDTO diagrammDataChartCoordinateDTO = new DiagrammDataChartCoordinateDTO();
            diagrammDataChartCoordinateDTO.setXValueObject(invoiceCategory);
            if (useAbsolutValues){
                diagrammDataChartCoordinateDTO.setYValue(valuesforMonth.get(invoiceCategory).abs());
            }else{
                diagrammDataChartCoordinateDTO.setYValue(valuesforMonth.get(invoiceCategory));
            }

            diagrammDataChartCoordinateDTOList.add(diagrammDataChartCoordinateDTO);
        }

        Collections.sort(diagrammDataChartCoordinateDTOList, new FloatComparator());

        for (int i = 0; i < diagrammDataChartCoordinateDTOList.size(); i++) {
            InvoiceCategory paymentPersonPojo = (InvoiceCategory) diagrammDataChartCoordinateDTOList.get(i).getXValueObject();
            xValues.put(new BigDecimal(i), paymentPersonPojo.getInvoiceCategoryName());
            diagrammDataChartCoordinateDTOList.get(i).setXValue(new BigDecimal(i));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateDTOList);
        diagrammDataChart.setXAxesValues(xValues);

        return diagrammDataChart;
    }

    public DiagrammDataChart chartForDaysInMonth(List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanz im Monatsverlauf");
        diagrammDataChart.setXAxeDisplayName("Tag");
        diagrammDataChart.setYAxeDisplayName("Bilanz");
        diagrammDataChart.setDiagramType(DiagramType.DAY);
        diagrammDataChart.setUseAbsolutValues(useAbsolutValues);

        List<BigDecimal> valuesforEachDay = balanceForEachDay(invoiceCostDistributionItems);

        Map<BigDecimal, String> xValues = new HashMap<>();

        List<DiagrammDataChartCoordinateDTO> diagrammDataChartCoordinateDTOList = new ArrayList<>();
        for (int i = 0; i < 31; i++) {

            BigDecimal valueForDay = valuesforEachDay.get(i);

            DiagrammDataChartCoordinateDTO diagrammDataChartCoordinateDTO = new DiagrammDataChartCoordinateDTO();
            diagrammDataChartCoordinateDTO.setXValueObject("Tag " + i);
            if (useAbsolutValues){
                diagrammDataChartCoordinateDTO.setYValue(valueForDay.abs());
            }else{
                diagrammDataChartCoordinateDTO.setYValue(valueForDay);
            }

            diagrammDataChartCoordinateDTOList.add(diagrammDataChartCoordinateDTO);
        }

        for (int i = 0; i < diagrammDataChartCoordinateDTOList.size(); i++) {
            String dayName = (String) diagrammDataChartCoordinateDTOList.get(i).getXValueObject();
            xValues.put(new BigDecimal(i), dayName);
            diagrammDataChartCoordinateDTOList.get(i).setXValue(new BigDecimal(i));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateDTOList);
        diagrammDataChart.setXAxesValues(xValues);

        return diagrammDataChart;
    }

    public DiagrammDataChart chartForCostPayer(List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanz je Träger");
        diagrammDataChart.setXAxeDisplayName("Träger");
        diagrammDataChart.setYAxeDisplayName("Bilanz");
        diagrammDataChart.setDiagramType(DiagramType.COST_PAYER);
        diagrammDataChart.setUseAbsolutValues(useAbsolutValues);

        Map<PaymentPersonPojo, BigDecimal> valuesForPersons = costsByCostsPayer(invoiceCostDistributionItems);

        Map<BigDecimal, String> xValues = new HashMap<>();

        List<DiagrammDataChartCoordinateDTO> diagrammDataChartCoordinateDTOList = new ArrayList<>();
        for (PaymentPersonPojo paymentPersonPojo : valuesForPersons.keySet()) {

            DiagrammDataChartCoordinateDTO diagrammDataChartCoordinateDTO = new DiagrammDataChartCoordinateDTO();
            diagrammDataChartCoordinateDTO.setXValueObject(paymentPersonPojo);
            if (useAbsolutValues){
                diagrammDataChartCoordinateDTO.setYValue(valuesForPersons.get(paymentPersonPojo).abs());
            }else{
                diagrammDataChartCoordinateDTO.setYValue(valuesForPersons.get(paymentPersonPojo));
            }

            diagrammDataChartCoordinateDTOList.add(diagrammDataChartCoordinateDTO);
        }

        Collections.sort(diagrammDataChartCoordinateDTOList, new FloatComparator());

        for (int i = 0; i < diagrammDataChartCoordinateDTOList.size(); i++) {
            PaymentPersonPojo paymentPersonPojo = (PaymentPersonPojo) diagrammDataChartCoordinateDTOList.get(i).getXValueObject();
            xValues.put(new BigDecimal(i), paymentPersonPojo.getPaymentPersonName());
            diagrammDataChartCoordinateDTOList.get(i).setXValue(new BigDecimal(i));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateDTOList);
        diagrammDataChart.setXAxesValues(xValues);

        return diagrammDataChart;
    }

    public DiagrammDataChart chartForPaymentRecipients(List<InvoiceCostDistributionItem> invoiceCostDistributionItems, boolean useAbsolutValues){
        DiagrammDataChart diagrammDataChart = new DiagrammDataChart();
        diagrammDataChart.setDisplayName("Bilanzvergleich je Zahlungsempfänger");
        diagrammDataChart.setXAxeDisplayName("Zahlungsempfänger");
        diagrammDataChart.setYAxeDisplayName("Bilanz");
        diagrammDataChart.setDiagramType(DiagramType.PAYMENT_RECIPIENTS);
        diagrammDataChart.setUseAbsolutValues(useAbsolutValues);

        Map<PaymentPersonPojo, BigDecimal> valuesForPersons = costsByPaymentRecipients(invoiceCostDistributionItems);

        Map<BigDecimal, String> xValues = new HashMap<>();

        List<DiagrammDataChartCoordinateDTO> diagrammDataChartCoordinateDTOList = new ArrayList<>();
        for (PaymentPersonPojo paymentPersonPojo : valuesForPersons.keySet()) {

            DiagrammDataChartCoordinateDTO diagrammDataChartCoordinateDTO = new DiagrammDataChartCoordinateDTO();
            diagrammDataChartCoordinateDTO.setXValueObject(paymentPersonPojo);
            if (useAbsolutValues){
                diagrammDataChartCoordinateDTO.setYValue(valuesForPersons.get(paymentPersonPojo).abs());
            }else{
                diagrammDataChartCoordinateDTO.setYValue(valuesForPersons.get(paymentPersonPojo));
            }

            diagrammDataChartCoordinateDTOList.add(diagrammDataChartCoordinateDTO);
        }

        Collections.sort(diagrammDataChartCoordinateDTOList, new FloatComparator());

        for (int i = 0; i < diagrammDataChartCoordinateDTOList.size(); i++) {
            PaymentPersonPojo paymentPersonPojo = (PaymentPersonPojo) diagrammDataChartCoordinateDTOList.get(i).getXValueObject();
            xValues.put(new BigDecimal(i), paymentPersonPojo.getPaymentPersonName());
            diagrammDataChartCoordinateDTOList.get(i).setXValue(new BigDecimal(i));
        }

        diagrammDataChart.setCoordinates(diagrammDataChartCoordinateDTOList);
        diagrammDataChart.setXAxesValues(xValues);

        return diagrammDataChart;
    }

    public DiagrammDataChartDTO convertDiagrammForChart(DiagrammDataChart diagrammDataChart){

        BeanMapper beanMapper = new BeanMapper();
        DiagrammDataChartDTO diagrammDataChartResponse = beanMapper.map(diagrammDataChart, DiagrammDataChartDTO.class);

        Map<Integer, String> numberMonthMap = new HashMap<>();
        numberMonthMap.put(1, "Jan");
        numberMonthMap.put(2, "Feb");
        numberMonthMap.put(3, "Mär");
        numberMonthMap.put(4, "Apr");
        numberMonthMap.put(5, "Mai");
        numberMonthMap.put(6, "Jun");
        numberMonthMap.put(7, "Jul");
        numberMonthMap.put(8, "Aug");
        numberMonthMap.put(9, "Sep");
        numberMonthMap.put(10, "Okt");
        numberMonthMap.put(11, "Nov");
        numberMonthMap.put(12, "Dez");

        List<String> xAxeValues = new ArrayList<>();
        List<BigDecimal> yAxeValues = new ArrayList<>();
        for (DiagrammDataChartCoordinateDTO diagrammDataChartCoordinateDTO : diagrammDataChart.getCoordinates()) {

            if (diagrammDataChartCoordinateDTO.getXValueObject() instanceof InvoiceCategory) {
                InvoiceCategory invoiceCategory = (InvoiceCategory) diagrammDataChartCoordinateDTO.getXValueObject();
                xAxeValues.add(invoiceCategory.getInvoiceCategoryName());
            }else if (diagrammDataChartCoordinateDTO.getXValueObject() instanceof String){
                String string = (String) diagrammDataChartCoordinateDTO.getXValueObject();
                xAxeValues.add(string);
            }else if (diagrammDataChartCoordinateDTO.getXValueObject() instanceof PaymentPersonPojo){
                PaymentPersonPojo paymentPersonPojo = (PaymentPersonPojo) diagrammDataChartCoordinateDTO.getXValueObject();
                xAxeValues.add(paymentPersonPojo.getPaymentPersonName());
            }else if (diagrammDataChartCoordinateDTO.getXValueObject() instanceof Integer){
                Integer month = diagrammDataChartCoordinateDTO.getXValue().intValue();
                xAxeValues.add(numberMonthMap.get(month));
            }

            yAxeValues.add(diagrammDataChartCoordinateDTO.getYValue());
        }

        diagrammDataChartResponse.setXAxesValues(xAxeValues);
        diagrammDataChartResponse.setYAxesValues(yAxeValues);

        return diagrammDataChartResponse;
    }

    private List<BigDecimal> balanceForEachMonth(List<InvoiceCostDistributionItem> invoiceCostDistributionItems){

        List<BigDecimal> monthValue = new ArrayList<>();

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        Map<UUID, List<UUID>> userContactIds = new HashMap<>();

        for (int i = 6; i >= 0; i--){
            BigDecimal valueSum = new BigDecimal(0);
            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
                Date dateOfInvoice = invoiceCostDistributionItem.getInvoice().getDateOfInvoice();

                if (dateOfInvoice == null){
                    continue;
                }

                Calendar month = Calendar.getInstance();
                month.add(Calendar.MONTH, -1 * i);
                Integer firstDayOfMonth = month.getActualMinimum(Calendar.DAY_OF_MONTH);
                Integer lastDayOfMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
                Calendar firstDay = (Calendar) month.clone();
                Calendar lastDay = (Calendar) month.clone();
                firstDay.set(Calendar.DAY_OF_MONTH, firstDayOfMonth);
                firstDay.set(Calendar.HOUR_OF_DAY, 0);
                firstDay.set(Calendar.MINUTE, 0);
                lastDay.set(Calendar.DAY_OF_MONTH, lastDayOfMonth);
                lastDay.set(Calendar.HOUR_OF_DAY, 23);
                lastDay.set(Calendar.MINUTE, 59);
                Date firstDayDate = firstDay.getTime();
                Date lastDayDate = lastDay.getTime();

                Calendar invoiceDate =  Calendar.getInstance();
                invoiceDate.setTime(dateOfInvoice);
                invoiceDate.set(Calendar.HOUR_OF_DAY, 6);
                invoiceDate.set(Calendar.MINUTE, 0);
                Date invoiceDateDate = invoiceDate.getTime();

                if (invoiceDateDate.getTime() > firstDayDate.getTime() && invoiceDateDate.getTime() < lastDayDate.getTime()){
                    BigDecimal sum = costDistributionHelper.invoiceCostForPaymentPerson(invoiceCostDistributionItem.getInvoice(), currentUser, null, userContactIds);
                    valueSum = valueSum.add(sum);
                }
            }

            monthValue.add(valueSum);
        }

        return monthValue;
    }


    private List<BigDecimal> balanceForEachDay(List<InvoiceCostDistributionItem> invoiceCostDistributionItems){

        List<BigDecimal> dayValue = new ArrayList<>();

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        Map<UUID, List<UUID>> userContactIds = new HashMap<>();

        for (int i = 1; i <= 31; i++){
            BigDecimal valueSum = new BigDecimal(0);
            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
                Date dateOfInvoice = invoiceCostDistributionItem.getInvoice().getDateOfInvoice();

                if (dateOfInvoice == null){
                    continue;
                }

                Calendar dayDate = Calendar.getInstance();
                dayDate.setTime(dateOfInvoice);

                Integer dayOfMonth = dayDate.get(Calendar.DAY_OF_MONTH);

                if (dayOfMonth == i){
                    BigDecimal sum = costDistributionHelper.invoiceCostForPaymentPerson(invoiceCostDistributionItem.getInvoice(), currentUser, null, userContactIds);
                    valueSum = valueSum.add(sum);
                }
            }

            dayValue.add(valueSum);
        }

        return dayValue;
    }

    public Map<InvoiceCategory, BigDecimal> valuesForEachCategory(List<InvoiceCostDistributionItem> invoiceCostDistributionItems){

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        Map<InvoiceCategory, BigDecimal> categoryValue = new HashMap<>();

        List<InvoiceCategory> invoiceCategories = invoiceCategoryRepository.findAllByAppUserIsNullOrAppUser(currentUser);

        Map<UUID, List<UUID>> userContactIds = new HashMap<>();

        for (InvoiceCategory invoiceCategory : invoiceCategories) {
            BigDecimal sumForCategory = new BigDecimal(0);

            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
                if (invoiceCostDistributionItem.getInvoice().getInvoiceCategory() != null &&
                        invoiceCostDistributionItem.getInvoice().getInvoiceCategory().getInvoiceCategoryId().equals(invoiceCategory.getInvoiceCategoryId())){
                    BigDecimal sum = costDistributionHelper.invoiceCostForPaymentPerson(invoiceCostDistributionItem.getInvoice(), currentUser, null, userContactIds);
                    sumForCategory = sumForCategory.add(sum);
                }
            }

            if (sumForCategory.compareTo(new BigDecimal(0)) != 0){
                categoryValue.put(invoiceCategory, sumForCategory);
            }
        }

        return categoryValue;
    }

    private Map<PaymentPersonPojo, BigDecimal> costsByPaymentRecipients(List<InvoiceCostDistributionItem> invoiceCostDistributionItems){

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));

        Map<PaymentPersonPojo, BigDecimal> personValue = new HashMap<>();

        List<UUID> userContactIdsOfCurrentUser = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());
        userContactIdsOfCurrentUser.add(currentUser.getAppUserId());

        Map<UUID, List<UUID>> userContactIds = new HashMap<>();

        for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {
            if (invoiceCostDistributionItem.getInvoice().getPaymentRecipientId() != null && !userContactIdsOfCurrentUser.contains(invoiceCostDistributionItem.getInvoice().getPaymentRecipientId().toString())){

                PaymentPerson paymentPerson = paymentPersonService.findPaymentPerson(invoiceCostDistributionItem.getInvoice().getPaymentRecipientId(), invoiceCostDistributionItem.getInvoice().getPaymentRecipientTypeEnum());
                PaymentPersonPojo paymentPersonPojo = PaymentPersonPojo.fromIPaymentPerson(paymentPerson);

                BigDecimal sum = costDistributionHelper.invoiceCostForPaymentPerson(invoiceCostDistributionItem.getInvoice(), currentUser, null, userContactIds);
                if (personValue.containsKey(paymentPersonPojo)){
                    sum = sum.add(personValue.get(paymentPersonPojo));
                }

                personValue.remove(paymentPersonPojo);
                personValue.put(paymentPersonPojo, sum);
            }
        }

        return personValue;
    }

    private Map<PaymentPersonPojo, BigDecimal> costsByCostsPayer(List<InvoiceCostDistributionItem> invoiceCostDistributionItems){

        Map<PaymentPersonPojo, BigDecimal> personValue = new HashMap<>();

        Map<PaymentPersonPojo, List<PaymentPersonPojo>> personIds = new HashMap<>();

        String loggedInUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser currentUser = appUserRepository.findOneByAppUserId(UUID.fromString(loggedInUserName));
        List<PaymentPersonPojo> userIds = new ArrayList<>();

        PaymentPersonPojo userPaymentPersonPojo = new PaymentPersonPojo();
        userPaymentPersonPojo.setPayerEnum(PaymentPersonTypeEnum.USER);
        userPaymentPersonPojo.setPayerId(currentUser.getAppUserId());
        userPaymentPersonPojo.setPayerName(currentUser.getAppUserName());

        userIds.add(userPaymentPersonPojo);
        personIds.put(userPaymentPersonPojo, userIds);

        List<UserContact> userContacts = userContactRepository.findAllByAppUserAndBasicStatusEnum(currentUser, BasicStatusEnum.OK);

        Map<UUID, List<UUID>> userContactIdsMap = new HashMap<>();

        for (UserContact userContact : userContacts) {
            List<PaymentPersonPojo> userContactIds = new ArrayList<>();

            PaymentPersonPojo paymentPersonPojo = new PaymentPersonPojo();
            paymentPersonPojo.setPayerEnum(PaymentPersonTypeEnum.CONTACT);
            paymentPersonPojo.setPayerId(userContact.getUserContactId());
            paymentPersonPojo.setPayerName(userContact.getContactName());

            AppUser appUserContact = userContact.getAppUserContact();
            if (appUserContact != null){
                PaymentPersonPojo apppaymentPersonPojo = new PaymentPersonPojo();
                apppaymentPersonPojo.setPayerEnum(PaymentPersonTypeEnum.USER);
                apppaymentPersonPojo.setPayerId(appUserContact.getAppUserId());
                apppaymentPersonPojo.setPayerName(userContact.getContactName());

                userContactIds.add(apppaymentPersonPojo);
            }else{
                userContactIds.add(paymentPersonPojo);
            }

            personIds.put(paymentPersonPojo, userContactIds);
        }

        for (Map.Entry<PaymentPersonPojo, List<PaymentPersonPojo>> paymentPersonPojoListEntry : personIds.entrySet()) {

            PaymentPersonPojo paymentPersonPojo = paymentPersonPojoListEntry.getKey();

            BigDecimal sumTotalForBean = new BigDecimal(0);
            for (InvoiceCostDistributionItem invoiceCostDistributionItem : invoiceCostDistributionItems) {

                for (PaymentPersonPojo personBean : paymentPersonPojoListEntry.getValue()) {
                    BigDecimal sum = costDistributionHelper.invoiceCostForPaymentPerson(invoiceCostDistributionItem.getInvoice(), personBean, null, userContactIdsMap);

                    sumTotalForBean = sumTotalForBean.add(sum);
                }
            }

            if (!sumTotalForBean.equals(new BigDecimal(0))){
                personValue.put(paymentPersonPojo, sumTotalForBean);
            }
        }

        return personValue;
    }


}
