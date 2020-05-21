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
import de.nextbill.domain.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends PagingAndSortingRepository<Invoice, UUID>, AuditFieldsRepository<Invoice> {

    List<Invoice> findByBillings(Billing billing);

    List<Invoice> findByCreatedByAndInvoiceStatusEnumAndInvoiceCategory(String appUser, InvoiceStatusEnum invoiceStatusEnum, InvoiceCategory invoiceCategory);

    List<Invoice> findByInvoiceCategory(InvoiceCategory invoiceCategory);

    List<Invoice> findAllByInvoiceIdInAndLastModifiedAtAfter(List<UUID> id, Date lastModifiedDate);

    @Query("SELECT distinct i FROM Invoice i, StandingOrder st, StandingOrderItem sti WHERE st.invoiceTemplate = i AND sti.standingOrder = st AND sti.createdInvoice = :invoice")
    Invoice invoiceTemplateForStandingOrderInvoice(@Param("invoice") Invoice invoice);

    @Query("SELECT j FROM Invoice j WHERE j.invoiceStatusEnum NOT LIKE :invoiceStatusEnum AND j.createdBy = :appUser " +
            "AND j NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i)")
    List<Invoice> findWithUserAndNotStatusAndNotInvoiceTemplate(@Param("appUser") String appUser, @Param("invoiceStatusEnum") InvoiceStatusEnum invoiceStatusEnum);

    @Query("SELECT j FROM Invoice j WHERE j.invoiceStatusEnum NOT LIKE :invoiceStatusEnum AND j.createdBy = :appUserId")
    Page<Invoice> findWithUserAndNotStatus(@Param("appUserId") String appUser, @Param("invoiceStatusEnum") InvoiceStatusEnum invoiceStatusEnum, Pageable pageable);

    @Query("SELECT j FROM Invoice j WHERE j.createdBy = :appUserId and j.lastModifiedAt >= :modificationDate")
    Page<Invoice> findWithUser(@Param("appUserId") String appUser, @Param("modificationDate") Date modificationDate, Pageable pageable);

    @Query("SELECT j FROM Invoice j WHERE j.createdBy = :appUserId ")
    Page<Invoice> findWithUser(@Param("appUserId") String appUser, Pageable pageable);

    @Query("SELECT j FROM Invoice j WHERE j.invoiceStatusEnum NOT LIKE 'DELETED' AND j.createdBy = :appUserId " +
            "AND j IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i)")
    Page<Invoice> findWithUserAndNotStatusAndIsInvoiceTemplate(@Param("appUserId") String appUser, Pageable pageable);

    @Query("SELECT distinct j FROM Invoice j, CostDistributionItem c WHERE c.invoice = j " +
            "AND j NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) " +
            " AND (" +
            "(j.createdBy = :appUser AND (j.invoiceStatusEnum LIKE 'CHECK' OR j.invoiceStatusEnum LIKE 'ANALYZING' OR j.invoiceStatusEnum LIKE 'WAIT_FOR_UPLOAD') ) OR " +
            "(j.invoiceStatusEnum LIKE 'READY' AND c.payerId IN :userContactIds AND (c.correctionStatus IS NULL OR c.correctionStatus LIKE 'CHECK' OR c.correctionStatus LIKE 'PROBLEM') ) OR " +
            "(j.invoiceStatusEnum LIKE 'READY' AND (j.payerId IN :userContactIds OR j.paymentRecipientId IN :userContactIds) AND (j.correctionStatus IS NULL OR j.correctionStatus LIKE 'CHECK' OR j.correctionStatus LIKE 'PROBLEM') ) " +
            ")")
    Page<Invoice> findAllForCheckList(@Param("appUser") String appUser, @Param("userContactIds") List<UUID> userContactIds, Pageable pageable);

    @Query("SELECT distinct j.invoiceId FROM Invoice j, CostDistributionItem c WHERE c.invoice = j " +
            "AND j NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) " +
            " AND (" +
            "(j.createdBy = :appUser AND j.invoiceStatusEnum LIKE 'READY') OR " +
            "(j.invoiceStatusEnum LIKE 'READY' AND c.payerId IN :userContactIds AND (c.correctionStatus LIKE 'READY' OR c.correctionStatus LIKE 'IGNORE' )) OR " +
            "(j.invoiceStatusEnum LIKE 'READY' AND (j.payerId IN :userContactIds OR j.paymentRecipientId IN :userContactIds) AND (j.correctionStatus IS NULL OR j.correctionStatus LIKE 'READY' OR j.correctionStatus LIKE 'IGNORE') ) " +
            ")")
    List<UUID> findAllIdsForReadyList(@Param("appUser") String appUser, @Param("userContactIds") List<UUID> userContactIds);

    @Query("SELECT MAX(j.dateOfInvoice) FROM Invoice j, CostDistributionItem c WHERE c.invoice = j " +
            "AND j NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) AND j.invoiceId IN :invoiceIds " +
            " AND (" +
            "(j.createdBy = :appUser AND j.invoiceStatusEnum LIKE 'READY') OR " +
            "(j.invoiceStatusEnum LIKE 'READY' AND c.payerId IN :userContactIds AND (c.correctionStatus LIKE 'READY' OR c.correctionStatus LIKE 'IGNORE' )) OR " +
            "(j.invoiceStatusEnum LIKE 'READY' AND (j.payerId IN :userContactIds OR j.paymentRecipientId IN :userContactIds) AND (j.correctionStatus IS NULL OR j.correctionStatus LIKE 'READY' OR j.correctionStatus LIKE 'IGNORE') ) " +
            ")")
    Object findNewestDateForReadyList(@Param("appUser") String appUser, @Param("userContactIds") List<UUID> userContactIds, @Param("invoiceIds") List<UUID> invoiceIds);

    @Query("SELECT MAX(j.dateOfInvoice) FROM Invoice j, CostDistributionItem c WHERE c.invoice = j AND j.dateOfInvoice < :startDate " +
            "AND j NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) AND j.invoiceId IN :invoiceIds " +
            " AND (" +
            "(j.createdBy = :appUser AND j.invoiceStatusEnum LIKE 'READY') OR " +
            "(j.invoiceStatusEnum LIKE 'READY' AND c.payerId IN :userContactIds AND (c.correctionStatus LIKE 'READY' OR c.correctionStatus LIKE 'IGNORE' )) OR " +
            "(j.invoiceStatusEnum LIKE 'READY' AND (j.payerId IN :userContactIds OR j.paymentRecipientId IN :userContactIds) AND (j.correctionStatus IS NULL OR j.correctionStatus LIKE 'READY' OR j.correctionStatus LIKE 'IGNORE') ) " +
            ")")
    Date findNewestDateForReadyList(@Param("appUser") String appUser, @Param("userContactIds") List<UUID> userContactIds, @Param("startDate") Date startDate, @Param("invoiceIds") List<UUID> invoiceIds);

    List<Invoice> findByPaymentRecipientIdAndPaymentRecipientTypeEnumAndInvoiceStatusEnumAndCreatedBy(UUID paymentRecipientId, PaymentPersonTypeEnum paymentRecipientTypeEnum, InvoiceStatusEnum invoiceStatusEnum, AppUser createdBy);

    @Query("SELECT c FROM Invoice c, StandingOrder s WHERE s.invoiceTemplate = c AND s = :standingOrder")
    Invoice findOneByStandingOrder(@Param("standingOrder") StandingOrder standingOrder);

    @Query("SELECT j.specialType, COUNT(j.specialType) as cou FROM Invoice j, InvoiceCategory ic WHERE j.invoiceStatusEnum LIKE :invoiceStatusEnum AND j.createdBy = :appUser AND j.invoiceCategory = ic AND ic.invoiceCategoryId = :invoiceCategory " +
            "AND j NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) GROUP BY j.specialType ORDER BY cou")
    List<Object[]> categoryToSpecialTypeStatistic(@Param("invoiceStatusEnum") InvoiceStatusEnum invoiceStatusEnum, @Param("appUser") String appUser, @Param("invoiceCategory") UUID invoiceCategory);

    @Query("SELECT j.repetitionTypeEnum, COUNT(j.repetitionTypeEnum) as cou FROM Invoice j, InvoiceCategory ic WHERE j.invoiceStatusEnum LIKE :invoiceStatusEnum AND j.createdBy = :appUser AND j.invoiceCategory = ic AND ic.invoiceCategoryId = :invoiceCategory " +
            "AND j NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) GROUP BY j.repetitionTypeEnum ORDER BY cou")
    List<Object[]> categoryToRepetitionTypeStatistic(@Param("invoiceStatusEnum") InvoiceStatusEnum invoiceStatusEnum, @Param("appUser") String appUser, @Param("invoiceCategory") UUID invoiceCategory);

    @Query("SELECT distinct i.createdInvoice, s FROM StandingOrder s JOIN s.standingOrderItems i WHERE i.createdInvoice in :standingOrderItemInvoices")
    List<Object[]> findOneByStandingOrderInvoice(@Param("standingOrderItemInvoices") List<Invoice> standingOrderItemInvoices);

    @Query("SELECT distinct i, s FROM StandingOrder s JOIN s.invoiceTemplate i WHERE i in :invoiceTemplates")
    List<Object[]> findOneByStandingOrderInvoiceTemplate(@Param("invoiceTemplates") List<Invoice> invoiceTemplates);

    @Query("SELECT distinct i FROM Invoice i WHERE i.invoiceImage = :invoiceImage")
    Invoice findInvoiceByInvoiceImage(@Param("invoiceImage") InvoiceImage invoiceImage);
}
