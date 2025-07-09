angular.module('page', ['blimpKit', 'platformView', 'platformLocale', 'EntityService'])
	.config(['EntityServiceProvider', (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/SalesOrder/SalesOrderPaymentService.ts';
	}])
	.controller('PageController', ($scope, $http, EntityService, Extensions, LocaleService, ButtonStates) => {
		const Dialogs = new DialogHub();
		let translated = {
			yes: 'Yes',
			no: 'No',
			deleteConfirm: 'Are you sure you want to delete SalesOrderPayment? This action cannot be undone.',
			deleteTitle: 'Delete SalesOrderPayment?'
		};

		LocaleService.onInit(() => {
			translated.yes = LocaleService.t('DependsOnScenariosTestProject:defaults.yes');
			translated.no = LocaleService.t('DependsOnScenariosTestProject:defaults.no');
			translated.deleteTitle = LocaleService.t('DependsOnScenariosTestProject:defaults.deleteTitle', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)' });
			translated.deleteConfirm = LocaleService.t('DependsOnScenariosTestProject:messages.deleteConfirm', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)' });
		});
		//-----------------Custom Actions-------------------//
		Extensions.getWindows(['DependsOnScenariosTestProject-custom-action']).then((response) => {
			$scope.pageActions = response.data.filter(e => e.perspective === 'SalesOrder' && e.view === 'SalesOrderPayment' && (e.type === 'page' || e.type === undefined));
			$scope.entityActions = response.data.filter(e => e.perspective === 'SalesOrder' && e.view === 'SalesOrderPayment' && e.type === 'entity');
		});

		$scope.triggerPageAction = (action) => {
			Dialogs.showWindow({
				hasHeader: true,
        		title: LocaleService.t(action.translation.key, action.translation.options, action.label),
				path: action.path,
				params: {
					selectedMainEntityKey: 'SalesOrder',
					selectedMainEntityId: $scope.selectedMainEntityId,
				},
				maxWidth: action.maxWidth,
				maxHeight: action.maxHeight,
				closeButton: true
			});
		};

		$scope.triggerEntityAction = (action) => {
			Dialogs.showWindow({
				hasHeader: true,
        		title: LocaleService.t(action.translation.key, action.translation.options, action.label),
				path: action.path,
				params: {
					id: $scope.entity.Id,
					selectedMainEntityKey: 'SalesOrder',
					selectedMainEntityId: $scope.selectedMainEntityId,
				},
				closeButton: true
			});
		};
		//-----------------Custom Actions-------------------//

		function resetPagination() {
			$scope.dataPage = 1;
			$scope.dataCount = 0;
			$scope.dataLimit = 10;
		}
		resetPagination();

		//-----------------Events-------------------//
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.entitySelected', handler: (data) => {
			resetPagination();
			$scope.selectedMainEntityId = data.selectedMainEntityId;
			$scope.loadPage($scope.dataPage);
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrder.clearDetails', handler: () => {
			$scope.$evalAsync(() => {
				resetPagination();
				$scope.selectedMainEntityId = null;
				$scope.data = null;
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrderPayment.clearDetails', handler: () => {
			$scope.$evalAsync(() => {
				$scope.entity = {};
				$scope.action = 'select';
			});
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrderPayment.entityCreated', handler: () => {
			$scope.loadPage($scope.dataPage, $scope.filter);
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrderPayment.entityUpdated', handler: () => {
			$scope.loadPage($scope.dataPage, $scope.filter);
		}});
		Dialogs.addMessageListener({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrderPayment.entitySearch', handler: (data) => {
			resetPagination();
			$scope.filter = data.filter;
			$scope.filterEntity = data.entity;
			$scope.loadPage($scope.dataPage, $scope.filter);
		}});
		//-----------------Events-------------------//

		$scope.loadPage = (pageNumber, filter) => {
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
			EntityService.count(filter).then((resp) => {
				if (resp.data) {
					$scope.dataCount = resp.data.count;
				}
				filter.$offset = (pageNumber - 1) * $scope.dataLimit;
				filter.$limit = $scope.dataLimit;
				EntityService.search(filter).then((response) => {
					$scope.data = response.data;
				}, (error) => {
					const message = error.data ? error.data.message : '';
					Dialogs.showAlert({
						title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERPAYMENT'),
						message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToLF', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)', message: message }),
						type: AlertTypes.Error
					});
					console.error('EntityService:', error);
				});
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERPAYMENT'),
					message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToCount', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)', message: message }),
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.selectEntity = (entity) => {
			$scope.selectedEntity = entity;
		};

		$scope.openDetails = (entity) => {
			$scope.selectedEntity = entity;
			Dialogs.showWindow({
				id: 'SalesOrderPayment-details',
				params: {
					action: 'select',
					entity: entity,
					optionsCustomer: $scope.optionsCustomer,
					optionsCustomerPayment: $scope.optionsCustomerPayment,
				},
			});
		};

		$scope.openFilter = () => {
			Dialogs.showWindow({
				id: 'SalesOrderPayment-filter',
				params: {
					entity: $scope.filterEntity,
					optionsCustomer: $scope.optionsCustomer,
					optionsCustomerPayment: $scope.optionsCustomerPayment,
				},
			});
		};

		$scope.createEntity = () => {
			$scope.selectedEntity = null;
			Dialogs.showWindow({
				id: 'SalesOrderPayment-details',
				params: {
					action: 'create',
					entity: {
						'SalesOrder': $scope.selectedMainEntityId
					},
					selectedMainEntityKey: 'SalesOrder',
					selectedMainEntityId: $scope.selectedMainEntityId,
					optionsCustomer: $scope.optionsCustomer,
					optionsCustomerPayment: $scope.optionsCustomerPayment,
				},
				closeButton: false
			});
		};

		$scope.updateEntity = (entity) => {
			Dialogs.showWindow({
				id: 'SalesOrderPayment-details',
				params: {
					action: 'update',
					entity: entity,
					selectedMainEntityKey: 'SalesOrder',
					selectedMainEntityId: $scope.selectedMainEntityId,
					optionsCustomer: $scope.optionsCustomer,
					optionsCustomerPayment: $scope.optionsCustomerPayment,
			},
				closeButton: false
			});
		};

		$scope.deleteEntity = (entity) => {
			let id = entity.Id;
			Dialogs.showDialog({
				title: translated.deleteTitle,
				message: translated.deleteConfirm,
				buttons: [{
					id: 'delete-btn-yes',
					state: ButtonStates.Emphasized,
					label: translated.yes,
				}, {
					id: 'delete-btn-no',
					label: translated.no,
				}],
				closeButton: false
			}).then((buttonId) => {
				if (buttonId === 'delete-btn-yes') {
					EntityService.delete(id).then(() => {
						$scope.loadPage($scope.dataPage, $scope.filter);
						Dialogs.triggerEvent('DependsOnScenariosTestProject.SalesOrder.SalesOrderPayment.clearDetails');
					}, (error) => {
						const message = error.data ? error.data.message : '';
						Dialogs.showAlert({
							title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERPAYMENT'),
							message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToDelete', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)', message: message }),
							type: AlertTypes.Error,
						});
						console.error('EntityService:', error);
					});
				}
			});
		};

		//----------------Dropdowns-----------------//
		$scope.optionsCustomer = [];
		$scope.optionsCustomerPayment = [];


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
				message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToLoad', { message: message }),
				type: AlertTypes.Error
			});
		});

		$http.get('/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerPaymentService.ts').then((response) => {
			$scope.optionsCustomerPayment = response.data.map(e => ({
				value: e.Id,
				text: e.Name
			}));
		}, (error) => {
			console.error(error);
			const message = error.data ? error.data.message : '';
			Dialogs.showAlert({
				title: 'CustomerPayment',
				message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToLoad', { message: message }),
				type: AlertTypes.Error
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
	});
