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
		.factory('BusinesspartnerService', BusinesspartnerService);

	BusinesspartnerService.$inject = ['$http','$q'];

	/* @ngInject */
	function BusinesspartnerService($http, $q) {
		var base = {
				businesspartners: '/webapp/api/businesspartners',
				businesspartnerSearch: '/webapp/api/businesspartners/search',
                autoCategory: '/autoCategory',
			},
			service = {
				list: list,
				getOne: getOne,
                getAutoCategory: getAutoCategory,
				getBusinessPartners: getBusinessPartners,
				search: search,
				getBusinessPartnerNewName: getBusinessPartnerNewName,
				create: create,
				remove: remove
			},
			businessPartnerNewName = null,
			businessPartners = [];

		return service;

		////////////////

		function getBusinessPartners(){
			return businessPartners;
		}

		function getBusinessPartnerNewName(){
			return businessPartnerNewName;
		}

		function create(){

			var businessPartnerDTO = {
				businessPartnerName: businessPartnerNewName
			};

			return $q(function(resolve) {
				$http({
					method: 'POST',
					url: base.businesspartners,
					data: businessPartnerDTO,
					headers: {
						'Content-Type': 'application/json;charset=UTF-8'
					}
				}).then(
					function(result) {
						businessPartners.push(result.data);
						businessPartnerNewName = null;
						resolve(result.data);
					}
				);
			});

		}

		function search(searchName) {

			var businessPartnerDTO = {
				businessPartnerName: searchName
			};

			$http({
				method: 'POST',
				url: base.businesspartnerSearch,
				data: businessPartnerDTO,
				headers: {
					'Content-Type': 'application/json;charset=UTF-8'
				}
			}).then(function(response){
				businessPartners = response.data.businessPartnerDTOList;
				businessPartnerNewName = response.data.newUserName;
			});
		}

		function list() {
			return $http({
				method: 'GET',
				url: base.businesspartners
			});
		}

		function getOne(uuid) {
			return $http({
				method: 'GET',
				url: base.businesspartners + '/' + uuid
			});
		}

        function getAutoCategory(uuid) {
            return $http({
                method: 'GET',
                url: base.businesspartners + '/' + uuid + base.autoCategory
            });
        }

		function remove(businessPartnerId){
			return $q(function(resolve) {
				return $http({
					method: 'DELETE',
					url: base.businesspartners + '/' + businessPartnerId
				}).then(
					function(result) {

						var foundBusinessPartner = _.filter(businessPartners, function(businessPartner){ return (businessPartner.businessPartnerId === businessPartnerId);});
						if (foundBusinessPartner != null && foundBusinessPartner != undefined && foundBusinessPartner.length > 0){
							var foundBusinessPartnerIndex = _.indexOf(businessPartners, foundBusinessPartner[0]);

							businessPartners.splice(foundBusinessPartnerIndex, 1);
						}
						resolve(null);
					},
					function(result) {
						$log.warn('Could not delete business partner!','result',result);
						resolve(null);
					}
				);
			});
		}
	}

})();

