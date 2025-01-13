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
angular.module('ui.entity-data.modeler', ["ideUI", "ideView", "ideWorkspace", "ideGenerate", "ideTemplates"])
	.controller('ModelerCtrl', function ($scope, messageHub, $window, workspaceApi, generateApi, templatesApi, ViewParameters, uuid) {
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

		angular.element($window).bind("focus", function () {
			messageHub.setFocusedEditor($scope.dataParameters.file);
			messageHub.setStatusCaret('');
		});

		$scope.load = function () {
			if (!$scope.state.error) {
				workspaceApi.loadContent('', $scope.dataParameters.file).then(function (response) {
					if (response.status === 200) {
						contents = response.data;
						initializeModelJson();
						main(document.getElementById('graphContainer'),
							document.getElementById('outlineContainer'),
							document.getElementById('toolbarContainer'),
							document.getElementById('sidebarContainer'));
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

		$scope.checkGenFile = function () {
			workspaceApi.resourceExists(genFile).then(function (response) {
				if (response.status === 200) $scope.canRegenerate = true;
				else $scope.canRegenerate = false;
			});
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

		function initializeModelJson() {
			workspaceApi.resourceExists(modelFile).then(function (exists) {
				if (exists.status === 200) {
					$scope.checkGenFile();
				} else {
					workspaceApi.createNode('', modelFile, false, '').then(function (response) {
						if (response.status === 201) {
							const fileDescriptor = {
								name: modelFile.substring(modelFile.lastIndexOf('/') + 1),
								path: modelFile.substring(modelFile.indexOf('/', 1)),
								contentType: $scope.dataParameters.contentType,
								workspace: modelFile.substring(1, modelFile.indexOf('/', 1)),
							};
							messageHub.announceFileSaved(fileDescriptor);
							messageHub.postMessage('projects.tree.refresh', { partial: true, project: fileDescriptor.path.substring(1, fileDescriptor.path.indexOf('/', 1)), workspace: fileDescriptor.workspace }, true);
							messageHub.setStatusMessage(`File '${modelFile}' created`);
							$scope.checkGenFile();
						} else {
							messageHub.setStatusError(`Error saving '${modelFile}'`);
							messageHub.showAlertError('Error while saving the file', 'Please look at the console for more information');
							$scope.state.isBusy = false;
						}
					});
				}
			});
		}

		$scope.chooseTemplate = function (project, filePath, params) {
			const templateItems = [];
			templatesApi.listTemplates().then(function (response) {
				if (response.status === 200) {
					for (let i = 0; i < response.data.length; i++) {
						if (response.data[i].hasOwnProperty('extension') && response.data[i].extension === 'model') {
							templateItems.push({
								label: response.data[i].name,
								value: response.data[i].id,
							});
						}
					}
					messageHub.hideLoadingDialog('edmRegenerateModel');
					messageHub.showFormDialog(
						'edmRegenerateChooseTemplate',
						'Choose template',
						[{
							id: 'pgfd1',
							type: 'dropdown',
							label: 'Choose template',
							required: true,
							value: '',
							items: templateItems,
						}],
						[{
							id: 'b1',
							type: 'emphasized',
							label: 'OK',
							whenValid: true,
						}, {
							id: 'b2',
							type: 'transparent',
							label: 'Cancel',
						}],
						'edm.regenerate.template',
						'Setting template...',
					);
					messageHub.onDidReceiveMessage(
						'edm.regenerate.template',
						function (msg) {
							if (msg.data.buttonId === "b1") {
								messageHub.hideFormDialog('edmRegenerateChooseTemplate');
								messageHub.showLoadingDialog('edmRegenerateModel', 'Regenerating', 'Regenerating from model');
								$scope.generateFromModel(project, filePath, msg.data.formData[0].value, params);
							} else messageHub.hideFormDialog('projectRegenerateChooseTemplate');
						},
						true
					);
				} else {
					messageHub.hideLoadingDialog('edmRegenerateModel');
					messageHub.setStatusError('Unable to load template list');
				}
			});
		};

		$scope.generateFromModel = function (project, filePath, templateId, params) {
			generateApi.generateFromModel(
				fileWorkspace,
				project,
				filePath,
				templateId,
				params
			).then(function (response) {
				messageHub.hideLoadingDialog('edmRegenerateModel');
				if (response.status !== 201) {
					messageHub.showAlertError(
						'Failed to generate from model',
						`An unexpected error has occurred while trying generate from model '${filePath}'`
					);
					messageHub.setStatusError(`Unable to generate from model '${filePath}'`);
				} else {
					messageHub.setStatusMessage(`Generated from model '${filePath}'`);
				}
				messageHub.postMessage('projects.tree.refresh', { partial: true, project: project, workspace: fileWorkspace }, true);
			});
		};

		$scope.regenerate = function () {
			messageHub.showLoadingDialog('edmRegenerateModel', 'Regenerating', 'Loading data');
			$scope.save();
			workspaceApi.loadContent('', genFile).then(function (response) {
				if (response.status === 200) {
					let { models, perspectives, templateId, filePath, workspaceName, projectName, ...params } = response.data;
					if (!response.data.templateId) {
						$scope.chooseTemplate(response.data.projectName, response.data.filePath, params);
					} else {
						messageHub.updateLoadingDialog('edmRegenerateModel', 'Regenerating from model');
						$scope.generateFromModel(response.data.projectName, response.data.filePath, response.data.templateId, params);
					}
				} else {
					messageHub.hideLoadingDialog('edmRegenerateModel');
					messageHub.showAlertError('Unable to load model file', 'There was an error while loading the model file. See the log for more information.');
					console.error(response);
				}
			});
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
				if (!$scope.state.error) {
					saveContents(createModel($scope.graph));
				}
			},
			true,
		);

		messageHub.onDidReceiveMessage(
			"editor.file.save",
			function (msg) {
				if (!$scope.state.error) {
					let file = msg.data && typeof msg.data === 'object' && msg.data.file;
					if (file && file === $scope.dataParameters.file) {
						saveContents(createModel($scope.graph));
					}
				}
			},
			true,
		);

		messageHub.onDidReceiveMessage(
			"edm.editor.entity",
			function (msg) {
				let cell = $scope.graph.model.getCell(msg.data.cellId);
				cell.value.name = msg.data.name;
				cell.value.entityType = msg.data.entityType;
				cell.value.dataName = msg.data.dataName;
				cell.value.dataCount = msg.data.dataCount;
				cell.value.dataQuery = msg.data.dataQuery;
				cell.value.title = msg.data.title;
				cell.value.caption = msg.data.caption;
				cell.value.description = msg.data.description;
				cell.value.tooltip = msg.data.tooltip;
				cell.value.icon = msg.data.icon;
				cell.value.menuKey = msg.data.menuKey;
				cell.value.menuLabel = msg.data.menuLabel;
				cell.value.menuIndex = msg.data.menuIndex;
				cell.value.layoutType = msg.data.layoutType;
				cell.value.perspectiveName = msg.data.perspectiveName;
				cell.value.perspectiveLabel = msg.data.perspectiveLabel;
				cell.value.navigationPath = msg.data.navigationPath;
				cell.value.feedUrl = msg.data.feedUrl;
				cell.value.feedUsername = msg.data.feedUsername;
				cell.value.feedPassword = msg.data.feedPassword;
				cell.value.feedSchedule = msg.data.feedSchedule;
				cell.value.feedPath = msg.data.feedPath;
				cell.value.roleRead = msg.data.roleRead;
				cell.value.roleWrite = msg.data.roleWrite;
				cell.value.importsCode = msg.data.importsCode;
				cell.value.generateReport = msg.data.generateReport;

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
				messageHub.closeDialogWindow("edmDetails");
			},
			true,
		);

		messageHub.onDidReceiveMessage(
			"edm.editor.property",
			function (msg) {
				let cell = $scope.graph.model.getCell(msg.data.cellId);
				cell.value.name = msg.data.name;
				cell.value.description = msg.data.description;
				cell.value.tooltip = msg.data.tooltip;
				cell.value.isRequiredProperty = msg.data.isRequiredProperty;
				cell.value.isCalculatedProperty = msg.data.isCalculatedProperty;
				cell.value.calculatedPropertyExpressionCreate = msg.data.calculatedPropertyExpressionCreate;
				cell.value.calculatedPropertyExpressionUpdate = msg.data.calculatedPropertyExpressionUpdate;
				cell.value.dataName = msg.data.dataName;
				cell.value.dataType = msg.data.dataType;
				cell.value.dataOrderBy = msg.data.dataOrderBy;
				cell.value.dataLength = msg.data.dataLength;
				cell.value.dataPrimaryKey = msg.data.dataPrimaryKey;
				cell.value.dataAutoIncrement = msg.data.dataAutoIncrement;
				cell.value.dataNotNull = msg.data.dataNotNull;
				cell.value.dataUnique = msg.data.dataUnique;
				cell.value.dataPrecision = msg.data.dataPrecision;
				cell.value.dataScale = msg.data.dataScale;
				cell.value.dataDefaultValue = msg.data.dataDefaultValue;
				cell.value.widgetType = msg.data.widgetType;
				cell.value.widgetSize = msg.data.widgetSize;
				cell.value.widgetLength = msg.data.widgetLength;
				cell.value.widgetLabel = msg.data.widgetLabel;
				cell.value.widgetShortLabel = msg.data.widgetShortLabel;
				cell.value.widgetPattern = msg.data.widgetPattern;
				cell.value.widgetFormat = msg.data.widgetFormat;
				cell.value.widgetService = msg.data.widgetService;
				cell.value.widgetSection = msg.data.widgetSection;
				cell.value.widgetIsMajor = msg.data.widgetIsMajor;
				cell.value.widgetDropDownKey = msg.data.widgetDropDownKey;
				cell.value.widgetDropDownValue = msg.data.widgetDropDownValue;
				cell.value.widgetDependsOnProperty = msg.data.widgetDependsOnProperty;
				cell.value.widgetDependsOnEntity = msg.data.widgetDependsOnEntity;
				cell.value.widgetDependsOnValueFrom = msg.data.widgetDependsOnValueFrom;
				cell.value.widgetDependsOnFilterBy = msg.data.widgetDependsOnFilterBy;
				cell.value.feedPropertyName = msg.data.feedPropertyName;
				cell.value.roleRead = msg.data.roleRead;
				cell.value.roleWrite = msg.data.roleWrite;
				// Maybe we should do this with "cell.value.clone()'
				$scope.graph.model.setValue(cell, cell.value);
				messageHub.closeDialogWindow("edmDetails");
			},
			true,
		);

		messageHub.onDidReceiveMessage(
			"edmEditor.connector.properties",
			function (msg) {
				if (msg.data.buttonId === "b1") {
					let cell = $scope.graph.model.getCell(msg.data.formData[0].id.substring(4));
					cell.source.value.relationshipName = msg.data.formData[0].value;
					cell.source.value.relationshipType = msg.data.formData[1].value;
					cell.source.value.relationshipCardinality = msg.data.formData[2].value;
					$scope.graph.model.setValue(cell.source, cell.source.value);

					let connector = new Connector();
					connector.name = cell.source.value.relationshipName;
					$scope.graph.model.setValue(cell, connector);
				}
				messageHub.hideFormDialog("edmConnectorProperties");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"edmEditor.navigation.details",
			function (msg) {
				$scope.graph.model.perspectives = msg.data.perspectives;
				$scope.graph.model.navigations = msg.data.navigations;
				messageHub.setEditorDirty($scope.dataParameters.file, true);
				messageHub.closeDialogWindow("edmNavDetails");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"edm.editor.reference",
			function (msg) {
				let model = $scope.graph.getModel();
				model.beginUpdate();
				try {
					let cell = $scope.graph.model.getCell(msg.data.cellId);
					cell.value.name = msg.data.entity;
					cell.value.entityType = "PROJECTION";
					cell.value.projectionReferencedModel = msg.data.model;
					cell.value.projectionReferencedEntity = msg.data.entity;
					cell.value.perspectiveName = msg.data.perspectiveName;
					cell.value.perspectiveLabel = msg.data.perspectiveLabel;
					cell.value.perspectiveIcon = msg.data.perspectiveIcon;
					cell.value.perspectiveOrder = msg.data.perspectiveOrder;
					cell.value.perspectiveRole = msg.data.perspectiveRole;
					$scope.graph.model.setValue(cell, cell.value);

					for (let i = 0; i < msg.data.entityProperties.length; i++) {
						let propertyObject = new Property('propertyName');
						let property = new mxCell(propertyObject, new mxGeometry(0, 0, 0, 26));
						property.setId(uuid.generate());
						property.setVertex(true);
						property.setConnectable(false);
						for (let attributeName in msg.data.entityProperties[i]) {
							property.value[attributeName] = msg.data.entityProperties[i][attributeName];
						}
						property.style = 'projectionproperty';
						cell.insert(property);
					}
					model.setCollapsed(cell, true);
				} finally {
					model.endUpdate();
				}
				$scope.graph.refresh();
				messageHub.setEditorDirty($scope.dataParameters.file, true);
				messageHub.closeDialogWindow("edmReference");
			},
			true
		);

		messageHub.onDidReceiveMessage(
			"edm.editor.copiedEntity",
			function (msg) {
				let model = $scope.graph.getModel();
				model.beginUpdate();
				try {
					let cell = $scope.graph.model.getCell(msg.data.cellId);
					cell.value.name = msg.data.entity;
					cell.value.entityType = "COPIED";
					cell.value.projectionReferencedModel = msg.data.model;
					cell.value.projectionReferencedEntity = msg.data.entity;
					cell.value.perspectiveName = msg.data.perspectiveName;
					cell.value.perspectiveLabel = msg.data.perspectiveLabel;
					cell.value.perspectiveIcon = msg.data.perspectiveIcon;
					cell.value.perspectiveOrder = msg.data.perspectiveOrder;
					cell.value.perspectiveRole = msg.data.perspectiveRole;
					$scope.graph.model.setValue(cell, cell.value);

					for (let i = 0; i < msg.data.entityProperties.length; i++) {
						let propertyObject = new Property('propertyName');
						let property = new mxCell(propertyObject, new mxGeometry(0, 0, 0, 26));
						property.setId(uuid.generate());
						property.setVertex(true);
						property.setConnectable(false);
						for (let attributeName in msg.data.entityProperties[i]) {
							property.value[attributeName] = msg.data.entityProperties[i][attributeName];
						}
						cell.insert(property);
					}
					model.setCollapsed(cell, true);
				} finally {
					model.endUpdate();
				}

				model.setCollapsed($scope.$cell, true);
				$scope.graph.refresh();
				messageHub.setEditorDirty($scope.dataParameters.file, true);
				messageHub.closeDialogWindow("edmReference");
			},
			true
		);

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

						let suffix = mxUtils.htmlEntities(cell.value.dataType, false) + (cell.value.dataLength ? '(' + cell.value.dataLength + ')' : '');
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
				addSidebarIcon($scope.graph, sidebar, entity, ICON_ENTITY, 'Drag this to the diagram to create a new Entity', $scope, messageHub);

				// Adds sidebar icon for the property object
				let propertyObject = new Property('PropertyName');
				let property = new mxCell(propertyObject, new mxGeometry(0, 0, 0, 26));
				property.setVertex(true);
				property.setConnectable(false);

				addSidebarIcon($scope.graph, sidebar, property, ICON_PROPERTY, 'Drag this to an Entity to create a new Property', $scope, messageHub);

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
						messageHub.showAlertError("Error", "Target Entity must have a Primary Key");
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
				addSidebarIcon($scope.graph, sidebar, dependent, ICON_DEPENDENT, 'Drag this to the diagram to create a new Dependent Entity', $scope, messageHub);

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
				addSidebarIcon($scope.graph, sidebar, report, ICON_REPORT, 'Drag this to the diagram to create a new Report Entity', $scope, messageHub);

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
				addSidebarIcon($scope.graph, sidebar, reportFilter, ICON_FILTER, 'Drag this to the diagram to create a new Report Filter Entity', $scope, messageHub);

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
				addSidebarIcon($scope.graph, sidebar, setting, ICON_SETTING, 'Drag this to the diagram to create a new Setting Entity', $scope, messageHub);

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
				addSidebarIcon($scope.graph, sidebar, copied, ICON_COPIED, 'Drag this to the diagram to create a copy to an Entity from external model', $scope, messageHub);
				$scope.showCopiedEntityDialog = function (cellId) {
					messageHub.showDialogWindow(
						"edmReference",
						{ cellId: cellId, dialogType: 'copiedEntity' },
						null,
						false,
					);
				};

				// Adds sidebar icon for the projection entity object
				let projectionObject = new Entity('EntityName');
				let projection = new mxCell(projectionObject, new mxGeometry(0, 0, 200, 28), 'projection');
				projection.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, projection, ICON_PROJECTION, 'Drag this to the diagram to create a reference to an Entity from external model', $scope, messageHub);
				$scope.showReferDialog = function (cellId) {
					messageHub.showDialogWindow(
						"edmReference",
						{ cellId: cellId, dialogType: 'refer' },
						null,
						false,
					);
				};

				// Extension Entity ----------------------------------------------

				// Adds sidebar icon for the extension entity object
				let extensionObject = new Entity('EntityName');
				let extension = new mxCell(extensionObject, new mxGeometry(0, 0, 200, 28), 'extension');
				extension.setVertex(true);
				addSidebarIcon($scope.graph, sidebar, extension, ICON_EXTENSION, 'Drag this to the diagram to create a new Extension Entity', $scope, messageHub);

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
							messageHub.showAlertError(
								"Error",
								"Select an Entity, a Property or a Connector."
							);
							return;
						}
					}

					if ($scope.graph.isHtmlLabel(cell)) {
						if (cell) {
							// assume Entity's property
							//showProperties($scope.graph, cell);
							messageHub.showDialogWindow(
								"edmDetails",
								{
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
									widgetDependsOnProperty: cell.value.widgetDependsOnProperty,
									widgetDependsOnEntity: cell.value.widgetDependsOnEntity,
									widgetDependsOnValueFrom: cell.value.widgetDependsOnValueFrom,
									widgetDependsOnFilterBy: cell.value.widgetDependsOnFilterBy,
									feedPropertyName: cell.value.feedPropertyName,
									roleRead: cell.value.roleRead,
									roleWrite: cell.value.roleWrite,
								},
								null,
								false,
							);
						} else {
							messageHub.showAlertError(
								"Error",
								"Select a Property."
							);
						}
					} else {
						// assume Entity or Connector
						if (cell.value && Entity.prototype.isPrototypeOf(cell.value)) {
							// assume Entity
							//showEntityProperties($scope.graph, cell);
							messageHub.showDialogWindow(
								"edmDetails",
								{
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
									roleRead: cell.value.roleRead,
									roleWrite: cell.value.roleWrite,
									perspectives: $scope.graph.model.perspectives,
									navigations: $scope.graph.model.navigations,
									importsCode: cell.value.importsCode,
									generateReport: cell.value.generateReport
								},
								null,
								false,
							);
						} else {
							// assume Connector
							//showConnectorProperties($scope.graph, cell);
							messageHub.showFormDialog(
								"edmConnectorProperties",
								"Relationship properties",
								[{
									id: `edm-${cell.id}`,
									type: "input",
									required: true,
									label: "Name",
									value: cell.source.value.relationshipName,
								}, {
									id: "edmRelationshipType",
									type: "dropdown",
									label: "Type",
									required: true,
									value: cell.source.value.relationshipType,
									items: $scope.relationshipTypes,
								}, {
									id: "edmRelationshipCardinalities",
									type: "dropdown",
									label: "Cardinalities",
									required: true,
									value: cell.source.value.relationshipCardinalities,
									items: $scope.relationshipCardinalities,
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
								"edmEditor.connector.properties",
								"Updating..."
							);
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
				$scope.navigation = function () {
					messageHub.showDialogWindow(
						"edmNavDetails",
						{
							perspectives: $scope.graph.model.perspectives,
							navigations: $scope.graph.model.navigations,
						},
						null,
						false,
					);
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
				messageHub.setEditorDirty($scope.dataParameters.file, true);
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
									} else if (attribute.localName === "icon") {
										copy.icon = attribute.textContent;
									} else if (attribute.localName === "order") {
										copy.order = attribute.textContent;
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
									if (attribute.localName === "path") {
										copy.path = attribute.textContent;
									} else if (attribute.localName === "label") {
										copy.label = attribute.textContent;
									} else if (attribute.localName === "icon") {
										copy.icon = attribute.textContent;
									} else if (attribute.localName === "url") {
										copy.url = attribute.textContent;
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
		if (!$scope.dataParameters.hasOwnProperty('file')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'file' data parameter is missing.";
		} else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
			$scope.state.error = true;
			$scope.errorMessage = "The 'contentType' data parameter is missing.";
		} else {
			modelFile = $scope.dataParameters.file.substring(0, $scope.dataParameters.file.lastIndexOf('.')) + '.model';
			genFile = $scope.dataParameters.file.substring(0, $scope.dataParameters.file.lastIndexOf('.')) + '.gen';
			fileWorkspace = $scope.dataParameters.workspaceName || workspaceApi.getCurrentWorkspace();
			$scope.load();
		}
	});