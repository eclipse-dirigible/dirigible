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
angular.module('git.config', []).constant('GIT_SVC_URL', '/services/ide/git');

/**
 * Git Service API delegate
 */
let GitService = function ($http, gitServiceUrl) {
	this.gitServiceUrl = gitServiceUrl;
	this.$http = $http;
}

GitService.prototype.history = function (workspace, project, file) {
	let url = new UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path("history").build();
	if (file) {
		url += "?path=" + file;
	}
	return this.$http.get(url);
}

let historyApp = angular.module('historyApp', ['ideUI', 'ideView', 'git.config'])

	.config(['$httpProvider', function ($httpProvider) {
		//check if response is error. errors currently are non-json formatted and fail too early
		$httpProvider.defaults.transformResponse.unshift(function (data, headersGetter, status) {
			if (status > 399) {
				data = {
					"error": data
				}
				data = JSON.stringify(data);
			}
			return data;
		});
	}])
	.config(["messageHubProvider", function (messageHubProvider) {
		messageHubProvider.eventIdPrefix = 'history-view';
	}])
	.factory('gitService', ['$http', 'GIT_SVC_URL', function ($http, GIT_SVC_URL) {
		return new GitService($http, GIT_SVC_URL);
	}])
	.controller('HistoryContoller', ['$scope', 'gitService', 'messageHub', function ($scope, gitService, messageHub) {

		$scope.selectedWorkspace;
		$scope.selectedProject;
		$scope.selectedFile;
		$scope.history = null;

		$scope.refreshRepository = function () {
			$scope.selectedFile = null;
			$scope.refresh();
		}

		$scope.refresh = function () {
			if (!$scope.selectedWorkspace || !$scope.selectedProject) {
				$scope.history = [];
				return;
			}
			gitService.history($scope.selectedWorkspace, $scope.selectedProject, $scope.selectedFile)
				.then(function (response) {
					$scope.history = response.data;
					$scope.history.map(e => e.shortId = e.id.substring(0, 7));
				}.bind(this), function (response) {
					let errorMessage = JSON.parse(response.data.error).message;
					messageHub.showAlertError("Loading Git Repository History Error", errorMessage);
				});
		};

		$scope.getNoDataMessage = function () {
			return !$scope.history ? 'Please, select a project' : 'No data found';
		}

		$scope.showEmptyRow = function () {
			return !$scope.history || !$scope.history.length;
		}

		messageHub.onDidReceiveMessage('git.repository.selected', function (msg) {
			if (msg.data.isGitProject) {
				$scope.selectedWorkspace = msg.data.workspace;
				$scope.selectedProject = msg.data.project;
				$scope.selectedFile = null;
			} else {
				$scope.selectedProject = null;
			}
			$scope.refresh();
		}, true);

		messageHub.onDidReceiveMessage('git.repository.file.selected', function (msg) {
			if (msg.data.isGitProject) {
				$scope.selectedWorkspace = msg.data.workspace;
				$scope.selectedProject = msg.data.project;
				$scope.selectedFile = msg.data.file;
			} else {
				$scope.selectedProject = null;
			}
			$scope.refresh();
		}, true);
	}]);