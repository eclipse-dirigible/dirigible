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
angular.module('ui.schema.modeler', ['blimpKit', 'platformView', 'WorkspaceService']).controller('ModelerCtrl', ($scope, WorkspaceService, $window, ViewParameters) => {
	const statusBarHub = new StatusBarHub();
	const workspaceHub = new WorkspaceHub();
	const layoutHub = new LayoutHub();
	const dialogHub = new DialogHub();
	let contents;
	let schemaFile;
	$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
	$scope.state = {
		isBusy: true,
		error: false,
		busyText: "Loading...",
	};
	$scope.dataTypes = [
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

	angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

	const showAlert = (title, message) => {
		dialogHub.showAlert({
			title: title,
			message: message,
			type: AlertTypes.Error,
			preformatted: false,
		});
	};

	function initializeSchemaJson() {
		WorkspaceService.createFile('', schemaFile, '').then(() => {
			workspaceHub.announceFileSaved({
				path: schemaFile,
				contentType: $scope.dataParameters.contentType,
			});
			const workspace = $scope.dataParameters.filePath.substring(1, $scope.dataParameters.filePath.indexOf('/', 1));
			workspaceHub.postMessage({
				topic: 'projects.tree.refresh',
				data: { partial: true, project: $scope.dataParameters.filePath.substring($scope.dataParameters.filePath.indexOf('/', workspace.length + 2), workspace.length + 2), workspace: workspace }
			});
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: `Error saving '${schemaFile}'`,
				message: 'Please look at the console for more information',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	}

	function saveContents(text, resourcePath) {
		WorkspaceService.saveContent(resourcePath, text).then(() => {
			contents = text;
			layoutHub.setEditorDirty({
				path: resourcePath,
				dirty: false,
			});
			workspaceHub.announceFileSaved({
				path: resourcePath,
				contentType: $scope.dataParameters.contentType,
			});
			$scope.$evalAsync(() => {
				$scope.state.isBusy = false;
			});
		}, (response) => {
			console.error(response);
			$scope.$evalAsync(() => {
				$scope.state.error = true;
				$scope.errorMessage = `Error saving '${resourcePath}'. Please look at the console for more information.`;
				$scope.state.isBusy = false;
			});
		});
	}

	$scope.checkSchema = () => {
		WorkspaceService.resourceExists(schemaFile).then(() => { }, () => {
			initializeSchemaJson();
		});
	};

	const loadFileContents = () => {
		if (!$scope.state.error) {
			$scope.state.isBusy = true;
			WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
				contents = response.data;
				$scope.checkSchema();
				main(document.getElementById('graphContainer'),
					document.getElementById('outlineContainer'),
					document.getElementById('toolbarContainer'),
					document.getElementById('sidebarContainer'));
				$scope.$evalAsync(() => {
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

	$scope.saveSchema = () => {
		saveContents(createSchema($scope.graph), $scope.dataParameters.filePath);
		saveContents(createSchemaJson($scope.graph), schemaFile);
	};

	$scope.getBoolean = (value) => {
		if (typeof value === "string") {
			if (value === "true") return true;
			return false;
		} else if (typeof value === "number") {
			if (value === 1) return true;
			else return false;
		} else if (typeof value === "boolean") return value;
		return false;
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
		if (!$scope.state.error) {
			$scope.saveSchema();
		}
	});

	workspaceHub.onSaveFile((data) => {
		if (data.path && data.path === $scope.dataParameters.filePath && !$scope.state.error) {
			$scope.saveSchema();
		}
	});

	function main(container, outline, toolbar, sidebar) {
		if (!mxClient.isBrowserSupported()) {
			mxUtils.error('Browser is not supported!', 200, false);
			$scope.state.error = true;
			$scope.errorMessage = "Your browser is not supported with this editor!";
		} else {
			// Specifies shadow opacity, color and offset
			mxConstants.SHADOW_OPACITY = 0.5;
			mxConstants.SHADOWCOLOR = '#C0C0C0';
			mxConstants.SHADOW_OFFSET_X = 0;
			mxConstants.SHADOW_OFFSET_Y = 0;

			// Table icon dimensions and position
			mxSwimlane.prototype.imageSize = 20;
			mxSwimlane.prototype.imageDx = 16;
			mxSwimlane.prototype.imageDy = 4;

			// Changes swimlane icon bounds
			mxSwimlane.prototype.getImageBounds = function (x, y, w, h) {
				return new mxRectangle(x + this.imageDx, y + this.imageDy, this.imageSize, this.imageSize);
			};

			// Defines an icon for creating new connections in the connection handler.
			// This will automatically disable the highlighting of the source vertex.
			mxConnectionHandler.prototype.connectImage = new mxImage('images/connector.gif', 16, 16);

			// Workaround for Internet Explorer ignoring certain CSS directives
			if (mxClient.IS_QUIRKS) {
				document.body.style.overflow = 'hidden';
				new mxDivResizer(container);
				new mxDivResizer(outline);
				new mxDivResizer(toolbar);
				new mxDivResizer(sidebar);
			}

			// Creates the graph inside the given container. The
			// editor is used to create certain functionality for the
			// graph, such as the rubberband selection, but most parts
			// of the UI are custom in this example.
			let editor = new mxEditor();
			$scope.graph = editor.graph;

			initClipboard($scope.graph);

			// Disables some global features
			$scope.graph.setConnectable(true);
			$scope.graph.setCellsDisconnectable(false);
			$scope.graph.setCellsCloneable(false);
			$scope.graph.swimlaneNesting = false;
			$scope.graph.dropEnabled = true;

			// Does not allow dangling edges
			$scope.graph.setAllowDanglingEdges(false);

			// Forces use of default edge in mxConnectionHandler
			$scope.graph.connectionHandler.factoryMethod = null;

			// Only tables are resizable
			$scope.graph.isCellResizable = function (cell) {
				return this.isSwimlane(cell);
			};

			// Only tables are movable
			$scope.graph.isCellMovable = function (cell) {
				return this.isSwimlane(cell);
			};

			// Sets the graph container and configures the editor
			editor.setGraphContainer(container);
			let config = mxUtils.load(
				'editors/config/keyhandler-minimal.xml').
				getDocumentElement();
			editor.configure(config);

			// Configures the automatic layout for the table columns
			editor.layoutSwimlanes = true;
			editor.createSwimlaneLayout = function () {
				let layout = new mxStackLayout($scope.graph, false);
				layout.fill = true;
				layout.resizeParent = true;

				// Overrides the function to always return true
				layout.isVertexMovable = function () {
					return true;
				};

				return layout;
			};

			// Text label changes will go into the name field of the user object
			$scope.graph.model.valueForCellChanged = function (cell, value) {
				if (value.name != null) {
					return mxGraphModel.prototype.valueForCellChanged.apply(this, arguments);
				}
				let old = cell.value.name;
				cell.value.name = value;
				return old;
			};

			// Columns are dynamically created HTML labels
			$scope.graph.isHtmlLabel = function (cell) {
				return !this.isSwimlane(cell) &&
					!$scope.graph.model.isEdge(cell);
			};

			// Edges are not editable
			$scope.graph.isCellEditable = function (cell) {
				return !$scope.graph.model.isEdge(cell);
			};

			// Returns the name field of the user object for the label
			$scope.graph.convertValueToString = function (cell) {
				if (cell.value != null && cell.value.name != null) {
					return cell.value.name;
				}

				return mxGraph.prototype.convertValueToString.apply(this, arguments); // "supercall"
			};

			// Returns the type as the tooltip for column cells
			$scope.graph.getTooltip = function (state) {
				if (this.isHtmlLabel(state.cell)) {
					return 'Type: ' + state.cell.value.type;
				} else if ($scope.graph.model.isEdge(state.cell)) {
					let source = $scope.graph.model.getTerminal(state.cell, true);
					let parent = $scope.graph.model.getParent(source);

					return parent.value.name + '.' + source.value.name;
				}

				return mxGraph.prototype.getTooltip.apply(this, arguments); // "supercall"
			};

			// Creates a dynamic HTML label for column fields
			$scope.graph.getLabel = function (cell) {
				if (this.isHtmlLabel(cell)) {
					let label = '';
					if (cell.value.primaryKey === 'true') {
						label += '<i title="Primary Key" class="dsm-table-icon sap-icon--key"></i>';
					} else {
						label += '<i class="dsm-table-spacer"></i>';
					}
					if (cell.value.autoIncrement === 'true') {
						label += '<i title="Auto Increment" class="dsm-table-icon sap-icon--add"></i>';
					} else if (cell.value.unique === 'true') {
						label += '<i title="Unique" class="dsm-table-icon sap-icon--accept"></i>';
					} else {
						label += '<i class="dsm-table-spacer"></i>';
					}
					let suffix = ': ' + mxUtils.htmlEntities(cell.value.type, false) + (cell.value.columnLength && (cell.value.type === 'CHAR' || cell.value.type === 'VARCHAR') ?
						'(' + cell.value.columnLength + ')' : '') + (cell.value.type === 'DECIMAL' && cell.value.precision && cell.value.scale ?
							'(' + cell.value.precision + ',' + cell.value.scale + ')' : '');
					suffix = cell.value.isSQL ? '' : suffix;
					return label + mxUtils.htmlEntities(cell.value.name, false) + suffix;
				}

				return mxGraph.prototype.getLabel.apply(this, arguments); // "supercall"
			};

			// Removes the source vertex if edges are removed
			$scope.graph.addListener(mxEvent.REMOVE_CELLS, function (sender, evt) {
				let cells = evt.getProperty('cells');
				for (let i = 0; i < cells.length; i++) {
					let cell = cells[i];
					if ($scope.graph.model.isEdge(cell)) {
						let terminal = $scope.graph.model.getTerminal(cell, true);
						// let parent = $scope.graph.model.getParent(terminal);
						$scope.graph.model.remove(terminal);
					}
				}
			});

			// Disables drag-and-drop into non-swimlanes.
			$scope.graph.isValidDropTarget = function (cell, cells, evt) {
				return this.isSwimlane(cell);
			};

			// Installs a popupmenu handler using local function (see below).
			$scope.graph.popupMenuHandler.factoryMethod = function (menu, cell, evt) {
				createPopupMenu(editor, $scope.graph, menu, cell, evt);
			};

			// Adds all required styles to the graph (see below)
			configureStylesheet($scope.graph);

			// Adds sidebar icon for the table object
			let tableObject = new Table('TABLENAME');
			let table = new mxCell(tableObject, new mxGeometry(0, 0, 200, 28), 'table');

			table.setVertex(true);
			addSidebarIcon($scope.graph, sidebar, table, 'sap-icon--table-view', 'Drag this to the diagram to create a new Table', $scope);

			// Adds sidebar icon for the column object
			let columnObject = new Column('COLUMNNAME');
			let column = new mxCell(columnObject, new mxGeometry(0, 0, 0, 26));

			column.setVertex(true);
			column.setConnectable(false);

			addSidebarIcon($scope.graph, sidebar, column, 'sap-icon--table-column', 'Drag this to a Table to create a new Column', $scope);

			// Adds sidebar icon for the view object
			let viewObject = new View('VIEWENAME');
			let view = new mxCell(viewObject, new mxGeometry(0, 0, 200, 28), 'view');

			view.setVertex(true);
			addSidebarIcon($scope.graph, sidebar, view, 'sap-icon--border', 'Drag this to the diagram to create a new View', $scope);

			// Adds primary key field into table
			let firstColumn = column.clone();

			firstColumn.value.name = 'TABLENAME_ID';
			firstColumn.value.type = 'INTEGER';
			firstColumn.value.columnLength = 0;
			firstColumn.value.primaryKey = 'true';
			firstColumn.value.autoIncrement = 'true';

			table.insert(firstColumn);

			// Adds sql field into view
			let sqlColumn = column.clone();

			sqlColumn.value.name = 'SELECT ...';
			sqlColumn.value.isSQL = true;

			view.insert(sqlColumn);

			// Adds child columns for new connections between tables
			$scope.graph.addEdge = function (edge, parent, source, target, index) {
				// check whether the source is view
				if (source.value.type === 'VIEW') {
					showAlert('Drop', 'Source must be a Table not a View');
					return;
				}
				// Finds the primary key child of the target table
				let primaryKey = null;
				let childCount = $scope.graph.model.getChildCount(target);

				for (let i = 0; i < childCount; i++) {
					let child = $scope.graph.model.getChildAt(target, i);
					if (child.value.primaryKey) {
						primaryKey = child;
						break;
					}
				}

				if (primaryKey.value.primaryKey !== 'true') {
					showAlert('Drop', 'Target Table must have a Primary Key');
					return;
				}

				$scope.graph.model.beginUpdate();
				try {
					let col1 = $scope.graph.model.cloneCell(column);
					col1.value.name = primaryKey.value.name;
					col1.value.type = primaryKey.value.type;
					col1.value.columnLength = primaryKey.value.columnLength;
					this.addCell(col1, source);
					source = col1;
					target = primaryKey;
					return mxGraph.prototype.addEdge.apply(this, arguments); // "supercall"
				} finally {
					$scope.graph.model.endUpdate();
				}
			};

			// Creates a new DIV that is used as a toolbar and adds
			// toolbar buttons.
			let spacer = document.createElement('div');
			spacer.style.display = 'inline';
			spacer.style.padding = '8px';

			// Defines a new export action
			editor.addAction('save', function (editor, cell) {
				$scope.saveSchema($scope.graph);
			});
			// Defines a new export action
			editor.addAction('properties', function (editor, cell) {
				if (!cell) {
					cell = $scope.graph.getSelectionCell();
				}
				if ($scope.graph.isHtmlLabel(cell)) {
					if (cell) {
						// assume column
						if (cell.value.isSQL) {
							// assume View's (the only) column
							dialogHub.showFormDialog({
								title: 'Column SQL properties',
								form: {
									[`dsmt-${cell.id}`]: {
										label: 'Name',
										controlType: 'input',
										placeholder: 'Enter name',
										type: 'text',
										value: cell.value.name,
										focus: true,
										required: true,
										submitOnEnter: true
									}
								},
								submitLabel: 'Update',
								cancelLabel: 'Cancel'
							}).then((form) => {
								if (form) {
									// Maybe we should do this with "cell.value.clone()'
									// let refCell = $scope.graph.model.getCell(cell.id);
									cell.value.name = form[`dsmt-${cell.id}`];
									$scope.$evalAsync(() => {
										$scope.graph.model.setValue(cell, cell.value);
									});
								}
							}, (error) => {
								console.error(error);
								dialogHub.showAlert({
									title: 'Column SQL properties error',
									message: 'There was an error while updating the column SQL propertie.',
									type: AlertTypes.Error,
									preformatted: false,
								});
							});
						} else {
							// assume Table's column
							dialogHub.showFormDialog({
								title: 'Column properties',
								form: {
									[`dsmt-${cell.id}`]: {
										label: 'SQL',
										controlType: 'input',
										type: 'text',
										value: cell.value.name,
										focus: true,
									},
									'dsmdType': {
										label: 'Data type',
										controlType: 'dropdown',
										options: $scope.dataTypes,
										value: cell.value.type,
										required: true,
									},
									'dsmcpLength': {
										label: 'Length',
										controlType: 'input',
										placeholder: '20',
										inputRules: {
											patterns: ['^[0-9]*$'],
										},
										type: 'text',
										value: cell.value.columnLength,
										hiddenOn: { key: 'dsmdType', value: 'DECIMAL' }
										//visibleOn: { key: 'dsmdType', value: ['VARCHAR', 'CHAR'] }
									},
									'dsmcpPrimaryKey': {
										label: 'Primary Key',
										controlType: 'checkbox',
										value: $scope.getBoolean(cell.value.primaryKey),
									},
									'dsmcpAutoIncrement': {
										label: 'Auto Increment',
										controlType: 'checkbox',
										value: $scope.getBoolean(cell.value.autoIncrement),
									},
									'dsmcpNotNull': {
										label: 'Not null',
										controlType: 'checkbox',
										value: $scope.getBoolean(cell.value.notNull),
									},
									'dsmcpunique': {
										label: 'Unique',
										controlType: 'checkbox',
										value: $scope.getBoolean(cell.value.unique),
									},
									'dsmcpPrecision': {
										label: 'Precision',
										controlType: 'input',
										placeholder: 'Enter precision number',
										inputRules: {
											patterns: ['^[0-9]*$'],
										},
										type: 'text',
										value: cell.value.precision,
										visibleOn: { key: 'dsmdType', value: 'DECIMAL' }
									},
									'dsmcpScale': {
										label: 'Scale',
										controlType: 'input',
										placeholder: 'Enter scale number',
										inputRules: {
											patterns: ['^[0-9]*$'],
										},
										type: 'text',
										value: cell.value.scale,
										visibleOn: { key: 'dsmdType', value: 'DECIMAL' }
									},
									'dsmcpDefaultValue': {
										label: 'Default Value',
										controlType: 'input',
										placeholder: "Enter value",
										inputRules: {
											patterns: ['^[0-9]*$'],
										},
										type: 'text',
										value: cell.value.defaultValue,
									},
								},
								submitLabel: 'Update',
								cancelLabel: 'Cancel'
							}).then((form) => {
								if (form) {
									// Maybe we should do this with "cell.value.clone()'
									// let refCell = $scope.graph.model.getCell(cell.id);
									cell.value.name = form[`dsmt-${cell.id}`];
									cell.value.type = form['dsmdType'];
									cell.value.columnLength = form['dsmcpLength'];
									cell.value.primaryKey = `${form['dsmcpPrimaryKey']}`;
									cell.value.autoIncrement = `${form['dsmcpAutoIncrement']}`;
									cell.value.notNull = `${form['dsmcpNotNull']}`;
									cell.value.unique = `${form['dsmcpunique']}`;
									cell.value.precision = form['dsmcpPrecision'];
									cell.value.scale = form['dsmcpScale'];
									cell.value.defaultValue = form['dsmcpDefaultValue'];
									$scope.$evalAsync(() => {
										$scope.graph.model.setValue(cell, cell.value);
									});
								}
							}, (error) => {
								console.error(error);
								dialogHub.showAlert({
									title: 'Column properties error',
									message: 'There was an error while updating the column propertie.',
									type: AlertTypes.Error,
									preformatted: false,
								});
							});
						}
					} else {
						dialogHub.showAlert({
							title: 'Nothing is selected',
							message: 'Please select a table, view or column.',
							type: AlertTypes.Warning,
							preformatted: false,
						});
					}
				} else {
					// assume Table, View or Connector
					if (cell.value) {
						// assume Table or View
						dialogHub.showFormDialog({
							title: (cell.value.type === "TABLE") ? "Table properties" : "View properties",
							form: {
								[`dsmt-${cell.id}`]: {
									label: 'Name',
									controlType: 'input',
									type: 'text',
									value: cell.value.name,
									focus: true,
									submitOnEnter: true,
								},
							},
							submitLabel: 'Update',
							cancelLabel: 'Cancel'
						}).then((form) => {
							if (form) {
								// let refCell = $scope.graph.model.getCell(cell.id);
								cell.value.name = form[`dsmt-${cell.id}`];
								$scope.$evalAsync(() => {
									$scope.graph.model.setValue(cell, cell.value);
								});
							}
						}, (error) => {
							console.error(error);
							dialogHub.showAlert({
								title: 'Column properties error',
								message: 'There was an error while updating the column propertie.',
								type: AlertTypes.Error,
								preformatted: false,
							});
						});
					}
					// else {
					// 	// assume connector
					// 	// TODO
					// }

				}
			});

			// Defines a new move up action
			editor.addAction('moveup', function (editor, cell) {
				if (cell.parent.children.length > 1) {
					$scope.graph.getModel().beginUpdate();
					try {
						for (index = 0; index < cell.parent.children.length; index++) {
							let current = cell.parent.children[index];
							if (cell.id === current.id) {
								if (index > 0) {
									let previous = cell.parent.children[index - 1];
									let y = previous.geometry.y;
									previous.geometry.y = current.geometry.y;
									current.geometry.y = y;
									cell.parent.children[index - 1] = current;
									cell.parent.children[index] = previous;
									break;
								}

							}
						}
					} finally {
						$scope.graph.getModel().endUpdate();
						$scope.graph.refresh();
					}
				}
			});

			// Defines a new move down action
			editor.addAction('movedown', function (editor, cell) {
				if (cell.parent.children.length > 2) {
					$scope.graph.getModel().beginUpdate();
					try {
						for (index = 0; index < cell.parent.children.length; index++) {
							let current = cell.parent.children[index];
							if (cell.id === current.id) {
								if (index < cell.parent.children.length - 1) {
									let next = cell.parent.children[index + 1];
									let y = next.geometry.y;
									next.geometry.y = current.geometry.y;
									current.geometry.y = y;
									cell.parent.children[index + 1] = current;
									cell.parent.children[index] = next;
									break;
								}
							}
						}
					} finally {
						$scope.graph.getModel().endUpdate();
						$scope.graph.refresh();
					}
				}
			});
			// Defines a new save action
			editor.addAction('copy', function (editor, cell) {
				mxClipboard.copy($scope.graph);
			});
			// Defines a new save action
			editor.addAction('paste', function (editor, cell) {
				mxClipboard.paste($scope.graph);
			});

			// Defines a create SQL action
			editor.addAction('showSql', function (editor, cell) {
				let sql = createSql($scope.graph);
				if (sql.length > 0) {
					dialogHub.showFormDialog({
						title: 'Schema SQL',
						form: {
							'dsmssta': {
								label: 'SQL',
								controlType: 'textarea',
								value: sql.trim(),
								rows: 20,
								focus: true,
								submitOnEnter: true,
							},
						},
						width: '100%',
						maxWidth: '1024px',
						submitLabel: 'OK',
						cancelLabel: 'Close'
					});
				} else {
					dialogHub.showAlert({
						title: 'Warning',
						message: 'Schema is empty',
						type: AlertTypes.Warning,
						preformatted: false,
					});
				}
			});

			$scope.save = function () {
				editor.execute('save');
			};
			$scope.properties = function () {
				editor.execute('properties');
			};
			$scope.copy = function () {
				editor.execute('copy');
			};
			$scope.paste = function () {
				editor.execute('paste');
			};
			$scope.undo = function () {
				editor.execute('undo');
			};
			$scope.redo = function () {
				editor.execute('redo');
			};
			$scope.delete = function () {
				editor.execute('delete');
			};
			$scope.show = function () {
				editor.execute('show');
			};
			$scope.print = function () {
				editor.execute('print');
			};
			$scope.showSql = function () {
				editor.execute('showSql');
			};
			// // Defines export XML action
			// editor.addAction('export', function (editor, cell) {
			// 	let textarea = document.createElement('textarea');
			// 	textarea.style.width = '410px';
			// 	textarea.style.height = '420px';
			// 	let enc = new mxCodec(mxUtils.createXmlDocument());
			// 	let node = enc.encode(editor.graph.getModel());
			// 	textarea.value = mxUtils.getPrettyXml(node);
			// 	showModalWindow('XML', textarea, 410, 440);
			// });
			// $scope.export = function () {
			// 	editor.execute('export');
			// };

			$scope.collapseAll = function () {
				editor.execute('collapseAll');
			};
			$scope.expandAll = function () {
				editor.execute('expandAll');
			};
			$scope.zoomIn = function () {
				editor.execute('zoomIn');
			};
			$scope.zoomOut = function () {
				editor.execute('zoomOut');
			};
			$scope.actualSize = function () {
				editor.execute('actualSize');
			};
			$scope.fit = function () {
				editor.execute('fit');
			};

			// Creates the outline (navigator, overview) for moving
			// around the graph in the top, right corner of the window.
			new mxOutline($scope.graph, outline);
		}

		let doc = mxUtils.parseXml(contents);
		let codec = new mxCodec(doc.mxGraphModel);
		codec.decode(doc.documentElement.getElementsByTagName('mxGraphModel')[0], $scope.graph.getModel());
		$scope.graph.model.addListener(mxEvent.START_EDIT, function (_sender, _evt) {
			layoutHub.setEditorDirty({
				path: $scope.dataParameters.filePath,
				dirty: true,
			});
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
		schemaFile = $scope.dataParameters.filePath.substring(0, $scope.dataParameters.filePath.lastIndexOf('.')) + '.schema';
		loadFileContents();
	}
});