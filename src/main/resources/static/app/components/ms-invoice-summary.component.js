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

(function() {
	'use strict';

	angular
		.module('msWebApp.msInvoiceSummary', [
			'moment-picker'
		])
		.component('msInvoiceSummary', {
			templateUrl: 'app/components/ms-invoice-summary.html',
			bindings: {
				invoice: '<msInvoice',
                isStandingOrder: '<?msIsStandingOrder',
                isReadyOnlyMode: '<?msReadOnly',
                returnView: '=?msReturnView'
			},
			controller: InvoiceSummaryController,
			controllerAs: 'vm'
		});

    InvoiceSummaryController.$inject = ['$timeout','$state','InvoiceService','CostdistributionitemService','InvoiceimageService','NotificationService','_', 'UserService'];

	/* @ngInject */
	function InvoiceSummaryController($timeout,$state,InvoiceService,CostdistributionitemService,InvoiceimageService,NotificationService,_,UserService) {
        var vm = this;
        vm.title = 'InvoiceSummaryController';

        vm.costDistributionItems = [];
        vm.payerName = '';
        vm.paymentRecipientName = '';

        vm.currentUser = {};

        vm.hasInvoiceImageValue = false;

        vm.repetionTypeEnums = [];
        vm.paymentTypeEnums = [];

        vm.standingOrder = {};
        vm.originalStandingOrder = {};
        vm.costDistributions = [];

        vm.goToInvoice = function() {
            // if (vm.invoice.invoiceStatusEnum !== 'ANALYZING'){

                var params = {
                    invoiceId: vm.invoice.invoiceId
                };
                if (vm.returnView != undefined){
                    params.returnView = vm.returnView;
                }
                $state.go('invoices.details', params);
            // }else{
            //     NotificationService.create('Rechnung wird gerade analysiert! Bitte warten!',3000);
            // }
        };

        vm.isReadOnlyMode = function(){
            if (vm.invoice.invoiceWorkflowMode === 'EXTERNAL_USER_CHECK_MODE' || vm.invoice.invoiceWorkflowMode === 'EXTERNAL_USER_READY_MODE')	{
                return true;
            }

            return false;
        };

        vm.remove = function() {
            if (vm.isStandingOrder){
                vm.removeInvoiceTemplate();
            }else{
                vm.removeInvoice();
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


        vm.hasInvoiceImage = function(){
            vm.hasInvoiceImageValue = !!vm.invoice.invoiceImageId;
        };

        vm.getInvoiceImageUrl = function(){
            return InvoiceimageService.getInvoiceImageUrl(vm.invoice.invoiceImageId);
        };

        vm.removeInvoice = function() {
            InvoiceService.remove(vm.invoice.invoiceId);
            goToOverview();
        };

        vm.removeInvoiceTemplate = function() {
            InvoiceService.deleteStandingOrder(vm.invoice.invoiceId);
            goToOverview();
        };

        vm.isNullOrUndefined = function(value){
            if (value === null || value === undefined){
                return true;
            }
            return false;
        };

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

        function getCurrentUser(todoAfter) {
            UserService.currentUser().then(function(result) {
                vm.currentUser = result;

                if (todoAfter != undefined){
                    todoAfter();
                }
            });
        }

        function toDoAfterCurrentUserGet(){

            if (vm.invoice.mainFunctionEnum === 'INCOME'){
                vm.invoice.paymentRecipientId = vm.currentUser.appUserId;
                vm.invoice.paymentRecipientTypeEnum = 'USER';
            }else{
                vm.invoice.payerId = vm.currentUser.appUserId;
                vm.invoice.payerTypeEnum = 'USER';
            }
            vm.originalInvoice = angular.copy(vm.invoice);
        }

        function activate() {

            getCurrentUser();
            getInvoice();

            vm.standingOrder.invoiceTemplateId = vm.invoice.invoiceId;
            vm.standingOrder.futureInvoiceTemplateId = generateUUID();
            getStandingOrder();
            getRepetitionTypeEnums();
            getPaymentTypeEnums();
        }

        function refreshInvoiceData(){
            // vm.isStandingOrder = false;
            // if (vm.invoice.standingOrderInvoiceTemplateId === vm.invoice.invoiceId){
            // 	vm.isStandingOrder = true;
            // }

            vm.hasInvoiceImage();

            if (vm.invoice.paymentRecipientId != null){
                vm.paymentRecipientName = vm.invoice.paymentRecipientDTO.displayName;
            }

            if (vm.invoice.payerId != null){
                vm.payerName = vm.invoice.payerDTO.displayName;
            }
        }

        function getInvoice() {
            InvoiceService.details(vm.invoice.invoiceId).then(function(response) {
                vm.invoice = response.data;

                refreshInvoiceData();

				vm.costDistributionItems = vm.invoice.costDistributionItemDTOs;
				vm.originalCostDistributionItems = angular.copy(vm.costDistributionItems);

                vm.originalInvoice = angular.copy(vm.invoice);
            });
        }

        function getRepetitionTypeEnums() {
            InvoiceService.repetitionTypeEnumList().then(function(result) {
                vm.repetionTypeEnums = result.data;

                if (vm.isStandingOrder){
                    var foundRepetionTypeEnum = _.filter(vm.repetionTypeEnums, function(repetionTypeEnum){ return (repetionTypeEnum.name === 'ONCE');});
                    if (foundRepetionTypeEnum != null && foundRepetionTypeEnum != undefined && foundRepetionTypeEnum.length > 0){
                        var foundRepetionTypeEnumIndex = _.indexOf(vm.repetionTypeEnums, foundRepetionTypeEnum[0]);

                        vm.repetionTypeEnums.splice(foundRepetionTypeEnumIndex, 1);
                    }
                }
            });
        }

        function getPaymentTypeEnums() {
            InvoiceService.paymentTypeEnumList().then(function(result) {
                vm.paymentTypeEnums = result.data;
            });
        }

        function getStandingOrder(){
            if (vm.isStandingOrder){
                $timeout(function(){
                    InvoiceService.getStandingOrder(vm.invoice.invoiceId).then(function(result){
                        vm.standingOrder.repetitionTypeEnum = result.repetitionTypeEnum;
                        vm.standingOrder.startDate = result.startDate;

                        vm.originalStandingOrder = angular.copy(vm.standingOrder);
                    }, function(response){
                        vm.standingOrder.startDate = new Date().getTime();
                        vm.standingOrder.repetitionTypeEnum = 'MONTHLY';
                        vm.originalStandingOrder = {
                            repetitionTypeEnum: vm.standingOrder.repetitionTypeEnum,
                            startDate: vm.standingOrder.startDate
                        }

                        vm.invoice.repetitionTypeEnum = vm.standingOrder.repetitionTypeEnum;
                        vm.originalInvoice.repetitionTypeEnum = vm.standingOrder.repetitionTypeEnum;
                    })
                }, 100);
            }else{
                vm.originalStandingOrder = angular.copy(vm.standingOrder);
            }
        }

        function getCostDistributionItems(){
            CostdistributionitemService.listForInvoice(vm.invoice.invoiceId).then(
                function(response) {
                    vm.costDistributionItems = response.data;

                    vm.originalCostDistributionItems = angular.copy(vm.costDistributionItems);
                }, function(error){
                }
            );
        }
	}

})();