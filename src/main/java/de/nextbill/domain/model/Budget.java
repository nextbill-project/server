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

import de.nextbill.domain.enums.BudgetRepetitionType;
import de.nextbill.domain.enums.PaymentTypeEnum;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
public class Budget extends AuditFields {

    @Id
    private UUID budgetId;

    private String name;

    @NotNull
    private BigDecimal sum;

    private BigDecimal currentSum;

    @Enumerated(EnumType.STRING)
    private BudgetRepetitionType budgetRepetitionType;

    private Boolean specialType;

    private String remarks;

    @ManyToOne
    private AppUser appUser;

    @Enumerated(EnumType.STRING)
    private PaymentTypeEnum paymentTypeEnum;

    private String filterText;

    private BigDecimal lastSum;

    private Date lastExceeding;

    private BigDecimal lastExceedingSum;

}
