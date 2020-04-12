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
		.controller('BillingDetailsController', BillingDetailsController);

    BillingDetailsController.$inject = ['$state','$stateParams','BillingService'];

	/* @ngInject */
	function BillingDetailsController($state,$stateParams,BillingService) {
		var vm = this;
		vm.title = 'BillingCreateController';

        vm.selected = [];
        
        vm.billing = {};
        
        vm.switched = false;

        if ($stateParams.billingId) {
            vm.billing.billingId = $stateParams.billingId;
        }else{
            $state.go('billings.processList');
        }

        vm.returnViewObject = {
            view: 'billings.details',
            params: {
                billingId: vm.billing.billingId
            }
        };
        
        vm.toggleSwitch = function(){
          vm.switched = !vm.switched;  
        };

        vm.returnSumPaidSum = function(){
			return vm.billing.sumPaid;
        };

        vm.archiveBilling = function(){
            BillingService.archiveBilling(vm.billing.billingId).then(function(result){
				leaveState();
            });
        };

        vm.firstAction = function(){
            BillingService.billingUpdate(vm.billing.billingId, vm.billing.messageType).then(function(result){
                getBilling();
            });
        };

        vm.secondAction = function(){
            BillingService.billingDelete(vm.billing.billingId).then(function(result){
				leaveState();
            });
        };

        vm.getBillingReportUrl = function(){
            return BillingService.billingReportUrl(vm.billing.billingId);
        };
        
        vm.billingServiceLoading = function(){
            return BillingService.isLoading();
        };

        vm.createCompensation = function(){
            BillingService.createCompensation(vm.billing.billingId).then(
                function(result){
                    var params = {
                        invoiceId: result.invoiceId
                    };
                    params.returnView = vm.returnViewObject;
    
                    $state.go('invoices.details', params);
                }
            )
        };

        vm.executeEquality = function(){
            BillingService.executeEquality(vm.billing.billingId).then(function(result){
                vm.billing = result;
            });
        };

        activate();

        ////////////////

		function leaveState() {
			$state.go('^.processList');
		}

        function activate() {
            getBilling();
        }
        
        function getBilling() {
            BillingService.getBilling(vm.billing.billingId).then(function(result){
                vm.billing = result;
            });
        }
	}

})();

