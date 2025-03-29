angular.module('page', ["ideUI", "ideView", "entityApi"])
	.config(["messageHubProvider", function (messageHubProvider) {
		messageHubProvider.eventIdPrefix = 'sales-order-app.Customer.Customer';
	}])
	.config(["entityApiProvider", function (entityApiProvider) {
		entityApiProvider.baseUrl = "/services/ts/sales-order-app/gen/sales-order/api/Customer/CustomerService.ts";
	}])
	.controller('PageController', ['$scope',  '$http', 'messageHub', 'ViewParameters', 'entityApi', function ($scope,  $http, messageHub, ViewParameters, entityApi) {

		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: "Customer Details",
			create: "Create Customer",
			update: "Update Customer"
		};
		$scope.action = 'select';

		let params = ViewParameters.get();
		if (Object.keys(params).length) {
			$scope.action = params.action;
			$scope.entity = params.entity;
			$scope.selectedMainEntityKey = params.selectedMainEntityKey;
			$scope.selectedMainEntityId = params.selectedMainEntityId;
			$scope.optionsCountry = params.optionsCountry;
			$scope.optionsCity = params.optionsCity;
		}

		$scope.create = function () {
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			entityApi.create(entity).then(function (response) {
				if (response.status != 201) {
					$scope.errorMessage = `Unable to create Customer: '${response.message}'`;
					return;
				}
				messageHub.postMessage("entityCreated", response.data);
				$scope.cancel();
				messageHub.showAlertSuccess("Customer", "Customer successfully created");
			});
		};

		$scope.update = function () {
			let id = $scope.entity.Id;
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			entityApi.update(id, entity).then(function (response) {
				if (response.status != 200) {
					$scope.errorMessage = `Unable to update Customer: '${response.message}'`;
					return;
				}
				messageHub.postMessage("entityUpdated", response.data);
				$scope.cancel();
				messageHub.showAlertSuccess("Customer", "Customer successfully updated");
			});
		};

		$scope.serviceCountry = "/services/ts/sales-order-app/gen/sales-order/api/Country/CountryService.ts";
		
		$scope.optionsCountry = [];
		
		$http.get("/services/ts/sales-order-app/gen/sales-order/api/Country/CountryService.ts").then(function (response) {
			$scope.optionsCountry = response.data.map(e => {
				return {
					value: e.Id,
					text: e.Name
				}
			});
		});
		$scope.serviceCity = "/services/ts/sales-order-app/gen/sales-order/api/Country/CityService.ts";
		
		$scope.optionsCity = [];
		
		$http.get("/services/ts/sales-order-app/gen/sales-order/api/Country/CityService.ts").then(function (response) {
			$scope.optionsCity = response.data.map(e => {
				return {
					value: e.Id,
					text: e.Name
				}
			});
		});

		$scope.$watch('entity.Country', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				entityApi.$http.get($scope.serviceCountry + '/' + newValue).then(function (response) {
					let valueFrom = response.data.Id;
					entityApi.$http.post("/services/ts/sales-order-app/gen/sales-order/api/Country/CityService.ts/search", {
						$filter: {
							equals: {
								Country: valueFrom
							}
						}
					}).then(function (response) {
						$scope.optionsCity = response.data.map(e => {
							return {
								value: e.Id,
								text: e.Name
							}
						});
						if ($scope.action !== 'select' && newValue !== oldValue) {
							if ($scope.optionsCity.length == 1) {
								$scope.entity.City = $scope.optionsCity[0].value;
							} else {
								$scope.entity.City = undefined;
							}
						}
					});
				});
			}
		});

		$scope.cancel = function () {
			$scope.entity = {};
			$scope.action = 'select';
			messageHub.closeDialogWindow("Customer-details");
		};

		$scope.clearErrorMessage = function () {
			$scope.errorMessage = null;
		};

	}]);