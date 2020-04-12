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


import de.nextbill.domain.enums.InvoiceFailureTypeEnum;
import lombok.Data;

import java.util.UUID;

@Data
public class InvoiceFailureDTO implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private UUID invoiceFailureId;

    private InvoiceFailureTypeEnum invoiceFailureTypeEnum;

    private String message;

    private UUID invoiceId;

}
