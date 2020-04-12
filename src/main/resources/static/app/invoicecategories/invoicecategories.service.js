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
		.factory('InvoicecategoriesService', InvoicecategoriesService);

	InvoicecategoriesService.$inject = ['$http', '$q'];

	/* @ngInject */
	function InvoicecategoriesService($http, $q) {
		var base = {
				invoicecategories: '/webapp/api/invoicecategories',
				autoAttributes: '/autoAttributes'
			},
			service = {
				list: list,
				getOne: getOne,
                getAutoAttributes: getAutoAttributes,
                getInvoiceCategories: getInvoiceCategories,
                getParentInvoiceCategories: getParentInvoiceCategories,
				create: create,
				remove: remove
			},
			invoiceCategories = [],
            categorieParents = [];

		activate();

		return service;

		////////////////

		function activate(){
            list();
		}

		function remove(invoiceCategoryId){
			return $q(function(resolve) {
				return $http({
					method: 'DELETE',
					url: base.invoicecategories + '/' + invoiceCategoryId
				}).then(
					function(result) {
                        list().then(function(result2){
                            resolve(null);
                        });
					},
					function(result) {
						$log.warn('Could not delete invoice category!','result',result);
						resolve(null);
					}
				);
			});
		}

		function create(invoiceCategoryNameInput, parentInvoiceCategoryId){

			var invoiceCategoryDTO = {
				invoiceCategoryName: invoiceCategoryNameInput,
				parentInvoiceCategoryDTO: {
					invoiceCategoryId: parentInvoiceCategoryId
				}
			};

			return $q(function(resolve) {
				return $http({
					method: 'POST',
					url: base.invoicecategories,
					data: invoiceCategoryDTO,
					headers: {
						'Content-Type': 'application/json;charset=UTF-8'
					}
				}).then(
					function (result) {
						list().then(function(result2){
							resolve(result2.data);
						});
					}
				);
			});
		}

		function getInvoiceCategories(){
			return invoiceCategories;
		}

        function getParentInvoiceCategories(expenseType){

			if (!expenseType){
				return categorieParents;
			}

            return _.filter(categorieParents, function(invoiceCategory){ return (invoiceCategory.invoiceCategoryType === expenseType);});
        }

		function list() {
			return $q(function(resolve) {
				$http({
					method: 'GET',
					url: base.invoicecategories
				}).then(function(result){
					invoiceCategories = result.data;
					categorieParents = _.filter(invoiceCategories, function(invoiceCategory){ return (invoiceCategory.parentInvoiceCategoryDTO === null);});
					resolve(result.data);
				});
			});
		}

		function getOne(uuid) {
			return $http({
				method: 'GET',
				url: base.invoicecategories + '/' + uuid
			});
		}

        function getAutoAttributes(uuid) {
            return $http({
                method: 'GET',
                url: base.invoicecategories + '/' + uuid + base.autoAttributes
            });
        }
	}

})();

