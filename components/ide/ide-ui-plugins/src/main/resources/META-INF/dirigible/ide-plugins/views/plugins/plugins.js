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
const pluginsView = angular.module('plugins', ['ideUI', 'ideView']);

pluginsView.config(["messageHubProvider", function (messageHubProvider) {
	messageHubProvider.eventIdPrefix = 'plugins-view';
}]);

pluginsView.controller('PluginsController', ['$scope', '$http', 'messageHub', function ($scope, $http, messageHub) {

	let pluginsApi = '/services/js/ide-plugins/views/plugins/plugins-service.js';

	function loadPlugins() {
		$http.get(pluginsApi)
			.then(function (data) {
				$scope.depots = data.data;
			});
	}

	loadPlugins();
}]);