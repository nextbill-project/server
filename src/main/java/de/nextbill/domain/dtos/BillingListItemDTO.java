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

import de.nextbill.domain.model.InvoiceCategory;
import de.nextbill.domain.pojos.PaymentPersonPojo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BillingListItemDTO {

    private PaymentPersonPojo invoicePayer;
    private PaymentPersonPojo costPayer;

    private BigDecimal sumToBePaid;
    private BigDecimal sumToBeChecked;

    private BigDecimal costsFromInvoicePayer;
    private BigDecimal costsFromCostPayer;

    private List<InvoiceCategory> invoiceCategoriesOfInvoicePayer;
    private List<InvoiceCategory> invoiceCategoriesOfCostPayer;

    private List<InvoiceDTO> invoicesForCostPayer = new ArrayList<>();
    private List<InvoiceDTO> invoicesForInvoicePayer = new ArrayList<>();

}
