/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('debugger', ['ngAnimate', 'ngSanitize', 'ui.bootstrap'])
.factory('httpRequestInterceptor', function () {
	var csrfToken = null;
	return {
		request: function (config) {
			config.headers['X-Requested-With'] = 'Fetch';
			config.headers['X-CSRF-Token'] = csrfToken ? csrfToken : 'Fetch';
			return config;
		},
		response: function(response) {
			var token = response.headers()['x-csrf-token']
			if (token) {
				csrfToken = token;
			}
			return response;
		}
	};
})
.config(['$httpProvider', function($httpProvider) {
	$httpProvider.interceptors.push('httpRequestInterceptor');
	//check if response is error. errors currently are non-json formatted and fail too early
	$httpProvider.defaults.transformResponse.unshift(function(data, headersGetter, status){
		if(status>399){
			data = {
				"error": data
			}
			data = JSON.stringify(data);
		}
		return data;
	});
}])
.factory('$messageHub', [function(){
	var messageHub = new FramesMessageHub();	
	var message = function(evtName, data) {
		messageHub.post({data: data}, 'debugger.' + evtName);
	};

	var on = function(topic, callback) {
		messageHub.subscribe(callback, topic);
	};

	return {
		message: message,
		on: on
	};
}])
.controller('DebuggerController', ['$scope', '$http', '$messageHub', function ($scope, $http, $messageHub) {

	var protocol = window.location.protocol === "http:" ? "ws" : "wss";
	var hostPortIndexOf = window.location.host.indexOf(":");
	var host =  hostPortIndexOf > 0 ? window.location.host.substring(0, hostPortIndexOf) : window.location.host;
	var devToolsLocation = "../../../../../../services/v4/web/dev-tools/js_app.html"
	// TODO: The debug port can be configured
	var debugPort = 8081;
	var debuggerLocation = devToolsLocation + "?" + protocol + "=" + host + ":" + debugPort;

	function refreshDebugger(resourcePath) {
		$scope.previewUrl = debuggerLocation + resourcePath;
		var tokenParam = 'refreshToken=' + new Date().getTime();
		$scope.previewUrl += ($scope.previewUrl.indexOf('?') > 0 ? ($scope.previewUrl.endsWith('?') ? tokenParam : ('&' + tokenParam)) : ('?' + tokenParam));
		$scope.$apply();
	}
	$messageHub.on('workspace.file.selected', function(msg) {
		var resourcePath = msg.data.path.substring(msg.data.path.indexOf('/', 1));
		refreshDebugger(resourcePath);
	}.bind(this));

	$messageHub.on('debugger.refresh', function(msg) {
		refreshDebugger(msg.data.resourcePath);
	});
}]);

