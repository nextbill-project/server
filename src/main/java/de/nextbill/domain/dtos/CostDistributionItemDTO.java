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

import de.nextbill.domain.enums.CorrectionStatus;
import de.nextbill.domain.enums.CostDistributionItemTypeEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.model.CostDistributionItem;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CostDistributionItemDTO {

    private UUID costDistributionItemId;
    private CostDistributionItemTypeEnum costDistributionItemTypeEnum;
    private Double value;
    private Double costPaid;
    private UUID payerId;
    private PaymentPersonTypeEnum paymentPersonTypeEnum;
    private Integer position;
    private UUID invoiceId;
    private UUID costDistributionId;
    private Double moneyValue;
    private String remarks;
    private CorrectionStatus correctionStatus;

    private PaymentPersonDTO payerDTO;

    private List<ArticleDTO> articleDTOs;

    public static CostDistributionItemDTO generateDTO(CostDistributionItem costDistributionItem){
        CostDistributionItemDTO costDistributionItemDTO = new CostDistributionItemDTO();

        costDistributionItemDTO.setCostDistributionItemId(costDistributionItem.getCostDistributionItemId());
        costDistributionItemDTO.setCostDistributionItemTypeEnum(costDistributionItem.getCostDistributionItemTypeEnum());
        if (costDistributionItem.getValue() != null) costDistributionItemDTO.setValue(costDistributionItem.getValue().doubleValue());
        if (costDistributionItem.getCostPaid() != null) costDistributionItemDTO.setCostPaid(costDistributionItem.getCostPaid().doubleValue());
        costDistributionItemDTO.setPayerId(costDistributionItem.getPayerId());
        costDistributionItemDTO.setPaymentPersonTypeEnum(costDistributionItem.getPaymentPersonTypeEnum());
        costDistributionItemDTO.setPosition(costDistributionItem.getPosition());
        if (costDistributionItem.getMoneyValue() != null) costDistributionItemDTO.setMoneyValue(costDistributionItem.getMoneyValue().doubleValue());
        costDistributionItemDTO.setRemarks(costDistributionItem.getRemarks());
        costDistributionItemDTO.setCorrectionStatus(costDistributionItem.getCorrectionStatus());

        return costDistributionItemDTO;
    }

}
