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
const rolesView = angular.module('roles', ['ideUI', 'ideView']);

rolesView.config(["messageHubProvider", function (messageHubProvider) {
	messageHubProvider.eventIdPrefix = 'roles-view';
}]);

rolesView.controller('RolesController', ['$scope', '$http', function ($scope, $http) {

	$http.get('/services/security/roles').then(function (response) {
		$scope.list = response.data;
	});

}]);
