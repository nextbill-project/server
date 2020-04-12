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
		.directive('msNotification', msNotification);

	msNotification.$inject = ['$rootScope','$compile','$timeout'];

	/* @ngInject */
	function msNotification($rootScope,$compile,$timeout) {
		var directive = {
			restrict: 'E',
			template: '<div class="notifications"></div>',
			replace: true,
			link: link,
			controller: NotificationController,
			controllerAs: 'vm',
			bindToController: true,
		};

		return directive;

		function link(scope, element, attrs) {
			var container = angular.element(element),
				duration = attrs.duration ? attrs.duration : 3000,
				removeDelay = attrs.removeDelay ? attrs.removeDelay : 200;

			$rootScope.$on('createNotification', function(event, received) {
				var toastId = generateUUID();

				var template = '<div class="float-right w-100"><div class="toast m-2 float-right" role="alert" aria-live="assertive" aria-atomic="true" id="'+toastId+'"><div class="toast-header"><div class="mr-auto">Hinweis</div></div><div class="toast-body"><b>'+ received.content+ '</b></div></div></div>',
					notification = angular.element($compile(template)(scope));

				container.append(notification);

				$("#"+toastId).toast({
					delay: (received.duration ? received.duration : duration) - 1000
				}).toast('show');

				$timeout(function() {
					notification.attr('hidden','hidden');

					$timeout(function() {
						$("#"+toastId).toast('hide');
						notification.remove();
					}, removeDelay, false);
				}, received.duration ? received.duration : duration, false);
			});
		}

		function generateUUID(){
			function s4() {
				return Math.floor((1 + Math.random()) * 0x10000)
					.toString(16)
					.substring(1);
			}
			return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
				s4() + '-' + s4() + s4() + s4();
		}
	}

	/* @ngInject */
	function NotificationController() {

	}

})();