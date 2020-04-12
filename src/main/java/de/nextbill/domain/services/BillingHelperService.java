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

import de.nextbill.domain.enums.BillingStatusEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.model.AppUser;
import de.nextbill.domain.model.Billing;
import de.nextbill.domain.model.UserContact;
import de.nextbill.domain.repositories.BillingRepository;
import de.nextbill.domain.repositories.UserContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingHelperService {

    @Autowired
    private UserContactRepository userContactRepository;

    @Autowired
    private BillingRepository billingRepository;

    public List<Billing> billingsForAppUser(AppUser billingAppUser){
        return billingsForAppUser(billingAppUser, false, false);
    }

    public List<Billing> billingsForAppUser(AppUser billingAppUser, boolean includeFinished, boolean includeArchived){

        List<UUID> appUserContactIds = userContactRepository.findAllByAppUserContact(billingAppUser).stream().map(UserContact::getUserContactId).collect(Collectors.toList());
        appUserContactIds.addAll(userContactRepository.findAllByAppUser(billingAppUser).stream().map(UserContact::getUserContactId).collect(Collectors.toList()));

        List<Billing> billings = new ArrayList<>();
        for (UUID appUserContactId : appUserContactIds) {
            billings.addAll(billingsForAppUser(billingAppUser, appUserContactId, includeFinished, includeArchived));
        }

        return billings;
    }

    public List<Billing> billingsForAppUser(AppUser billingAppUser, UUID appUserContactId, boolean includeFinished, boolean includeArchived){
        String appUserId = billingAppUser.getAppUserId().toString();

        List<UUID> appUserContactIds = new ArrayList<>();
        appUserContactIds.add(appUserContactId);

        Set<Billing> billings = new HashSet<>();
        billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.TO_PAY, true, appUserContactIds));
        billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.TO_PAY, false, appUserContactIds));

        billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.TO_PAY, false, appUserContactIds));
        billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.TO_PAY, true, appUserContactIds));

        billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.PAID, false, appUserContactIds));
        billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.PAID, true, appUserContactIds));

        if (includeFinished){

            billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.PAYMENT_CONFIRMED, true, appUserContactIds));
            billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.PAYMENT_CONFIRMED, false, appUserContactIds));

            billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.PAID, true, appUserContactIds));
            billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.PAID, false, appUserContactIds));

            billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.PAYMENT_CONFIRMED, false, appUserContactIds));
            billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.PAYMENT_CONFIRMED, true, appUserContactIds));

            billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.FINISHED, false, appUserContactIds));
            billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.FINISHED, true, appUserContactIds));
            billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.FINISHED, true, appUserContactIds));
            billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.FINISHED, false, appUserContactIds));
        }

        if (includeArchived){

            billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.ARCHIVED, false, appUserContactIds));
            billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.ARCHIVED, true, appUserContactIds));
            billings.addAll(billingRepository.findAllByCostPayerTypeEnumAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(PaymentPersonTypeEnum.CONTACT, BillingStatusEnum.ARCHIVED, true, appUserContactIds));
            billings.addAll(billingRepository.findAllByCreatedByAndBillingStatusEnumAndIsNormalPaymentAndCostPayerIdIn(appUserId, BillingStatusEnum.ARCHIVED, false, appUserContactIds));
        }

        return new ArrayList<>(billings);
    }

}
