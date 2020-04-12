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

import com.mysema.query.BooleanBuilder;
import de.nextbill.domain.enums.*;
import de.nextbill.domain.model.*;
import de.nextbill.domain.pojos.InvoiceCostDistributionItem;
import de.nextbill.domain.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StatisticService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BasicDataRepository basicDataRepository;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private InvoiceCategoryRepository invoiceCategoryRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CostDistributionRepository costDistributionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CostDistributionItemRepository costDistributionItemRepository;

    @Autowired
    private AutoFillHelperService autoFillHelperService;

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private CostDistributionHelper costDistributionHelper;

    @Autowired
    private SearchService searchService;

    public void execute() {

        List<AppUser> appUsers = appUserRepository.findAll();

        for (AppUser appUser : appUsers) {

            if (appUser.getDeleted() != null && appUser.getDeleted()){
                continue;
            }

            if (appUser.getEmail().equals("BatchUser")){
                continue;
            }

            BigDecimal monthAverageExpense = expensePerMonthForUser(appUser);
            BigDecimal monthAverageIncome = incomePerMonthForUser(appUser);
            BigDecimal estimatedIncomeForCurrentMonth = estimatedIncomeForCurrentMonth(appUser);
            BigDecimal averageExpenseTillNow = expenseForEachMonthTillNowDateForUser(appUser);
            BigDecimal averageExpenseAfterNow = expenseForEachMonthAfterNowDateForUser(appUser);

            BigDecimal sumForCurrentMonth = expenseSumForMonth(appUser);
            BigDecimal estimatedTotalSumForMonth = sumForCurrentMonth.add(averageExpenseAfterNow);

//            BigDecimal differenceBetweenNowAndAverage = averageExpenseTillNow.subtract(sumForCurrentMonth);

            Boolean areThereEnoughEntriesForAnalysis = areThereEnoughEntriesForAnalysis(appUser);
            BigDecimal resultPlusAtMonthEnd = new BigDecimal(0);
            if (areThereEnoughEntriesForAnalysis){
                resultPlusAtMonthEnd = estimatedIncomeForCurrentMonth.subtract(estimatedTotalSumForMonth);
            }else{
                resultPlusAtMonthEnd = estimatedIncomeForCurrentMonth.subtract(monthAverageExpense);
            }

            Date startDate = new Date();

            generateBusinessPartnerToCategoryRate(appUser);

            Date endDate = new Date();
            long diff = endDate.getTime() - startDate.getTime();
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
            logger.info("1 Duration of cronjob StatisticScheduler:" + seconds);

            startDate = new Date();

            generateCategoryToCostDistributionRate(appUser);

            endDate = new Date();
            diff = endDate.getTime() - startDate.getTime();
            seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
            logger.info("2 Duration of cronjob StatisticScheduler:" + seconds);

            generateCategoryToSpecialType(appUser);

            BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.MONTH_AVERAGE_INCOME);
            if (basicData == null){
                basicData = new BasicData();
                basicData.setBasicDataId(UUID.randomUUID());
                basicData.setAppUser(appUser);
                basicData.setBasicDataType(BasicDataType.STATISTIC);
                basicData.setBasicDataSubType(BasicDataSubType.MONTH_AVERAGE_INCOME);
            }
            basicData.setNumberValue(monthAverageIncome);
            basicDataRepository.save(basicData);

            basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.CALCULATED_PROFIT_FOR_MONTH);
            if (basicData == null){
                basicData = new BasicData();
                basicData.setBasicDataId(UUID.randomUUID());
                basicData.setAppUser(appUser);
                basicData.setBasicDataType(BasicDataType.STATISTIC);
                basicData.setBasicDataSubType(BasicDataSubType.CALCULATED_PROFIT_FOR_MONTH);
            }
            basicData.setNumberValue(resultPlusAtMonthEnd);
            basicDataRepository.save(basicData);

            basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.MONTH_AVERAGE_EXPENSE);
            if (basicData == null){
                basicData = new BasicData();
                basicData.setBasicDataId(UUID.randomUUID());
                basicData.setAppUser(appUser);
                basicData.setBasicDataType(BasicDataType.STATISTIC);
                basicData.setBasicDataSubType(BasicDataSubType.MONTH_AVERAGE_EXPENSE);
            }
            basicData.setNumberValue(monthAverageExpense);
            basicDataRepository.save(basicData);

            basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.AVERAGE_EXPENSE_TILL_NOW);
            if (basicData == null){
                basicData = new BasicData();
                basicData.setBasicDataId(UUID.randomUUID());
                basicData.setAppUser(appUser);
                basicData.setBasicDataType(BasicDataType.STATISTIC);
                basicData.setBasicDataSubType(BasicDataSubType.AVERAGE_EXPENSE_TILL_NOW);
            }
            basicData.setNumberValue(averageExpenseTillNow);
            basicDataRepository.save(basicData);

            basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.AVERAGE_EXPENSE_AFTER_NOW);
            if (basicData == null){
                basicData = new BasicData();
                basicData.setBasicDataId(UUID.randomUUID());
                basicData.setAppUser(appUser);
                basicData.setBasicDataType(BasicDataType.STATISTIC);
                basicData.setBasicDataSubType(BasicDataSubType.AVERAGE_EXPENSE_AFTER_NOW);
            }
            basicData.setNumberValue(averageExpenseAfterNow);
            basicDataRepository.save(basicData);

            basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.ESTIMATED_INCOME_FOR_CURRENT_MONTH);
            if (basicData == null){
                basicData = new BasicData();
                basicData.setBasicDataId(UUID.randomUUID());
                basicData.setAppUser(appUser);
                basicData.setBasicDataType(BasicDataType.STATISTIC);
                basicData.setBasicDataSubType(BasicDataSubType.ESTIMATED_INCOME_FOR_CURRENT_MONTH);
            }
            basicData.setNumberValue(estimatedIncomeForCurrentMonth);
            basicDataRepository.save(basicData);

            basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.ARE_THERE_ENOUGH_ENTRIES_FOR_ANALYSIS);
            if (basicData == null){
                basicData = new BasicData();
                basicData.setBasicDataId(UUID.randomUUID());
                basicData.setAppUser(appUser);
                basicData.setBasicDataType(BasicDataType.STATISTIC);
                basicData.setBasicDataSubType(BasicDataSubType.ARE_THERE_ENOUGH_ENTRIES_FOR_ANALYSIS);
            }
            basicData.setValue(areThereEnoughEntriesForAnalysis.toString());
            basicDataRepository.save(basicData);


        }


    }

    public BigDecimal expenseSumForMonth(AppUser currentUser){

        Date now = new Date();

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.MINUTE, -1);
        Date startDate = calendar.getTime();

        Calendar calendar2 = new GregorianCalendar();
        calendar2.setTime(now);
        Date endDate = calendar2.getTime();

        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        if (userContactIds.isEmpty()){
            userContactIds.add(UUID.randomUUID());
        }

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        List<MainFunctionEnum> mainFunctionEna = new ArrayList<>();
        mainFunctionEna.add(MainFunctionEnum.EXPENSE);

        List<InvoiceCostDistributionItem> invoiceCostDistributionItems = searchService.search(false, true,null, false,true,null,null,null,null,null,null,null,null,null, booleanBuilder, currentUser.getAppUserId().toString(), null, true, null, null);
        List<Invoice> invoices = searchService.invoicesInInvoiceCostDistributionItems(invoiceCostDistributionItems);

        BigDecimal sumResult = filterSumResultByDate(currentUser, startDate, endDate, invoices);

        sumResult = sumResult.multiply(new BigDecimal(-1));

        return sumResult;
    }

    public BigDecimal incomeSumForMonth(AppUser currentUser){

        Date now = new Date();

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.add(Calendar.MINUTE, -1);
        Date startDate = calendar.getTime();

        Calendar calendar2 = new GregorianCalendar();
        calendar2.setTime(now);
        Date endDate = calendar2.getTime();

        List<UUID> userContactIds = userContactRepository.findAllByAppUserContactAndBasicStatusEnum(currentUser, BasicStatusEnum.OK).stream().map(UserContact::getUserContactId).collect(Collectors.toList());

        if (userContactIds.isEmpty()){
            userContactIds.add(UUID.randomUUID());
        }

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        List<MainFunctionEnum> mainFunctionEna = new ArrayList<>();
        mainFunctionEna.add(MainFunctionEnum.INCOME);

        List<InvoiceCostDistributionItem> invoiceCostDistributionItems = searchService.search(false, true,null,false,true,null,null,null,null,null,null,null,null,null, booleanBuilder, currentUser.getAppUserId().toString(), null, true, null, null);
        List<Invoice> invoices = searchService.invoicesInInvoiceCostDistributionItems(invoiceCostDistributionItems);

        BigDecimal sumResult = filterSumResultByDate(currentUser, startDate, endDate, invoices);

        return sumResult;
    }

    public BigDecimal estimatedIncomeForCurrentMonth(AppUser appUser){

        Date nowDate = new Date();

        List<RepetitionTypeEnum> repetitionTypeEnumList = new ArrayList<>();
        repetitionTypeEnumList.addAll(Arrays.asList(RepetitionTypeEnum.values()));

        BigDecimal sumOfAllRepetitionTypeSums = new BigDecimal(0);

        LocalDate startLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
        startLocalDateTmp = startLocalDateTmp.withDayOfMonth(1);
        Date startDateTmp = Date.from(startLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

        LocalDate endLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
        endLocalDateTmp = endLocalDateTmp.withDayOfMonth(endLocalDateTmp.lengthOfMonth());
        Date endDateTmp = Date.from(endLocalDateTmp.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

        for (RepetitionTypeEnum repetitionTypeEnum : repetitionTypeEnumList) {

            List<CostDistributionItem> standingOrderCostDistributionItems = costDistributionItemRepository.findStandingOrderIncomeForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, false, repetitionTypeEnum);
            standingOrderCostDistributionItems.addAll(costDistributionItemRepository.findStandingOrderIncomeForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, true, repetitionTypeEnum));
            for (CostDistributionItem standingOrderCostDistributionItem : standingOrderCostDistributionItems) {

                BigDecimal averageMonthCosts = standingOrderCostDistributionItem.getMoneyValue();
                sumOfAllRepetitionTypeSums = sumOfAllRepetitionTypeSums.add(averageMonthCosts);
            }

            List<CostDistributionItem> repetitionNormalCostDistributionItems = costDistributionItemRepository.findIncomeForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, false, repetitionTypeEnum, InvoiceStatusEnum.READY);
            repetitionNormalCostDistributionItems.addAll(costDistributionItemRepository.findIncomeForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, false, repetitionTypeEnum, InvoiceStatusEnum.CHECK));
            repetitionNormalCostDistributionItems.addAll(costDistributionItemRepository.findIncomeForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, true, repetitionTypeEnum, InvoiceStatusEnum.READY));
            repetitionNormalCostDistributionItems.addAll(costDistributionItemRepository.findIncomeForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, true, repetitionTypeEnum, InvoiceStatusEnum.CHECK));
            for (CostDistributionItem repetitionNormalCostDistributionItem : repetitionNormalCostDistributionItems) {
                BigDecimal averageMonthCosts = repetitionNormalCostDistributionItem.getMoneyValue();
                sumOfAllRepetitionTypeSums = sumOfAllRepetitionTypeSums.add(averageMonthCosts);
            }
        }

        return sumOfAllRepetitionTypeSums;
    }

    public BigDecimal filterSumResultByDate(AppUser currentUser, Date startDate, Date endDate, List<Invoice> invoices) {

        BigDecimal sumResult = new BigDecimal(0);

        for (Invoice invoice : invoices) {

            if (invoice.getDateOfInvoice() == null){
                continue;
            }

            Calendar invoiceDate =  Calendar.getInstance();
            invoiceDate.setTime(invoice.getDateOfInvoice());
            invoiceDate.set(Calendar.HOUR_OF_DAY, 6);
            invoiceDate.set(Calendar.MINUTE, 0);
            Date invoiceDateDate = invoiceDate.getTime();

            if (invoiceDateDate.after(startDate) && invoiceDateDate.before(endDate)){
                BigDecimal invoiceCost = costDistributionHelper.invoiceCostForPaymentPerson(invoice, currentUser);
                sumResult = sumResult.add(invoiceCost);
            }
        }
        return sumResult;
    }

    public BigDecimal incomePerMonthForUser(AppUser appUser){

        Date nowDate = new Date();

        List<BigDecimal> incomeSumsForEachMonth = new ArrayList<>();

        BigDecimal averageResult = new BigDecimal(0);

        int lastOneBeforeHasItems = 0;
        Integer secondLastMonthBefore = null;

        Integer monthRange = 7;
        for(int i = 0; i < monthRange; i++){
            LocalDate startLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            if (i > 0){
                startLocalDate = startLocalDate.minusMonths(i);
            }
            startLocalDate = startLocalDate.withDayOfMonth(1);
            Date startDate = Date.from(startLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            LocalDate endLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endLocalDate = endLocalDate.minusMonths(i);
            endLocalDate = endLocalDate.withDayOfMonth(endLocalDate.lengthOfMonth());
            Date endDate = Date.from(endLocalDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

            List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findIncomeForAppUser(appUser.getAppUserId(), startDate, endDate, false, RepetitionTypeEnum.ONCE, InvoiceStatusEnum.READY);
            BigDecimal sumForMonth = sumOfCostDistributionItems(costDistributionItems);

            if (lastOneBeforeHasItems > 0){
                if (sumForMonth.compareTo(new BigDecimal(0)) != 0){

                    if (secondLastMonthBefore == null){
                        secondLastMonthBefore = i;
                    }
                    incomeSumsForEachMonth.add(sumForMonth);
                }
            }

            lastOneBeforeHasItems = costDistributionItems.size();
        }

        BigDecimal overallSum = sumOfBigDecimals(incomeSumsForEachMonth);

        if (!incomeSumsForEachMonth.isEmpty()){
            averageResult = overallSum.divide(new BigDecimal(incomeSumsForEachMonth.size()), RoundingMode.HALF_EVEN);
        }

        List<RepetitionTypeEnum> repetitionTypeEnumList = new ArrayList<>();
        for (RepetitionTypeEnum repetitionTypeEnum : Arrays.asList(RepetitionTypeEnum.values())) {
            repetitionTypeEnumList.add(repetitionTypeEnum);
        }
        repetitionTypeEnumList.remove(RepetitionTypeEnum.ONCE);

        BigDecimal sumOfAllRepetitionTypeSums = new BigDecimal(0);
        for (RepetitionTypeEnum repetitionTypeEnum : repetitionTypeEnumList) {

            BigDecimal sumOfStandingOrderItems = new BigDecimal(0);

            LocalDate startLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDateTmp = startLocalDateTmp.plusDays(1);
            Date startDateTmp = Date.from(startLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            LocalDate endLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endLocalDateTmp = endLocalDateTmp.plusMonths(repetitionTypeEnum.getCounterMonths());
            Date endDateTmp = Date.from(endLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            List<Invoice> invoiceTemplates = new ArrayList<>();
            List<CostDistributionItem> standingOrderCostDistributionItems = costDistributionItemRepository.findStandingOrderIncomeForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, false, repetitionTypeEnum);
            for (CostDistributionItem standingOrderCostDistributionItem : standingOrderCostDistributionItems) {

                invoiceTemplates.add(standingOrderCostDistributionItem.getInvoice());

                BigDecimal averageMonthCosts = standingOrderCostDistributionItem.getMoneyValue().divide(new BigDecimal(repetitionTypeEnum.getCounterMonths()), RoundingMode.HALF_EVEN);
                sumOfStandingOrderItems = sumOfStandingOrderItems.add(averageMonthCosts);
            }

            List<BigDecimal> repetitionTypesNormalItemsSumList = new ArrayList<>();

            int maxMonthBefore = (secondLastMonthBefore != null ? secondLastMonthBefore : 0);
            int counterMonths = repetitionTypeEnum.getCounterMonths();
            if (counterMonths > maxMonthBefore){
                counterMonths = maxMonthBefore;
            }

            startLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDateTmp = startLocalDateTmp.minusMonths(counterMonths);
            startDateTmp = Date.from(startLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            endLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endDateTmp = Date.from(endLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            List<CostDistributionItem> repetitionNormalCostDistributionItems = costDistributionItemRepository.findIncomeForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, false, repetitionTypeEnum, InvoiceStatusEnum.READY);
            for (CostDistributionItem repetitionNormalCostDistributionItem : repetitionNormalCostDistributionItems) {
                Invoice invoiceTemplate = invoiceRepository.invoiceTemplateForStandingOrderInvoice(repetitionNormalCostDistributionItem.getInvoice());

                if (!invoiceTemplates.contains(invoiceTemplate)){
                    BigDecimal averageMonthCosts = repetitionNormalCostDistributionItem.getMoneyValue().divide(new BigDecimal(repetitionTypeEnum.getCounterMonths()), RoundingMode.HALF_EVEN);
                    repetitionTypesNormalItemsSumList.add(averageMonthCosts);
                }
            }

            sumOfAllRepetitionTypeSums = sumOfAllRepetitionTypeSums.add(sumOfStandingOrderItems);
            sumOfAllRepetitionTypeSums = sumOfAllRepetitionTypeSums.add(sumOfBigDecimals(repetitionTypesNormalItemsSumList));
        }

        averageResult = averageResult.add(sumOfAllRepetitionTypeSums);

        return averageResult;
    }

    public Boolean areThereEnoughEntriesForAnalysis(AppUser appUser){

        Date nowDate = new Date();

        LocalDate startLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
        startLocalDate = startLocalDate.minusMonths(2);
        startLocalDate = startLocalDate.withDayOfMonth(1);
        Date startDate = Date.from(startLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

        LocalDate endLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
        endLocalDate = endLocalDate.minusMonths(2);
        endLocalDate = endLocalDate.withDayOfMonth(endLocalDate.lengthOfMonth());
        Date endDate = Date.from(endLocalDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

        List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findExpenseForAppUser(appUser.getAppUserId(), startDate, endDate, false, RepetitionTypeEnum.ONCE);

        return (!costDistributionItems.isEmpty());
    }

    public BigDecimal expensePerMonthForUser(AppUser appUser){

        Date nowDate = new Date();

        List<BigDecimal> expenseSumsForEachMonth = new ArrayList<>();

        BigDecimal averageResult = new BigDecimal(0);

        boolean userHasExpenseBeforeMonthNow = false;
        int lastOneBeforeHasItems = 0;
        Integer secondLastMonthBefore = null;
        for(int i = 6; i >= 1; i--){
            LocalDate startLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDate = startLocalDate.minusMonths(i);

            startLocalDate = startLocalDate.withDayOfMonth(1);
            Date startDate = Date.from(startLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            LocalDate endLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endLocalDate = endLocalDate.minusMonths(i);
            endLocalDate = endLocalDate.withDayOfMonth(endLocalDate.lengthOfMonth());
            Date endDate = Date.from(endLocalDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

            List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findExpenseForAppUser(appUser.getAppUserId(), startDate, endDate, false, RepetitionTypeEnum.ONCE);
            BigDecimal sumForMonth = sumOfCostDistributionItems(costDistributionItems);

            if (lastOneBeforeHasItems > 0){
                if (sumForMonth.compareTo(new BigDecimal(0)) != 0){

                    if (secondLastMonthBefore == null){
                        secondLastMonthBefore = i;
                    }

                    expenseSumsForEachMonth.add(sumForMonth);

                    if (i != 0){
                        userHasExpenseBeforeMonthNow = true;
                    }
                }
            }

            lastOneBeforeHasItems = costDistributionItems.size();

        }

        if (!userHasExpenseBeforeMonthNow){

            BigDecimal estimatedExpenseTillMonthEnd = new BigDecimal(0);

            LocalDate startLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDate = startLocalDate.withDayOfMonth(1);
            Date startDate = Date.from(startLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            LocalDate endLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endLocalDate = endLocalDate.withDayOfMonth(endLocalDate.lengthOfMonth());
            Date endDate = Date.from(endLocalDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

            List<CostDistributionItem> costDistributionItems = costDistributionItemRepository.findExpenseForAppUser(appUser.getAppUserId(), startDate, endDate, false, RepetitionTypeEnum.ONCE);

            List<BigDecimal> normalCostDistributionItems = new ArrayList<>();
            List<BigDecimal> expensiveCostDistributionItems = new ArrayList<>();

            for (CostDistributionItem costDistributionItem : costDistributionItems) {
                if (costDistributionItem.getMoneyValue().compareTo(new BigDecimal(150)) == 1){
                    expensiveCostDistributionItems.add(costDistributionItem.getMoneyValue());
                }else{
                    normalCostDistributionItems.add(costDistributionItem.getMoneyValue());
                }
            }

            BigDecimal sumOfAllNormalItems = sumOfBigDecimals(normalCostDistributionItems);

            LocalDate nowLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();

            BigDecimal costsPerDay = sumOfAllNormalItems.divide(new BigDecimal(nowLocalDate.getDayOfMonth()), RoundingMode.HALF_EVEN);
            BigDecimal costsTillMonthEnd = costsPerDay.multiply(new BigDecimal(nowLocalDate.lengthOfMonth()));

            LocalDate startLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            Date startDateTmp = Date.from(startLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            LocalDate endLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endLocalDateTmp = endLocalDateTmp.withDayOfMonth(endLocalDateTmp.lengthOfMonth());
            Date endDateTmp = Date.from(endLocalDateTmp.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
            costDistributionItems = costDistributionItemRepository.findExpenseForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, true, RepetitionTypeEnum.ONCE);
            List<BigDecimal> standingOrderSumItems = new ArrayList<>();
            for (CostDistributionItem costDistributionItem : costDistributionItems) {
                standingOrderSumItems.add(costDistributionItem.getMoneyValue());
            }

            estimatedExpenseTillMonthEnd = estimatedExpenseTillMonthEnd.add(costsTillMonthEnd);
            estimatedExpenseTillMonthEnd = estimatedExpenseTillMonthEnd.add(sumOfBigDecimals(expensiveCostDistributionItems));
            estimatedExpenseTillMonthEnd = estimatedExpenseTillMonthEnd.add(sumOfBigDecimals(standingOrderSumItems));

            averageResult = estimatedExpenseTillMonthEnd;
        }else{
            BigDecimal overallSum = sumOfBigDecimals(expenseSumsForEachMonth);

            if (!expenseSumsForEachMonth.isEmpty()){
                averageResult = overallSum.divide(new BigDecimal(expenseSumsForEachMonth.size()), RoundingMode.HALF_EVEN);
            }
        }

        List<RepetitionTypeEnum> repetitionTypeEnumList = new ArrayList<>();
        for (RepetitionTypeEnum repetitionTypeEnum : Arrays.asList(RepetitionTypeEnum.values())) {
            repetitionTypeEnumList.add(repetitionTypeEnum);
        }
        repetitionTypeEnumList.remove(RepetitionTypeEnum.ONCE);

        BigDecimal sumOfAllRepetitionTypeSums = new BigDecimal(0);


        for (RepetitionTypeEnum repetitionTypeEnum : repetitionTypeEnumList) {

            BigDecimal sumOfStandingOrderItems = new BigDecimal(0);

            LocalDate startLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDateTmp = startLocalDateTmp.plusDays(1);
            Date startDateTmp = Date.from(startLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            LocalDate endLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endLocalDateTmp = endLocalDateTmp.plusMonths(repetitionTypeEnum.getCounterMonths());
            Date endDateTmp = Date.from(endLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            List<Invoice> invoiceTemplates = new ArrayList<>();
            List<CostDistributionItem> standingOrderCostDistributionItems = costDistributionItemRepository.findStandingOrderExpenseForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, false, repetitionTypeEnum);
            for (CostDistributionItem standingOrderCostDistributionItem : standingOrderCostDistributionItems) {

                invoiceTemplates.add(standingOrderCostDistributionItem.getInvoice());

                BigDecimal averageMonthCosts = standingOrderCostDistributionItem.getMoneyValue().divide(new BigDecimal(repetitionTypeEnum.getCounterMonths()), RoundingMode.HALF_EVEN);
                sumOfStandingOrderItems = sumOfStandingOrderItems.add(averageMonthCosts);
            }

            List<BigDecimal> repetitionTypesNormalItemsSumList = new ArrayList<>();

            int maxMonthBefore = (secondLastMonthBefore != null ? secondLastMonthBefore : 0);
            int counterMonths = repetitionTypeEnum.getCounterMonths();
            if (counterMonths > maxMonthBefore){
                counterMonths = maxMonthBefore;
            }

            startLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDateTmp = startLocalDateTmp.minusMonths(counterMonths);
            startDateTmp = Date.from(startLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            endLocalDateTmp = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
//            endLocalDateTmp = endLocalDateTmp.plusMonths(repetitionTypeEnum.getCounterMonths());
            endDateTmp = Date.from(endLocalDateTmp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            List<CostDistributionItem> repetitionNormalCostDistributionItems = costDistributionItemRepository.findExpenseForAppUser(appUser.getAppUserId(), startDateTmp, endDateTmp, false, repetitionTypeEnum);
            for (CostDistributionItem repetitionNormalCostDistributionItem : repetitionNormalCostDistributionItems) {
                Invoice invoiceTemplate = invoiceRepository.invoiceTemplateForStandingOrderInvoice(repetitionNormalCostDistributionItem.getInvoice());

                if (!invoiceTemplates.stream().map(Invoice::getInvoiceId).collect(Collectors.toList()).contains(invoiceTemplate.getInvoiceId())){
                    BigDecimal averageMonthCosts = repetitionNormalCostDistributionItem.getMoneyValue().divide(new BigDecimal(repetitionTypeEnum.getCounterMonths()), RoundingMode.HALF_EVEN);
                    repetitionTypesNormalItemsSumList.add(averageMonthCosts);
                }
            }

//            BigDecimal sumOfNormalItems = new BigDecimal(0);
//            BigDecimal sumOfPartRepetitionTypeList = sumOfBigDecimals(repetitionTypesNormalItemsSumList);
//            if (!repetitionTypesNormalItemsSumList.isEmpty()){
//                sumOfNormalItems = sumOfPartRepetitionTypeList.divide(new BigDecimal(repetitionTypesNormalItemsSumList.size()), RoundingMode.HALF_EVEN);
//            }

            sumOfAllRepetitionTypeSums = sumOfAllRepetitionTypeSums.add(sumOfStandingOrderItems);
//            sumOfAllRepetitionTypeSums = sumOfAllRepetitionTypeSums.add(sumOfNormalItems);
            sumOfAllRepetitionTypeSums = sumOfAllRepetitionTypeSums.add(sumOfBigDecimals(repetitionTypesNormalItemsSumList));
        }

        averageResult = averageResult.add(sumOfAllRepetitionTypeSums);

        return averageResult;
    }

    public BigDecimal expenseForEachMonthTillNowDateForUser(AppUser appUser){

        Date nowDate = new Date();

        List<BigDecimal> expenseSumsForEachMonth = new ArrayList<>();

        int lastOneBeforeHasItems = 0;
        for(int i = 6; i >= 0; i--){
            LocalDate startLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDate = startLocalDate.minusMonths(i);
            startLocalDate = startLocalDate.withDayOfMonth(1);
            Date startDate = Date.from(startLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            LocalDate endLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endLocalDate = endLocalDate.minusMonths(i);
            Date endDate = Date.from(endLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            BooleanBuilder booleanBuilder = new BooleanBuilder();
            List<InvoiceCostDistributionItem> invoiceCostDistributionItems = searchService.search(false, true,null,false,true,null,null,null,false,null,null,null,null,null, booleanBuilder, appUser.getAppUserId().toString(), null, true, null, null);
            List<Invoice> invoices = searchService.invoicesInInvoiceCostDistributionItems(invoiceCostDistributionItems);

            BigDecimal sumForMonth = filterSumResultByDate(appUser, startDate, endDate, invoices);

            if (lastOneBeforeHasItems > 0 || i == 0){
                if (sumForMonth.compareTo(new BigDecimal(0)) != 0){
                    expenseSumsForEachMonth.add(sumForMonth);
                }
            }

            lastOneBeforeHasItems = invoices.size();
        }

        BigDecimal overallSum = sumOfBigDecimals(expenseSumsForEachMonth);

        BigDecimal averageResult = new BigDecimal(0);
        if (!expenseSumsForEachMonth.isEmpty()){
            averageResult = overallSum.divide(new BigDecimal(expenseSumsForEachMonth.size()), RoundingMode.HALF_EVEN);
        }

        averageResult = averageResult.multiply(new BigDecimal(-1));

        return averageResult;
    }

    public BigDecimal expenseForEachMonthAfterNowDateForUser(AppUser appUser){

        Date nowDate = new Date();

        List<BigDecimal> expenseSumsForEachMonth = new ArrayList<>();

        int lastOneBeforeHasItems = 0;
        for(int i = 6; i >= 0; i--){
            LocalDate startLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            startLocalDate = startLocalDate.minusMonths(i);
            Date startDate = Date.from(startLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            LocalDate endLocalDate = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
            endLocalDate = endLocalDate.minusMonths(i);
            endLocalDate = endLocalDate.withDayOfMonth(endLocalDate.lengthOfMonth());
            Date endDate = Date.from(endLocalDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

            BooleanBuilder booleanBuilder = new BooleanBuilder();
            List<InvoiceCostDistributionItem> invoiceCostDistributionItems = searchService.search(false, true,null,false,true,null,null,null,false,null,null,null,null,null, booleanBuilder, appUser.getAppUserId().toString(), null, true, null,null);
            List<Invoice> invoices = searchService.invoicesInInvoiceCostDistributionItems(invoiceCostDistributionItems);

            BigDecimal sumForMonth = filterSumResultByDate(appUser, startDate, endDate, invoices);

            if (lastOneBeforeHasItems > 0 || i == 0){
                if (sumForMonth.compareTo(new BigDecimal(0)) != 0){
                    expenseSumsForEachMonth.add(sumForMonth);
                }
            }

            lastOneBeforeHasItems = invoices.size();
        }

        BigDecimal overallSum = sumOfBigDecimals(expenseSumsForEachMonth);

        BigDecimal averageResult = new BigDecimal(0);
        if (!expenseSumsForEachMonth.isEmpty()){
            averageResult = overallSum.divide(new BigDecimal(expenseSumsForEachMonth.size()), RoundingMode.HALF_EVEN);
        }

        averageResult = averageResult.multiply(new BigDecimal(-1));

        return averageResult;
    }

    public BigDecimal sumOfCostDistributionItems(List<CostDistributionItem> costDistributionItems){
        BigDecimal sum = new BigDecimal(0);
        for (CostDistributionItem costDistributionItem : costDistributionItems) {
            if (costDistributionItem.getMoneyValue() != null){
                sum = sum.add(costDistributionItem.getMoneyValue());
            }
        }
        return sum;
    }

    public BigDecimal sumOfBigDecimals(List<BigDecimal> bigDecimals){
        BigDecimal sum = new BigDecimal(0);
        for (BigDecimal bigDecimal : bigDecimals) {
            sum = sum.add(bigDecimal);
        }
        return sum;
    }

    public void generateCategoryToSpecialType(AppUser appUser){

        List<InvoiceCategory> invoiceCategories = invoiceCategoryRepository.findAllByAppUserIsNullOrAppUser(appUser);

        List<BasicData> basicDatasToDelete = basicDataRepository.findAllByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.SPECIAL_TYPE_FOR_CATEGORY);
        basicDataRepository.deleteAll(basicDatasToDelete);

        for (InvoiceCategory invoiceCategory : invoiceCategories) {
            List<Object[]> specialTypes = invoiceRepository.categoryToSpecialTypeStatistic(InvoiceStatusEnum.READY, appUser.getAppUserId().toString(), invoiceCategory.getInvoiceCategoryId());
            if (!specialTypes.isEmpty()){
                Long counter = (Long) specialTypes.get(0)[1];
                Boolean specialType = (Boolean) specialTypes.get(0)[0];

                if (counter >= 2){
                    BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(appUser, BasicDataType.STATISTIC, BasicDataSubType.SPECIAL_TYPE_FOR_CATEGORY, invoiceCategory.getClass().getSimpleName(), invoiceCategory.getInvoiceCategoryId().toString());
                    if (basicData == null){
                        basicData = new BasicData();
                        basicData.setBasicDataId(UUID.randomUUID());
                        basicData.setAppUser(appUser);
                        basicData.setBasicDataType(BasicDataType.STATISTIC);
                        basicData.setBasicDataSubType(BasicDataSubType.SPECIAL_TYPE_FOR_CATEGORY);
                    }

                    String stringBoolean = "false";
                    if (specialType != null && specialType == true){
                        stringBoolean = "true";
                    }

                    basicData.setObject1Class(invoiceCategory.getClass().getSimpleName());
                    basicData.setObject1Id(invoiceCategory.getInvoiceCategoryId().toString());
                    basicData.setObject2Class(specialType.getClass().getSimpleName());
                    basicData.setObject2Id(stringBoolean);

                    basicDataRepository.save(basicData);
                }

            }

        }
    }

    public void generateCategoryToRepetitionType(AppUser appUser){

        List<InvoiceCategory> invoiceCategories = invoiceCategoryRepository.findAllByAppUserIsNullOrAppUser(appUser);

        List<BasicData> basicDatasToDelete = basicDataRepository.findAllByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.REPETITION_TYPE_FOR_CATEGORY);
        basicDataRepository.deleteAll(basicDatasToDelete);

        for (InvoiceCategory invoiceCategory : invoiceCategories) {
            List<Object[]> specialTypes = invoiceRepository.categoryToRepetitionTypeStatistic(InvoiceStatusEnum.READY, appUser.getAppUserId().toString(), invoiceCategory.getInvoiceCategoryId());
            if (!specialTypes.isEmpty()){
                Long counter = (Long) specialTypes.get(0)[1];
                RepetitionTypeEnum repetitionTypeEnum = (RepetitionTypeEnum) specialTypes.get(0)[0];

                if (counter >= 2){
                    BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(appUser, BasicDataType.STATISTIC, BasicDataSubType.REPETITION_TYPE_FOR_CATEGORY, invoiceCategory.getClass().getSimpleName(), invoiceCategory.getInvoiceCategoryId().toString());
                    if (basicData == null){
                        basicData = new BasicData();
                        basicData.setBasicDataId(UUID.randomUUID());
                        basicData.setAppUser(appUser);
                        basicData.setBasicDataType(BasicDataType.STATISTIC);
                        basicData.setBasicDataSubType(BasicDataSubType.REPETITION_TYPE_FOR_CATEGORY);
                    }

                    basicData.setObject1Class(invoiceCategory.getClass().getSimpleName());
                    basicData.setObject1Id(invoiceCategory.getInvoiceCategoryId().toString());
                    basicData.setObject2Class(repetitionTypeEnum.getClass().getSimpleName());
                    basicData.setObject2Id(repetitionTypeEnum.name());

                    basicDataRepository.save(basicData);
                }

            }

        }
    }

    public void generateBusinessPartnerToCategoryRate(AppUser appUser){

        List<BusinessPartner> businessPartners = businessPartnerRepository.findAllByAppUserIsNullOrAppUserAndBasicStatusEnum(appUser, BasicStatusEnum.OK);

        List<BasicData> basicDatasToDelete = basicDataRepository.findAllByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.CATEGORY_FOR_BUSINESS_PARTNER);
        basicDataRepository.deleteAll(basicDatasToDelete);

        for (BusinessPartner businessPartner : businessPartners) {

            Long counterResult = null;
            InvoiceCategory invoiceCategoryResult = null;

            List<Object[]> invoiceCategories = invoiceCategoryRepository.businessPartnerToCategoryStatistic(InvoiceStatusEnum.READY, appUser.getAppUserId().toString(), businessPartner.getBusinessPartnerId());

            if (invoiceCategories != null && !invoiceCategories.isEmpty()) {
                Object[]  invoiceCategoryObject = invoiceCategories.get(0);

                counterResult =  (Long) invoiceCategoryObject[1];
                invoiceCategoryResult = (InvoiceCategory) invoiceCategoryObject[0];
            }

            if (invoiceCategoryResult != null && counterResult != null && counterResult >= 2) {
                BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(appUser, BasicDataType.STATISTIC, BasicDataSubType.CATEGORY_FOR_BUSINESS_PARTNER, businessPartner.getClass().getSimpleName(), businessPartner.getBusinessPartnerId().toString());
                if (basicData == null){
                    basicData = new BasicData();
                    basicData.setBasicDataId(UUID.randomUUID());
                    basicData.setAppUser(appUser);
                    basicData.setBasicDataType(BasicDataType.STATISTIC);
                    basicData.setBasicDataSubType(BasicDataSubType.CATEGORY_FOR_BUSINESS_PARTNER);
                }

                basicData.setObject1Class(businessPartner.getClass().getSimpleName());
                basicData.setObject1Id(businessPartner.getBusinessPartnerId().toString());
                basicData.setObject2Class(invoiceCategoryResult.getClass().getSimpleName());
                basicData.setObject2Id(invoiceCategoryResult.getInvoiceCategoryId().toString());

                basicDataRepository.save(basicData);
            }

        }
    }

    public void generateCategoryToCostDistributionRate(AppUser appUser){

        List<InvoiceCategory> invoiceCategories = invoiceCategoryRepository.findAllByAppUserIsNullOrAppUser(appUser);

        List<CostDistribution> costDistributions = costDistributionRepository.findByCreatedByAndBasicStatusEnum(appUser.getAppUserId().toString(), BasicStatusEnum.OK);
        Map<CostDistribution, List<CostDistributionItem>> itemsForCostDistribution = new HashMap<>();

        for (CostDistribution costDistribution : costDistributions) {
            List<CostDistributionItem> costDistributionItemsOfCostDistribution = costDistributionItemRepository.findByCostDistribution(costDistribution);
            itemsForCostDistribution.put(costDistribution, costDistributionItemsOfCostDistribution);
        }

        List<BasicData> basicDatasToDelete = basicDataRepository.findAllByAppUserAndBasicDataTypeAndBasicDataSubType(appUser, BasicDataType.STATISTIC, BasicDataSubType.COST_DISTRIBUTION_FOR_CATEGORY);
        basicDataRepository.deleteAll(basicDatasToDelete);

        for (InvoiceCategory invoiceCategory : invoiceCategories) {

            Map<CostDistribution, Integer> rankingOfCostDistributions = new HashMap<>();

            List<Invoice> invoicesWithCategory = invoiceRepository.findByCreatedByAndInvoiceStatusEnumAndInvoiceCategory(appUser.getAppUserId().toString(), InvoiceStatusEnum.READY, invoiceCategory);

            if (!invoicesWithCategory.isEmpty()){

                for (Invoice invoice : invoicesWithCategory) {

                    List<CostDistributionItem> costDistributionItemsOfInvoice = invoice.getCostDistributionItems();

                    for (CostDistribution costDistribution : costDistributions) {
                        List<CostDistributionItem> costDistributionItemsOfCostDistribution = itemsForCostDistribution.get(costDistribution);

                        if (autoFillHelperService.areCostDistributionItemListsEqual(costDistributionItemsOfInvoice, costDistributionItemsOfCostDistribution)){
                            if (rankingOfCostDistributions.get(costDistribution) == null){
                                rankingOfCostDistributions.put(costDistribution, 1);
                            }else{
                                rankingOfCostDistributions.replace(costDistribution, rankingOfCostDistributions.get(costDistribution) + 1);
                            }
                        }

                    }

                }

                CostDistribution bestCostDistributionSelection = null;
                Integer bestCounter = 0;
                for (CostDistribution costDistribution : rankingOfCostDistributions.keySet()) {
                    Integer counter = rankingOfCostDistributions.get(costDistribution);

                    if (counter < 2){
                        continue;
                    }

                    if (counter > bestCounter){
                        bestCostDistributionSelection = costDistribution;
                        bestCounter = counter;
                    }
                }

                if (bestCostDistributionSelection != null){
                    BasicData basicData = basicDataRepository.findOneByAppUserAndBasicDataTypeAndBasicDataSubTypeAndObject1ClassAndObject1Id(appUser, BasicDataType.STATISTIC, BasicDataSubType.COST_DISTRIBUTION_FOR_CATEGORY, invoiceCategory.getClass().getSimpleName(), invoiceCategory.getInvoiceCategoryId().toString());
                    if (basicData == null){
                        basicData = new BasicData();
                        basicData.setBasicDataId(UUID.randomUUID());
                        basicData.setAppUser(appUser);
                        basicData.setBasicDataType(BasicDataType.STATISTIC);
                        basicData.setBasicDataSubType(BasicDataSubType.COST_DISTRIBUTION_FOR_CATEGORY);
                    }

                    basicData.setObject1Class(invoiceCategory.getClass().getSimpleName());
                    basicData.setObject1Id(invoiceCategory.getInvoiceCategoryId().toString());
                    basicData.setObject2Class(bestCostDistributionSelection.getClass().getSimpleName());
                    basicData.setObject2Id(bestCostDistributionSelection.getCostDistributionId().toString());

                    basicDataRepository.save(basicData);
                }

            }

        }
    }

}
