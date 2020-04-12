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

import de.nextbill.domain.enums.BillingStatusEnum;
import de.nextbill.domain.enums.MessageType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class BillingDTO {

    private UUID billingId;

    private PaymentPersonDTO costPayerDTO;

    private PaymentPersonDTO invoicePayerDTO;

    private BillingStatusEnum billingStatusEnum;

    private BigDecimal sumPaid;

    private BigDecimal sumPaidTemp;

    private BigDecimal sumToPay;

    private BigDecimal currentSumToPay;

    private Boolean isNormalPayment;

    private MessageType messageType;

    private String subject;

    private Date createdDate;

    private Boolean compensationPossible;

    private String paypalMeUrl;

    private List<InvoiceDTO> invoicesForCostPayer = new ArrayList<>();

    private List<InvoiceDTO> invoicesForInvoicePayer = new ArrayList<>();

}
