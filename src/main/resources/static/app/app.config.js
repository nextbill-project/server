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
		.module('msWebApp')
		.config(configure);

	configure.$inject = ['$httpProvider','$urlRouterProvider','$stateProvider','$translateProvider'];

	function configure($httpProvider,$urlRouterProvider,$stateProvider,$translateProvider) {
		var langCode = 'de';

		$httpProvider.defaults.xsrfHeaderName = csrfToken;
		$httpProvider.defaults.xsrfCookieName = csrfToken;

		$urlRouterProvider
			.when('/invoices', '/invoices/list')
			.otherwise('/');

		$stateProvider.
			state('dashboard', {
				url: '/',
				templateUrl: 'app/dashboard/dashboard.html',
				controller: 'DashboardController',
				controllerAs: 'vm'
			}).
			state('setup', {
				url: '/setup',
				templateUrl: 'app/settings/setup.html',
				controller: 'SetupController',
				controllerAs: 'vm'
			}).
			state('profile', {
				url: '/profile',
				templateUrl: 'app/settings/profile.html',
				controller: 'ProfileController',
				controllerAs: 'vm'
			}).
			state('invoices', {
				abstract: true,
				url: '/invoices',
				template: '<ui-view/>'
			}).
			state('invoices.list', {
				url: '/list?view',
				templateUrl: 'app/invoices/invoice-list.html',
				controller: 'InvoiceListController',
				controllerAs: 'vm'
			}).
			state('user', {
				abstract: true,
				url: '/user',
				template: '<ui-view/>'
			}).
			state('user.list', {
				url: '/list',
				templateUrl: 'app/user/user-list.html',
				controller: 'UserListController',
				controllerAs: 'vm'
			}).
			state('budgets', {
				abstract: true,
				url: '/budgets',
				template: '<ui-view/>'
			}).
			state('budgets.list', {
				url: '/budgets',
				templateUrl: 'app/budgets/budget-list.html',
				controller: 'BudgetListController',
				controllerAs: 'vm'
			}).
			state('budgets.create', {
				url: '/new',
				templateUrl: 'app/budgets/budget-edit.html',
				controller: 'BudgetEditController',
				controllerAs: 'vm'
			}).
			state('budgets.edit', {
				url: '/:budgetId',
				templateUrl: 'app/budgets/budget-edit.html',
				controller: 'BudgetEditController',
				controllerAs: 'vm'
			}).
			state('standingorders', {
				abstract: true,
				url: '/standingorders',
				template: '<ui-view/>'
			}).
			state('standingorders.list', {
				url: '/list?view',
				templateUrl: 'app/standingorders/standingorder-list.html',
				controller: 'StandingOrderListController',
				controllerAs: 'vm'
			}).
			state('billings', {
				abstract: true,
				url: '/billings',
				template: '<ui-view/>'
			}).
			state('billings.processList', {
				url: '/processList',
				templateUrl: 'app/billings/billing-process-list.html',
				controller: 'BillingProcessListController',
				controllerAs: 'vm'
			}).
			state('billings.create', {
				url: '/create',
				templateUrl: 'app/billings/billing-create.html',
				controller: 'BillingCreateController',
				controllerAs: 'vm',
				params: {
					sumToBePaid: null,
					userContact: null,
					billingListItem: null
				}
			}).
			state('billings.details', {
				url: '/:billingId',
				templateUrl: 'app/billings/billing-details.html',
				controller: 'BillingDetailsController',
				controllerAs: 'vm'
			}).
			state('invoices.create', {
				url: '/new?transactionType&returnView&isStandingOrder',
				templateUrl: 'app/invoices/invoice-edit.html',
				controller: 'InvoiceEditController',
				controllerAs: 'vm'
			}).
			state('invoices.details', {
				url: '/:invoiceId?isStandingOrder&transactionType',
				templateUrl: 'app/invoices/invoice-edit.html',
				controller: 'InvoiceEditController',
				controllerAs: 'vm',
				params: {
					returnView: null
				}
			}).
			state('analysis', {
				abstract: true,
				url: '/analysis',
				template: '<ui-view/>'
			}).
			state('analysis.charts', {
				url: '/charts',
				templateUrl: 'app/analysis/analysis-chart.html',
				controller: 'AnalysisChartController',
				controllerAs: 'vm'
			}).
			state('fileanalyses', {
				abstract: true,
				url: '/fileanalyses',
				template: '<ui-view/>'
			}).
			state('fileanalyses.upload', {
				url: '/upload',
				templateUrl: 'app/fileanalyses/fileanalyses-upload.html',
				controller: 'FileAnalysesController',
				controllerAs: 'vm'
			});

		$translateProvider.
			useSanitizeValueStrategy('sanitize').
			useStaticFilesLoader({
				prefix: 'i18n/locale-',
				suffix: '.json'
			}).
			fallbackLanguage('de').
			preferredLanguage(langCode);
	}

})();