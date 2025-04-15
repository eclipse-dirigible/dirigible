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
		$scope.optionsCountry = params.optionsCountry;
		$scope.optionsCity = params.optionsCity;
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
		if (entity.Name) {
			filter.$filter.contains.Name = entity.Name;
		}
		if (entity.Country !== undefined) {
			filter.$filter.equals.Country = entity.Country;
		}
		if (entity.City !== undefined) {
			filter.$filter.equals.City = entity.City;
		}
		if (entity.Address) {
			filter.$filter.contains.Address = entity.Address;
		}
		Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Customer.Customer.entitySearch', data: {
			entity: entity,
			filter: filter
		}});
		Dialogs.triggerEvent('DependsOnScenariosTestProject.Customer.Customer.clearDetails');
		$scope.cancel();
	};

	$scope.resetFilter = () => {
		$scope.entity = {};
		$scope.filter();
	};

	$scope.cancel = () => {
		Dialogs.closeWindow({ id: 'Customer-filter' });
	};

	$scope.clearErrorMessage = () => {
		$scope.errorMessage = null;
	};
});