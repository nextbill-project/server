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
		.module('msWebApp.msArticlesSelect', [])
		.component('msArticlesSelect', {
			templateUrl: 'app/components/ms-articles-select.html',
			bindings: {
				callback: '=msCallback',
				data: '=msArticleDialogData'
			},
			controller: ArticlesSelectController,
			controllerAs: 'vm'
		});

	ArticlesSelectController.$inject = ['$filter','$log','$timeout','$http','_'];

	/* @ngInject */
	function ArticlesSelectController($filter,$log,$timeout,$http,_) {
		var vm = this;

		vm.showLoadingText = true;

		var dataInput = vm.data();

		vm.articles = dataInput.articles ? dataInput.articles : [];
		vm.imageUrl = dataInput.imageUrl;
		vm.invoiceId = dataInput.invoiceId;
		vm.username = dataInput.username ? dataInput.username : 'Keine Eingabe';

		setTimeout(function() {
				prepareAnalysis();
			}
		, 1000);


		console.log("article select started");

		vm.calculateSum = function(){
			vm.sum = 0;

			vm.articles.forEach(function(item){
				vm.sum = vm.sum + item.price;
			});

			var roundedString = vm.sum.toFixed(2);
			vm.sum = Number(roundedString);
		};

		vm.useThisSum = function() {
			vm.callback(vm.articles, vm.sum);
		};

		setTimeout(function() {
			document.getElementById('imageTarget' + vm.invoiceId).addEventListener('click', function (event) {
				var bounds=this.getBoundingClientRect();
				var left=bounds.left;
				var top=bounds.top;
				var x = event.pageX - left;
				var y = event.pageY - top;
				var cw=this.clientWidth;
				var ch=this.clientHeight;
				var iw=this.naturalWidth;
				var ih=this.naturalHeight;
				var px=x/cw*iw;
				var py=y/ch*ih;

				var percentX = (px/iw).toPrecision(8);
				var percentY = (py/ih).toPrecision(8);

				findArticleNameAndSum(percentX, percentY);
			});
		}, 100);

		vm.calculateSum();

		vm.removeArticleFromList = function(article){
			var indexOfPosition = vm.articles.findIndex(function(item){
				return item === article;
			});

			vm.articles.splice(indexOfPosition,1);

			vm.calculateSum();
		};

		function findArticleNameAndSum(x, y) {

			var httpConfiguration = {
				method: 'POST',
				url: '/webapp/api/service/upload/'+vm.invoiceId+'/articleAnalysis?x='+x+'&y='+y
			};

			$http(httpConfiguration).then(
				function(result) {
					console.log('Article name:'+result.data.name+' price:'+result.data.price);
					vm.articles.push(result.data);

					vm.calculateSum();
				}
			);
		}

		function prepareAnalysis() {

			var httpConfiguration = {
				method: 'POST',
				url: '/webapp/api/service/upload/'+vm.invoiceId+'/articleAnalysisPreparement'
			};

			$http(httpConfiguration).then(
				function(result) {
					vm.showLoadingText = false;
				},
				function(error) {
					vm.showLoadingText = false;
				}
			);
		}

	}

})();

