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
		.controller('BudgetListController', BudgetListController);

	BudgetListController.$inject = ['$state','BudgetService'];

	/* @ngInject */
	function BudgetListController($state,BudgetService) {
		var vm = this;
		vm.title = 'BudgetListController';
		vm.useArchived = false;
		vm.budgets = [];

		vm.newBudget = function(){
			$state.go('budgets.create');
		};

        vm.budgetServiceLoading = function(){
            return BudgetService.isLoading();
        };

		vm.deleteBudget = function(budgetId){
			BudgetService.deleteBudget(budgetId);
		};

		vm.goToDetails = function(budgetId){
			var paramsTransfer = {
				budgetId: budgetId
			};

			$state.go('budgets.edit', paramsTransfer)
		};

		vm.displayNameForBudgetType = function(budgetType){
			vm.displayNames = {
				'WEEK': 'Woche',
				'MONTH': 'Monat'
			};

			return vm.displayNames[budgetType];
		};

		activate();

		////////////////

		function activate() {
            BudgetService.listBudgets().then(
				function(result) {
					vm.budgets = result;
				},
				function(result) {
					vm.budgets = []
				}
			);
		}
	}

})();

