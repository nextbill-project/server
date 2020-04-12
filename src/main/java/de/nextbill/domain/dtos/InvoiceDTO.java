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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import de.nextbill.domain.enums.*;
import de.nextbill.domain.model.Invoice;
import de.nextbill.domain.utils.views.MappingView;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceDTO implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @JsonView(MappingView.Summary.class)
    private UUID standingOrderInvoiceTemplateId;

    @JsonView(MappingView.Detail.class)
    private Date standingOrderStartDate;

    @JsonView(MappingView.Detail.class)
    private UUID invoiceId;

    @JsonView(MappingView.Detail.class)
    private UUID createdById;

    @JsonView(MappingView.Summary.class)
    private Date dateOfInvoice;

    @JsonView(MappingView.Detail.class)
    private InvoiceStatusEnum invoiceStatusEnum;

    @JsonView(MappingView.Detail.class)
    private Double sumOfInvoice;

    @JsonView(MappingView.Detail.class)
    private UUID invoiceImageId;

    @JsonView(MappingView.Detail.class)
    private Boolean rotateImage;

    @JsonView(MappingView.Detail.class)
    private UUID payerId;

    @JsonView(MappingView.Detail.class)
    private PaymentPersonTypeEnum payerTypeEnum;

    @JsonView(MappingView.Detail.class)
    private UUID paymentRecipientId;

    @JsonView(MappingView.Detail.class)
    private PaymentPersonTypeEnum paymentRecipientTypeEnum;

    @JsonView(MappingView.Detail.class)
    private Boolean specialType;

    @JsonView(MappingView.Detail.class)
    private String remarks;

    @JsonView(MappingView.Detail.class)
    private RepetitionTypeEnum repetitionTypeEnum;

    @JsonView(MappingView.Detail.class)
    private PaymentTypeEnum paymentTypeEnum;

    @JsonView(MappingView.Summary.class)
    private InvoiceSource invoiceSource;

    @JsonView(MappingView.Detail.class)
    private CorrectionStatus correctionStatus;

    @JsonView(MappingView.Detail.class)
    private BigDecimal costPaid;

    @JsonView(MappingView.Detail.class)
    private InvoiceCategoryDTO invoiceCategoryDTO;

    @JsonView(MappingView.Detail.class)
    private List<CostDistributionItemDTO> costDistributionItemDTOs;

    @JsonView(MappingView.Detail.class)
    private List<InvoiceFailureDTO> invoiceFailureDTOs;

    @JsonView(MappingView.Summary.class)
    private MainFunctionEnum mainFunctionEnum;

    @JsonView(MappingView.Summary.class)
    private InvoiceWorkflowMode invoiceWorkflowMode;

    @JsonView(MappingView.Detail.class)
    private Boolean reverseInvoice;

    @JsonView(MappingView.Summary.class)
    private PaymentPersonDTO payerDTO;

    @JsonView(MappingView.Summary.class)
    private PaymentPersonDTO paymentRecipientDTO;

    @JsonView(MappingView.Summary.class)
    private PaymentPersonDTO createdByDTO;

    @JsonView(MappingView.Summary.class)
    private BigDecimal moneyValue;

    @JsonView(MappingView.Summary.class)
    private BigDecimal debtValue;

    @JsonView(MappingView.Summary.class)
    private BigDecimal restDebtValue;

    @JsonView(MappingView.Detail.class)
    private List<ArticleDTO> articleDTOs;

    @JsonView(MappingView.Summary.class)
    private String invoiceFailureMessage;

    public static InvoiceDTO generateDTO(Invoice invoice){
        InvoiceDTO invoiceDTO = new InvoiceDTO();

        invoiceDTO.setInvoiceId(invoice.getInvoiceId());
        invoiceDTO.setDateOfInvoice(invoice.getDateOfInvoice());
        invoiceDTO.setInvoiceStatusEnum(invoice.getInvoiceStatusEnum());
        if (invoice.getSumOfInvoice() != null) invoiceDTO.setSumOfInvoice(invoice.getSumOfInvoice().doubleValue());
        invoiceDTO.setPayerId(invoice.getPayerId());
        invoiceDTO.setPayerTypeEnum(invoice.getPayerTypeEnum());
        invoiceDTO.setPaymentRecipientId(invoice.getPaymentRecipientId());
        invoiceDTO.setPaymentRecipientTypeEnum(invoice.getPaymentRecipientTypeEnum());
        invoiceDTO.setSpecialType(invoice.getSpecialType());
        invoiceDTO.setRemarks(invoice.getRemarks());
        invoiceDTO.setRepetitionTypeEnum(invoice.getRepetitionTypeEnum());
        invoiceDTO.setInvoiceSource(invoice.getInvoiceSource());
        invoiceDTO.setCorrectionStatus(invoice.getCorrectionStatus());
        invoiceDTO.setCostPaid(invoice.getCostPaid());
        invoiceDTO.setPaymentTypeEnum(invoice.getPaymentTypeEnum() != null ? invoice.getPaymentTypeEnum() : PaymentTypeEnum.NOT_DEFINED);

        return invoiceDTO;
    }

}
