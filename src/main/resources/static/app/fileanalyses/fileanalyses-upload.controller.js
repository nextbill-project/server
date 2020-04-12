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
		.controller('FileAnalysesController', FileAnalysesController);

	FileAnalysesController.$inject = ['FileUploader','$state','$timeout','NotificationService','InvoiceService','SearchService','csrfToken'];

	/* @ngInject */
	function FileAnalysesController(FileUploader,$state, $timeout,NotificationService, InvoiceService, SearchService, csrfToken) {
		var vm = this;
		vm.title = 'FileAnalysesController';

		var uploader = vm.uploader = new FileUploader({
			url: '/webapp/api/service/upload/' + generateUUID(),
			headers : {},
			alias: 'fileUpload'
		});
		
		var csrf_token = getCookie(csrfToken);
		uploader.headers[csrfToken] = csrf_token;

		// FILTERS

		// a sync filter
		uploader.filters.push({
			name: 'syncFilter',
			fn: function(item /*{File|FileLikeObject}*/, options) {
				console.log('syncFilter');
				return this.queue.length < 40;
			}
		});

		// an async filter
		// uploader.filters.push({
		// 	name: 'asyncFilter',
		// 	fn: function(item /*{File|FileLikeObject}*/, options, deferred) {
		// 		console.log('asyncFilter');
		// 		setTimeout(deferred.resolve, 1e3);
		// 	}
		// });

		// CALLBACKS

		uploader.onWhenAddingFileFailed = function(item /*{File|FileLikeObject}*/, filter, options) {
			console.info('onWhenAddingFileFailed', item, filter, options);
		};
		uploader.onAfterAddingFile = function(fileItem) {
			console.info('onAfterAddingFile', fileItem);
		};
		uploader.onAfterAddingAll = function(addedFileItems) {
			console.info('onAfterAddingAll', addedFileItems);
		};
		uploader.onBeforeUploadItem = function(item) {
            item.url = '/webapp/api/service/upload/' + generateUUID();
			console.info('onBeforeUploadItem', item);
		};
		uploader.onProgressItem = function(fileItem, progress) {
			console.info('onProgressItem', fileItem, progress);
		};
		uploader.onProgressAll = function(progress) {
			console.info('onProgressAll', progress);
		};
		uploader.onSuccessItem = function(fileItem, response, status, headers) {
			NotificationService.create('Datei erfolgreich hochgeladen!',5000);
			console.info('onSuccessItem', fileItem, response, status, headers);
		};
		uploader.onErrorItem = function(fileItem, response, status, headers) {
			NotificationService.create('Fehler beim Upload! Ist die Datei beschädigt?',5000);
			console.info('onErrorItem', fileItem, response, status, headers);
		};
		uploader.onCancelItem = function(fileItem, response, status, headers) {
			console.info('onCancelItem', fileItem, response, status, headers);
		};
		uploader.onCompleteItem = function(fileItem, response, status, headers) {
			console.info('onCompleteItem', fileItem, response, status, headers);
		};
		uploader.onCompleteAll = function() {
			NotificationService.create('Datei(en) erfolgreich hochgeladen! Sie wird/werden nun im Hintergrund analysiert...',5000);
			SearchService.resetSearchConfiguration();
			InvoiceService.setSearchMode(false);
			InvoiceService.allInvoices();
			$timeout(function(){
				$state.go('invoices.list');
			}, 2000);

			console.info('onCompleteAll');
		};

		console.info('uploader', uploader);

		function generateUUID(){
			function s4() {
				return Math.floor((1 + Math.random()) * 0x10000)
					.toString(16)
					.substring(1);
			}
			return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
				s4() + '-' + s4() + s4() + s4();
		}

		function getCookie(cname) {
			var name = cname + "=";
			var decodedCookie = decodeURIComponent(document.cookie);
			var ca = decodedCookie.split(';');
			for(var i = 0; i <ca.length; i++) {
				var c = ca[i];
				while (c.charAt(0) == ' ') {
					c = c.substring(1);
				}
				if (c.indexOf(name) == 0) {
					return c.substring(name.length, c.length);
				}
			}
			return "";
		}
	}

})();

