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
const debuggerView = angular.module('debugger', ['ideUI', 'ideView']);

debuggerView.config(["messageHubProvider", function (messageHubProvider) {
	messageHubProvider.eventIdPrefix = 'debugger-view';
}]);

debuggerView.filter('trusted', ['$sce', function ($sce) {
	return function (url) {
		return $sce.trustAsResourceUrl(url);
	};
}]);

debuggerView.controller('DebuggerController', ['$scope', 'messageHub', function ($scope, messageHub) {


	// .factory('$messageHub', [function () {
	// 	let messageHub = new FramesMessageHub();
	// 	let message = function (evtName, data) {
	// 		messageHub.post({ data: data }, 'debugger.' + evtName);
	// 	};

	// 	let on = function (topic, callback) {
	// 		messageHub.subscribe(callback, topic);
	// 	};

	// 	return {
	// 		message: message,
	// 		on: on
	// 	};
	// }])
	// .controller('DebuggerController', ['$scope', '$messageHub', function ($scope, $messageHub) {

	let protocol = window.location.protocol === "http:" ? "ws" : "wss";
	let hostPortIndexOf = window.location.host.indexOf(":");
	let host = hostPortIndexOf > 0 ? window.location.host.substring(0, hostPortIndexOf) : window.location.host;
	let devToolsLocation = "/services/web/dev-tools/js_app.html"; // "devtools://devtools/bundled/js_app.html";
	// TODO: The debug port can be configured
	let debugPort = 8081;
	let debuggerLocation = devToolsLocation + "?" + protocol + "=" + host + ":" + debugPort + "/debug";

	function refreshDebugger(resourcePath) {
		if ($scope.previewUrl == null || $scope.previewUrl == undefined) {
			$scope.previewUrl = debuggerLocation;// + resourcePath;
			let tokenParam = 'refreshToken=' + new Date().getTime();
			$scope.previewUrl += ($scope.previewUrl.indexOf('?') > 0 ? ($scope.previewUrl.endsWith('?') ? tokenParam : ('&' + tokenParam)) : ('?' + tokenParam));
			$scope.$apply();
		}
	}
	messageHub.onFileSelected((fileDescriptor) => {
		let resourcePath = fileDescriptor.path.substring(fileDescriptor.path.indexOf('/', 1));
		refreshDebugger(resourcePath);
	});

}]);