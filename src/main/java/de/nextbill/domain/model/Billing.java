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

import de.nextbill.domain.enums.BillingStatusEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.interfaces.PaymentPerson;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
public class Billing extends AuditFields {

    @Id
    private UUID billingId;

    private UUID costPayerId;

    @Enumerated(EnumType.STRING)
    private BillingStatusEnum billingStatusEnum;

    @Enumerated(EnumType.STRING)
    private PaymentPersonTypeEnum costPayerTypeEnum;

    private BigDecimal sumPaid;

    private BigDecimal sumToPay;

    private Boolean isNormalPayment;

    @Transient
    public void setCostPayer(PaymentPerson paymentPerson){
        this.costPayerId = paymentPerson.getPaymentPersonId();
        this.costPayerTypeEnum = paymentPerson.getPaymentPersonEnum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Billing billing = (Billing) o;

        return new EqualsBuilder()
                .append(billingId, billing.billingId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(billingId)
                .toHashCode();
    }
}
