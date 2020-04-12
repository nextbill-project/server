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

import de.nextbill.domain.enums.*;
import de.nextbill.domain.interfaces.PaymentItem;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Data
public class Invoice extends AuditFields implements java.io.Serializable, PaymentItem {

    private static final long serialVersionUID = 1L;

    @Id
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    private InvoiceStatusEnum invoiceStatusEnum;

    @Enumerated(EnumType.STRING)
    private InvoiceSource invoiceSource;

    private Date dateOfInvoice;

    @Column(precision = 10, scale = 2)
    private BigDecimal sumOfInvoice;

    private UUID payerId;

    @Enumerated(EnumType.STRING)
    private PaymentPersonTypeEnum payerTypeEnum;

    private UUID paymentRecipientId;

    @ManyToOne(fetch = FetchType.EAGER)
    private InvoiceImage invoiceImage;

    @OneToOne(mappedBy = "invoiceTemplate")
    private StandingOrder standingOrder;

    @Enumerated(EnumType.STRING)
    private PaymentPersonTypeEnum paymentRecipientTypeEnum;

    @Enumerated(EnumType.STRING)
    private RepetitionTypeEnum repetitionTypeEnum;

    @Enumerated(EnumType.STRING)
    private PaymentTypeEnum paymentTypeEnum;

    @ManyToOne
    private InvoiceCategory invoiceCategory;

    @ManyToMany
    private Set<Billing> billings = new HashSet<>();

    private Boolean specialType;

    @Column(length = 1000000)
    private String remarks;

    @Enumerated(EnumType.STRING)
    private CorrectionStatus correctionStatus;

    private BigDecimal costPaid;

    @Column(length = 1000000)
    private String scansioResultData;

    @Column(length = 1000000)
    private String ocrFullText;

    @Column(length = 1000000)
    private String articleDTOsAsJson;

    @OneToMany(mappedBy = "invoice", fetch = FetchType.EAGER)
    private List<CostDistributionItem> costDistributionItems;

    @Transient
    @Override
    public UUID getCostPayerId() {
        return payerId;
    }

    @Transient
    @Override
    public BigDecimal getMoneyValue() {
        if (sumOfInvoice != null){
            return sumOfInvoice;
        }
        return new BigDecimal(0);
    }
}
