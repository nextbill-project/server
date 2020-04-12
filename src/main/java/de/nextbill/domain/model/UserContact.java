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
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.interfaces.PaymentPerson;
import lombok.Data;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Data
public class UserContact extends AuditFields implements java.io.Serializable, PaymentPerson {

    private static final long serialVersionUID = 1L;

    @Id
    private UUID userContactId;

    private String contactName;

    @Enumerated(EnumType.STRING)
    private BasicStatusEnum basicStatusEnum;

    @ManyToOne
    private AppUser appUser;

    @ManyToOne
    private AppUser appUserContact;

    private String email;

    private Boolean project = false;

    @Override
    public PaymentPersonTypeEnum getVirtualPaymentPersonEnum() {
        if (appUserContact != null){
            return PaymentPersonTypeEnum.USER;
        }else if (project != null && project == true){
            return PaymentPersonTypeEnum.PROJECT;
        }
        return PaymentPersonTypeEnum.CONTACT;
    }

    @Transient
    @Override
    public String getPaymentPersonName() {
        return getContactName();
    }

    @Transient
    @Override
    public UUID getPaymentPersonId() {
        if (getUserContactId() != null) {
            return getUserContactId();
        }
        return null;
    }

    @Transient
    @Override
    public PaymentPersonTypeEnum getPaymentPersonEnum() {
        return PaymentPersonTypeEnum.CONTACT;
    }
}
