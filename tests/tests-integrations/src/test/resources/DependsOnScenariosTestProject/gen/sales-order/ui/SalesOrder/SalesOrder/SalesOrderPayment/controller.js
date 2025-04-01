angular.module('page', ["ideUI", "ideView", "entityApi"])
	.config(["messageHubProvider", function (messageHubProvider) {
		messageHubProvider.eventIdPrefix = 'DependsOnScenariosTestProject.SalesOrder.SalesOrderPayment';
	}])
	.config(["entityApiProvider", function (entityApiProvider) {
		entityApiProvider.baseUrl = "/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/SalesOrder/SalesOrderPaymentService.ts";
	}])
	.controller('PageController', ['$scope', '$http', 'messageHub', 'entityApi', 'Extensions', function ($scope, $http, messageHub, entityApi, Extensions) {
		//-----------------Custom Actions-------------------//
		Extensions.get('dialogWindow', 'DependsOnScenariosTestProject-custom-action').then(function (response) {
			$scope.pageActions = response.filter(e => e.perspective === "SalesOrder" && e.view === "SalesOrderPayment" && (e.type === "page" || e.type === undefined));
			$scope.entityActions = response.filter(e => e.perspective === "SalesOrder" && e.view === "SalesOrderPayment" && e.type === "entity");
		});

		$scope.triggerPageAction = function (action) {
			messageHub.showDialogWindow(
				action.id,
				{},
				null,
				true,
				action
			);
		};

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

		function resetPagination() {
			$scope.dataPage = 1;
			$scope.dataCount = 0;
			$scope.dataLimit = 10;
		}
		resetPagination();

		//-----------------Events-------------------//
		messageHub.onDidReceiveMessage("DependsOnScenariosTestProject.SalesOrder.SalesOrder.entitySelected", function (msg) {
			resetPagination();
			$scope.selectedMainEntityId = msg.data.selectedMainEntityId;
			$scope.loadPage($scope.dataPage);
		}, true);

		messageHub.onDidReceiveMessage("DependsOnScenariosTestProject.SalesOrder.SalesOrder.clearDetails", function (msg) {
			$scope.$apply(function () {
				resetPagination();
				$scope.selectedMainEntityId = null;
				$scope.data = null;
			});
		}, true);

		messageHub.onDidReceiveMessage("clearDetails", function (msg) {
			$scope.$apply(function () {
				$scope.entity = {};
				$scope.action = 'select';
			});
		});

		messageHub.onDidReceiveMessage("entityCreated", function (msg) {
			$scope.loadPage($scope.dataPage, $scope.filter);
		});

		messageHub.onDidReceiveMessage("entityUpdated", function (msg) {
			$scope.loadPage($scope.dataPage, $scope.filter);
		});

		messageHub.onDidReceiveMessage("entitySearch", function (msg) {
			resetPagination();
			$scope.filter = msg.data.filter;
			$scope.filterEntity = msg.data.entity;
			$scope.loadPage($scope.dataPage, $scope.filter);
		});
		//-----------------Events-------------------//

		$scope.loadPage = function (pageNumber, filter) {
			let SalesOrder = $scope.selectedMainEntityId;
			$scope.dataPage = pageNumber;
			if (!filter && $scope.filter) {
				filter = $scope.filter;
			}
			if (!filter) {
				filter = {};
			}
			if (!filter.$filter) {
				filter.$filter = {};
			}
			if (!filter.$filter.equals) {
				filter.$filter.equals = {};
			}
			filter.$filter.equals.SalesOrder = SalesOrder;
			entityApi.count(filter).then(function (response) {
				if (response.status != 200) {
					messageHub.showAlertError("SalesOrderPayment", `Unable to count SalesOrderPayment: '${response.message}'`);
					return;
				}
				if (response.data) {
					$scope.dataCount = response.data;
				}
				filter.$offset = (pageNumber - 1) * $scope.dataLimit;
				filter.$limit = $scope.dataLimit;
				entityApi.search(filter).then(function (response) {
					if (response.status != 200) {
						messageHub.showAlertError("SalesOrderPayment", `Unable to list/filter SalesOrderPayment: '${response.message}'`);
						return;
					}
					$scope.data = response.data;
				});
			});
		};

		$scope.selectEntity = function (entity) {
			$scope.selectedEntity = entity;
		};

		$scope.openDetails = function (entity) {
			$scope.selectedEntity = entity;
			messageHub.showDialogWindow("SalesOrderPayment-details", {
				action: "select",
				entity: entity,
				optionsCustomer: $scope.optionsCustomer,
				optionsCustomerPayment: $scope.optionsCustomerPayment,
			});
		};

		$scope.openFilter = function (entity) {
			messageHub.showDialogWindow("SalesOrderPayment-filter", {
				entity: $scope.filterEntity,
				optionsCustomer: $scope.optionsCustomer,
				optionsCustomerPayment: $scope.optionsCustomerPayment,
			});
		};

		$scope.createEntity = function () {
			$scope.selectedEntity = null;
			messageHub.showDialogWindow("SalesOrderPayment-details", {
				action: "create",
				entity: {},
				selectedMainEntityKey: "SalesOrder",
				selectedMainEntityId: $scope.selectedMainEntityId,
				optionsCustomer: $scope.optionsCustomer,
				optionsCustomerPayment: $scope.optionsCustomerPayment,
			}, null, false);
		};

		$scope.updateEntity = function (entity) {
			messageHub.showDialogWindow("SalesOrderPayment-details", {
				action: "update",
				entity: entity,
				selectedMainEntityKey: "SalesOrder",
				selectedMainEntityId: $scope.selectedMainEntityId,
				optionsCustomer: $scope.optionsCustomer,
				optionsCustomerPayment: $scope.optionsCustomerPayment,
			}, null, false);
		};

		$scope.deleteEntity = function (entity) {
			let id = entity.Id;
			messageHub.showDialogAsync(
				'Delete SalesOrderPayment?',
				`Are you sure you want to delete SalesOrderPayment? This action cannot be undone.`,
				[{
					id: "delete-btn-yes",
					type: "emphasized",
					label: "Yes",
				},
				{
					id: "delete-btn-no",
					type: "normal",
					label: "No",
				}],
			).then(function (msg) {
				if (msg.data === "delete-btn-yes") {
					entityApi.delete(id).then(function (response) {
						if (response.status != 204) {
							messageHub.showAlertError("SalesOrderPayment", `Unable to delete SalesOrderPayment: '${response.message}'`);
							return;
						}
						$scope.loadPage($scope.dataPage, $scope.filter);
						messageHub.postMessage("clearDetails");
					});
				}
			});
		};

		//----------------Dropdowns-----------------//
		$scope.optionsCustomer = [];
		$scope.optionsCustomerPayment = [];


		$http.get("/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts").then(function (response) {
			$scope.optionsCustomer = response.data.map(e => {
				return {
					value: e.Id,
					text: e.Name
				}
			});
		});

		$http.get("/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerPaymentService.ts").then(function (response) {
			$scope.optionsCustomerPayment = response.data.map(e => {
				return {
					value: e.Id,
					text: e.Name
				}
			});
		});

		$scope.optionsCustomerValue = function (optionKey) {
			for (let i = 0; i < $scope.optionsCustomer.length; i++) {
				if ($scope.optionsCustomer[i].value === optionKey) {
					return $scope.optionsCustomer[i].text;
				}
			}
			return null;
		};
		$scope.optionsCustomerPaymentValue = function (optionKey) {
			for (let i = 0; i < $scope.optionsCustomerPayment.length; i++) {
				if ($scope.optionsCustomerPayment[i].value === optionKey) {
					return $scope.optionsCustomerPayment[i].text;
				}
			}
			return null;
		};
		//----------------Dropdowns-----------------//

	}]);
