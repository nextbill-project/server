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
		.factory('AnalysisService', AnalysisService);

    AnalysisService.$inject = ['$http','$q', 'NotificationService','SearchService','InvoiceService','_'];

	/* @ngInject */
	function AnalysisService($http, $q, NotificationService, SearchService, InvoiceService, _) {
		var base = {
                chartsWithFilter: '/webapp/api/charts/withFilterBean',
				statistics: '/webapp/api/statistics'
			},
			statistics = [],
			chartData = [],
			isLoadingValue = false,
			service = {
                updateChartData: updateChartData,
                getChartData: getChartData,
				isLoading: isLoading,
				setIsLoading: setIsLoading,
				getStatisticData: getStatisticData
			};

		activate();

		return service;

		////////////////

		function activate(){
			getStatistics();
		}

		function getStatisticData(basicDataSubType){
			var indexOfPosition = statistics.findIndex(function(item){
				return item.basicDataSubType === basicDataSubType;
			});
			
			return statistics[indexOfPosition];
		}

		function isLoading() {
			return isLoadingValue;
		}

		function setIsLoading(isLoadingValueTmp) {
			isLoadingValue = isLoadingValueTmp;
		}

		function updateChartData(useAbsolutValuesTmp, chartTypeTmp) {
			var searchConfiguration = SearchService.getSearchConfiguration();

            // if (SearchService.isSearchConfigurationDefaultOne()){
			    InvoiceService.clearCurrentPageReadyList();
			    InvoiceService.clearCurrentPageCheckList();
                InvoiceService.setSearchMode(false);
                InvoiceService.allInvoices();
            // }else{
            //     InvoiceService.clearCurrentPageReadyList();
            //     InvoiceService.clearCurrentPageCheckList();
            //     InvoiceService.setSearchMode(true);
            //     // SearchService.search();
				// InvoiceService.allInvoices();
            // }

            var tmpSearchConfiguration = angular.copy(searchConfiguration);
            tmpSearchConfiguration.standingOrder = false;

			return $q(function(resolve) {
				isLoadingValue = true;

				$http({
					method: 'POST',
					data: tmpSearchConfiguration,
                    params: {
                        useAbsolutValues: useAbsolutValuesTmp,
                        chartType: chartTypeTmp
                    },
					url: base.chartsWithFilter
				}).then(
					function (result) {
						chartData = result.data;
						isLoadingValue = false;

						resolve(chartData);
					},
					function (result) {
						$log.warn('Could not get chart data!', 'result', result);
					}
				);
			});
		}

		function getStatistics() {

			return $q(function(resolve) {
				$http({
					method: 'GET',
					url: base.statistics
				}).then(
					function (result) {
						statistics = result.data;

						resolve(statistics);
					},
					function (result) {
						$log.warn('Could not get statistic data!', 'result', result);
					}
				);
			});
		}

		function getChartData(){
			return chartData;
		}
	}

})();

