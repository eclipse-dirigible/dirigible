angular.module('page', ["ideUI", "ideView", "entityApi"])
	.config(["messageHubProvider", function (messageHubProvider) {
		messageHubProvider.eventIdPrefix = 'DependsOnScenariosTestProject.SalesOrder.SalesOrderPayment';
	}])
	.config(["entityApiProvider", function (entityApiProvider) {
		entityApiProvider.baseUrl = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/SalesOrder/SalesOrderPaymentService.ts";
	}])
	.controller('PageController', ['$scope', 'messageHub', 'ViewParameters', 'entityApi', function ($scope, messageHub, ViewParameters, entityApi) {

		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: "SalesOrderPayment Details",
			create: "Create SalesOrderPayment",
			update: "Update SalesOrderPayment"
		};
		$scope.action = 'select';

		let params = ViewParameters.get();
		if (Object.keys(params).length) {
			$scope.action = params.action;
			$scope.entity = params.entity;
			$scope.selectedMainEntityKey = params.selectedMainEntityKey;
			$scope.selectedMainEntityId = params.selectedMainEntityId;
			$scope.optionsCustomer = params.optionsCustomer;
			$scope.optionsCustomerPayment = params.optionsCustomerPayment;
		}

		$scope.create = function () {
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			entityApi.create(entity).then(function (response) {
				if (response.status != 201) {
					messageHub.showAlertError("SalesOrderPayment", `Unable to create SalesOrderPayment: '${response.message}'`);
					return;
				}
				messageHub.postMessage("entityCreated", response.data);
				$scope.cancel();
				messageHub.showAlertSuccess("SalesOrderPayment", "SalesOrderPayment successfully created");
			});
		};

		$scope.update = function () {
			let id = $scope.entity.Id;
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			entityApi.update(id, entity).then(function (response) {
				if (response.status != 200) {
					messageHub.showAlertError("SalesOrderPayment", `Unable to update SalesOrderPayment: '${response.message}'`);
					return;
				}
				messageHub.postMessage("entityUpdated", response.data);
				$scope.cancel();
				messageHub.showAlertSuccess("SalesOrderPayment", "SalesOrderPayment successfully updated");
			});
		};

		$scope.serviceCustomer = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts";
		$scope.serviceCustomerPayment = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerPaymentService.ts";

		$scope.$watch('entity.SalesOrder', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				entityApi.$http.get($scope.serviceSalesOrder + '/' + newValue).then(function (response) {
					let valueFrom = response.data.Customer;
					entityApi.$http.post("/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts/search", {
						$filter: {
							equals: {
								Id: valueFrom
							}
						}
					}).then(function (response) {
						$scope.optionsCustomer = response.data.map(e => {
							return {
								value: e.Id,
								text: e.Name
							}
						});
						if ($scope.action !== 'select' && newValue !== oldValue) {
							if ($scope.optionsCustomer.length == 1) {
								$scope.entity.Customer = $scope.optionsCustomer[0].value;
							} else {
								$scope.entity.Customer = undefined;
							}
						}
					});
				});
			}
		});

		$scope.$watch('entity.Customer', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				entityApi.$http.get($scope.serviceCustomer + '/' + newValue).then(function (response) {
					let valueFrom = response.data.Id;
					entityApi.$http.post("/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerPaymentService.ts/search", {
						$filter: {
							equals: {
								Customer: valueFrom
							}
						}
					}).then(function (response) {
						$scope.optionsCustomerPayment = response.data.map(e => {
							return {
								value: e.Id,
								text: e.Name
							}
						});
						if ($scope.action !== 'select' && newValue !== oldValue) {
							if ($scope.optionsCustomerPayment.length == 1) {
								$scope.entity.CustomerPayment = $scope.optionsCustomerPayment[0].value;
							} else {
								$scope.entity.CustomerPayment = undefined;
							}
						}
					});
				});
			}
		});

		$scope.$watch('entity.CustomerPayment', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				entityApi.$http.get($scope.serviceCustomerPayment + '/' + newValue).then(function (response) {
					let valueFrom = response.data.Amount;
					$scope.entity.Amount = valueFrom;
				});
			}
		});

		$scope.cancel = function () {
			$scope.entity = {};
			$scope.action = 'select';
			messageHub.closeDialogWindow("SalesOrderPayment-details");
		};

	}]);