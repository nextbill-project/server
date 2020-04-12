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
		.module('msWebApp')
		.controller('ShellController',ShellController);

    ShellController.$inject = ['$state','$http', 'MessageService', 'NotificationService','BillingService','UserService','SettingsService'];


	function ShellController($state, $http, MessageService, NotificationService, BillingService,UserService,SettingsService) {
		var vm = this;

		vm.currentUser = {appUserName: 'Keine Angabe'};

		vm.userCanEditUsers = false;
		vm.userCanEditSettings = false;

		vm.settingsLoaded = false;

		vm.countOfMessages = function(){
			return MessageService.getMessages().length;
		};

		vm.getMessages = function(){
			var messages = MessageService.getMessages();
			return messages;
		};

		vm.getMessageDTO = function(basicDataMessage){
			return JSON.parse(basicDataMessage.value)
		};

		vm.firstAction = function(basicDataMessage){
			BillingService.billingUpdate(basicDataMessage.object1Id, basicDataMessage.messageType);
		};

		vm.secondAction = function(basicDataMessage){
			BillingService.billingDelete(basicDataMessage.object1Id);
		};

		vm.thirdAction = function(basicDataMessage){
			MessageService.deleteMessage(basicDataMessage.basicDataId);
		};

		vm.problemSolved = function(basicDataMessage){
			MessageService.problemSolved(basicDataMessage.basicDataId);
		};

		vm.goToInvoice = function(invoiceId) {
			var params = {
				invoiceId: invoiceId
			};

			$state.go('invoices.details', params);
		};

		activate();

		function activate(){
			SettingsService.isCustomized().then(function(response){
				if (response.data.value){
					UserService.currentUser().then(function(response){
						vm.currentUser = response;

						UserService.hasUserRight('EDIT_USERS').then(function(result){
							vm.userCanEditUsers = result;
						});

						UserService.hasUserRight('EDIT_SETTINGS').then(function(result){
							vm.userCanEditSettings = result;
						});
					});

					vm.settingsLoaded = true;
				}
			});
		}
	}

})();