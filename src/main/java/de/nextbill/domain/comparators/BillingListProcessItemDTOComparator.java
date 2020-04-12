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

package de.nextbill.domain.comparators;

import de.nextbill.domain.dtos.BillingListProcessItemDTO;

import java.util.Comparator;

public class BillingListProcessItemDTOComparator implements Comparator<BillingListProcessItemDTO> {

    public BillingListProcessItemDTOComparator() {
    }

    @Override
    public int compare(BillingListProcessItemDTO o1, BillingListProcessItemDTO o2) {
        if (o1.getCreatedDate() == null && o2.getCreatedDate() == null){
            return 0;
        }else if (o1.getCreatedDate() == null){
            return 1;
        }else if (o2.getCreatedDate() == null){
            return -1;
        }else if(o1.getCreatedDate().before(o2.getCreatedDate())){
            return 1;
        }else if(o2.getCreatedDate().before(o1.getCreatedDate())){
            return -1;
        }else if(o1.getCreatedDate().equals(o2.getCreatedDate())){
            return 0;
        }
        return 0;
    }

}
