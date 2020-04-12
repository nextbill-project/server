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

package de.nextbill.domain.model;

import de.nextbill.domain.enums.CorrectionStatus;
import de.nextbill.domain.enums.CostDistributionItemTypeEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.interfaces.PaymentItem;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
public class CostDistributionItem extends AuditFields implements java.io.Serializable, PaymentItem {

    @Id
    private UUID costDistributionItemId;

    @Enumerated(EnumType.STRING)
    private CostDistributionItemTypeEnum costDistributionItemTypeEnum;

    private BigDecimal value;
    private BigDecimal costPaid;

    private UUID payerId;

    @Enumerated(EnumType.STRING)
    private PaymentPersonTypeEnum paymentPersonTypeEnum;

    private Integer position;

    @ManyToOne
    private Invoice invoice;

    @ManyToOne
    private CostDistribution costDistribution;

    private String remarks;

    private BigDecimal moneyValue;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Billing> billings = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private CorrectionStatus correctionStatus;

    @Column(length = 1000000)
    private String articleDTOsAsJson;

    @Transient
    @Override
    public UUID getCostPayerId() {
        return payerId;
    }

    @Transient
    @Override
    public PaymentPersonTypeEnum getPayerTypeEnum() {
        return paymentPersonTypeEnum;
    }

}
