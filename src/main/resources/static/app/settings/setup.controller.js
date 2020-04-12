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
		.controller('SetupController', SetupController);

    SetupController.$inject = ['$http', '$state', 'SettingsService', 'NotificationService','UserService'];

	/* @ngInject */
	function SetupController($http, $state, SettingsService, NotificationService,UserService) {
		var vm = this;

		vm.setupData = {};
		vm.loaded = false;
		vm.version = null;

		activate();

		vm.saveData = function(){
			if (!vm.setupData.isCustomized){
				SettingsService.updateInit(vm.setupData).then(function(response){
					var newUrl = window.location.protocol + "//" + window.location.host;
					window.location.replace(newUrl);
				},function(error){
					NotificationService.create('Fehler bei der Initialisierung! Evtl. fehlende Ordner-Zugriffsberechtigungen.',6000);
				});
			} else {
				SettingsService.update(vm.setupData).then(function(response){
					NotificationService.create('Konfiguration erfolgreich gespeichert.',6000);
					UserService.resetUser();
				},function(error){
					NotificationService.create('Fehler beim Speichern! Bitte Ordner-Zugriffsberechtigungen überprüfen.',6000);
					UserService.resetUser();
				});
			}

		};

		vm.sendTestMail = function() {
			NotificationService.create('Einen Moment bitte. Test-E-Mail wird versandt!',6000);
			SettingsService.sendTestMail(vm.setupData).then(function(response){
				$('#updateModalDialog').modal('show');
			},function(error){
				NotificationService.create('Versand der Test-E-Mail fehlgeschlagen!',6000);
			});
		};

		vm.versionCheck = function() {
			NotificationService.create('Einen Moment bitte. Es wird auf Updates geprüft!',1000);
			SettingsService.versionCheck().then(function(response){
				vm.version = response.data;
				$('#updateModalDialog').modal('show');
			},function(error){
				NotificationService.create('Abruf der aktuellen Version fehlgeschlagen!.',6000);
			});
		};

        function activate(){
        	SettingsService.isCustomized().then(function(response){
        		if (response.data.value){
					SettingsService.get().then(function(response){
						vm.setupData = response.data;
						vm.loaded = true;
					});
				}else{
					SettingsService.getInit().then(function(response){
						vm.setupData = response.data;
						vm.loaded = true;
					});
				}
			});

		}

	}

})();

