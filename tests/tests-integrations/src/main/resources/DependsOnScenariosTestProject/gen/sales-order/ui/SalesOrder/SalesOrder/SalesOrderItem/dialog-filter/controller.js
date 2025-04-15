angular.module('page', ['blimpKit', 'platformView']).controller('PageController', ($scope, ViewParameters) => {
	const Dialogs = new DialogHub();
	$scope.entity = {};
	$scope.forms = {
		details: {},
	};

	let params = ViewParameters.get();
	if (Object.keys(params).length) {
		$scope.entity = params.entity ?? {};
		$scope.selectedMainEntityKey = params.selectedMainEntityKey;
		$scope.selectedMainEntityId = params.selectedMainEntityId;
		$scope.optionsProduct = params.optionsProduct;
		$scope.optionsUoM = params.optionsUoM;
	}

	$scope.filter = () => {
		let entity = $scope.entity;
		const filter = {
			$filter: {
				equals: {
				},
				notEquals: {
				},
				contains: {
				},
				greaterThan: {
				},
				greaterThanOrEqual: {
				},
				lessThan: {
				},
				lessThanOrEqual: {
				}
			},
		};
		if (entity.Id !== undefined) {
			filter.$filter.equals.Id = entity.Id;
		}
		if (entity.SalesOrder !== undefined) {
			filter.$filter.equals.SalesOrder = entity.SalesOrder;
		}
		if (entity.Product !== undefined) {
			filter.$filter.equals.Product = entity.Product;
		}
		if (entity.UoM !== undefined) {
			filter.$filter.equals.UoM = entity.UoM;
		}
		if (entity.Price !== undefined) {
			filter.$filter.equals.Price = entity.Price;
		}
		Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrderItem.entitySearch', data: {
			entity: entity,
			filter: filter
		}});
		$scope.cancel();
	};

	$scope.resetFilter = () => {
		$scope.entity = {};
		$scope.filter();
	};

	$scope.cancel = () => {
		Dialogs.closeWindow({ id: 'SalesOrderItem-filter' });
	};

	$scope.clearErrorMessage = () => {
		$scope.errorMessage = null;
	};
});