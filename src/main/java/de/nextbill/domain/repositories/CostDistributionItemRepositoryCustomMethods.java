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

package de.nextbill.domain.repositories;

import de.nextbill.domain.enums.CorrectionStatus;
import de.nextbill.domain.enums.InvoiceStatusEnum;
import de.nextbill.domain.enums.MainFunctionEnum;
import de.nextbill.domain.enums.PaymentPersonTypeEnum;
import de.nextbill.domain.interfaces.PaymentItem;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface CostDistributionItemRepositoryCustomMethods {

    List<PaymentItem> findCostDistributionItems(MainFunctionEnum mainFunctionEnum, UUID invoicePayerId, PaymentPersonTypeEnum invoicePaymentPersonTypeEnum,
                                                UUID costPayerId, PaymentPersonTypeEnum costPaymentPersonTypeEnum,
                                                Date startDate, Date endDate, InvoiceStatusEnum invoiceStatusEnum, CorrectionStatus correctionStatus, boolean paidAndNotPaid);

    List<PaymentItem> findDirectTransactionsCostDistributionItems(MainFunctionEnum mainFunctionEnum, UUID invoicePayerId, PaymentPersonTypeEnum invoicePaymentPersonTypeEnum,
                                                                  UUID costPayerId, PaymentPersonTypeEnum costPaymentPersonTypeEnum,
                                                                  Date startDate, Date endDate, InvoiceStatusEnum invoiceStatusEnum, CorrectionStatus correctionStatus, boolean paidAndNotPaid);
}
