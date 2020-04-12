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
		.module('msWebApp.msCostdistributioncalculate',[])
		.directive('msCostdistributioncalculate', msCostdistributioncalculate);

	msCostdistributioncalculate.$inject = ['$document','$log'];

	/* @ngInject */
	function msCostdistributioncalculate($document,$log) {
		var directive = {
			restrict: 'E',
			templateUrl: 'app/components/ms-costdistribution-calculate.html',
			scope: {
				costDistributionItem: '=msCostDistributionItem',
				costDistributionItems: '=msCostDistributionItems',
				dialogId: '=msDialogId',
				updateMethod:'=msUpdateMethod',
				sumCost: '=msSumCost'
			},
			link: function link(scope, element, attrs, vm) {

			},
			controller: CostdistributioncalculateController,
			controllerAs: 'vm',
			bindToController: true
		};

		return directive;


	}

	CostdistributioncalculateController.$inject = ['$filter','$log','CostdistributioncalculateService'];

	/* @ngInject */
	function CostdistributioncalculateController($filter,$log,CostdistributioncalculateService) {
		var vm = this;

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

		vm.costDistributionPercentageTv = '0 %';
		vm.costDistributionQuotaTv = '0.0';
		vm.costDistributionFixedAmountEt = 0.0;

		vm.costDistributionItemTypeEnum = null;

		if (vm.sumCost == null || vm.sumCost == undefined){
			vm.sumCost = 0;
		}

		vm.updateMethod = function(){
			vm.currentValue = vm.costDistributionItem.value;
			vm.costDistributionItemTypeEnum = vm.costDistributionItem.costDistributionItemTypeEnum;
			costDistributionItemsWithoutCurrentItemUpdate();
			vm.refreshAllViews();

			vm.updateCostDistributionItems();
		};

		vm.getDialogIdWithoutHash = function(){
			var resultId = vm.dialogId;
			return resultId;
		};

		vm.updateCostDistributionItems = function(){
			vm.costDistributionItems.forEach(function(item){
				item.moneyValue = CostdistributioncalculateService.calculateAmountForCostDistributionItemPrecise(item, vm.costDistributionItems, vm.sumCost);
			})
		};

		vm.onProgressChanged = function(){

			if (vm.costDistributionItemsWithoutCurrentItem == undefined){
				return;
			}

			var percent = parseInt(vm.seekBar).divide(100, 30);

			if (vm.costDistributionItem.costDistributionItemTypeEnum === 'FIXED_AMOUNT') {
				var maxAmountForOneCostDistributionItem = CostdistributioncalculateService.getMaxAmountForOneCostDistributionItem(vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
				vm.currentValue = maxAmountForOneCostDistributionItem.multiply(percent).setScale(2);
			} else if (vm.costDistributionItem.costDistributionItemTypeEnum === 'QUOTA') {
				var maxQuota = CostdistributioncalculateService.getCountWithoutFixed(vm.costDistributionItems);
				var maxQuotaWeighted = CostdistributioncalculateService.getCountWeightedWithoutFixed(vm.costDistributionItemsWithoutCurrentItem);
				var maxQuotaSubtractMaxQuotaWeighted = maxQuota.subtract(maxQuotaWeighted);

				vm.currentValue = percent.multiply(maxQuotaSubtractMaxQuotaWeighted).setScale(1);
			} else if (vm.costDistributionItem.costDistributionItemTypeEnum === 'PERCENT') {
				var maxAmount = CostdistributioncalculateService.getMaxAmountForOneCostDistributionItem(vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
				var amountOfPercentOfMaxAmount = percent.multiply(maxAmount);
				var percentOfResult = amountOfPercentOfMaxAmount.divide(vm.sumCost, 2);
				vm.currentValue = percentOfResult;
			}

			updateCostDistributionItem();
			vm.refreshAllViews('UNDEFINED');

			vm.updateCostDistributionItems();
		};

		function costDistributionItemsWithoutCurrentItemUpdate(){
			vm.costDistributionItemsWithoutCurrentItem = [];
			vm.costDistributionItems.forEach(function(item){
				vm.costDistributionItemsWithoutCurrentItem.push(item);
			});

			var indexOfPosition = vm.costDistributionItems.findIndex(function(item){
				return item.costDistributionItemId === vm.costDistributionItem.costDistributionItemId;
			});
			vm.costDistributionItemsWithoutCurrentItem.splice(indexOfPosition, 1);
		}

		function updateCostDistributionItem(){
			vm.costDistributionItem.value = vm.currentValue;
		}

		vm.refreshAllViews = function(costDistributionItemTypeEnum) {

			if (vm.sumCost == undefined){
				vm.sumCost = 0;
			}

			if (costDistributionItemTypeEnum != null && costDistributionItemTypeEnum != undefined) {
				if (costDistributionItemTypeEnum === 'FIXED_AMOUNT') {
					vm.refreshProgressBar();
				} else if (costDistributionItemTypeEnum === 'QUOTA') {
					vm.refreshFixedAmountEt();
					vm.refreshProgressBar();
				} else if (costDistributionItemTypeEnum === 'PERCENT') {
					vm.refreshFixedAmountEt();
					vm.refreshProgressBar();
				} else if (costDistributionItemTypeEnum === 'UNDEFINED') {
					vm.refreshFixedAmountEt();
				}
			} else {
				vm.refreshFixedAmountEt();
				vm.refreshProgressBar();
			}

			refreshQuotaEt();
			refreshPercentageTv();
		};

		function refreshPercentageTv() {
			var percentOfCurrentQuota = 0;

			if (vm.costDistributionItem.costDistributionItemTypeEnum === 'FIXED_AMOUNT') {
				percentOfCurrentQuota = vm.currentValue.divide(vm.sumCost, 10).multiply(100).setScale(0);
			} else if (vm.costDistributionItem.costDistributionItemTypeEnum === 'QUOTA') {
				var percentForValue = CostdistributioncalculateService.calculatePercentForValue('QUOTA', vm.currentValue, vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
				percentOfCurrentQuota = percentForValue.multiply(100).setScale(0);
			} else if (vm.costDistributionItem.costDistributionItemTypeEnum === 'PERCENT') {
				percentOfCurrentQuota = vm.currentValue.multiply(100).setScale(0);
			}

			vm.costDistributionPercentageTv = percentOfCurrentQuota.toFixed(0) + '%';
		}

		function refreshQuotaEt() {
			var quotaForEt = CostdistributioncalculateService.calculateQuotaForValue(vm.costDistributionItem.costDistributionItemTypeEnum, vm.currentValue, vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
			vm.costDistributionQuotaTv = quotaForEt.toFixed(1);
		}

		vm.onQuotaCheckBoxClick = function() {
			var quota = CostdistributioncalculateService.calculateQuotaForValue(vm.costDistributionItemTypeEnum, vm.currentValue, vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
			vm.currentValue = quota;

			vm.costDistributionItemTypeEnum = 'QUOTA';

			updateCostDistributionItem();

			vm.refreshAllViews('QUOTA');
		};

		vm.onPercentCheckBoxClick = function() {
			var percent = CostdistributioncalculateService.calculatePercentForValue(vm.costDistributionItemTypeEnum, vm.currentValue, vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
			vm.currentValue = percent;

			vm.costDistributionItemTypeEnum = 'PERCENT';

			updateCostDistributionItem();

			vm.refreshAllViews('PERCENT');
		};

		vm.onFixedAmountCheckBoxClick = function() {
			var fixedAmount = CostdistributioncalculateService.calculateAmountForValue(vm.costDistributionItemTypeEnum, vm.currentValue, vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
			vm.currentValue = fixedAmount;

			vm.costDistributionItemTypeEnum = 'FIXED_AMOUNT';

			updateCostDistributionItem();

			vm.refreshAllViews('FIXED_AMOUNT');
		};

		vm.onFixedAmountTextChanged = function() {
			if (vm.costDistributionItemTypeEnum === 'FIXED_AMOUNT'){
				vm.currentValue = vm.costDistributionFixedAmountEt;

				updateCostDistributionItem();

				vm.refreshAllViews('FIXED_AMOUNT');

				vm.updateCostDistributionItems();
			}
		};

		vm.refreshFixedAmountEt = function() {
			var fixedAmountForEt = CostdistributioncalculateService.calculateAmountForValue(vm.costDistributionItem.costDistributionItemTypeEnum, vm.currentValue, vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
			vm.costDistributionFixedAmountEt = fixedAmountForEt;
			vm.costDistributionItem.moneyValue = fixedAmountForEt;
		};

		vm.refreshProgressBar = function() {

			var percentForProgressEt = 0;

			if (vm.costDistributionItem.costDistributionItemTypeEnum === 'FIXED_AMOUNT') {
				var maxAmountForOneCostDistributionItem = CostdistributioncalculateService.getMaxAmountForOneCostDistributionItem(vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
				percentForProgressEt = vm.currentValue.divide(maxAmountForOneCostDistributionItem, 10).multiply(100).setScale(0);
			} else if (vm.costDistributionItem.costDistributionItemTypeEnum === 'QUOTA') {
				var maxQuota = CostdistributioncalculateService.getCountWithoutFixed(vm.costDistributionItems);
				var maxQuotaWeighted = CostdistributioncalculateService.getCountWeightedWithoutFixed(vm.costDistributionItemsWithoutCurrentItem);
				var maxQuotaSubtractMaxQuotaWeighted = maxQuota.subtract(maxQuotaWeighted);

				percentForProgressEt = vm.currentValue.divide(maxQuotaSubtractMaxQuotaWeighted, 10).multiply(100).setScale(0);
			} else if (vm.costDistributionItem.costDistributionItemTypeEnum === 'PERCENT') {
				var maxAmount = CostdistributioncalculateService.getMaxAmountForOneCostDistributionItem(vm.costDistributionItemsWithoutCurrentItem, vm.sumCost);
				var amountOfPercent = vm.currentValue.multiply(vm.sumCost);
				var percentOfResult = 0;
				if (maxAmount != 0){
					percentOfResult  = amountOfPercent.divide(maxAmount, 3);
				}

				percentForProgressEt = percentOfResult.multiply(100);
			}

			vm.seekBar = percentForProgressEt.setScale(0);
		}
	}

})();

