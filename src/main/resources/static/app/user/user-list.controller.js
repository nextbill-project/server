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
		.controller('UserListController', UserListController);

	UserListController.$inject = ['UserService', 'SettingsService','NotificationService', '_'];

	/* @ngInject */
	function UserListController(UserService, SettingsService,NotificationService, _) {
		var vm = this;

		vm.users = [];
		vm.key = null;
		vm.userToDelete = null;

		vm.scansioEnabled = false;

		vm.lockUser = function(user) {
			user.editable = false;

			if (user.new) {
				var indexOfUser = _.indexOf(vm.users, user);
				vm.users.splice(indexOfUser, 1);
			}
		};

		vm.updateUser = function(user){

			if (!user.appUserId) {
				user.appUserId = generateUUID();
			}

            UserService.adminUpdate(user).then(
				function(result) {
					NotificationService.create('Benutzer wurde aktualisiert!',5000);
					user.editable = false;
					user.new = false;
					user = result.data;

					var foundUser = _.filter(vm.users, function(tmpUser){ return (tmpUser.appUserId === user.appUserId);});
					if (foundUser && foundUser.length > 0){
						var foundUserIndex = _.indexOf(vm.users, foundUser[0]);

						vm.users[foundUserIndex] = result.data;
					}

					UserService.resetUser();
				},
				function(error) {
					NotificationService.create('Ausführung nicht möglich!',8000);
					UserService.list().then(function(response){
						vm.users = response.data;
					});

					user.editable = false;
					if (user.new) {
						var indexOfUser = _.indexOf(vm.users, user);
						vm.users.splice(indexOfUser, 1);
					}
				}
			);
		};

		vm.showDeleteUserDialog = function(user) {
			vm.userToDelete = user;
			$('#userDeleteModalDialog').modal('show');
		};

		vm.deleteUser = function(user){
			UserService.adminDelete(user).then(
				function(result) {
					NotificationService.create('Benutzer wurde gelöscht!',5000);
					var indexOfUser = _.indexOf(vm.users, user);
					vm.users.splice(indexOfUser, 1);
					vm.userToDelete = null;

					$('#userDeleteModalDialog').modal('hide');
				},
				function(error) {
					NotificationService.create('Ausführung nicht möglich!',8000);
					UserService.list().then(function(response){
						vm.users = response.data;
					});

					user.editable = false;
					if (user.new) {
						var indexOfUser = _.indexOf(vm.users, user);
						vm.users.splice(indexOfUser, 1);
					}
				}
			);
		};

		vm.addUser = function() {
			vm.users.unshift({
				new: true,
				editable: true,
				enabled: true,
				useAndroid: true,
				useOcr: false,
				editUsers: false,
				editSettings: false}
			)
		};

		vm.sendPassword = function(mail){
			UserService.sendPassword(mail).then(
				function(result) {
					if (result.status === 200){
						vm.key = result.data.value;
						$('#passwordResetModalDialog').modal('show');
					}else{
						NotificationService.create('Eine E-Mail zur Wiederherstellung wurde verschickt.',5000);
					}
					user.editable = false;
				},
				function(error) {
					NotificationService.create('Leider ist ein unerwarteter Fehler aufgetreten.',8000);
					user.editable = false;
				}
			);
		};

		activate();

		function generateUUID(){
			function s4() {
				return Math.floor((1 + Math.random()) * 0x10000)
					.toString(16)
					.substring(1);
			}
			return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
				s4() + '-' + s4() + s4() + s4();
		}

        function activate(){

			SettingsService.scansioEnabled().then(function(responseScansio){
				vm.scansioEnabled = responseScansio.data.value;
				UserService.list().then(function(response){
					vm.users = response.data;
				},function(error){
					NotificationService.create('Benutzer konnnten nicht geladen werden.',4000);
				});
			});
		}

	}

})();

