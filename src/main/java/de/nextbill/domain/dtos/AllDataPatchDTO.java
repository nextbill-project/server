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

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class AllDataPatchDTO {

    private Date lastModifiedDateFromServer;
    private List<DatabaseChangeDTO<AppUserDTO>> appUserDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<BasicDataDTO>> basicDataDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<BusinessPartnerDTO>> businessPartnerDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<CostDistributionDTO>> costDistributionDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<CostDistributionItemDTO>> costDistributionItemDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<InvoiceDTO>> invoiceDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<InvoiceCategoryDTO>> invoiceCategorieDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<InvoiceFailureDTO>> invoiceFailureDTOs = new ArrayList<>();
    private List<DatabaseChangeDTO<UserContactDTO>> userContactDTOs = new ArrayList<>();

}
