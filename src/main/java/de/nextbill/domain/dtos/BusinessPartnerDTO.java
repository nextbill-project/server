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
import de.nextbill.domain.enums.BasicStatusEnum;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessPartnerDTO implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	private BasicStatusEnum basicStatusEnum;

	private UUID appUserId;

	private Boolean canBeDeleted;

	private UUID businessPartnerId;

	private String businessPartnerName;

	private String businessPartnerReceiptName;

	private String businessPartnerCode;

	private UUID invoiceCategoryId;

	public boolean equals(Object otherObject) {
        if (this == otherObject) return true;
        if ( !(otherObject instanceof BusinessPartnerDTO) ) return false;

        final BusinessPartnerDTO other = (BusinessPartnerDTO) otherObject;

		return other.getBusinessPartnerCode().equals(this.getBusinessPartnerCode());

	}
	
    public int hashCode() {
        int result;
        result = 29 * getBusinessPartnerId().hashCode();
        return result;
    }

}
