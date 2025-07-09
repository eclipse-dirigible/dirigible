angular.module('page', ['blimpKit', 'platformView', 'platformLocale', 'EntityService'])
	.config(['EntityServiceProvider', (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Product/ProductService.ts';
	}])
	.controller('PageController', ($scope, $http, ViewParameters, LocaleService, EntityService) => {
		const Dialogs = new DialogHub();
		const Notifications = new NotificationHub();
		let description = 'Description';
		let propertySuccessfullyCreated = 'Product successfully created';
		let propertySuccessfullyUpdated = 'Product successfully updated';
		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: 'Product Details',
			create: 'Create Product',
			update: 'Update Product'
		};
		$scope.action = 'select';

		LocaleService.onInit(() => {
			description = LocaleService.t('DependsOnScenariosTestProject:defaults.description');
			$scope.formHeaders.select = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadSelect', { name: '$t(DependsOnScenariosTestProject:t.PRODUCT)' });
			$scope.formHeaders.create = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadCreate', { name: '$t(DependsOnScenariosTestProject:t.PRODUCT)' });
			$scope.formHeaders.update = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadUpdate', { name: '$t(DependsOnScenariosTestProject:t.PRODUCT)' });
			propertySuccessfullyCreated = LocaleService.t('DependsOnScenariosTestProject:messages.propertySuccessfullyCreated', { name: '$t(DependsOnScenariosTestProject:t.PRODUCT)' });
			propertySuccessfullyUpdated = LocaleService.t('DependsOnScenariosTestProject:messages.propertySuccessfullyUpdated', { name: '$t(DependsOnScenariosTestProject:t.PRODUCT)' });
		});

		let params = ViewParameters.get();
		if (Object.keys(params).length) {
			$scope.action = params.action;
			$scope.entity = params.entity;
			$scope.selectedMainEntityKey = params.selectedMainEntityKey;
			$scope.selectedMainEntityId = params.selectedMainEntityId;
			$scope.optionsUoM = params.optionsUoM;
		}

		$scope.create = () => {
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			EntityService.create(entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Product.Product.entityCreated', data: response.data });
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.PRODUCT'),
					description: propertySuccessfullyCreated,
					type: 'positive'
				});
				$scope.cancel();
			}, (error) => {
				const message = error.data ? error.data.message : '';
				$scope.$evalAsync(() => {
					$scope.errorMessage = LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToCreate', { name: '$t(DependsOnScenariosTestProject:t.PRODUCT)', message: message });
				});
				console.error('EntityService:', error);
			});
		};

		$scope.update = () => {
			let id = $scope.entity.Id;
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			EntityService.update(id, entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Product.Product.entityUpdated', data: response.data });
				$scope.cancel();
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.PRODUCT'),
					description: propertySuccessfullyUpdated,
					type: 'positive'
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				$scope.$evalAsync(() => {
					$scope.errorMessage = LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToUpdate', { name: '$t(DependsOnScenariosTestProject:t.PRODUCT)', message: message });
				});
				console.error('EntityService:', error);
			});
		};

		$scope.serviceUoM = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/UoM/UoMService.ts';
		
		$scope.optionsUoM = [];
		
		$http.get('/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/UoM/UoMService.ts').then((response) => {
			$scope.optionsUoM = response.data.map(e => ({
				value: e.Id,
				text: e.Name
			}));
		}, (error) => {
			console.error(error);
			const message = error.data ? error.data.message : '';
			Dialogs.showAlert({
				title: 'UoM',
				message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToLoad', { message: message }),
				type: AlertTypes.Error
			});
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
			Dialogs.closeWindow({ id: 'Product-details' });
		};

		$scope.clearErrorMessage = () => {
			$scope.errorMessage = null;
		};
	});