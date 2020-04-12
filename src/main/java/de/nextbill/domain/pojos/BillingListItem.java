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

package de.nextbill.domain.pojos;

import de.nextbill.domain.interfaces.PaymentItem;
import de.nextbill.domain.model.InvoiceCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BillingListItem {

    private PaymentPersonPojo invoicePayer;
    private PaymentPersonPojo costPayer;

    private BigDecimal sumToBePaid;
    private BigDecimal sumToBeChecked;

    private BigDecimal costsFromInvoicePayer;
    private BigDecimal costsFromCostPayer;

    private List<InvoiceCategory> invoiceCategoriesOfInvoicePayer;
    private List<InvoiceCategory> invoiceCategoriesOfCostPayer;

    private List<PaymentItem> sumTotalItemsExpense = new ArrayList<>();
    private List<PaymentItem> sumTotalItemsIncome = new ArrayList<>();

    private List<PaymentItem> sumTotalReverseItemsExpense = new ArrayList<>();
    private List<PaymentItem> sumTotalReverseItemsIncome = new ArrayList<>();

    public void setCostItemsForInvoicePayer(List<PaymentItem> costItemsForInvoicePayer) {
        sumTotalItemsIncome.addAll(costItemsForInvoicePayer);
    }

    public void setCostItemsForCostPayer(List<PaymentItem> costItemsForCostPayer) {
        sumTotalItemsExpense.addAll(costItemsForCostPayer);
    }

    public List<PaymentItem> getCostItemsForInvoicePayer() {
        List<PaymentItem> resultList = new ArrayList<>();
        resultList.addAll(sumTotalItemsIncome);
        resultList.addAll(sumTotalReverseItemsExpense);
        return resultList;
    }

    public List<PaymentItem> getCostItemsForCostPayer() {
        List<PaymentItem> resultList = new ArrayList<>();
        resultList.addAll(sumTotalItemsExpense);
        resultList.addAll(sumTotalReverseItemsIncome);
        return resultList;
    }

}
