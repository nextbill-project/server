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
		.factory('InvoiceService', InvoiceService);

	InvoiceService.$inject = ['$log','$http','$q','CostdistributionitemService','NotificationService','SearchService','_'];

	/* @ngInject */
	function InvoiceService($log, $http, $q, CostdistributionitemService,NotificationService,SearchService, _) {
		var base = {
				invoices: '/webapp/api/invoices',
				invoicesWithFilterBean: '/webapp/api/search/withFilterBean',
                invoiceImages: '/webapp/api/invoices/image',
				standingOrders: '/webapp/api/standingOrders',
				standingOrderInvoiceTemplates: '/webapp/api/standingOrders/invoiceTemplates',
				deleteStandingOrders: '/webapp/api/standingOrders/deleteByInvoiceTemplate',
                invoiceAnalysis: '/webapp/api/service/upload'
			},
			invoiceListCheck = [],
			invoiceListReady = [],
			invoiceListReadyRaw = [],
			viewMode = 'CHECK',
			invoiceListStandingOrder = [],
			isLoadingValue = false,
			currentPageReadyList = 0,
			currentPageCheckList = 0,
			useSearchMode = false,
			isScrollingDisabled = true,
			service = {
				list: list,
				create: create,
				details: details,
				update: update,
				remove: remove,
                sendMistakeMessage: sendMistakeMessage,
				deleteInvoiceImage: deleteInvoiceImage,
				repetitionTypeEnumList: repetitionTypeEnumList,
				paymentTypeEnumList: paymentTypeEnumList,
				getInvoices: getInvoices,
				getIsScrollingDisabled: getIsScrollingDisabled,
				setIsScrollingDisabled: setIsScrollingDisabled,
				getInvoicesReadyRawList: getInvoicesReadyRawList,
                setInvoices: setInvoices,
				isLoading: isLoading,
                setIsLoading: setIsLoading,
				generateStandingOrder: generateStandingOrder,
				deleteStandingOrder: deleteStandingOrder,
				updateStandingOrder: updateStandingOrder,
				getStandingOrder: getStandingOrder,
				allInvoices: allInvoices,
				addValueToCurrentPageReadyList: addValueToCurrentPageReadyList,
				addValueToCurrentPageCheckList: addValueToCurrentPageCheckList,
				readyInvoicesList: readyInvoicesList,
				checkInvoicesList: checkInvoicesList,
                clearCurrentPageReadyList: clearCurrentPageReadyList,
                clearCurrentPageCheckList: clearCurrentPageCheckList,
				isSearchMode : isSearchMode,
                setSearchMode : setSearchMode,
                updateCorrectionStatus: updateCorrectionStatus,
				getViewMode: getViewMode,
				setViewMode: setViewMode,
                repeatAnalysis:  repeatAnalysis,
				pollingProcessRecursive: pollingProcessRecursive
			};

		allInvoices();
		
		return service;

		////////////////

		function getViewMode(){
			return viewMode;
		}

		function setViewMode(viewModeInput){
			viewMode = viewModeInput;
		}

		function isSearchMode(){
			return useSearchMode;
		}

        function setSearchMode(searchModeInput){
            useSearchMode = searchModeInput;
        }

        function clearCurrentPageReadyList(){
            currentPageReadyList = 0;
        }

        function clearCurrentPageCheckList(){
            currentPageReadyList = 0;
        }

		function addValueToCurrentPageReadyList(){
			currentPageReadyList = currentPageReadyList + 1;
		}

		function addValueToCurrentPageCheckList(){
			currentPageCheckList = currentPageCheckList + 1;
		}

        function isLoading() {
            return isLoadingValue;
        }
		
		function getIsScrollingDisabled(){
			return isScrollingDisabled;
		}
		
		function setIsScrollingDisabled(value){
			isScrollingDisabled = value;
		}

        function setIsLoading(isLoadingValueTmp) {
            isLoadingValue = isLoadingValueTmp;
        }

		function getInvoicesReadyRawList() {
			return invoiceListReadyRaw;
		}

		function pollingProcessRecursive(invoiceId, currentIteration){

			var sleeptimes = [3,1,1,1,1,2,3,4,4,5,5,5];

			var currentSleepTime = sleeptimes[currentIteration] * 1000;

			setTimeout(function() {
				details(invoiceId).then(
					function(result) {
						if (result.data.invoiceStatusEnum !== 'ANALYZING') {
							searchAndUpdateInvoiceInList(result.data);
						}else{
							if (currentIteration < sleeptimes.length - 1){
								pollingProcessRecursive(invoiceId, currentIteration + 1);
							}

						}
					})
				}
				, currentSleepTime);

		}
		
        function getInvoices(mode) {
			if (mode === 'CHECK'){
				return invoiceListCheck;
			}else if (mode === 'READY'){
				return invoiceListReady;
			}else if (mode === 'STANDING_ORDER'){
				return invoiceListStandingOrder;
			}
			return [];
        }

        function setInvoices(invoiceListTmp) {
			var invoiceListTmp2 = [];
			invoiceListTmp.forEach(function(item){
				invoiceListTmp2.push(item);
			});
			distributeInvoices(invoiceListTmp2);
        }

        function sendMistakeMessage(invoiceId, message){

			var messageDTO = {
				message: message
			};

            return $http({
                method: 'POST',
                url: base.invoices + '/' + invoiceId + '/mistake',
                data: messageDTO,
                headers: {
                    'Content-Type': 'application/json;charset=UTF-8'
                }
            }).then(
                function(result) {
                    NotificationService.create('Fehler erfolgreich gemeldet!',5000);
                },
                function(result) {
                    NotificationService.create('Fehlermeldung nicht erfolgreich!',5000);
                    $log.warn('Could not get create mistake message!','result',result);
                    isLoadingValue = false;
                }
            );
        }

		function generateStandingOrder(standingOrder, deleteInvoiceTemplate){

			var url = deleteInvoiceTemplate ? base.standingOrders + '?deleteInvoiceTemplate=true' : base.standingOrders;

			return $http({
				method: 'POST',
				url: url,
				data: standingOrder,
				headers: {
					'Content-Type': 'application/json;charset=UTF-8'
				}
			}).then(
				function(result) {
					NotificationService.create('Dauerauftrag erfolgreich erstellt!',5000);
					allInvoices();
				},
				function(result) {
					NotificationService.create('Fehler bei Erstellung des Dauerauftrags!',5000);
					$log.warn('Could not get create standing order!','result',result);
					isLoadingValue = false;
				}
			);
		}

		function deleteStandingOrder(invoiceTemplateId){
			return $http({
				method: 'DELETE',
				url: base.deleteStandingOrders + '/' + invoiceTemplateId
			}).then(
				function(result) {
					var foundInvoice = _.filter(invoiceListStandingOrder, function(invoice){ return (invoice.invoiceId === invoiceTemplateId);});
					if (foundInvoice != null && foundInvoice != undefined && foundInvoice.length > 0){
						var foundInvoiceIndex = _.indexOf(invoiceListStandingOrder, foundInvoice[0]);

						invoiceListStandingOrder.splice(foundInvoiceIndex, 1);
					}

					NotificationService.create('Dauerauftrag wurde gelöscht!',5000);
				},
				function(result) {
					NotificationService.create('Fehler beim Löschen von Dauerauftrag!',5000);
					$log.warn('Could not delete standing order!','result',result);
				}
			);
		}

        function deleteInvoiceImage(invoiceId){
            return $http({
                method: 'DELETE',
                url: base.invoiceImages + '/' + invoiceId
            }).then(
                function(result) {

                },
                function(result) {
                    NotificationService.create('Fehler beim Löschen von Bild!',5000);
                    $log.warn('Could not delete standing order!','result',result);
                }
            );
        }

        function repeatAnalysis(invoiceId){
            return $http({
                method: 'POST',
                url: base.invoiceAnalysis + '/' + invoiceId + '/repeatAnalysis'
            }).then(
                function(result) {
                    searchAndRemoveInvoiceInList(invoiceId);
                    distributeInvoice(result.data);

                    NotificationService.create('Bild wird nochmal analysiert!',5000);
                },
                function(result) {
                    NotificationService.create('Fehler bei Analyse vom Bild!',5000);
                    $log.warn('Could not analyze image!','result',result);
                }
            );
        }

		function distributeInvoices(invoiceList){
			invoiceListCheck = _.filter(invoiceList, function(invoice){ return ((invoice.invoiceWorkflowMode === 'EXTERNAL_USER_CHECK_MODE' || invoice.invoiceWorkflowMode === 'CREATED_USER_CHECK_MODE') && invoice.standingOrderInvoiceTemplateId !== invoice.invoiceId);});

			var invoiceListReadyTmp = _.filter(invoiceList, function(invoice){ return ((invoice.invoiceWorkflowMode === 'EXTERNAL_USER_READY_MODE' || invoice.invoiceWorkflowMode === 'CREATED_USER_READY_MODE') && invoice.standingOrderInvoiceTemplateId !== invoice.invoiceId);});
			var invoiceListReadyTmpSorted = _.sortBy(invoiceListReadyTmp, function(o) {
				var dateOfInvoice = new Date(o.dateOfInvoice);
				return dateOfInvoice; }
			).reverse();
			invoiceListReady = itemsGroupedByMonth(invoiceListReadyTmpSorted);

			invoiceListStandingOrder = _.filter(invoiceList, function(invoice){ return (invoice.standingOrderInvoiceTemplateId === invoice.invoiceId);});
		}

		function distributeInvoice(invoiceInput){

			if ((invoiceInput.invoiceWorkflowMode === 'EXTERNAL_USER_READY_MODE' || invoiceInput.invoiceWorkflowMode === 'CREATED_USER_READY_MODE') && invoiceInput.standingOrderInvoiceTemplateId !== invoiceInput.invoiceId){
				invoiceListReadyRaw.push(invoiceInput);
				invoiceListReady = distributeReadyInvoices(invoiceListReadyRaw);
				return invoiceListReadyRaw;
			}else if ((invoiceInput.invoiceWorkflowMode === 'EXTERNAL_USER_CHECK_MODE' || invoiceInput.invoiceWorkflowMode === 'CREATED_USER_CHECK_MODE') && invoiceInput.standingOrderInvoiceTemplateId !== invoiceInput.invoiceId){
				invoiceListCheck.push(invoiceInput);
				return invoiceListCheck;
			}else if (invoiceInput.standingOrderInvoiceTemplateId === invoiceInput.invoiceId){
				invoiceListStandingOrder.push(invoiceInput);
				return invoiceListStandingOrder;
			}
		}

		function distributeReadyInvoices(invoiceList){
			var invoicesListReadyTmp = [];

			invoiceList.forEach(function(item){
				invoicesListReadyTmp.push(item);
			});

			var invoiceListReadyTmpSorted = _.sortBy(invoicesListReadyTmp, function(o) {
				var dateOfInvoice = new Date(o.dateOfInvoice);
				return dateOfInvoice; }
			).reverse();
			return itemsGroupedByMonth(invoiceListReadyTmpSorted);
		}

		function itemsGroupedByMonth (items) {

			var groups = [],
				groupsTmp = [],
				itemGroupedByMonths = [],
				monthLabels = ['Januar','Februar','März','April','Mai','Juni','Juli','August','September','Oktober','November','Dezember'];

			for (var i = 0; i < items.length; i++) {
				var dateOfInvoiceTmp = items[i].dateOfInvoice;
				var intTimestamp = parseInt(dateOfInvoiceTmp);
				var dateOfInvoice = new Date(intTimestamp);

				var tmpKey = (dateOfInvoice.getMonth() + ' ' +dateOfInvoice.getFullYear()).toString();
				var tmpKeyObject = {month:dateOfInvoice.getMonth(), year: dateOfInvoice.getFullYear(), key: tmpKey};

				var groupsTmpFilter = _.filter(groupsTmp, function(groupTmp){ return groupTmp.key === tmpKey; });
				if (groupsTmpFilter.length == 0){
					groupsTmp.push(tmpKeyObject);
				}

				if (groups[tmpKey] == undefined){
					groups[tmpKey] = [];
				}
				groups[tmpKey].push(items[i]);
			}
			
			for (var j = 0; j < groupsTmp.length; j++) {
				var groupLabel = monthLabels[groupsTmp[j].month] + ' ' + groupsTmp[j].year;
				var groupSum = 0;
				groups[groupsTmp[j].key].forEach(function(item){
					groupSum = groupSum + item.moneyValue;
				});
				var pow = Math.pow(10,2);
				groupSum = +( Math.round(groupSum * pow) / pow );
				itemGroupedByMonths.push({
					groupLabel: groupLabel,
					items: groups[groupsTmp[j].key],
					groupSum: groupSum
				});
			}
			return itemGroupedByMonths;
		}

		function allInvoices(){

			currentPageReadyList = 0;
			currentPageCheckList = 0;

			invoiceListCheck = [];
			invoiceListReadyRaw = [];
			invoiceListStandingOrder = [];

			isLoadingValue = true;
			
			return $q(function(resolve) {
				var invoiceListReadyRawSize = invoiceListReadyRaw.length;
				var invoiceListReadyRawTmp = [];
                var currentPageReadyListTmp = angular.copy(currentPageReadyList);
                var invoiceListReadyRawOriginalCopy = [];
                invoiceListReadyRaw.forEach(function(item){
                    invoiceListReadyRawOriginalCopy.push(item);
                });
				$q.all([list(true, null, invoiceListStandingOrder), list(false, 'READY', invoiceListReady, currentPageReadyListTmp, undefined, invoiceListReadyRawSize, invoiceListReadyRawTmp, invoiceListReadyRawOriginalCopy), list(false, 'CHECK', invoiceListCheck, currentPageCheckList)])
					.then(function (result) {
						isLoadingValue = false;

						if ((invoiceListCheck == undefined || invoiceListCheck.length == 0) && (invoiceListReadyRaw != undefined && invoiceListReadyRaw.length >= 0)){
							viewMode = 'READY';
						}else{
							viewMode = 'CHECK';
						}

						resolve(null);
					}, function (error) {
						isLoadingValue = false;
						resolve(null);
					});
			});
		}

		function readyInvoicesList(){
            var invoiceListReadyRawTmp = [];
            var currentPageReadyListTmp = angular.copy(currentPageReadyList);
            var invoiceListReadyRawSize = invoiceListReadyRaw.length;
            var invoiceListReadyRawOriginalCopy = [];
            invoiceListReadyRaw.forEach(function(item){
                invoiceListReadyRawOriginalCopy.push(item);
            });
			return list(false, 'READY', invoiceListReady, currentPageReadyListTmp, undefined, invoiceListReadyRawSize, invoiceListReadyRawTmp, invoiceListReadyRawOriginalCopy);
		}

		function checkInvoicesList(){
			return list(false, 'CHECK', invoiceListCheck, currentPageCheckList);
		}

		function list(onlyStandingOrder, invoiceStatusEnum, invoiceList, currentPage, counter, invoiceListReadyRawCounter, invoiceListReadyRawTmp, invoiceListReadyRawOriginalCopy) {

			var searchFilterBean = SearchService.getSearchConfiguration();

			return $http({
				method: 'POST',
				url: base.invoicesWithFilterBean,
				params: {
					onlyStandingOrder: onlyStandingOrder,
					invoiceStatusEnum: invoiceStatusEnum,
					pageNumber: currentPage
				},
				data: searchFilterBean,
				headers: {
					'Content-Type': 'application/json;charset=UTF-8'
				}
			}).then(
				function(result) {
					var invoiceListTmp = result.data;

					if (invoiceStatusEnum != null && invoiceStatusEnum === 'READY'){
						
					invoiceListTmp = result.data.invoiceDTOs;
					currentPage = result.data.currentPage;

					invoiceListTmp.forEach(function(item){
						invoiceListReadyRawTmp.push(item);
					});

						invoiceListReadyRawTmp.forEach(function(item){
							invoiceListReadyRawOriginalCopy.push(item);
						});

						invoiceListReadyRaw = [];
						invoiceListReadyRawOriginalCopy.forEach(function(item){
							invoiceListReadyRaw.push(item);
						});

						currentPageReadyList = currentPage;

						invoiceListReady = distributeReadyInvoices(invoiceListReadyRaw);

						isScrollingDisabled = false;
					}else{
						invoiceListTmp.forEach(function(item){
							if (item.invoiceStatusEnum === 'ANALYZING'){
								pollingProcessRecursive(item.invoiceId, 0);
							}
							invoiceList.push(item);
						});
					}
				},
				function(result) {
					$log.warn('Could not get invoices!','result',result);
				}
			);
		}

		function repetitionTypeEnumList() {
			return $http({
				method: 'GET',
				url: base.invoices + "/repetitionTypeEnums"
			});
		}

		function paymentTypeEnumList() {
			return $http({
				method: 'GET',
				url: base.invoices + "/paymentTypeEnums"
			});
		}

		function create(invoice, costDistributionItems) {

			return $q(function(resolve) {
				var httpConfiguration = {
					method: 'PUT',
					url: base.invoices + '/' + invoice.invoiceId,
					data: invoice,
					headers: {
						'Content-Type': 'application/json;charset=UTF-8'
					}
				};

				$http(httpConfiguration).then(
					function(result) {
						CostdistributionitemService.update(result.data.invoiceId, costDistributionItems).then(function(resultCostItems){
							details(result.data.invoiceId).then(function(detailsInvoice){
								distributeInvoice(detailsInvoice.data);
								resolve(null);
							})
						});
						NotificationService.create('Rechnung wurde erstellt!',5000);

					}
				);
			});
		}

		function details(invoiceId) {
			return $http({
				method: 'GET',
				url: base.invoices + '/' + invoiceId
			});
		}

        function updateCorrectionStatus(invoices, correctionStatus) {

            return $q(function(resolve) {
                var httpConfiguration = {
                    method: 'POST',
                    url: base.invoices + '/setCorrectionStatus',
                    data: invoices,
                    headers: {
                        'Content-Type': 'application/json;charset=UTF-8'
                    },
                    params: {
                        setWorkflow: correctionStatus
                    }
                };

                $http(httpConfiguration).then(
                    function(result) {

                    	result.data.forEach(function(item){
                            searchAndRemoveInvoiceInList(item.invoiceId);
                            distributeInvoice(item);
						});

						NotificationService.create('Status wurde aktualisiert!',5000);
						resolve(null);

                    }, function(result){
                        NotificationService.create('Fehler während der Aktualisierung!',5000);
                        allInvoices();
                    }
                );
            });

        }

		function update(invoice, correctionStatus, costDistributionItems) {

			invoice.costDistributionItemDTOs = costDistributionItems;
			
            return $q(function(resolve) {
                var httpConfiguration = {
                    method: 'PUT',
                    url: base.invoices + '/' + invoice.invoiceId,
                    data: invoice,
                    headers: {
                        'Content-Type': 'application/json;charset=UTF-8'
                    }
                };

                if (correctionStatus != undefined && correctionStatus != null){
                    httpConfiguration.params = {
                        setWorkflow: correctionStatus
                    };
                }

                $http(httpConfiguration).then(
                    function(result) {

						if (correctionStatus != undefined && correctionStatus != null){

							searchAndRemoveInvoiceInList(invoice.invoiceId);
							distributeInvoice(result.data);
						}else{
							searchAndUpdateInvoiceInList(result.data);

							NotificationService.create('Rechnung wurde aktualisiert!',5000);
							resolve(null);
						}

                    }, function(result){
						NotificationService.create('Fehler während der Aktualisierung!',5000);
						allInvoices();
					}
                );
            });
		}

		function updateStandingOrder(invoiceTemplateId, standingOrder) {

			return $q(function(resolve) {
				var httpConfiguration = {
					method: 'PUT',
					url: base.standingOrderInvoiceTemplates + '/' + invoiceTemplateId,
					data: standingOrder,
					headers: {
						'Content-Type': 'application/json;charset=UTF-8'
					}
				};

				$http(httpConfiguration).then(
					function(result) {
						NotificationService.create('Dauerauftrag wurde aktualisiert!',5000);
						resolve(null);
					}, function(result){
						NotificationService.create('Fehler während der Aktualisierung!',5000);
						allInvoices();
					}
				);
			});
		}

		function getStandingOrder(invoiceTemplateId) {

			return $q(function(resolve, error) {
				var httpConfiguration = {
					method: 'GET',
					url: base.standingOrderInvoiceTemplates + '/' + invoiceTemplateId,
				};

				$http(httpConfiguration).then(
					function(result) {
						resolve(result.data);
					}, function(result){
						error(null);
					}
				);
			});
		}

		function remove(invoiceId) {

            return $q(function(resolve) {
                return $http({
                    method: 'DELETE',
                    url: base.invoices + '/' + invoiceId
                }).then(
                    function(result) {
						searchAndRemoveInvoiceInList(invoiceId);
                        NotificationService.create('Rechnung wurde gelöscht!',5000);

                        resolve(null);
                    },
                    function(result) {
                        $log.warn('Could not delete invoice!','result',result);
                        resolve(null);
                    }
                );
            });
		}

		function searchAndRemoveInvoiceInList(invoiceId){

			var foundInvoiceIndex, foundInvoice;

			foundInvoice = _.filter(invoiceListCheck, function(invoice){ return (invoice.invoiceId === invoiceId);});
			if (foundInvoice != null && foundInvoice != undefined && foundInvoice.length > 0){
				foundInvoiceIndex = _.indexOf(invoiceListCheck, foundInvoice[0]);

				invoiceListCheck.splice(foundInvoiceIndex, 1);
				invoiceListCheck[foundInvoiceIndex];
			}

			foundInvoice = _.filter(invoiceListReadyRaw, function(invoice){ return (invoice.invoiceId === invoiceId);});
			if (foundInvoice != null && foundInvoice != undefined && foundInvoice.length > 0){
				foundInvoiceIndex = _.indexOf(invoiceListReadyRaw, foundInvoice[0]);

				invoiceListReadyRaw.splice(foundInvoiceIndex, 1);
				invoiceListReady[foundInvoiceIndex];
				invoiceListReady = distributeReadyInvoices(invoiceListReadyRaw);
			}

			foundInvoice = _.filter(invoiceListStandingOrder, function(invoice){ return (invoice.invoiceId === invoiceId);});
			if (foundInvoice != null && foundInvoice != undefined && foundInvoice.length > 0){
				foundInvoiceIndex = _.indexOf(invoiceListStandingOrder, foundInvoice[0]);

				invoiceListStandingOrder.splice(foundInvoiceIndex, 1);
				invoiceListStandingOrder[foundInvoiceIndex];
			}
		}

		function searchAndUpdateInvoiceInList(invoice){

			var foundInvoiceIndex, foundInvoiceIndex, foundInvoice;

			foundInvoice = _.filter(invoiceListCheck, function(invoiceTmp){ return (invoiceTmp.invoiceId === invoice.invoiceId);});
			if (foundInvoice != null && foundInvoice != undefined && foundInvoice.length > 0) {
				foundInvoiceIndex = _.indexOf(invoiceListCheck, foundInvoice[0]);

				invoiceListCheck[foundInvoiceIndex] = invoice;
			}

			foundInvoice = _.filter(invoiceListReadyRaw, function(invoiceTmp){ return (invoiceTmp.invoiceId === invoice.invoiceId);});
			if (foundInvoice != null && foundInvoice != undefined && foundInvoice.length > 0) {
				foundInvoiceIndex = _.indexOf(invoiceListReadyRaw, foundInvoice[0]);

				invoiceListReadyRaw[foundInvoiceIndex] = invoice;
				invoiceListReady = distributeReadyInvoices(invoiceListReadyRaw);
			}

			foundInvoice = _.filter(invoiceListStandingOrder, function(invoiceTmp){ return (invoiceTmp.invoiceId === invoice.invoiceId);});
			if (foundInvoice != null && foundInvoice != undefined && foundInvoice.length > 0) {
				foundInvoiceIndex = _.indexOf(invoiceListStandingOrder, foundInvoice[0]);

				invoiceListStandingOrder[foundInvoiceIndex] = invoice;
			}
		}
	}

})();

