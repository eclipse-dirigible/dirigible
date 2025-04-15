angular.module('page', ['blimpKit', 'platformView', 'EntityService'])
	.config(["EntityServiceProvider", (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Country/CountryService.ts';
	}])
	.controller('PageController', ($scope, $http, Extensions, EntityService) => {
		const Dialogs = new DialogHub();
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

		//-----------------Custom Actions-------------------//
		Extensions.getWindows(['DependsOnScenariosTestProject-custom-action']).then((response) => {
			$scope.entityActions = response.data.filter(e => e.perspective === 'Country' && e.view === 'Country' && e.type === 'entity');
		});

		$scope.triggerEntityAction = (action) => {
			Dialogs.showWindow({
				hasHeader: true,
        		title: action.label,
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
				Dialogs.showAlert({
					title: 'Country',
					message: 'Country successfully created',
					type: AlertTypes.Success
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: 'Country',
					message: `Unable to create Country: '${message}'`,
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.update = () => {
			EntityService.update($scope.entity.Id, $scope.entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Country.Country.entityUpdated', data: response.data });
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Country.Country.clearDetails', data: response.data });
				Dialogs.showAlert({
					title: 'Country',
					message: 'Country successfully updated',
					type: AlertTypes.Success
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: 'Country',
					message: `Unable to create Country: '${message}'`,
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
				title: 'Description',
				message: message,
				type: AlertTypes.Information,
				preformatted: true,
			});
		};
		

		//-----------------Dialogs-------------------//



		//----------------Dropdowns-----------------//


		//----------------Dropdowns-----------------//	
	});