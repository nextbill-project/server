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
		.module('msWebApp.msInvoicecategorieSelect',[])
		.directive('msInvoicecategorieSelect', msInvoicecategorieSelect);

	msInvoicecategorieSelect.$inject = ['$document','$log','InvoicecategoriesService','_'];

	/* @ngInject */
	function msInvoicecategorieSelect($document,$log,InvoicecategoriesService,_) {
		var directive = {
			restrict: 'E',
			templateUrl: 'app/components/ms-invoicecategorie-select.html',
			scope: {
				invoiceCategories: '=msInvoiceCategories',
				returnInvoiceCategory: '=msReturnInvoiceCategory',
				disabled: '<disabled',
				currentSelection: '=msCurrentSelection',
				dialogId: '=msDialogId',
				hideSelectionButton: '=msHideSelectionButton',
				expenseType: '=msExpenseType',
				hiddenSelButtonActivator: '=msHiddenSelButtonActivator'
			},
			link: link,
			controller: InvoicecategorieSelectController,
			controllerAs: 'vm',
			bindToController: true
		};

		return directive;

		function link(scope, element, attrs, vm) {
            //
			// scope.$watch('InvoicecategoriesService.getParentInvoiceCategories()', function(newValue, oldValue) {
			// 	initCategories();
			// });
		}
	}

	InvoicecategorieSelectController.$inject = ['$filter','$log','InvoicecategoriesService','_'];

	/* @ngInject */
	function InvoicecategorieSelectController($filter,$log,InvoicecategoriesService,_) {
		var vm = this;

		vm.newInvoiceCategoryName = '';
		
		vm.getParentInvoiceCategories = function()
		{
			if (!vm.categorieParentsFiltered){
				return InvoicecategoriesService.getParentInvoiceCategories(vm.expenseType);
			}else{
				return vm.categorieParentsFiltered;
			}

		};

		vm.getDialogId = function(customIdentifier){
			var resultId = '#' + vm.dialogId;
			if (customIdentifier){
				resultId = resultId + customIdentifier;
			}
			return resultId;
		};

        vm.selectNone = function(){
            vm.returnInvoiceCategory(null);
        };

		vm.getDialogIdWithoutHash = function(customIdentifier){
			var resultId = vm.dialogId;
			if (customIdentifier){
				resultId = resultId + customIdentifier;
			}
			return resultId;
		};

		vm.findInvoiceCategoriesForParent = function(parentInvoiceCategory){
			return _.filter(InvoicecategoriesService.getInvoiceCategories(), function(invoiceCategory){ return (invoiceCategory.parentInvoiceCategoryDTO != null && parentInvoiceCategory.invoiceCategoryId === invoiceCategory.parentInvoiceCategoryDTO.invoiceCategoryId);});
		};

		vm.selectInvoiceCategory = function(invoiceCategorie){
			vm.returnInvoiceCategory(invoiceCategorie);
			vm.invoiceCategorySearch = "";
			vm.refreshInvoiceParents();
		};

		vm.searchFilterForInvoiceCategories = function() {
			return vm.searchMatcher(vm.invoiceCategorySearch);
		};

		vm.refreshInvoiceParents = function(){
			vm.categorieParentsFiltered = _.filter(InvoicecategoriesService.getParentInvoiceCategories(), function(parentInvoiceCategory){
				var hasChilds = false;

				var childCategories = _.filter(InvoicecategoriesService.getInvoiceCategories(), function(invoiceCategory){ return (invoiceCategory.parentInvoiceCategoryDTO != null && parentInvoiceCategory.invoiceCategoryId === invoiceCategory.parentInvoiceCategoryDTO.invoiceCategoryId);});
				var searchFilter = vm.searchFilterForInvoiceCategories();

				childCategories.forEach(function(item){
					if (searchFilter(item)){
						hasChilds = true;
					}
				});

				return hasChilds;
			});
		};

		vm.searchMatcher = function(searchInput){
			var searchMatch = (searchInput ? new RegExp(".*" + searchInput + ".*", 'i') : null);
			return function(item) {
				if (searchMatch && !searchMatch.test(item.invoiceCategoryName)) {
					return false;
				}
				return true;
			};
		};
		
		vm.createInvoiceCategory = function(parentInvoiceCategory){
			InvoicecategoriesService.create(vm.newInvoiceCategoryName, parentInvoiceCategory.invoiceCategoryId).then(function(result){
				vm.refreshInvoiceParents();
			});
			vm.parentInvoiceCategoryAdding[parentInvoiceCategory.invoiceCategoryId] = false;
		};

		vm.removeInvoiceCategory = function(invoiceCategory){
            InvoicecategoriesService.remove(invoiceCategory.invoiceCategoryId).then(function(result){
                vm.refreshInvoiceParents();
            });
		}
	}

})();

