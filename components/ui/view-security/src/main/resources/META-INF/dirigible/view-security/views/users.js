/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const usersView = angular.module('users', ['platformView', 'blimpKit']);
usersView.constant('Dialogs', new DialogHub());
usersView.controller('UsersController', ($scope, $http, Dialogs, ButtonStates) => {
	$scope.listUsers = () => {
		$http.get('/services/security/users').then((response) => {
			$scope.list = response.data;
			$scope.list.forEach(user => user.rolesNames = user.roles.map(role => role['name']).join(','));
			$scope.list.forEach(user => user.rolesIds = user.roles.map(role => role['id']));
		});
	};
	$scope.listUsers();

	$scope.newUser = () => {
		Dialogs.showWindow({
			hasHeader: true,
			id: 'user-create-edit',
			params: { editMode: false },
			closeButton: false,
			maxWidth: '400px',
			maxHeight: '420px'
		});
	};

	Dialogs.addMessageListener({
		topic: 'ide-security.user.create',
		handler: (data) => {
			$http.post('/services/security/users', JSON.stringify(data)).then(() => {
				$scope.listUsers();
				// Dialogs.triggerEvent('ide-security.explorer.refresh');
			}, (response) => {
				console.error(response);
				Dialogs.showAlert({
					title: 'Error while creating user',
					message: response.message ?? 'Please look at the console for more information',
					type: AlertTypes.Error,
					preformatted: false,
				});
			});
			Dialogs.closeWindow();
		}
	});

	$scope.editUser = (user) => {
		$scope.user = {
			id: user.id,
			username: user.username,
			password: user.password,
			tenant: user.tenant.id,
			roles: user.rolesIds
		};
		Dialogs.showWindow({
			hasHeader: true,
			id: 'user-create-edit',
			params: {
				editMode: true,
				user: {
					id: '',
					username: user.username,
					password: user.password,
					tenant: user.tenant.id,
					roles: user.rolesIds
				}
			},
			closeButton: false,
			maxWidth: '400px',
			maxHeight: '420px'
		});
	};

	Dialogs.addMessageListener({
		topic: 'ide-security.user.edit',
		handler: (data) => {
			let user = data;
			//user.username = $scope.user.name;
			$http.put('/services/security/users/' + $scope.user.id, JSON.stringify(user)).then(() => {
				$scope.listUsers();
				// Dialogs.triggerEvent('ide-security.explorer.refresh');
			}, (response) => {
				console.error(response);
				Dialogs.showAlert({
					title: 'Error while updating user',
					message: response.message ?? 'Please look at the console for more information',
					type: AlertTypes.Error,
					preformatted: false,
				});
			});
			Dialogs.closeWindow();
		}
	});

	$scope.deleteUser = (user) => {
		$scope.user = {
			id: user.id
		};
		Dialogs.showDialog({
			title: 'Delete User',
			message: 'Are you sure you want to delete the selected user?',
			buttons: [
				{ id: 'b1', label: 'Delete', state: ButtonStates.Negative },
				{ id: 'b3', label: 'Cancel', state: ButtonStates.Transparent },
			]
		}).then((buttonId) => {
			if (buttonId === 'b1') {
				$http.delete('/services/security/users/' + $scope.user.id)
					.then(() => {
						$scope.listUsers();
						// Dialogs.triggerEvent('ide-security.explorer.refresh');
					}, (response) => {
						console.error(response.data);
						Dialogs.showAlert({
							title: 'Error while deleting user',
							message: response.message ?? 'Please look at the console for more information',
							type: AlertTypes.Error,
							preformatted: false,
						});
					});
			}
		}, (error) => {
			console.error(error);
			Dialogs.showAlert({
				title: 'Delete error',
				message: 'Error while deleting user.\nPlease look at the console for more information.',
				type: AlertTypes.Error,
				preformatted: true,
			});
		});
	};
});