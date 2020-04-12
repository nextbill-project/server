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
		.controller('AnalysisChartController', AnalysisChartController);

    AnalysisChartController.$inject = ['$state','moment','SearchService','$stateParams','NotificationService', 'AnalysisService','InvoiceService','Chart','_'];

	/* @ngInject */
	function AnalysisChartController($state,moment,SearchService,$stateParams,NotificationService, AnalysisService,InvoiceService, Chart, _) {
		var vm = this;
		vm.title = 'AnalysisChartController';

        vm.diagramViewOptions = [
			{name: 'Liniendiagramm', code: 'BAR' ,useAbsolutValues:false, isActive:true},
			{name: 'Tortendiagramm', code: 'PIE' , useAbsolutValues:true}
        ];

		vm.diagramOptions = [
			{name: 'Kategorien', diagramType:'CATEGORY'},
            {name: 'Träger', diagramType:'COST_PAYER'},
            {name: 'Tagesvergleich', diagramType:'DAY'},
            {name: 'Zahlungsempfänger', diagramType:'PAYMENT_RECIPIENTS'},
            {name: 'Monatsvergleich', diagramType:'MONTH', isActive:true}
		];

        vm.barChartOptions = {
            // showAllTooltips: true,
            // scales: {
            //     xAxes: [{
            //         ticks: {
            //             suggestedMax: 30,
            //             suggestedMin: 10,
            //             maxTicksLimit: 20,
            //             stepSize: 1
            //         }
            //     }]
            // }
        };

        vm.pieChartOptions = {
            showAllTooltips: true
        };

        Chart.pluginService.register({
            beforeRender: function (chart) {
                if (chart.config.options.showAllTooltips) {
                    // create an array of tooltips
                    // we can't use the chart tooltip because there is only one tooltip per chart
                    chart.pluginTooltips = [];
                    chart.config.data.datasets.forEach(function (dataset, i) {
                        chart.getDatasetMeta(i).data.forEach(function (sector, j) {
                            chart.pluginTooltips.push(new Chart.Tooltip({
                                _chart: chart.chart,
                                _chartInstance: chart,
                                _data: chart.data,
                                _options: chart.options.tooltips,
                                _active: [sector]
                            }, chart));
                        });
                    });

                    // turn off normal tooltips
                    chart.options.tooltips.enabled = false;
                }
            },
            afterDraw: function (chart, easing) {
                if (chart.config.options.showAllTooltips) {
                    // we don't want the permanent tooltips to animate, so don't do anything till the animation runs atleast once
                    if (!chart.allTooltipsOnce) {
                        if (easing !== 1)
                            return;
                        chart.allTooltipsOnce = true;
                    }

                    // turn on tooltips
                    chart.options.tooltips.enabled = true;
                    Chart.helpers.each(chart.pluginTooltips, function (tooltip) {
                        tooltip.initialize();
                        tooltip._options.displayColors = false;
                        tooltip._options.bodyFontSize = tooltip._chart.height*0.025;
                        tooltip._options.yPadding = tooltip._options.bodyFontSize*0.10;
                        tooltip._options.xPadding = tooltip._options.bodyFontSize*0.10;
                        tooltip._options.caretSize = tooltip._options.bodyFontSize*0.3;
                        tooltip._options.cornerRadius = tooltip._options.bodyFontSize*0.20;
                        tooltip.update();
                        // we don't actually need this since we are not animating tooltips
                        tooltip.pivot();
                        tooltip.transition(easing).draw();
                    });
                    chart.options.tooltips.enabled = false;
                }
            }
        });

        vm.selectedMonth = null;
        vm.monthRanges = [];
        vm.showAfterButton = true;
        vm.showBeforeButton = true;

        vm.isSearchFiringActivated = true;

        vm.diagramType = vm.diagramOptions[4];
        vm.diagramViewType = vm.diagramViewOptions[0];

		vm.startSearch = function(){
		    if (vm.isSearchFiringActivated){
                updateChartData();
            }
		};

		vm.setChartType = function(diagramType){
            vm.diagramOptions.forEach(function(item){
               item.isActive = false;
            });
            vm.diagramType = diagramType;
            vm.diagramType.isActive = true;
            updateChartData();
		};
        
        vm.statisticData = function(basicDataSubType){
            var tmpValue = AnalysisService.getStatisticData(basicDataSubType);

            if (tmpValue != undefined){
                return tmpValue.numberValue;
            }else{
                return "Keine Angabe";
            }

        };

        vm.setChartViewType = function(diagramViewType){
            vm.diagramViewOptions.forEach(function(item){
                item.isActive = false;
            });
            vm.diagramViewType = diagramViewType;
            vm.diagramViewType.isActive = true;
            updateChartData();
        };

        vm.chartServiceLoading = function(){
            return AnalysisService.isLoading();
        };

        vm.transferSelectionToSearch = function(){
            if (vm.selectedMonth == undefined || vm.selectedMonth == null || vm.selectedMonth === ''){
                SearchService.getSearchConfiguration().startDate = null;
                SearchService.getSearchConfiguration().endDate = null;
            }else{
                var monthSelected = _.findWhere(vm.monthRanges, {monthNumber: parseInt(vm.selectedMonth)});
                SearchService.getSearchConfiguration().startDate = monthSelected.startMonth;
                SearchService.getSearchConfiguration().endDate = monthSelected.endMonth;

                if (monthSelected.monthNumber == 6){
                    vm.showAfterButton = true;
                    vm.showBeforeButton = false;
                }else if (monthSelected.monthNumber == 0) {
                    vm.showAfterButton = false;
                    vm.showBeforeButton = true;
                }else{
                    vm.showAfterButton = true;
                    vm.showBeforeButton = true;
                }
            }

            updateChartData();
        };

        vm.monthBefore = function(){
            var currentMonthNumber = parseInt(vm.selectedMonth);
            currentMonthNumber = currentMonthNumber + 1;

            if (currentMonthNumber == 6){
                vm.showAfterButton = true;
                vm.showBeforeButton = false;
            }else{
                vm.showAfterButton = true;
                vm.showBeforeButton = true;
            }

            vm.selectedMonth = currentMonthNumber.toString();
            vm.transferSelectionToSearch();
        };

        vm.monthAfter = function(){
            var currentMonthNumber = parseInt(vm.selectedMonth);
            currentMonthNumber = currentMonthNumber - 1;

            if (currentMonthNumber == 0){
                vm.showAfterButton = false;
                vm.showBeforeButton = true;
            }else{
                vm.showAfterButton = true;
                vm.showBeforeButton = true;
            }

            vm.selectedMonth = currentMonthNumber.toString();
            vm.transferSelectionToSearch();
        };

        activate();

        ////////////////

        function activate() {

        	var chartData = AnalysisService.getChartData();
        	if (chartData && chartData.length > 0 ){
        		refreshChart();
			}

            updateChartData();
            generateMonthRanges();
        }

        function updateChartData() {
            if (vm.isSearchFiringActivated){
                vm.isSearchFiringActivated = false;
                AnalysisService.updateChartData(vm.diagramViewType.useAbsolutValues, vm.diagramType.diagramType).then(function(response){
                    refreshChart();
                    vm.isSearchFiringActivated = true;
                }, function(response2){
                    vm.isSearchFiringActivated = true;
                });
            }
        }

        function refreshChart(){
            var chartData = AnalysisService.getChartData();
            if (chartData){
                vm.labels = chartData.xaxesValues;
                vm.chartData = [];
                vm.chartData.push(chartData.yaxesValues);
            }
		}

        function generateMonthRanges(){
            var monthNames = ['Januar', 'Februar', 'März', 'April', 'Mai', 'Juni', 'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'];
            for (var i = 0; i < 7; i++) {
                var currentDate = new moment();
                currentDate = currentDate.subtract(i, 'months');
                var currentDate2 = new moment();
                currentDate2 = currentDate2.subtract(i, 'months');

                var monthRange = {
                    startMonth: currentDate.startOf('month').toDate().getTime(),
                    endMonth: currentDate2.endOf('month').toDate().getTime(),
                    monthName: monthNames[currentDate.month()] + ' ' + currentDate.year(),
                    monthNumber: i
                };

                vm.monthRanges.push(monthRange);
            }
        }
	}

})();

