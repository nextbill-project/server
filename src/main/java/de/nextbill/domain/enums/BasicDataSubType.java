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

package de.nextbill.domain.enums;

public enum BasicDataSubType {
	CALCULATED_PROFIT_FOR_MONTH, MONTH_AVERAGE_EXPENSE, MONTH_AVERAGE_INCOME, AVERAGE_EXPENSE_TILL_NOW, AVERAGE_EXPENSE_AFTER_NOW, ESTIMATED_INCOME_FOR_CURRENT_MONTH, ARE_THERE_ENOUGH_ENTRIES_FOR_ANALYSIS, CATEGORY_FOR_BUSINESS_PARTNER, COST_DISTRIBUTION_FOR_CATEGORY, SPECIAL_TYPE_FOR_CATEGORY, REPETITION_TYPE_FOR_CATEGORY, WAIT_FOR_PAYMENT, TO_PAY
}
