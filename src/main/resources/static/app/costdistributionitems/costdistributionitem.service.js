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
		.factory('CostdistributionitemService', CostdistributionitemService);

	CostdistributionitemService.$inject = ['$http'];

	/* @ngInject */
	function CostdistributionitemService($http) {
		var base = {
				invoices: '/webapp/api/invoices',
				costDistributionItems: '/webapp/api/costdistributionitems'
			},
			service = {
				listForInvoice: listForInvoice,
				update: update
			};

		return service;

		////////////////

		function listForInvoice(id){
			return $http({
				method: 'GET',
				url: base.invoices + '/' + id + '/costdistributionitems'
			});
		}

		function update(invoiceId,costDistributionItems) {
			return $http({
				method: 'PUT',
				url: base.invoices + '/' + invoiceId + '/costdistributionitems',
				data: costDistributionItems,
				headers: {
					'Content-Type': 'application/json;charset=UTF-8'
				}
			});
		}
	}

})();

