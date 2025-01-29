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
const historyView = angular.module('history', ['blimpKit', 'platformView', 'GitService']);
historyView.controller('HistoryContoller', ($scope, GitService) => {
	$scope.state = {
		isBusy: false,
	};
	const dialogHub = new DialogHub();
	$scope.selectedWorkspace;
	$scope.selectedProject;
	$scope.selectedFile;
	$scope.history = null;

	$scope.refreshRepository = () => {
		$scope.selectedFile = null;
		refresh();
	};

	const refresh = () => {
		if (!$scope.selectedWorkspace || !$scope.selectedProject) {
			$scope.$evalAsync(() => {
				$scope.history = [];
				$scope.state.isBusy = false;
			});
			return;
		}
		GitService.history($scope.selectedWorkspace, $scope.selectedProject, $scope.selectedFile).then((response) => {
			$scope.$evalAsync(() => {
				$scope.history = response.data;
				$scope.history.map(e => e.shortId = e.id.substring(0, 7));
				$scope.state.isBusy = false;
			});
		}, (response) => {
			$scope.$evalAsync(() => {
				$scope.state.isBusy = false;
			});
			dialogHub.showAlert({
				title: 'History Error',
				message: response.message || 'There was an error while loading the git repository history.',
				type: AlertTypes.Error,
			});
		});
	};

	$scope.getNoDataMessage = () => {
		return !$scope.history ? 'Please, select a project' : 'No data found';
	};

	$scope.showEmptyRow = () => {
		return !$scope.history || !$scope.history.length;
	};

	const repositorySelectedListener = dialogHub.addMessageListener({
		topic: 'git.repository.selected',
		handler: (data) => {
			if ($scope.selectedWorkspace !== data.workspace || $scope.selectedProject !== data.project) {
				$scope.$evalAsync(() => {
					$scope.state.isBusy = true;
					if (data.isGitProject) {
						$scope.selectedWorkspace = data.workspace;
						$scope.selectedProject = data.project;
						$scope.selectedFile = null;
					} else {
						$scope.selectedProject = null;
					}
					refresh();
				});
			}
		}
	});

	const fileSelectedListener = dialogHub.addMessageListener({
		topic: 'git.repository.file.selected',
		handler: (data) => {
			if ($scope.selectedWorkspace !== data.workspace || $scope.selectedProject !== data.project || $scope.selectedFile !== data.file) {
				$scope.$evalAsync(() => {
					$scope.state.isBusy = true;
					if (data.isGitProject) {
						$scope.selectedWorkspace = data.workspace;
						$scope.selectedProject = data.project;
						$scope.selectedFile = data.file;
					} else {
						$scope.selectedProject = null;
					}
					refresh();
				});
			}
		}
	});

	$scope.$on('$destroy', () => {
		dialogHub.removeMessageListener(repositorySelectedListener);
		dialogHub.removeMessageListener(fileSelectedListener);
	});
});