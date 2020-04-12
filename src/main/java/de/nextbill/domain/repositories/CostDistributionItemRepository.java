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

package de.nextbill.domain.repositories;

import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.enums.RepetitionTypeEnum;
import de.nextbill.domain.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface CostDistributionItemRepository extends JpaRepository<CostDistributionItem, UUID>, CostDistributionItemRepositoryCustomMethods, AuditFieldsRepository<CostDistributionItem>{

    List<CostDistributionItem> findByInvoice(Invoice invoice);
    List<CostDistributionItem> findByInvoiceIn(List<Invoice> invoices);
    List<CostDistributionItem> findByCostDistributionIn(List<CostDistribution> costDistributions);
    List<CostDistributionItem> findByCostDistribution(CostDistribution costDistribution);
    List<CostDistributionItem> findAllByCostDistributionItemIdInAndLastModifiedAtAfter(List<UUID> id, Date lastModifiedDate);

    @Query("SELECT c FROM CostDistributionItem c, Invoice i, UserContact uc WHERE c.invoice = i.invoiceId " +
            "AND ((c.payerId = uc.userContactId AND uc.appUserContact = :payerId) OR (i.payerId = uc.userContactId AND uc.appUserContact = :payerId) OR (i.paymentRecipientId = uc.userContactId AND uc.appUserContact = :payerId)) AND " +
            " i is not null AND i NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) AND (c.lastModifiedAt >= :lastModified OR i.lastModifiedAt >= :lastModified)")
    List<CostDistributionItem> findByPayerAndInvoiceNotNullAndNoStandingOrder(@Param("payerId") AppUser appUser, @Param("lastModified") Date modificationDate);

    @Query("SELECT c FROM CostDistributionItem c, Invoice i, UserContact uc WHERE c.invoice = i.invoiceId AND i.invoiceStatusEnum NOT LIKE 'DELETED' " +
            "AND ((c.payerId = uc.userContactId AND uc.appUserContact = :payerId) OR (i.payerId = uc.userContactId AND uc.appUserContact = :payerId) OR (i.paymentRecipientId = uc.userContactId AND uc.appUserContact = :payerId)) AND " +
            " i is not null AND i NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) ")
    List<CostDistributionItem> findByPayerAndInvoiceNotNullAndNoStandingOrder(@Param("payerId") AppUser appUser);

    @Query("SELECT c FROM CostDistributionItem c, Invoice i, UserContact uc WHERE c.paymentPersonTypeEnum = :paymentPersonTypeEnum AND c.invoice = i.invoiceId AND i.invoiceStatusEnum NOT LIKE 'DELETED' " +
            "AND ((c.payerId = uc.userContactId AND uc.appUserContact = :payerId) OR (i.payerId = uc.userContactId AND uc.appUserContact = :payerId) OR (i.paymentRecipientId = uc.userContactId AND uc.appUserContact = :payerId)) AND " +
            " i is not null AND i NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) ")
    List<CostDistributionItem> findByPayerAndInvoiceNotNullAndNoStandingOrderAndPaymentPersonTypeEnum(@Param("payerId") AppUser appUser, @Param("paymentPersonTypeEnum") PaymentPersonTypeEnum paymentPersonTypeEnum);

    @Query("SELECT DISTINCT c FROM CostDistributionItem c, Invoice i, UserContact uc, AppUser ap WHERE c.invoice = i.invoiceId AND i.paymentRecipientTypeEnum = 'USER' " +
            "AND (c.payerId = :appUserId OR (c.payerId = uc.userContactId AND uc.appUserContact = ap AND ap.appUserId = :appUserId))" +
            "AND (i.dateOfInvoice >= :dateStart AND i.dateOfInvoice <= :dateEnd) AND i.invoiceStatusEnum = :invoiceStatusEnum AND i.specialType = :isSpecialType AND i.repetitionTypeEnum = :repetitionTypeEnum " +
            "AND i IS NOT NULL AND i NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i)")
    List<CostDistributionItem> findIncomeForAppUser(@Param("appUserId") UUID appUserId, @Param("dateStart") Date startDate, @Param("dateEnd") Date endDate, @Param("isSpecialType") Boolean isSpecialType, @Param("repetitionTypeEnum") RepetitionTypeEnum repetitionTypeEnum, @Param("invoiceStatusEnum") InvoiceStatusEnum invoiceStatusEnum);

    @Query("SELECT DISTINCT c FROM CostDistributionItem c, Invoice i, UserContact uc, AppUser ap WHERE c.invoice = i.invoiceId AND i.paymentRecipientTypeEnum = 'USER' " +
            "AND (c.payerId = :appUserId OR (c.payerId = uc.userContactId AND uc.appUserContact = ap AND ap.appUserId = :appUserId))" +
            "AND (i.dateOfInvoice >= :dateStart AND i.dateOfInvoice <= :dateEnd) AND i.invoiceStatusEnum = 'CHECK' AND i.specialType = :isSpecialType AND i.repetitionTypeEnum = :repetitionTypeEnum " +
            "AND i IS NOT NULL AND i IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i)")
    List<CostDistributionItem> findStandingOrderIncomeForAppUser(@Param("appUserId") UUID appUserId, @Param("dateStart") Date startDate, @Param("dateEnd") Date endDate, @Param("isSpecialType") Boolean isSpecialType, @Param("repetitionTypeEnum") RepetitionTypeEnum repetitionTypeEnum);

    @Query("SELECT DISTINCT c FROM CostDistributionItem c, Invoice i, UserContact uc, AppUser ap WHERE c.invoice = i.invoiceId AND i.payerTypeEnum = 'USER' " +
            "AND (c.payerId = :appUserId OR (c.payerId = uc.userContactId AND uc.appUserContact = ap AND ap.appUserId = :appUserId))" +
            "AND (i.dateOfInvoice > :dateStart AND i.dateOfInvoice <= :dateEnd) AND i.invoiceStatusEnum = 'READY' AND i.specialType = :isSpecialType AND i.repetitionTypeEnum = :repetitionTypeEnum " +
            "AND i IS NOT NULL AND i NOT IN (SELECT inv FROM Invoice inv, StandingOrder s JOIN s.invoiceTemplate si WHERE si = inv) ")
    List<CostDistributionItem> findExpenseForAppUser(@Param("appUserId") UUID appUserId, @Param("dateStart") Date startDate, @Param("dateEnd") Date endDate, @Param("isSpecialType") Boolean isSpecialType, @Param("repetitionTypeEnum") RepetitionTypeEnum repetitionTypeEnum);

    @Query("SELECT DISTINCT c FROM CostDistributionItem c, Invoice i, UserContact uc, AppUser ap WHERE c.invoice = i.invoiceId AND i.payerTypeEnum = 'USER' " +
            "AND (c.payerId = :appUserId OR (c.payerId = uc.userContactId AND uc.appUserContact = ap AND ap.appUserId = :appUserId))" +
            "AND (i.dateOfInvoice >= :dateStart AND i.dateOfInvoice <= :dateEnd) AND i.invoiceStatusEnum = 'CHECK' AND i.specialType = :isSpecialType AND i.repetitionTypeEnum = :repetitionTypeEnum " +
            "AND i IS NOT NULL AND i IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i)")
    List<CostDistributionItem> findStandingOrderExpenseForAppUser(@Param("appUserId") UUID appUserId, @Param("dateStart") Date startDate, @Param("dateEnd") Date endDate, @Param("isSpecialType") Boolean isSpecialType, @Param("repetitionTypeEnum") RepetitionTypeEnum repetitionTypeEnum);

    List<CostDistributionItem> findByBillings(Billing billing);
}
