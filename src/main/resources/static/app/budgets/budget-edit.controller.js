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
		.controller('BudgetEditController', BudgetEditController);

	BudgetEditController.$inject = ['$timeout','$filter','$log','csrfToken', '$state','$stateParams','$scope','BudgetService','InvoiceService'];

	/* @ngInject */
	function BudgetEditController($timeout,$filter,$log,csrfToken,$state,$stateParams,$scope,BudgetService,InvoiceService) {
		var vm = this;
		vm.title = 'BudgetEditController';

		vm.budget = {};
		vm.createMode = false;

		if ($stateParams.budgetId) {
			vm.budget.budgetId = $stateParams.budgetId;
		}else{
			vm.createMode = true;
		}
		
		vm.paymentTypeEnums = [];
		vm.budgetRepetionTypeEnums = [];

		vm.submitForm = function() {

			vm.budget.paymentTypeEnum = vm.budget.paymentTypeEnum !== '' && vm.budget.paymentTypeEnum !== undefined ? vm.budget.paymentTypeEnum : null;

			if (vm.createMode) {
				createBudget();
			} else {
				updateBudget();
			}
		};

		vm.getDialogId = function(customIdentifier){
			var resultId = '#';
			if (customIdentifier != undefined){
				resultId = resultId + customIdentifier;
			}
			return resultId;
		};

		vm.getDialogIdWithoutHash = function(customIdentifier){
			var resultId = customIdentifier;
			return resultId;
		};

		var goToOverview = function () {
			$state.go('budgets.list');
		};

		vm.removePersonFromList = function(payerDtoInput) {
			var indexOfPosition = vm.budget.payerDTOS.findIndex(function (payerDTO) {
				return payerDTO.id === payerDtoInput.id;
			});

			vm.budget.payerDTOS.splice(indexOfPosition, 1);
		};

		vm.removeCategoryFromList = function(categoryDtoInput) {
			var indexOfPosition = vm.budget.invoiceCategoryDTOS.findIndex(function (categoryDTO) {
				return categoryDTO.id === categoryDtoInput.id;
			});

			vm.budget.invoiceCategoryDTOS.splice(indexOfPosition, 1);
		};

		vm.addPersonToList = function (payer){
			var budgetRecipient = {
				'id': payer.id,
				'displayName':payer.name,
				'paymentPersonTypeEnum':payer.type
			};

			vm.budget.payerDTOS.push(budgetRecipient);
		};

		vm.addCategoryToList = function (category){
			vm.budget.invoiceCategoryDTOS.push(category);
		};

        vm.removeBudget = function() {
            BudgetService.deleteBudget(vm.budget.budgetId).then(
				function(result) {
					goToOverview();
				}
			);
        };

        vm.deleteConfirmationMenu = [
            ['Löchen bestätigen', function ($itemScope, $event, color) {
                vm.removeBudget();
            }]
        ];

		activate();

		////////////////

		function generateUUID(){
			function s4() {
				return Math.floor((1 + Math.random()) * 0x10000)
					.toString(16)
					.substring(1);
			}
			return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
				s4() + '-' + s4() + s4() + s4();
		}

		function getPaymentTypeEnums() {
			InvoiceService.paymentTypeEnumList().then(function(result) {
				vm.paymentTypeEnums = result.data;
			});
		}

		function getBudgetRepetitionTypeEnums() {
			BudgetService.budgetRepetitionTypeEnumList().then(function(result) {
				vm.budgetRepetionTypeEnums = result.data;
			});
		}

		function getBudget() {
			BudgetService.details(vm.budget.budgetId).then(function(response) {
				vm.budget = response.data;
			});
		}

		function activate() {

			getPaymentTypeEnums();
			getBudgetRepetitionTypeEnums();

			if (vm.budget.budgetId) {
				getBudget();
			}else{
				var today = new Date();
				vm.budget.budgetId = generateUUID();
				vm.budget.payerDTOS = [];
				vm.budget.budgetRepetitionType = 'MONTH';
				vm.budget.invoiceCategoryDTOS = [];
				vm.budget.sum = 0;
			}
		}

		function createBudget() {
			BudgetService.createBudget(vm.budget).then(function(result){
				goToOverview();
			});
		}

		function updateBudget() {
			BudgetService.updateBudget(vm.budget.budgetId, vm.budget).then(function(result){
				goToOverview();
			});
		}

	}

})();

