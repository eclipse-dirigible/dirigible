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
const registryView = angular.module('registry', ['blimpKit', 'platformView', 'platformShortcuts', 'RepositoryService', 'RegistryService']);
registryView.constant('StatusBar', new StatusBarHub());
registryView.constant('Layout', new LayoutHub());
registryView.constant('Dialogs', new DialogHub());
registryView.constant('Notifications', new NotificationHub());
registryView.constant('Repository', new RepositoryHub());
registryView.constant('ContextMenu', new ContextMenuHub());
registryView.controller('RegistryController', ($scope, $document, clientOS, StatusBar, Layout, Dialogs, Notifications, ViewParameters, ContextMenu, Repository, RepositoryService, RegistryService, ButtonStates) => {
	$scope.state = {
		isBusy: true,
		error: false,
		busyText: 'Loading...',
	};
	const inMacOS = clientOS.isMac();
	$scope.searchVisible = false;
	$scope.searchField = { text: '' };
	let newNodeData = {
		parent: '',
		path: '',
	};
	const imageFileExts = ['ico', 'bmp', 'png', 'jpg', 'jpeg', 'gif', 'svg'];
	const modelFileExts = ['extension', 'extensionpoint', 'edm', 'model', 'dsm', 'schema', 'dmm', 'mapping', 'bpmn', 'job', 'listener', 'websocket', 'roles', 'constraints', 'table', 'view'];

	const treeData = [];
	$scope.basePath = '/';

	const jstreeWidget = angular.element('#dgRegistry');
	const jstreeConfig = {
		core: {
			check_callback: true,
			themes: {
				name: 'fiori',
				variant: 'compact',
			},
			data: (_node, cb) => {
				cb(treeData);
			},
			keyboard: { // We have to have this in order to disable the default behavior.
				'enter': () => { },
				'f2': () => { },
			}
		},
		search: {
			case_sensitive: false,
		},
		plugins: ['wholerow', 'search', 'state', 'types', 'sort'],
		dnd: {
			large_drop_target: true,
			large_drag_target: true,
		},
		sort: function (firstNodeId, secondNodeId) {
			const firstNode = this.get_node(firstNodeId);
			const secondNode = this.get_node(secondNodeId);
			if (firstNode.type === 'spinner') return -1;
			else if (secondNode.type === 'spinner') return 1;
			else if (firstNode.type === secondNode.type) {
				const res = firstNode.text.localeCompare(secondNode.text, 'en-GB', { numeric: true, sensitivity: 'base' });
				if (res < 0) return -1;
				else if (res > 0) return 1;
				return 0;
			} else if (firstNode.type === 'folder') return -1;
			else if (secondNode.type === 'folder') return 1;
			else {
				const res = firstNode.text.localeCompare(secondNode.text, 'en-GB', { numeric: true, sensitivity: 'base' });
				if (res < 0) return -1;
				else if (res > 0) return 1;
				return 0;
			}
		},
		state: { key: `${getBrandingInfo().prefix}.view-registry.state` },
		types: {
			'default': {
				icon: 'sap-icon--question-mark',
				valid_children: [],
			},
			file: {
				icon: 'jstree-file',
				valid_children: [],
			},
			folder: {
				icon: 'jstree-folder',
				valid_children: ['folder', 'file', 'spinner'],
			},
			spinner: {
				icon: 'jstree-spinner',
				valid_children: [],
			},
		},
	};

	$scope.keyboardShortcuts = (keySet, event) => {
		event.preventDefault();
		switch (keySet) {
			case 'enter':
				const focused = jstreeWidget.jstree(true).get_node($document[0].activeElement);
				if (focused) {
					if (!focused.state.selected) {
						jstreeWidget.jstree(true).deselect_all();
						jstreeWidget.jstree(true).select_node(focused);
					}
					if (focused.type === 'folder') {
						if (focused.state.opened) jstreeWidget.jstree(true).close_node(focused);
						else jstreeWidget.jstree(true).open_node(focused);
					} else openFile(focused, 'monaco');
				}
				break;
			case 'shift+enter':
				const toSelect = jstreeWidget.jstree(true).get_node(document.activeElement);
				if (toSelect && !toSelect.state.selected) jstreeWidget.jstree(true).select_node(toSelect);
				else jstreeWidget.jstree(true).deselect_node(toSelect);
				break;
			case 'delete':
			case 'meta+backspace':
				const nodes = jstreeWidget.jstree(true).get_top_selected(true);
				if (nodes.length === 1) openDeleteDialog(nodes[0]);
				break;
			case 'ctrl+f':
				$scope.$evalAsync(() => $scope.toggleSearch());
				break;
			default:
				break;
		}
	};

	jstreeWidget.on('dblclick.jstree', (event) => {
		const node = jstreeWidget.jstree(true).get_node(event.target);
		if (node.type === 'file') {
			openFile(node, 'monaco'); // Temporarily set monaco
		}
	});

	function getChildrenNames(node, type = '') {
		let root = jstreeWidget.jstree(true).get_node(node);
		let names = [];
		if (type) {
			for (let i = 0; i < root.children.length; i++) {
				let child = jstreeWidget.jstree(true).get_node(root.children[i]);
				if (child.type === type) names.push(child.text);
			}
		} else {
			for (let i = 0; i < root.children.length; i++) {
				names.push(jstreeWidget.jstree(true).get_text(root.children[i]));
			}
		}
		return names;
	}

	$scope.toggleSearch = () => {
		$scope.searchField.text = '';
		jstreeWidget.jstree(true).clear_search();
		$scope.searchVisible = !$scope.searchVisible;
	};

	$scope.deleteFileFolder = (path, callback) => {
		RepositoryService.remove(path).then(() => {
			StatusBar.showMessage(`Deleted '${path}'.`);
			if (callback) callback();
		}, (error) => {
			console.error(error);
			StatusBar.showError(`Unable to delete '${path}'.`);
		});
	};

	$scope.reloadFileTree = (basePath, setConfig = false) => {
		treeData.length = 0;
		$scope.state.isBusy = true;
		RegistryService.load(basePath).then((response) => {
			treeData.push(...processChildren(response.data.collections));
			treeData.push(...processChildren(response.data.resources));
			if (setConfig) jstreeWidget.jstree(jstreeConfig);
			else jstreeWidget.jstree(true).refresh();
			$scope.$evalAsync(() => {
				$scope.state.isBusy = false;
				$scope.state.error = false;
			});
		}, (error) => {
			console.error(error);
			$scope.$evalAsync(() => {
				$scope.state.isBusy = false;
				$scope.state.error = false;
				$scope.errorMessage = 'Unable to load registry list';
			});
			Notifications.show({
				type: 'negative',
				title: 'Unable to load registry list',
				description: 'There was an error while trying to load the registry list.'
			});
		});
	};

	let selectedNode;

	$scope.showContextMenu = (event) => {
		selectedNode = undefined;
		const items = [];
		const folder = {
			id: 'folder',
			label: 'New Folder',
			leftIconClass: 'sap-icon--add-folder',
		};
		const file = {
			id: 'file',
			label: 'New File',
			leftIconClass: 'sap-icon--add-document',
		};
		if (jstreeWidget[0].contains(event.target)) {
			event.preventDefault();
			let id;
			if (event.target.tagName !== 'LI') {
				let closest = event.target.closest('li');
				if (closest) id = closest.id;
				else items.push(folder, file);
			} else {
				id = event.target.id;
			}
			if (id) {
				selectedNode = jstreeWidget.jstree(true).get_node(id);
				if (!selectedNode.state.selected) {
					jstreeWidget.jstree(true).deselect_all();
					jstreeWidget.jstree(true).select_node(selectedNode, false, true);
				}
				if (selectedNode.type === 'folder') {
					items.push({
						id: 'new',
						label: 'New',
						iconClass: 'sap-icon--create',
						items: [file, folder],
						separator: true,
					});
				} else if (selectedNode.type === 'file') {
					items.push({
						id: 'open',
						label: 'Open',
						leftIconClass: 'sap-icon--action',
						separator: true,
					});
				}
				items.push({
					id: 'delete',
					label: 'Delete',
					shortcut: inMacOS ? '⌘⌫' : 'Del',
					leftIconClass: 'sap-icon--delete',
				});
			}
			ContextMenu.showContextMenu({
				ariaLabel: 'registry view contextmenu',
				posX: event.clientX,
				posY: event.clientY,
				icons: true,
				items: items
			}).then((id) => {
				if (id === 'open') {
					openFile(selectedNode, 'monaco');
				} else if (id === 'file') {
					newNodeData.parent = selectedNode.id;
					if (newNodeData.parent === '#') {
						newNodeData.path = '/registry/public';
					} else newNodeData.path = selectedNode.data.path;
					Dialogs.showFormDialog({
						title: 'Create a new file',
						form: {
							'fdti1': {
								label: 'Name',
								controlType: 'input',
								type: 'text',
								placeholder: 'new file.txt',
								inputRules: {
									excluded: getChildrenNames(newNodeData.parent, 'file'),
									patterns: ['^[^/:]*$'],
								},
								submitOnEnter: true,
								focus: true,
								required: true
							},
						},
						submitLabel: 'Create',
						cancelLabel: 'Cancel'
					}).then((form) => {
						if (form) createFile(newNodeData.parent, form['fdti1'], newNodeData.path);
					}, (error) => {
						console.error(error);
						Dialogs.showAlert({
							title: 'Create file error',
							message: 'There was an error while processing the new file data.',
							type: AlertTypes.Error,
							preformatted: false,
						});
					});
				} else if (id === 'folder') {
					newNodeData.parent = selectedNode ? selectedNode.id : '#';
					if (newNodeData.parent === '#') {
						newNodeData.path = '/registry/public';
					} else newNodeData.path = selectedNode.data.path;
					Dialogs.showFormDialog({
						title: 'Create new folder',
						form: {
							'fdti1': {
								label: 'Name',
								controlType: 'input',
								type: 'text',
								inputRules: {
									excluded: getChildrenNames(newNodeData.parent, 'folder'),
									patterns: ['^[^/:]*$'],
								},
								submitOnEnter: true,
								focus: true,
								required: true
							}
						},
						submitLabel: 'Create',
						cancelLabel: 'Cancel'
					}).then((form) => {
						if (form) createFolder(newNodeData.parent, form['fdti1'], newNodeData.path);
					}, (error) => {
						console.error(error);
						Dialogs.showAlert({
							title: 'Create folder error',
							message: 'There was an error while processing the new folder data.',
							type: AlertTypes.Error,
							preformatted: false,
						});
					});
				} else if (id === 'delete') {
					openDeleteDialog(selectedNode);
				}
			}, (error) => {
				console.error(error);
				StatusBar.showError('Unable to process context menu data');
			});
		};
	};

	function openDeleteDialog(selected) {
		Dialogs.showDialog({
			title: `Delete '${selected.text}'?`,
			message: 'This action cannot be undone. It is recommended that you unpublish and delete.',
			buttons: [
				{ id: 'b1', label: 'Delete', state: ButtonStates.Negative },
				{ id: 'b3', label: 'Cancel', state: ButtonStates.Transparent },
			]
		}).then((buttonId) => {
			if (buttonId === 'b1') {
				$scope.deleteFileFolder(selected.data.path, () => {
					jstreeWidget.jstree(true).delete_node(selected);
					Layout.closeEditor({ path: selected.data.path });
				});
			}
		}, (error) => {
			console.error(error);
			Dialogs.showAlert({
				title: 'Delete error',
				message: `Error while deleting '${selected.text}'.`,
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	}

	let to = 0;
	$scope.search = (event) => {
		if (to) { clearTimeout(to); }
		if (event.originalEvent.key === 'Escape') {
			$scope.toggleSearch();
			return;
		}
		to = setTimeout(() => {
			jstreeWidget.jstree(true).search($scope.searchField.text);
		}, 250);
	};

	function processChildren(children) {
		const treeChildren = [];
		for (let i = 0; i < children.length; i++) {
			let child = {
				text: children[i].name,
				type: (children[i].type === 'collection' ? 'folder' : 'file'),
				data: {
					path: `/registry/public${children[i].path.slice(9)}`,
				}
			};
			if (children[i].type === 'resource') {
				child.data.contentType = children[i].contentType;
				let icon = getFileIcon(children[i].name);
				if (icon) child.icon = icon;
			}
			if (children[i].collections && children[i].resources) {
				child['children'] = processChildren(children[i].collections.concat(children[i].resources));
			} else if (children[i].collections) {
				child['children'] = processChildren(children[i].collections);
			} else if (children[i].resources) {
				child['children'] = processChildren(children[i].resources);
			}
			treeChildren.push(child);
		}
		return treeChildren;
	}

	function getFileExtension(fileName) {
		return fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length).toLowerCase();
	}

	function getFileIcon(fileName) {
		const ext = getFileExtension(fileName);
		let icon;
		if (ext === 'js' || ext === 'mjs' || ext === 'xsjs' || ext === 'ts' || ext === 'tsx' || ext === 'py' || ext === 'json') {
			icon = 'sap-icon--syntax';
		} else if (ext === 'css' || ext === 'less' || ext === 'scss') {
			icon = 'sap-icon--number-sign';
		} else if (ext === 'txt') {
			icon = 'sap-icon--text';
		} else if (ext === 'pdf') {
			icon = 'sap-icon--pdf-attachment';
		} else if (ext === 'md') {
			icon = 'sap-icon--information';
		} else if (ext === 'access') {
			icon = 'sap-icon--locked';
		} else if (ext === 'zip') {
			icon = 'sap-icon--attachment-zip-file';
		} else if (ext === 'extensionpoint') {
			icon = 'sap-icon--puzzle';
		} else if (imageFileExts.indexOf(ext) !== -1) {
			icon = 'sap-icon--picture';
		} else if (modelFileExts.indexOf(ext) !== -1) {
			icon = 'sap-icon--document-text';
		} else {
			icon = 'jstree-file';
		}
		return icon;
	}

	function openFile(node, editor) {
		Layout.openEditor({
			path: node.data.path,
			contentType: node.data.contentType,
			editorId: editor,
			params: {
				readOnly: $scope.parameters.perspectiveId !== 'workbench',
				resourceType: 'repository',
			},
		});
	}

	function createFile(parent, name, path) {
		RepositoryService.createResource(path, name).then(() => {
			jstreeWidget.jstree(true).deselect_all(true);
			jstreeWidget.jstree(true).select_node(
				jstreeWidget.jstree(true).create_node(
					parent,
					{
						text: name,
						type: 'file',
						data: {
							path: (path.endsWith('/') ? path + name : `${path}/${name}`),
							contentType: 'text/plain',
						}
					},
				)
			);
			// Bug #1948
			// RepositoryService.getMetadata(response.data).then(function (metadata) {
			// 	if (metadata.status === 200) {
			// 		jstreeWidget.jstree(true).deselect_all(true);
			// 		jstreeWidget.jstree(true).select_node(
			// 			jstreeWidget.jstree(true).create_node(
			// 				parent,
			// 				{
			// 					text: metadata.data.name,
			// 					type: 'file',
			// 					data: {
			// 						path: metadata.data.path,
			// 						contentType: metadata.data.contentType,
			// 					}
			// 				},
			// 			)
			// 		);
			// 	} else {
			// 		messageHub.showAlertError('Could not get metadata', `There was an error while getting metadata for '${name}'`);
			// 	}
			// });
		}, (error) => {
			console.error(error);
			Dialogs.showAlert({
				title: 'Could not create a file',
				message: error.message || `There was an error while creating '${name}'`,
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	}

	function createFolder(parent, name, path) {
		RepositoryService.createCollection(path, name).then(() => {
			jstreeWidget.jstree(true).deselect_all(true);
			jstreeWidget.jstree(true).select_node(
				jstreeWidget.jstree(true).create_node(
					parent,
					{
						text: name,
						type: 'folder',
						data: {
							path: (path.endsWith('/') ? path + name : `${path}/${name}`),
						}
					},
				)
			);
			// Bug #1948
			// RepositoryService.getMetadata(response.data).then(function (metadata) {
			//     if (metadata.status === 200) {
			//         jstreeWidget.jstree(true).deselect_all(true);
			//         jstreeWidget.jstree(true).select_node(
			//             jstreeWidget.jstree(true).create_node(
			//                 parent,
			//                 {
			//                     text: metadata.data.name,
			//                     type: 'folder',
			//                     data: {
			//                         path: metadata.data.path,
			//                     }
			//                 },
			//             )
			//         );
			//     } else {
			//         messageHub.showAlertError('Could not get metadata', `There was an error while getting metadata for '${name}'`);
			//     }
			// });
		}, (error) => {
			console.error(error);
			Dialogs.showAlert({
				title: 'Could not create a folder',
				message: error.message || `There was an error while creating '${name}'`,
				type: AlertTypes.Error,
				preformatted: false,
			});
		});
	}

	Repository.onRepositoryModified(() => {
		$scope.reloadFileTree();
	});

	Repository.addMessageListener({
		topic: 'registry.tree.select',
		handler: (data) => {
			const objects = jstreeWidget.jstree(true).get_json('#', {
				no_state: true,
				no_li_attr: true,
				no_a_attr: true,
				flat: true
			});
			for (let i = 0; i < objects.length; i++) {
				if (objects[i].data.path === data.filePath) {
					jstreeWidget.jstree(true).select_node(objects[i]);
					break;
				}
			}
		}
	});

	angular.element($document[0]).ready(() => {
		$scope.reloadFileTree($scope.basePath, true);
	});
	$scope.parameters = ViewParameters.get();
});