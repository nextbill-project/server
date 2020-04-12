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
		.factory('SearchService', SearchService);

    SearchService.$inject = ['$http','$log','NotificationService'];

	/* @ngInject */
	function SearchService($http, $log,NotificationService) {
		var base = {
                searchBase: '/webapp/api/search/withFilterBean'
            },
			service = {
				search: search,
				getSearchConfiguration: getSearchConfiguration,
                setSearchConfiguration: setSearchConfiguration,
                resetSearchConfiguration: resetSearchConfiguration,
                repairSearchConfiguration: repairSearchConfiguration,
                isSearchConfigurationDefaultOne: isSearchConfigurationDefaultOne
			},
            defaultSearchConfiguration = {
                incomeMainFunctionEnum: true,
                expenseMainFunctionEnum: true,
                repetitionTypeEnum : null,
                costDistributionId: null,
                specialType: null,
                paymentRecipientPerson: null,
                costPayer: null,
                payerPerson: null,
                invoiceCategoryDTO: null,
                startDate: null,
                endDate: null,
                remarks: null,
                fullText: null
            },
            searchConfiguration = {
                incomeMainFunctionEnum: true,
                expenseMainFunctionEnum: true,
                repetitionTypeEnum : null,
                costDistributionId: null,
                specialType: null,
                paymentRecipientPerson: null,
                costPayer: null,
                payerPerson: null,
                invoiceCategoryDTO: null,
                startDate: null,
                endDate: null,
                remarks: null,
                fullText: null
            };

		return service;

		////////////////

        function resetSearchConfiguration(){
            searchConfiguration = angular.copy(defaultSearchConfiguration);
        }
        
        function setSearchConfiguration(searchConfigurationInput){
            searchConfiguration = searchConfigurationInput;
        }

        function isSearchConfigurationDefaultOne(){
            if (searchConfiguration.startDate == undefined || searchConfiguration.startDate == NaN){
                searchConfiguration.startDate = null;
            }
            if (searchConfiguration.endDate == undefined || searchConfiguration.endDate == NaN){
                searchConfiguration.endDate = null;
            }
            return angular.equals(searchConfiguration, defaultSearchConfiguration);
        }

		function getSearchConfiguration(){
			return searchConfiguration;
		}
		
		function repairSearchConfiguration(){
            if (searchConfiguration.repetitionTypeEnum === ''){
                searchConfiguration.repetitionTypeEnum = null;
            }
            if (searchConfiguration.costDistributionId === ''){
                searchConfiguration.costDistributionId = null;
            }
            if (searchConfiguration.specialType === ''){
                searchConfiguration.specialType = null;
            }
            if (searchConfiguration.remarks === ''){
                searchConfiguration.remarks = null;
            }
        }

		function search(){
            // InvoiceService.setIsLoading(true);

            return $http({
                method: 'POST',
                url: base.searchBase,
                data: searchConfiguration,
                headers: {
                    'Content-Type': 'application/json;charset=UTF-8'
                }
            }).then(
                function(result) {
                    var invoiceList = result.data;

                    // InvoiceService.setInvoices(invoiceList);
                    // InvoiceService.setIsLoading(false);
                },
                function(result) {
                    $log.warn('Could not get invoices!','result',result);
                    // InvoiceService.setIsLoading(false);
                }
            );
		}
	}

})();

