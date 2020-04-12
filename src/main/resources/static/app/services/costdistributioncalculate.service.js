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

(function () {
	'use strict';

	angular
		.module('msWebApp')
		.factory('CostdistributioncalculateService', CostdistributioncalculateService);

	CostdistributioncalculateService.$inject = [];

	/* @ngInject */
	function CostdistributioncalculateService() {
		var service = {
				getMaxQuotaForOneCostDistributionItem: getMaxQuotaForOneCostDistributionItem,
				getMaxAmountForOneQuota: getMaxAmountForOneQuota,
				getMaxAmountForOneCostDistributionItem: getMaxAmountForOneCostDistributionItem,
				getCountWeightedWithoutFixed: getCountWeightedWithoutFixed,
				calculateQuotaForValue: calculateQuotaForValue,
				calculateAmountForValue: calculateAmountForValue,
				calculatePercentForValue: calculatePercentForValue,
				getCountWithoutFixed: getCountWithoutFixed,
				getCountOfFixedAmount: getCountOfFixedAmount,
				getCountOfPercent: getCountOfPercent,
				calculateAmountForCostDistributionItem: calculateAmountForCostDistributionItem,
				calculateAmountForCostDistributionItemPrecise: calculateAmountForCostDistributionItemPrecise
			};

		Number.prototype.setScale = function(scale){
			var pow = Math.pow(10,scale);
			return +( Math.round(this * pow) / pow );
		};

		Number.prototype.divide = function(number2, scale){
			var divisionResult = this / number2;
			return divisionResult.setScale(scale);
		};

		Number.prototype.remainder = function(n){
			return ((this%n)+n)%n;
		};

		Number.prototype.compareTo = function(number2){
			if (this < number2){
				return -1;
			}else if (this > number2){
				return 1;
			}else {
				return 0;
			}
		};

		Number.prototype.multiply = function(number2){
			return this * number2;
		};

		Number.prototype.subtract = function(number2){
			return this - number2;
		};

		Number.prototype.add = function(number2){
			return this + number2;
		};

		Number.prototype.floor = function() {
			return Math.floor(this);
		};

		return service;

		////////////////

		function getCalculatedRestSum(sum, costDistributionItems){
			var restSumTmp;

			var sumOfAllCostDistributions = 0;
			costDistributionItems.forEach(function(tmpCostDistributionItem){
				sumOfAllCostDistributions = sumOfAllCostDistributions.add(calculateAmountForCostDistributionItem(tmpCostDistributionItem, costDistributionItems, sum));
			});

			restSumTmp = sum.subtract(sumOfAllCostDistributions);

			return restSumTmp;
		}

		function getMaxQuotaForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem){
			var maxQuota = getCountWithoutFixed(costDistributionItemsWithoutCurrentItem).add(1);
			var maxQuotaWeighted = getCountWeightedWithoutFixed(costDistributionItemsWithoutCurrentItem);
			var maxQuotaForOneCostDistributionItem = maxQuota.subtract(maxQuotaWeighted);

			return maxQuotaForOneCostDistributionItem;
		}

		function getMaxAmountForOneQuota(costDistributionItemsWithoutCurrentItem, sum){
			var maxAmountForOneCostDistributionItem = getMaxAmountForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem, sum);
			var countWithoutFixed = getCountWithoutFixed(costDistributionItemsWithoutCurrentItem).add(1);

			var result = maxAmountForOneCostDistributionItem.divide(countWithoutFixed, 50);

			return result;
		}

		function getMaxAmountForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem, sum){
			var amountWithoutFixed = 0;
			costDistributionItemsWithoutCurrentItem.forEach(function(tmpCostDistributionItem1) {
				if (tmpCostDistributionItem1.costDistributionItemTypeEnum === 'FIXED_AMOUNT' || tmpCostDistributionItem1.costDistributionItemTypeEnum === 'PERCENT'){
					amountWithoutFixed = amountWithoutFixed + calculateAmountForCostDistributionItem(tmpCostDistributionItem1, costDistributionItemsWithoutCurrentItem, sum);
				}
			});

			var sumOfCostDistributionItemsWithoutFixedAmount = sum.subtract(amountWithoutFixed);
			return sumOfCostDistributionItemsWithoutFixedAmount;
		}
		
		function getCountWeightedWithoutFixed(costDistributionItemsWithoutCurrentItem){
			var countWeightedWithoutFixed = 0;
			costDistributionItemsWithoutCurrentItem.forEach(function(tmpCostDistributionItem1) {
				if (tmpCostDistributionItem1.costDistributionItemTypeEnum === 'QUOTA') {
					countWeightedWithoutFixed = countWeightedWithoutFixed + tmpCostDistributionItem1.value;
				}
			});

			return countWeightedWithoutFixed;
		}

		function calculateQuotaForValue(costDistributionItemTypeOfValue, value, costDistributionItemsWithoutCurrentItem, sum){
			var quotaForEt = 0;
			if (costDistributionItemTypeOfValue === 'FIXED_AMOUNT') {

				var quotaWeighted = 0;
				if (value.compareTo(0) == 1){


					var maxQuotaForOneCostDistributionItemTmp = getMaxQuotaForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem);
					var maxAmountForOneQuota = calculateAmountForValue('QUOTA', maxQuotaForOneCostDistributionItemTmp, costDistributionItemsWithoutCurrentItem, sum);

					var valueForCalculating = value;

					if (value.compareTo(maxAmountForOneQuota) == 1){
						valueForCalculating = maxAmountForOneQuota;
					}else if (value.compareTo(0) == -1 || value.compareTo(0) == 0){
						valueForCalculating = 0;
					}

					if (valueForCalculating.compareTo(0) != 0){
						var percentOfMaxAmountForOneQuota = valueForCalculating.divide(maxAmountForOneQuota, 50);
						var maxQuotaForOneCostDistributionItem = getMaxQuotaForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem);
						quotaWeighted = percentOfMaxAmountForOneQuota.multiply(maxQuotaForOneCostDistributionItem).setScale(1);
					}
				}

				quotaForEt = quotaWeighted;
			} else if (costDistributionItemTypeOfValue === 'QUOTA') {
				quotaForEt = value.setScale(50);
			} else if (costDistributionItemTypeOfValue === 'PERCENT') {
				quotaWeighted = 0;
				if (value.compareTo(0) == 1){

					maxQuotaForOneCostDistributionItemTmp = getMaxQuotaForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem);
					maxAmountForOneQuota = calculateAmountForValue('QUOTA', maxQuotaForOneCostDistributionItemTmp, costDistributionItemsWithoutCurrentItem, sum);

					valueForCalculating = calculateAmountForValue('PERCENT', value, costDistributionItemsWithoutCurrentItem, sum);

					if (value.compareTo(maxAmountForOneQuota) == 1){
						valueForCalculating = maxAmountForOneQuota;
					}else if (value.compareTo(0) == -1 || value.compareTo(0) == 0){
						valueForCalculating = 0;
					}else if (valueForCalculating.compareTo(maxAmountForOneQuota) == 1 || valueForCalculating.compareTo(maxAmountForOneQuota) == 0){
						valueForCalculating = maxAmountForOneQuota;
					}

					if (valueForCalculating.compareTo(0) != 0){
						percentOfMaxAmountForOneQuota = valueForCalculating.divide(maxAmountForOneQuota, 50);
						maxQuotaForOneCostDistributionItem = getMaxQuotaForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem);
						quotaWeighted = percentOfMaxAmountForOneQuota.multiply(maxQuotaForOneCostDistributionItem).setScale(1);
					}
				}

				quotaForEt = quotaWeighted;
			} else if (costDistributionItemTypeOfValue === 'REST') {
				var maxQuota = getCountWithoutFixed(costDistributionItemsWithoutCurrentItem);
				var maxQuotaWeighted = getCountWeightedWithoutFixed(costDistributionItemsWithoutCurrentItem);
				quotaForEt = maxQuota.subtract(maxQuotaWeighted);
			}

			return  quotaForEt.setScale(1);
		}

		function calculateAmountForValue(costDistributionItemTypeOfValue, value, costDistributionItemsWithoutCurrentItem, sum){
			var calculateAmountForValue = 0;

			if (costDistributionItemTypeOfValue === 'QUOTA'){
				var maxAmountForOneQuota = getMaxAmountForOneQuota(costDistributionItemsWithoutCurrentItem, sum);

				if (maxAmountForOneQuota.compareTo(0) == -1){
					maxAmountForOneQuota = 0;
				}

				var quotaOfCostDistributionItem = value.multiply(maxAmountForOneQuota).setScale(2);
				calculateAmountForValue = quotaOfCostDistributionItem;
			}else if (costDistributionItemTypeOfValue === 'FIXED_AMOUNT'){

				calculateAmountForValue = value;
			}else if (costDistributionItemTypeOfValue === 'PERCENT'){
				var amountForQuota = sum.multiply(value).setScale(2);
				calculateAmountForValue = amountForQuota;
			}else if (costDistributionItemTypeOfValue === 'REST') {

				var sumOfAllCostDistributionItemsWithoutRest = 0;
				costDistributionItemsWithoutCurrentItem.forEach(function(tmpCostDistributionItem){
					if (tmpCostDistributionItem.costDistributionItemTypeEnum != 'REST'){
						sumOfAllCostDistributionItemsWithoutRest.add(calculateAmountForCostDistributionItem(tmpCostDistributionItem, costDistributionItemsWithoutCurrentItem, sum));
					}
				});

				var restSum = sum.subtract(sumOfAllCostDistributionItemsWithoutRest);
				calculateAmountForValue = restSum;
			}

			return calculateAmountForValue.setScale(2);
		}

		function calculatePercentForValue(costDistributionItemTypeOfValue, value, costDistributionItemsWithoutCurrentItem, sum){
			var resultPercent = 0;

			if (costDistributionItemTypeOfValue === 'QUOTA'){
				var amountForQuota = calculateAmountForValue('QUOTA', value, costDistributionItemsWithoutCurrentItem, sum);
				var percent = amountForQuota.divide(sum, 30);
				resultPercent = percent;
			}else if (costDistributionItemTypeOfValue === 'FIXED_AMOUNT'){
				percent = value.divide(sum, 2);
				resultPercent = percent;
			}else if (costDistributionItemTypeOfValue === 'PERCENT'){
				resultPercent = value;
			}

			return resultPercent.setScale(2);
		}

		function getCountWithoutFixed(costDistributionItems){
			var countWithoutFixed = 0;
			costDistributionItems.forEach(function(tmpCostDistributionItem1){
				if (tmpCostDistributionItem1.costDistributionItemTypeEnum === 'QUOTA'){
					countWithoutFixed = countWithoutFixed.add(1);
				}
			});

			return countWithoutFixed;
		}

		function getCountOfFixedAmount(costDistributionItems){
			var getCountOfFixed = 0;
			costDistributionItems.forEach(function(tmpCostDistributionItem1){
				if (tmpCostDistributionItem1.costDistributionItemTypeEnum === 'FIXED_AMOUNT'){
					getCountOfFixed = getCountOfFixed.add(1);
				}
			});

			return getCountOfFixed;
		}

		function getCountOfPercent(costDistributionItems){
			var getCountOfPercent = 0;
			costDistributionItems.forEach(function(tmpCostDistributionItem1){
				if (tmpCostDistributionItem1.costDistributionItemTypeEnum === 'PERCENT'){
					getCountOfPercent = getCountOfPercent.add(1);
				}
			});

			return getCountOfPercent;
		}

		function calculateAmountForCostDistributionItem(costDistributionItem, costDistributionItems, sum){
			var costDistributionItemsWithoutCurrentItem = [];
			costDistributionItems.forEach(function(item){
				costDistributionItemsWithoutCurrentItem.push(item);
			});

			var indexOfPosition = costDistributionItems.findIndex(function(item){
				return item.costDistributionItemId === costDistributionItem.costDistributionItemId;
			});
			costDistributionItemsWithoutCurrentItem.splice(indexOfPosition, 1);

			return calculateAmountForValue(costDistributionItem.costDistributionItemTypeEnum, costDistributionItem.value, costDistributionItemsWithoutCurrentItem, sum).setScale(2);
		}

		function isCostDistributionComplete(costDistributionItems, sum){

			if (costDistributionItems.length == 0){
				return false;
			}

			var countWeightedWithoutFixed = getCountWeightedWithoutFixed(costDistributionItems);
			var countWithoutFixed = getCountWithoutFixed(costDistributionItems);
			var countOfFixed = getCountOfFixedAmount(costDistributionItems);
			var countOfPercent = getCountOfPercent(costDistributionItems);

			if (countWithoutFixed.compareTo(0) == 1){
				return countWithoutFixed.compareTo(countWeightedWithoutFixed) == 0;
			}else{
				if (countOfFixed.compareTo(0) == 0 && countOfPercent.compareTo(0) == 1){
					var sumOfPercent = 0;
					costDistributionItems.forEach(function(tmpCostDistributionItem){
						sumOfPercent = sumOfPercent.add(tmpCostDistributionItem.value);
					});

					return sumOfPercent.compareTo(1) == 0;
				}else if (countOfFixed.compareTo(0) == 1 && countOfPercent.compareTo(0) == 0){
					var sumOfFixedAmount = 0;
					costDistributionItems.forEach(function(tmpCostDistributionItem){
						sumOfFixedAmount = sumOfFixedAmount.add(tmpCostDistributionItem.value);
					});

					return sumOfFixedAmount.compareTo(sum) == 0;
				}else if (countOfFixed.compareTo(0) == 1 && countOfPercent.compareTo(0) == 1){
					var sumOfMixed = 0;
					costDistributionItems.forEach(function(tmpCostDistributionItem){
						sumOfMixed = calculateAmountForCostDistributionItem(tmpCostDistributionItem, costDistributionItems, sum);
					});
					return sumOfMixed.compareTo(sum) == 0;
				}
			}
			return true;
		}

		function calculateAmountForCostDistributionItemPrecise(costDistributionItem, costDistributionItems, sum){
			var resultAmount;

			var costDistributionItemsWithoutCurrentItem = [];
			costDistributionItems.forEach(function(item){
				costDistributionItemsWithoutCurrentItem.push(item);
			});

			var indexOfPosition = costDistributionItems.findIndex(function(item){
				return item.costDistributionItemId === costDistributionItem.costDistributionItemId;
			});
			costDistributionItemsWithoutCurrentItem.splice(indexOfPosition, 1);

			if (isCostDistributionComplete(costDistributionItems, sum)){
				var countCostDistributionItemsIncludeQuota = 0;
				var counter = 0;
				var costDistributionPosition = -1;

				var costDistributionItemQuotaList = [];

				costDistributionItems.forEach(function(tmpCostDistributionItem1){
					if (tmpCostDistributionItem1.costDistributionItemTypeEnum !== 'FIXED_AMOUNT'){

						countCostDistributionItemsIncludeQuota = countCostDistributionItemsIncludeQuota + 1;

						if (tmpCostDistributionItem1.costDistributionItemId === costDistributionItem.costDistributionItemId){
							costDistributionPosition=counter;
						}

						costDistributionItemQuotaList.push(tmpCostDistributionItem1);

						counter++;
					}
				});

				if (countCostDistributionItemsIncludeQuota > 1){
					var calculatedRestSum = getCalculatedRestSum(sum, costDistributionItems);
					resultAmount = calculateAmountForValue(costDistributionItem.costDistributionItemTypeEnum, costDistributionItem.value, costDistributionItemsWithoutCurrentItem, sum).setScale(2);

					if (calculatedRestSum.compareTo(0) == -1 || calculatedRestSum.compareTo(0) == 1){

						var rest = calculatedRestSum;

						var corrector = 1;

						if (rest.compareTo(0) == -1){
							corrector = -1;
						}

						var restAmountForOneCostDistributionItem = calculatedRestSum.multiply(corrector).multiply(100).divide(countCostDistributionItemsIncludeQuota,50).floor();
						var remainder = calculatedRestSum.multiply(corrector).multiply(100).remainder(countCostDistributionItemsIncludeQuota);

						restAmountForOneCostDistributionItem = restAmountForOneCostDistributionItem.multiply(corrector);
						remainder = remainder.multiply(corrector);

						resultAmount = resultAmount.add(restAmountForOneCostDistributionItem.divide(100, 2));

						for(var i = 0; i < remainder.multiply(corrector).setScale(0); i++){
							if (i == costDistributionPosition){
								resultAmount = resultAmount.add(0.01.multiply(corrector));
							}
						}

						resultAmount = resultAmount.setScale(2);

					}

				}else{
					resultAmount = calculateAmountForValue(costDistributionItem.costDistributionItemTypeEnum, costDistributionItem.value, costDistributionItemsWithoutCurrentItem, sum).setScale(2);
				}
			}else{
				resultAmount = calculateAmountForValue(costDistributionItem.costDistributionItemTypeEnum, costDistributionItem.value, costDistributionItemsWithoutCurrentItem, sum).setScale(2);
			}

			return resultAmount;
		}
	}

})();

