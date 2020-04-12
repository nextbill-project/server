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

package de.nextbill.domain.services;

import de.nextbill.domain.dtos.PaymentPersonDTO;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.interfaces.PaymentPerson;
import de.nextbill.domain.repositories.AppUserRepository;
import de.nextbill.domain.repositories.BusinessPartnerRepository;
import de.nextbill.domain.repositories.UserContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentPersonService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private UserContactRepository userContactRepository;

    public PaymentPerson findPaymentPerson(UUID uuid, PaymentPersonTypeEnum paymentPersonTypeEnum){
        PaymentPerson costPayer = null;

        if (uuid == null || paymentPersonTypeEnum == null){
            return null;
        }

        if (paymentPersonTypeEnum.equals(PaymentPersonTypeEnum.USER)) {
            costPayer = appUserRepository.findById(uuid).orElse(null);
        }else if (paymentPersonTypeEnum.equals(PaymentPersonTypeEnum.BUSINESS_PARTNER)){
            costPayer = businessPartnerRepository.findById(uuid).orElse(null);
        }else if (paymentPersonTypeEnum.equals(PaymentPersonTypeEnum.CONTACT)){
            costPayer = userContactRepository.findById(uuid).orElse(null);
        }else if (paymentPersonTypeEnum.equals(PaymentPersonTypeEnum.PROJECT)){
            costPayer = userContactRepository.findById(uuid).orElse(null);
        }

        return costPayer;
    }

    public PaymentPersonDTO findPaymentPersonAndGetDTO(UUID uuid, PaymentPersonTypeEnum paymentPersonTypeEnum){
        PaymentPerson paymentPerson = findPaymentPerson(uuid, paymentPersonTypeEnum);
        return mapEntityToDTO(paymentPerson);
    }

    public PaymentPersonDTO mapEntityToDTO(PaymentPerson paymentPersonEntity){
        if (paymentPersonEntity != null){
            PaymentPersonDTO paymentPersonDTO = new PaymentPersonDTO();
            paymentPersonDTO.setDisplayName(paymentPersonEntity.getPaymentPersonName());
            paymentPersonDTO.setId(paymentPersonEntity.getPaymentPersonId());
            paymentPersonDTO.setPaymentPersonTypeEnum(paymentPersonEntity.getPaymentPersonEnum());

            return paymentPersonDTO;
        }

        return null;
    }
}
