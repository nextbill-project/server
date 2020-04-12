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
		.controller('InvoiceEditController', InvoiceEditController);

	InvoiceEditController.$inject = ['$timeout','$filter','$log','csrfToken', 'FileUploader','$state','$stateParams','$scope','InvoiceService','BusinesspartnerService','UsercontactService','CostdistributionitemService','CostdistributioncalculateService','InvoiceimageService','InvoicecategoriesService','CostdistributionService', 'UserService'];

	/* @ngInject */
	function InvoiceEditController($timeout,$filter,$log,csrfToken,FileUploader,$state,$stateParams,$scope,InvoiceService,BusinesspartnerService,UsercontactService,CostdistributionitemService,CostdistributioncalculateService,InvoiceimageService,InvoicecategoriesService,CostdistributionService,UserService) {
		var vm = this;
		vm.title = 'InvoiceEditController';

		vm.invoice = {};
		vm.invoice.articleDTOs = [];
		vm.createMode = false;

        vm.mistakeMessage = '';

        vm.imagePreview = null;

		vm.uploader = new FileUploader({
			url: '/webapp/api/invoices/image' ,
			headers : {},
			alias: 'fileUpload'
		});

		if ($stateParams.returnView) {
			vm.returnView = $stateParams.returnView;
		}else{
			vm.returnView = 'invoices.list';
		}

		if ($stateParams.transactionType) {
			vm.invoice.mainFunctionEnum = $stateParams.transactionType;
            vm.createMode = true;
		}else{
			vm.invoice.mainFunctionEnum = 'EXPENSE';
		}

		if ($stateParams.isStandingOrder) {
			vm.isStandingOrder = true;
		}else{
			vm.isStandingOrder = false;
		}

		vm.costDistributionItems = [];
		vm.payerName = '';
		vm.paymentRecipientName = '';


		vm.currentUser = {};

		vm.hasInvoiceImageValue = false;
		vm.loadingBarCategory = false;

		if ($stateParams.invoiceId) {
			vm.invoice.invoiceId = $stateParams.invoiceId;
		}
		
		vm.repetionTypeEnums = [];
		vm.paymentTypeEnums = [];

		vm.userSelectSearch = '';

		vm.standingOrder = {};
		vm.originalStandingOrder = {};
		vm.costDistributions = [];
        vm.costDistributionsMenu = [];

        vm.costDistributionsMenuOptions = [];

        vm.selectedCostDistributionItemForArticleSelection = null;

		vm.submitForm = function() {
			if (!areChanges()){
				goToOverview();
			}else{
				if (vm.createMode) {
					createInvoice();
				} else {
					updateInvoice();
				}
			}
		};

		vm.resetForm = function() {
			if (vm.invoice.invoiceId) {
				getInvoice();
			} else {
				vm.invoice = {};
			}

			$scope.invoiceForm.$setPristine();
			$scope.invoiceForm.$setUntouched();
		};

		vm.repeatAnalysis = function() {
            repeatAnalysis();
		};

		vm.getInvoice = function() {
			return vm.invoice;
		};

		vm.addCostDistributionItemToList = function (payer){
			var costDistributionItem = {
				'costDistributionItemId':generateUUID(),
				'costDistributionItemTypeEnum':'QUOTA',
				'position':vm.costDistributionItems.length,
				'value':1.0,
				'costPaid':0,
				'invoiceId':vm.invoice.invoiceId,
                'payerId': payer.id,
				'paymentPersonName':payer.name,
				'paymentPersonTypeEnum':payer.type,
				'payerDTO':{
					'displayName':payer.name
				}
			};

			if (payer.virtualType != undefined && payer.virtualType === 'USER'){
                costDistributionItem.correctionStatus = 'CHECK'
			}else if (payer.type === 'CONTACT'){
                costDistributionItem.correctionStatus = 'READY'
            }else{
                costDistributionItem.correctionStatus = 'IGNORE'
            }

			vm.costDistributionItems.push(costDistributionItem);

			vm.updateCostDistributionItems();
		};
		
		vm.isReadOnlyMode = function(){
			if (vm.invoice.invoiceWorkflowMode === 'EXTERNAL_USER_CHECK_MODE' || vm.invoice.invoiceWorkflowMode === 'EXTERNAL_USER_READY_MODE')	{
				return true;
			}
			
			return false;
		};

		vm.generateStandingOrder = function (){
			InvoiceService.generateStandingOrder(vm.standingOrder, true);
		};

		vm.selectInvoiceCategory = function (invoiceCategory){
			vm.invoice.invoiceCategoryDTO = invoiceCategory;

			getAttributesForCategory(vm.invoice.invoiceCategoryDTO.invoiceCategoryId);
		};

		vm.updateCostDistributionItems = function(){
			if (vm.invoice.sumOfInvoice != null && vm.invoice.sumOfInvoice != undefined){
				vm.costDistributionItems.forEach(function(item){
					item.moneyValue = CostdistributioncalculateService.calculateAmountForCostDistributionItemPrecise(item, vm.costDistributionItems, vm.invoice.sumOfInvoice);
				})
			}
		};

		vm.resetInvoice = function(){
			vm.costDistributionItems = angular.copy(vm.originalCostDistributionItems);
			vm.invoice = angular.copy(vm.originalInvoice);

			if (vm.isStandingOrder){
				vm.standingOrder = angular.copy(vm.originalStandingOrder);
			}

			refreshInvoiceData();
		};

		vm.paymentRecipientTransfer = function (payer){
            if (payer != null){
                vm.invoice.paymentRecipientId = payer.id;
                vm.invoice.paymentRecipientTypeEnum = payer.type;
                vm.paymentRecipientName = payer.name;

                if (payer.virtualType != undefined && payer.virtualType === 'USER'){
                    vm.invoice.correctionStatus = 'CHECK'
                }else if (payer.type === 'CONTACT'){
                    vm.invoice.correctionStatus = 'READY'
                }else{
                    vm.invoice.correctionStatus = 'IGNORE'
                }

                if (payer.type === 'BUSINESS_PARTNER'){
                    getCategoryForBusinessPartner(payer.id);
                }
            }else{
                vm.invoice.paymentRecipientId = null;
                vm.invoice.paymentRecipientTypeEnum = null;
                vm.paymentRecipientName = null;
            }
		};

		vm.showDialog = function(id){
			$(vm.getDialogId(id)).modal('show');
		};

		vm.updateCostDistributionItemDirectiveExec = function(id){
			$log.info(id);
			$log.info(vm.updateCostDistributionItemDirective);
			vm.updateCostDistributionItemDirective[id]();
		};

		vm.payerTransfer = function (payer){
			if (payer != null){
                vm.invoice.payerId = payer.id;
                vm.invoice.payerTypeEnum = payer.type;
                vm.payerName = payer.name;


                if (payer.virtualType != undefined && payer.virtualType === 'USER'){
                    vm.invoice.correctionStatus = 'CHECK'
                }else if (payer.type === 'CONTACT'){
                    vm.invoice.correctionStatus = 'READY'
                }else{
                    vm.invoice.correctionStatus = 'IGNORE'
                }

                if (payer.type === 'BUSINESS_PARTNER'){
                    getCategoryForBusinessPartner(payer.id);
                }
			}else{
                vm.invoice.payerId = null;
                vm.invoice.payerTypeEnum = null;
                vm.payerName = null;
			}
		};

		vm.remove = function() {
			if (vm.isStandingOrder){
				vm.removeInvoiceTemplate();
			}else{
				vm.removeInvoice();
			}
		};

        vm.removeImage = function() {
			vm.invoice.invoiceImageId = null;
			vm.hasInvoiceImage();
			vm.invoice.articleDTOs = [];
			vm.imagePreview = undefined;
			vm.uploader.clearQueue();
        };

		vm.getDialogId = function(customIdentifier){
			var resultId = '#';
			if (customIdentifier != undefined){
				resultId = resultId + customIdentifier;
			}
			return resultId;
		};

		vm.getDialogIdWithoutHash = function(customIdentifier){
			var resultId = customIdentifier;
			return resultId;
		};

		vm.showArticlesInvoiceDialog = function() {
			vm.showArticlesDialogForInvoice = true;
			$('#articlesModalDialogForInvoice').modal('show');
		};

		vm.hideArticlesInvoiceDialog = function() {
			$('#articlesModalDialogForInvoice').modal('hide');
			vm.showArticlesDialogForInvoice = false;
		};

        vm.selectArticleInvoiceData = function(){
            if (vm.showPaymentRecipient()){
                return { username: vm.paymentRecipientName,
					invoiceId: vm.invoice.invoiceId,
					articles: vm.invoice.articleDTOs,
					imageUrl: vm.getInvoiceImageUrl()};
            }else if (vm.showPayer()){
                return { username: vm.payerName,
                    invoiceId: vm.invoice.invoiceId,
                    articles: vm.invoice.articleDTOs,
                    imageUrl: vm.getInvoiceImageUrl()};
			}
        };

        vm.callbackForArticlesSelectInvoice = function(articles, sum) {
        	vm.invoice.articleDTOs = articles;
        	vm.invoice.sumOfInvoice = sum;

            vm.updateCostDistributionItems();
			vm.hideArticlesInvoiceDialog();
		};

        vm.selectArticleCostDistributionItem = function(costDistributionItem){
			vm.selectedCostDistributionItemForArticleSelection = costDistributionItem;
			vm.showArticlesDialogForCostDistributionItem = true;
            $('#articlesModalDialogCostDistributionItem').modal('show');
        };

        vm.hideSelectArticleCostDistributionItemDialog = function() {
			$('#articlesModalDialogCostDistributionItem').modal('hide');
			vm.showArticlesDialogForCostDistributionItem = false;
		};

        vm.selectedArticleCostDistributionItemData = function(){
			return { username: vm.selectedCostDistributionItemForArticleSelection.payerDTO.displayName,
				invoiceId: vm.invoice.invoiceId,
				articles: vm.selectedCostDistributionItemForArticleSelection.articleDTOs,
				imageUrl: vm.getInvoiceImageUrl()};
        };

        vm.callbackForArticlesSelectCostDistributionItem = function(articles, sum) {
            vm.selectedCostDistributionItemForArticleSelection.articleDTOs = articles;
            vm.selectedCostDistributionItemForArticleSelection.moneyValue = sum;
            vm.selectedCostDistributionItemForArticleSelection.value = sum;
            vm.selectedCostDistributionItemForArticleSelection.costDistributionItemTypeEnum = 'FIXED_AMOUNT';

            vm.updateCostDistributionItems();
			vm.hideSelectArticleCostDistributionItemDialog();
        };

		vm.showPaymentRecipient = function(){
			return (vm.invoice.mainFunctionEnum === 'EXPENSE' && !vm.invoice.reverseInvoice) || (vm.invoice.mainFunctionEnum !== 'EXPENSE' && vm.invoice.reverseInvoice);
		};

		vm.showPayer = function(){
			return (vm.invoice.mainFunctionEnum === 'INCOME' && !vm.invoice.reverseInvoice) || (vm.invoice.mainFunctionEnum !== 'INCOME' && vm.invoice.reverseInvoice);
		};

		vm.removeCostDistributionItemFromList = function(costDistributionItem){
			var indexOfPosition = vm.costDistributionItems.findIndex(function(item){
				return item.costDistributionItemId === costDistributionItem.costDistributionItemId;
			});
			
			vm.costDistributionItems.splice(indexOfPosition,1);

			var sum = vm.invoice.sumOfInvoice;

            if (vm.costDistributionItems.length === 1){

                var lastCostDistributionItem = vm.costDistributionItems[0];

                var futureSumOfCostDistributionItem = CostdistributioncalculateService.calculateAmountForCostDistributionItemPrecise(lastCostDistributionItem, vm.costDistributionItems, vm.invoice.sumOfInvoice);

                if (futureSumOfCostDistributionItem > sum){
                    var costDistributionItemTypeEnum = lastCostDistributionItem.costDistributionItemTypeEnum;

                    if ('FIXED_AMOUNT' === costDistributionItemTypeEnum){
                        lastCostDistributionItem.value = sum;
                    }else if ('PERCENT' === costDistributionItemTypeEnum){
                        lastCostDistributionItem.value = 100;
                    }else if ('QUOTA' === costDistributionItemTypeEnum){
                        lastCostDistributionItem.value = 1;
                    }

                    lastCostDistributionItem.moneyValue = sum;
                }
            }

			vm.updateCostDistributionItems();
		};



		vm.hasInvoiceImage = function(){
			vm.hasInvoiceImageValue = !!vm.invoice.invoiceImageId;
		};

		vm.getInvoiceImageUrl = function(){
			if (vm.imagePreview) {
				return vm.imagePreview;
			}

			return InvoiceimageService.getInvoiceImageUrl(vm.invoice.invoiceImageId);
		};

		vm.goToOverview = function() {
			goToOverview();
		};

        vm.removeInvoice = function() {
            InvoiceService.remove(vm.invoice.invoiceId);
            goToOverview();
        };

		vm.removeInvoiceTemplate = function() {
			InvoiceService.deleteStandingOrder(vm.invoice.invoiceId);
			goToOverview();
		};

        vm.isNullOrUndefined = function(value){
        	if (value === null || value === undefined){
        		return true;
			}
			return false;
		};

        vm.setCorrectionStatus = function(correctionStatus) {
        	(vm.isNullOrUndefined(vm.correctionStatus)) ? vm.correctionStatus = correctionStatus : vm.correctionStatus = null;
        	updateInvoice();
        };

        vm.setCostPaidStatus = function(paymentItem){
            if (!paymentItem.costPaid || paymentItem.costPaid === 0){
                paymentItem.costPaid = paymentItem.moneyValue;
            }else{
                paymentItem.costPaid = 0;
            }
		};

        vm.setInvoiceCostPaidStatus = function(paymentItem){
            if (!paymentItem.costPaid || paymentItem.costPaid === 0){
                paymentItem.costPaid = paymentItem.sumOfInvoice;
            }else{
                paymentItem.costPaid = 0;
            }
        };

        vm.showPaymentRecipientPaymentPersonHasPaidButton = function(){
            if (vm.invoice.paymentRecipientTypeEnum != null &&
                (vm.invoice.paymentRecipientTypeEnum === 'CONTACT' || vm.invoice.paymentRecipientTypeEnum === 'USER') &&
                !(vm.invoice.paymentRecipientId === vm.invoice.createdById)){
                return true;
            }else {
                return false;
            }
        };

        vm.showPayerPaymentPersonHasPaidButton = function(){
            if (vm.invoice.payerTypeEnum != null &&
                (vm.invoice.payerTypeEnum === 'CONTACT' || vm.invoice.payerTypeEnum === 'USER') &&
                !(vm.invoice.payerId === vm.invoice.createdById)){
                return true;
            }else {
                return false;
            }
        };

        vm.showCostPaymentPersonHasPaidButton = function(costDistributionItem){
            if ((costDistributionItem.paymentPersonTypeEnum != null &&
                (costDistributionItem.paymentPersonTypeEnum === 'CONTACT' || costDistributionItem.paymentPersonTypeEnum === 'USER')) &&
                costDistributionItem.payerId !== vm.invoice.createdById &&
				vm.invoice.sumOfInvoice !== 0){
                return true;
            }else {
                return false;
            }
        };

        vm.paymentItemPersonHasPaid = function(paymentItem){
			if ((paymentItem.costPaid == null && paymentItem.moneyValue != 0) ||
				(paymentItem.costPaid != null && paymentItem.costPaid != paymentItem.moneyValue != 0)){
				return false;
			}else{
				return true;
			}
        };

        vm.invoicePaymentItemPersonHasPaid = function(paymentItem){
            if ((paymentItem.costPaid == null && paymentItem.sumOfInvoice != 0) ||
                (paymentItem.costPaid != null && paymentItem.costPaid != paymentItem.sumOfInvoice != 0)){
                return false;
            }else{
                return true;
            }
        };

		vm.createNewCostDistributionItem = function (){

			vm.costDistributionItems.push({
            'costDistributionItemId':generateUUID(),
            'costDistributionItemTypeEnum':'QUOTA',
            'position':vm.costDistributionItems.length,
            'value':1.0,
            'costPaid':0,
            'invoiceId':vm.invoice.invoiceId,
            'correctionStatus':'READY',
            'payerId' : vm.currentUser.appUserId,
            'payerTypeEnum' : 'USER',
            'paymentPersonName':vm.currentUser.appUserName,
            'paymentPersonTypeEnum':'USER',
            'payerDTO':{
                'displayName':vm.currentUser.appUserName
            }
        });

			vm.updateCostDistributionItems();

            vm.originalCostDistributionItems = angular.copy(vm.costDistributionItems);
		};

        vm.correctDateAndRepetitionForStandingOrder = function(){
            var hasChangesInDate = (vm.originalStandingOrder.startDate != vm.standingOrder.startDate);

            if (hasChangesInDate){
            	vm.invoice.dateOfInvoice = vm.standingOrder.startDate;
			}else{
            	vm.invoice.dateOfInvoice = vm.originalInvoice.dateOfInvoice;
			}

			vm.invoice.repetitionTypeEnum = vm.standingOrder.repetitionTypeEnum;
		};

        vm.areChanges = function(){
			return areChanges();
		};

        vm.useCostDistribution = function(costDistribution){
            vm.costDistributionItems = [];
            costDistribution.costDistributionItemDTOS.forEach(function(innerItem){
                var newCostDistributionItem = CostdistributionService.prepareItemForCostDistribution(angular.copy(innerItem));
                vm.costDistributionItems.push(newCostDistributionItem);
            });

            vm.costDistributionItems.forEach(function(innerItem){
                innerItem.moneyValue = CostdistributioncalculateService.calculateAmountForCostDistributionItemPrecise(innerItem, vm.costDistributionItems, vm.invoice.sumOfInvoice);
                innerItem.invoiceId = vm.invoice.invoiceId;
            });
		};

        vm.createCostDistributionRequest = function(){
        	var tmpCostDistribution = {
        		name: vm.newCostDistributionName
            };
            CostdistributionService.create(tmpCostDistribution, vm.costDistributionItems).then(function(result){
                vm.newCostDistributionName = null;
                vm.costDistributions.push(result);
			});
		};

        vm.removeCostDistribution = function(costDistributionInput){
            CostdistributionService.remove(costDistributionInput.costDistributionId).then(function(result){
                var foundCostDistribution = _.filter(vm.costDistributions, function(costDistribution){ return (costDistribution.costDistributionId === costDistributionInput.costDistributionId);});
                if (foundCostDistribution != null && foundCostDistribution != undefined && foundCostDistribution.length > 0){
                    var foundCostDistributionIndex = _.indexOf(vm.costDistributions, foundCostDistribution[0]);

                    vm.costDistributions.splice(foundCostDistributionIndex, 1);
                }
			})
		};

        vm.sendMistakeMessage = function(){
        	InvoiceService.sendMistakeMessage(vm.invoice.invoiceId, vm.mistakeMessage).then(function(result){
                vm.mistakeMessage = '';
			});
		};

        vm.useStandardCostDistribution = function(){
        	vm.costDistributionItems = [];

            vm.costDistributionItems.push({
				'costDistributionItemId':generateUUID(),
				'costDistributionItemTypeEnum':'QUOTA',
				'position':vm.costDistributionItems.length,
				'value':1.0,
				'costPaid':0,
				'invoiceId':vm.invoice.invoiceId,
				'correctionStatus':'READY',
				'payerId' : vm.currentUser.appUserId,
				'payerTypeEnum' : 'USER',
				'paymentPersonName':vm.currentUser.appUserName,
				'paymentPersonTypeEnum':'USER',
				'payerDTO':{
					'displayName':vm.currentUser.appUserName
				}
			});

            vm.updateCostDistributionItems();
        };

        vm.deleteConfirmationMenu = [
            ['Löchen bestätigen', function ($itemScope, $event, color) {
                vm.remove();
            }]
        ];

		activate();

		////////////////

		function areChanges(){
            var isStandingOrderEqual = true;

            if (vm.imagePreview) {
            	return true;
			}

            if (vm.isStandingOrder){
                isStandingOrderEqual = (vm.originalStandingOrder.startDate == vm.standingOrder.startDate && vm.originalStandingOrder.repetitionTypeEnum == vm.standingOrder.repetitionTypeEnum);
            }
            var isAllAnotherEqual = (angular.equals(vm.originalCostDistributionItems, vm.costDistributionItems) && angular.equals(vm.originalInvoice, vm.invoice));
            if ((isStandingOrderEqual && isAllAnotherEqual) || (vm.invoice.invoiceWorkflowMode === 'EXTERNAL_USER_CHECK_MODE' || vm.invoice.invoiceWorkflowMode === 'EXTERNAL_USER_READY_MODE')){
				return false;
            }
            return true;
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

		function getCurrentUser(todoAfter) {
			UserService.currentUser().then(function(result) {
				vm.currentUser = result;

				if (todoAfter != undefined){
					todoAfter();
				}
			});
		}

		function toDoAfterCurrentUserGet(){
			vm.createNewCostDistributionItem();

			if (vm.invoice.mainFunctionEnum === 'INCOME'){
				vm.invoice.paymentRecipientId = vm.currentUser.appUserId;
				vm.invoice.paymentRecipientTypeEnum = 'USER';
			}else{
				vm.invoice.payerId = vm.currentUser.appUserId;
				vm.invoice.payerTypeEnum = 'USER';
			}
            vm.originalInvoice = angular.copy(vm.invoice);
		}

		function activate() {

			if (vm.invoice.invoiceId) {
				getCurrentUser();
				getInvoice();


			}else{
				var today = new Date();
				vm.invoice.dateOfInvoice = today.getTime();
				vm.invoice.invoiceId = generateUUID();
				if (vm.isStandingOrder){
                    vm.invoice.invoiceStatusEnum = 'CHECK';
				}else{
                    vm.invoice.invoiceStatusEnum = 'READY';
				}
				vm.invoice.invoiceSource = 'MANUAL';
				vm.invoice.repetitionTypeEnum = 'ONCE';
				vm.invoice.paymentTypeEnum = 'NOT_DEFINED';
				getCurrentUser(toDoAfterCurrentUserGet);
				vm.invoice.sumOfInvoice = 0;
				vm.invoice.costPaid = 0;
			}

			vm.uploader = new FileUploader({
				url: '/webapp/api/invoices/image/' + vm.invoice.invoiceId,
				headers : {},
				alias: 'fileUpload'
			});

			var csrf_token = getCookie(csrfToken);
			vm.uploader.headers[csrfToken] = csrf_token;

			vm.uploader.filters.push({
				name: 'syncFilter',
				fn: function(item /*{File|FileLikeObject}*/, options) {
					console.log('syncFilter');
					return this.queue.length < 2;
				}
			});

			vm.uploader.onAfterAddingFile = function(fileItem) {

				for(var i = 0; i < vm.uploader.queue.length - 1; i++) {
					vm.uploader.queue[0].remove();
				}
				vm.uploader.progress = 0;

				console.info('onAfterAddingFile', fileItem);
				var reader = new FileReader();
				reader.onload = function(){
					vm.imagePreview = reader.result;
				};
				reader.readAsDataURL(fileItem._file);

				$timeout(function(){
					vm.hasInvoiceImage();
				}, 500);

				vm.invoice.invoiceImageId = undefined;
			};
			vm.uploader.onSuccessItem = function(fileItem, response, status, headers) {
				console.info('onSuccessItem', fileItem, response, status, headers);
				vm.invoice.invoiceImageId = response.invoiceImageId;
				vm.originalInvoice.invoiceImageId = response.invoiceImageId;
				vm.hasInvoiceImage();
			};
			
			vm.standingOrder.invoiceTemplateId = vm.invoice.invoiceId;
			vm.standingOrder.futureInvoiceTemplateId = generateUUID();
			getStandingOrder();
			getRepetitionTypeEnums();
			getPaymentTypeEnums();
			getCostDistributions();
			hasScanRight();
		}

		function getPaymentPerson(id, type){
			if (type === 'BUSINESS_PARTNER'){
				BusinesspartnerService.getOne(id).then(
					function(response) {
						vm.payerName = response.data.businessPartnerName;
					}
				);
			}else if (type === 'USER' || type === 'CONTACT' || type === 'PROJECT'){
				UsercontactService.getOne(id).then(
					function(response) {
						vm.payerName = response.data.contactName;
					}
				);
			}
		}

		var goToOverview = function () {
			if (angular.isObject(vm.returnView)){
				$state.go(vm.returnView.view, vm.returnView.params);
			}else{
				$state.go(vm.returnView);
			}
		};

		function refreshInvoiceData(){
			// vm.isStandingOrder = false;
			// if (vm.invoice.standingOrderInvoiceTemplateId === vm.invoice.invoiceId){
			// 	vm.isStandingOrder = true;
			// }

			vm.hasInvoiceImage();

			if (vm.invoice.paymentRecipientId != null){
				vm.paymentRecipientName = vm.invoice.paymentRecipientDTO.displayName;
			}

			if (vm.invoice.payerId != null){
				vm.payerName = vm.invoice.payerDTO.displayName;
			}
		}

		function getInvoice() {
			InvoiceService.details(vm.invoice.invoiceId).then(function(response) {
				vm.invoice = response.data;

				refreshInvoiceData();

				getCostDistributionItems();

				vm.originalInvoice = angular.copy(vm.invoice);
			});
		}

		function createInvoice() {
			InvoiceService.create(vm.invoice, vm.costDistributionItems).then(function(result){
				if (vm.imagePreview) {
					vm.uploader.uploadAll();
				}
				if (vm.isStandingOrder){
					InvoiceService.generateStandingOrder(vm.standingOrder, true);
				}
			});
			goToOverview();
		}

		function updateInvoice() {
			InvoiceService.update(vm.invoice, vm.correctionStatus, vm.costDistributionItems).then(function(result){
				if (vm.imagePreview) {
					vm.uploader.uploadAll();
				} else if(vm.originalInvoice.invoiceImageId && !vm.invoice.invoiceImageId) {
					InvoiceService.deleteInvoiceImage(vm.invoice.invoiceId);
				}
				if (vm.isStandingOrder){
					InvoiceService.updateStandingOrder(vm.invoice.invoiceId, vm.standingOrder);
				}
			});
            goToOverview();
		}

        function repeatAnalysis() {
            InvoiceService.repeatAnalysis(vm.invoice.invoiceId).then(function(result){

            });
            goToOverview();
        }

		function getRepetitionTypeEnums() {
			InvoiceService.repetitionTypeEnumList().then(function(result) {
				vm.repetionTypeEnums = result.data;

				if (vm.isStandingOrder){
					var foundRepetionTypeEnum = _.filter(vm.repetionTypeEnums, function(repetionTypeEnum){ return (repetionTypeEnum.name === 'ONCE');});
					if (foundRepetionTypeEnum != null && foundRepetionTypeEnum != undefined && foundRepetionTypeEnum.length > 0){
						var foundRepetionTypeEnumIndex = _.indexOf(vm.repetionTypeEnums, foundRepetionTypeEnum[0]);

						vm.repetionTypeEnums.splice(foundRepetionTypeEnumIndex, 1);
					}
				}
			});
		}

		function getPaymentTypeEnums() {
			InvoiceService.paymentTypeEnumList().then(function(result) {
				vm.paymentTypeEnums = result.data;
			});
		}
		
		function getStandingOrder(){
			if (vm.isStandingOrder){
				$timeout(function(){
					InvoiceService.getStandingOrder(vm.invoice.invoiceId).then(function(result){
						vm.standingOrder.repetitionTypeEnum = result.repetitionTypeEnum;
						vm.standingOrder.startDate = result.startDate;

						vm.originalStandingOrder = angular.copy(vm.standingOrder);
					}, function(response){
						vm.standingOrder.startDate = new Date().getTime();
						vm.standingOrder.repetitionTypeEnum = 'MONTHLY';
						vm.originalStandingOrder = {
							repetitionTypeEnum: vm.standingOrder.repetitionTypeEnum,
							startDate: vm.standingOrder.startDate
						};

						vm.invoice.repetitionTypeEnum = vm.standingOrder.repetitionTypeEnum;
						vm.originalInvoice.repetitionTypeEnum = vm.standingOrder.repetitionTypeEnum;
					})
				}, 400);
			}else{
				vm.originalStandingOrder = angular.copy(vm.standingOrder);
			}
		}

        function getCategoryForBusinessPartner(businessPartnerId) {
			vm.loadingBarCategory = true;
            return BusinesspartnerService.getAutoCategory(businessPartnerId).then(function(result) {
                vm.invoice.invoiceCategoryDTO = result.data;
				vm.loadingBarCategory = false;

                getAttributesForCategory(vm.invoice.invoiceCategoryDTO.invoiceCategoryId);

            },function(result) {
				vm.invoice.invoiceCategoryDTO = null;
				vm.loadingBarCategory = false;

			});
        }

        function getAttributesForCategory(invoiceCategoryId) {

            InvoicecategoriesService.getAutoAttributes(invoiceCategoryId).then(function(result) {

                var costDistributionItems = result.data['costDistributionItems'];
				if (costDistributionItems != null && costDistributionItems != undefined){
					vm.costDistributionItems = costDistributionItems;

                    vm.costDistributionItems.forEach(function(item){
                        item.moneyValue = CostdistributioncalculateService.calculateAmountForCostDistributionItemPrecise(item, vm.costDistributionItems, vm.invoice.sumOfInvoice);
						item.costDistributionItemId = generateUUID();
						item.costDistributionId = null;
                        item.invoiceId = vm.invoice.invoiceId;
                    });
				}

                var repetitionTypeEnum = result.data['repetitionTypeEnum'];
                if (repetitionTypeEnum != null && repetitionTypeEnum != undefined){
                    vm.invoice.repetitionTypeEnum = repetitionTypeEnum;
                }

                var specialType = result.data['isSpecialType'];
                if (specialType != null && specialType != undefined){
                    vm.invoice.specialType = specialType;
                }

            });
        }

		function getCostDistributionItems(){
			CostdistributionitemService.listForInvoice(vm.invoice.invoiceId).then(
				function(response) {
					vm.costDistributionItems = response.data;

					vm.originalCostDistributionItems = angular.copy(vm.costDistributionItems);
				}, function(error){
				}
			);
		}

        function getCostDistributions(){
            vm.costDistributions = CostdistributionService.getCostDistributions();
        }

		function hasScanRight(){

			UserService.hasUserRight('OCR').then(function(result) {
				vm.hasScanRight = result;
			});
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

