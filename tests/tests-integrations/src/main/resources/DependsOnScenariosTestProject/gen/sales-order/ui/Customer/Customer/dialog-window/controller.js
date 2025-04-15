angular.module('page', ['blimpKit', 'platformView', 'EntityService'])
	.config(['EntityServiceProvider', (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts';
	}])
	.controller('PageController', ($scope, $http, ViewParameters, EntityService) => {
		const Dialogs = new DialogHub();
		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: 'Customer Details',
			create: 'Create Customer',
			update: 'Update Customer'
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

		$scope.create = () => {
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			EntityService.create(entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Customer.Customer.entityCreated', data: response.data });
				Dialogs.showAlert({
					title: 'Customer',
					message: 'Customer successfully created',
					type: AlertTypes.Success
				});
				$scope.cancel();
			}, (error) => {
				const message = error.data ? error.data.message : '';
				$scope.$evalAsync(() => {
					$scope.errorMessage = `Unable to create Customer: '${message}'`;
				});
				console.error('EntityService:', error);
			});
		};

		$scope.update = () => {
			let id = $scope.entity.Id;
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			EntityService.update(id, entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Customer.Customer.entityUpdated', data: response.data });
				$scope.cancel();
				Dialogs.showAlert({
					title: 'Customer',
					message: 'Customer successfully updated',
					type: AlertTypes.Success
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				$scope.$evalAsync(() => {
					$scope.errorMessage = `Unable to update Customer: '${message}'`;
				});
				console.error('EntityService:', error);
			});
		};

		$scope.serviceCountry = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CountryService.ts';
		
		$scope.optionsCountry = [];
		
		$http.get('/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CountryService.ts').then((response) => {
			$scope.optionsCountry = response.data.map(e => ({
				value: e.Id,
				text: e.Name
			}));
		}, (error) => {
			console.error(error);
			const message = error.data ? error.data.message : '';
			Dialogs.showAlert({
				title: 'Country',
				message: `Unable to load data: '${message}'`,
				type: AlertTypes.Error
			});
		});
		$scope.serviceCity = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CityService.ts';
		
		$scope.optionsCity = [];
		
		$http.get('/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CityService.ts').then((response) => {
			$scope.optionsCity = response.data.map(e => ({
				value: e.Id,
				text: e.Name
			}));
		}, (error) => {
			console.error(error);
			const message = error.data ? error.data.message : '';
			Dialogs.showAlert({
				title: 'City',
				message: `Unable to load data: '${message}'`,
				type: AlertTypes.Error
			});
		});

		$scope.$watch('entity.Country', (newValue, oldValue) => {
			if (newValue !== undefined && newValue !== null) {
				$http.get($scope.serviceCountry + '/' + newValue).then((response) => {
					let valueFrom = response.data.Id;
					$http.post('/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CityService.ts/search', {
						$filter: {
							equals: {
								Country: valueFrom
							}
						}
					}).then((response) => {
						$scope.optionsCity = response.data.map(e => ({
							value: e.Id,
							text: e.Name
						}));
						if ($scope.action !== 'select' && newValue !== oldValue) {
							if ($scope.optionsCity.length == 1) {
								$scope.entity.City = $scope.optionsCity[0].value;
							} else {
								$scope.entity.City = undefined;
							}
						}
					}, (error) => {
						console.error(error);
					});
				}, (error) => {
					console.error(error);
				});
			}
		});

		$scope.alert = (message) => {
			if (message) Dialogs.showAlert({
				title: 'Description',
				message: message,
				type: AlertTypes.Information,
				preformatted: true,
			});
		};

		$scope.cancel = () => {
			$scope.entity = {};
			$scope.action = 'select';
			Dialogs.closeWindow({ id: 'Customer-details' });
		};

		$scope.clearErrorMessage = () => {
			$scope.errorMessage = null;
		};
	});