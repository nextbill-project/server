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

import de.nextbill.domain.enums.BasicStatusEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.enums.PublicStatus;
import de.nextbill.domain.interfaces.PaymentPerson;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Data
public class BusinessPartner extends AuditFields implements java.io.Serializable, PaymentPerson {

    private static final long serialVersionUID = 1L;

    @Id
    private UUID businessPartnerId;

    @Column(nullable = false)
    private String businessPartnerName = "";

    @Column(nullable = false)
    private String businessPartnerReceiptName = "";

    @ManyToOne
    private InvoiceCategory invoiceCategory;

    @Enumerated(EnumType.STRING)
    private BasicStatusEnum basicStatusEnum;

    @ManyToOne
    private AppUser appUser;

    @Enumerated(EnumType.STRING)
    private PublicStatus businessPartnerPublicStatus;

    @Enumerated(EnumType.STRING)
    private PublicStatus categoryPublicStatus;

    @Column
    private Long amountUsages;

    @Column
    private Long amountBusinessPartnerCategoryUsage;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BusinessPartner that = (BusinessPartner) o;

        return new EqualsBuilder()
                .append(businessPartnerId, that.businessPartnerId)
                .append(businessPartnerName, that.businessPartnerName)
                .append(businessPartnerReceiptName, that.businessPartnerReceiptName)
                .append(basicStatusEnum, that.basicStatusEnum)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(businessPartnerId)
                .append(businessPartnerName)
                .append(businessPartnerReceiptName)
                .append(basicStatusEnum)
                .toHashCode();
    }

    @Transient
    @Override
    public String getPaymentPersonName() {
        return getBusinessPartnerName();
    }

    @Transient
    @Override
    public UUID getPaymentPersonId() {
        if (getBusinessPartnerId() != null) {
            return getBusinessPartnerId();
        }
        return null;
    }

    @Transient
    @Override
    public PaymentPersonTypeEnum getPaymentPersonEnum() {
        return PaymentPersonTypeEnum.BUSINESS_PARTNER;
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public PaymentPersonTypeEnum getVirtualPaymentPersonEnum() {
        return PaymentPersonTypeEnum.BUSINESS_PARTNER;
    }


}
