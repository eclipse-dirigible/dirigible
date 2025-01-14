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
	$scope.inputErrorMessage = 'Allowed characters include all letters, numbers, \'_\', \'-\', \'.\', \':\' and \'"\'. Maximum length is 255.';
	$scope.forms = {
		editor: {},
	};
	$scope.state = {
		isBusy: true,
		error: false,
		busyText: 'Loading...',
	};
	$scope.editColumnIndex = 0;
	$scope.nameRegex = { patterns: ['^[a-zA-Z0-9_.:"-]*$'] };
	$scope.types = [
		{ value: 'VARCHAR', label: 'VARCHAR' },
		{ value: 'CHAR', label: 'CHAR' },
		{ value: 'DATE', label: 'DATE' },
		{ value: 'TIME', label: 'TIME' },
		{ value: 'TIMESTAMP', label: 'TIMESTAMP' },
		{ value: 'INTEGER', label: 'INTEGER' },
		{ value: 'TINYINT', label: 'TINYINT' },
		{ value: 'BIGINT', label: 'BIGINT' },
		{ value: 'SMALLINT', label: 'SMALLINT' },
		{ value: 'REAL', label: 'REAL' },
		{ value: 'DOUBLE', label: 'DOUBLE' },
		{ value: 'BOOLEAN', label: 'BOOLEAN' },
		{ value: 'BLOB', label: 'BLOB' },
		{ value: 'DECIMAL', label: 'DECIMAL' },
		{ value: 'BIT', label: 'BIT' },
	];

	angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

	const loadFileContents = () => {
		if (!$scope.state.error) {
			$scope.state.isBusy = true;
			WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
				$scope.$evalAsync(() => {
					if (response.data === '') $scope.table = {};
					else {
						$scope.table = response.data;
						$scope.fixBooleans();
					}
					contents = JSON.stringify($scope.table, null, 4);
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

	// For legacy reasons
	$scope.fixBooleans = () => {
		for (let i = 0; i < $scope.table.columns.length; i++) {
			if (typeof $scope.table.columns[i].nullable !== 'boolean') {
				if ($scope.table.columns[i].primaryKey === 'true') {
					$scope.table.columns[i].primaryKey = true;
				} else {
					$scope.table.columns[i].primaryKey = false;
				}
			}
			if (typeof $scope.table.columns[i].unique !== 'boolean') {
				if ($scope.table.columns[i].unique === 'true') {
					$scope.table.columns[i].unique = true;
				} else {
					$scope.table.columns[i].unique = false;
				}
			}
			if (typeof $scope.table.columns[i].nullable !== 'boolean') {
				if ($scope.table.columns[i].nullable === 'true') {
					$scope.table.columns[i].nullable = true;
				} else {
					$scope.table.columns[i].nullable = false;
				}
			}
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
				saveContents(JSON.stringify($scope.table, null, 4));
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

	$scope.$watch('table', () => {
		if (!$scope.state.error && !$scope.state.isBusy) {
			const isDirty = contents !== JSON.stringify($scope.table, null, 4);
			if ($scope.changed !== isDirty) {
				$scope.changed = isDirty;
				layoutHub.setEditorDirty({
					path: $scope.dataParameters.filePath,
					dirty: isDirty,
				});
			}
		}
	}, true);

	$scope.addColumn = () => {
		const excludedNames = [];
		for (let i = 0; i < $scope.table.columns.length; i++) {
			excludedNames.push($scope.table.columns[i].name);
		}
		dialogHub.showFormDialog({
			title: 'Add column',
			form: {
				'teiName': {
					label: 'Name',
					controlType: 'input',
					placeholder: 'Enter name',
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
					},
					focus: true,
					required: true
				},
				'tedType': {
					label: 'Type',
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.types,
					value: $scope.types[0].value,
					required: true,
				},
				'teiLength': {
					label: 'Length',
					controlType: 'input',
					placeholder: "Enter length",
					type: 'text',
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
				},
				'tecPrimaryKey': {
					label: 'Primary Key',
					controlType: 'checkbox',
					value: false
				},
				'tecUnique': {
					label: 'Unique',
					controlType: 'checkbox',
					value: false
				},
				'tecNullable': {
					label: 'Nullable',
					controlType: 'checkbox',
					value: false
				},
				'teiDefaultValue': {
					label: 'Default Value',
					controlType: 'input',
					placeholder: 'Enter value',
					type: 'text',
				},
				'teiPrecision': {
					label: 'Precision',
					controlType: 'input',
					placeholder: 'Enter precision number',
					type: 'text',
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
				},
				'teiScale': {
					label: 'Scale',
					controlType: 'input',
					placeholder: 'Enter scale number',
					type: 'text',
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) $scope.$evalAsync(() => {
				$scope.table.columns.push({
					name: form['teiName'],
					type: form['tedType'],
					length: form['teiLength'],
					primaryKey: form['tecPrimaryKey'],
					unique: form['tecUnique'],
					nullable: form['tecNullable'],
					defaultValue: form['teiDefaultValue'],
					precision: form['teiPrecision'],
					scale: form['teiScale'],
				});
			});
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'New column error',
				message: 'There was an error while adding the new column.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.editColumn = (index) => {
		$scope.editColumnIndex = index;
		const excludedNames = [];
		for (let i = 0; i < $scope.table.columns.length; i++) {
			if (i !== index)
				excludedNames.push($scope.table.columns[i].name);
		}
		dialogHub.showFormDialog({
			title: 'Edit column',
			form: {
				'teiName': {
					label: 'Name',
					controlType: 'input',
					placeholder: 'Enter name',
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
					},
					value: $scope.table.columns[index].name,
					focus: true,
					required: true
				},
				'tedType': {
					label: 'Type',
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.types,
					value: $scope.table.columns[index].type,
					required: true,
				},
				'teiLength': {
					label: 'Length',
					controlType: 'input',
					placeholder: "Enter length",
					type: 'text',
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
					value: $scope.table.columns[index].length,
				},
				'tecPrimaryKey': {
					label: 'Primary Key',
					controlType: 'checkbox',
					value: $scope.table.columns[index].primaryKey || false,
				},
				'tecUnique': {
					label: 'Unique',
					controlType: 'checkbox',
					value: $scope.table.columns[index].unique || false,
				},
				'tecNullable': {
					label: 'Nullable',
					controlType: 'checkbox',
					value: $scope.table.columns[index].nullable || false,
				},
				'teiDefaultValue': {
					label: 'Default Value',
					controlType: 'input',
					placeholder: 'Enter value',
					type: 'text',
					value: $scope.table.columns[index].defaultValue,
				},
				'teiPrecision': {
					label: 'Precision',
					controlType: 'input',
					placeholder: 'Enter precision number',
					type: 'text',
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
					value: $scope.table.columns[index].precision,
				},
				'teiScale': {
					label: 'Scale',
					controlType: 'input',
					placeholder: 'Enter scale number',
					type: 'text',
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
					value: $scope.table.columns[index].scale,
				},
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) $scope.$evalAsync(() => {
				$scope.table.columns[$scope.editColumnIndex].name = form['teiName'];
				$scope.table.columns[$scope.editColumnIndex].type = form['tedType'];
				$scope.table.columns[$scope.editColumnIndex].length = form['teiLength'];
				$scope.table.columns[$scope.editColumnIndex].primaryKey = form['tecPrimaryKey'];
				$scope.table.columns[$scope.editColumnIndex].unique = form['tecUnique'];
				$scope.table.columns[$scope.editColumnIndex].nullable = form['tecNullable'];
				$scope.table.columns[$scope.editColumnIndex].defaultValue = form['teiDefaultValue'];
				$scope.table.columns[$scope.editColumnIndex].precision = form['teiPrecision'];
				$scope.table.columns[$scope.editColumnIndex].scale = form['teiScale'];
			});
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Column update error',
				message: 'There was an error while updating the column.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.deleteColumn = (index) => {
		dialogHub.showDialog({
			title: `Delete ${$scope.table.columns[index].name}?`,
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
					$scope.table.columns.splice(index, 1);
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