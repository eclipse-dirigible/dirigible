angular.module('page', ['blimpKit', 'platformView', 'platformLocale', 'EntityService'])
	.config(["EntityServiceProvider", (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CountryService.ts';
	}])
	.controller('PageController', ($scope, $http, Extensions, LocaleService, EntityService) => {
		const Dialogs = new DialogHub();
		const Notifications = new NotificationHub();
		let description = 'Description';
		let propertySuccessfullyCreated = 'Country successfully created';
		let propertySuccessfullyUpdated = 'Country successfully updated';
		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: 'Country Details',
			create: 'Create Country',
			update: 'Update Country'
		};
		$scope.action = 'select';

		LocaleService.onInit(() => {
			description = LocaleService.t('DependsOnScenariosTestProject:defaults.description');
			$scope.formHeaders.select = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadSelect', { name: '$t(DependsOnScenariosTestProject:t.COUNTRY)' });
			$scope.formHeaders.create = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadCreate', { name: '$t(DependsOnScenariosTestProject:t.COUNTRY)' });
			$scope.formHeaders.update = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadUpdate', { name: '$t(DependsOnScenariosTestProject:t.COUNTRY)' });
			propertySuccessfullyCreated = LocaleService.t('DependsOnScenariosTestProject:messages.propertySuccessfullyCreated', { name: '$t(DependsOnScenariosTestProject:t.COUNTRY)' });
			propertySuccessfullyUpdated = LocaleService.t('DependsOnScenariosTestProject:messages.propertySuccessfullyUpdated', { name: '$t(DependsOnScenariosTestProject:t.COUNTRY)' });
		});

		//-----------------Custom Actions-------------------//
		Extensions.getWindows(['DependsOnScenariosTestProject-custom-action']).then((response) => {
			$scope.entityActions = response.data.filter(e => e.perspective === 'Country' && e.view === 'Country' && e.type === 'entity');
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
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.Country.Country.clearDetails', handler: () => {
			$scope.$evalAsync(() => {
				$scope.entity = {};
				$scope.action = 'select';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.Country.Country.entitySelected', handler: (data) => {
			$scope.$evalAsync(() => {
				$scope.entity = data.entity;
				$scope.action = 'select';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.Country.Country.createEntity', handler: (data) => {
			$scope.$evalAsync(() => {
				$scope.entity = {};
				$scope.action = 'create';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.Country.Country.updateEntity', handler: (data) => {
			$scope.$evalAsync(() => {
				$scope.entity = data.entity;
				$scope.action = 'update';
			});
		}});


		//-----------------Events-------------------//

		$scope.create = () => {
			EntityService.create($scope.entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Country.Country.entityCreated', data: response.data });
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Country.Country.clearDetails' , data: response.data });
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.COUNTRY'),
					description: propertySuccessfullyCreated,
					type: 'positive'
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: LocaleService.t('DependsOnScenariosTestProject:t.COUNTRY'),
					message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToCreate', { name: '$t(DependsOnScenariosTestProject:t.COUNTRY)', message: message }),
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.update = () => {
			EntityService.update($scope.entity.Id, $scope.entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Country.Country.entityUpdated', data: response.data });
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Country.Country.clearDetails', data: response.data });
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.COUNTRY'),
					description: propertySuccessfullyUpdated,
					type: 'positive'
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: LocaleService.t('DependsOnScenariosTestProject:t.COUNTRY'),
					message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToCreate', { name: '$t(DependsOnScenariosTestProject:t.COUNTRY)', message: message }),
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.cancel = () => {
			Dialogs.triggerEvent('DependsOnScenariosTestProject.Country.Country.clearDetails');
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
		

		//-----------------Dialogs-------------------//



		//----------------Dropdowns-----------------//


		//----------------Dropdowns-----------------//	
	});