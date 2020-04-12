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

import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.InvoiceCategoryType;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.InvoiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface InvoiceCategoryRepository extends JpaRepository<InvoiceCategory, UUID>, AuditFieldsRepository<InvoiceCategory> {

    List<InvoiceCategory> findAllByAppUserIsNullOrAppUser(AppUser appUser);

    List<InvoiceCategory> findAllByAppUserIsNullAndBasicStatusEnum(BasicStatusEnum basicStatusEnum);

    List<InvoiceCategory> findAllByInvoiceCategoryNameAndInvoiceCategoryTypeAndAppUserIsNull(String invoiceCategoryName, InvoiceCategoryType invoiceCategoryType);

    List<InvoiceCategory> findAllByInvoiceCategoryNameInAndInvoiceCategoryTypeAndParentInvoiceCategoryIsNullAndAppUserIsNull(List<String> invoiceCategoryName, InvoiceCategoryType invoiceCategoryType);

    InvoiceCategory findOneByInvoiceCategoryNameAndInvoiceCategoryTypeAndParentInvoiceCategoryIsNullAndAppUserIsNull(String invoiceCategoryName, InvoiceCategoryType invoiceCategoryType);

    List<InvoiceCategory> findAllByInvoiceCategoryIdInAndLastModifiedAtAfter(List<UUID> id, Date lastModifiedDate);

    @Query("SELECT j FROM InvoiceCategory j WHERE j.basicStatusEnum = :basicStatusEnum AND (j.appUser = :appUser OR j.appUser IS NULL)")
    List<InvoiceCategory> findAllByAppUserIsNullOrAppUserAndBasicStatusEnum(@Param("appUser") AppUser appUser, @Param("basicStatusEnum") BasicStatusEnum basicStatusEnum);

    @Query("SELECT ic, COUNT(ic) as cou FROM Invoice j, InvoiceCategory ic WHERE j.invoiceStatusEnum LIKE :invoiceStatusEnum AND j.createdBy = :appUser AND j.invoiceCategory = ic " +
            "AND (j.payerId = :businessPartnerId OR j.paymentRecipientId = :businessPartnerId) " +
            "AND j NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) GROUP BY ic ORDER BY cou DESC")
    List<Object[]> businessPartnerToCategoryStatistic(@Param("invoiceStatusEnum") InvoiceStatusEnum invoiceStatusEnum, @Param("appUser") String appUser, @Param("businessPartnerId") UUID businessPartnerId);

}
