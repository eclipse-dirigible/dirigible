angular.module('page', ["ideUI", "ideView", "entityApi"])
	.config(["messageHubProvider", function (messageHubProvider) {
		messageHubProvider.eventIdPrefix = 'DependsOnScenariosTestProject.SalesOrder.SalesOrderItem';
	}])
	.config(["entityApiProvider", function (entityApiProvider) {
		entityApiProvider.baseUrl = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/SalesOrder/SalesOrderItemService.ts";
	}])
	.controller('PageController', ['$scope', 'messageHub', 'ViewParameters', 'entityApi', function ($scope, messageHub, ViewParameters, entityApi) {

		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: "SalesOrderItem Details",
			create: "Create SalesOrderItem",
			update: "Update SalesOrderItem"
		};
		$scope.action = 'select';

		let params = ViewParameters.get();
		if (Object.keys(params).length) {
			$scope.action = params.action;
			$scope.entity = params.entity;
			$scope.selectedMainEntityKey = params.selectedMainEntityKey;
			$scope.selectedMainEntityId = params.selectedMainEntityId;
			$scope.optionsProduct = params.optionsProduct;
			$scope.optionsUoM = params.optionsUoM;
		}

		$scope.create = function () {
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			entityApi.create(entity).then(function (response) {
				if (response.status != 201) {
					messageHub.showAlertError("SalesOrderItem", `Unable to create SalesOrderItem: '${response.message}'`);
					return;
				}
				messageHub.postMessage("entityCreated", response.data);
				$scope.cancel();
				messageHub.showAlertSuccess("SalesOrderItem", "SalesOrderItem successfully created");
			});
		};

		$scope.update = function () {
			let id = $scope.entity.Id;
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			entityApi.update(id, entity).then(function (response) {
				if (response.status != 200) {
					messageHub.showAlertError("SalesOrderItem", `Unable to update SalesOrderItem: '${response.message}'`);
					return;
				}
				messageHub.postMessage("entityUpdated", response.data);
				$scope.cancel();
				messageHub.showAlertSuccess("SalesOrderItem", "SalesOrderItem successfully updated");
			});
		};

		$scope.serviceProduct = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Product/ProductService.ts";
		$scope.serviceUoM = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/UoM/UoMService.ts";

		$scope.$watch('entity.Product', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				entityApi.$http.get($scope.serviceProduct + '/' + newValue).then(function (response) {
					let valueFrom = response.data.UoM;
					entityApi.$http.post("/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/UoM/UoMService.ts/search", {
						$filter: {
							equals: {
								Id: valueFrom
							}
						}
					}).then(function (response) {
						$scope.optionsUoM = response.data.map(e => {
							return {
								value: e.Id,
								text: e.Name
							}
						});
						if ($scope.action !== 'select' && newValue !== oldValue) {
							if ($scope.optionsUoM.length == 1) {
								$scope.entity.UoM = $scope.optionsUoM[0].value;
							} else {
								$scope.entity.UoM = undefined;
							}
						}
					});
				});
			}
		});

		$scope.$watch('entity.Product', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				entityApi.$http.get($scope.serviceProduct + '/' + newValue).then(function (response) {
					let valueFrom = response.data.Price;
					$scope.entity.Price = valueFrom;
				});
			}
		});

		$scope.cancel = function () {
			$scope.entity = {};
			$scope.action = 'select';
			messageHub.closeDialogWindow("SalesOrderItem-details");
		};

	}]);