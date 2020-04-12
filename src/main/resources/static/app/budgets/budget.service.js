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
		.factory('BudgetService', BudgetService);

    BudgetService.$inject = ['$http','$q','NotificationService', 'MessageService','_'];

	/* @ngInject */
	function BudgetService($http, $q,NotificationService,MessageService, _) {
		var base = {
				budgets: '/webapp/api/budgets',
				createBudget: '/webapp/api/budgets',
                budgetUdate: '/webapp/api/budgets/{budgetId}',
                budgetDelete: '/webapp/api/budgets/{budgetId}'
			},
			budgetList = [],
            isLoadingValue = false,
			service = {
                listBudgets: listBudgets,
                getBudgets: getBudgets,
                details: details,
                createBudget: createBudget,
                updateBudget: updateBudget,
                deleteBudget: deleteBudget,
                isLoading: isLoading,
                setIsLoading: setIsLoading,
                budgetRepetitionTypeEnumList: budgetRepetitionTypeEnumList
			};

		return service;

		////////////////

        function isLoading() {
            return isLoadingValue;
        }

        function setIsLoading(isLoadingValueTmp) {
            isLoadingValue = isLoadingValueTmp;
        }

        function budgetRepetitionTypeEnumList() {
            return $http({
                method: 'GET',
                url: base.budgets + "/budgetRepetitionTypeEnums"
            });
        }

        function details(budgetId) {
            return $http({
                method: 'GET',
                url: base.budgets + '/' + budgetId
            });
        }

        function getBudgets() {
            return budgetList;
        }

		function listBudgets() {
            isLoadingValue = true;

            return $q(function(resolve) {
                $http({
                    method: 'GET',
                    url: base.budgets
                }).then(
                    function(result) {
                        budgetList = result.data;
                        isLoadingValue = false;
                        resolve(budgetList);
                    },
                    function(result) {
                        isLoadingValue = false;
                        resolve(null);
                    }
                );
            });
		}

        function createBudget(budget) {

            return $q(function(resolve) {
                var httpConfiguration = {
                    method: 'POST',
                    url: base.createBudget,
                    data: budget,
                    headers: {
                        'Content-Type': 'application/json;charset=UTF-8'
                    }
                };

                $http(httpConfiguration).then(
                    function(result) {
                        NotificationService.create('Budget wurde erstellt!',5000);

                        MessageService.list();
                        listBudgets();
                        resolve(null);
                    }
                );
            });
        }

        function updateBudget(budgetId, budget){

            var url = base.budgetUdate
                .replace('{budgetId}',budgetId);

            return $q(function(resolve) {
                $http({
                    method: 'PUT',
                    url: url,
                    data: budget,
                    headers: {
                        'Content-Type': 'application/json;charset=UTF-8'
                    }
                }).then(
                    function(result) {
                        MessageService.list();
                        listBudgets().then(function(result3){
                            NotificationService.create('Budget aktualisiert!',5000);
                            resolve(result.data);
                        });
                    }
                );
            });
        }

        function deleteBudget(object1Id){

            var url = base.budgetDelete
                .replace('{budgetId}',object1Id);

            return $q(function(resolve) {
                $http({
                    method: 'DELETE',
                    url: url
                }).then(
                    function(result) {
                        MessageService.list();
                        listBudgets().then(function(result3){
                            NotificationService.create('Budget wurde gelöscht!',5000);
                            resolve(null);
                        });
                    }
                );
            });
        }
	}

})();

