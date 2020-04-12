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
		.factory('NotificationService', NotificationService);

	NotificationService.$inject = ['$log','$rootScope','$translate'];

	/* @ngInject */
	function NotificationService($log,$rootScope,$translate) {
		var service = {
			create: create
		};

		return service;

		////////////////

		function create(content, duration) {
			$translate(content).then(function(translatedContent) {
				$rootScope.$broadcast('createNotification', {
					'content': translatedContent,
					'duration': duration
				});
			}, function(translationKey) {
				$log.error('No translation available:',translationKey);
				$rootScope.$broadcast('createNotification', {
					'content': content,
					'duration': duration
				});
			});
		}
	}
})();