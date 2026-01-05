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
angular.module('ui.entity-data.modeler', ['blimpKit', 'platformView', 'WorkspaceService', "GenerateService", "TemplatesService"])
	.controller('ModelerCtrl', function ($scope, $window, WorkspaceService, GenerateService, TemplatesService, ViewParameters, uuid) {
		const statusBarHub = new StatusBarHub();
		const workspaceHub = new WorkspaceHub();
		const layoutHub = new LayoutHub();
		const dialogHub = new DialogHub();
		let contents;
		let modelFile = '';
		let genFile = '';
		let fileWorkspace = '';
		$scope.canRegenerate = false;
		$scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
		$scope.forms = {
			editor: {},
		};
		$scope.state = {
			isBusy: true,
			error: false,
			busyText: "Loading...",
		};

		$scope.relationshipTypes = [
			{ value: "ASSOCIATION", label: "Association" },
			{ value: "AGGREGATION", label: "Aggregation" },
			{ value: "COMPOSITION", label: "Composition" },
			{ value: "EXTENSION", label: "Extension" }
		];

		$scope.relationshipCardinalities = [
			{ value: "1_1", label: "one-to-one" },
			{ value: "1_n", label: "one-to-many" },
			{ value: "n_1", label: "many-to-one" },
		];

		angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

		const loadFileContents = () => {
			if (!$scope.state.error) {
				$scope.state.isBusy = true;
				WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
					contents = response.data;
					initializeModelJson();
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

		$scope.checkGenFile = () => {
			WorkspaceService.resourceExists(genFile).then(() => {
				$scope.canRegenerate = true;
			}, () => {
				$scope.canRegenerate = false;
			});
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

		function initializeModelJson() {
			WorkspaceService.resourceExists(modelFile).then(() => {
				$scope.checkGenFile();
			}, () => {
				WorkspaceService.createFile('', modelFile, '').then(() => {
					workspaceHub.announceFileSaved({
						path: modelFile,
						contentType: $scope.dataParameters.contentType,
					});
					workspaceHub.postMessage({
						topic: 'projects.tree.refresh',
						data: { partial: true, project: $scope.dataParameters.filePath.substring($scope.dataParameters.filePath.indexOf('/', fileWorkspace.length + 2), fileWorkspace.length + 2), workspace: fileWorkspace }
					});
					$scope.checkGenFile();
				}, (error) => {
					console.error(error);
					dialogHub.showAlert({
						title: `Error saving '${modelFile}'`,
						message: 'Please look at the console for more information',
						type: AlertTypes.Error,
						preformatted: false,
					});
				});
			});
		}

		$scope.chooseTemplate = (project, filePath, params) => {
			const templateItems = [];
			TemplatesService.listTemplates().then((response) => {
				for (let i = 0; i < response.data.length; i++) {
					if (response.data[i].hasOwnProperty('extension') && response.data[i].extension === 'model') {
						templateItems.push({
							label: response.data[i].name,
							value: response.data[i].id,
						});
					}
				}
				dialogHub.closeBusyDialog();
				dialogHub.showFormDialog({
					title: 'Choose template',
					form: {
						'pgfd1': {
							label: 'Choose template',
							controlType: 'dropdown',
							options: templateItems,
							required: true,
							focus: true
						},
					},
					submitLabel: 'OK',
					cancelLabel: 'Cancel'
				}).then((form) => {
					if (form) {
						dialogHub.showBusyDialog('Regenerating from model');
						$scope.generateFromModel(project, filePath, form['pgfd1'], params);
					}
				}, (error) => {
					console.error(error);
					dialogHub.showAlert({
						title: 'Choose template error',
						message: 'There was an error while processing the template.',
						type: AlertTypes.Error,
						preformatted: false,
					});
				});
			}, (error) => {
				console.error(error);
				dialogHub.showAlert({
					title: 'Template service error',
					message: 'Unable to load template list. Please look at the console for more information.',
					type: AlertTypes.Error,
					preformatted: false,
				});
			});
		};

		$scope.generateFromModel = (project, filePath, templateId, params) => {
			GenerateService.generateFromModel(
				fileWorkspace,
				project,
				filePath,
				templateId,
				params
			).then(() => {
				statusBarHub.showMessage(`Generated from model '${filePath}'`);
			}, (error) => {
				console.error(error);
				dialogHub.showAlert({
					title: 'Failed to generate from model',
					message: `An unexpected error has occurred while trying generate from model '${filePath}'`,
					type: AlertTypes.Error,
					preformatted: false,
				});
			}).finally(() => {
				workspaceHub.postMessage({
					topic: 'projects.tree.refresh',
					data: { partial: true, project: project, workspace: fileWorkspace }
				});
				dialogHub.closeBusyDialog();
			});
		};

		$scope.regenerate = () => {
			dialogHub.showBusyDialog('Regenerating');
			$scope.save();
			WorkspaceService.loadContent(genFile).then((response) => {
				let { models, perspectives, templateId, filePath, workspaceName, projectName, ...params } = response.data;
				if (!response.data.templateId) {
					$scope.chooseTemplate(response.data.projectName, response.data.filePath, params);
				} else {
					dialogHub.showBusyDialog('Regenerating from model');
					$scope.generateFromModel(response.data.projectName, response.data.filePath, response.data.templateId, params);
				}
			}, (error) => {
				console.error(error);
				dialogHub.showAlert({
					title: 'Unable to load model file',
					message: 'There was an error while loading the model file. See the log for more information.',
					type: AlertTypes.Error,
					preformatted: false,
				});
				dialogHub.closeBusyDialog();
			});
		};

		layoutHub.onFocusEditor((data) => {
			if (data.path && data.path === $scope.dataParameters.filePath) statusBarHub.showLabel('');
		});

		layoutHub.onReloadEditorParams((data) => {
			if (data.path === $scope.dataParameters.filePath) {
				$scope.$evalAsync(() => {
					$scope.dataParameters = ViewParameters.get();
					modelFile = $scope.dataParameters.filePath.substring(0, $scope.dataParameters.filePath.lastIndexOf('.')) + '.model';
					genFile = $scope.dataParameters.filePath.substring(0, $scope.dataParameters.filePath.lastIndexOf('.')) + '.gen';
					fileWorkspace = $scope.dataParameters.filePath.substring(1, $scope.dataParameters.filePath.indexOf('/', 1));
				});
			};
		});

		workspaceHub.onSaveAll(() => {
			if (!$scope.state.error) {
				saveContents(createModel($scope.graph));
			}
		});

		workspaceHub.onSaveFile((data) => {
			if (data.path && data.path === $scope.dataParameters.filePath && !$scope.state.error) {
				saveContents(createModel($scope.graph));
			}
		});

		dialogHub.addMessageListener({
			topic: "edm.editor.entity",
			handler: (data) => {
				let cell = $scope.graph.model.getCell(data.cellId);
				cell.value.name = data.name;
				cell.value.entityType = data.entityType;
				cell.value.dataName = data.dataName;
				cell.value.dataCount = data.dataCount;
				cell.value.dataQuery = data.dataQuery;
				cell.value.title = data.title;
				cell.value.caption = data.caption;
				cell.value.description = data.description;
				cell.value.tooltip = data.tooltip;
				cell.value.icon = data.icon;
				cell.value.menuKey = data.menuKey;
				cell.value.menuLabel = data.menuLabel;
				cell.value.menuIndex = data.menuIndex;
				cell.value.layoutType = data.layoutType;
				cell.value.perspectiveName = data.perspectiveName;
				cell.value.perspectiveLabel = data.perspectiveLabel;
				cell.value.navigationPath = data.navigationPath;
				cell.value.feedUrl = data.feedUrl;
				cell.value.feedUsername = data.feedUsername;
				cell.value.feedPassword = data.feedPassword;
				cell.value.feedSchedule = data.feedSchedule;
				cell.value.feedPath = data.feedPath;
				cell.value.generateDefaultRoles = data.generateDefaultRoles;
				cell.value.roleRead = data.roleRead;
				cell.value.roleWrite = data.roleWrite;
				cell.value.importsCode = data.importsCode;
				cell.value.generateReport = data.generateReport;
				cell.value.multilingual = data.multilingual;

				$scope.graph.model.setValue(cell, cell.value.clone());

				if (cell.entityType === 'DEPENDENT') {
					$scope.graph.getSelectionCell().style = 'dependent';
					$scope.graph.refresh();
				} else if (cell.entityType === 'COPIED') {
					$scope.graph.getSelectionCell().style = 'copied';
					$scope.graph.getSelectionCell().children.forEach(cell => cell.style = 'copiedproperty');
					$scope.graph.refresh();
				} else if (cell.entityType === 'PROJECTION') {
					$scope.graph.getSelectionCell().style = 'projection';
					$scope.graph.getSelectionCell().children.forEach(cell => cell.style = 'projectionproperty');
					$scope.graph.refresh();
				} else if (cell.entityType === 'EXTENSION') {
					$scope.graph.getSelectionCell().style = 'extension';
					$scope.graph.getSelectionCell().children.forEach(cell => cell.style = 'extensionproperty');
					$scope.graph.refresh();
				}
				dialogHub.closeWindow();
			}
		});

		dialogHub.addMessageListener({
			topic: "edm.editor.property",
			handler: (data) => {
				let cell = $scope.graph.model.getCell(data.cellId);
				cell.value.name = data.name;
				cell.value.description = data.description;
				cell.value.tooltip = data.tooltip;
				cell.value.isRequiredProperty = data.isRequiredProperty;
				cell.value.isCalculatedProperty = data.isCalculatedProperty;
				cell.value.calculatedPropertyExpressionCreate = data.calculatedPropertyExpressionCreate;
				cell.value.calculatedPropertyExpressionUpdate = data.calculatedPropertyExpressionUpdate;
				cell.value.dataName = data.dataName;
				cell.value.dataType = data.dataType;
				cell.value.dataOrderBy = data.dataOrderBy;
				cell.value.dataLength = data.dataLength;
				cell.value.dataPrimaryKey = data.dataPrimaryKey;
				cell.value.dataAutoIncrement = data.dataAutoIncrement;
				cell.value.dataNotNull = data.dataNotNull;
				cell.value.dataUnique = data.dataUnique;
				cell.value.dataPrecision = data.dataPrecision;
				cell.value.dataScale = data.dataScale;
				cell.value.dataDefaultValue = data.dataDefaultValue;
				cell.value.widgetType = data.widgetType;
				cell.value.widgetSize = data.widgetSize;
				cell.value.widgetLength = data.widgetLength;
				cell.value.widgetLabel = data.widgetLabel;
				cell.value.widgetShortLabel = data.widgetShortLabel;
				cell.value.widgetPattern = data.widgetPattern;
				cell.value.widgetFormat = data.widgetFormat;
				cell.value.widgetService = data.widgetService;
				cell.value.widgetSection = data.widgetSection;
				cell.value.widgetIsMajor = data.widgetIsMajor;
				cell.value.widgetDropDownKey = data.widgetDropDownKey;
				cell.value.widgetDropDownValue = data.widgetDropDownValue;
				cell.value.widgetDropDownMultiSelect = data.widgetDropDownMultiSelect;
				cell.value.widgetDependsOnProperty = data.widgetDependsOnProperty;
				cell.value.widgetDependsOnEntity = data.widgetDependsOnEntity;
				cell.value.widgetDependsOnValueFrom = data.widgetDependsOnValueFrom;
				cell.value.widgetDependsOnFilterBy = data.widgetDependsOnFilterBy;
				cell.value.feedPropertyName = data.feedPropertyName;
				cell.value.roleRead = data.roleRead;
				cell.value.roleWrite = data.roleWrite;
				// Maybe we should do this with "cell.value.clone()'
				$scope.graph.model.setValue(cell, cell.value);
				dialogHub.closeWindow();
			}
		});

		dialogHub.addMessageListener({
			topic: "edmEditor.navigation.details",
			handler: (data) => {
				$scope.graph.model.perspectives = data.perspectives;
				$scope.graph.model.navigations = data.navigations;
				layoutHub.setEditorDirty({
					path: $scope.dataParameters.filePath,
					dirty: true,
				});
				dialogHub.closeWindow();
			}
		});

		dialogHub.addMessageListener({
			topic: "edm.editor.reference",
			handler: (data) => {
				let model = $scope.graph.getModel();
				model.beginUpdate();
				try {
					let cell = $scope.graph.model.getCell(data.cellId);
					cell.value.name = data.entity;
					cell.value.entityType = "PROJECTION";
					cell.value.projectionReferencedModel = data.model;
					cell.value.projectionReferencedEntity = data.entity;
					cell.value.perspectiveName = data.perspectiveName;
					cell.value.perspectiveLabel = data.perspectiveLabel;
					cell.value.perspectiveIcon = data.perspectiveIcon;
					cell.value.perspectiveOrder = data.perspectiveOrder;
					cell.value.perspectiveRole = data.perspectiveRole;
					$scope.graph.model.setValue(cell, cell.value);

					for (let i = 0; i < data.entityProperties.length; i++) {
						let propertyObject = new Property('propertyName');
						let property = new mxCell(propertyObject, new mxGeometry(0, 0, 0, 26));
						property.setId(uuid.generate());
						property.setVertex(true);
						property.setConnectable(false);
						for (let attributeName in data.entityProperties[i]) {
							property.value[attributeName] = data.entityProperties[i][attributeName];
						}
						property.style = 'projectionproperty';
						cell.insert(property);
					}
					model.setCollapsed(cell, true);
				} finally {
					model.endUpdate();
				}
				$scope.graph.refresh();
				layoutHub.setEditorDirty({
					path: $scope.dataParameters.filePath,
					dirty: true,
				});
				dialogHub.closeWindow();
			}
		});

		dialogHub.addMessageListener({
			topic: "edm.editor.copiedEntity",
			handler: (data) => {
				let model = $scope.graph.getModel();
				model.beginUpdate();
				try {
					let cell = $scope.graph.model.getCell(data.cellId);
					cell.value.name = data.entity;
					cell.value.entityType = "COPIED";
					cell.value.projectionReferencedModel = data.model;
					cell.value.projectionReferencedEntity = data.entity;
					cell.value.perspectiveName = data.perspectiveName;
					cell.value.perspectiveLabel = data.perspectiveLabel;
					cell.value.perspectiveIcon = data.perspectiveIcon;
					cell.value.perspectiveOrder = data.perspectiveOrder;
					cell.value.perspectiveRole = data.perspectiveRole;
					$scope.graph.model.setValue(cell, cell.value);

					for (let i = 0; i < data.entityProperties.length; i++) {
						let propertyObject = new Property('propertyName');
						let property = new mxCell(propertyObject, new mxGeometry(0, 0, 0, 26));
						property.setId(uuid.generate());
						property.setVertex(true);
						property.setConnectable(false);
						for (let attributeName in data.entityProperties[i]) {
							property.value[attributeName] = data.entityProperties[i][attributeName];
						}
						cell.insert(property);
					}
					model.setCollapsed(cell, true);
				} finally {
					model.endUpdate();
				}

				model.setCollapsed($scope.$cell, true);
				$scope.graph.refresh();
				layoutHub.setEditorDirty({
					path: $scope.dataParameters.filePath,
					dirty: true,
				});
				dialogHub.closeWindow();
			}
		});

		function main(container, outline, toolbar, sidebar) {
			let ICON_ENTITY = 'sap-icon--header';
			let ICON_PROPERTY = 'sap-icon--bullet-text';
			let ICON_DEPENDENT = 'sap-icon--accelerated';
			let ICON_REPORT = 'sap-icon--area-chart';
			let ICON_SETTING = 'sap-icon--wrench';
			let ICON_FILTER = 'sap-icon--filter';
			let ICON_COPIED = 'sap-icon--duplicate';
			let ICON_PROJECTION = 'sap-icon--journey-arrive';
			let ICON_EXTENSION = 'sap-icon--puzzle';

			function replaceSpecialSymbols(value) {
				let v = value.replace(/[`~!@#$%^&*()_|+\-=?;:'",.<>\{\}\[\]\\\/]/gi, '');
				v = v.replace(/\s/g, "_");
				return v;
			}

			// Checks if the browser is supported
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

				// Entity icon dimensions and position
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

				// Only entities are resizable
				$scope.graph.isCellResizable = function (cell) {
					return this.isSwimlane(cell);
				};

				const selectionModel = $scope.graph.getSelectionModel();

				// Only entities are movable
				$scope.graph.isCellMovable = function (cell) {
					if (selectionModel.cells.length <= 1 && cell.style === undefined && cell.parent.value && cell.parent.value.entityType) {
						return true;
					}
					return this.isSwimlane(cell);
				};

				$scope.graph.model.createId = function (_cell) {
					const id = uuid.generate();
					return this.prefix + id + this.postfix;
				};

				// Sets the graph container and configures the editor
				editor.setGraphContainer(container);
				let config = mxUtils.load('editors/config/keyhandler-minimal.xml').getDocumentElement();
				editor.configure(config);

				// Configures the automatic layout for the entity properties
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
						value.name = replaceSpecialSymbols(value.name);
						return mxGraphModel.prototype.valueForCellChanged.apply(this, arguments);
					}
					let old = cell.value.name;
					value = replaceSpecialSymbols(value);
					cell.value.name = value;
					return old;
				};

				// Properties are dynamically created HTML labels
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

				// Returns the type as the tooltip for property cells
				$scope.graph.getTooltip = function (state) {
					if (this.isHtmlLabel(state.cell)) {
						return 'Type: ' + state.cell.value.dataType;
					} else if ($scope.graph.model.isEdge(state.cell)) {
						let source = $scope.graph.model.getTerminal(state.cell, true);
						let parent = $scope.graph.model.getParent(source);

						return parent.value.name + '.' + source.value.name;
					}

					return mxGraph.prototype.getTooltip.apply(this, arguments); // "supercall"
				};

				// Creates a dynamic HTML label for property fields
				$scope.graph.getLabel = function (cell) {
					if (this.isHtmlLabel(cell)) {
						let label = '';

						if (cell.value.dataPrimaryKey === 'true') {
							label += '<i title="Primary Key" class="dsm-table-icon sap-icon--key"></i>';
						} else {
							label += '<i class="dsm-table-spacer"></i>';
						}

						if (cell.value.dataAutoIncrement === 'true') {
							label += '<i title="Auto Increment" class="dsm-table-icon sap-icon--add"></i>';
						} else if (cell.value.dataUnique === 'true') {
							label += '<i title="Unique" class="dsm-table-icon sap-icon--accept"></i>';
						} else {
							label += '<i class="dsm-table-spacer"></i>';
						}

						let suffix = mxUtils.htmlEntities(cell.value.dataType, false) + (cell.value.dataLength && (cell.value.dataType === 'CHAR' || cell.value.dataType === 'VARCHAR') ? 
						'(' + cell.value.dataLength + ')' : '') + (cell.value.dataType === 'DECIMAL' && cell.value.dataPrecision && cell.value.dataScale ?
							'(' + cell.value.dataPrecision + ',' + cell.value.dataScale + ')' : '');
						return label + mxUtils.htmlEntities(cell.value.name, false) + ":" + suffix;
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

				const moveCells = $scope.graph.moveCells;

				$scope.graph.moveCells = function (cells, dx, dy, clone, target, evt, mapping) {
					if (target && cells && cells.length === 1 && cells[0].style === undefined && cells[0].parent.value.entityType) {
						if (cells[0].parent.id !== target.id) {
							return cells;
						}
					}
					return moveCells.apply(this, arguments);
				};

				// Disables drag-and-drop into non-swimlanes.
				$scope.graph.isValidDropTarget = function (cell, cells, _evt) {
					if (cells.length === 1 && cells[0].style === undefined && cells[0].parent.value.entityType) {
						if (cells[0].parent.id === cell.id) return true;
						else return false;
					}
					return this.isSwimlane(cell);
				};

				// Installs a popupmenu handler using local function (see below).
				$scope.graph.popupMenuHandler.factoryMethod = function (menu, cell, evt) {
					createPopupMenu(editor, $scope.graph, menu, cell, evt);
				};

				// Adds all required styles to the graph (see below)
				configureStylesheet($scope.graph);

				// Primary Entity ----------------------------------------------

				// Adds sidebar icon for the entity object
				let entityObject = new Entity('EntityName');
				let entity = new mxCell(entityObject, new mxGeometry(0, 0, 200, 28), 'entity');
				entity.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, entity, ICON_ENTITY, 'Drag this to the diagram to create a new Entity', $scope, dialogHub);

				// Adds sidebar icon for the property object
				let propertyObject = new Property('PropertyName');
				let property = new mxCell(propertyObject, new mxGeometry(0, 0, 0, 26));
				property.setVertex(true);
				property.setConnectable(false);

				addSidebarIcon($scope.graph, sidebar, property, ICON_PROPERTY, 'Drag this to an Entity to create a new Property', $scope, dialogHub);

				// Adds primary key field into entity
				let firstProperty = new mxCell(new Property('PropertyName'), new mxGeometry(0, 0, 0, 26));
				firstProperty.setVertex(true);
				firstProperty.setConnectable(false);
				firstProperty.value.name = 'Id';
				firstProperty.value.dataType = 'INTEGER';
				firstProperty.value.dataLength = 0;
				firstProperty.value.dataPrimaryKey = 'true';
				firstProperty.value.dataAutoIncrement = 'true';
				entity.insert(firstProperty);

				// Adds child properties for new connections between entities
				$scope.graph.addEdge = function (edge, parent, source, target, index) {

					// Finds the primary key child of the target table
					let primaryKey = null;
					let childCount = $scope.graph.model.getChildCount(target);

					for (let i = 0; i < childCount; i++) {
						let child = $scope.graph.model.getChildAt(target, i);

						if (child.value.dataPrimaryKey === 'true') {
							primaryKey = child;
							break;
						}
					}

					if (primaryKey === null) {
						dialogHub.showAlert({
							title: 'Error',
							message: 'Target Entity must have a Primary Key.',
							type: AlertTypes.Error,
							preformatted: false,
						});
						return;
					}

					$scope.graph.model.beginUpdate();
					try {
						let prop1 = $scope.graph.model.cloneCell(property);
						if (target.style && target.style.startsWith('projection')) {
							prop1.value.name = primaryKey.parent.value.projectionReferencedEntity;
						} else {
							prop1.value.name = primaryKey.parent.value.name;
						}
						prop1.value.dataType = primaryKey.value.dataType;
						prop1.value.dataLength = primaryKey.value.dataLength;

						this.addCell(prop1, source);
						source = prop1;
						target = primaryKey;

						return mxGraph.prototype.addEdge.apply(this, arguments); // "supercall"
					} finally {
						$scope.graph.model.endUpdate();
					}
				};

				// Dependent Entity ----------------------------------------------

				// Adds sidebar icon for the dependent entity object
				let dependentObject = new Entity('DependentEntityName');
				let dependent = new mxCell(dependentObject, new mxGeometry(0, 0, 200, 28), 'dependent');
				dependent.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, dependent, ICON_DEPENDENT, 'Drag this to the diagram to create a new Dependent Entity', $scope, dialogHub);

				// Adds primary key field into entity
				firstProperty = property.clone();
				firstProperty.value.name = 'Id';
				firstProperty.value.dataType = 'INTEGER';
				firstProperty.value.dataLength = 0;
				firstProperty.value.dataPrimaryKey = 'true';
				firstProperty.value.dataAutoIncrement = 'true';
				dependent.insert(firstProperty);

				// Report Entity ----------------------------------------------

				// Adds sidebar icon for the report entity object
				let reportObject = new Entity('ReportEntityName');
				let report = new mxCell(reportObject, new mxGeometry(0, 0, 200, 28), 'report');
				report.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, report, ICON_REPORT, 'Drag this to the diagram to create a new Report Entity', $scope, dialogHub);

				// Adds primary key field into entity
				firstProperty = property.clone();
				firstProperty.value.name = 'Id';
				firstProperty.value.dataType = 'INTEGER';
				firstProperty.value.dataLength = 0;
				firstProperty.value.dataPrimaryKey = 'true';
				firstProperty.value.dataAutoIncrement = 'true';
				report.insert(firstProperty);

				// Filter Entity ----------------------------------------------

				// Adds sidebar icon for the filter entity object
				let reportFilterObject = new Entity('ReportFilterEntityName');
				let reportFilter = new mxCell(reportFilterObject, new mxGeometry(0, 0, 200, 28), 'filter');
				reportFilter.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, reportFilter, ICON_FILTER, 'Drag this to the diagram to create a new Report Filter Entity', $scope, dialogHub);

				// Adds primary key field into entity
				firstProperty = property.clone();
				firstProperty.value.name = 'Id';
				firstProperty.value.dataType = 'INTEGER';
				firstProperty.value.dataLength = 0;
				firstProperty.value.dataPrimaryKey = 'true';
				firstProperty.value.dataAutoIncrement = 'true';
				reportFilter.insert(firstProperty);

				// Setting Entity ----------------------------------------------

				// Adds sidebar icon for the setting entity object
				let settingObject = new Entity('SettingEntityName');
				let setting = new mxCell(settingObject, new mxGeometry(0, 0, 200, 28), 'setting');
				setting.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, setting, ICON_SETTING, 'Drag this to the diagram to create a new Setting Entity', $scope, dialogHub);

				// Adds primary key field into entity
				firstProperty = property.clone();
				firstProperty.value.name = 'Id';
				firstProperty.value.dataType = 'INTEGER';
				firstProperty.value.dataLength = 0;
				firstProperty.value.dataPrimaryKey = 'true';
				firstProperty.value.dataAutoIncrement = 'true';
				setting.insert(firstProperty);

				// Copied Entity ----------------------------------------------

				// Adds sidebar icon for the copied entity object
				let copiedObject = new Entity('EntityName');
				let copied = new mxCell(copiedObject, new mxGeometry(0, 0, 200, 28), 'copied');
				copied.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, copied, ICON_COPIED, 'Drag this to the diagram to create a copy to an Entity from external model', $scope, dialogHub);
				$scope.showCopiedEntityDialog = (cellId) => {
					dialogHub.showWindow({
						id: 'edmReference',
						hasHeader: true,
						params: { cellId: cellId, dialogType: 'copiedEntity' },
						maxWidth: '640px',
						closeButton: false
					});
				};

				// Adds sidebar icon for the projection entity object
				let projectionObject = new Entity('EntityName');
				let projection = new mxCell(projectionObject, new mxGeometry(0, 0, 200, 28), 'projection');
				projection.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, projection, ICON_PROJECTION, 'Drag this to the diagram to create a reference to an Entity from external model', $scope, dialogHub);
				$scope.showReferDialog = (cellId) => {
					dialogHub.showWindow({
						id: 'edmReference',
						hasHeader: true,
						params: { cellId: cellId, dialogType: 'refer' },
						maxWidth: '640px',
						closeButton: false
					});
				};

				// Extension Entity ----------------------------------------------

				// Adds sidebar icon for the extension entity object
				let extensionObject = new Entity('EntityName');
				let extension = new mxCell(extensionObject, new mxGeometry(0, 0, 200, 28), 'extension');
				extension.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, extension, ICON_EXTENSION, 'Drag this to the diagram to create a new Extension Entity', $scope, dialogHub);

				// Adds primary key field into extension entity
				keyProperty = property.clone();
				keyProperty.value.name = 'Id';
				keyProperty.value.dataType = 'INTEGER';
				keyProperty.value.dataLength = 0;
				keyProperty.value.dataPrimaryKey = 'true';
				keyProperty.value.dataAutoIncrement = 'true';
				keyProperty.style = 'extensionproperty';
				extension.insert(keyProperty);

				// Creates a new DIV that is used as a toolbar and adds
				// toolbar buttons.
				let spacer = document.createElement('div');
				spacer.style.display = 'inline';
				spacer.style.padding = '8px';

				// Defines a new save action
				editor.addAction('save', function (_editor, _cell) {
					saveContents(createModel($scope.graph));
				});

				// Defines a new properties action
				editor.addAction('properties', function (_editor, cell) {
					if (!cell) {
						cell = $scope.graph.getSelectionCell();
						if (!cell) {
							dialogHub.showAlert({
								title: 'Error',
								message: 'Select an Entity, a Property or a Connector.',
								type: AlertTypes.Error,
								preformatted: false,
							});
							return;
						}
					}

					if ($scope.graph.isHtmlLabel(cell)) {
						if (cell) {
							// assume Entity's property
							//showProperties($scope.graph, cell);
							dialogHub.showWindow({
								id: 'edmDetails',
								hasHeader: false,
								params: {
									dialogType: 'property',
									cellId: cell.id,
									name: cell.value.name,
									description: cell.value.description,
									tooltip: cell.value.tooltip,
									isRequiredProperty: cell.value.isRequiredProperty,
									isCalculatedProperty: cell.value.isCalculatedProperty,
									calculatedPropertyExpressionCreate: cell.value.calculatedPropertyExpressionCreate,
									calculatedPropertyExpressionUpdate: cell.value.calculatedPropertyExpressionUpdate,
									dataName: cell.value.dataName,
									dataType: cell.value.dataType,
									dataOrderBy: cell.value.dataOrderBy,
									dataLength: cell.value.dataLength,
									dataPrimaryKey: cell.value.dataPrimaryKey,
									dataAutoIncrement: cell.value.dataAutoIncrement,
									dataNotNull: cell.value.dataNotNull,
									dataUnique: cell.value.dataUnique,
									dataPrecision: cell.value.dataPrecision,
									dataScale: cell.value.dataScale,
									dataDefaultValue: cell.value.dataDefaultValue,
									widgetType: cell.value.widgetType,
									widgetSize: cell.value.widgetSize,
									widgetLength: cell.value.widgetLength,
									widgetLabel: cell.value.widgetLabel,
									widgetShortLabel: cell.value.widgetShortLabel,
									widgetPattern: cell.value.widgetPattern,
									widgetFormat: cell.value.widgetFormat,
									widgetService: cell.value.widgetService,
									widgetSection: cell.value.widgetSection,
									widgetIsMajor: cell.value.widgetIsMajor,
									widgetDropDownKey: cell.value.widgetDropDownKey,
									widgetDropDownValue: cell.value.widgetDropDownValue,
									widgetDropDownMultiSelect: cell.value.widgetDropDownMultiSelect,
									widgetDependsOnProperty: cell.value.widgetDependsOnProperty,
									widgetDependsOnEntity: cell.value.widgetDependsOnEntity,
									widgetDependsOnValueFrom: cell.value.widgetDependsOnValueFrom,
									widgetDependsOnFilterBy: cell.value.widgetDependsOnFilterBy,
									feedPropertyName: cell.value.feedPropertyName,
									roleRead: cell.value.roleRead,
									roleWrite: cell.value.roleWrite,
								},
								maxWidth: '1024px',
								closeButton: false
							});
						} else {
							dialogHub.showAlert({
								title: 'Error',
								message: 'Select a Property.',
								type: AlertTypes.Error,
								preformatted: false,
							});
						}
					} else {
						// assume Entity or Connector
						if (cell.value && Entity.prototype.isPrototypeOf(cell.value)) {
							// assume Entity
							//showEntityProperties($scope.graph, cell);
							dialogHub.showWindow({
								id: 'edmDetails',
								hasHeader: false,
								params: {
									projectName: modelFile.split("/")[2],
									dialogType: 'entity',
									cellId: cell.id,
									name: cell.value.name,
									entityType: cell.value.entityType,
									dataName: cell.value.dataName,
									dataCount: cell.value.dataCount,
									dataQuery: cell.value.dataQuery,
									title: cell.value.title,
									caption: cell.value.caption,
									description: cell.value.description,
									tooltip: cell.value.tooltip,
									icon: cell.value.icon,
									menuKey: cell.value.menuKey,
									menuLabel: cell.value.menuLabel,
									menuIndex: cell.value.menuIndex,
									layoutType: cell.value.layoutType,
									perspectiveName: cell.value.perspectiveName,
									perspectiveLabel: cell.value.perspectiveLabel,
									navigationPath: cell.value.navigationPath,
									feedUrl: cell.value.feedUrl,
									feedUsername: cell.value.feedUsername,
									feedPassword: cell.value.feedPassword,
									feedSchedule: cell.value.feedSchedule,
									feedPath: cell.value.feedPath,
									generateDefaultRoles: cell.value.generateDefaultRoles,
									roleRead: cell.value.roleRead,
									roleWrite: cell.value.roleWrite,
									perspectives: $scope.graph.model.perspectives,
									navigations: $scope.graph.model.navigations,
									importsCode: cell.value.importsCode,
									generateReport: cell.value.generateReport,
									multilingual: cell.value.multilingual
								},
								maxWidth: '1024px',
								closeButton: false
							});
						} else {
							// assume Connector
							//showConnectorProperties($scope.graph, cell);
							dialogHub.showFormDialog({
								title: 'Relationship properties',
								form: {
									[`edm-${cell.id}`]: {
										label: 'Name',
										controlType: 'input',
										placeholder: 'Enter name',
										type: 'text',
										value: cell.source.value.relationshipName,
										focus: true,
										required: true,
									},
									'edmRelationshipType': {
										label: 'Type',
										controlType: 'dropdown',
										options: $scope.relationshipTypes,
										value: cell.source.value.relationshipType,
										required: true,
									},
									'edmRelationshipCardinalities': {
										label: 'Cardinalities',
										controlType: 'dropdown',
										options: $scope.relationshipCardinalities,
										value: cell.source.value.relationshipCardinalities,
										required: true,
									},
								},
								submitLabel: 'Update',
								cancelLabel: 'Cancel'
							}).then((form) => {
								if (form) {
									// let refCell = $scope.graph.model.getCell(cell.id);
									cell.source.value.relationshipName = form[`edm-${cell.id}`];
									cell.source.value.relationshipType = form['edmRelationshipType'];
									cell.source.value.relationshipCardinality = form['edmRelationshipCardinalities'];
									$scope.graph.model.setValue(cell.source, cell.source.value);

									let connector = new Connector();
									connector.name = cell.source.value.relationshipName;
									$scope.graph.model.setValue(cell, connector);
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
						}
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
					//			document.execCommand("copy");
				});

				// Defines a new save action
				editor.addAction('paste', function (editor, cell) {
					mxClipboard.paste($scope.graph);
				});

				$scope.save = function () {
					editor.execute('save');
				};
				$scope.properties = function () {
					editor.execute('properties');
				};
				$scope.navigation = () => {
					dialogHub.showWindow({
						id: 'edmNavDetails',
						hasHeader: false,
						params: {
							perspectives: $scope.graph.model.perspectives,
							navigations: $scope.graph.model.navigations,
						},
						maxWidth: '1600px',
						closeButton: false
					});
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
				let outln = new mxOutline($scope.graph, outline);

				// Fades-out the splash screen after the UI has been loaded.
				let splash = document.getElementById('splash');
				if (splash != null) {
					try {
						mxEvent.release(splash);
						mxEffects.fadeOut(splash, 100, true);
					} catch (e) {

						// mxUtils is not available (library not loaded)
						splash.parentNode.removeChild(splash);
					}
				}
			}

			let doc = mxUtils.parseXml(contents);
			let codec = new mxCodec(doc.mxGraphModel);
			codec.decode(doc.documentElement.getElementsByTagName('mxGraphModel')[0], $scope.graph.getModel());

			deserializeFilter($scope.graph);
			loadPerspectives(doc, $scope.graph);
			loadNavigations(doc, $scope.graph);
			$scope.graph.model.addListener(mxEvent.START_EDIT, function (_sender, _evt) {
				layoutHub.setEditorDirty({
					path: $scope.dataParameters.filePath,
					dirty: true,
				});
			});
			$scope.graph.enterStopsCellEditing = true;
		}

		function deserializeFilter(graph) {
			let parent = graph.getDefaultParent();
			let childCount = graph.model.getChildCount(parent);

			// Base64 deserialization of the encoded properties
			for (let i = 0; i < childCount; i++) {
				let child = graph.model.getChildAt(parent, i);
				if (!graph.model.isEdge(child)) {
					if (child.value.feedUrl && child.value.feedUrl !== "") {
						child.value.feedUrl = atob(child.value.feedUrl);
					}
					if (child.value.feedUsername && child.value.feedUsername !== "") {
						child.value.feedUsername = atob(child.value.feedUsername);
					}
					if (child.value.feedPassword && child.value.feedPassword !== "") {
						child.value.feedPassword = atob(child.value.feedPassword);
					}
					if (child.value.feedSchedule && child.value.feedSchedule !== "") {
						child.value.feedSchedule = atob(child.value.feedSchedule);
					}
					if (child.value.importsCode && child.value.importsCode !== "") {
						child.value.importsCode = atob(child.value.importsCode);
					}
				}
			}
		}

		function loadPerspectives(doc, graph) {
			if (!graph.getModel().perspectives) {
				graph.getModel().perspectives = [];
			}
			for (let i = 0; i < doc.children.length; i++) {
				let element = doc.children[i];
				if (element.localName === "model") {
					for (let j = 0; j < element.children.length; j++) {
						let perspectives = element.children[j];
						if (perspectives.localName === "perspectives") {
							for (let k = 0; k < perspectives.children.length; k++) {
								let item = perspectives.children[k];
								let copy = {};
								for (let m = 0; m < item.children.length; m++) {
									let attribute = item.children[m];
									if (attribute.localName === "name") {
										copy.id = attribute.textContent;
									} else if (attribute.localName === "label") {
										copy.label = attribute.textContent;
									} else if (attribute.localName === "header") {
										copy.header = attribute.textContent;
									} else if (attribute.localName === "icon") {
										copy.icon = attribute.textContent;
									} else if (attribute.localName === "navId") {
										copy.navId = attribute.textContent;
									} else if (attribute.localName === "order" && attribute.textContent !== '') {
										copy.order = parseInt(attribute.textContent);
									} else if (attribute.localName === "role") {
										copy.role = attribute.textContent;
									}
								}
								graph.getModel().perspectives.push(copy);
							}
							break;
						}
					}
					break;
				}
			}
		}

		function loadNavigations(doc, graph) {
			if (!graph.getModel().navigations) {
				graph.getModel().navigations = [];
			}
			for (let i = 0; i < doc.children.length; i++) {
				let element = doc.children[i];
				if (element.localName === "model") {
					for (let j = 0; j < element.children.length; j++) {
						let navigation = element.children[j];
						if (navigation.localName === "navigations") {
							for (let k = 0; k < navigation.children.length; k++) {
								let item = navigation.children[k];
								let copy = {};
								for (let m = 0; m < item.children.length; m++) {
									let attribute = item.children[m];
									if (attribute.localName === "id") {
										copy.id = attribute.textContent;
									} else if (attribute.localName === "label") {
										copy.label = attribute.textContent;
									} else if (attribute.localName === "header") {
										copy.header = attribute.textContent;
									} else if (attribute.localName === "expanded") {
										copy.expanded = attribute.textContent === 'true';
									} else if (attribute.localName === "icon") {
										copy.icon = attribute.textContent;
									} else if (attribute.localName === "order" && attribute.textContent !== '') {
										copy.order = parseInt(attribute.textContent);
									} else if (attribute.localName === "role") {
										copy.role = attribute.textContent;
									}
								}
								graph.getModel().navigations.push(copy);
							}
							break;
						}
					}
					break;
				}
			}
		}

		$scope.dataParameters = ViewParameters.get();
		if (!$scope.dataParameters.hasOwnProperty('filePath')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'filePath' data parameter is missing.";
		} else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'contentType' data parameter is missing.";
		} else {
			modelFile = $scope.dataParameters.filePath.substring(0, $scope.dataParameters.filePath.lastIndexOf('.')) + '.model';
			genFile = $scope.dataParameters.filePath.substring(0, $scope.dataParameters.filePath.lastIndexOf('.')) + '.gen';
			fileWorkspace = $scope.dataParameters.filePath.substring(1, $scope.dataParameters.filePath.indexOf('/', 1));
			loadFileContents();
		}
	});