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

package de.nextbill.domain.dtos;

import de.nextbill.domain.enums.BudgetRepetitionType;
import de.nextbill.domain.enums.PaymentTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class BudgetDTO {

    private UUID budgetId;

    private String name;

    private BigDecimal sum;

    private BigDecimal currentSum;

    private String filterText;

    private BudgetRepetitionType budgetRepetitionType;

    private Boolean specialType;

    private String remarks;

    private PaymentTypeEnum paymentTypeEnum;

    private List<PaymentPersonDTO> payerDTOS = new ArrayList<>();

    private List<InvoiceCategoryDTO> invoiceCategoryDTOS = new ArrayList<>();

    private BigDecimal lastSum;

    private Date lastExceeding;

    private BigDecimal lastExceedingSum;

}
