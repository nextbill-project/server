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
		.controller('StandingOrderListController', StandingOrderListController);

	StandingOrderListController.$inject = ['$state','$stateParams','InvoiceService', 'SearchService'];

	/* @ngInject */
	function StandingOrderListController($state,$stateParams,InvoiceService, SearchService) {
		var vm = this;
		vm.title = 'StandingOrderListController';

		vm.toggleFilter = false;

		vm.goToInvoice = function(invoiceId, viewList) {
			$state.go('invoices.details', {
				invoiceId: invoiceId,
				view: viewList,
				returnView: "standingorders.list",
				isStandingOrder: true
			});
		};

		vm.getInvoices = function(mode){
			var invoices = InvoiceService.getInvoices(mode);
			return invoices;
		};

        vm.startSearch = function(){
            // SearchService.search();
			vm.refreshInvoices();
        };

		vm.toggleFilterView = function(){
			vm.toggleFilter = !vm.toggleFilter;
		};

		vm.refreshInvoices = function(){
			// SearchService.resetSearchConfiguration();
			InvoiceService.setSearchMode(false);
			InvoiceService.allInvoices();
		};

        vm.invoiceServiceLoading = function(){
			return InvoiceService.isLoading();
		};

		vm.newExpense = function(){
			$state.go('invoices.create', {
				transactionType: 'EXPENSE',
				isStandingOrder: true,
                returnView: "standingorders.list"
			});
		};

		vm.newIncome = function(){
			$state.go('invoices.create', {
				transactionType: 'INCOME',
				isStandingOrder: true,
                returnView: "standingorders.list"
			});
		};

		vm.otherMenuOptions = [
			['Einnahme', function ($itemScope, $event, color) {
				vm.newIncome();
			}],['Ausgabe', function ($itemScope, $event, color) {
				vm.newExpense();
			}]
		];
	}

})();

