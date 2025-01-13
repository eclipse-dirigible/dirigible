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
const logsView = angular.module('logs', ['ideUI', 'ideView']);

logsView.config(["messageHubProvider", function (messageHubProvider) {
	messageHubProvider.eventIdPrefix = 'logs-view';
}]);

logsView.controller('LogsController', ['$scope', '$http', 'messageHub', function ($scope, $http, messageHub) {

	$scope.log = {
		selectedLog: null,
		logsList: []
	};

	$http.get('/services/ide/logs/').then(function (response) {
		$scope.log.logsList = response.data.map(x => ({ text: x, value: x }));
	});

	$scope.logChanged = function () {
		if ($scope.log.selectedLog) {
			$http.get('/services/ide/logs/' + $scope.log.selectedLog).then(function (response) {
				$scope.logContent = response.data;
			});
		} else {
			$scope.logContent = "";
		}
	}

}]);