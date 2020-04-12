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
		.controller('BillingCreateController', BillingCreateController);

    BillingCreateController.$inject = ['$state','$stateParams','BillingService','SettingsService'];

	/* @ngInject */
	function BillingCreateController($state,$stateParams,BillingService,SettingsService) {
		var vm = this;
		vm.title = 'BillingCreateController';

        vm.switched = false;

        vm.smtpMailServiceEnabled = false;

        vm.toggleSwitch = function(){
            vm.switched = !vm.switched;
        };

        if ($stateParams.userContact) {
            vm.userContact = $stateParams.userContact;
        }else{
            $state.go('billings.processList');
        }

        vm.returnViewObject = {
            view: 'billings.processList'
        };

        vm.billingConfig = {
            costPayerMail: vm.userContact.email
        };

        if ($stateParams.sumToBePaid) {
            vm.sumToBePaid = $stateParams.sumToBePaid;
        }

        if ($stateParams.billingListItem) {
            vm.billingListItem = $stateParams.billingListItem;
        }

        vm.getCategoriesForInvoicePayer = function(billing){
            vm.categoriesInvoicePayer = _.map(billing.invoiceCategoriesOfInvoicePayer, function(category){ return category.invoiceCategoryName; }).join(', ');
            return vm.categoriesInvoicePayer;
        };

        vm.getCategoriesForCostPayer = function(billing){
            vm.categoriesCostPayer = _.map(billing.invoiceCategoriesOfCostPayer, function(category){ return category.invoiceCategoryName; }).join(', ');
            return vm.categoriesCostPayer;
        };

        vm.createBilling = function(){

			vm.billingConfig.startDate = null;
			vm.billingConfig.endDate = null;

			vm.billingConfig.userSelectionPaymentPersonTypeEnum= vm.userContact.paymentPersonTypeEnum;
			vm.billingConfig.userSelection = vm.userContact.paymentPersonId;

			if (vm.billingConfig.sendMailInvoicePayer == undefined || vm.billingConfig.sendMailInvoicePayer == null){
                vm.billingConfig.sendMailInvoicePayer = false;
			}
            if (vm.billingConfig.sendMailCostPayer == undefined || vm.billingConfig.sendMailCostPayer == null){
                vm.billingConfig.sendMailCostPayer = false;
            }

            vm.lockBillingButton = true;
            
            BillingService.createBilling(vm.billingConfig).then(function(result){
                $state.go('billings.processList')
            });



        };

        activate();

        function activate() {
            SettingsService.get().then(function(response){
                vm.smtpMailServiceEnabled = response.data.smtpMailServiceEnabled;

            });
        }
	}

})();

