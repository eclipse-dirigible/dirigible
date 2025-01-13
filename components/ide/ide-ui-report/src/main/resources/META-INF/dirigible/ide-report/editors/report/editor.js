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
angular.module('page', ['ideUI', 'ideView', 'ideWorkspace'])
	.controller('PageController', function ($scope, $http, messageHub, $window, workspaceApi, ViewParameters) {
		let contents;
		$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
		$scope.forms = {
			editor: {},
		};
		$scope.state = {
			isBusy: true,
			error: false,
			busyText: 'Loading...',
		};
		$scope.isFileChanged = false;
		$scope.editColumnIndex = 0;
		$scope.editJoinIndex = 0;
		$scope.editConditionIndex = 0;
		$scope.editHavingIndex = 0;
		$scope.editOrderingIndex = 0;
		$scope.editParameterIndex = 0;
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
		$scope.aggregates = [
			{ value: "NONE", label: "NONE" },
			{ value: "COUNT", label: "COUNT" },
			{ value: "SUM", label: "SUM" },
			{ value: "AVG", label: "AVG" },
			{ value: "MIN", label: "MIN" },
			{ value: "MAX", label: "MAX" }
		];
		$scope.operations = [
			{ value: "=", label: "=" },
			{ value: "<>", label: "<>" },
			{ value: ">", label: ">" },
			{ value: ">=", label: ">=" },
			{ value: "<", label: "<" },
			{ value: "<=", label: "<=" },
			{ value: "IS NULL", label: "IS NULL" },
			{ value: "IS NOT NULL", label: "IS NOT NULL" },
			{ value: "BETWEEN", label: "BETWEEN" },
			{ value: "IN", label: "IN" },
			{ value: "LIKE", label: "LIKE" },
			{ value: "NOT LIKE", label: "NOT LIKE" }
		];
		$scope.joins = [
			{ value: "INNER", label: "INNER" },
			{ value: "LEFT", label: "LEFT" },
			{ value: "RIGHT", label: "RIGHT" },
			{ value: "FULL", label: "FULL" }
		];
		$scope.directions = [
			{ value: "ASC", label: "ASC" },
			{ value: "DESC", label: "DESC" }
		];
		$scope.tables = [];
		$scope.tablesMetadata = {};

		let databasesSvcUrl = "/services/data/";

		function uuidv4() {
			return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, c =>
				(+c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> +c / 4).toString(16)
			);
		}

		const snakeToCamel = str =>
			str.toLowerCase().replace(/([-_][a-z])/g, group =>
				group
					.toUpperCase()
					.replace('-', '')
					.replace('_', '')
			);

		angular.element($window).bind("focus", function () {
			messageHub.setFocusedEditor($scope.dataParameters.file);
			messageHub.setStatusCaret('');
		});

		$scope.load = function () {
			if (!$scope.state.error) {
				workspaceApi.loadContent('', $scope.dataParameters.file).then(function (response) {
					if (response.status === 200) {
						if (response.data === '') $scope.report = {};
						else $scope.report = response.data;
						contents = JSON.stringify($scope.report, null, 4);
						$scope.$apply(function () {
							$scope.state.isBusy = false;
						});
					} else {
						$scope.$apply(function () {
							$scope.state.error = true;
							$scope.errorMessage = "There was a problem with loading the file";
							$scope.state.isBusy = false;
						});
					}
				});
			}
			loadDatabasesMetadata();
		};

		function loadDatabasesMetadata() {
			$http.get(databasesSvcUrl)
				.then(function (data) {
					let databases = data.data;
					for (let i = 0; i < databases.length; i++) {
						$http.get(databasesSvcUrl + databases[i] + "/").then(function (data) {
							let datasources = data.data;
							for (let j = 0; j < datasources.length; j++) {
								$http.get(databasesSvcUrl + databases[i] + "/" + datasources[j]).then(function (data) {
									let schemas = data.data.schemas;
									for (let k = 0; k < schemas.length; k++) {
										let schema = schemas[k];
										for (let m = 0; m < schema.tables.length; m++) {
											let tableKey = uuidv4();
											let tableLabel = datasources[j] + ' -> ' + schemas[k].name + ' -> ' + schema.tables[m].name;
											$scope.tables.push({
												value: tableKey,
												label: tableLabel,
											});
											let tableMetadata = {
												'name': schema.tables[m].name,
												'schema': schema.name,
												'datasource': datasources[j],
												'database': databases[i]
											}
											$scope.tablesMetadata[tableKey] = tableMetadata;
										}
									}
								});
							}
						});
					}
				});
		}

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
						$scope.isFileChanged = false;
					});
				} else {
					console.error(`Error saving '${$scope.dataParameters.file}'`);
					messageHub.setStatusError(`Error saving '${$scope.dataParameters.file}'`);
					messageHub.showAlertError('Error while saving the file', 'Please look at the console for more information');
					$scope.$apply(function () {
						$scope.state.isBusy = false;
					});
				}
			});
		}

		$scope.save = function (_keySet, event) {
			$scope.generateQuery();
			if (event) event.preventDefault();
			if ($scope.forms.editor.$valid && !$scope.state.error) {
				$scope.state.busyText = "Saving...";
				$scope.state.isBusy = true;
				contents = JSON.stringify($scope.report, null, 4);
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
					if (isFileChanged) {
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
						if (isFileChanged) $scope.save();
					}
				}
			},
			true,
		);

		$scope.$watch('report', function () {
			if (!$scope.state.error && !$scope.state.isBusy) {
				const report = JSON.stringify($scope.report, null, 4);
				$scope.isFileChanged = contents !== report;
				messageHub.setEditorDirty($scope.dataParameters.file, $scope.isFileChanged);
				$scope.generateQuery();
			}
		}, true);

		// Begin Columns Section ----------------------------------------------------------------------------------
		messageHub.onDidReceiveMessage(
			"reportEditor.column.add",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						if (!$scope.report.columns) $scope.report.columns = [];
						$scope.report.columns.push({
							table: msg.data.formData[0].value,
							alias: msg.data.formData[1].value,
							name: msg.data.formData[2].value,
							type: msg.data.formData[3].value,
							aggregate: msg.data.formData[4].value,
							select: msg.data.formData[5].value,
							grouping: msg.data.formData[6].value,
						});
					});
				}
				messageHub.hideFormDialog("reportEditorAddColumn");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"reportEditor.column.edit",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.report.columns[$scope.editColumnIndex].table = msg.data.formData[0].value;
						$scope.report.columns[$scope.editColumnIndex].alias = msg.data.formData[1].value;
						$scope.report.columns[$scope.editColumnIndex].name = msg.data.formData[2].value;
						$scope.report.columns[$scope.editColumnIndex].type = msg.data.formData[3].value;
						$scope.report.columns[$scope.editColumnIndex].aggregate = msg.data.formData[4].value;
						$scope.report.columns[$scope.editColumnIndex].select = msg.data.formData[5].value;
						$scope.report.columns[$scope.editColumnIndex].grouping = msg.data.formData[6].value;
					});
				}
				messageHub.hideFormDialog("reportEditorEditColumn");
			},
			true
		);

		$scope.addColumn = function () {
			let excludedAliases = [];
			let excludedNames = [];
			if ($scope.report.columns) {
				for (let i = 0; i < $scope.report.columns.length; i++) {
					excludedAliases.push($scope.report.columns[i].alias);
				}
				for (let i = 0; i < $scope.report.columns.length; i++) {
					excludedNames.push($scope.report.columns[i].name);
				}
			}
			messageHub.showFormDialog(
				"reportEditorAddColumn",
				"Add column",
				[{
					id: "teiTable",
					type: "input",
					label: "Table Alias",
					required: true,
					placeholder: "Enter table alias",
					minlength: 1,
					maxlength: 255,
					value: '',
				},
				{
					id: "teiAlias",
					type: "input",
					label: "Column Alias",
					required: true,
					placeholder: "Enter column alias",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					value: '',
				},
				{
					id: "teiName",
					type: "input",
					label: "Column Name",
					required: true,
					placeholder: "Enter column name",
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
					label: "Column Type",
					required: true,
					value: $scope.types[0].value,
					items: $scope.types,
				},
				{
					id: "tedAggregate",
					type: "dropdown",
					label: "Aggregate Function",
					required: true,
					value: $scope.aggregates[0].value,
					items: $scope.aggregates,
				},
				{
					id: "tecSelect",
					type: "checkbox",
					label: "Select",
					value: false,
				},
				{
					id: "tecGrouping",
					type: "checkbox",
					label: "Grouping",
					value: false,
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
				"reportEditor.column.add",
				"Adding parameter..."
			);
		};

		$scope.editColumn = function (index) {
			$scope.editColumnIndex = index;
			let excludedAliases = [];
			for (let i = 0; i < $scope.report.columns.length; i++) {
				if (i !== index)
					excludedAliases.push($scope.report.columns[i].alias);
			}
			let excludedNames = [];
			for (let i = 0; i < $scope.report.columns.length; i++) {
				if (i !== index)
					excludedNames.push($scope.report.columns[i].name);
			}
			messageHub.showFormDialog(
				"reportEditorEditColumn",
				"Edit column",
				[{
					id: "teiTable",
					type: "input",
					label: "Table Alias",
					required: true,
					placeholder: "Enter table alias",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					value: $scope.report.columns[index].table,
				},
				{
					id: "teiAlias",
					type: "input",
					label: "Column Alias",
					required: true,
					placeholder: "Enter column alias",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					value: $scope.report.columns[index].alias,
				},
				{
					id: "teiName",
					type: "input",
					label: "Column Name",
					required: true,
					placeholder: "Enter column name",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
					},
					value: $scope.report.columns[index].name,
				},
				{
					id: "tedType",
					type: "dropdown",
					label: "Column Type",
					required: true,
					value: $scope.report.columns[index].type,
					items: $scope.types,
				},
				{
					id: "tedAggregate",
					type: "dropdown",
					label: "Aggregate Function",
					required: true,
					value: $scope.report.columns[index].aggregate,
					items: $scope.aggregates,
				},
				{
					id: "tecSelect",
					type: "checkbox",
					label: "Select",
					value: $scope.report.columns[index].select || false,
				},
				{
					id: "tecGrouping",
					type: "checkbox",
					label: "Grouping",
					value: $scope.report.columns[index].grouping || false,
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
				"reportEditor.column.edit",
				"Updating parameter..."
			);
		};

		$scope.deleteColumn = function (index) {
			messageHub.showDialogAsync(
				`Delete ${$scope.report.columns[index].name}?`,
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
						$scope.report.columns.splice(index, 1);
					});
				}
			});
		};
		// End Columns Section ------------------------------------------------------------------------------------

		// Begin Joins Section ------------------------------------------------------------------------------------
		messageHub.onDidReceiveMessage(
			"reportEditor.join.add",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						if (!$scope.report.joins) $scope.report.joins = [];
						$scope.report.joins.push({
							alias: msg.data.formData[0].value,
							name: msg.data.formData[1].value,
							type: msg.data.formData[2].value,
							condition: msg.data.formData[3].value
						});
					});
				}
				messageHub.hideFormDialog("reportEditorAddJoin");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"reportEditor.join.edit",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.report.joins[$scope.editJoinIndex].alias = msg.data.formData[0].value;
						$scope.report.joins[$scope.editJoinIndex].name = msg.data.formData[1].value;
						$scope.report.joins[$scope.editJoinIndex].type = msg.data.formData[2].value;
						$scope.report.joins[$scope.editJoinIndex].condition = msg.data.formData[3].value;
					});
				}
				messageHub.hideFormDialog("reportEditorEditJoin");
			},
			true
		);

		$scope.addJoin = function () {
			let excludedAliases = [];
			let excludedNames = [];
			if ($scope.report.joins) {
				for (let i = 0; i < $scope.report.joins.length; i++) {
					excludedAliases.push($scope.report.joins[i].alias);
				}
				for (let i = 0; i < $scope.report.joins.length; i++) {
					excludedNames.push($scope.report.joins[i].name);
				}
			}
			messageHub.showFormDialog(
				"reportEditorAddJoin",
				"Add join",
				[{
					id: "teiTable",
					type: "input",
					label: "Table Alias",
					required: true,
					placeholder: "Enter table alias",
					minlength: 1,
					maxlength: 255,
					value: '',
				},
				{
					id: "teiName",
					type: "input",
					label: "Table Name",
					required: true,
					placeholder: "Enter table name",
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
					label: "Join Type",
					required: true,
					value: $scope.types[0].value,
					items: $scope.joins,
				},
				{
					id: "teiCondition",
					type: "input",
					label: "Join Condition",
					required: true,
					placeholder: "Enter join condition",
					minlength: 1,
					maxlength: 255,
					value: '',
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
				"reportEditor.join.add",
				"Adding parameter..."
			);
		};

		$scope.editJoin = function (index) {
			$scope.editJoinIndex = index;
			let excludedAliases = [];
			for (let i = 0; i < $scope.report.joins.length; i++) {
				if (i !== index)
					excludedAliases.push($scope.report.joins[i].alias);
			}
			let excludedNames = [];
			for (let i = 0; i < $scope.report.joins.length; i++) {
				if (i !== index)
					excludedNames.push($scope.report.joins[i].name);
			}
			messageHub.showFormDialog(
				"reportEditorEditJoin",
				"Edit join",
				[{
					id: "teiTable",
					type: "input",
					label: "Table Alias",
					required: true,
					placeholder: "Enter table alias",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					value: $scope.report.joins[index].alias,
				},
				{
					id: "teiName",
					type: "input",
					label: "Table Name",
					required: true,
					placeholder: "Enter table name",
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
					},
					value: $scope.report.joins[index].name,
				},
				{
					id: "tedType",
					type: "dropdown",
					label: "Join Type",
					required: true,
					value: $scope.report.joins[index].type,
					items: $scope.joins,
				},
				{
					id: "teiCondition",
					type: "input",
					label: "Join Condition",
					required: true,
					placeholder: "Enter join condition",
					minlength: 1,
					maxlength: 255,
					value: $scope.report.joins[index].condition,
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
				"reportEditor.join.edit",
				"Updating parameter..."
			);
		};

		$scope.deleteJoin = function (index) {
			messageHub.showDialogAsync(
				`Delete ${$scope.report.joins[index].name}?`,
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
						$scope.report.joins.splice(index, 1);
					});
				}
			});
		};
		// End Joins Section --------------------------------------------------------------------------------------

		// Begin Conditions Section -------------------------------------------------------------------------------
		messageHub.onDidReceiveMessage(
			"reportEditor.condition.add",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						if (!$scope.report.conditions) $scope.report.conditions = [];
						$scope.report.conditions.push({
							left: msg.data.formData[0].value,
							operation: msg.data.formData[1].value,
							right: msg.data.formData[2].value
						});
					});
				}
				messageHub.hideFormDialog("reportEditorAddCondition");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"reportEditor.condition.edit",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.report.conditions[$scope.editConditionIndex].left = msg.data.formData[0].value;
						$scope.report.conditions[$scope.editConditionIndex].operation = msg.data.formData[1].value;
						$scope.report.conditions[$scope.editConditionIndex].right = msg.data.formData[2].value;
					});
				}
				messageHub.hideFormDialog("reportEditorEditCondition");
			},
			true
		);

		$scope.addCondition = function () {
			messageHub.showFormDialog(
				"reportEditorAddCondition",
				"Add condition",
				[{
					id: "teiLeft",
					type: "input",
					label: "Left",
					required: true,
					placeholder: "Enter left operand",
					minlength: 1,
					maxlength: 255,
					value: '',
				},
				{
					id: "tedOperation",
					type: "dropdown",
					label: "Operation",
					required: true,
					value: $scope.operations[0].value,
					items: $scope.operations,
				},
				{
					id: "teiRight",
					type: "input",
					label: "Right",
					required: true,
					placeholder: "Enter right operand",
					minlength: 1,
					maxlength: 255,
					value: '',
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
				"reportEditor.condition.add",
				"Adding parameter..."
			);
		};

		$scope.editCondition = function (index) {
			$scope.editConditionIndex = index;
			messageHub.showFormDialog(
				"reportEditorEditCondition",
				"Edit condition",
				[{
					id: "teiLeft",
					type: "input",
					label: "Left",
					required: true,
					placeholder: "Enter left operand",
					minlength: 1,
					maxlength: 255,
					value: $scope.report.conditions[index].left,
				},
				{
					id: "tedOperation",
					type: "dropdown",
					label: "Type",
					required: true,
					value: $scope.report.conditions[index].operation,
					items: $scope.operations,
				},
				{
					id: "teiRight",
					type: "input",
					label: "Right",
					required: true,
					placeholder: "Enter right operand",
					minlength: 1,
					maxlength: 255,
					value: $scope.report.conditions[index].right,
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
				"reportEditor.condition.edit",
				"Updating parameter..."
			);
		};

		$scope.deleteCondition = function (index) {
			messageHub.showDialogAsync(
				`Delete ${$scope.report.conditions[index].name}?`,
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
						$scope.report.conditions.splice(index, 1);
					});
				}
			});
		};
		// End Conditions Section ---------------------------------------------------------------------------------

		// Begin Havings Section ----------------------------------------------------------------------------------
		messageHub.onDidReceiveMessage(
			"reportEditor.having.add",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						if (!$scope.report.havings) $scope.report.havings = [];
						$scope.report.havings.push({
							left: msg.data.formData[0].value,
							operation: msg.data.formData[1].value,
							right: msg.data.formData[2].value
						});
					});
				}
				messageHub.hideFormDialog("reportEditorAddHaving");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"reportEditor.having.edit",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.report.havings[$scope.editHavingIndex].left = msg.data.formData[0].value;
						$scope.report.havings[$scope.editHavingIndex].operation = msg.data.formData[1].value;
						$scope.report.havings[$scope.editHavingIndex].right = msg.data.formData[2].value;
					});
				}
				messageHub.hideFormDialog("reportEditorEditHaving");
			},
			true
		);

		$scope.addHaving = function () {
			messageHub.showFormDialog(
				"reportEditorAddHaving",
				"Add having",
				[{
					id: "teiLeft",
					type: "input",
					label: "Left",
					required: true,
					placeholder: "Enter left operand",
					minlength: 1,
					maxlength: 255,
					value: '',
				},
				{
					id: "tedOperation",
					type: "dropdown",
					label: "Operation",
					required: true,
					value: $scope.operations[0].value,
					items: $scope.operations,
				},
				{
					id: "teiRight",
					type: "input",
					label: "Right",
					required: true,
					placeholder: "Enter right operand",
					minlength: 1,
					maxlength: 255,
					value: '',
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
				"reportEditor.having.add",
				"Adding parameter..."
			);
		};

		$scope.editHaving = function (index) {
			$scope.editHavingIndex = index;
			messageHub.showFormDialog(
				"reportEditorEditHaving",
				"Edit having",
				[{
					id: "teiLeft",
					type: "input",
					label: "Left",
					required: true,
					placeholder: "Enter left operand",
					minlength: 1,
					maxlength: 255,
					value: $scope.report.havings[index].left,
				},
				{
					id: "tedOperation",
					type: "dropdown",
					label: "Type",
					required: true,
					value: $scope.report.havings[index].operation,
					items: $scope.operations,
				},
				{
					id: "teiRight",
					type: "input",
					label: "Right",
					required: true,
					placeholder: "Enter right operand",
					minlength: 1,
					maxlength: 255,
					value: $scope.report.havings[index].right,
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
				"reportEditor.having.edit",
				"Updating parameter..."
			);
		};

		$scope.deleteHaving = function (index) {
			messageHub.showDialogAsync(
				`Delete ${$scope.report.havings[index].name}?`,
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
						$scope.report.havings.splice(index, 1);
					});
				}
			});
		};
		// End Havings Section ------------------------------------------------------------------------------------

		// Begin Orderings Section --------------------------------------------------------------------------------
		messageHub.onDidReceiveMessage(
			"reportEditor.ordering.add",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						if (!$scope.report.orderings) $scope.report.orderings = [];
						$scope.report.orderings.push({
							column: msg.data.formData[0].value,
							direction: msg.data.formData[1].value,
						});
					});
				}
				messageHub.hideFormDialog("reportEditorAddOrdering");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"reportEditor.ordering.edit",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.report.orderings[$scope.editOrderingIndex].column = msg.data.formData[0].value;
						$scope.report.orderings[$scope.editOrderingIndex].direction = msg.data.formData[1].value;
					});
				}
				messageHub.hideFormDialog("reportEditorEditOrdering");
			},
			true
		);

		$scope.addOrdering = function () {
			messageHub.showFormDialog(
				"reportEditorAddOrdering",
				"Add ordering",
				[{
					id: "teiColumn",
					type: "input",
					label: "Column",
					required: true,
					placeholder: "Enter column",
					minlength: 1,
					maxlength: 255,
					value: '',
				},
				{
					id: "tedDirection",
					type: "dropdown",
					label: "Direction",
					required: true,
					value: $scope.directions[0].value,
					items: $scope.directions,
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
				"reportEditor.ordering.add",
				"Adding parameter..."
			);
		};

		$scope.editOrdering = function (index) {
			$scope.editOrderingIndex = index;
			messageHub.showFormDialog(
				"reportEditorEditOrdering",
				"Edit ordering",
				[{
					id: "teiColumn",
					type: "input",
					label: "Column",
					required: true,
					placeholder: "Enter column",
					minlength: 1,
					maxlength: 255,
					value: $scope.report.orderings[index].column,
				},
				{
					id: "tedDirection",
					type: "dropdown",
					label: "Direction",
					required: true,
					value: $scope.report.orderings[index].direction,
					items: $scope.directions,
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
				"reportEditor.ordering.edit",
				"Updating parameter..."
			);
		};

		$scope.deleteOrdering = function (index) {
			messageHub.showDialogAsync(
				`Delete ${$scope.report.orderings[index].name}?`,
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
						$scope.report.orderings.splice(index, 1);
					});
				}
			});
		};
		// End Orderings Section ----------------------------------------------------------------------------------

		// Begin Parameters Section -------------------------------------------------------------------------------
		messageHub.onDidReceiveMessage(
			"reportEditor.parameter.add",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						if (!$scope.report.parameters) $scope.report.parameters = [];
						$scope.report.parameters.push({
							name: msg.data.formData[0].value,
							type: msg.data.formData[1].value,
							initial: msg.data.formData[2].value,
						});
					});
				}
				messageHub.hideFormDialog("reportEditorAddParameter");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"reportEditor.parameter.edit",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						$scope.report.parameters[$scope.editParameterIndex].name = msg.data.formData[0].value;
						$scope.report.parameters[$scope.editParameterIndex].type = msg.data.formData[1].value;
						$scope.report.parameters[$scope.editParameterIndex].initial = msg.data.formData[2].value;
					});
				}
				messageHub.hideFormDialog("reportEditorEditParameter");
			},
			true
		);

		$scope.addParameter = function () {
			let excludedNames = [];
			for (let i = 0; i < $scope.report.columns.length; i++) {
				excludedNames.push($scope.report.columns[i].name);
			}
			messageHub.showFormDialog(
				"reportEditorAddParameter",
				"Add parameter",
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
					id: "teiInitial",
					type: "input",
					label: "Initial",
					required: true,
					placeholder: "Enter initial value",
					minlength: 1,
					maxlength: 255,
					value: '',
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
				"reportEditor.parameter.add",
				"Adding parameter..."
			);
		};

		$scope.editParameter = function (index) {
			$scope.editParameterIndex = index;
			let excludedNames = [];
			for (let i = 0; i < $scope.report.columns.length; i++) {
				if (i !== index)
					excludedNames.push($scope.report.columns[i].name);
			}
			messageHub.showFormDialog(
				"reportEditorEditParameter",
				"Edit parameter",
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
					value: $scope.report.parameters[index].name,
				},
				{
					id: "tedType",
					type: "dropdown",
					label: "Type",
					required: true,
					value: $scope.report.parameters[index].type,
					items: $scope.types,
				},
				{
					id: "teiInitial",
					type: "input",
					label: "Initial",
					required: true,
					placeholder: "Enter initial value",
					minlength: 1,
					maxlength: 255,
					value: $scope.report.parameters[index].initial,
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
				"reportEditor.parameter.edit",
				"Updating parameter..."
			);
		};

		$scope.deleteParameter = function (index) {
			messageHub.showDialogAsync(
				`Delete ${$scope.report.parameters[index].name}?`,
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
						$scope.report.parameters.splice(index, 1);
					});
				}
			});
		};
		// End Parameters Section ---------------------------------------------------------------------------------

		$scope.generateQuery = function () {
			$scope.query = "SELECT ";
			if ($scope.report.columns) {
				for (let i = 0; i < $scope.report.columns.length; i++) {
					if ($scope.report.columns[i].select === true) {
						if ($scope.report.columns[i].aggregate !== undefined && $scope.report.columns[i].aggregate !== "NONE") {
							$scope.query += $scope.report.columns[i].aggregate + "(";
						}
						$scope.query += $scope.report.columns[i].table + "." + $scope.report.columns[i].name;
						if ($scope.report.columns[i].aggregate !== undefined && $scope.report.columns[i].aggregate !== "NONE") {
							$scope.query += ")";
						}
						$scope.query += ` as "${$scope.report.columns[i].alias}", `;
					}
				}
			}
			if ($scope.query.substring($scope.query.length - 2) === ', ')
				$scope.query = $scope.query.substring(0, $scope.query.length - 2);
			if ($scope.report.table && $scope.report.alias)
				$scope.query += "\nFROM " + $scope.report.table + " as " + $scope.report.alias;

			if ($scope.report.joins) {
				for (let i = 0; i < $scope.report.joins.length; i++) {
					$scope.query += "\n  " + $scope.report.joins[i].type + " JOIN " + $scope.report.joins[i].name + " " + $scope.report.joins[i].alias + " ON " + $scope.report.joins[i].condition;
				}
			}

			if ($scope.report.conditions) {
				$scope.query += "\nWHERE ";
				for (let i = 0; i < $scope.report.conditions.length; i++) {
					if (i > 0) { $scope.query += ' AND ' }
					$scope.query += $scope.report.conditions[i].left + " " + $scope.report.conditions[i].operation + " " + $scope.report.conditions[i].right;
				}
			}

			if ($scope.report.columns) {
				let g = false;
				for (let i = 0; i < $scope.report.columns.length; i++) {
					if ($scope.report.columns[i].grouping === true) {
						if (!g) {
							$scope.query += "\nGROUP BY ";
							g = true;
						}
						$scope.query += $scope.report.columns[i].table + "." + $scope.report.columns[i].name + ', ';
					}
				}
			}
			if ($scope.query.substring($scope.query.length - 2) === ', ')
				$scope.query = $scope.query.substring(0, $scope.query.length - 2);

			if ($scope.report.havings) {
				$scope.query += "\nHAVING ";
				for (let i = 0; i < $scope.report.havings.length; i++) {
					if (i > 0) { $scope.query += ', ' }
					$scope.query += $scope.report.havings[i].left + " " + $scope.report.havings[i].operation + " " + $scope.report.havings[i].right;
				}
			}

			if ($scope.report.orderings) {
				$scope.query += "\nORDER BY ";
				for (let i = 0; i < $scope.report.orderings.length; i++) {
					if (i > 0) { $scope.query += ', ' }
					$scope.query += $scope.report.orderings[i].column + " " + $scope.report.orderings[i].direction;
				}
			}
			$scope.report.query = $scope.query;
		}

		$scope.dataParameters = ViewParameters.get();
		if (!$scope.dataParameters.hasOwnProperty('file')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'file' data parameter is missing.";
		} else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'contentType' data parameter is missing.";
		} else $scope.load();

		// Begin Base Table Section -------------------------------------------------------------------------------

		messageHub.onDidReceiveMessage(
			"reportEditor.base.table.set",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						let tableMetadataPointer = $scope.tablesMetadata[msg.data.formData[0].value];
						$http.get(databasesSvcUrl + tableMetadataPointer.database + "/" + tableMetadataPointer.datasource + "/" + tableMetadataPointer.schema + "/" + tableMetadataPointer.name).then(function (data) {
							let tableMetadata = data.data;
							$scope.report.alias = snakeToCamel(tableMetadata.name);
							$scope.report.table = tableMetadata.name;
							if (!$scope.report.columns) $scope.report.columns = [];
							for (let i = 0; i < tableMetadata.columns.length; i++) {
								$scope.report.columns.push({
									table: snakeToCamel(tableMetadata.name),
									alias: snakeToCamel(tableMetadata.columns[i].name),
									name: tableMetadata.columns[i].name,
									type: tableMetadata.columns[i].type,
									aggregate: "NONE",
									select: true,
									grouping: false
								});
							}
						});
					});
				}
				messageHub.hideFormDialog("reportEditorSetTable");
			},
			true
		);

		$scope.setBaseTable = function () {
			messageHub.showFormDialog(
				"reportEditorSetTable",
				"Set from tables",
				[{
					id: "tedTable",
					type: "dropdown",
					label: "Table",
					required: true,
					value: $scope.tables[0].value,
					items: $scope.tables,
				}],
				[{
					id: "b1",
					type: "emphasized",
					label: "Set",
					whenValid: true,
				},
				{
					id: "b2",
					type: "transparent",
					label: "Cancel",
				}],
				"reportEditor.base.table.set",
				"Setting parameter..."
			);
		};

		// End Base Table Section ---------------------------------------------------------------------------------

		// Begin Add from Tables Section -------------------------------------------------------------------------------

		messageHub.onDidReceiveMessage(
			"reportEditor.join.add.table",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					$scope.$apply(function () {
						let tableMetadataPointer = $scope.tablesMetadata[msg.data.formData[0].value];
						$http.get(databasesSvcUrl + tableMetadataPointer.database + "/" + tableMetadataPointer.datasource + "/" + tableMetadataPointer.schema + "/" + tableMetadataPointer.name).then(function (data) {
							let tableMetadata = data.data;

							if (!$scope.report.joins) $scope.report.joins = [];
							$scope.report.joins.push({
								alias: snakeToCamel(tableMetadata.name),
								name: tableMetadata.name,
								type: "INNER",
								condition: "<DEFINE JOIN CONDITION HERE>"
							});
							if (!$scope.report.columns) $scope.report.columns = [];
							for (let i = 0; i < tableMetadata.columns.length; i++) {
								$scope.report.columns.push({
									table: snakeToCamel(tableMetadata.name),
									alias: snakeToCamel(tableMetadata.columns[i].name),
									name: tableMetadata.columns[i].name,
									type: tableMetadata.columns[i].type,
									aggregate: "NONE",
									select: true,
									grouping: false
								});
							}
						});
					});
				}
				messageHub.hideFormDialog("reportEditorAddJoinTable");
			},
			true
		);

		$scope.addFromTables = function () {
			messageHub.showFormDialog(
				"reportEditorAddJoinTable",
				"Add from tables",
				[{
					id: "tedTable",
					type: "dropdown",
					label: "Table",
					required: true,
					value: $scope.tables[0].value,
					items: $scope.tables,
				}],
				[{
					id: "b1",
					type: "emphasized",
					label: "Set",
					whenValid: true,
				},
				{
					id: "b2",
					type: "transparent",
					label: "Cancel",
				}],
				"reportEditor.join.add.table",
				"Adding join..."
			);
		};

		// End Add from Tables Section ---------------------------------------------------------------------------------


	});