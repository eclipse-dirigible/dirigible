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
angular.module('page', ['blimpKit', 'platformView', 'platformShortcuts', 'WorkspaceService']).controller('PageController', ($scope, $window, WorkspaceService, ViewParameters, ButtonStates) => {
	const statusBarHub = new StatusBarHub();
	const workspaceHub = new WorkspaceHub();
	const layoutHub = new LayoutHub();
	const dialogHub = new DialogHub();
	let contents;
	$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
	$scope.inputErrorMessage = 'Allowed characters include all letters, numbers, \'_\', \'-\', \'.\', \':\' and \'"\'. Maximum length is 255.';
	$scope.forms = {
		editor: {},
	};
	$scope.state = {
		isBusy: true,
		error: false,
		busyText: 'Loading...',
	};
	$scope.nameRegex = { patterns: ['^[a-zA-Z0-9_.:"-]*$'] };
	$scope.types = [
		{ value: "TABLE", label: "Table" },
		{ value: "VIEW", label: "View" },
	];
	$scope.editDependencyIndex = 0;

	angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

	const loadFileContents = () => {
		if (!$scope.state.error) {
			$scope.state.isBusy = true;
			WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
				$scope.$evalAsync(() => {
					if (response.data === '') $scope.view = {};
					else $scope.view = response.data;
					contents = JSON.stringify($scope.view, null, 4);
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
				saveContents(JSON.stringify($scope.view, null, 4));
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

	$scope.$watch('view', () => {
		if (!$scope.state.error && !$scope.state.isBusy) {
			const isDirty = contents !== JSON.stringify($scope.view, null, 4);
			if ($scope.changed !== isDirty) {
				$scope.changed = isDirty;
				layoutHub.setEditorDirty({
					path: $scope.dataParameters.filePath,
					dirty: isDirty,
				});
			}
		}
	}, true);

	$scope.addDependency = () => {
		const excludedNames = [];
		for (let i = 0; i < $scope.view.dependencies.length; i++) {
			excludedNames.push($scope.view.dependencies[i].name);
		}
		dialogHub.showFormDialog({
			title: 'Add dependency',
			form: {
				'vedType': {
					label: 'Type',
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.types,
					value: $scope.types[0].value,
					required: true,
				},
				'veiName': {
					label: 'Name',
					controlType: 'input',
					placeholder: "Enter name",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
						patterns: $scope.nameRegex.patterns,
					},
				},
				required: true,
				focus: true,
				submitOnEnter: true,
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) $scope.$evalAsync(() => {
				$scope.view.dependencies.push({
					name: form['veiName'],
					type: form['vedType'],
				});
			});
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'New dependency error',
				message: 'There was an error while adding the new dependency.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.editDependency = function (index) {
		$scope.editDependencyIndex = index;
		const excludedNames = [];
		for (let i = 0; i < $scope.view.dependencies.length; i++) {
			if (i !== index)
				excludedNames.push($scope.view.dependencies[i].name);
		}
		dialogHub.showFormDialog({
			title: 'Add dependency',
			form: {
				'vedType': {
					label: 'Type',
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.types,
					value: $scope.view.dependencies[index].type,
					required: true,
				},
				'veiName': {
					label: 'Name',
					controlType: 'input',
					placeholder: "Enter name",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
						patterns: $scope.nameRegex.patterns,
					},
					value: $scope.view.dependencies[index].name,
				},
				required: true,
				focus: true,
				submitOnEnter: true,
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) $scope.$evalAsync(() => {
				$scope.view.dependencies[$scope.editDependencyIndex].name = form['veiName'];
				$scope.view.dependencies[$scope.editDependencyIndex].type = form['vedType'];
			});
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Dependency update error',
				message: 'There was an error while updating the dependency.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.deleteDependency = (index) => {
		dialogHub.showDialog({
			title: `Delete ${$scope.view.dependencies[index].name}?`,
			message: 'This action cannot be undone.',
			buttons: [{
				id: 'bd',
				state: ButtonStates.Negative,
				label: 'Delete',
			},
			{
				id: 'bc',
				state: ButtonStates.Transparent,
				label: 'Cancel',
			}]
		}).then((buttonId) => {
			if (buttonId === 'bd') {
				$scope.$evalAsync(() => {
					$scope.view.dependencies.splice(index, 1);
				});
			}
		});
	};

	$scope.dataParameters = ViewParameters.get();
	if (!$scope.dataParameters.hasOwnProperty('filePath')) {
		$scope.state.error = true;
		$scope.errorMessage = 'The \'filePath\' data parameter is missing.';
	} else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
		$scope.state.error = true;
		$scope.errorMessage = 'The \'contentType\' data parameter is missing.';
	} else loadFileContents();
});
