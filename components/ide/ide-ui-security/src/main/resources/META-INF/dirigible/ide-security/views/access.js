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
const accessView = angular.module('access', ['ideUI', 'ideView']);

accessView.config(["messageHubProvider", function (messageHubProvider) {
	messageHubProvider.eventIdPrefix = 'access-view';
}]);

accessView.controller('AccessController', ['$scope', '$http', function ($scope, $http) {

	$http.get('/services/security/access').then(function (response) {
		$scope.list = response.data;
	});

}]);