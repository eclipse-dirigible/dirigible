angular.module('page', ['blimpKit', 'platformView', 'EntityService'])
	.config(['EntityServiceProvider', (EntityServiceProvider) => {
		EntityServiceProvider.baseUrl = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerPaymentService.ts';
	}])
	.controller('PageController', ($scope, $http, ViewParameters, EntityService) => {
		const Dialogs = new DialogHub();
		$scope.entity = {};
		$scope.forms = {
			details: {},
		};
		$scope.formHeaders = {
			select: 'CustomerPayment Details',
			create: 'Create CustomerPayment',
			update: 'Update CustomerPayment'
		};
		$scope.action = 'select';

		let params = ViewParameters.get();
		if (Object.keys(params).length) {
			$scope.action = params.action;
			$scope.entity = params.entity;
			$scope.selectedMainEntityKey = params.selectedMainEntityKey;
			$scope.selectedMainEntityId = params.selectedMainEntityId;
			$scope.optionsCustomer = params.optionsCustomer;
		}

		$scope.create = () => {
			let entity = $scope.entity;
			entity[$scope.selectedMainEntityKey] = $scope.selectedMainEntityId;
			EntityService.create(entity).then((response) => {
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Customer.CustomerPayment.entityCreated', data: response.data });
				Dialogs.showAlert({
					title: 'CustomerPayment',
					message: 'CustomerPayment successfully created',
					type: AlertTypes.Success
				});
				$scope.cancel();
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: 'CustomerPayment',
					message: `Unable to create CustomerPayment: '${message}'`,
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
				Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Customer.CustomerPayment.entityUpdated', data: response.data });
				Dialogs.showAlert({
					title: 'CustomerPayment',
					message: 'CustomerPayment successfully updated',
					type: AlertTypes.Success
				});
				$scope.cancel();
			}, (error) => {
				const message = error.data ? error.data.message : '';
				Dialogs.showAlert({
					title: 'CustomerPayment',
					message: `Unable to update CustomerPayment: '${message}'`,
					type: AlertTypes.Error
				});
				console.error('EntityService:', error);
			});
		};

		$scope.serviceCustomer = '/services/ts/DependsOnScenariosTestProject/gen/sales-order/api/Customer/CustomerService.ts';

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
			Dialogs.closeWindow({ id: 'CustomerPayment-details' });
		};
	});