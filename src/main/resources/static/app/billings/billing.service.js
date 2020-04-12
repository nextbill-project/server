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
		.factory('BillingService', BillingService);

    BillingService.$inject = ['$http','$q','NotificationService', 'MessageService','_'];

	/* @ngInject */
	function BillingService($http, $q,NotificationService,MessageService, _) {
		var base = {
				createBilling: '/webapp/api/billings/payment',
                billingUdate: '/webapp/api/billings/{billingId}/payment',
                billingDelete: '/webapp/api/billings/{billingId}',
                billing: '/webapp/api/billings/{billingId}',
                billingProcesses: '/webapp/api/billings/billingListItems/grouped',
                billingReport: '/webapp/api/billings/{billingId}/downloadReport/Kostenuebersicht.pdf',
                archive : '/webapp/api/billings/{billingId}/archive',
                checkReminderMessage : '/webapp/api/billings/checkReminderMessage/{appUserId}',
                createCompensation : '/webapp/api/billings/{billingId}/createCompensation',
                executeEquality: '/webapp/api/billings/{billingId}/executeEquality'
			},
            billingProcessList = [],
			billingList = [],
            isLoadingValue = false,
            useArchived = false,
			service = {
                getBillings: getBillings,
                getBilling: getBilling,
                createBilling: createBilling,
                billingUpdate: billingUpdate,
                billingDelete: billingDelete,
                isLoading: isLoading,
                setIsLoading: setIsLoading,
                getUseArchived: getUseArchived,
                setUseArchived: setUseArchived,
                getBillingProcesses: getBillingProcesses,
                listBillingProcesses: listBillingProcesses,
                billingReportUrl: billingReportUrl,
                archiveBilling: archiveBilling,
                checkReminderMessage: checkReminderMessage,
                createCompensation: createCompensation,
                executeEquality: executeEquality
			};

		return service;

		////////////////

        function getBillingProcesses(){
            return billingProcessList;
        }

        function listBillingProcesses(){

            var urlExtended = useArchived ? '?archived=true' : '';

            isLoadingValue = true;

            return $q(function(resolve) {
                $http({
                    method: 'GET',
                    url: base.billingProcesses + urlExtended
                }).then(
                    function(result) {
                        billingProcessList = result.data;
                        isLoadingValue = false;
                        resolve(billingProcessList);
                    },
                    function(result) {
                        isLoadingValue = false;
                        resolve(null);
                    }
                );
            });
        }

        function isLoading() {
            return isLoadingValue;
        }

        function setIsLoading(isLoadingValueTmp) {
            isLoadingValue = isLoadingValueTmp;
        }

        function getUseArchived() {
            return useArchived;
        }

        function setUseArchived(useArchivedTmp) {
            useArchived = useArchivedTmp;
        }

        function getBillings() {
            return billingList;
        }

        function getBilling(billingId) {
            isLoadingValue = true;

            var url = base.billing.replace('{billingId}',billingId);

            return $q(function(resolve) {
                $http({
                    method: 'GET',
                    url: url
                }).then(
                    function(result) {
                        resolve(result.data);
                        isLoadingValue = false;
                    },
                    function(result) {
                        isLoadingValue = false;
                        resolve(null);
                    }
                );
            });
        }

        function createBilling(reportConfig) {

            return $q(function(resolve) {
                var httpConfiguration = {
                    method: 'POST',
                    url: base.createBilling,
                    data: reportConfig,
                    headers: {
                        'Content-Type': 'application/json;charset=UTF-8'
                    }
                };

                $http(httpConfiguration).then(
                    function(result) {
                        NotificationService.create('Abrechnung wurde erstellt!',5000);

                        MessageService.list();
                        listBillingProcesses();
                        resolve(null);
                    }
                );
            });
        }

        function executeEquality(billingId){

            var url = base.executeEquality
                .replace('{billingId}',billingId);

            return $q(function(resolve) {
                var httpConfiguration = {
                    method: 'POST',
                    url: url,
                    headers: {
                        'Content-Type': 'application/json;charset=UTF-8'
                    }
                };

                $http(httpConfiguration).then(
                    function(result) {
                        NotificationService.create('Summen wurden gleichgesetzt!',5000);

                        MessageService.list();
                        listBillingProcesses();
                        resolve(result.data);
                    }
                );
            });
        }

        function checkReminderMessage(appUserId) {

            var url = base.checkReminderMessage
                .replace('{appUserId}',appUserId);

            return $q(function(resolve) {
                var httpConfiguration = {
                    method: 'POST',
                    url: url
                };

                $http(httpConfiguration).then(
                    function(result) {
                        NotificationService.create('Nachricht wurde verschickt!',5000);

                        MessageService.list();
                        resolve(null);
                    }
                );
            });
        }

        function createCompensation(billingId) {

            var url = base.createCompensation
                .replace('{billingId}',billingId);

            return $q(function(resolve, reject) {
                var httpConfiguration = {
                    method: 'POST',
                    url: url
                };

                $http(httpConfiguration).then(
                    function(result) {
                        MessageService.list();
                        NotificationService.create('Ausbalancierende Rechnung wurde gespeichert und kann noch bearbeitet werden!',8000);
                        resolve(result.data);
                    }, function(resultError){

                        NotificationService.create('Ausbalancierende Rechnung konnte nicht erstellt werden!',5000);
                        reject(null);
                    }
                );
            });
        }

        function archiveBilling(billingId) {

            var url = base.archive
                .replace('{billingId}',billingId);

            return $q(function(resolve) {
                var httpConfiguration = {
                    method: 'POST',
                    url: url
                };

                $http(httpConfiguration).then(
                    function(result) {
                        NotificationService.create('Abrechnung wurde archiviert!',5000);

                        MessageService.list();
                        listBillingProcesses();
                        resolve(null);
                    }
                );
            });
        }

        function billingUpdate(billingId, messageType){

            var  billingPaymentBean = {};
            billingPaymentBean.billingId = billingId;
            if (messageType === 'TO_PAY'){
                billingPaymentBean.billingStatusEnum = 'PAID';
            }else if (messageType === 'PAID' || messageType === 'WAIT_FOR_PAYMENT'){
                billingPaymentBean.billingStatusEnum = 'PAYMENT_CONFIRMED';
            }else if (messageType === 'PAYMENT_CONFIRMED'){
                billingPaymentBean.billingStatusEnum = 'FINISHED';
            }

            var url = base.billingUdate
                .replace('{billingId}',billingId);

            return $q(function(resolve) {
                $http({
                    method: 'POST',
                    url: url,
                    data: billingPaymentBean,
                    headers: {
                        'Content-Type': 'application/json;charset=UTF-8'
                    }
                }).then(
                    function(result) {
                        listBillingProcesses().then(function(result3){
                            NotificationService.create('Workflow-Status wurde gesetzt!',5000);
                        });

                        MessageService.list().then(function(result2){
                            resolve(result2);
                        });
                    }
                );
            });
        }

        function billingReportUrl(billingId){
            var url = base.billingReport.replace('{billingId}',billingId);
            return url;
        }

        function billingDelete(object1Id){

            var url = base.billingDelete
                .replace('{billingId}',object1Id);

            return $q(function(resolve) {
                $http({
                    method: 'DELETE',
                    url: url
                }).then(
                    function(result) {
                        listBillingProcesses().then(function(result3){
                            NotificationService.create('Abrechnung wurde gelöscht!',5000);
                        });

                        MessageService.list().then(function(result2){
                            resolve(result2);
                        });
                    }
                );
            });
        }
	}

})();

