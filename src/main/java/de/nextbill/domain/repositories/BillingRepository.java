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

import de.nextbill.domain.enums.BillingStatusEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.model.Billing;
import de.nextbill.domain.model.CostDistributionItem;
import de.nextbill.domain.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BillingRepository extends JpaRepository<Billing, UUID>, AuditFieldsRepository<Billing>{

    List<Billing> findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum paymentPersonTypeEnum, BillingStatusEnum billingStatusEnum, Boolean isNormalPayment, List<UUID> costPayerIds);

    List<Billing> findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(String login, BillingStatusEnum billingStatusEnum, Boolean isNormalPayment, List<UUID> costPayerIds);

    @Query("SELECT distinct b FROM Invoice i left join i.billings be, Billing b WHERE i = :invoice AND b = be AND Not (b.billingStatusEnum = 'ABORTED' or b.billingStatusEnum = 'ARCHIVED')")
    List<Billing> findByInvoice(@Param("invoice") Invoice invoice);

    @Query("SELECT distinct b FROM CostDistributionItem c left join c.billings be, Billing b WHERE c = :costDistributionItem AND b = be AND Not (b.billingStatusEnum = 'ABORTED' or b.billingStatusEnum = 'ARCHIVED')")
    List<Billing> findByCostDistributionItem(@Param("costDistributionItem") CostDistributionItem costDistributionItem);
}
