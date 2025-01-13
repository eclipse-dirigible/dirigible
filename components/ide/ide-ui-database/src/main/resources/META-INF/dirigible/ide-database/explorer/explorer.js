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
const database = angular.module('database', ['ideUI', 'ideView']);
database.controller('DatabaseController', function ($scope, $http, messageHub) {
	let databasesSvcUrl = "/services/data/";
	let databasesInvalidateSvcUrl = "/services/data/metadata/invalidate-cache";
	$scope.selectedDatabase;
	$scope.jstreeWidget = angular.element('#dgDatabases');
	$scope.spinnerColumns = {
		text: "Loading Columns...",
		type: "spinner",
		li_attr: { spinner: true },
	};
	$scope.spinnerIndices = {
		text: "Loading Indices...",
		type: "spinner",
		li_attr: { spinner: true },
	};
	$scope.spinnerForeignKeys = {
		text: "Loading Foreign Keys...",
		type: "spinner",
		li_attr: { spinner: true },
	};
	$scope.jstreeConfig = {
		core: {
			check_callback: true,
			themes: {
				name: "fiori",
				variant: "compact",
			},
			data: []
		},
		search: {
			case_sensitive: false,
		},
		plugins: ['wholerow', 'state', 'types', 'dnd', 'unique', 'contextmenu'],
		dnd: {
			large_drop_target: true,
			large_drag_target: true,
		},
		state: { key: 'ide-databases' },
		types: {
			'default': {
				icon: "jstree-file",
			},
			folder: {
				icon: "jstree-folder",
			},
			file: {
				icon: "jstree-file",
			},
			project: {
				icon: "jstree-project",
			},
			table: {
				icon: "sap-icon--table-view",
			},
			tableView: {
				icon: "sap-icon--grid",
			},
			tableCollection: {
				icon: "sap-icon--list",
			},
			tableLock: {
				icon: "sap-icon--locked",
			},
			column: {
				icon: "sap-icon--table-column",
			},
			columns: {
				icon: "sap-icon--table-column",
			},
			indice: {
				icon: "sap-icon--table-row",
			},
			indices: {
				icon: "sap-icon--table-row",
			},
			foreignKeys: {
				icon: "sap-icon--two-keys",
			},
			schema: {
				icon: "sap-icon--database",
			},
			procedure: {
				icon: "sap-icon--workflow-tasks",
			},
			'function': {
				icon: "sap-icon--settings",
			},
			varchar: {
				icon: "sap-icon--sort-ascending",
			},
			nvarchar: {
				icon: "sap-icon--sort-ascending",
			},
			char: {
				icon: "sap-icon--text",
			},
			date: {
				icon: "sap-icon--calendar",
			},
			datetime: {
				icon: "sap-icon--date-time",
			},
			timestamp: {
				icon: "sap-icon--date-time",
			},
			smallint: {
				icon: "sap-icon--numbered-text",
			},
			tinyint: {
				icon: "sap-icon--numbered-text",
			},
			integer: {
				icon: "sap-icon--numbered-text",
			},
			float: {
				icon: "sap-icon--numbered-text",
			},
			double: {
				icon: "sap-icon--numbered-text",
			},
			decimal: {
				icon: "sap-icon--numbered-text",
			},
			bigint: {
				icon: "sap-icon--trend-up",
			},
			boolean: {
				icon: "sap-icon--checklist-item",
			},
			clob: {
				icon: "sap-icon--rhombus-milestone",
			},
			blob: {
				icon: "sap-icon--rhombus-milestone",
			},
			key: {
				icon: "sap-icon--key",
			},
			spinner: {
				icon: "jstree-spinner",
				valid_children: [],
			},
		},
		contextmenu: {
			items: function (node) {
				let ctxmenu = {};
				// Select contents
				if (node.original.type === 'table'
					|| node.original.type === 'base table'
					|| node.original.type === 'view') {
					ctxmenu.contents = {
						"separator_before": false,
						"label": "Show Contents",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);

							let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
							let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

							messageHub.postMessage('result-view.database.sql.showContent', {
								schemaName: topLevelSchemaName,
								tableName: node.original.text
							});
						}.bind(this)
					};

					// Generate scripts
					let tree = $scope.jstreeWidget.jstree(true);
					let columnsMaybe = tree.get_node(node.children[0]);
					let internalMaybe = null;
					if (columnsMaybe) {
						internalMaybe = tree.get_node(columnsMaybe.children[0]);
					}
					// Select
					if ((node.original.type === 'table' || node.original.type === 'base table' || node.original.type === 'view')
						&& (internalMaybe !== null && internalMaybe.text !== "Loading Columns..." && internalMaybe.text !== "Loading Indices...")) {
						ctxmenu.selectScript = {
							"separator_before": true,
							"label": "Select",
							"action": function (data) {
								let tree = $.jstree.reference(data.reference);
								let node = tree.get_node(data.reference);

								let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
								let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

								let columns = tree.get_node(node.children[0]);
								let sqlCommand = "SELECT ";
								for (let i = 0; i < columns.children.length; i++) {
									sqlCommand += "\"" + tree.get_node(columns.children[i]).original.column.name + "\"";
									sqlCommand += ", ";
								}
								sqlCommand = sqlCommand.substring(0, sqlCommand.length - 2);
								sqlCommand += " FROM \"" + topLevelSchemaName + "\"" + "." + "\"" + node.original.text + "\";\n";
								messageHub.postMessage('database.sql.script', sqlCommand);
								$scope.jstreeWidget.jstree(true).refresh();

							}.bind(this)
						};
					}

					// Insert
					if ((node.original.type === 'table' || node.original.type === 'base table')
						&& (internalMaybe !== null && internalMaybe.text !== "Loading Columns..." && internalMaybe.text !== "Loading Indices...")) {
						ctxmenu.insertScript = {
							"separator_before": false,
							"label": "Insert",
							"action": function (data) {
								let tree = $.jstree.reference(data.reference);
								let node = tree.get_node(data.reference);
								let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
								let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

								let columns = tree.get_node(node.children[0]);
								let sqlCommand = "INSERT INTO \"" + topLevelSchemaName + "\"" + "." + "\"" + node.original.text + "\" (";
								for (let i = 0; i < columns.children.length; i++) {
									sqlCommand += "\"" + tree.get_node(columns.children[i]).original.column.name + "\"";
									sqlCommand += ", ";
								}
								sqlCommand = sqlCommand.substring(0, sqlCommand.length - 2);
								sqlCommand += ") VALUES (";
								for (let i = 0; i < columns.children.length; i++) {
									sqlCommand += "'" + tree.get_node(columns.children[i]).original.column.type;
									sqlCommand += "', ";
								}
								sqlCommand = sqlCommand.substring(0, sqlCommand.length - 2);
								sqlCommand += ");\n";
								messageHub.postMessage('database.sql.script', sqlCommand);
								$scope.jstreeWidget.jstree(true).refresh();

							}.bind(this)
						};
					}

					// Update
					if ((node.original.type === 'table' || node.original.type === 'base table')
						&& (internalMaybe !== null && internalMaybe.text !== "Loading Columns..." && internalMaybe.text !== "Loading Indices...")) {
						ctxmenu.updateScript = {
							"separator_before": false,
							"label": "Update",
							"action": function (data) {
								let tree = $.jstree.reference(data.reference);
								let node = tree.get_node(data.reference);

								let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
								let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

								let columns = tree.get_node(node.children[0]);
								let sqlCommand = "UPDATE \"" + topLevelSchemaName + "\"" + "." + "\"" + node.original.text + "\" SET ";
								for (let i = 0; i < columns.children.length; i++) {
									sqlCommand += "\"" + tree.get_node(columns.children[i]).original.column.name + "\"" +
										" = '" + tree.get_node(columns.children[i]).original.column.type
									sqlCommand += "', ";
								}
								sqlCommand = sqlCommand.substring(0, sqlCommand.length - 2);
								sqlCommand += " WHERE ";
								for (let i = 0; i < columns.children.length; i++) {
									sqlCommand += "\"" + tree.get_node(columns.children[i]).original.column.name + "\"" +
										" = '" + tree.get_node(columns.children[i]).original.column.type
									sqlCommand += "' AND ";
								}
								sqlCommand = sqlCommand.substring(0, sqlCommand.length - 5);
								sqlCommand += ";\n";
								messageHub.postMessage('database.sql.script', sqlCommand);
								$scope.jstreeWidget.jstree(true).refresh();

							}.bind(this)
						};
					}

					// Delete
					if ((node.original.type === 'table' || node.original.type === 'base table')
						&& (internalMaybe !== null && internalMaybe.text !== "Loading Columns..." && internalMaybe.text !== "Loading Indices...")) {
						ctxmenu.deleteScript = {
							"separator_before": false,
							"label": "Delete",
							"action": function (data) {
								let tree = $.jstree.reference(data.reference);
								let node = tree.get_node(data.reference);

								let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
								let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

								let columns = tree.get_node(node.children[0]);
								let sqlCommand = "DELETE FROM \"" + topLevelSchemaName + "\"" + "." + "\"" + node.original.text + "\"";
								sqlCommand += " WHERE ";
								for (let i = 0; i < columns.children.length; i++) {
									sqlCommand += "\"" + tree.get_node(columns.children[i]).original.column.name + "\"" +
										" = '" + tree.get_node(columns.children[i]).original.column.type
									sqlCommand += "' AND ";
								}
								sqlCommand = sqlCommand.substring(0, sqlCommand.length - 5);
								messageHub.postMessage('database.sql.script', sqlCommand);
								$scope.jstreeWidget.jstree(true).refresh();

							}.bind(this)
						};
					}

					// Export data
					ctxmenu.exportData = {
						"separator_before": true,
						"label": "Export Data",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);

							let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
							let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

							let sqlCommand = topLevelSchemaName + "." + node.original.text;
							messageHub.postMessage('database.data.export.artifact', sqlCommand);
						}.bind(this)
					};

					// Import data to table
					if (node.original.type === 'table' || node.original.type === 'base table') {
						ctxmenu.importScript = {
							"separator_before": false,
							"label": "Import Data",
							"action": function (data) {
								let tree = $.jstree.reference(data.reference);
								let node = tree.get_node(data.reference);

								let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
								let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

								let sqlCommand = topLevelSchemaName + "." + node.original.text;
								messageHub.postMessage('database.data.import.artifact', sqlCommand);
							}.bind(this)
						};
					}

					// Export metadata
					ctxmenu.exportMetadata = {
						"separator_before": true,
						"label": "Export Metadata",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);

							let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
							let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

							let sqlCommand = topLevelSchemaName + "." + node.original.text;
							messageHub.postMessage('database.metadata.export.artifact', sqlCommand);
						}.bind(this)
					};

					// Drop table
					if (node.original.type === 'table' || node.original.type === 'base table') {
						ctxmenu.dropScript = {
							"separator_before": true,
							"label": "Drop",
							"action": function (data) {
								let tree = $.jstree.reference(data.reference);
								let node = tree.get_node(data.reference);

								let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
								let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

								let sqlCommand = "DROP TABLE \"" + topLevelSchemaName + "\"" + "." + "\"" + node.original.text + "\" CASCADE;\n";
								messageHub.postMessage('database.sql.script', sqlCommand);
							}.bind(this)
						};
					}
					// Drop view
					if (node.original.type === 'view') {
						ctxmenu.dropScript = {
							"separator_before": false,
							"label": "Drop",
							"action": function (data) {
								let tree = $.jstree.reference(data.reference);
								let node = tree.get_node(data.reference);
								//let parentNodeName = tree.get_text(node.parent);

								let topLevelSchemaNode = node.parents.find(parentId => tree.get_node(parentId).original.kind === 'schema');
								let topLevelSchemaName = tree.get_text(topLevelSchemaNode);

								let sqlCommand = "DROP VIEW \"" + topLevelSchemaName + "\"" + "." + "\"" + node.original.text + "\";\n";
								messageHub.postMessage('database.sql.script', sqlCommand);
							}.bind(this)
						};
					}

				}

				// Procedure related actions
				if (node.original.kind === 'procedure') {
					ctxmenu.dropProcedure = {
						"separator_before": false,
						"label": "Drop",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let sqlCommand = "DROP PROCEDURE \"" + node.original.text + "\";\n";
							messageHub.postMessage('database.sql.script', sqlCommand);
						}.bind(this)
					};
				}

				// Schema related actions
				if (node.original.kind === 'schema') {
					ctxmenu.exportData = {
						"separator_before": false,
						"label": "Export Data",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let sqlCommand = node.original.text;
							messageHub.postMessage('database.data.export.schema', sqlCommand);
						}.bind(this)
					};
					ctxmenu.exportMetadata = {
						"separator_before": false,
						"label": "Export Metadata",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let sqlCommand = node.original.text;
							messageHub.postMessage('database.metadata.export.schema', sqlCommand);
						}.bind(this)
					};
					ctxmenu.exportDataInProject = {
						"separator_before": true,
						"label": "Export Data in Project",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let sqlCommand = node.original.text;
							messageHub.postMessage('database.data.project.export.schema', sqlCommand);
						}.bind(this)
					};
					ctxmenu.exportMetadataInProject = {
						"separator_before": false,
						"label": "Export Metadata in Project",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let sqlCommand = node.original.text;
							messageHub.postMessage('database.metadata.project.export.schema', sqlCommand);
						}.bind(this)
					};
					ctxmenu.exportTopologicalOrder = {
						"separator_before": false,
						"label": "Export Topological Order",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let sqlCommand = node.original.text;
							messageHub.postMessage('database.metadata.project.export.topology', sqlCommand);
						}.bind(this)
					};
					ctxmenu.exportAsModel = {
						"separator_before": false,
						"label": "Export Schema as Model",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let sqlCommand = node.original.text;
							messageHub.postMessage('database.metadata.project.export.model', sqlCommand);
						}.bind(this)
					};
					ctxmenu.dropScript = {
						"separator_before": true,
						"label": "Drop",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let sqlCommand = "DROP SCHEMA \"" + node.original.text + "\" CASCADE;\n";
							messageHub.postMessage('database.sql.script', sqlCommand);
						}.bind(this)
					};
				}

				// Collection related actions
				if (node.original.kind === 'table' && node.original.type === 'collection') {
					ctxmenu.showContents = {
						"separator_before": false,
						"label": "Show Contents",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let sqlCommand = "query: {'find': '" + node.original.text + "'}";
							messageHub.postMessage('database.sql.execute', sqlCommand);
						}.bind(this)
					};
					ctxmenu.exportData = {
						"separator_before": true,
						"label": "Export Data",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let parentNodeName = tree.get_text(node.parent);
							let sqlCommand = parentNodeName + "." + node.original.text;
							messageHub.postMessage('database.data.export.artifact', sqlCommand);
						}.bind(this)
					};
					// Import data to table
					ctxmenu.importScript = {
						"separator_before": false,
						"label": "Import Data",
						"action": function (data) {
							let tree = $.jstree.reference(data.reference);
							let node = tree.get_node(data.reference);
							let parentNodeName = tree.get_text(node.parent);
							let sqlCommand = parentNodeName + "." + node.original.text;
							messageHub.postMessage('database.data.import.artifact', sqlCommand);
						}.bind(this)
					};
				}

				// Column related actions
				if (node.original.kind === 'column') {
					ctxmenu.anonymizeFullName = {
						"separator_before": false,
						"label": "Anonymize Full Name",
						"action": function (data) {
							anonymizeMenu(node, data, "FULL_NAME");
						}.bind(this)
					};
					ctxmenu.anonymizeFirstName = {
						"separator_before": false,
						"label": "Anonymize First Name",
						"action": function (data) {
							anonymizeMenu(node, data, "FIRST_NAME");
						}.bind(this)
					};
					ctxmenu.anonymizeLastName = {
						"separator_before": false,
						"label": "Anonymize Last Name",
						"action": function (data) {
							anonymizeMenu(node, data, "LAST_NAME");
						}.bind(this)
					};
					ctxmenu.anonymizeUserName = {
						"separator_before": false,
						"label": "Anonymize User Name",
						"action": function (data) {
							anonymizeMenu(node, data, "USER_NAME");
						}.bind(this)
					};
					ctxmenu.anonymizePhone = {
						"separator_before": false,
						"label": "Anonymize Phone",
						"action": function (data) {
							anonymizeMenu(node, data, "PHONE");
						}.bind(this)
					};
					ctxmenu.anonymizeEmail = {
						"separator_before": false,
						"label": "Anonymize e-mail",
						"action": function (data) {
							anonymizeMenu(node, data, "EMAIL");
						}.bind(this)
					};
					ctxmenu.anonymizeAddress = {
						"separator_before": false,
						"label": "Anonymize Address",
						"action": function (data) {
							anonymizeMenu(node, data, "ADDRESS");
						}.bind(this)
					};
					ctxmenu.anonymizeCity = {
						"separator_before": false,
						"label": "Anonymize City",
						"action": function (data) {
							anonymizeMenu(node, data, "CITY");
						}.bind(this)
					};
					ctxmenu.anonymizeCountry = {
						"separator_before": false,
						"label": "Anonymize Country",
						"action": function (data) {
							anonymizeMenu(node, data, "COUNTRY");
						}.bind(this)
					};
					ctxmenu.anonymizeCountry = {
						"separator_before": false,
						"label": "Anonymize Country",
						"action": function (data) {
							anonymizeMenu(node, data, "COUNTRY");
						}.bind(this)
					};
					ctxmenu.anonymizeDate = {
						"separator_before": false,
						"label": "Anonymize Date",
						"action": function (data) {
							anonymizeMenu(node, data, "DATE");
						}.bind(this)
					};
					ctxmenu.anonymizeRandom = {
						"separator_before": false,
						"label": "Randomize Value",
						"action": function (data) {
							anonymizeMenu(node, data, "RANDOM");
						}.bind(this)
					};
					ctxmenu.anonymizeMask = {
						"separator_before": false,
						"label": "Mask Value",
						"action": function (data) {
							anonymizeMenu(node, data, "MASK");
						}.bind(this)
					};
					ctxmenu.anonymizeEmpty = {
						"separator_before": false,
						"label": "Empty Value",
						"action": function (data) {
							anonymizeMenu(node, data, "EMPTY");
						}.bind(this)
					};
					ctxmenu.anonymizeNull = {
						"separator_before": false,
						"label": "Set to Null",
						"action": function (data) {
							anonymizeMenu(node, data, "NULL");
						}.bind(this)
					};
				}

				return ctxmenu;
			}
		}
	};

	let anonymizeMenu = function (node, data, type) {
		let tree = $.jstree.reference(data.reference);
		let columnNode = tree.get_node(data.reference);
		let tableNode = findParentTableOfColumn(tree, node); //tree.get_node(tree.get_node(node.parent).parent);
		let schemaNode = findParentSchemaOfColumn(tree, node); //tree.get_node(tree.get_node(tree.get_node(node.parent).parent).parent);

		let primaryKeyName = tree.get_node(tree.get_node(node.parent).children[0]).original.name;
		tree.get_node(node.parent).children.forEach(c => {
			if (tree.get_node(c).original.key) {
				primaryKeyName = tree.get_node(c).original.name;
			}
		});

		let parameters = {};
		parameters.datasource = $scope.selectedDatasource;
		parameters.schema = schemaNode.original.text;
		parameters.table = tableNode.original.text;
		let a = columnNode.id.split('$');
		parameters.column = a.splice(1, a.length - 1).join('.'); //columnNode.original.name;
		parameters.primaryKey = primaryKeyName;
		parameters.type = type;
		messageHub.postMessage('database.data.anonymize.column', parameters);
	}

	let findParentTableOfColumn = function (tree, node) {
		let maybe = tree.get_node(node.parent);
		if (!maybe.original.type && "Columns" === maybe.text) {
			maybe = tree.get_node(maybe.parent);
		}
		if (maybe.original.type == "table" || maybe.original.type == "base table" || maybe.original.type == "collection") {
			return maybe;
		}
		return findParentTableOfColumn(tree, maybe);
	}

	let findParentSchemaOfColumn = function (tree, node) {
		let maybe = tree.get_node(node.parent);
		if (!maybe.original.type && "Columns" === maybe.text) {
			maybe = tree.get_node(maybe.parent);
		}
		if (maybe.original.type == "schema" || maybe.original.type == "nosql") {
			return maybe;
		}
		return findParentSchemaOfColumn(tree, maybe);
	}

	$scope.jstreeWidget.on('open_node.jstree', function (event, data) {
		if (data.node.children.length === 1 && $scope.jstreeWidget.jstree(true).get_text(data.node.children[0]) === "Loading Columns...") {
			expandColumns(event, data);
		} else if (data.node.children.length === 1 && $scope.jstreeWidget.jstree(true).get_text(data.node.children[0]) === "Loading Indices...") {
			expandIndices(event, data);
		} else if (data.node.children.length === 1 && $scope.jstreeWidget.jstree(true).get_text(data.node.children[0]) === "Loading Foreign Keys...") {
			expandForeignKeys(event, data);
		}
	});

	$scope.getDatabases = function () {
		$http.get(databasesSvcUrl)
			.then(function (data) {
				$scope.databases = data.data;
				if ($scope.databases.length > 0) {
					let storedDatabase = JSON.parse(localStorage.getItem('DIRIGIBLE.database'));
					if (storedDatabase !== null) {
						$scope.selectedDatabase = storedDatabase.type;
					} else {
						$scope.selectedDatabase = $scope.databases[0];
					}
					if ($scope.selectedDatabase) {
						messageHub.postMessage('database.database.selection.changed', $scope.selectedDatabase);
						$http.get(databasesSvcUrl + $scope.selectedDatabase + "/").then(function (data) {
							$scope.datasources = data.data;
							if ($scope.datasources.length > 0) {
								if (storedDatabase !== null) {
									$scope.selectedDatasource = storedDatabase.name;
								} else {
									$scope.selectedDatasource = $scope.datasources[0];
								}
								if ($scope.selectedDatasource) {
									messageHub.postMessage('database.datasource.selection.changed', $scope.selectedDatasource);
									$scope.refreshDatabase();
								}
							}
						});
					}
				}
			});
	}
	setTimeout($scope.getDatabases(), 500);

	let expandColumns = function (evt, data) {
		let parent = $scope.jstreeWidget.jstree(true).get_node(data.node);
		let tableParent = $scope.jstreeWidget.jstree(true).get_node(data.node.parent);
		let topLevelSchemaNode = tableParent.parents.find(parentId => {
			let node = $scope.jstreeWidget.jstree(true).get_node(parentId);
			return (node.original.kind === 'schema' || node.original.kind === 'nosql') && node.text !== 'Tables';
		});

		let schemaParent;

		if (topLevelSchemaNode) {
			schemaParent = $scope.jstreeWidget.jstree(true).get_text(topLevelSchemaNode);
		} else {
			schemaParent = $scope.jstreeWidget.jstree(true).get_text(tableParent.parent);
		}

		$scope.jstreeWidget.jstree("delete_node", $scope.jstreeWidget.jstree(true).get_node(data.node.children[0]));

		$http.get(databasesSvcUrl + $scope.selectedDatabase + '/' + $scope.selectedDatasource
			+ '/' + schemaParent + '/' + tableParent.text + "?kind=" + tableParent.original.kind.toUpperCase())
			.then(function (data) {
				data.data.columns.forEach(function (column) {
					expandColumn(parent, column);
				})
			});
	}

	let expandColumn = function (parent, column) {
		let position = 'last';
		let icon = "sap-icon--grid";
		if (column.key) {
			icon = "sap-icon--key";
		} else {
			switch (column.type.toLowerCase()) {
				case "character varying":
				case "varchar":
				case "nvarchar":
				case "char":
					icon = "sap-icon--text";
					break;
				case "date":
					icon = "sap-icon--calendar";
					break;
				case "datetime":
				case "timestamp":
					icon = "sap-icon--date-time";
					break;
				case "bigint":
				case "smallint":
				case "tinyint":
				case "integer":
					icon = "sap-icon--numbered-text";
					break;
				case "float":
				case "double":
				case "double precision":
				case "decimal":
					icon = "sap-icon--numbered-text"
					break;
				case "boolean":
					icon = "sap-icon--sys-enter";
					break;
				case "clob":
				case "blob":
					icon = "sap-icon--rhombus-milestone";
					break;
			}
		}
		let nodeText = column.name + ' - <i style="font-size: smaller;">' + column.type;
		if ((column.size !== undefined && column.size !== 0)
			|| (column.length !== undefined && column.size !== 0)) {
			nodeText += "(" + (column.size !== undefined ? column.size : (column.length !== undefined ? column.length : "N/A")) + ")";
		}
		nodeText += "</i>";
		let newNode = {
			id: parent.id + "$" + column.name,
			state: "open",
			text: nodeText,
			column: column,
			icon: icon,
			kind: column.kind,
			name: column.name,
			key: column.key

		};
		$scope.jstreeWidget.jstree("create_node", parent, newNode, position, false, false);
		if (column.columns) {
			column.columns.forEach(function (column) {
				expandColumn(newNode, column);
			});
		}
	}

	let expandIndices = function (evt, data) {
		let parent = $scope.jstreeWidget.jstree(true).get_node(data.node);
		let tableParent = $scope.jstreeWidget.jstree(true).get_node(data.node.parent);
		let topLevelSchemaNode = tableParent.parents.find(parentId => {
			let node = $scope.jstreeWidget.jstree(true).get_node(parentId);
			return node.original.kind === 'schema' && node.text !== 'Tables';
		});

		let schemaParent;

		if (topLevelSchemaNode) {
			schemaParent = $scope.jstreeWidget.jstree(true).get_text(topLevelSchemaNode);
		} else {
			schemaParent = $scope.jstreeWidget.jstree(true).get_text(tableParent.parent);
		}

		$scope.jstreeWidget.jstree("delete_node", $scope.jstreeWidget.jstree(true).get_node(data.node.children[0]));
		let position = 'last';
		$http.get(databasesSvcUrl + $scope.selectedDatabase + '/' + $scope.selectedDatasource
			+ '/' + schemaParent + '/' + tableParent.text)
			.then(function (data) {
				data.data.indices.forEach(function (index) {
					let nodeText = index.name;
					let newNode = { state: "open", "text": nodeText, "id": parent.id + "$" + index.name, "icon": "sap-icon--bullet-text" };
					$scope.jstreeWidget.jstree("create_node", parent, newNode, position, false, false);
				})
			});
	}

	let expandForeignKeys = function (evt, data) {
		let parent = $scope.jstreeWidget.jstree(true).get_node(data.node);
		let tableParent = $scope.jstreeWidget.jstree(true).get_node(data.node.parent);
		let topLevelSchemaNode = tableParent.parents.find(parentId => {
			let node = $scope.jstreeWidget.jstree(true).get_node(parentId);
			return node.original.kind === 'schema' && node.text !== 'Tables';
		});

		let schemaParent;

		if (topLevelSchemaNode) {
			schemaParent = $scope.jstreeWidget.jstree(true).get_text(topLevelSchemaNode);
		} else {
			schemaParent = $scope.jstreeWidget.jstree(true).get_text(tableParent.parent);
		}

		$scope.jstreeWidget.jstree("delete_node", $scope.jstreeWidget.jstree(true).get_node(data.node.children[0]));
		let position = 'last';
		$http.get(databasesSvcUrl + $scope.selectedDatabase + '/' + $scope.selectedDatasource
			+ '/' + schemaParent + '/' + tableParent.text)
			.then(function (data) {
				data.data.foreignKeys.forEach(function (foreignKey) {
					let nodeText = foreignKey.name;
					let newNode = { state: "open", "text": nodeText, "id": parent.id + "$" + foreignKey.name, "icon": "sap-icon--bullet-text" };
					$scope.jstreeWidget.jstree("create_node", parent, newNode, position, false, false);
				})
			});
	}

	$scope.refreshDatabase = function () {
		if ($scope.jstreeWidget.jstree(true).settings === undefined) $scope.jstreeWidget.jstree($scope.jstreeConfig);
		if ($scope.selectedDatabase && $scope.selectedDatasource) {
			$http.get(databasesSvcUrl + $scope.selectedDatabase + '/' + $scope.selectedDatasource)
				.then(function (data) {
					$scope.datasource = data.data;
					this.baseUrl = databasesSvcUrl + $scope.selectedDatabase + '/' + $scope.selectedDatasource;
					let schemas = $scope.datasource.schemas.map(function (schemas) {
						return build(schemas);
					})
					$scope.jstreeWidget.jstree(true).settings.core.data = schemas;
					$scope.jstreeWidget.jstree(true).refresh();
				}.bind(this));
		} else {
			$scope.jstreeWidget.jstree(true).settings.core.data = [];
			$scope.jstreeWidget.jstree(true).refresh();
		}
	};

	let build = function (f) {
		let children = [];
		let icon = 'sap-icon--grid';
		let name = f.name;
		if (f.kind == 'schema') {
			const types = ["Tables", "Views", "Procedures", "Functions", "Sequences"];

			children = types.map((type, index) => {
				return {
					text: type,
					icon: "sap-icon--folder",
					children: f[type.toLowerCase()].map(item => build(item))
				};
			});

			icon = 'sap-icon--database';
		} else if (f.kind == 'table' && (f.type === 'TABLE' || f.type === 'BASE TABLE')) {
			children = [
				{ text: "Columns", "icon": "sap-icon--table-column", children: [$scope.spinnerColumns] },
				{ text: "Indices", "icon": "sap-icon--table-row", children: [$scope.spinnerIndices] },
				{ text: "Foreign Keys", "icon": "sap-icon--two-keys", children: [$scope.spinnerForeignKeys] }
			];
			icon = 'sap-icon--table-view';
		} else if (f.kind == 'table' && f.type === 'VIEW') {
			children = [
				{ text: "Columns", "icon": "sap-icon--table-column", children: [$scope.spinnerColumns] },
				{ text: "Indices", "icon": "sap-icon--table-row", children: [$scope.spinnerIndices] },
			];
			icon = 'sap-icon--grid';
		} else if (f.kind == 'table' && f.type === 'COLLECTION') {
			children = [
				{ text: "Columns", "icon": "sap-icon--table-column", children: [$scope.spinnerColumns] }
				// , { text: "Indices", "icon": "sap-icon--table-row", children: [$scope.spinnerIndices] },
			];
			icon = 'sap-icon--list';
		} else if (f.kind == 'table' && f.type !== 'TABLE' && f.type !== 'VIEW') {
			children = [
				{ text: "Columns", "icon": "sap-icon--table-column", children: [$scope.spinnerColumns] },
				{ text: "Indices", "icon": "sap-icon--table-row", children: [$scope.spinnerIndices] },
			];
			icon = 'sap-icon--locked';
		} else if (f.kind == 'procedure') { // && f.type === 'XXX'
			icon = 'sap-icon--workflow-tasks';
		} else if (f.kind == 'function') { // && f.type === 'XXX'
			icon = 'sap-icon--settings';
		} else if (f.kind == 'sequence') { // && f.type === 'XXX'
			icon = 'sap-icon--number-sign';
		} else if (f.kind == 'column') {
			icon = 'sap-icon--grid';
			name += ` [<i>${data.type}</i>(<i>${data.size}</i>)]`;
		} else if (f.kind == 'nosql') {
			let tablesChildren = f.tables.map(function (_table) {
				return build(_table)
			});
			children = children.concat(tablesChildren);
			icon = 'sap-icon--grid';
		}
		f.label = f.name;
		return {
			"text": name,
			"children": children,
			"type": (f.type) ? f.type.toLowerCase() : f.kind,
			"kind": f.kind,
			"_file": f,
			"icon": icon
		}
	}

	$scope.isSelectedDatabase = function (name) {
		if ($scope.selectedDatabase === name) return true;
		return false;
	};

	$scope.isSelectedDatasource = function (name) {
		if ($scope.selectedDatasource === name) return true;
		return false;
	};

	$scope.switchDatabase = function (name) {
		$scope.selectedDatabase = name;
		$http.get(databasesSvcUrl + $scope.selectedDatabase)
			.then(function (data) {
				$scope.datasources = data.data;
				if ($scope.datasources[0]) {
					$scope.selectedDatasource = $scope.datasources[0];
					messageHub.postMessage('database.database.selection.changed', $scope.selectedDatabase);
					messageHub.postMessage('database.datasource.selection.changed', $scope.selectedDatasource);
					$scope.switchDatasource();
				} else {
					$scope.selectedDatasource = undefined;
				}
				$scope.refreshDatabase();
			});
	};

	$scope.switchDatasource = function (name) {
		if (name) $scope.selectedDatasource = name;
		localStorage.setItem('DIRIGIBLE.database', JSON.stringify({ "type": $scope.selectedDatabase, "name": $scope.selectedDatasource }));
		messageHub.postMessage('database.datasource.selection.changed', $scope.selectedDatasource);
		$scope.refreshDatabase();
	};

	$scope.runSQL = function () {
		messageHub.postMessage('database.sql.run', {});
	};

	messageHub.onDidReceiveMessage(
		'ide-databases.explorer.refresh',
		function () {
			$scope.$apply(function () {
				$scope.refresh();
			});
		},
		true
	);

	$scope.refresh = function () {
		$scope.invalidateCache();
		$scope.getDatabases();
	}

	$scope.invalidateCache = function () {
		$http.get(databasesInvalidateSvcUrl);
	}

});
