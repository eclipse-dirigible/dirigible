/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('page', ['blimpKit', 'platformView', 'platformShortcuts', 'WorkspaceService']).controller('PageController', ($scope, $window, WorkspaceService, ViewParameters) => {
	const statusBarHub = new StatusBarHub();
	const workspaceHub = new WorkspaceHub();
	const layoutHub = new LayoutHub();
	let contents;
	$scope.changed = false;
	$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
	$scope.endpointErrorMessage = 'Allowed characters include all letters, numbers, \'_\', \'.\' and \'-\'. Maximum length is 255.';
	$scope.handlerErrorMessage = 'Allowed characters include all letters, numbers, \'_\', \'.\', \'-\', \'/\', and \'$\'. Maximum length is 255.';
	$scope.forms = {
		editor: {},
	};
	$scope.state = {
		isBusy: true,
		error: false,
		busyText: "Loading...",
	};

	angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

	const loadFileContents = () => {
		if (!$scope.state.error) {
			$scope.state.isBusy = true;
			WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
				$scope.$evalAsync(() => {
					if (response.data === '') $scope.websocket = {};
					else $scope.websocket = response.data;
					contents = JSON.stringify($scope.websocket, null, 4);
					$scope.state.isBusy = false;
				});
			}, (response) => {
				console.error(response);
				$scope.$evalAsync(() => {
					$scope.state.error = true;
					$scope.errorMessage = 'Error while loading file. Please look at the console for more information.';
					$scope.state.isBusy = false;
				});
			});
		}
	};

	function saveContents(text) {
		WorkspaceService.saveContent($scope.dataParameters.filePath, text).then(() => {
			contents = text;
			layoutHub.setEditorDirty({
				path: $scope.dataParameters.filePath,
				dirty: false,
			});
			workspaceHub.announceFileSaved({
				path: $scope.dataParameters.filePath,
				contentType: $scope.dataParameters.contentType,
			});
			$scope.$evalAsync(() => {
				$scope.changed = false;
				$scope.state.isBusy = false;
			});
		}, (response) => {
			console.error(response);
			$scope.$evalAsync(() => {
				$scope.state.error = true;
				$scope.errorMessage = `Error saving '${$scope.dataParameters.filePath}'. Please look at the console for more information.`;
				$scope.state.isBusy = false;
			});
		});
	}

	$scope.save = (keySet = 'ctrl+s', event) => {
		event?.preventDefault();
		if (keySet === 'ctrl+s') {
			if ($scope.changed && $scope.forms.editor.$valid && !$scope.state.error) {
				$scope.state.busyText = 'Saving...';
				$scope.state.isBusy = true;
				saveContents(JSON.stringify($scope.websocket, null, 4));
			}
		}
	};

	layoutHub.onFocusEditor((data) => {
		if (data.path && data.path === $scope.dataParameters.filePath) statusBarHub.showLabel('');
	});

	layoutHub.onReloadEditorParams((data) => {
		if (data.path === $scope.dataParameters.filePath) {
			$scope.$evalAsync(() => {
				$scope.dataParameters = ViewParameters.get();
			});
		};
	});

	workspaceHub.onSaveAll(() => {
		if ($scope.changed && !$scope.state.error && $scope.forms.editor.$valid) {
			$scope.save();
		}
	});

	workspaceHub.onSaveFile((data) => {
		if (data.path && data.path === $scope.dataParameters.filePath) {
			if ($scope.changed && !$scope.state.error && $scope.forms.editor.$valid) {
				$scope.save();
			}
		}
	});

	$scope.$watch('websocket', () => {
		if (!$scope.state.error && !$scope.state.isBusy) {
			const isDirty = contents !== JSON.stringify($scope.websocket, null, 4);
			if ($scope.changed !== isDirty) {
				$scope.changed = isDirty;
				layoutHub.setEditorDirty({
					path: $scope.dataParameters.filePath,
					dirty: isDirty,
				});
			}
		}
	}, true);

	$scope.dataParameters = ViewParameters.get();
	if (!$scope.dataParameters.hasOwnProperty('filePath')) {
		$scope.state.error = true;
		$scope.errorMessage = 'The \'filePath\' data parameter is missing.';
	} else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
		$scope.state.error = true;
		$scope.errorMessage = 'The \'contentType\' data parameter is missing.';
	} else loadFileContents();
});