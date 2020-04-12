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
		.factory('CostdistributionService', CostdistributionService);

	CostdistributionService.$inject = ['$log','$q','$http', 'NotificationService'];

	/* @ngInject */
	function CostdistributionService($log,$q,$http, NotificationService) {
		var base = {
				invoices: '/webapp/api/invoices',
				costDistribution: '/webapp/api/costdistributions'
			},
			service = {
                list: list,
                create: create,
				remove: remove,
				getCostDistributions: getCostDistributions,
                prepareItemForCostDistribution: prepareItemForCostDistribution
			},
			costDistributions = [];

		activate();

		return service;

		////////////////

		function getCostDistributions(){
			return costDistributions;
		}

		function activate(){
            list().then(
                function(response) {
                    costDistributions = response.data;
                }
			)
		}

		function list(){
			return $http({
				method: 'GET',
				url: base.costDistribution
			});
		}

        function remove(costDistributionId) {

            return $q(function(resolve) {
                return $http({
                    method: 'DELETE',
                    url: base.costDistribution + '/' + costDistributionId
                }).then(
                    function(result) {
                        NotificationService.create('Kostenverteilungs-Vorlage wurde gelöscht!',5000);
                        resolve(null);
                    },
                    function(result) {
                        $log.warn('Could not delete costdistribution!','result',result);
                        resolve(null);
                    }
                );
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

        function prepareItemForCostDistribution(costDistributionItem){
			costDistributionItem.costDistributionItemId = generateUUID();
			costDistributionItem.costDistributionId = null;

            return costDistributionItem;
        }

		function create(costDistribution, costDistributionItems) {

			costDistribution.costDistributionItemDTOS = [];
            costDistributionItems.forEach(function(item){
                costDistribution.costDistributionItemDTOS.push(item);
			});

        	var url = base.costDistribution + '/complete';

            return $q(function(resolve) {
                return $http({
                    method: 'POST',
                    url: url,
                    data: costDistribution,
                    headers: {
                        'Content-Type': 'application/json;charset=UTF-8'
                    }
                }).then(
                    function(result) {
                        NotificationService.create('Kostenverteilungs-Vorlage wurde erstellt!',5000);
                        costDistributions.push(result.data);
                        resolve(result.data);
                    },
                    function(result) {
                        $log.warn('Could not create costdistribution!','result',result);
                        resolve(null);
                    }
                );
            });
		}
	}

})();

