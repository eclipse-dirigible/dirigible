/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('page', ['blimpKit', 'platformView', 'EntityService'])
	.config(["EntityServiceProvider", (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/SalesOrder/SalesOrderService.ts';
	}])
	.controller('PageController', ($scope, $http, Extensions, EntityService) => {
		const Dialogs = new DialogHub();
		const Notifications = new NotificationHub();
		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: 'SalesOrder Details',
			create: 'Create SalesOrder',
			update: 'Update SalesOrder'
		};
		$scope.action = 'select';

		//-----------------Custom Actions-------------------//
		Extensions.getWindows(['DependsOnScenariosTestProject-custom-action']).then((response) => {
			$scope.entityActions = response.data.filter(e => e.perspective === 'SalesOrder' && e.view === 'SalesOrder' && e.type === 'entity');
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
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.clearDetails', handler: () => {
			$scope.$evalAsync(() => {
				$scope.entity = {};
				$scope.optionsCustomer = [];
				$scope.action = 'select';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.entitySelected', handler: (data) => {
			$scope.$evalAsync(() => {
				if (data.entity.Date) {
					data.entity.Date = new Date(data.entity.Date);
				}
				$scope.entity = data.entity;
				$scope.optionsCustomer = data.optionsCustomer;
				$scope.action = 'select';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.createEntity', handler: (data) => {
			$scope.$evalAsync(() => {
				$scope.entity = {};
				$scope.optionsCustomer = data.optionsCustomer;
				$scope.action = 'create';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.updateEntity', handler: (data) => {
			$scope.$evalAsync(() => {
				if (data.entity.Date) {
					data.entity.Date = new Date(data.entity.Date);
				}
				$scope.entity = data.entity;
				$scope.optionsCustomer = data.optionsCustomer;
				$scope.action = 'update';
			});
		}});

		$scope.serviceCustomer = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts';

		//-----------------Events-------------------//

		$scope.create = () => {
			EntityService.create($scope.entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.entityCreated', data: response.data });
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.clearDetails' , data: response.data });
				Notifications.show({
					title: 'SalesOrder',
					description: 'SalesOrder successfully created',
					type: 'positive'
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: 'SalesOrder',
					message: `Unable to create SalesOrder: '${message}'`,
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.update = () => {
			EntityService.update($scope.entity.Id, $scope.entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.entityUpdated', data: response.data });
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.clearDetails', data: response.data });
				Notifications.show({
					title: 'SalesOrder',
					description: 'SalesOrder successfully updated',
					type: 'positive'
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: 'SalesOrder',
					message: `Unable to create SalesOrder: '${message}'`,
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.cancel = () => {
			Dialogs.triggerEvent('DependsOnScenariosTestProject.SalesOrder.SalesOrder.clearDetails');
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
		
		$scope.createCustomer = () => {
			Dialogs.showWindow({
				id: 'Customer-details',
				params: {
					action: 'create',
					entity: {},
				},
				closeButton: false
			});
		};

		//-----------------Dialogs-------------------//



		//----------------Dropdowns-----------------//

		$scope.refreshCustomer = () => {
			$scope.optionsCustomer = [];
			$http.get('/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts').then((response) => {
				$scope.optionsCustomer = response.data.map(e => ({
					value: e.Id,
					text: e.Name
				}));
			}, (error) => {
				console.error(error);
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: 'Customer',
					message: `Unable to load data: '${message}'`,
					type: AlertTypes.Error
				});
			});
		};

		//----------------Dropdowns-----------------//	
	});