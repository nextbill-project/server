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
		.controller('BillingProcessListController', BillingProcessListController);

	BillingProcessListController.$inject = ['$state','BillingService', 'UserService'];

	/* @ngInject */
	function BillingProcessListController($state,BillingService, UserService) {
		var vm = this;
		vm.title = 'BillingProcessListController';
		vm.useArchived = false;

		vm.getGroupedBillingListItems = function(){
			var groupedBillingListItems = BillingService.getBillingProcesses();
			return groupedBillingListItems;
		};

		vm.getBillingListProcessItems = function(groupedBillingListItem){
			var billingListProcessItems = groupedBillingListItem.billingListProcessItemDTOs;
			return billingListProcessItems;
		};

		vm.checkReminderMessage = function(appUserId){
			BillingService.checkReminderMessage(appUserId);
		};

		vm.refreshArchived = function(){
			BillingService.setUseArchived(vm.useArchived);
		};

		vm.createBilling = function(billingListItem){

			var paramsTransfer = {
				sumToBePaid: billingListItem.sumToBePaid,
				billingListItem: billingListItem
			};

			UserService.currentUser().then(function(result){
				var currentuser = result;
				
				if (billingListItem.invoicePayer.paymentPersonEnum === 'CONTACT'){

					var foundCostDistribution = _.filter(currentuser.userContactDTOs, function(userContactDTO){ return (userContactDTO.userContactId === billingListItem.invoicePayer.payerId);});
					if (foundCostDistribution != null && foundCostDistribution != undefined && foundCostDistribution.length > 0){
						paramsTransfer.userContact = billingListItem.costPayer;
					}else{
						paramsTransfer.userContact = billingListItem.invoicePayer;
					}
					
				}else{
					paramsTransfer.userContact = billingListItem.costPayer;
				}

				$state.go('billings.create', paramsTransfer);
			});
			

		};

		vm.showCanBeCompensatedWarning = function(billingProcess){
			return (billingProcess.sumToPay !== billingProcess.sumPaid &&
			(billingProcess.messageType === 'PAID' || billingProcess.messageType === 'PAYMENT_CONFIRMED' || billingProcess.messageType === 'FINISHED' || billingProcess.messageType === 'ARCHIVED'));
		};
		
		vm.showBillingPaidPart = function(groupedBillingListItem){
			return (groupedBillingListItem.billingListItemDTO && (groupedBillingListItem.billingListItemDTO.sumToBePaid && groupedBillingListItem.billingListItemDTO.sumToBePaid !== 0));
		};

		vm.showSumToBeChecked = function(billingListItemDTO){
            return (billingListItemDTO && (billingListItemDTO.sumToBeChecked && billingListItemDTO.sumToBeChecked >= 0));
		};

		vm.goToDetails = function(billingListItem){
			var paramsTransfer = {
				billingId: billingListItem.billingId
			};

			$state.go('billings.details', paramsTransfer)
		};

        vm.billingServiceLoading = function(){
            return BillingService.isLoading();
        };

		vm.getBillingReportUrl = function(billingProcess){
			return BillingService.billingReportUrl(billingProcess.billingId);
		};

		vm.archiveBilling = function(billingListProcessItem){
			BillingService.archiveBilling(billingListProcessItem.billingId);
		};

		vm.firstAction = function(billingListProcessItem){
			BillingService.billingUpdate(billingListProcessItem.billingId, billingListProcessItem.messageType);
		};

		vm.secondAction = function(billingListProcessItem){
			BillingService.billingDelete(billingListProcessItem.billingId);
		};

		vm.listBillingProcesses = function(){
            BillingService.listBillingProcesses(vm.useArchived);
		};

		activate();

		////////////////

		function activate() {
            BillingService.listBillingProcesses(false);
            vm.useArchived = BillingService.getUseArchived();
		}
	}

})();

