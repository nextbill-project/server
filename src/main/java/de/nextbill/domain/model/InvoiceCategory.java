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
import de.nextbill.domain.enums.InvoiceCategoryType;
import lombok.Data;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Data
public class InvoiceCategory extends AuditFields implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private UUID invoiceCategoryId;

    private String invoiceCategoryName;

    @ManyToOne
    private InvoiceCategory parentInvoiceCategory;

    @ManyToOne
    private AppUser appUser;

    @Enumerated(EnumType.STRING)
    private BasicStatusEnum basicStatusEnum;

    @Enumerated(EnumType.STRING)
    private InvoiceCategoryType invoiceCategoryType;

}
