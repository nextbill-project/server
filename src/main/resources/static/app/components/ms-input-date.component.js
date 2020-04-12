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
		.module('msWebApp.msInputDate', [
			'moment-picker'
		])
		.component('msInputDate', {
			templateUrl: 'app/components/ms-input-date.html',
			bindings: {
				value: '=msValue',
				required: '<?msRequired',
				disabled: '<?msDisabled',
				setNull: '<?msSetNull',
				callback: '&?msOnChange'
			},
			controller: InputDateController,
			controllerAs: 'vm'
		});

	InputDateController.$inject = ['$element', '$log', '$scope', '_'];

	/* @ngInject */
	function InputDateController($element, $log, $scope, _) {
		var vm = this;

		vm.$onInit = function() {
			$scope.$watch('vm.value', function(newValue) {
				vm.timestamp = newValue;
				vm.date = timestampToDate(newValue);
			});
		};

		vm.$onChanges = function(changes) {
			if (changes.required) {
				setRequiredProperty(changes.required.currentValue);
			}
		};

		vm.dateToTimestamp = function() {
			vm.timestamp = dateToTimestamp(vm.date);
			vm.value = vm.timestamp;
			if (angular.isFunction(vm.callback)) {
				vm.callback();
			}
		};

		function timestampToDate(timestamp) {
			var intTimestamp, date;

			if (!isNaN(timestamp) && timestamp != null && timestamp != undefined) {
				intTimestamp = parseInt(timestamp);
				date = new Date(intTimestamp);

				return date;
			} else {
				return null;
			}
		}

		vm.setDateToNull = function(){
			if (vm.setNull === undefined || vm.setNull === true){
				vm.date = null;
				vm.timestamp = null;
				vm.value = vm.timestamp;
			}
		};

		function setRequiredProperty(required) {
			if (required) {
				$element.attr('data-required','required');
			} else {
				$element.removeAttribute('data-required');
			}
		}

		function dateToTimestamp(date) {
			if (angular.isDate(date)) {
				return date.getTime();
			} else {
				return null;
			}
		}
	}

})();