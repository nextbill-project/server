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
		.controller('InvoiceListController', InvoiceListController);

	InvoiceListController.$inject = ['$log','$scope','$filter','$state','$stateParams','InvoiceService', 'SearchService', 'NotificationService'];

	/* @ngInject */
	function InvoiceListController($log, $scope,$filter,$state,$stateParams,InvoiceService, SearchService, NotificationService) {
		var vm = this;
		vm.title = 'InvoiceListController';
		vm.showCheckList = false;
		vm.showReadyList = false;
		vm.isLoadingReadyList = false;

		vm.toggleFilter = false;

		vm.selected = [];

		vm.goToInvoice = function(invoice) {
			if (invoice.invoiceStatusEnum !== 'ANALYZING'){
				$state.go('^.details', {
					invoiceId: invoice.invoiceId
				});
			}else{
				NotificationService.create('Rechnung wird gerade analysiert! Bitte warten!',3000);
			}
		};

		vm.setViewMode = function(viewMode){
			InvoiceService.setViewMode(viewMode);
		};

		vm.getInvoices = function(mode){
			var invoices = InvoiceService.getInvoices(mode);
			return invoices;
		};

		vm.viewMode = function(viewInput){
			var view = InvoiceService.getViewMode();

			if (view === viewInput){
				return true;
			}
			return false;

			// if ((InvoiceService.invoiceListCheck == undefined || InvoiceService.invoiceListCheck.length == 0) && (InvoiceService.invoiceListReady != undefined && InvoiceService.invoiceListReady.length > 0)){
			// 	vm.showCheckList = false;
			// 	vm.showReadyList = true;
			// }
			//
			// if (view != undefined){
			// 	if (view != null){
			// 		if (view === 'CHECK'){
			// 			vm.showCheckList = true;
			// 			vm.showReadyList = false;
			// 		}else if (view === 'READY'){
			// 			vm.showCheckList = false;
			// 			vm.showReadyList = true;
			// 		}
			// 	}
			// }else{
			// 	InvoiceService.viewMode = 'CHECK';
			// 	vm.showCheckList = true;
			// 	vm.showReadyList = false;
			// }
		};

		vm.getIsScrollingDisabled = function(){
			return InvoiceService.getIsScrollingDisabled();
		};

        vm.toggleFilterView = function(){
        	vm.toggleFilter = !vm.toggleFilter;
		};

		vm.toggleExportView = function(){
			vm.showCsvExport = !vm.showCsvExport;
			if (vm.showCsvExport){
				vm.toggleFilter = true;
			}
		};

		vm.getCsvExportUrl = function() {
			var jsonString = JSON.stringify(SearchService.getSearchConfiguration());
			var objJsonB64 = btoa(jsonString);

			return '/webapp/api/export/withFilter/export.csv?invoiceStatusEnum='+ InvoiceService.getViewMode() + '&filter=' + objJsonB64;
		};

        vm.setCorrectionStatusForSelection = function(){
        	InvoiceService.updateCorrectionStatus(vm.selected, 'READY').then(function(result){
        		vm.selected = [];
			})
		};

        vm.startSearch = function(){
			vm.refreshInvoiceList();
        };

        vm.invoiceServiceLoading = function(){
			return InvoiceService.isLoading();
		};
		
		vm.refreshInvoices = function(){
			SearchService.resetSearchConfiguration();

			vm.refreshInvoiceList();
		};

		vm.refreshInvoiceList = function(){
			InvoiceService.clearCurrentPageReadyList();
			InvoiceService.clearCurrentPageCheckList();

			InvoiceService.setSearchMode(false);
			InvoiceService.setIsScrollingDisabled(true);
			InvoiceService.allInvoices().then(function(){
				if (!InvoiceService.getIsScrollingDisabled()){
					$scope.$emit('readyList:filtered');
				}
			});
		};
		
		vm.showMoreReadyList = function(){
			if (!InvoiceService.isSearchMode() && !InvoiceService.getIsScrollingDisabled() && InvoiceService.getViewMode() === 'READY'){
                vm.isLoadingReadyList = true;
				$log.warn('Invoice list showMoreReadyList');
				InvoiceService.setIsScrollingDisabled(true);
                InvoiceService.addValueToCurrentPageReadyList();
                InvoiceService.readyInvoicesList().then(function (resolve) {
                    vm.isLoadingReadyList = false;
                }, function (resolve) {
                    vm.isLoadingReadyList = false;
                });;
			}
		};

		vm.showMoreCheckList = function(){
            if (!InvoiceService.isSearchMode()){
                InvoiceService.addValueToCurrentPageCheckList();
                InvoiceService.checkInvoicesList();
            }
		};

		vm.newExpense = function(){
			$state.go('^.create', {
				transactionType: 'EXPENSE'
			});
		};

		vm.newIncome = function(){
			$state.go('^.create', {
				transactionType: 'INCOME'
			});
		};

		vm.newFileAnalysis = function(){
			$state.go('fileanalyses.upload');
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

