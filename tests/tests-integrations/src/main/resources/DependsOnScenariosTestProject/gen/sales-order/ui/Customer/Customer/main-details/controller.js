angular.module('page', ["ideUI", "ideView", "entityApi"])
	.config(["messageHubProvider", function (messageHubProvider) {
		messageHubProvider.eventIdPrefix = 'DependsOnScenariosTestProject.Customer.Customer';
	}])
	.config(["entityApiProvider", function (entityApiProvider) {
		entityApiProvider.baseUrl = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts";
	}])
	.controller('PageController', ['$scope',  '$http', 'Extensions', 'messageHub', 'entityApi', function ($scope,  $http, Extensions, messageHub, entityApi) {

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

		//-----------------Custom Actions-------------------//
		Extensions.get('dialogWindow', 'DependsOnScenariosTestProject-custom-action').then(function (response) {
			$scope.entityActions = response.filter(e => e.perspective === "Customer" && e.view === "Customer" && e.type === "entity");
		});

		$scope.triggerEntityAction = function (action) {
			messageHub.showDialogWindow(
				action.id,
				{
					id: $scope.entity.Id
				},
				null,
				true,
				action
			);
		};
		//-----------------Custom Actions-------------------//

		//-----------------Events-------------------//
		messageHub.onDidReceiveMessage("clearDetails", function (msg) {
			$scope.$apply(function () {
				$scope.entity = {};
				$scope.optionsCountry = [];
				$scope.optionsCity = [];
				$scope.action = 'select';
			});
		});

		messageHub.onDidReceiveMessage("entitySelected", function (msg) {
			$scope.$apply(function () {
				$scope.entity = msg.data.entity;
				$scope.optionsCountry = msg.data.optionsCountry;
				$scope.optionsCity = msg.data.optionsCity;
				$scope.action = 'select';
			});
		});

		messageHub.onDidReceiveMessage("createEntity", function (msg) {
			$scope.$apply(function () {
				$scope.entity = {};
				$scope.optionsCountry = msg.data.optionsCountry;
				$scope.optionsCity = msg.data.optionsCity;
				$scope.action = 'create';
			});
		});

		messageHub.onDidReceiveMessage("updateEntity", function (msg) {
			$scope.$apply(function () {
				$scope.entity = msg.data.entity;
				$scope.optionsCountry = msg.data.optionsCountry;
				$scope.optionsCity = msg.data.optionsCity;
				$scope.action = 'update';
			});
		});

		$scope.serviceCountry = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CountryService.ts";
		$scope.serviceCity = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CityService.ts";


		$scope.$watch('entity.Country', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				entityApi.$http.get($scope.serviceCountry + '/' + newValue).then(function (response) {
					let valueFrom = response.data.Id;
					entityApi.$http.post("/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CityService.ts/search", {
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
		//-----------------Events-------------------//

		$scope.create = function () {
			entityApi.create($scope.entity).then(function (response) {
				if (response.status != 201) {
					messageHub.showAlertError("Customer", `Unable to create Customer: '${response.message}'`);
					return;
				}
				messageHub.postMessage("entityCreated", response.data);
				messageHub.postMessage("clearDetails", response.data);
				messageHub.showAlertSuccess("Customer", "Customer successfully created");
			});
		};

		$scope.update = function () {
			entityApi.update($scope.entity.Id, $scope.entity).then(function (response) {
				if (response.status != 200) {
					messageHub.showAlertError("Customer", `Unable to update Customer: '${response.message}'`);
					return;
				}
				messageHub.postMessage("entityUpdated", response.data);
				messageHub.postMessage("clearDetails", response.data);
				messageHub.showAlertSuccess("Customer", "Customer successfully updated");
			});
		};

		$scope.cancel = function () {
			messageHub.postMessage("clearDetails");
		};
		
		//-----------------Dialogs-------------------//
		
		$scope.createCountry = function () {
			messageHub.showDialogWindow("Country-details", {
				action: "create",
				entity: {},
			}, null, false);
		};
		$scope.createCity = function () {
			messageHub.showDialogWindow("City-details", {
				action: "create",
				entity: {},
			}, null, false);
		};

		//-----------------Dialogs-------------------//



		//----------------Dropdowns-----------------//

		$scope.refreshCountry = function () {
			$scope.optionsCountry = [];
			$http.get("/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CountryService.ts").then(function (response) {
				$scope.optionsCountry = response.data.map(e => {
					return {
						value: e.Id,
						text: e.Name
					}
				});
			});
		};
		$scope.refreshCity = function () {
			$scope.optionsCity = [];
			$http.get("/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CityService.ts").then(function (response) {
				$scope.optionsCity = response.data.map(e => {
					return {
						value: e.Id,
						text: e.Name
					}
				});
			});
		};

		//----------------Dropdowns-----------------//	
		

	}]);