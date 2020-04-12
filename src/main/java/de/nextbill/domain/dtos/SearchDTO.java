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

import de.nextbill.domain.enums.PaymentTypeEnum;
import de.nextbill.domain.enums.RepetitionTypeEnum;
import de.nextbill.domain.pojos.PaymentPersonPojo;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class SearchDTO {

    private Boolean incomeMainFunctionEnum;
    private Boolean expenseMainFunctionEnum;
    private Boolean standingOrder;
    private PaymentPersonPojo payerPerson;
    private PaymentPersonPojo paymentRecipientPerson;
    private PaymentPersonPojo costPayer;
    private Boolean specialType;
    private RepetitionTypeEnum repetitionTypeEnum;
    private PaymentTypeEnum paymentTypeEnum;
    private InvoiceCategoryDTO invoiceCategoryDTO;
    private UUID costDistributionId;
    private Date startDate;
    private Date endDate;
    private String remarks;
    private String fullText;

    private List<UUID> invoiceCategoryDTOs;
    private List<UUID> costPayers;
}
