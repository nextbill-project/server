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

import de.nextbill.domain.enums.RepetitionTypeEnum;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class StandingOrder extends AuditFields {

    @Id
    private UUID standingOrderId;

    @OneToMany(mappedBy = "standingOrder")
    private List<StandingOrderItem> standingOrderItems = new ArrayList<>();;

    @OneToOne()
    private Invoice invoiceTemplate;

    private Date startDate;

    @Enumerated(EnumType.STRING)
    private RepetitionTypeEnum repetitionTypeEnum;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        StandingOrder that = (StandingOrder) o;

        return new EqualsBuilder()
                .append(standingOrderId, that.standingOrderId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(standingOrderId)
                .toHashCode();
    }
}
