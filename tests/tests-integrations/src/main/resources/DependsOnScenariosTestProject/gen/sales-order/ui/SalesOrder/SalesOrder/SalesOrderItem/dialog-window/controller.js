angular.module('page', ['blimpKit', 'platformView', 'platformLocale', 'EntityService'])
	.config(['EntityServiceProvider', (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/SalesOrder/SalesOrderItemService.ts';
	}])
	.controller('PageController', ($scope, $http, ViewParameters, LocaleService, EntityService) => {
		const Dialogs = new DialogHub();
		const Notifications = new NotificationHub();
		let description = 'Description';
		let propertySuccessfullyCreated = 'SalesOrderItem successfully created';
		let propertySuccessfullyUpdated = 'SalesOrderItem successfully updated';
		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: 'SalesOrderItem Details',
			create: 'Create SalesOrderItem',
			update: 'Update SalesOrderItem'
		};
		$scope.action = 'select';

		LocaleService.onInit(() => {
			description = LocaleService.t('DependsOnScenariosTestProject:defaults.description');
			$scope.formHeaders.select = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadSelect', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERITEM)' });
			$scope.formHeaders.create = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadCreate', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERITEM)' });
			$scope.formHeaders.update = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadUpdate', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERITEM)' });
			propertySuccessfullyCreated = LocaleService.t('DependsOnScenariosTestProject:messages.propertySuccessfullyCreated', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERITEM)' });
			propertySuccessfullyUpdated = LocaleService.t('DependsOnScenariosTestProject:messages.propertySuccessfullyUpdated', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERITEM)' });
		});

		let params = ViewParameters.get();
		if (Object.keys(params).length) {
			$scope.action = params.action;
			$scope.entity = params.entity;
			$scope.selectedMainEntityKey = params.selectedMainEntityKey;
			$scope.selectedMainEntityId = params.selectedMainEntityId;
			$scope.optionsProduct = params.optionsProduct;
			$scope.optionsUoM = params.optionsUoM;
		}

		$scope.create = () => {
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			EntityService.create(entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrderItem.entityCreated', data: response.data });
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERITEM'),
					description: propertySuccessfullyCreated,
					type: 'positive'
				});
				$scope.cancel();
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERITEM'),
					message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToCreate', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERITEM)', message: message }),
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.update = () => {
			let id = $scope.entity.Id;
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			EntityService.update(id, entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrderItem.entityUpdated', data: response.data });
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERITEM'),
					description: propertySuccessfullyUpdated,
					type: 'positive'
				});
				$scope.cancel();
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERITEM'),
					message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToUpdate', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERITEM)', message: message }),
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.serviceProduct = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Product/ProductService.ts';
		$scope.serviceUoM = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/UoM/UoMService.ts';

		$scope.$watch('entity.Product', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				$http.get($scope.serviceProduct + '/' + newValue).then((response) => {
					let valueFrom = response.data.UoM;
					$http.post('/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/UoM/UoMService.ts/search', {
						$filter: {
							equals: {
								Id: valueFrom
							}
						}
					}).then((response) => {
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
					}, (error) => {
						console.error(error);
					});
				}, (error) => {
					console.error(error);
				});
			}
		});

		$scope.$watch('entity.Product', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				$http.get($scope.serviceProduct + '/' + newValue).then((response) => {
					let valueFrom = response.data.Price;
					$scope.entity.Price = valueFrom;
				}, (error) => {
					console.error(error);
				});
			}
		});

		$scope.alert = (message) => {
			if (message) Dialogs.showAlert({
				title: description,
				message: message,
				type: AlertTypes.Information,
				preformatted: true,
			});
		};

		$scope.cancel = () => {
			$scope.entity = {};
			$scope.action = 'select';
			Dialogs.closeWindow({ id: 'SalesOrderItem-details' });
		};
	});