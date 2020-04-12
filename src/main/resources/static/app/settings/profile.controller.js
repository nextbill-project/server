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
		.controller('ProfileController', ProfileController);

    ProfileController.$inject = ['UserService', 'NotificationService', 'SettingsService'];

	/* @ngInject */
	function ProfileController(UserService, NotificationService, SettingsService) {
		var vm = this;

		vm.currentUser = {};
		vm.mailSendEnabled = false;

		vm.submitForm = function(){
            UserService.update(vm.currentUser);
		};

		vm.sendPasswordResetMail = function(){
			UserService.sendPassword(vm.currentUser.email).then(
				function(result) {
					NotificationService.create('Eine E-Mail zur Passwort-Änderung wurde verschickt.',5000);
				},
				function(error) {
					NotificationService.create('Leider ist ein unerwarteter Fehler aufgetreten.',8000);
				}
			);
		};

		activate();

        function activate(){
			UserService.currentUser().then(function(response){
				vm.currentUser = response;
			});

			SettingsService.mailSendEnabled().then(function(responseMail){
				vm.mailSendEnabled = responseMail.data.value;
			});
		}

	}

})();

