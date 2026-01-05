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
angular.module('page', ['blimpKit', 'platformView', 'platformShortcuts', 'WorkspaceService']).controller('PageController', ($scope, $window, $http, WorkspaceService, ViewParameters, ButtonStates) => {
	const statusBarHub = new StatusBarHub();
	const workspaceHub = new WorkspaceHub();
	const layoutHub = new LayoutHub();
	const dialogHub = new DialogHub();
	let contents;
	$scope.changed = false;
	$scope.nameErrorMessage = 'Allowed characters include all letters, numbers, \'_\', \'-\', \'.\', \':\' and \'"\'. Maximum length is 255.';
	$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
	$scope.forms = {
		editor: {},
	};
	$scope.state = {
		isBusy: true,
		error: false,
		busyText: 'Loading...',
	};
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

	angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

	function getTranslationId(str) {
		return `${str.replaceAll(' ', '').replaceAll('_', '').replaceAll('.', '').replaceAll(':', '')}`;
	}
	// Same migration happens in generateUtils.js
	function migrateReport(report) {
		if (!report.hasOwnProperty('tId')) {
			report['tId'] = getTranslationId(report.alias);
			report['label'] = report.alias;
			$scope.fileChanged();
		}
		for (let i = 0; i < report.columns.length; i++) {
			if (!report.columns[i].hasOwnProperty('tId')) {
				report.columns[i]['tId'] = getTranslationId(report.columns[i]['alias']);
				report.columns[i]['label'] = report.columns[i]['alias'];
				$scope.fileChanged();
			}
		}
		return report;
	}

	const loadFileContents = () => {
		if (!$scope.state.error) {
			$scope.state.isBusy = true;
			WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
				$scope.$evalAsync(() => {
					if (response.data === '') $scope.report = {};
					else $scope.report = migrateReport(response.data);
					contents = JSON.stringify($scope.report, null, 4);
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
				saveContents(JSON.stringify($scope.report, null, 4));
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

	$scope.fileChanged = () => {
		if (!$scope.changed) {
			$scope.changed = true;
			layoutHub.setEditorDirty({
				path: $scope.dataParameters.filePath,
				dirty: $scope.changed,
			});
		}
	};

	$scope.$watch('report', () => {
		if (!$scope.state.error && !$scope.state.isBusy) {
			const isDirty = contents !== JSON.stringify($scope.report, null, 4);
			if ($scope.changed !== isDirty) {
				$scope.fileChanged();
				$scope.generateQuery();
			}
		}
	}, true);

	// Begin Columns Section ------------------------------------------------------------------------------------

	$scope.addColumn = () => {
		const excludedAliases = [];
		const excludedNames = [];
		if ($scope.report.columns) {
			for (let i = 0; i < $scope.report.columns.length; i++) {
				excludedAliases.push($scope.report.columns[i].alias);
			}
			for (let i = 0; i < $scope.report.columns.length; i++) {
				excludedNames.push($scope.report.columns[i].name);
			}
		}
		dialogHub.showFormDialog({
			title: 'Add column',
			form: {
				'teiLabel': {
					label: 'Label',
					controlType: 'input',
					placeholder: "Enter label",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					focus: true,
					required: true
				},
				'teiTable': {
					label: 'Table Alias',
					controlType: 'input',
					placeholder: "Enter table alias",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					required: true
				},
				'teiAlias': {
					label: 'Column Alias',
					controlType: 'input',
					placeholder: "Enter column alias",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					required: true
				},
				'teiName': {
					label: 'Column Name',
					controlType: 'input',
					placeholder: "Enter column name",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
					},
					required: true
				},
				'tedType': {
					label: 'Column Type',
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.types,
					value: $scope.types[0].value,
					required: true,
				},
				'tedAggregate': {
					label: "Aggregate Function",
					placeholder: 'Select function',
					controlType: 'dropdown',
					options: $scope.aggregates,
					value: $scope.aggregates[0].value,
					required: true,
				},
				'tecSelect': {
					label: 'Select',
					controlType: 'checkbox',
					value: false,
				},
				'tecGrouping': {
					label: 'Grouping',
					controlType: 'checkbox',
					value: false,
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					if (!$scope.report.columns) $scope.report.columns = [];
					$scope.report.columns.push({
						tId: getTranslationId(form['teiAlias']),
						label: form['teiLabel'],
						table: form['teiTable'],
						alias: form['teiAlias'],
						name: form['teiName'],
						type: form['tedType'],
						aggregate: form['tedAggregate'],
						select: form['tecSelect'],
						grouping: form['tecGrouping'],
					});
				});
			}
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
		const excludedAliases = [];
		for (let i = 0; i < $scope.report.columns.length; i++) {
			if (i !== index)
				excludedAliases.push($scope.report.columns[i].alias);
		}
		const excludedNames = [];
		for (let i = 0; i < $scope.report.columns.length; i++) {
			if (i !== index)
				excludedNames.push($scope.report.columns[i].name);
		}
		dialogHub.showFormDialog({
			title: 'Add column',
			form: {
				'teiLabel': {
					label: 'Label',
					controlType: 'input',
					placeholder: "Enter label",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					value: $scope.report.columns[index].label,
					focus: true,
					required: true
				},
				'teiTable': {
					label: 'Table Alias',
					controlType: 'input',
					placeholder: "Enter table alias",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					value: $scope.report.columns[index].table,
					required: true
				},
				'teiAlias': {
					label: 'Column Alias',
					controlType: 'input',
					placeholder: "Enter column alias",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					value: $scope.report.columns[index].alias,
					required: true
				},
				'teiName': {
					label: 'Column Name',
					controlType: 'input',
					placeholder: "Enter column name",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
					},
					value: $scope.report.columns[index].name,
					required: true
				},
				'tedType': {
					label: 'Column Type',
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.types,
					value: $scope.report.columns[index].type,
					required: true,
				},
				'tedAggregate': {
					label: "Aggregate Function",
					placeholder: 'Select function',
					controlType: 'dropdown',
					options: $scope.aggregates,
					value: $scope.report.columns[index].aggregate,
					required: true,
				},
				'tecSelect': {
					label: 'Select',
					controlType: 'checkbox',
					value: $scope.report.columns[index].select || false,
				},
				'tecGrouping': {
					label: 'Grouping',
					controlType: 'checkbox',
					value: $scope.report.columns[index].grouping || false,
				},
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					$scope.report.columns[$scope.editColumnIndex].tId = getTranslationId(form['teiAlias']);
					$scope.report.columns[$scope.editColumnIndex].label = form['teiLabel'];
					$scope.report.columns[$scope.editColumnIndex].table = form['teiTable'];
					$scope.report.columns[$scope.editColumnIndex].alias = form['teiAlias'];
					$scope.report.columns[$scope.editColumnIndex].name = form['teiName'];
					$scope.report.columns[$scope.editColumnIndex].type = form['tedType'];
					$scope.report.columns[$scope.editColumnIndex].aggregate = form['tedAggregate'];
					$scope.report.columns[$scope.editColumnIndex].select = form['tecSelect'];
					$scope.report.columns[$scope.editColumnIndex].grouping = form['tecGrouping'];
				});
			}
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
			title: `Delete ${$scope.report.columns[index].name}?`,
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
					$scope.report.columns.splice(index, 1);
				});
			}
		});
	};
	// End Columns Section ------------------------------------------------------------------------------------

	// Begin Joins Section ------------------------------------------------------------------------------------
	$scope.addJoin = () => {
		const excludedAliases = [];
		const excludedNames = [];
		if ($scope.report.joins) {
			for (let i = 0; i < $scope.report.joins.length; i++) {
				excludedAliases.push($scope.report.joins[i].alias);
			}
			for (let i = 0; i < $scope.report.joins.length; i++) {
				excludedNames.push($scope.report.joins[i].name);
			}
		}
		dialogHub.showFormDialog({
			title: 'Add join',
			form: {
				'teiTable': {
					label: 'Table Alias',
					controlType: 'input',
					placeholder: "Enter table alias",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					focus: true,
					required: true
				},
				'teiName': {
					label: "Table Name",
					controlType: 'input',
					placeholder: "Enter table name",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					required: true
				},
				'tedType': {
					label: "Join Type",
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.joins,
					value: $scope.types[0].value,
					required: true,
				},
				'teiCondition': {
					label: "Join Condition",
					controlType: 'input',
					placeholder: "Enter join condition",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					required: true
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					if (!$scope.report.joins) $scope.report.joins = [];
					$scope.report.joins.push({
						alias: form['teiTable'],
						name: form['teiName'],
						type: form['tedType'],
						condition: form['teiCondition']
					});
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'New join error',
				message: 'There was an error while adding the new join.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.editJoin = (index) => {
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
		dialogHub.showFormDialog({
			title: 'Add join',
			form: {
				'teiTable': {
					label: 'Table Alias',
					controlType: 'input',
					placeholder: "Enter table alias",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					value: $scope.report.joins[index].alias,
					focus: true,
					required: true
				},
				'teiName': {
					label: "Table Name",
					controlType: 'input',
					placeholder: "Enter table name",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedAliases,
					},
					value: $scope.report.joins[index].name,
					required: true
				},
				'tedType': {
					label: "Join Type",
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.joins,
					value: $scope.report.joins[index].type,
					required: true,
				},
				'teiCondition': {
					label: "Join Condition",
					controlType: 'input',
					placeholder: "Enter join condition",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					value: $scope.report.joins[index].condition,
					required: true
				},
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					$scope.report.joins[$scope.editJoinIndex].alias = form['teiTable'];
					$scope.report.joins[$scope.editJoinIndex].name = form['teiName'];
					$scope.report.joins[$scope.editJoinIndex].type = form['tedType'];
					$scope.report.joins[$scope.editJoinIndex].condition = form['teiCondition'];
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Join update error',
				message: 'There was an error while updating the join.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.deleteJoin = (index) => {
		dialogHub.showDialog({
			title: `Delete ${$scope.report.joins[index].name}?`,
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
					$scope.report.joins.splice(index, 1);
				});
			}
		});
	};
	// End Joins Section --------------------------------------------------------------------------------------

	// Begin Conditions Section -------------------------------------------------------------------------------
	$scope.addCondition = () => {
		dialogHub.showFormDialog({
			title: 'Add condition',
			form: {
				'teiLeft': {
					label: 'Left',
					controlType: 'input',
					placeholder: "Enter left operand",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					focus: true,
					required: true
				},
				'tedOperation': {
					label: 'Operation',
					placeholder: 'Select operation',
					controlType: 'dropdown',
					options: $scope.operations,
					value: $scope.operations[0].value,
					required: true,
				},
				'teiRight': {
					label: "Right",
					controlType: 'input',
					placeholder: "Enter right operand",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					required: true
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					if (!$scope.report.conditions) $scope.report.conditions = [];
					$scope.report.conditions.push({
						left: form['teiLeft'],
						operation: form['tedOperation'],
						right: form['teiRight']
					});
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'New condition error',
				message: 'There was an error while adding the new condition.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.editCondition = (index) => {
		$scope.editConditionIndex = index;
		dialogHub.showFormDialog({
			title: 'Edit condition',
			form: {
				'teiLeft': {
					label: 'Left',
					controlType: 'input',
					placeholder: "Enter left operand",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					value: $scope.report.conditions[index].left,
					focus: true,
					required: true
				},
				'tedOperation': {
					label: 'Operation',
					placeholder: 'Select operation',
					controlType: 'dropdown',
					options: $scope.operations,
					value: $scope.report.conditions[index].operation,
					required: true,
				},
				'teiRight': {
					label: "Right",
					controlType: 'input',
					placeholder: "Enter right operand",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					value: $scope.report.conditions[index].right,
					required: true
				},
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					$scope.report.conditions[$scope.editConditionIndex].left = form['teiLeft'];
					$scope.report.conditions[$scope.editConditionIndex].operation = form['tedOperation'];
					$scope.report.conditions[$scope.editConditionIndex].right = form['teiRight'];
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Condition update error',
				message: 'There was an error while updating the condition.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.deleteCondition = (index) => {
		dialogHub.showDialog({
			title: `Delete ${$scope.report.conditions[index].name}?`,
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
					$scope.report.conditions.splice(index, 1);
				});
			}
		});
	};
	// End Conditions Section ---------------------------------------------------------------------------------

	// Begin Havings Section ----------------------------------------------------------------------------------
	$scope.addHaving = () => {
		dialogHub.showFormDialog({
			title: 'Add having',
			form: {
				'teiLeft': {
					label: 'Left',
					controlType: 'input',
					placeholder: "Enter left operand",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					focus: true,
					required: true
				},
				'tedOperation': {
					label: 'Operation',
					placeholder: 'Select operation',
					controlType: 'dropdown',
					options: $scope.operations,
					value: $scope.operations[0].value,
					required: true,
				},
				'teiRight': {
					label: "Right",
					controlType: 'input',
					placeholder: "Enter right operand",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					required: true
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					if (!$scope.report.havings) $scope.report.havings = [];
					$scope.report.havings.push({
						left: form['teiLeft'],
						operation: form['tedOperation'],
						right: form['teiRight']
					});
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'New having error',
				message: 'There was an error while adding the new having.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.editHaving = (index) => {
		$scope.editHavingIndex = index;
		dialogHub.showFormDialog({
			title: 'Edit having',
			form: {
				'teiLeft': {
					label: 'Left',
					controlType: 'input',
					placeholder: "Enter left operand",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					value: $scope.report.havings[index].left,
					focus: true,
					required: true
				},
				'tedOperation': {
					label: 'Operation',
					placeholder: 'Select operation',
					controlType: 'dropdown',
					options: $scope.operations,
					value: $scope.report.havings[index].operation,
					required: true,
				},
				'teiRight': {
					label: "Right",
					controlType: 'input',
					placeholder: "Enter right operand",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					value: $scope.report.havings[index].right,
					required: true
				},
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					$scope.report.conditions[$scope.editConditionIndex].left = form['teiLeft'];
					$scope.report.conditions[$scope.editConditionIndex].operation = form['tedOperation'];
					$scope.report.conditions[$scope.editConditionIndex].right = form['teiRight'];
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Having update error',
				message: 'There was an error while updating the having.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.deleteHaving = (index) => {
		dialogHub.showDialog({
			title: `Delete ${$scope.report.havings[index].name}?`,
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
					$scope.report.havings.splice(index, 1);
				});
			}
		});
	};
	// End Havings Section ------------------------------------------------------------------------------------

	// Begin Orderings Section --------------------------------------------------------------------------------
	$scope.addOrdering = () => {
		dialogHub.showFormDialog({
			title: 'Add ordering',
			form: {
				'teiColumn': {
					label: "Column",
					controlType: 'input',
					placeholder: "Enter column",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					focus: true,
					required: true
				},
				'tedDirection': {
					label: "Direction",
					placeholder: 'Select direction',
					controlType: 'dropdown',
					options: $scope.directions,
					value: $scope.directions[0].value,
					required: true,
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					if (!$scope.report.orderings) $scope.report.orderings = [];
					$scope.report.orderings.push({
						column: form['teiColumn'],
						direction: form['tedDirection'],
					});
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'New ordering error',
				message: 'There was an error while adding the new ordering.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.editOrdering = (index) => {
		$scope.editOrderingIndex = index;
		dialogHub.showFormDialog({
			title: 'Edit ordering',
			form: {
				'teiColumn': {
					label: "Column",
					controlType: 'input',
					placeholder: "Enter column",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					value: $scope.report.orderings[index].column,
					focus: true,
					required: true
				},
				'tedDirection': {
					label: "Direction",
					placeholder: 'Select direction',
					controlType: 'dropdown',
					options: $scope.directions,
					value: $scope.report.orderings[index].direction,
					required: true,
				},
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					$scope.report.orderings[$scope.editOrderingIndex].column = form['teiColumn'];
					$scope.report.orderings[$scope.editOrderingIndex].direction = form['tedDirection'];
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Ordering update error',
				message: 'There was an error while updating the ordering.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.deleteOrdering = (index) => {
		dialogHub.showDialog({
			title: `Delete ${$scope.report.orderings[index].name}?`,
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
					$scope.report.orderings.splice(index, 1);
				});
			}
		});
	};
	// End Orderings Section ----------------------------------------------------------------------------------

	// Begin Parameters Section -------------------------------------------------------------------------------
	$scope.addParameter = () => {
		const excludedNames = [];
		for (let i = 0; i < $scope.report.columns.length; i++) {
			excludedNames.push($scope.report.columns[i].name);
		}
		dialogHub.showFormDialog({
			title: 'Add parameter',
			form: {
				'teiName': {
					label: "Name",
					controlType: 'input',
					placeholder: "Enter name",
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
					label: "Type",
					placeholder: 'Select type',
					controlType: 'dropdown',
					options: $scope.types,
					value: $scope.types[0].value,
					required: true,
				},
				'teiInitial': {
					label: "Initial",
					controlType: 'input',
					placeholder: "Enter initial value",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					required: true
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					if (!$scope.report.parameters) $scope.report.parameters = [];
					$scope.report.parameters.push({
						name: form['teiName'],
						type: form['tedType'],
						initial: form['teiInitial'],
					});
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
		for (let i = 0; i < $scope.report.columns.length; i++) {
			if (i !== index)
				excludedNames.push($scope.report.columns[i].name);
		}
		dialogHub.showFormDialog({
			title: 'Add parameter',
			form: {
				'teiName': {
					label: "Name",
					controlType: 'input',
					placeholder: "Enter name",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					inputRules: {
						excluded: excludedNames,
					},
					value: $scope.report.parameters[index].name,
					focus: true,
					required: true
				},
				'tedType': {
					label: "Direction",
					placeholder: 'Select direction',
					controlType: 'dropdown',
					options: $scope.types,
					value: $scope.report.parameters[index].type,
					required: true,
				},
				'teiInitial': {
					label: "Initial",
					controlType: 'input',
					placeholder: "Enter initial value",
					type: 'text',
					minlength: 1,
					maxlength: 255,
					value: $scope.report.parameters[index].initial,
					required: true
				},
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					$scope.report.parameters[$scope.editParameterIndex].name = form['teiName'];
					$scope.report.parameters[$scope.editParameterIndex].type = form['tedType'];
					$scope.report.parameters[$scope.editParameterIndex].initial = form['teiInitial'];
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Parameter update error',
				message: 'There was an error while updating the parameter.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.deleteParameter = (index) => {
		dialogHub.showDialog({
			title: `Delete ${$scope.report.parameters[index].name}?`,
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
					$scope.report.parameters.splice(index, 1);
				});
			}
		});
	};
	// End Parameters Section ---------------------------------------------------------------------------------

	$scope.generateQuery = () => {
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
	if (!$scope.dataParameters.hasOwnProperty('filePath')) {
		$scope.state.error = true;
		$scope.errorMessage = 'The \'filePath\' data parameter is missing.';
	} else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
		$scope.state.error = true;
		$scope.errorMessage = 'The \'contentType\' data parameter is missing.';
	} else loadFileContents();

	// Begin Base Table Section -------------------------------------------------------------------------------
	$scope.setBaseTable = () => {
		dialogHub.showFormDialog({
			title: 'Set from tables',
			form: {
				'tedTable': {
					label: "Table",
					placeholder: 'Select table',
					controlType: 'dropdown',
					options: $scope.tables,
					value: $scope.tables[0].value,
					required: true,
					focus: true
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					const tableMetadataPointer = $scope.tablesMetadata[form['tedTable']];
					$http.get(databasesSvcUrl + tableMetadataPointer.database + "/" + tableMetadataPointer.datasource + "/" + tableMetadataPointer.schema + "/" + tableMetadataPointer.name).then((data) => {
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
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Table error',
				message: 'There was an error while setting the table.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	// End Base Table Section ---------------------------------------------------------------------------------

	// Begin Add from Tables Section -------------------------------------------------------------------------------
	$scope.addFromTables = () => {
		dialogHub.showFormDialog({
			title: 'Add from tables',
			form: {
				'tedTable': {
					label: "Table",
					placeholder: 'Select table',
					controlType: 'dropdown',
					options: $scope.tables,
					value: $scope.tables[0].value,
					required: true,
					focus: true
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					const tableMetadataPointer = $scope.tablesMetadata[form['tedTable']];
					$http.get(databasesSvcUrl + tableMetadataPointer.database + "/" + tableMetadataPointer.datasource + "/" + tableMetadataPointer.schema + "/" + tableMetadataPointer.name).then((data) => {
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
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Table error',
				message: 'There was an error while adding the table.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	// End Add from Tables Section ---------------------------------------------------------------------------------

	// Begin Security Section --------------------------------------------------------------------------------------

	$scope.toggleDefaultRoles = () => {
		if ($scope.report.security.generateDefaultRoles === 'true') {
			$scope.report.security.roleRead = $scope.dataParameters.filePath.split('/')[2] + '.' + "Report" + '.' + $scope.report.name + "ReadOnly";
		} else {
			$scope.report.security.roleRead = null;
		}
	};

	// End Security Section ----------------------------------------------------------------------------------------
});