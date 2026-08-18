/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('page', ['blimpKit', 'platformView', 'platformShortcuts', 'WorkspaceService', 'GenerateService']).controller('PageController', ($scope, $window, $http, WorkspaceService, GenerateService, ViewParameters, ButtonStates) => {
	const statusBarHub = new StatusBarHub();
	const workspaceHub = new WorkspaceHub();
	const layoutHub = new LayoutHub();
	const dialogHub = new DialogHub();
	let genFile = '';
	let workspace = '';
	let contents;
	$scope.changed = false;
	// 'structured': the visual builder owns report.query and regenerates it on every model
	// edit. 'freestyle': the stored SQL is richer than what the builder can reproduce
	// (expressions, custom joins, quoting), so the query string is the source of truth,
	// the query-shaping builder sections are hidden and the query is edited directly.
	$scope.queryMode = 'structured';
	$scope.queryPanelExpanded = false;
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
	// The generation pipeline migrates an old report the same way (Java ModelTranslations)
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

	// angular.toJson drops Angular's $$-prefixed bookkeeping (e.g. the $$hashKey that
	// ng-repeat stamps on rows) so it never leaks into the saved artefact; the 2-space
	// indentation matches the intent generator's output.
	const serializeReport = () => angular.toJson($scope.report, 2);

	const loadFileContents = () => {
		if (!$scope.state.error) {
			$scope.state.isBusy = true;
			WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
				$scope.$evalAsync(() => {
					if (response.data === '') $scope.report = {};
					else $scope.report = migrateReport(response.data);
					$scope.widgetEnabled = !!$scope.report.widget;
					// Absent means shown on the dashboard - make it explicit so the checkbox binds
					// cleanly (before the dirty-tracking snapshot below, so this is not a change).
					if ($scope.report.dashboard === undefined) $scope.report.dashboard = true;
					contents = serializeReport();
					// Round-trip guard: the builder may only own the query when it can actually
					// reproduce the stored one. Otherwise (intent-generated or hand-tuned SQL)
					// the query string is the source of truth and is never regenerated.
					$scope.queryMode = !$scope.report.query || $scope.report.query === buildQuery($scope.report) ? 'structured' : 'freestyle';
					$scope.queryPanelExpanded = $scope.queryMode === 'freestyle';
					$scope.state.isBusy = false;
				});
			}, (response) => {
				console.error(response);
				if (response && response.status === 404) {
					// The file no longer exists (e.g. the workspace was cleaned by a rebuild) - close the stale editor.
					layoutHub.closeEditor({ path: $scope.dataParameters.filePath });
					return;
				}
				$scope.$evalAsync(() => {
					$scope.state.error = true;
					$scope.errorMessage = 'Error while loading file. Please look at the console for more information.';
					$scope.state.isBusy = false;
				});
			});
		}
		loadDatabasesMetadata();
	};

	$scope.refreshTables = function () {
		loadDatabasesMetadata();
	};

	$scope.regenerate = () => {
		$scope.save();
		dialogHub.showBusyDialog('Loading data');
		WorkspaceService.loadContent(genFile).then((response) => {
			let { models, perspectives, templateId, filePath, workspaceName, projectName, ...params } = response.data;
			if (!response.data.templateId) {
				$scope.chooseTemplate(response.data.projectName, response.data.filePath, params);
			} else {
				dialogHub.showBusyDialog('Regenerating');
				$scope.generateFromModel(response.data.projectName, response.data.filePath, response.data.templateId, params);
			}
		}, (error) => {
			console.error(error);
			dialogHub.closeBusyDialog();
			dialogHub.showAlert({
				title: 'Unable to load gen file',
				message: 'There was an error while loading the gen file.\nPlease look at the console for more information.',
				type: AlertTypes.Error,
				preformatted: true,
			});
		});
	};

	$scope.generateFromModel = (project, filePath, templateId, params) => {
		GenerateService.generateFromModel(
			workspace,
			project,
			filePath,
			templateId,
			params
		).then(() => {
			dialogHub.closeBusyDialog();
			statusBarHub.showMessage(`Generated from model '${filePath}'`);
			dialogHub.postMessage({ topic: 'projects.tree.refresh', data: { partial: true, project: project, workspace: workspace } });
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Failed to generate',
				message: 'Please look at the console for more information',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.checkGenFile = () => {
		WorkspaceService.resourceExists(genFile).then(() => {
			$scope.$evalAsync(() => {
				$scope.canRegenerate = true;
			});
		}, () => {
			$scope.$evalAsync(() => {
				$scope.canRegenerate = false;
			});
		});
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
				$scope.errorMessage = `Error saving '${$scope.dataParameters.filePath}'. Please look at the console for more information.`;
				$scope.state.isBusy = false;
			});
		});
	}

	$scope.save = (keySet = 'ctrl+s', event) => {
		event?.preventDefault();
		if (keySet === 'ctrl+s') {
			if ($scope.changed && $scope.forms.editor.$valid) {
				$scope.state.busyText = 'Saving...';
				$scope.state.isBusy = true;
				$scope.state.error = false;
				saveContents(serializeReport());
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
				genFile = $scope.dataParameters.filePath.substring(0, $scope.dataParameters.filePath.lastIndexOf('.')) + '.gen';
				workspace = $scope.dataParameters.filePath.substring($scope.dataParameters.filePath.indexOf('/', 1), 1);
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
		if (!$scope.state.isBusy) {
			// Regenerate before the dirty check so both see the same state. In free-style
			// mode the stored SQL is authoritative and must never be overwritten.
			if ($scope.queryMode === 'structured' && $scope.report) {
				$scope.report.query = buildQuery($scope.report);
			}
			if (!$scope.state.error) {
				const isDirty = contents !== serializeReport();
				if ($scope.changed !== isDirty) {
					$scope.fileChanged();
				}
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

	// Physical identifiers (table and column names) are stored unquoted on the model;
	// quoting happens on emission, mirroring ReportIntentGenerator.buildQuery(). '*' and
	// already-quoted values (legacy hand-edited files) pass through untouched.
	function quoteIdentifier(name) {
		return !name || name === '*' || name.startsWith('"') ? name : `"${name}"`;
	}

	// The SELECT/GROUP BY term of a column: a verbatim SQL expression when present
	// (computed dimensions like date buckets), else the quoted qualified column.
	function columnTerm(column) {
		if (column.expression) return column.expression;
		if (column.name === '*') return '*';
		return column.table + '.' + quoteIdentifier(column.name);
	}

	// Rebuild the SQL from the structured model. Emission is aligned token-for-token with
	// ReportIntentGenerator.buildQuery() so generated reports round-trip unchanged.
	function buildQuery(report) {
		let query = 'SELECT ';
		if (report.columns) {
			const selectParts = [];
			for (let i = 0; i < report.columns.length; i++) {
				const column = report.columns[i];
				if (column.select === true) {
					let part = columnTerm(column);
					if (column.aggregate !== undefined && column.aggregate !== 'NONE') {
						part = column.aggregate + '(' + part + ')';
					}
					selectParts.push(`${part} as "${column.alias}"`);
				}
			}
			query += selectParts.join(', ');
		}
		if (report.table && report.alias)
			query += '\nFROM ' + quoteIdentifier(report.table) + ' as ' + report.alias;

		if (report.joins) {
			for (let i = 0; i < report.joins.length; i++) {
				query += '\n' + report.joins[i].type + ' JOIN ' + quoteIdentifier(report.joins[i].name) + ' as ' + report.joins[i].alias + ' ON ' + report.joins[i].condition;
			}
		}

		if (report.conditions && report.conditions.length > 0) {
			query += '\nWHERE ';
			for (let i = 0; i < report.conditions.length; i++) {
				if (i > 0) { query += ' AND ' }
				query += report.conditions[i].left + ' ' + report.conditions[i].operation + ' ' + report.conditions[i].right;
			}
		}

		if (report.columns) {
			const groupParts = [];
			for (let i = 0; i < report.columns.length; i++) {
				if (report.columns[i].grouping === true) {
					groupParts.push(columnTerm(report.columns[i]));
				}
			}
			if (groupParts.length > 0) query += '\nGROUP BY ' + groupParts.join(', ');
		}

		if (report.havings && report.havings.length > 0) {
			query += '\nHAVING ';
			for (let i = 0; i < report.havings.length; i++) {
				if (i > 0) { query += ' AND ' }
				query += report.havings[i].left + ' ' + report.havings[i].operation + ' ' + report.havings[i].right;
			}
		}

		if (report.orderings && report.orderings.length > 0) {
			query += '\nORDER BY ';
			for (let i = 0; i < report.orderings.length; i++) {
				if (i > 0) { query += ', ' }
				query += report.orderings[i].column + ' ' + report.orderings[i].direction;
			}
		}
		return query;
	}

	// Structured -> free-style is always safe: the current query is kept as-is and simply
	// stops being regenerated.
	$scope.switchToFreestyle = () => {
		$scope.queryMode = 'freestyle';
		$scope.queryPanelExpanded = true;
	};

	$scope.convertToStructured = () => {
		dialogHub.showDialog({
			title: 'Convert to structured editing?',
			message: 'The query will be rebuilt from the visual builder. Custom SQL that the builder cannot represent (expressions, joins, filters) will be lost.',
			buttons: [{
				id: 'bconv',
				state: ButtonStates.Emphasized,
				label: 'Convert',
			},
			{
				id: 'bc',
				state: ButtonStates.Transparent,
				label: 'Cancel',
			}]
		}).then((buttonId) => {
			if (buttonId === 'bconv') {
				$scope.$evalAsync(() => {
					$scope.queryMode = 'structured';
					$scope.report.query = buildQuery($scope.report);
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
	} else {
		genFile = $scope.dataParameters.filePath.substring(0, $scope.dataParameters.filePath.lastIndexOf('.')) + '.gen';
		workspace = $scope.dataParameters.filePath.substring($scope.dataParameters.filePath.indexOf('/', 1), 1);
		loadFileContents();
		$scope.checkGenFile();
	}

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

	// Begin Dashboard Widget Section ------------------------------------------------------------------------------
	// The `widget` block turns the report into a dashboard KPI tile: kind `count` shows the report's
	// record count, `value` one aggregate cell (a measure column, optionally pinned by `at` equals
	// conditions over grouping columns), `list` the first rows as a mini table. Consumed at runtime
	// by the Harmonia shell's reports store; the tile replaces the report's dashboard preview tile.

	$scope.widgetKinds = [
		{ value: 'count', label: 'Count - the number of records' },
		{ value: 'value', label: 'Value - one aggregate cell' },
		{ value: 'list', label: 'List - the first rows' },
	];

	$scope.toggleWidget = () => {
		if ($scope.widgetEnabled) {
			if (!$scope.report.widget) {
				$scope.report.widget = {
					kind: 'count',
					label: $scope.report.label || $scope.report.name,
					tId: getTranslationId('widget' + ($scope.report.name || '')),
					icon: 'gauge',
				};
			}
		} else {
			delete $scope.report.widget;
		}
	};

	$scope.widgetKindChanged = () => {
		const widget = $scope.report.widget;
		if (widget.kind === 'list') {
			if (!widget.limit) widget.limit = 5;
		} else {
			delete widget.limit;
		}
		if (widget.kind !== 'value') {
			delete widget.valueColumn;
			delete widget.valueType;
			delete widget.pattern;
		}
	};

	// The measure the tile shows: an aggregate column of this report. Type and (money) pattern ride
	// along so the dashboard can format the number without re-deriving the column.
	$scope.widgetMeasureColumns = () => ($scope.report.columns || []).filter(c => c.aggregate && c.aggregate !== 'NONE');

	$scope.widgetPinColumns = () => ($scope.report.columns || []).filter(c => !c.aggregate || c.aggregate === 'NONE');

	$scope.widgetValueChanged = () => {
		const widget = $scope.report.widget;
		const column = ($scope.report.columns || []).find(c => c.alias === widget.valueColumn);
		if (column) {
			widget.valueType = column.type;
			if (column.pattern) widget.pattern = column.pattern;
			else delete widget.pattern;
		}
	};

	$scope.addWidgetPin = () => {
		const options = $scope.widgetPinColumns().map(c => ({ label: c.alias, value: c.alias }));
		if (options.length === 0) {
			dialogHub.showAlert({
				title: 'No columns to pin',
				message: 'Widget pins reference the report\'s non-aggregate (grouping) columns - add such a column first.',
				type: AlertTypes.Information,
				preformatted: false,
			});
			return;
		}
		dialogHub.showFormDialog({
			title: 'Add widget pin',
			form: {
				'wpdColumn': {
					label: 'Column',
					placeholder: 'Select column',
					controlType: 'dropdown',
					options: options,
					value: options[0].value,
					required: true,
				},
				'wpdMode': {
					label: 'Pin to',
					controlType: 'dropdown',
					options: [
						{ label: 'Literal value', value: 'literal' },
						{ label: 'Now - the current date / period', value: 'now' },
					],
					value: 'literal',
					required: true,
				},
				'wpiValue': {
					label: 'Value (for a literal pin)',
					controlType: 'input',
					placeholder: 'Enter value',
					type: 'text',
					maxlength: 255,
				},
			},
			submitLabel: 'Add',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				$scope.$evalAsync(() => {
					const widget = $scope.report.widget;
					if (!widget.at) widget.at = [];
					const column = ($scope.report.columns || []).find(c => c.alias === form['wpdColumn']);
					const pin = { column: form['wpdColumn'], type: column ? column.type : 'VARCHAR' };
					if (form['wpdMode'] === 'now') pin.token = 'now';
					else pin.value = form['wpiValue'];
					widget.at.push(pin);
				});
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'New widget pin error',
				message: 'There was an error while adding the new widget pin.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	};

	$scope.deleteWidgetPin = (index) => {
		$scope.$evalAsync(() => {
			$scope.report.widget.at.splice(index, 1);
			if ($scope.report.widget.at.length === 0) delete $scope.report.widget.at;
		});
	};

	// End Dashboard Widget Section --------------------------------------------------------------------------------
});