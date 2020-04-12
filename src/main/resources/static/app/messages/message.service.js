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
		.factory('MessageService', MessageService);

    MessageService.$inject = ['$http','$q','NotificationService', '_', 'SettingsService'];

	/* @ngInject */
	function MessageService($http, $q, NotificationService, _, SettingsService) {
		var base = {
				messages: '/webapp/api/messages',
                invoices: '/webapp/api/invoices',
                deleteMessage: '/webapp/api/basicdata'
            
			},
			messagesList = [],
			service = {
				list: list,
                getMessages: getMessages,
                problemSolved: problemSolved,
                deleteMessage: deleteMessage
			};

        listWithCheck();

		return service;

		////////////////

        function getMessages() {
            return messagesList;
        }

        function listWithCheck() {
            SettingsService.isCustomized().then(function(response){
                if (response.data.value){
                    list();
                }
            });
        }

		function list() {
            return $q(function(resolve) {
                $http({
                    method: 'GET',
                    url: base.messages
                }).then(
                    function(result) {
                        var tmpMessageList = result.data;

                        messagesList = [];

                        tmpMessageList.forEach(function(item){
                           var tmpItem = item;
                           var tmpItemMessageParsed = JSON.parse(tmpItem.value);

                            tmpItem.subject = tmpItemMessageParsed.subject;
                            tmpItem.message = tmpItemMessageParsed.message;
                            tmpItem.messageType = tmpItemMessageParsed.messageType;

                            messagesList.push(tmpItem);
                        });
                        resolve(messagesList);
                    }
                );
            });
		}
        
        function deleteMessage(basicDataId) {

            return $q(function(resolve) {
                var httpConfiguration = {
                    method: 'DELETE',
                    url: base.deleteMessage + '/' + basicDataId
                };

                $http(httpConfiguration).then(
                    function(result) {
                        NotificationService.create('Nachricht wurde gelöscht!',5000);
                        list();
                        resolve(null);
                    }
                );
            });
        }

        function problemSolved(basicDataId) {

            return $q(function(resolve) {
                var httpConfiguration = {
                    method: 'DELETE',
                    url: base.invoices + '/' + basicDataId + '/problemSolved'
                };

                $http(httpConfiguration).then(
                    function(result) {

                        var foundMessage = _.filter(messagesList, function(message){ return (message.basicDataId === basicDataId);});
                        if (foundMessage != null && foundMessage != undefined && foundMessage.length > 0){
                            var foundMessageIndex = _.indexOf(messagesList, foundMessage[0]);

                            messagesList.splice(foundMessageIndex, 1);
                            resolve(messagesList[foundMessageIndex]);
                        }

                        NotificationService.create('Nachricht wurde gelöscht!',5000);
                        resolve(result);
                    }
                );
            });
        }
	}

})();

