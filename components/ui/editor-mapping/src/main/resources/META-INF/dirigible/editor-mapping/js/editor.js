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
angular.module('ui.mapping.modeler', ['blimpKit', 'platformView', 'WorkspaceService']).controller('ModelerCtrl', ($scope, WorkspaceService, $window, ViewParameters, $http) => {
	const statusBarHub = new StatusBarHub();
	const workspaceHub = new WorkspaceHub();
	const layoutHub = new LayoutHub();
	const dialogHub = new DialogHub();
	let contents;
	let mappingFile;
	let databasesSvcUrl = "/services/data/";
	$scope.tables = [];
	$scope.tablesMetadata = {};
	$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
	$scope.state = {
		isBusy: true,
		error: false,
		busyText: "Loading...",
	};

	angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

	$scope.showAlert = (title, message) => {
		dialogHub.showAlert({
			title: title,
			message: message,
			type: AlertTypes.Error,
			preformatted: false,
		});
	};

	function initializeMappingJson() {
		WorkspaceService.createFile('', mappingFile, '').then(() => {
			workspaceHub.announceFileSaved({
				path: mappingFile,
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
				title: `Error saving '${mappingFile}'`,
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

	$scope.checkMapping = () => {
		WorkspaceService.resourceExists(mappingFile).then(() => { }, () => {
			initializeMappingJson();
		});
	};

	const loadFileContents = () => {
		if (!$scope.state.error) {
			$scope.state.isBusy = true;
			WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
				contents = response.data;
				$scope.checkMapping();
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

	$scope.saveMapping = () => {
		saveContents(createMapping($scope.graph), $scope.dataParameters.filePath);
		saveContents(createMappingJson($scope.graph), mappingFile);
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
			$scope.saveMapping();
		}
	});

	workspaceHub.onSaveFile((data) => {
		if (data.path && data.path === $scope.dataParameters.filePath && !$scope.state.error) {
			$scope.saveMapping();
		}
	});

	// Setting the SOURCE table
	$scope.sourceMapping = (graph) => {
		dialogHub.showFormDialog({
			title: 'Set SOURCE from available tables',
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
						$scope.source = new Table(tableMetadata.name);
						$scope.source.columns = [];
						$scope.source.name = tableMetadata.name;
						$scope.source.type = "SOURCE";

						// Gets the default parent for inserting new cells. This
						// is normally the first child of the root (ie. layer 0).
						let parent = graph.getDefaultParent();

						// Adds cells to the model in a single step
						let width = 500;
						graph.getModel().beginUpdate();
						try {

							for (let i = 0; i < tableMetadata.columns.length; i++) {
								column = new Column(tableMetadata.columns[i].name);
								column.name = tableMetadata.columns[i].name;
								column.type = tableMetadata.columns[i].type;
								column.columnLength = tableMetadata.columns[i].size;
								column.primaryKey = tableMetadata.columns[i].key;
								column.nullable = tableMetadata.columns[i].nullable;
								$scope.source.columns.push(column);
							}
							$scope.sourceTable = graph.insertVertex(parent, null, $scope.source, 20, 20,
								width, ($scope.source.columns.length + 1) * 28, 'table');
							$scope.sourceTable.geometry.alternateBounds = new mxRectangle(0, 0, width, 26);

						} finally {
							// Updates the display
							graph.getModel().endUpdate();
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

	$scope.targetMapping = (graph) => {
		dialogHub.showFormDialog({
			title: 'Set TARGET from available tables',
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
						$scope.target = new Table(tableMetadata.name);
						$scope.target.columns = [];
						$scope.target.name = tableMetadata.name;
						$scope.target.type = "TARGET";

						// Gets the default parent for inserting new cells. This
						// is normally the first child of the root (ie. layer 0).
						let parent = graph.getDefaultParent();

						// Adds cells to the model in a single step
						let width = 500;
						graph.getModel().beginUpdate();
						try {

							for (let i = 0; i < tableMetadata.columns.length; i++) {
								column = new Column(tableMetadata.columns[i].name);
								column.name = tableMetadata.columns[i].name;
								column.type = tableMetadata.columns[i].type;
								column.columnLength = tableMetadata.columns[i].size;
								column.primaryKey = tableMetadata.columns[i].key;
								column.nullable = tableMetadata.columns[i].nullable;
								$scope.target.columns.push(column);
							}

							$scope.targetTable = graph.insertVertex(parent, null, $scope.target, 650, 20,
								width, ($scope.target.columns.length + 1) * 28, 'table');
							$scope.targetTable.geometry.alternateBounds = new mxRectangle(0, 0, width, 26);

						} finally {
							// Updates the display
							graph.getModel().endUpdate();
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

	openMappingConfiguration = (columnName) => {
		const column = $scope.target.columns.find((column) => column.name === columnName);
		// alert(JSON.stringify(column));
		// assume View's (the only) column
		dialogHub.showFormDialog({
			title: 'Configuration',
			form: {
				[`dt-${column.name}`]: {
					label: 'Direct',
					controlType: 'input',
					placeholder: 'Enter direct connection',
					type: 'text',
					value: column.direct,
					focus: true,
					required: false,
					submitOnEnter: true
				},
				[`ct-${column.name}`]: {
					label: 'Constant',
					controlType: 'input',
					placeholder: 'Enter constant value',
					type: 'text',
					value: column.constant,
					focus: true,
					required: false,
					submitOnEnter: true
				},
				[`ft-${column.name}`]: {
					label: 'Formula',
					controlType: 'input',
					placeholder: 'Enter formula',
					type: 'text',
					value: column.formula,
					focus: true,
					required: false,
					submitOnEnter: true
				},
				[`mt-${column.name}`]: {
					label: 'Module',
					controlType: 'input',
					placeholder: 'Enter module path',
					type: 'text',
					value: column.module,
					focus: true,
					required: false,
					submitOnEnter: true
				},
				[`fit-${column.name}`]: {
					label: 'Filter',
					controlType: 'input',
					placeholder: 'Enter filter',
					type: 'text',
					value: column.criteria,
					focus: true,
					required: false,
					submitOnEnter: true
				}
			},
			submitLabel: 'Update',
			cancelLabel: 'Cancel'
		}).then((form) => {
			if (form) {
				column.direct = form[`dt-${column.name}`];
				column.constant = form[`ct-${column.name}`];
				column.formula = form[`ft-${column.name}`];
				column.module = form[`mt-${column.name}`];
				column.criteria = form[`fit-${column.name}`];
				$scope.refresh();
			}
		}, (error) => {
			console.error(error);
			dialogHub.showAlert({
				title: 'Column configuration error',
				message: 'There was an error while updating the column configuration.',
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	}





	function main(container, outline, toolbar, sidebar) {
		if (!mxClient.isBrowserSupported()) {
			mxUtils.error('Browser is not supported!', 200, false);
			$scope.state.error = true;
			$scope.errorMessage = "Your browser is not supported with this editor!";
		} else {

			// Must be disabled to compute positions inside the DOM tree of the cell label.
			mxGraphView.prototype.optimizeVmlReflows = false;

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

			// If connect preview is not moved away then getCellAt is used to detect the cell under
			// the mouse if the mouse is over the preview shape in IE (no event transparency), ie.
			// the built-in hit-detection of the HTML document will not be used in this case. This is
			// not a problem here since the preview moves away from the mouse as soon as it connects
			// to any given table row. This is because the edge connects to the outside of the row and
			// is aligned to the grid during the preview.
			mxConnectionHandler.prototype.movePreviewAway = false;

			// Disables foreignObjects
			mxClient.NO_FO = true;

			// Enables move preview in HTML to appear on top
			mxGraphHandler.prototype.htmlPreview = true;

			// Enables connect icons to appear on top of HTML
			mxConnectionHandler.prototype.moveIconFront = true;

			// Defines an icon for creating new connections in the connection handler.
			// This will automatically disable the highlighting of the source vertex.
			mxConnectionHandler.prototype.connectImage = new mxImage('images/connector.gif', 16, 16);

			// Support for certain CSS styles in quirks mode
			if (mxClient.IS_QUIRKS) {
				document.body.style.overflow = 'hidden';
				new mxDivResizer(container);
				new mxDivResizer(outline);
				new mxDivResizer(toolbar);
				new mxDivResizer(sidebar);
			}

			// Disables the context menu
			mxEvent.disableContextMenu(container);

			// Overrides target perimeter point for connection previews
			mxConnectionHandler.prototype.getTargetPerimeterPoint = function (state, me) {
				// Determines the y-coordinate of the target perimeter point
				// by using the currentRowNode assigned in updateRow
				var y = me.getY();

				if (this.currentRowNode != null) {
					y = getRowY(state, this.currentRowNode);
				}

				// Checks on which side of the terminal to leave
				var x = state.x;

				if (this.previous.getCenterX() > state.getCenterX()) {
					x += state.width;
				}

				return new mxPoint(x, y);
			};

			// Overrides source perimeter point for connection previews
			mxConnectionHandler.prototype.getSourcePerimeterPoint = function (state, next, me) {
				var y = me.getY();

				if (this.sourceRowNode != null) {
					y = getRowY(state, this.sourceRowNode);
				}

				// Checks on which side of the terminal to leave
				var x = state.x;

				if (next.x > state.getCenterX()) {
					x += state.width;
				}

				return new mxPoint(x, y);
			};

			// Disables connections to invalid rows
			mxConnectionHandler.prototype.isValidTarget = function (cell) {
				return this.currentRowNode != null;
			};

			// Creates the graph inside the given container
			let editor = new mxEditor();
			$scope.graph = editor.graph;

			// Uses the entity perimeter (below) as default
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_VERTICAL_ALIGN] = mxConstants.ALIGN_TOP;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_PERIMETER] = mxPerimeter.EntityPerimeter;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_SHADOW] = false;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_ROUNDED] = true;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_ARCSIZE] = 4;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_FILLCOLOR] = 'transparent';
			// graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_GRADIENTCOLOR] = '#A9C4EB';
			// delete $scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_STROKECOLOR];

			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_STROKECOLOR] = '#00cc66';
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_FONTCOLOR] = 'var(--sapTextColor)';
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_FONTSTYLE] = 0;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_OPACITY] = '80';
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_STROKEWIDTH] = '2';

			// Used for HTML labels that use up the complete vertex space (see
			// graph.cellRenderer.redrawLabel below for syncing the size)
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_OVERFLOW] = 'fill';

			// Uses the entity edge style as default
			$scope.graph.stylesheet.getDefaultEdgeStyle()[mxConstants.STYLE_STROKECOLOR] = '#00cc66';
			$scope.graph.stylesheet.getDefaultEdgeStyle()[mxConstants.STYLE_STROKEWIDTH] = '2';
			$scope.graph.stylesheet.getDefaultEdgeStyle()[mxConstants.STYLE_ROUNDED] = true;
			$scope.graph.stylesheet.getDefaultEdgeStyle()[mxConstants.STYLE_EDGE] = mxEdgeStyle.EntityRelation;



			// Allows new connections to be made but do not allow existing
			// connections to be changed for the sake of simplicity of this
			// example
			$scope.graph.setCellsDisconnectable(false);
			$scope.graph.setAllowDanglingEdges(false);
			$scope.graph.setCellsEditable(false);
			$scope.graph.setConnectable(true);
			$scope.graph.setPanning(true);
			$scope.graph.centerZoom = false;

			// Forces use of default edge in mxConnectionHandler
			$scope.graph.connectionHandler.factoryMethod = null;

			// Override folding to allow for tables
			$scope.graph.isCellFoldable = function (cell, collapse) {
				// return this.getModel().isVertex(cell);
				return false;
			};

			// Overrides connectable state
			$scope.graph.isCellConnectable = function (cell) {
				return !this.isCellCollapsed(cell);
			};

			// Enables HTML markup in all labels
			$scope.graph.setHtmlLabels(true);

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

			// Scroll events should not start moving the vertex
			$scope.graph.cellRenderer.isLabelEvent = function (state, evt) {
				var source = mxEvent.getSource(evt);

				return state.text != null && source != state.text.node &&
					source != state.text.node.getElementsByTagName('div')[0];
			};

			// Adds scrollbars to the outermost div and keeps the
			// DIV position and size the same as the vertex
			var oldRedrawLabel = $scope.graph.cellRenderer.redrawLabel;
			$scope.graph.cellRenderer.redrawLabel = function (state) {
				oldRedrawLabel.apply(this, arguments); // "supercall"
				var graph = state.view.graph;
				var model = graph.model;

				if (model.isVertex(state.cell) && state.text != null) {
					// Scrollbars are on the div
					var s = graph.view.scale;
					var div = state.text.node.getElementsByTagName('div')[2];

					if (div != null) {
						// Installs the handler for updating connected edges
						if (div.scrollHandler == null) {
							div.scrollHandler = true;

							var updateEdges = mxUtils.bind(this, function () {
								var edgeCount = model.getEdgeCount(state.cell);

								// Only updates edges to avoid update in DOM order
								// for text label which would reset the scrollbar
								for (var i = 0; i < edgeCount; i++) {
									var edge = model.getEdgeAt(state.cell, i);
									graph.view.invalidate(edge, true, false);
									graph.view.validate(edge);
								}
							});

							mxEvent.addListener(div, 'scroll', updateEdges);
							mxEvent.addListener(div, 'mouseup', updateEdges);
						}
					}
				}
			};

			// Adds a new function to update the currentRow based on the given event
			// and return the DOM node for that row
			$scope.graph.connectionHandler.updateRow = function (target) {
				while (target != null && target.nodeName != 'TR') {
					target = target.parentNode;
				}

				this.currentRow = null;

				// Checks if we're dealing with a row in the correct table
				if (target != null && target.parentNode.parentNode.className == 'erd') {
					// Stores the current row number in a property so that it can
					// be retrieved to create the preview and final edge
					var rowNumber = 0;
					var current = target.parentNode.firstChild;

					while (target != current && current != null) {
						current = current.nextSibling;
						rowNumber++;
					}

					this.currentRow = rowNumber + 1;
				} else {
					target = null;
				}

				return target;
			};

			// Adds placement of the connect icon based on the mouse event target (row)
			$scope.graph.connectionHandler.updateIcons = function (state, icons, me) {

				if (state && state.cell && state.cell.value && state.cell.value.type === 'SOURCE') {
					var target = me.getSource();
					target = this.updateRow(target);

					if (target != null && this.currentRow != null) {
						var div = target.parentNode.parentNode.parentNode;
						var s = state.view.scale;

						icons[0].node.style.visibility = 'visible';
						icons[0].bounds.x = state.x + target.offsetLeft + Math.min(state.width,
							target.offsetWidth * s) - this.icons[0].bounds.width - 2;
						icons[0].bounds.y = state.y - this.icons[0].bounds.height / 2 + (target.offsetTop +
							target.offsetHeight / 2 - div.scrollTop + div.offsetTop) * s;
						icons[0].redraw();

						this.currentRowNode = target;
					} else {
						icons[0].node.style.visibility = 'hidden';
					}
				} else {
					icons[0].node.style.visibility = 'hidden';
				}
			};

			// Updates the targetRow in the preview edge State
			var oldMouseMove = $scope.graph.connectionHandler.mouseMove;
			$scope.graph.connectionHandler.mouseMove = function (sender, me) {
				if (this.edgeState != null) {
					this.currentRowNode = this.updateRow(me.getSource());

					if (this.currentRow != null) {
						this.edgeState.cell.value.setAttribute('targetRow', this.currentRow);
					} else {
						this.edgeState.cell.value.setAttribute('targetRow', '0');
					}

					// Destroys icon to prevent event redirection via image in IE
					this.destroyIcons();
				}

				oldMouseMove.apply(this, arguments);
			};

			// Updates the column in the preview edge State
			var oldMouseUp = $scope.graph.connectionHandler.mouseUp;
			$scope.graph.connectionHandler.mouseUp = function (sender, me) {
				if (this.edgeState != null) {
					this.currentRowNode = this.updateRow(me.getSource());

					if (this.currentRow != null) {
						this.edgeState.cell.value.setAttribute('targetRow', this.currentRow);
						this.edgeState.cell.value.setAttribute('targetColumn', me.state.cell.value.columns[this.currentRow - 1].name);

						// setting the configuration data
						let column = me.state.cell.value.columns[this.currentRow - 1];
						let sourceColumnName = this.edgeState.cell.value.getAttribute('sourceColumn');
						column.source = sourceColumnName;
						column.direct = 'SOURCE["' + sourceColumnName + '"]';
						$scope.refresh();
					} else {
						this.edgeState.cell.value.setAttribute('targetRow', '0');
					}
				}

				oldMouseUp.apply(this, arguments);
			};

			// Creates the edge state that may be used for preview
			$scope.graph.connectionHandler.createEdgeState = function (me) {
				var relation = doc.createElement('Relation');
				relation.setAttribute('sourceRow', this.currentRow || '0');
				relation.setAttribute('targetRow', '0');
				relation.setAttribute('sourceColumn', me.state.cell.value.columns[this.currentRow - 1].name);

				var edge = this.createEdge(relation);
				var style = this.graph.getCellStyle(edge);
				var state = new mxCellState(this.graph.view, edge, style);

				// Stores the source row in the handler
				this.sourceRowNode = this.currentRowNode;

				return state;
			};

			$scope.graph.model.addListener(mxEvent.EXECUTE, function (_sender, _evt) {
				if (_evt.properties.change.child
					&& _evt.properties.change.child.value
					&& _evt.properties.change.child.value.nodeName === 'Relation'
					&& _evt.properties.change.child.source
					&& _evt.properties.change.child.target) {
					const targetColumn = _evt.properties.change.child.value.getAttribute("targetColumn");
					const targetRow = _evt.properties.change.child.value.getAttribute("targetRow");
					const targetTable = _evt.properties.change.child.target.value;
					const column = targetTable.columns[targetRow - 1];
					if (column && column.name === targetColumn) {
						delete column.direct;
					}
					$scope.refresh();
				}
			});

			// Returns the type as the tooltip for column cells
			$scope.graph.getTooltip = function (state) {
				// if (this.isHtmlLabel(state.cell)) {
				// 	return 'Type: ' + state.cell.value.type;
				// } else if ($scope.graph.model.isEdge(state.cell)) {
				// 	let source = $scope.graph.model.getTerminal(state.cell, true);
				// 	let parent = $scope.graph.model.getParent(source);

				// 	return parent.value.name + '.' + source.value.name;
				// }

				// return mxGraph.prototype.getTooltip.apply(this, arguments); // "supercall"
			};

			// Overrides getLabel to return empty labels for edges and
			// short markup for collapsed cells.
			$scope.graph.getLabel = function (cell) {
				if (this.getModel().isVertex(cell)) {
					if (this.isCellCollapsed(cell)) {
						return '<table style="overflow:hidden;" width="100%" height="100%" border="0" cellpadding="4" class="title" style="height:100%;">' +
							'<tr><th>' + cell.value.name + '</th></tr>' +
							'</table>';
					} else {
						let label = '<table style="overflow:hidden;" width="100%" border="0" cellpadding="4" class="title">' +
							'<tr><th colspan="2">' + cell.value.name + '</th></tr>' +
							'</table>';
						label += '<div style="overflow:auto;cursor:default;top:26px;bottom:0px;position:absolute;width:100%;">' +
							'<table width="100%" height="100%" border="0" cellpadding="4" class="erd">';
						for (const c of cell.value.columns) {
							label += '<tr>';
							if (cell.value.type === 'TARGET') {
								let config = 'circle-task';
								if (c.constant) {
									config = 'number-sign';
								} else if (c.criteria) {
									config = 'filter';
								} else if (c.formula) {
									config = 'syntax';
								} else if (c.module) {
									config = 'attachment';
								} else if (c.direct) {
									config = 'circle-task-2';
								}
								label += '<td><i class="dsm-table-icon sap-icon--' + config + '" onclick="openMappingConfiguration(\'' + c.name + '\')"></i></td>';
							}
							label += '<td>';
							if (c.primaryKey) {
								label += '<i title="Primary Key" class="dsm-table-icon sap-icon--key"></i>';
							} else {
								label += '<i class="dsm-table-spacer"></i>';
							}
							// if (c.autoIncrement) {
							// 	label += '<i title="Auto Increment" class="dsm-table-icon sap-icon--add"></i>';
							// } else 
							if (!c.nullable) {
								label += '<i title="Required" class="dsm-table-icon sap-icon--favorite"></i>';
							} else {
								label += '<i class="dsm-table-spacer"></i>';
							}
							label += '</td>';
							label += '<td style="text-align: left">'
								+ c.name + '</td><td>'
								+ c.type + '</td><td>'
								+ c.columnLength + '</td></tr>';

						}
						label += '</table></div>';
						return label;
					}
				} else {
					return '';
				}
			};


			editor.addAction('source', function (editor, cell) {
				$scope.sourceMapping($scope.graph);
			});
			editor.addAction('target', function (editor, cell) {
				$scope.targetMapping($scope.graph);
			});
			editor.addAction('save', function (editor, cell) {
				$scope.saveMapping($scope.graph);
			});

			$scope.source = function () {
				editor.execute('source');
			};
			$scope.target = function () {
				editor.execute('target');
			};
			$scope.save = function () {
				editor.execute('save');
			};
			$scope.undo = function () {
				editor.execute('undo');
			};
			$scope.redo = function () {
				editor.execute('redo');
			};
			$scope.delete = function () {
				if ($scope.graph.getSelectionCount() > 0
					&& $scope.graph.getSelectionCells()[0].value
					&& $scope.graph.getSelectionCells()[0].value.nodeName === 'Relation') {
					editor.execute('delete');
				}
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
			$scope.refresh = function () {
				editor.execute('refresh');
			};

			// User objects (data) for the individual cells
			// var doc = mxUtils.createXmlDocument();


			// Load document
			let doc = mxUtils.parseXml(contents);
			let codec = new mxCodec(doc.mxGraphModel);
			codec.decode(doc.documentElement.getElementsByTagName('mxGraphModel')[0], $scope.graph.getModel());
			$scope.graph.model.addListener(mxEvent.START_EDIT, function (_sender, _evt) {
				layoutHub.setEditorDirty({
					path: $scope.dataParameters.filePath,
					dirty: true,
				});
			});

			if ($scope.graph.model.root
				&& $scope.graph.model.root.children
				&& $scope.graph.model.root.children[0]
				&& $scope.graph.model.root.children[0].children
				&& $scope.graph.model.root.children[0].children[0]) {
				$scope.source = $scope.graph.model.root.children[0].children[0].value;
				$scope.target = $scope.graph.model.root.children[0].children[1].value;

				setTimeout(() => {
					$scope.refresh();
				}, 300);

			}

			// Enables rubberband selection
			new mxRubberband($scope.graph);

			// Enables key handling (eg. escape)
			new mxKeyHandler($scope.graph);

			// Load database(s) metadata
			loadDatabasesMetadata();

		}
	};

	// Implements a special perimeter for table rows inside the table markup
	mxGraphView.prototype.updateFloatingTerminalPoint = function (edge, start, end, source) {
		var next = this.getNextPoint(edge, end, source);
		var div = start.text.node.getElementsByTagName('div')[2];

		var x = start.x;
		var y = start.getCenterY();

		// Checks on which side of the terminal to leave
		if (next.x > x + start.width / 2) {
			x += start.width;
		}

		if (div != null) {
			y = start.getCenterY() - div.scrollTop;

			if (mxUtils.isNode(edge.cell.value) && !this.graph.isCellCollapsed(start.cell)) {
				var attr = (source) ? 'sourceRow' : 'targetRow';
				var row = parseInt(edge.cell.value.getAttribute(attr));

				// HTML labels contain an outer table which is built-in
				var table = div.getElementsByTagName('table')[0];
				var trs = table.getElementsByTagName('tr');
				var tr = trs[Math.min(trs.length - 1, row - 1)];

				// Gets vertical center of source or target row
				if (tr != null) {
					y = getRowY(start, tr);
				}
			}

			// Keeps vertical coordinate inside start
			var offsetTop = parseInt(div.style.top) * start.view.scale;
			y = Math.min(start.y + start.height, Math.max(start.y + offsetTop, y));

			// Updates the vertical position of the nearest point if we're not
			// dealing with a connection preview, in which case either the
			// edgeState or the absolutePoints are null
			if (edge != null && edge.absolutePoints != null) {
				next.y = y;
			}
		}

		edge.setAbsoluteTerminalPoint(new mxPoint(x, y), source);

		// Routes multiple incoming edges along common waypoints if
		// the edges have a common target row
		if (source && mxUtils.isNode(edge.cell.value) && start != null && end != null) {
			var edges = this.graph.getEdgesBetween(start.cell, end.cell, true);
			var tmp = [];

			// Filters the edges with the same source row
			var row = edge.cell.value.getAttribute('targetRow');

			for (var i = 0; i < edges.length; i++) {
				if (mxUtils.isNode(edges[i].value) &&
					edges[i].value.getAttribute('targetRow') == row) {
					tmp.push(edges[i]);
				}
			}

			edges = tmp;

			if (edges.length > 1 && edge.cell == edges[edges.length - 1]) {
				// Finds the vertical center
				var states = [];
				var y = 0;

				for (var i = 0; i < edges.length; i++) {
					states[i] = this.getState(edges[i]);
					y += states[i].absolutePoints[0].y;
				}

				y /= edges.length;

				for (var i = 0; i < states.length; i++) {
					var x = states[i].absolutePoints[1].x;

					if (states[i].absolutePoints.length < 5) {
						states[i].absolutePoints.splice(2, 0, new mxPoint(x, y));
					}
					else {
						states[i].absolutePoints[2] = new mxPoint(x, y);
					}

					// Must redraw the previous edges with the changed point
					if (i < states.length - 1) {
						this.graph.cellRenderer.redraw(states[i]);
					}
				}
			}
		}
	};

	// Defines global helper function to get y-coordinate for a given cell state and row
	var getRowY = function (state, tr) {
		var s = state.view.scale;
		var div = tr.parentNode.parentNode.parentNode;
		var offsetTop = parseInt(div.style.top);
		var y = state.y + (tr.offsetTop + tr.offsetHeight / 2 - div.scrollTop + offsetTop) * s;
		y = Math.min(state.y + state.height, Math.max(state.y + offsetTop * s, y));

		return y;
	};

	function uuidv4() {
		return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, c =>
			(+c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> +c / 4).toString(16)
		);
	}

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
										let tableLabel = datasources[j] + ' : ' + schemas[k].name + ' : ' + schema.tables[m].name;
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

	$scope.dataParameters = ViewParameters.get();
	if (!$scope.dataParameters.hasOwnProperty('filePath')) {
		$scope.state.error = true;
		$scope.errorMessage = 'The \'filePath\' data parameter is missing.';
	} else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
		$scope.state.error = true;
		$scope.errorMessage = 'The \'contentType\' data parameter is missing.';
	} else {
		mappingFile = $scope.dataParameters.filePath.substring(0, $scope.dataParameters.filePath.lastIndexOf('.')) + '.mapping';
		loadFileContents();
	}

});