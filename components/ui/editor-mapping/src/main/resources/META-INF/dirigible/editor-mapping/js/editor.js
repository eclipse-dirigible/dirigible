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
angular.module('ui.mapping.modeler', ['blimpKit', 'platformView', 'WorkspaceService']).controller('ModelerCtrl', ($scope, WorkspaceService, $window, ViewParameters) => {
	const statusBarHub = new StatusBarHub();
	const workspaceHub = new WorkspaceHub();
	const layoutHub = new LayoutHub();
	const dialogHub = new DialogHub();
	let contents;
	let mappingFile;
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
		debugger
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
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_PERIMETER] =
				mxPerimeter.EntityPerimeter;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_SHADOW] = 1;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_ROUNDED] = true;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_ARCSIZE] = 4;
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_FILLCOLOR] = '#89c5f5';
			// graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_GRADIENTCOLOR] = '#A9C4EB';
			delete $scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_STROKECOLOR];

			// Used for HTML labels that use up the complete vertex space (see
			// graph.cellRenderer.redrawLabel below for syncing the size)
			$scope.graph.stylesheet.getDefaultVertexStyle()[mxConstants.STYLE_OVERFLOW] = 'fill';

			// Uses the entity edge style as default
			$scope.graph.stylesheet.getDefaultEdgeStyle()[mxConstants.STYLE_EDGE] =
				mxEdgeStyle.EntityRelation;
			$scope.graph.stylesheet.getDefaultEdgeStyle()[mxConstants.STYLE_STROKECOLOR] = '#117dd4';
			$scope.graph.stylesheet.getDefaultEdgeStyle()[mxConstants.STYLE_FONTCOLOR] = '#117dd4';
			$scope.graph.stylesheet.getDefaultEdgeStyle()[mxConstants.STYLE_OPACITY] = '80';

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
				return this.getModel().isVertex(cell);
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
				}
				else {
					target = null;
				}

				return target;
			};

			// Adds placement of the connect icon based on the mouse event target (row)
			$scope.graph.connectionHandler.updateIcons = function (state, icons, me) {
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
				}
				else {
					icons[0].node.style.visibility = 'hidden';
				}
			};

			// Updates the targetRow in the preview edge State
			var oldMouseMove = $scope.graph.connectionHandler.mouseMove;
			$scope.graph.connectionHandler.mouseMove = function (sender, me) {
				if (this.edgeState != null) {
					debugger
					this.currentRowNode = this.updateRow(me.getSource());

					if (this.currentRow != null) {
						this.edgeState.cell.value.setAttribute('targetRow', this.currentRow);
					}
					else {
						this.edgeState.cell.value.setAttribute('targetRow', '0');
					}

					// Destroys icon to prevent event redirection via image in IE
					this.destroyIcons();
				}

				oldMouseMove.apply(this, arguments);
			};

			// Creates the edge state that may be used for preview
			$scope.graph.connectionHandler.createEdgeState = function (me) {
				var relation = doc.createElement('Relation');
				relation.setAttribute('sourceRow', this.currentRow || '0');
				relation.setAttribute('targetRow', '0');

				var edge = this.createEdge(relation);
				var style = this.graph.getCellStyle(edge);
				var state = new mxCellState(this.graph.view, edge, style);

				// Stores the source row in the handler
				this.sourceRowNode = this.currentRowNode;

				return state;
			};

			// Overrides getLabel to return empty labels for edges and
			// short markup for collapsed cells.
			$scope.graph.getLabel = function (cell) {
				if (this.getModel().isVertex(cell)) {
					if (this.isCellCollapsed(cell)) {
						return '<table style="overflow:hidden;" width="100%" height="100%" border="1" cellpadding="4" class="title" style="height:100%;">' +
							'<tr><th>Customers</th></tr>' +
							'</table>';
					}
					else {
						return '<table style="overflow:hidden;" width="100%" border="1" cellpadding="4" class="title">' +
							'<tr><th colspan="2">Customers</th></tr>' +
							'</table>' +
							'<div style="overflow:auto;cursor:default;top:26px;bottom:0px;position:absolute;width:100%;">' +
							'<table width="100%" height="100%" border="1" cellpadding="4" class="erd">' +
							'<tr><td>' +
							'</td><td>' +
							'<u>customerId</u></td></tr><tr><td></td><td>number</td></tr>' +
							'<tr><td></td><td>firstName</td></tr><tr><td></td><td>lastName</td></tr>' +
							'<tr><td></td><td>streetAddress</td></tr><tr><td></td><td>city</td></tr>' +
							'<tr><td></td><td>state</td></tr><tr><td></td><td>zip</td></tr>' +
							'</table></div>';
					}
				}
				else {
					return '';
				}
			};

			// User objects (data) for the individual cells
			var doc = mxUtils.createXmlDocument();

			// Same should be used to create the XML node for the table
			// description and the rows (most probably as child nodes)
			// var relation = doc.createElement('Relation');
			// relation.setAttribute('sourceRow', '4');
			// relation.setAttribute('targetRow', '6');

			// Enables rubberband selection
			new mxRubberband($scope.graph);

			// Enables key handling (eg. escape)
			new mxKeyHandler($scope.graph);

			// Gets the default parent for inserting new cells. This
			// is normally the first child of the root (ie. layer 0).
			var parent = $scope.graph.getDefaultParent();

			// Adds cells to the model in a single step
			var width = 160;
			var height = 230;
			$scope.graph.getModel().beginUpdate();
			try {
				var v1 = $scope.graph.insertVertex(parent, null, '', 20, 20, width, height);
				v1.geometry.alternateBounds = new mxRectangle(0, 0, width, 26);

				var v2 = $scope.graph.insertVertex(parent, null, '', 400, 20, width, height);
				v2.geometry.alternateBounds = new mxRectangle(0, 0, width, 26);

				//$scope.graph.insertEdge(parent, null, relation, v1, v2);
			}
			finally {
				// Updates the display
				$scope.graph.getModel().endUpdate();
			}

		}



	};






	//------------------------------------------------------------------

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

	//==================================================================



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