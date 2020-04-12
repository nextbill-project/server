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

import de.nextbill.domain.dtos.StandingOrderDTO;
import de.nextbill.domain.enums.FirebaseMessageType;
import de.nextbill.domain.enums.InvoiceSource;
import de.nextbill.domain.enums.RepetitionTypeEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.model.StandingOrder;
import de.nextbill.domain.model.StandingOrderItem;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.InvoiceRepository;
import de.nextbill.domain.repositories.StandingOrderItemRepository;
import de.nextbill.domain.repositories.StandingOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
public class StandingOrderService {

    @Autowired
    private StandingOrderRepository standingOrderRepository;

    @Autowired
    private StandingOrderItemRepository standingOrderItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CopyService copyService;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private AppUserRepository appUserRepository;

    public void createStandingOrder(StandingOrderDTO standingOrderDTO, Invoice invoiceTemplate, AppUser currentUser, Optional<Boolean> deleteInvoiceTemplate){

        LocalDate startDateLocal = LocalDateTime.ofInstant(standingOrderDTO.getStartDate().toInstant(), ZoneId.systemDefault()).toLocalDate();

        Invoice createdInvoiceTemplate = copyService.copyInvoiceForStandingOrder(invoiceTemplate, startDateLocal, standingOrderDTO.getFutureInvoiceTemplateId(), standingOrderDTO.getIsAlwaysPaid(), InvoiceSource.STANDING_ORDER);

        StandingOrder standingOrder = new StandingOrder();

        UUID standingOrderId = standingOrderDTO.getStandingOrderId();
        if (standingOrderId == null){
            standingOrderId = UUID.randomUUID();
        }
        standingOrder.setStandingOrderId(standingOrderId);
        standingOrder.setRepetitionTypeEnum(standingOrderDTO.getRepetitionTypeEnum());
        standingOrder.setStartDate(standingOrderDTO.getStartDate());
        standingOrder = standingOrderRepository.save(standingOrder);

        standingOrder.setInvoiceTemplate(createdInvoiceTemplate);
        createdInvoiceTemplate.setStandingOrder(standingOrder);
        invoiceRepository.save(createdInvoiceTemplate);
        standingOrder.setInvoiceTemplate(createdInvoiceTemplate);
        standingOrder = standingOrderRepository.save(standingOrder);

        StandingOrderItem standingOrderItem = new StandingOrderItem();
        standingOrderItem.setStandingOrderItemId(UUID.randomUUID());

        Date dateCreated = new Date();
        if (createdInvoiceTemplate.getDateOfInvoice() != null){
            dateCreated = invoiceTemplate.getDateOfInvoice();
        }
        standingOrderItem.setDateCreated(dateCreated);
        standingOrderItem.setCreatedInvoice(invoiceTemplate);
        standingOrderItem.setStandingOrder(standingOrder);
        standingOrderItemRepository.save(standingOrderItem);

        firebaseService.sendDataMessage(currentUser, FirebaseMessageType.IMAGE_OCR_COMPLETED, createdInvoiceTemplate.getInvoiceId().toString(), null, null);

        generateStandingOrderInvoices(standingOrder);

        if (deleteInvoiceTemplate.isPresent() && deleteInvoiceTemplate.get()){
            invoiceService.deleteInvoice(invoiceTemplate, false, false);
            budgetService.updateBudgetsIfNecessary(invoiceTemplate);
        }
    }

    @Transactional
    public void generateStandingOrderInvoices() {
        List<StandingOrder> standingOrderList = standingOrderRepository.findAll();

        for (StandingOrder standingOrder : standingOrderList) {

            if (standingOrder.getCreatedBy() != null){
                AppUser appUser = appUserRepository.findOneByAppUserId(UUID.fromString(standingOrder.getCreatedBy()));
                if (appUser.getDeleted() != null && appUser.getDeleted()){
                    continue;
                }
            }

            generateStandingOrderInvoices(standingOrder);
        }
    }

    public void generateStandingOrderInvoices(StandingOrder standingOrder){
        LocalDate dateStart = LocalDateTime.ofInstant(standingOrder.getStartDate().toInstant(), ZoneId.systemDefault()).toLocalDate();
        LocalDate dateEnd = LocalDateTime.now().toLocalDate();

        List<LocalDate> dateTimesToCheck = new ArrayList<>();

        LocalDate tmpDateTime = dateStart;
        while(tmpDateTime.isBefore(dateEnd) || tmpDateTime.isEqual(dateEnd)){

            dateTimesToCheck.add(tmpDateTime);

            if (RepetitionTypeEnum.MONTHLY.equals(standingOrder.getRepetitionTypeEnum())) {
                tmpDateTime = tmpDateTime.plusMonths(1);
            }else if (RepetitionTypeEnum.ANNUALLY.equals(standingOrder.getRepetitionTypeEnum())) {
                tmpDateTime = tmpDateTime.plusYears(1);
            }else if (RepetitionTypeEnum.HALF_YEAR.equals(standingOrder.getRepetitionTypeEnum())) {
                tmpDateTime = tmpDateTime.plusMonths(6);
            }else if (RepetitionTypeEnum.QUARTER.equals(standingOrder.getRepetitionTypeEnum())) {
                tmpDateTime = tmpDateTime.plusMonths(4);
            }
        }

        for (StandingOrderItem standingOrderItem : standingOrderItemRepository.findItemsForStandingOrder(standingOrder.getStandingOrderId())) {
            LocalDate dateCreatedByStandingOrder = LocalDateTime.ofInstant(standingOrderItem.getDateCreated().toInstant(), ZoneId.systemDefault()).toLocalDate();
            if (standingOrderItem.getDateCreated() != null &&
                    dateTimesToCheck.contains(dateCreatedByStandingOrder)){
                dateTimesToCheck.remove(dateCreatedByStandingOrder);
            }
        }

        Invoice invoiceTemplate = invoiceRepository.findOneByStandingOrder(standingOrder);

        if (invoiceTemplate == null){
            return;
        }

        Date nextDay = nextDateAfterNowOfStandingOrder(standingOrder);
        if (nextDay != null){
            invoiceTemplate.setDateOfInvoice(nextDay);
            invoiceRepository.save(invoiceTemplate);
        }

        for (LocalDate localDateTime : dateTimesToCheck) {

            Invoice newInvoice = copyService.copyInvoiceForStandingOrder(invoiceTemplate, localDateTime, null, null, InvoiceSource.STANDING_ORDER);

            StandingOrderItem standingOrderItem = new StandingOrderItem();
            standingOrderItem.setStandingOrderItemId(UUID.randomUUID());
            standingOrderItem.setCreatedInvoice(newInvoice);
            standingOrderItem.setStandingOrder(standingOrder);
            standingOrderItem.setDateCreated(Date.from(localDateTime.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));

            standingOrderItemRepository.save(standingOrderItem);
        }
    }

    public Date nextDateAfterNowOfStandingOrder(StandingOrder standingOrder){

        LocalDate startDateLocal = LocalDateTime.ofInstant(standingOrder.getStartDate().toInstant(), ZoneId.systemDefault()).toLocalDate();

        Invoice invoiceTemplate = invoiceRepository.findOneByStandingOrder(standingOrder);

        if (invoiceTemplate == null){
            return null;
        }

        if (standingOrder.getStartDate().after(new Date())){
            return standingOrder.getStartDate();
        }

        Date nowDate = new Date();
        LocalDate tmpDateTime = LocalDateTime.ofInstant(nowDate.toInstant(), ZoneId.systemDefault()).toLocalDate();
        while(tmpDateTime.isAfter(startDateLocal) || tmpDateTime.isEqual(startDateLocal)){

            if (RepetitionTypeEnum.MONTHLY.equals(standingOrder.getRepetitionTypeEnum())) {
                startDateLocal = startDateLocal.plusMonths(1);
            }else if (RepetitionTypeEnum.ANNUALLY.equals(standingOrder.getRepetitionTypeEnum())) {
                startDateLocal = startDateLocal.plusYears(1);
            }else if (RepetitionTypeEnum.HALF_YEAR.equals(standingOrder.getRepetitionTypeEnum())) {
                startDateLocal = startDateLocal.plusMonths(6);
            }else if (RepetitionTypeEnum.QUARTER.equals(standingOrder.getRepetitionTypeEnum())) {
                startDateLocal = startDateLocal.plusMonths(4);
            }
        }

        Date newLocalDate = Date.from(startDateLocal.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        return newLocalDate;

    }
}
