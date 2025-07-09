angular.module('page', ['blimpKit', 'platformView', 'platformLocale', 'EntityService'])
	.config(['EntityServiceProvider', (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/SalesOrder/SalesOrderPaymentService.ts';
	}])
	.controller('PageController', ($scope, $http, ViewParameters, LocaleService, EntityService) => {
		const Dialogs = new DialogHub();
		const Notifications = new NotificationHub();
		let description = 'Description';
		let propertySuccessfullyCreated = 'SalesOrderPayment successfully created';
		let propertySuccessfullyUpdated = 'SalesOrderPayment successfully updated';
		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: 'SalesOrderPayment Details',
			create: 'Create SalesOrderPayment',
			update: 'Update SalesOrderPayment'
		};
		$scope.action = 'select';

		LocaleService.onInit(() => {
			description = LocaleService.t('DependsOnScenariosTestProject:defaults.description');
			$scope.formHeaders.select = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadSelect', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)' });
			$scope.formHeaders.create = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadCreate', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)' });
			$scope.formHeaders.update = LocaleService.t('DependsOnScenariosTestProject:defaults.formHeadUpdate', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)' });
			propertySuccessfullyCreated = LocaleService.t('DependsOnScenariosTestProject:messages.propertySuccessfullyCreated', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)' });
			propertySuccessfullyUpdated = LocaleService.t('DependsOnScenariosTestProject:messages.propertySuccessfullyUpdated', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)' });
		});

		let params = ViewParameters.get();
		if (Object.keys(params).length) {
			$scope.action = params.action;
			$scope.entity = params.entity;
			$scope.selectedMainEntityKey = params.selectedMainEntityKey;
			$scope.selectedMainEntityId = params.selectedMainEntityId;
			$scope.optionsCustomer = params.optionsCustomer;
			$scope.optionsCustomerPayment = params.optionsCustomerPayment;
		}

		$scope.create = () => {
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			EntityService.create(entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrderPayment.entityCreated', data: response.data });
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERPAYMENT'),
					description: propertySuccessfullyCreated,
					type: 'positive'
				});
				$scope.cancel();
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERPAYMENT'),
					message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToCreate', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)', message: message }),
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
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.SalesOrder.SalesOrderPayment.entityUpdated', data: response.data });
				Notifications.show({
					title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERPAYMENT'),
					description: propertySuccessfullyUpdated,
					type: 'positive'
				});
				$scope.cancel();
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: LocaleService.t('DependsOnScenariosTestProject:t.SALESORDERPAYMENT'),
					message: LocaleService.t('DependsOnScenariosTestProject:messages.error.unableToUpdate', { name: '$t(DependsOnScenariosTestProject:t.SALESORDERPAYMENT)', message: message }),
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.serviceCustomer = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts';
		$scope.serviceCustomerPayment = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerPaymentService.ts';

		$scope.$watch('entity.SalesOrder', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				$http.get($scope.serviceSalesOrder + '/' + newValue).then((response) => {
					let valueFrom = response.data.Customer;
					$http.post('/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts/search', {
						$filter: {
							equals: {
								Id: valueFrom
							}
						}
					}).then((response) => {
						$scope.optionsCustomer = response.data.map(e => {
							return {
								value: e.Id,
								text: e.Name
							}
						});
						if ($scope.action !== 'select' && newValue !== oldValue) {
							if ($scope.optionsCustomer.length == 1) {
								$scope.entity.Customer = $scope.optionsCustomer[0].value;
							} else {
								$scope.entity.Customer = undefined;
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

		$scope.$watch('entity.Customer', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				$http.get($scope.serviceCustomer + '/' + newValue).then((response) => {
					let valueFrom = response.data.Id;
					$http.post('/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerPaymentService.ts/search', {
						$filter: {
							equals: {
								Customer: valueFrom
							}
						}
					}).then((response) => {
						$scope.optionsCustomerPayment = response.data.map(e => {
							return {
								value: e.Id,
								text: e.Name
							}
						});
						if ($scope.action !== 'select' && newValue !== oldValue) {
							if ($scope.optionsCustomerPayment.length == 1) {
								$scope.entity.CustomerPayment = $scope.optionsCustomerPayment[0].value;
							} else {
								$scope.entity.CustomerPayment = undefined;
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

		$scope.$watch('entity.CustomerPayment', function (newValue, oldValue) {
			if (newValue !== undefined && newValue !== null) {
				$http.get($scope.serviceCustomerPayment + '/' + newValue).then((response) => {
					let valueFrom = response.data.Amount;
					$scope.entity.Amount = valueFrom;
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
			Dialogs.closeWindow({ id: 'SalesOrderPayment-details' });
		};
	});