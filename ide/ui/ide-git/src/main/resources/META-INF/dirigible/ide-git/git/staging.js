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
/**
 * Utility URL builder
 */
let UriBuilder = function UriBuilder() {
	this.pathSegments = [];
	return this;
}
UriBuilder.prototype.path = function (_pathSegments) {
	if (!Array.isArray(_pathSegments))
		_pathSegments = [_pathSegments];
	_pathSegments = _pathSegments.filter(function (segment) {
		return segment;
	})
		.map(function (segment) {
			if (segment.length) {
				if (segment.charAt(segment.length - 1) === '/')
					segment = segment.substring(0, segment.length - 2);
				segment = encodeURIComponent(segment);
			}
			return segment;
		});
	this.pathSegments = this.pathSegments.concat(_pathSegments);
	return this;
}
UriBuilder.prototype.build = function (isBasePath = true) {
	if (isBasePath) return '/' + this.pathSegments.join('/');
	return this.pathSegments.join('/');
}

angular.module('git.config', [])
	.constant('GIT_SVC_URL', '/services/v4/ide/git');


/**
 * Git Service API delegate
 */
let GitService = function ($http, $messageHub, gitServiceUrl) {
	this.gitServiceUrl = gitServiceUrl;
	this.$http = $http;
	this.$messageHub = $messageHub;
}

GitService.prototype.commit = function (workspace, project, commitMessage, username, password, email, branch) {
	let messageHub = this.$messageHub;
	let url = new UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path("commit").build();
	return this.$http.post(url, {
		"commitMessage": commitMessage,
		"username": username,
		"password": btoa(password),
		"email": email,
		"branch": branch,
		"autoAdd": false,
		"autoCommit": false
	})
		.then(function (response) {
			return response.data;
		}, function (response) {
			let errorMessage = JSON.parse(response.data.error).message;
			messageHub.announceAlertError("Git Commit Error", errorMessage);
		});
}
GitService.prototype.push = function (workspace, project, commitMessage, username, password, email, branch) {
	let messageHub = this.$messageHub;
	let url = new UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path("push").build();

	return this.$http.post(url, {
		"commitMessage": commitMessage,
		"username": username,
		"password": btoa(password),
		"email": email,
		"branch": branch,
		"autoAdd": false,
		"autoCommit": false
	}).then(function (response) {
		return response.data;
	}, function (response) {
		let errorMessage = JSON.parse(response.data.error).message;
		messageHub.announceAlertError("Git Push Error", errorMessage);
	});
}
GitService.prototype.getUnstagedFiles = function (workspace, project) {
	let messageHub = this.$messageHub;
	let url = new UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path("unstaged").build();

	return this.$http.get(url, {})
		.then(function (response) {
			return response.data.files;
		}, function (response) {
			let errorMessage = JSON.parse(response.data.error).message;
			messageHub.announceAlertError("Git Staging Error", errorMessage);
		});
}
GitService.prototype.getStagedFiles = function (workspace, project) {
	let messageHub = this.$messageHub;
	let url = new UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path("staged").build();

	return this.$http.get(url, {})
		.then(function (response) {
			return response.data.files;
		}, function (response) {
			let errorMessage = JSON.parse(response.data.error).message;
			messageHub.announceAlertError("Git Staging Error", errorMessage);
		});
}
GitService.prototype.addFiles = function (workspace, project, files) {
	let messageHub = this.$messageHub;
	let url = new UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path("add").build();
	let list = files.join(",");

	return this.$http.post(url, JSON.stringify(list))
		.then(function (response) {
			return response.data;
		}, function (response) {
			let errorMessage = JSON.parse(response.data.error).message;
			messageHub.announceAlertError("Git Staging Error", errorMessage);
		});
}
GitService.prototype.revertFiles = function (workspace, project, files) {
	let messageHub = this.$messageHub;
	let url = new UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path("revert").build();
	let list = files.join(",");

	return this.$http.post(url, JSON.stringify(list))
		.then(function (response) {
			return response.data;
		}, function (response) {
			let errorMessage = JSON.parse(response.data.error).message;
			messageHub.announceAlertError("Git Staging Error", errorMessage);
		});
}
GitService.prototype.removeFiles = function (workspace, project, files) {
	let messageHub = this.$messageHub;
	let url = new UriBuilder().path(this.gitServiceUrl.split('/')).path(workspace).path(project).path("remove").build();
	let list = files.join(",");

	return this.$http.post(url, JSON.stringify(list))
		.then(function (response) {
			return response.data;
		}, function (response) {
			let errorMessage = JSON.parse(response.data.error).message;
			messageHub.announceAlertError("Git Staging Error", errorMessage);
		});
}

let stagingApp = angular.module('stagingApp', ['git.config', 'ngAnimate', 'ngSanitize', 'ui.bootstrap'])
	.factory('httpRequestInterceptor', function () {
		let csrfToken = null;
		return {
			request: function (config) {
				config.headers['X-Requested-With'] = 'Fetch';
				config.headers['X-CSRF-Token'] = csrfToken ? csrfToken : 'Fetch';
				return config;
			},
			response: function (response) {
				let token = response.headers()['x-csrf-token'];
				if (token) {
					csrfToken = token;
				}
				return response;
			}
		};
	})
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
		$httpProvider.interceptors.push('httpRequestInterceptor');
	}])
	.factory('$messageHub', [function () {
		let messageHub = new FramesMessageHub();
		let message = function (evtName, data) {
			messageHub.post({ data: data }, 'git.' + evtName);
		};
		let announceFileDiff = function (fileDescriptor) {
			this.message('staging.file.diff', fileDescriptor);
		};
		let announceAlert = function (title, message, type) {
			messageHub.post({
				data: {
					title: title,
					message: message,
					type: type
				}
			}, 'ide.alert');
		};
		let announceAlertSuccess = function (title, message) {
			announceAlert(title, message, "success");
		};
		let announceAlertInfo = function (title, message) {
			announceAlert(title, message, "info");
		};
		let announceAlertWarning = function (title, message) {
			announceAlert(title, message, "warning");
		};
		let announceAlertError = function (title, message) {
			announceAlert(title, message, "error");
		};
		return {
			message: message,
			announceFileDiff: announceFileDiff,
			announceAlert: announceAlert,
			announceAlertSuccess: announceAlertSuccess,
			announceAlertInfo: announceAlertInfo,
			announceAlertWarning: announceAlertWarning,
			announceAlertError: announceAlertError,
			on: function (evt, cb) {
				messageHub.subscribe(cb, evt);
			}
		};
	}])
	.factory('gitService', ['$http', '$messageHub', 'GIT_SVC_URL', function ($http, $messageHub, GIT_SVC_URL) {
		return new GitService($http, $messageHub, GIT_SVC_URL);
	}])
	.controller('StagingContoller', ['gitService', '$messageHub', '$scope', function (gitService, $messageHub, $scope) {
		let revertWarnAccepted = false;

		$scope.unstagedFiles = [];
		$scope.selectedUnstagedFiles = [];
		$scope.stagedFiles = [];
		$scope.selectedStagedFiles = [];
		$scope.commitMessage;
		$scope.selectedProject;
		$scope.selectedWorkspace;
		$scope.username;
		$scope.password;
		$scope.email;
		$scope.branch;

		let types = [];
		types[0] = "&#xf071;"; //"conflicting";
		types[1] = "&#xf055;"; //"plus-circle";//"added"; // staged
		types[2] = "&#xf058;"; //"check-circle"//"changed"; // staged
		types[3] = "&#xf05c;"; //"times-circle-o"//"missing"; // unstaged
		types[4] = "&#xf05d;"; //"check-circle-o"//"modified"; // unstaged
		types[5] = "&#xf057;"; //"times-circle"//"removed"; // staged
		types[6] = "&#xf10c;"; //"question-circle-o"//"untracked"; // unstaged

		function typeIcon(i) {
			return types[i];
		}

		$scope.okCommitAndPushClicked = function () {
			if ($scope.commitMessage === undefined || $scope.commitMessage === "") {
				$scope.commitMessage = null;
			}
			gitService.commit($scope.selectedWorkspace, $scope.selectedProject, $scope.commitMessage, $scope.username, $scope.password, $scope.email, $scope.branch)
				.then(function () {
					gitService.push($scope.selectedWorkspace, $scope.selectedProject, $scope.commitMessage, $scope.username, $scope.password, $scope.email, $scope.branch)
						.then(function () {
							$scope.commitMessage = "";
							$messageHub.message("diff.view.clear");
							$scope.refresh();
						}.bind(this));
				}.bind(this));

		};

		$scope.okCommitClicked = function () {
			if ($scope.commitMessage === undefined || $scope.commitMessage === "") {
				$scope.commitMessage = null;
			}
			gitService.commit($scope.selectedWorkspace, $scope.selectedProject, $scope.commitMessage, $scope.username, $scope.password, $scope.email, $scope.branch)
				.then(function () {
					$scope.commitMessage = "";
					$messageHub.message("diff.view.clear");
					$scope.refresh();
				}.bind(this));
		};

		$scope.refresh = function () {
			if (!$scope.selectedWorkspace || !$scope.selectedProject) {
				$scope.unstagedFiles = [];
				$scope.stagedFiles = [];
				return;
			}
			gitService.getUnstagedFiles($scope.selectedWorkspace, $scope.selectedProject)
				.then(function (files) {
					$scope.unstagedFiles = files;
					$scope.unstagedFiles.map(e => { e.label = typeIcon(e.type) + ' ' + e.path });
				}.bind(this));
			gitService.getStagedFiles($scope.selectedWorkspace, $scope.selectedProject)
				.then(function (files) {
					$scope.stagedFiles = files;
					$scope.stagedFiles.map(e => { e.label = typeIcon(e.type) + ' ' + e.path });
				}.bind(this));
		};

		$scope.downClicked = function () {
			if ($scope.selectedUnstagedFiles.length > 0) {
				gitService.addFiles($scope.selectedWorkspace, $scope.selectedProject, $scope.selectedUnstagedFiles)
					.then(function () {
						$scope.refresh();
					}.bind(this));
			}
		};

		$scope.upClicked = function () {
			if ($scope.selectedStagedFiles.length > 0) {
				gitService.removeFiles($scope.selectedWorkspace, $scope.selectedProject, $scope.selectedStagedFiles)
					.then(function () {
						$scope.refresh();
					}.bind(this));
			}
		};

		$scope.diffClicked = function (file) {
			if (!file) {
				if ($scope.selectedUnstagedFiles.length > 0) file = $scope.selectedUnstagedFiles[0];
				else return;
			}
			$messageHub.announceFileDiff({
				project: $scope.selectedWorkspace + "/" + $scope.selectedProject,
				file: file.path
			});
		};

		$scope.confirmRevert = function () {
			revertWarnAccepted = true;
			$scope.revertClicked();
		};

		$scope.revertClicked = function () {
			if ($scope.selectedUnstagedFiles.length > 0) {
				if (revertWarnAccepted) {
					gitService.revertFiles($scope.selectedWorkspace, $scope.selectedProject, $scope.selectedUnstagedFiles)
						.then(function () {
							$scope.refresh();
						}.bind(this));
				}
				else {
					$('#revertModal').modal('show');
				}
			}
		};

		$messageHub.on('git.repository.selected', function (msg) {
			if (msg.data.isGitProject) {
				$scope.selectedWorkspace = msg.data.workspace;
				$scope.selectedProject = msg.data.project;
			} else {
				$scope.selectedProject = null;
			}
			$scope.refresh();
		}.bind(this));

	}]);