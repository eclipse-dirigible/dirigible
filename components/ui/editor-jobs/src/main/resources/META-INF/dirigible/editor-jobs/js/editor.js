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
	$scope.changed = false;
	$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
	$scope.groupErrorMessage = 'Allowed characters include all letters, numbers, \'_\', \'.\' and \'-\'. Maximum length is 255.';
	$scope.handlerErrorMessage = 'Allowed characters include all letters, numbers, \'_\', \'.\', \'-\', \'/\', and \'$\'. Maximum length is 255.';
	$scope.forms = {
		editor: {},
	};
	$scope.state = {
		isBusy: true,
		error: false,
		busyText: 'Loading...',
	};
	$scope.types = [
		{ value: 'string', label: 'string' },
		{ value: 'number', label: 'number' },
		{ value: 'boolean', label: 'boolean' },
		{ value: 'choice', label: 'choice' },
	];
	$scope.editParameterIndex = 0;

	angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

	const loadFileContents = () => {
		if (!$scope.state.error) {
			$scope.state.isBusy = true;
			WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
				$scope.$evalAsync(() => {
					if (response.data === '') $scope.job = { parameters: [] };
					else {
						$scope.job = response.data;
						if (!$scope.job.parameters || $scope.job.parameters === '') $scope.job.parameters = [];
					}
					contents = JSON.stringify($scope.job, null, 4);
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
				saveContents(JSON.stringify($scope.job, null, 4));
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

	$scope.$watch('job', () => {
		if (!$scope.state.error && !$scope.state.isBusy) {
			const isDirty = contents !== JSON.stringify($scope.job, null, 4);
			if ($scope.changed !== isDirty) {
				$scope.changed = isDirty;
				layoutHub.setEditorDirty({
					path: $scope.dataParameters.filePath,
					dirty: isDirty,
				});
			}
		}
	}, true);

	$scope.addParameter = () => {
		const excludedNames = [];
		for (let i = 0; i < $scope.job.parameters.length; i++) {
			excludedNames.push($scope.job.parameters[i].name);
		}
		dialogHub.showFormDialog({
			title: 'Add parameter',
			form: {
				'jeapiName': {
					label: 'Name',
					controlType: 'input',
					placeholder: 'Enter name',
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
						patterns: ['^[a-zA-Z0-9_.-]*$'],
					},
					focus: true,
					required: true
				},
				'jeapdType': {
					label: 'Type',
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.types,
					value: 'string',
					required: true,
				},
				'jeapiDefaultValue': {
					label: 'Default Value',
					controlType: 'input',
					placeholder: 'Enter default value',
					type: 'text',
				},
				'jeapiChoices': {
					label: 'Choices',
					controlType: 'input',
					placeholder: 'Comma separated choices',
					type: 'text',
					visibleOn: { key: 'jeapdType', value: 'choice' },
				},
				'jeapiDescription': {
					label: 'Description',
					controlType: 'input',
					placeholder: 'Enter description',
					type: 'text',
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				let parameter = {
					name: form['jeapiName'],
					type: form['jeapdType'],
					defaultValue: form['jeapiDefaultValue'],
					choices: form['jeapiChoices'],
					description: form['jeapiDescription'],
				};
				if (parameter.type !== 'choice') delete parameter['choices'];
				$scope.$evalAsync(() => {
					$scope.job.parameters.push(parameter);
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'New parameter error',
				message: 'There was an error while adding the new parameter.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.editParameter = (index) => {
		$scope.editParameterIndex = index;
		const excludedNames = [];
		for (let i = 0; i < $scope.job.parameters.length; i++) {
			if (i !== index) excludedNames.push($scope.job.parameters[i].name);
		}
		dialogHub.showFormDialog({
			title: 'Edit parameter',
			form: {
				'jeapiName': {
					label: 'Name',
					controlType: 'input',
					placeholder: 'Enter name',
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
						patterns: ['^[a-zA-Z0-9_.-]*$'],
					},
					value: $scope.job.parameters[index].name,
					focus: true,
					required: true
				},
				'jeapdType': {
					label: 'Type',
					placeholder: 'Select time',
					controlType: 'dropdown',
					options: $scope.types,
					value: $scope.job.parameters[index].type,
					required: true,
				},
				'jeapiDefaultValue': {
					label: 'Default Value',
					controlType: 'input',
					placeholder: 'Enter default value',
					type: 'text',
					value: $scope.job.parameters[index].defaultValue,
				},
				'jeapiChoices': {
					label: 'Choices',
					controlType: 'input',
					placeholder: 'Comma separated choices',
					type: 'text',
					value: $scope.job.parameters[index]['choices'] || '',
					visibleOn: { key: 'jeapdType', value: 'choice' },
				},
				'jeapiDescription': {
					label: 'Description',
					controlType: 'input',
					placeholder: 'Enter description',
					type: 'text',
					value: $scope.job.parameters[index].description,
				},
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					$scope.job.parameters[$scope.editParameterIndex].name = form['jeapiName'];
					$scope.job.parameters[$scope.editParameterIndex].type = form['jeapdType'];
					$scope.job.parameters[$scope.editParameterIndex].defaultValue = form['jeapiDefaultValue'];
					$scope.job.parameters[$scope.editParameterIndex].description = form['jeapiDescription'];
					if (form['jeapdType'] === 'choice') $scope.job.parameters[$scope.editParameterIndex]['choices'] = form['jeapiChoices'];
					else delete $scope.job.parameters[$scope.editParameterIndex]['choices'];
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Parameter update error',
				message: 'There was an error while updating the job parameter.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.deleteParameter = (index) => {
		dialogHub.showDialog({
			title: `Delete ${$scope.job.parameters[index].name}?`,
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
					$scope.job.parameters.splice(index, 1);
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