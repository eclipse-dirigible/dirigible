angular.module('page', ['blimpKit', 'platformView', 'platformLocale', 'EntityService'])
	.config(["EntityServiceProvider", (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Product/ProductService.ts';
	}])
	.controller('PageController', ($scope, $http, Extensions, LocaleService, EntityService) => {
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

		//-----------------Custom Actions-------------------//
		Extensions.getWindows(['DependsOnScenariosTestProject-custom-action']).then((response) => {
			$scope.entityActions = response.data.filter(e => e.perspective === 'Product' && e.view === 'Product' && e.type === 'entity');
		});

		$scope.triggerEntityAction = (action) => {
			Dialogs.showWindow({
				hasHeader: true,
        		title: LocaleService.t(action.translation.key, action.translation.options, action.label),
				path: action.path,
				params: {
					id: $scope.entity.Id
				},
				closeButton: true
			});
		};
		//-----------------Custom Actions-------------------//

		//-----------------Events-------------------//
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.Product.Product.clearDetails', handler: () => {
			$scope.$evalAsync(() => {
				$scope.entity = {};
				$scope.optionsUoM = [];
				$scope.action = 'select';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.Product.Product.entitySelected', handler: (data) => {
			$scope.$evalAsync(() => {
				$scope.entity = data.entity;
				$scope.optionsUoM = data.optionsUoM;
				$scope.action = 'select';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.Product.Product.createEntity', handler: (data) => {
			$scope.$evalAsync(() => {
				$scope.entity = {};
				$scope.optionsUoM = data.optionsUoM;
				$scope.action = 'create';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.Product.Product.updateEntity', handler: (data) => {
			$scope.$evalAsync(() => {
				$scope.entity = data.entity;
				$scope.optionsUoM = data.optionsUoM;
				$scope.action = 'update';
			});
		}});

		$scope.serviceUoM = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/UoM/UoMService.ts';

		//-----------------Events-------------------//

		$scope.create = () => {
			EntityService.create($scope.entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Product.Product.entityCreated', data: response.data });
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Product.Product.clearDetails' , data: response.data });
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.PRODUCT'),
					description: propertySuccessfullyCreated,
					type: 'positive'
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: LocaleService.t('DependsOnScenariosTestProject:t.PRODUCT'),
					message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToCreate', { name: '$t(DependsOnScenariosTestProject:t.PRODUCT)', message: message }),
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.update = () => {
			EntityService.update($scope.entity.Id, $scope.entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Product.Product.entityUpdated', data: response.data });
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Product.Product.clearDetails', data: response.data });
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.PRODUCT'),
					description: propertySuccessfullyUpdated,
					type: 'positive'
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: LocaleService.t('DependsOnScenariosTestProject:t.PRODUCT'),
					message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToCreate', { name: '$t(DependsOnScenariosTestProject:t.PRODUCT)', message: message }),
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.cancel = () => {
			Dialogs.triggerEvent('DependsOnScenariosTestProject.Product.Product.clearDetails');
		};
		
		//-----------------Dialogs-------------------//
		$scope.alert = (message) => {
			if (message) Dialogs.showAlert({
				title: description,
				message: message,
				type: AlertTypes.Information,
				preformatted: true,
			});
		};
		
		$scope.createUoM = () => {
			Dialogs.showWindow({
				id: 'UoM-details',
				params: {
					action: 'create',
					entity: {},
				},
				closeButton: false
			});
		};

		//-----------------Dialogs-------------------//



		//----------------Dropdowns-----------------//

		$scope.refreshUoM = () => {
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
		};

		//----------------Dropdowns-----------------//	
	});