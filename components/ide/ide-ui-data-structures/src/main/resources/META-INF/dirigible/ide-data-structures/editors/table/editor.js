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
angular.module('page', ["ideUI", "ideView", "ideWorkspace"])
	.controller('PageController', function ($scope, $window, workspaceApi, messageHub, ViewParameters) {
		let contents;
		$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
		$scope.forms = {
			editor: {},
		};
		$scope.state = {
			isBusy: true,
			error: false,
			busyText: "Loading...",
		};
		$scope.editColumnIndex = 0;
		$scope.nameRegex = { patterns: ['^[a-zA-Z0-9_.:"-]*$'] };
		$scope.types = [
			{ value: "VARCHAR", label: "VARCHAR" },
			{ value: "CHAR", label: "CHAR" },
			{ value: "DATE", label: "DATE" },
			{ value: "TIME", label: "TIME" },
			{ value: "TIMESTAMP", label: "TIMESTAMP" },
			{ value: "INTEGER", label: "INTEGER" },
			{ value: "TINYINT", label: "TINYINT" },
			{ value: "BIGINT", label: "BIGINT" },
			{ value: "SMALLINT", label: "SMALLINT" },
			{ value: "REAL", label: "REAL" },
			{ value: "DOUBLE", label: "DOUBLE" },
			{ value: "BOOLEAN", label: "BOOLEAN" },
			{ value: "BLOB", label: "BLOB" },
			{ value: "DECIMAL", label: "DECIMAL" },
			{ value: "BIT", label: "BIT" },
		];

		angular.element($window).bind("focus", function () {
			messageHub.setFocusedEditor($scope.dataParameters.file);
			messageHub.setStatusCaret('');
		});

		$scope.load = function () {
			if (!$scope.state.error) {
				workspaceApi.loadContent('', $scope.dataParameters.file).then(function (response) {
					if (response.status === 200) {
						$scope.table = response.data;
						$scope.fixBooleans();
						contents = JSON.stringify($scope.table, null, 4);
						$scope.$apply(() => $scope.state.isBusy = false);
					} else if (response.status === 404) {
						messageHub.closeEditor($scope.dataParameters.file);
					} else {
						$scope.$apply(function () {
							$scope.state.error = true;
							$scope.errorMessage = 'There was a problem with loading the file';
							$scope.state.isBusy = false;
						});
					}
				});
			}
		};

		// For legacy reasons
		$scope.fixBooleans = function () {
			for (let i = 0; i < $scope.table.columns.length; i++) {
				if (typeof $scope.table.columns[i].nullable !== "boolean") {
					if ($scope.table.columns[i].primaryKey === "true") {
						$scope.table.columns[i].primaryKey = true;
					} else {
						$scope.table.columns[i].primaryKey = false;
					}
				}
				if (typeof $scope.table.columns[i].unique !== "boolean") {
					if ($scope.table.columns[i].unique === "true") {
						$scope.table.columns[i].unique = true;
					} else {
						$scope.table.columns[i].unique = false;
					}
				}
				if (typeof $scope.table.columns[i].nullable !== "boolean") {
					if ($scope.table.columns[i].nullable === "true") {
						$scope.table.columns[i].nullable = true;
					} else {
						$scope.table.columns[i].nullable = false;
					}
				}
			}
		};

		function saveContents(text) {
			workspaceApi.saveContent('', $scope.dataParameters.file, text).then(function (response) {
				if (response.status === 200) {
					messageHub.announceFileSaved({
						name: $scope.dataParameters.file.substring($scope.dataParameters.file.lastIndexOf('/') + 1),
						path: $scope.dataParameters.file.substring($scope.dataParameters.file.indexOf('/', 1)),
						contentType: $scope.dataParameters.contentType,
						workspace: $scope.dataParameters.file.substring(1, $scope.dataParameters.file.indexOf('/', 1)),
					});
					messageHub.setStatusMessage(`File '${$scope.dataParameters.file}' saved`);
					messageHub.setEditorDirty($scope.dataParameters.file, false);
					$scope.$apply(function () {
						$scope.state.isBusy = false;
					});
				} else {
					messageHub.setStatusError(`Error saving '${$scope.dataParameters.file}'`);
					messageHub.showAlertError('Error while saving the file', 'Please look at the console for more information');
					$scope.$apply(function () {
						$scope.state.isBusy = false;
					});
				}
			});
		}

		$scope.save = function (_keySet, event) {
			if (event) event.preventDefault();
			if ($scope.forms.editor.$valid && !$scope.state.error) {
				$scope.state.busyText = "Saving...";
				$scope.state.isBusy = true;
				contents = JSON.stringify($scope.table, null, 4);
				saveContents(contents);
			}
		};

		messageHub.onEditorFocusGain(function (msg) {
			if (msg.resourcePath === $scope.dataParameters.file) messageHub.setStatusCaret('');
		});

		messageHub.onEditorReloadParameters(
			function (event) {
				$scope.$apply(() => {
					if (event.resourcePath === $scope.dataParameters.file) {
						$scope.dataParameters = ViewParameters.get();
					}
				});
			}
		);

		messageHub.onDidReceiveMessage(
			"editor.file.save.all",
			function () {
				if (!$scope.state.error && $scope.forms.editor.$valid) {
					let table = JSON.stringify($scope.table, null, 4);
					if (contents !== table) {
						$scope.save();
					}
				}
			},
			true,
		);

		messageHub.onDidReceiveMessage(
			"editor.file.save",
			function (msg) {
				if (!$scope.state.error && $scope.forms.editor.$valid) {
					let file = msg.data && typeof msg.data === 'object' && msg.data.file;
					if (file && file === $scope.dataParameters.file) {
						let table = JSON.stringify($scope.table, null, 4);
						if (contents !== table) $scope.save();
					}
				}
			},
			true,
		);

		messageHub.onDidReceiveMessage(
			"tableEditor.column.add",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.table.columns.push({
							name: msg.data.formData[0].value,
							type: msg.data.formData[1].value,
							length: msg.data.formData[2].value,
							primaryKey: msg.data.formData[3].value,
							unique: msg.data.formData[4].value,
							nullable: msg.data.formData[5].value,
							defaultValue: msg.data.formData[6].value,
							precision: msg.data.formData[7].value,
							scale: msg.data.formData[8].value,
						});
					});
				}
				messageHub.hideFormDialog("tableEditorAddColumn");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"tableEditor.column.edit",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.table.columns[$scope.editColumnIndex].name = msg.data.formData[0].value;
						$scope.table.columns[$scope.editColumnIndex].type = msg.data.formData[1].value;
						$scope.table.columns[$scope.editColumnIndex].length = msg.data.formData[2].value;
						$scope.table.columns[$scope.editColumnIndex].primaryKey = msg.data.formData[3].value;
						$scope.table.columns[$scope.editColumnIndex].unique = msg.data.formData[4].value;
						$scope.table.columns[$scope.editColumnIndex].nullable = msg.data.formData[5].value;
						$scope.table.columns[$scope.editColumnIndex].defaultValue = msg.data.formData[6].value;
						$scope.table.columns[$scope.editColumnIndex].precision = msg.data.formData[7].value;
						$scope.table.columns[$scope.editColumnIndex].scale = msg.data.formData[8].value;
					});
				}
				messageHub.hideFormDialog("tableEditorEditColumn");
			},
			true
		);

		$scope.$watch('table', function () {
			if (!$scope.state.error && !$scope.state.isBusy) {
				messageHub.setEditorDirty($scope.dataParameters.file, contents !== JSON.stringify($scope.table, null, 4));
			}
		}, true);

		$scope.addColumn = function () {
			let excludedNames = [];
			for (let i = 0; i < $scope.table.columns.length; i++) {
				excludedNames.push($scope.table.columns[i].name);
			}
			messageHub.showFormDialog(
				"tableEditorAddColumn",
				"Add column",
				[{
					id: "teiName",
					type: "input",
					label: "Name",
					required: true,
					placeholder: "Enter name",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
					},
					value: '',
				},
				{
					id: "tedType",
					type: "dropdown",
					label: "Type",
					required: true,
					value: $scope.types[0].value,
					items: $scope.types,
				},
				{
					id: "teiLength",
					type: "input",
					label: "Length",
					placeholder: "Enter length",
					value: '',
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
				},
				{
					id: "tecPrimaryKey",
					type: "checkbox",
					label: "Primary Key",
					value: false,
				},
				{
					id: "tecUnique",
					type: "checkbox",
					label: "Unique",
					value: false,
				},
				{
					id: "tecNullable",
					type: "checkbox",
					label: "Nullable",
					value: false,
				},
				{
					id: "teiDefaultValue",
					type: "input",
					label: "Default Value",
					placeholder: "Enter value",
					value: '',
				},
				{
					id: "teiPrecision",
					type: "input",
					label: "Precision",
					placeholder: "Enter precision number",
					value: '',
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
				},
				{
					id: "teiScale",
					type: "input",
					label: "Scale",
					placeholder: "Enter scale number",
					value: '',
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
				}],
				[{
					id: "b1",
					type: "emphasized",
					label: "Add",
					whenValid: true,
				},
				{
					id: "b2",
					type: "transparent",
					label: "Cancel",
				}],
				"tableEditor.column.add",
				"Adding parameter..."
			);
		};

		$scope.editColumn = function (index) {
			$scope.editColumnIndex = index;
			let excludedNames = [];
			for (let i = 0; i < $scope.table.columns.length; i++) {
				if (i !== index)
					excludedNames.push($scope.table.columns[i].name);
			}
			messageHub.showFormDialog(
				"tableEditorEditColumn",
				"Edit column",
				[{
					id: "teiName",
					type: "input",
					label: "Name",
					required: true,
					placeholder: "Enter name",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
					},
					value: $scope.table.columns[index].name,
				},
				{
					id: "tedType",
					type: "dropdown",
					label: "Type",
					required: true,
					value: $scope.table.columns[index].type,
					items: $scope.types,
				},
				{
					id: "teiLength",
					type: "input",
					label: "Length",
					placeholder: "Enter length",
					value: $scope.table.columns[index].length,
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
				},
				{
					id: "tecPrimaryKey",
					type: "checkbox",
					label: "Primary Key",
					value: $scope.table.columns[index].primaryKey || false,
				},
				{
					id: "tecUnique",
					type: "checkbox",
					label: "Unique",
					value: $scope.table.columns[index].unique || false,
				},
				{
					id: "tecNullable",
					type: "checkbox",
					label: "Nullable",
					value: $scope.table.columns[index].nullable || false,
				},
				{
					id: "teiDefaultValue",
					type: "input",
					label: "Default Value",
					placeholder: "Enter value",
					value: $scope.table.columns[index].defaultValue,
				},
				{
					id: "teiPrecision",
					type: "input",
					label: "Precision",
					placeholder: "Enter precision number",
					value: $scope.table.columns[index].precision,
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
				},
				{
					id: "teiScale",
					type: "input",
					label: "Scale",
					placeholder: "Enter scale number",
					value: $scope.table.columns[index].scale,
					inputRules: {
						patterns: ['^[0-9]*$'],
					},
				}],
				[{
					id: "b1",
					type: "emphasized",
					label: "Update",
					whenValid: true,
				},
				{
					id: "b2",
					type: "transparent",
					label: "Cancel",
				}],
				"tableEditor.column.edit",
				"Updating parameter..."
			);
		};

		$scope.deleteColumn = function (index) {
			messageHub.showDialogAsync(
				`Delete ${$scope.table.columns[index].name}?`,
				'This action cannot be undone.',
				[{
					id: 'b1',
					type: 'negative',
					label: 'Delete',
				},
				{
					id: 'b2',
					type: 'transparent',
					label: 'Cancel',
				}],
			).then(function (dialogResponse) {
				if (dialogResponse.data === 'b1') {
					$scope.$apply(function () {
						$scope.table.columns.splice(index, 1);
					});
				}
			});
		};

		$scope.dataParameters = ViewParameters.get();
		if (!$scope.dataParameters.hasOwnProperty('file')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'file' data parameter is missing.";
		} else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'contentType' data parameter is missing.";
		} else $scope.load();
	});