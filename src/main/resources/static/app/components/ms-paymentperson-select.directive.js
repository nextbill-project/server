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
		.module('msWebApp.msPaymentpersonSelect',[])
		.directive('msPaymentpersonSelect', msPaymentpersonSelect);

	msPaymentpersonSelect.$inject = ['$document','$log','_'];

	/* @ngInject */
	function msPaymentpersonSelect($document,$log,_) {
		var directive = {
			restrict: 'E',
			templateUrl: 'app/components/ms-paymentperson-select.html',
			scope: {
				appUsers: '=msUsers',
				userContacts: '=msUsercontacts',
				businessPartners: '=msBusinesspartners',
				projects: '=msProjects',
				callbackPaymentPerson: '=msCallbackPaymentPerson',
				dialogId: '=msDialogId',
				disabled: '<disabled',
				currentSelection: '=msCurrentSelection',
				hideBusinessPartners: '=msHideBusinessPartners',
				hideCurrentUser: '=msHideCurrentUser',
                hideSelectionButton: '=msHideSelectionButton',
				hiddenSelButtonActivator: '=msHiddenSelButtonActivator'
			},
			link: link,
			controller: PaymentpersonSelectController,
			controllerAs: 'vm',
			bindToController: true
		};

		return directive;

		function link(scope, element, attrs, vm) {

		}
	}

	PaymentpersonSelectController.$inject = ['$filter','$log','$timeout','UsercontactService','BusinesspartnerService', 'UserService', '_'];

	/* @ngInject */
	function PaymentpersonSelectController($filter,$log,$timeout,UsercontactService, BusinesspartnerService, UserService, _) {
		var vm = this;

		vm.showDialog = false;

		vm.businessPartnerTimeoutRunning = false;
		vm.appUserTimeoutRunning = false;

		vm.currentUserId = null;
		UserService.currentUser().then(function(result) {
			vm.currentUserId = result.appUserId;
		});

        vm.getUsers = function(){
        	var appUsers= [];

            if (vm.appUsers == undefined || vm.appUsers == null){
				UsercontactService.getUsers().forEach(function(appUser){
					appUsers.push(appUser);
				});
            }else{
				appUsers = vm.appUsers;
			}

			if (vm.hideCurrentUser != undefined && vm.hideCurrentUser != null) {
				var foundAppUser = _.filter(appUsers, function(appUser){ return (appUser.appUserId === vm.currentUserId && appUser.appUserContactId === undefined)});
				if (foundAppUser != null && foundAppUser != undefined && foundAppUser.length > 0){
					var foundAppUserIndex = _.indexOf(appUsers, foundAppUser[0]);
					appUsers.splice(foundAppUserIndex, 1);
				}
			}

			return appUsers;
        };

		vm.getNewBusinessPartnerName = function(){
			return BusinesspartnerService.getBusinessPartnerNewName();
		};

		vm.getFoundAppUser = function(){
			return UsercontactService.getFoundAppUser();
		};

		vm.createNewBusinessPartner = function(){
			BusinesspartnerService.create().then(
				function(result) {
					vm.selectBusinessPartner(result);
				}
			);

		};

		vm.createNewProject = function(){
			UsercontactService.createProject(vm.projectsSearch).then(
				function(result) {
					vm.selectProject(result);
				}
			);
		};

		vm.createNewUserContact = function(){
			UsercontactService.createUserContact(vm.userContactsSearch).then(
				function(result) {
					vm.selectUserContact(result);
				}
			);;
		};

		vm.createNewAppUserContact = function(){
			UsercontactService.createAppUserContact().then(
				function(result) {
					vm.selectUser(result);
				}
			);
		};

		vm.removeBusinessPartner = function(businessPartner){
			BusinesspartnerService.remove(businessPartner.businessPartnerId);
		};

		vm.removeUserContact = function(userContact){
			UsercontactService.remove(userContact.userContactId);
		};

		vm.businessPartnerInputDone = function(){
			if (vm.businessPartnerTimeoutRunning == false){
				vm.businessPartnerTimeoutRunning = true;
				$timeout(function(){
					if (vm.businessPartnerInput.length > 1){
						BusinesspartnerService.search(vm.businessPartnerInput);
					}
					vm.businessPartnerTimeoutRunning = false;
				}, 400);
			}
		};

		vm.appUserInputDone = function(){
			if (vm.appUserTimeoutRunning == false){
				vm.appUserTimeoutRunning = true;
				$timeout(function(){
					if (vm.usersSearch.length > 1){
						UsercontactService.searchForUser(vm.usersSearch);
					}
					vm.appUserTimeoutRunning = false;
				}, 400);
			}
		};

        vm.getUserContacts = function(){
            if (vm.userContacts == undefined || vm.userContacts == null){
                return UsercontactService.getUserContacts();
            }else{
                return vm.userContacts;
            }
        };

        vm.getProjects = function(){
            if (vm.projects == undefined || vm.projects == null){
                return UsercontactService.getProjects();
            }else{
                return vm.projects;
            }
        };

        vm.getBusinessPartners = function(){
            if (vm.businessPartners == undefined || vm.businessPartners == null){
                return BusinesspartnerService.getBusinessPartners();
            }else{
                return vm.businessPartners;
            }
        };

		vm.getDialogId = function(customIdentifier){
			var resultId = '#' + vm.dialogId;
			if (customIdentifier != undefined){
				resultId = resultId + customIdentifier;
			}
			return resultId;
		};

		vm.getDialogIdWithoutHash = function(customIdentifier){
			var resultId = vm.dialogId;
			if (customIdentifier != undefined){
				resultId = resultId + customIdentifier;
			}
			return resultId;
		};

		vm.selectBusinessPartner = function(businessPartner){
			vm.callbackPaymentPerson({
				name: businessPartner.businessPartnerName,
				id: businessPartner.businessPartnerId,
				type: 'BUSINESS_PARTNER'
			});

			vm.resetValues();
		};

		vm.selectUser = function(user){

			if (user.userContactId != undefined && user.userContactId != null){
				vm.callbackPaymentPerson({
					name: user.contactName,
					id: user.userContactId,
					type: 'CONTACT',
					virtualType: 'USER'
				});
			}else{
				vm.callbackPaymentPerson({
					name: user.contactName,
					id: user.appUserId,
					type: 'USER'
				});
			}

			vm.resetValues();
		};

		vm.selectUserContact = function(userContact){
			vm.callbackPaymentPerson({
				name: userContact.contactName,
				id: userContact.userContactId,
				type: 'CONTACT'
			});
			vm.resetValues();
		};

		vm.selectProject = function(project){
			vm.callbackPaymentPerson({
				name: project.contactName,
				id: project.userContactId,
				type: 'CONTACT'
			});
			vm.resetValues();
		};

        vm.selectNone = function(){
            vm.callbackPaymentPerson(null);
			vm.resetValues();
        };

		vm.searchFilterForBusinessPartners = function() {
			var searchMatch = (vm.businessPartnerSearch ? new RegExp(".*" + vm.businessPartnerSearch + ".*", 'i') : null);
			return function(item) {
				if (searchMatch && !searchMatch.test(item.businessPartnerName)) {
					return false;
				}
				return true;
			};
		};

		vm.searchFilterForUsers = function() {
			var searchMatch = (vm.usersSearch ? new RegExp(".*" + vm.usersSearch + ".*", 'i') : null);
			return function(item) {
				if (searchMatch && !searchMatch.test(item.contactName)) {
					return false;
				}
				return true;
			};
		};

		vm.searchFilterForUserContacts = function() {
			var searchMatch = (vm.userContactsSearch ? new RegExp(".*" + vm.userContactsSearch + ".*", 'i') : null);
			return function(item) {
				if (searchMatch && !searchMatch.test(item.contactName)) {
					return false;
				}
				return true;
			};
		};

		vm.searchFilterForProjects = function() {
			var searchMatch = (vm.projectsSearch ? new RegExp(".*" + vm.projectsSearch + ".*", 'i') : null);
			return function(item) {
				if (searchMatch && !searchMatch.test(item.contactName)) {
					return false;
				}
				return true;
			};
		};

		vm.resetValues = function() {
			setTimeout(function() {
					vm.businessPartnerInput = "";
					vm.userContactsSearch = "";
					vm.projectsSearch = "";
					vm.usersSearch = "";

					BusinesspartnerService.search('');
					UsercontactService.searchForUser('');
				}
				, 1000);
		};
	}

})();

