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
		.module('msWebApp.msSearchConfiguration',[])
		.directive('msSearchConfiguration', msSearchConfiguration);

    msSearchConfiguration.$inject = ['$document','$log', '_'];

	/* @ngInject */
	function msSearchConfiguration($document,$log, _) {
		var directive = {
			restrict: 'E',
			templateUrl: 'app/components/ms-search-configuration.html',
			scope: {
                startSearch: '=msStartSearch'
			},
			link: link,
			controller: SearchConfigurationController,
			controllerAs: 'vm',
			bindToController: true
		};

		return directive;

		function link(scope, element, attrs, vm) {

		}
	}

    SearchConfigurationController.$inject = ['$filter','$timeout','$log','SearchService','CostdistributionService','_'];

	/* @ngInject */
	function SearchConfigurationController($filter,$timeout,$log,SearchService,CostdistributionService,_) {
		var vm = this;

        vm.payerPersonSearchConfiguration = 'payerPersonSearchConfiguration' + Math.floor(Math.random() * (10000000 - 1)) + 1;
		vm.paymentRecipientPersonSearchConfiguration = 'paymentRecipientPersonSearchConfiguration' + Math.floor(Math.random() * (10000000 - 1)) + 1;
		vm.costPayerSearchConfiguration = 'costPayerSearchConfiguration' + Math.floor(Math.random() * (10000000 - 1)) + 1;
        vm.invoiceCategorySearchConfiguration = 'invoiceCategorySearchConfiguration' + Math.floor(Math.random() * (10000000 - 1)) + 1;

        vm.repetitionTypeEnums = [
            {enumName:null, displayName : 'Keine Auswahl'},
            {enumName:'ONCE', displayName : 'Einmalig'},
            {enumName:'MONTHLY', displayName : 'Monatlich'},
            {enumName:'QUARTER', displayName : 'Vierteljährlich'},
            {enumName:'HALF_YEAR', displayName : 'Halbjährlich'},
            {enumName:'ANNUALLY', displayName : 'Jährlich'}
        ];

        vm.booleanSelections = [
            {enumName:null, displayName : 'Keine Auswahl'},
            {enumName:true, displayName : 'Ja'},
            {enumName:false, displayName : 'Nein'}
        ];

        vm.remarksTimeoutRunning = false;
        vm.remarksInput = '';

        vm.fullTextTimeoutRunning = false;
        vm.fullTextInput = '';

        vm.remarksInputDone = function(){
            if (vm.remarksTimeoutRunning === false){
                vm.remarksTimeoutRunning = true;
                $timeout(function(){
                    var searchConfiguration = SearchService.getSearchConfiguration();
                    searchConfiguration.remarks = vm.remarksInput;
                    vm.startSearch();
                    vm.remarksTimeoutRunning = false;
                }, 1000);
            }
        };

        vm.fullTextInputDone = function(){
            if (vm.fullTextTimeoutRunning === false){
                vm.fullTextTimeoutRunning = true;
                $timeout(function(){
                    var searchConfiguration = SearchService.getSearchConfiguration();
                    searchConfiguration.fullText = vm.fullTextInput;
                    vm.startSearch();
                    vm.fullTextTimeoutRunning = false;
                }, 1000);
            }
        };

        vm.getPayerPersonSelection = function(){
			var searchConfiguration = SearchService.getSearchConfiguration();
			if (searchConfiguration.payerPerson !== undefined && searchConfiguration.payerPerson != null){
				return searchConfiguration.payerPerson.payerName;
			}

			return null;
		};

		vm.getCostDistributions = function(){
        	return CostdistributionService.getCostDistributions();
        };

		vm.repairSearchConfiguration = function(){
		    SearchService.repairSearchConfiguration();
        };

		vm.updateSearchConfiguration = function(){
            SearchService.getSearchConfiguration();

        };

        vm.getPaymentRecipientPersonSelection = function(){
            var searchConfiguration = SearchService.getSearchConfiguration();
            if (searchConfiguration.paymentRecipientPerson != undefined && searchConfiguration.paymentRecipientPerson != null){
                return searchConfiguration.paymentRecipientPerson.payerName;
            }

            return null;
        };

        vm.getCostPayerSelection = function(){
            var searchConfiguration = SearchService.getSearchConfiguration();
            if (searchConfiguration.costPayer != undefined && searchConfiguration.costPayer != null){
                return searchConfiguration.costPayer.payerName;
            }

            return null;
        };

		vm.setPayerPerson = function(payerPerson){
            var searchConfiguration = SearchService.getSearchConfiguration();
            if (payerPerson != null){
                searchConfiguration.payerPerson = {};
                searchConfiguration.payerPerson.payerId = payerPerson.id;
                searchConfiguration.payerPerson.payerEnum = payerPerson.type;
                searchConfiguration.payerPerson.payerName = payerPerson.name;
            }else{
                searchConfiguration.payerPerson = null;
            }
            vm.startSearch();
		};

        vm.setPaymentRecipientPerson = function(paymentRecipientPerson){
            var searchConfiguration = SearchService.getSearchConfiguration();
            if (paymentRecipientPerson != null){
                searchConfiguration.paymentRecipientPerson = {};
                searchConfiguration.paymentRecipientPerson.payerId = paymentRecipientPerson.id;
                searchConfiguration.paymentRecipientPerson.payerEnum = paymentRecipientPerson.type;
                searchConfiguration.paymentRecipientPerson.payerName = paymentRecipientPerson.name;
            }else{
                searchConfiguration.paymentRecipientPerson = null;
            }
            vm.startSearch();
        };

        vm.setCostPayer = function(costPayer){
            var searchConfiguration = SearchService.getSearchConfiguration();
            if (costPayer != null){
                searchConfiguration.costPayer = {};
                searchConfiguration.costPayer.payerId = costPayer.id;
                searchConfiguration.costPayer.payerEnum = costPayer.type;
                searchConfiguration.costPayer.payerName = costPayer.name;
            }else{
                searchConfiguration.costPayer = null;
            }
            vm.startSearch();
        };

        vm.selectInvoiceCategory = function(invoiceCategory){
            var searchConfiguration = SearchService.getSearchConfiguration();
            searchConfiguration.invoiceCategoryDTO = invoiceCategory;
            vm.startSearch();
		};

        vm.costDistributionItems = function(){
            return CostdistributionService.getCostDistributions();
        };

		vm.getSearchConfiguration = function(){
			return SearchService.getSearchConfiguration();
		};
	}

})();

