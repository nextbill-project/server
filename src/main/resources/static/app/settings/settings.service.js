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
		.factory('SettingsService', SettingsService);

	SettingsService.$inject = ['$http','NotificationService'];

		function SettingsService($http,NotificationService) {
			var base = {
                	settings: '/webapp/api/settings'
            	},
				service = {
                    getInit: getInit,
					updateInit: updateInit,
					get: get,
					update: update,
					sendTestMail: sendTestMail,
					scansioEnabled: scansioEnabled,
					mailSendEnabled: mailSendEnabled,
					isCustomized: isCustomized,
					versionCheck: versionCheck
				};

		return service;

		////////////

		function updateInit(setupData){

			return $http({
				method: 'PUT',
				data: setupData,
				url: base.settings + '/initSetupData'
			})
		}

		function getInit() {
			return $http({
				method: 'GET',
				url: base.settings + '/initSetupData'
			});
		}

		function versionCheck() {
			return $http({
				method: 'GET',
				url: base.settings + '/searchForUpdate'
			});
		}

		function update(setupData){

			return $http({
				method: 'PUT',
				data: setupData,
				url: base.settings
			})
		}

		function sendTestMail(setupData){

			return $http({
				method: 'POST',
				data: setupData,
				url: base.settings + '/sendTestMail'
			})
		}

		function get() {
			return $http({
				method: 'GET',
				url: base.settings
			});
		}

		function scansioEnabled() {
			return $http({
				method: 'GET',
				url: base.settings + '/scansioEnabled'
			});
		}

		function mailSendEnabled() {
			return $http({
				method: 'GET',
				url: base.settings + '/mailSendEnabled'
			});
		}

		function isCustomized() {
			return $http({
				method: 'GET',
				url: base.settings + '/isCustomized'
			});
		}

	}

})();