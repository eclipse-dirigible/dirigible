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
const usersView = angular.module('users', ['ideUI', 'ideView']);

usersView.config(["messageHubProvider", function (messageHubProvider) {
	messageHubProvider.eventIdPrefix = 'users-view';
}]);

usersView.controller('UsersController', ['$scope', '$http', 'messageHub', function ($scope, $http, messageHub) {

	$scope.listUsers = function () {
		$http.get('/services/security/users').then(function (response) {
			$scope.list = response.data;
			$scope.list.forEach(user => user.rolesNames = user.roles.map(role => role['name']).join(','));
			$scope.list.forEach(user => user.rolesIds = user.roles.map(role => role['id']));
		});
	}
	$scope.listUsers();

	$scope.newUser = function () {
		messageHub.showDialogWindow(
			"user-create-edit",
			{ editMode: false },
			null,
			false
		);
	};

	messageHub.onDidReceiveMessage(
		'ide-security.user.create',
		function (msg) {
			if (msg.data) {
				$http.post(
					'/services/security/users',
					JSON.stringify(msg.data)
				).then(function () {
					$scope.listUsers();
					messageHub.triggerEvent('ide-security.explorer.refresh', true);
				}, function (response) {
					console.error(response);
					messageHub.showAlertError('Error while creating user', 'Please look at the console for more information');
				});
			}
			messageHub.closeDialogWindow('user-create-edit');
		},
		true
	);

	$scope.editUser = function (user) {
		$scope.user = {
			id: user.id,
			username: user.username,
			password: user.password,
			tenant: user.tenant.id,
			roles: user.rolesIds
		};
		messageHub.showDialogWindow(
			"user-create-edit",
			{
				editMode: true,
				user: {
					id: '',
					username: user.username,
					password: user.password,
					tenant: user.tenant.id,
					roles: user.rolesIds
				}
			},
			null,
			false
		);
	};

	messageHub.onDidReceiveMessage(
		'ide-security.user.edit',
		function (msg) {
			if (msg.data) {
				let user = msg.data;
				//user.username = $scope.user.name;
				$http.put('/services/security/users/' + $scope.user.id, JSON.stringify(user))
					.then(function () {
						$scope.listUsers();
						messageHub.triggerEvent('ide-security.explorer.refresh', true);
					}, function (response) {
						console.error(response);
						messageHub.showAlertError('Error while updating user', 'Please look at the console for more information');
					});
			}
			messageHub.closeDialogWindow('user-create-edit');
		},
		true
	);

	$scope.deleteUser = function (user) {
		$scope.user = {
			id: user.id
		};

		messageHub.showDialog(
			'Delete User',
			'Are you sure you want to delete the selected user?',
			[{
				id: 'btnOK',
				type: 'emphasized',
				label: 'OK',
			},
			{
				id: 'btnCancel',
				type: 'transparent',
				label: 'Cancel',
			}],
			'ide-security.user.delete'
		);
	}

	messageHub.onDidReceiveMessage(
		'ide-security.user.delete',
		function (msg) {
			if (msg.data === 'btnOK' && $scope.user.id) {
				$http.delete('/services/security/users/' + $scope.user.id)
					.then(function () {
						$scope.listUsers();
						messageHub.triggerEvent('ide-security.explorer.refresh', true);
					}, function (response) {
						console.error(response.data);
						messageHub.showAlertError('Error while deleting user', 'Please look at the console for more information');
					});
			}
		},
		true
	);

}]);