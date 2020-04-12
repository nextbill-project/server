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

(function() {
	'use strict';

	angular
		.module('msWebApp.msInvoiceTable', [
			'moment-picker'
		])
		.component('msInvoiceTable', {
			templateUrl: 'app/components/ms-invoice-table.html',
			bindings: {
				invoices: '<msInvoices',
				allowGoTo: '<?msAllowGoTo',
				useSelection: '<msUseSelection',
                selected : '=?msSelected',
                detailViewId : '=?msDetailViewId',
                tableViewId : '=?msTableViewId',
                useDebtValue : '<?msUseDebt',
                returnView: '=?msReturnView'
			},
			controller: InvoiceTableController,
			controllerAs: 'vm'
		});

    InvoiceTableController.$inject = ['$filter','$state','$element', '$log', '$scope', 'InvoiceService', 'NotificationService', '_'];

	/* @ngInject */
	function InvoiceTableController($filter, $state, $element, $log, $scope, InvoiceService, NotificationService, _) {
		var vm = this;

		$log.info('msInvoiceTable loaded');

        vm.selectedInvoiceId = null;

        vm.isInvoiceDetailsEnabled = function(invoiceId) {
            if (vm.tableViewId !== undefined && vm.tableViewId != null){
                return ((vm.tableViewId + '' + invoiceId) === vm.detailViewId);
            }else{
                return vm.selectedInvoiceId === invoiceId;
            }

        };

        vm.goToInvoice = function(invoice) {

        	vm.allowGoTo = (vm.allowGoTo != undefined ? vm.allowGoTo : true);

        	if (vm.allowGoTo){
                if (invoice.invoiceStatusEnum !== 'ANALYZING'){
                    $state.go('invoices.details', {
                        invoiceId: invoice.invoiceId
                    });
                }else{
                    NotificationService.create('Rechnung wird gerade analysiert! Bitte warten!',3000);
                }
			}
        };

        vm.toggleInvoiceDetails = function(invoice) {
            var oldValue, currentValue, invoiceId = invoice.invoiceId;

            if (vm.tableViewId !== undefined && vm.tableViewId != null){
                oldValue = vm.detailViewId;
                currentValue = (oldValue === (vm.tableViewId + '' + invoiceId)) ? null : invoiceId;
                if (currentValue == null){
                    vm.detailViewId = null;
                }else{
                    vm.detailViewId = vm.tableViewId + '' + currentValue;
                }

            }else{
                oldValue = vm.selectedInvoiceId;
                currentValue = (oldValue === invoiceId) ? null : invoiceId;
                vm.selectedInvoiceId = currentValue;
            }

        };

        vm.isSelected = function(item) {

            var matchedRights = _.chain(vm.selected)
                .intersection([item])
                .value();

            if (matchedRights.length > 0) {
                return true
            }else{
                return false;
            }
        };

        vm.toggleSelect = function(item) {
            var index = _.indexOf(vm.selected, item);

            if (index === -1) {
                vm.selected.push(item);
            } else {
                vm.selected.splice(index, 1);
            }
        };

        vm.invoiceServiceLoading = function(){
            return InvoiceService.isLoading();
        };
	}

})();