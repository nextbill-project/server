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
		.factory('UserService', UserService);

    UserService.$inject = ['$http', '$q', 'NotificationService'];

		function UserService($http, $q, NotificationService) {
			var base = {
                	users: '/webapp/api/appusers',
					sendPassword: '/webapp/api',
            	},
				service = {
                    update: update,
					adminUpdate: adminUpdate,
					adminDelete: adminDelete,
					sendPassword: sendPassword,
					currentUser: currentUserFun,
					list: list,
					hasUserRight: hasUserRight,
					resetUser: resetUser
				},
				currentUser = null;

		return service;

		////////////

		function list(){
			return $http({
				method: 'GET',
				url: base.users,
			})
		}

		function sendPassword(mail){

			return $http({
				method: 'POST',
				url: base.sendPassword + '/sendPasswordKey',
				data: {value: mail},
				headers: {
					'Content-Type': 'application/json;charset=UTF-8'
				}
			})
		}

		function adminUpdate(appUserDTO){

			return $http({
				method: 'PUT',
				url: base.users,
				data: appUserDTO,
				headers: {
					'Content-Type': 'application/json;charset=UTF-8'
				}
			})
		}

		function resetUser() {
			currentUser = null;
			currentUserFun();
		}

		function adminDelete(appUserDTO){

			return $http({
				method: 'DELETE',
				url: base.users + '/' + appUserDTO.appUserId,
				data: appUserDTO,
				headers: {
					'Content-Type': 'application/json;charset=UTF-8'
				}
			})
		}

		function update(appUserDTO){

			$http({
				method: 'PUT',
				url: base.users + '/currentUser',
				data: appUserDTO,
				headers: {
					'Content-Type': 'application/json;charset=UTF-8'
				}
			}).then(
				function(result) {
					NotificationService.create('Profil wurde aktualisiert!',5000);
				}
			);
		}

		function currentUserFun() {

			return $q(function(resolve, error) {
				if (currentUser != null){
					resolve(currentUser);
				}else{
					$http({
						method: 'GET',
						url: base.users + '/currentUser'
					}).then(function(response){
						currentUser = response.data;
						resolve(currentUser);
					},function(response){
						error(null);
					});
				}
			});

		}

		function hasUserRight(rightCode) {

			return $q(function(resolve) {
				currentUserFun().then(function(user){
					resolve(findRight(user.appRightDTOs, rightCode));
				});
			});
		}

		function findRight(roles, rightCode){
			var matchedRights = _.chain(roles)
				.pluck('code')
				.intersection([rightCode])
				.value();

			if (matchedRights.length > 0) {
				return true
			}else{
				return false;
			}
		}

	}

})();