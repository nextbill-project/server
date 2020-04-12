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
import de.nextbill.domain.model.CostDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface CostDistributionRepository extends JpaRepository<CostDistribution, UUID>, AuditFieldsRepository<CostDistribution>{
    List<CostDistribution> findByCreatedByAndBasicStatusEnum(String login, BasicStatusEnum basicStatusEnum);

    List<CostDistribution> findByCreatedBy(String login);

    List<CostDistribution> findAllByCostDistributionIdInAndLastModifiedAtAfter(List<UUID> id, Date lastModifiedDate);

//    @Query("SELECT distinct co, COUNT(co) as cou FROM Invoice j, InvoiceCategory ic, CostDistribution co, CostDistributionItem coi WHERE j.invoiceStatusEnum LIKE :invoiceStatusEnum AND j.createdBy = :appUser AND j.invoiceCategory = ic " +
//            "AND j NOT IN (SELECT i FROM Invoice i, StandingOrder s JOIN s.invoiceTemplate si WHERE si = i) " +
//            "AND coi.costDistribution IS NULL AND coi.invoice = j AND  " +
//            "GROUP BY ic ORDER BY cou")
//    List<Object[]> categoryToCostDistributionRate(@Param("invoiceStatusEnum") InvoiceStatusEnum invoiceStatusEnum, @Param("appUser") AppUser appUser, @Param("businessPartner") UUID businessPartner);
}
