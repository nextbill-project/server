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
		.factory('UsercontactService', UsercontactService);

	UsercontactService.$inject = ['$http','$q'];

	/* @ngInject */
	function UsercontactService($http,$q) {
		var base = {
				usercontacts: '/webapp/api/usercontacts',
				findByEmail: '/webapp/api/appusers/findForUserContact',
				currentUser: '/webapp/api/appusers/currentUser'
			},
			service = {
				list: list,
				getOne: getOne,
                getUsers: getUsers,
                getUserContacts: getUserContacts,
                getProjects: getProjects,
				createProject: createProject,
				createUserContact: createUserContact,
				remove: remove,
				createAppUserContact: createAppUserContact,
				getFoundAppUser: getFoundAppUser,
				searchForUser: searchForUser,
				getCurrentUSer: getCurrentUser
			},
			foundAppUser = null,
			allContacts = [],
            userContacts = [],
            users = [],
            projects = [];

		activate();

		return service;

		////////////////

		function list() {
			return $http({
				method: 'GET',
				url: base.usercontacts
			});
		}

		function getCurrentUser(){
			return $q(function(resolve) {
				$http({
					method: 'GET',
					url: base.currentUser
				}).then(function(response){
					var currentUserData = response.data;
					currentUserData.contactName = currentUserData.appUserName;
					allContacts.push(currentUserData);
					resolve(currentUserData);
				})
			});
		}
		
		function getFoundAppUser(){
			return foundAppUser;
		}

		function getUsers(){
			return users;
		}

        function getUserContacts(){
            return userContacts;
        }

        function getProjects(){
            return projects;
        }

		function activate(){

            list().then(
                function(response) {
                    allContacts = response.data;
					getCurrentUser().then(function(response){
						distributeContacts();
					});
                }
            );
		}

		function searchForUser(searchMail) {

			var appUserDTO = {
				email: searchMail
			};

			$http({
				method: 'POST',
				url: base.findByEmail,
				data: appUserDTO,
				headers: {
					'Content-Type': 'application/json;charset=UTF-8'
				}
			}).then(
				function(response){
				foundAppUser = response.data;
			},
			function(response){
				foundAppUser = null;
			});
		}

		function distributeContacts(){
			userContacts = _.filter(allContacts, function(userContact){ return (userContact.appUserContactId === null && (userContact.project == null || userContact.project == undefined));});
			users = _.filter(allContacts, function(userContact){ return (userContact.appUserContactId != null && (userContact.project == null || userContact.project == undefined))});
			projects = _.filter(allContacts, function(userContact){ return (userContact.project != null);});

			var foundUsers = _.filter(allContacts, function(userContact){ return (userContact.appUserName != null)});
			foundUsers.forEach(function(foundUser) {
				users.push(foundUser);
			});
		}

		function getOne(uuid) {
			return $http({
				method: 'GET',
				url: base.usercontacts + '/' + uuid
			});
		}

		function createAppUserContact(){

			var userContactDTO = {
				appUserContactId: foundAppUser.appUserId,
				contactName: foundAppUser.appUserName,
				email: foundAppUser.email
			};

			return $q(function(resolve) {
				$http({
					method: 'POST',
					url: base.usercontacts,
					data: userContactDTO,
					headers: {
						'Content-Type': 'application/json;charset=UTF-8'
					}
				}).then(
					function(result) {
						allContacts.push(result.data);
						foundAppUser = null;
						distributeContacts();
						resolve(result.data);
					}
				);
			});

		}

		function createProject(projectNewName){

			var userContactDTO = {
				contactName: projectNewName,
				project: true
			};

			return $q(function(resolve) {
				$http({
					method: 'POST',
					url: base.usercontacts,
					data: userContactDTO,
					headers: {
						'Content-Type': 'application/json;charset=UTF-8'
					}
				}).then(
					function(result) {
						allContacts.push(result.data);
						distributeContacts();
						resolve(result.data);
					}
				);
			});

		}

		function createUserContact(userContactNewName){

			var userContactDTO = {
				contactName: userContactNewName
			};

			return $q(function(resolve) {
				$http({
					method: 'POST',
					url: base.usercontacts,
					data: userContactDTO,
					headers: {
						'Content-Type': 'application/json;charset=UTF-8'
					}
				}).then(
					function(result) {
						allContacts.push(result.data);
						distributeContacts();
						resolve(result.data);
					}
				);
			});

		}

		function remove(userContactId){
			return $q(function(resolve) {
				return $http({
					method: 'DELETE',
					url: base.usercontacts + '/' + userContactId
				}).then(
					function(result) {

						var foundUserContact = _.filter(allContacts, function(userContact){ return (userContact.userContactId === userContactId);});
						if (foundUserContact != null && foundUserContact != undefined && foundUserContact.length > 0){
							var foundUserContactIndex = _.indexOf(allContacts, foundUserContact[0]);

							allContacts.splice(foundUserContactIndex, 1);

							distributeContacts();
						}
						resolve(null);
					},
					function(result) {
						$log.warn('Could not delete!','result',result);
						resolve(null);
					}
				);
			});
		}
	}

})();

