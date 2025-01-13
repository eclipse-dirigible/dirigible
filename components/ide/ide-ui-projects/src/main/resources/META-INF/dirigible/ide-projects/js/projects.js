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
const projectsView = angular.module('projects', ['ideUI', 'ideView', 'ideEditors', 'ideWorkspace', 'idePublisher', 'ideTemplates', 'ideGenerate', 'ideTransport', 'ideActions']);

projectsView.controller('ProjectsViewController', [
    '$scope', '$document', 'messageHub', 'workspaceApi', 'Editors', 'publisherApi', 'templatesApi', 'generateApi', 'transportApi', 'actionsApi', 'platform',
    function ($scope, $document, messageHub, workspaceApi, Editors, publisherApi, templatesApi, generateApi, transportApi, actionsApi, platform) {
        $scope.state = {
            isBusy: true,
            error: false,
            busyText: 'Loading...',
        };
        $scope.searchVisible = false;
        $scope.searchField = { text: '' };
        $scope.workspaceNames = [];
        $scope.menuTemplates = [];
        $scope.genericTemplates = [];
        $scope.modelTemplates = [];
        $scope.modelTemplateExtensions = [];
        $scope.gmodel = {
            nodeParents: [],
            templateId: '',
            project: '',
            model: '',
            parameters: {},
        };
        $scope.newNodeData = {
            parent: '',
            workspace: '',
            path: '',
            content: '',
        };
        $scope.actionData = {
            workspace: '',
            project: '',
            action: '',
        };
        $scope.renameNodeData;
        $scope.duplicateProjectData = {};
        $scope.imageFileExts = ['ico', 'bmp', 'png', 'jpg', 'jpeg', 'gif', 'svg'];
        $scope.modelFileExts = ['extension', 'extensionpoint', 'edm', 'model', 'dsm', 'schema', 'bpmn', 'job', 'listener', 'websocket', 'roles', 'constraints', 'table', 'view'];

        $scope.selectedWorkspace = JSON.parse(localStorage.getItem('DIRIGIBLE.workspace') || '{}');
        if (!$scope.selectedWorkspace.name) {
            $scope.selectedWorkspace = { name: 'workspace' }; // Default
            saveSelectedWorkspace();
        }

        $scope.projects = [];

        $scope.jstreeWidget = angular.element('#dgProjects');
        $scope.dndCancel = false;
        $scope.spinnerObj = {
            text: 'Loading...',
            type: 'spinner',
            li_attr: { spinner: true },
        };
        $scope.jstreeConfig = {
            core: {
                check_callback: function (operation) {
                    if (operation == 'move_node' && $scope.dndCancel) return false;
                    return true;
                },
                themes: {
                    name: 'fiori',
                    variant: 'compact',
                },
                data: function (node, cb) {
                    if (node.id === '#') cb($scope.projects);
                    else {
                        workspaceApi.loadContent($scope.selectedWorkspace.name, node.data.path).then(function (response) {
                            if (response.status === 200) {
                                let children;
                                if (response.data.folders && response.data.files) {
                                    children = processChildren(response.data.folders.concat(response.data.files));
                                } else if (response.data.folders) {
                                    children = processChildren(response.data.folders);
                                } else if (response.data.files) {
                                    children = processChildren(response.data.files);
                                }
                                cb(children);
                            } else {
                                messageHub.setStatusError(`There was an error while refreshing the contents of '${node.data.path}'`);
                                console.error(response);
                                $scope.reloadWorkspace();
                            }
                        });
                    }
                },
                keyboard: { // We have to have this in order to disable the default behavior.
                    'enter': function () { },
                    'f2': function () { },
                }
            },
            search: {
                case_sensitive: false,
            },
            plugins: ['wholerow', 'dnd', 'search', 'state', 'types', 'indicator'],
            dnd: {
                touch: 'selected',
                copy: false,
                blank_space_drop: false,
                large_drop_target: true,
                large_drag_target: true,
                is_draggable: function (nodes) {
                    for (let i = 0; i < nodes.length; i++) {
                        if (nodes[i].type === 'project') return false;
                    }
                    return true;
                }
            },
            state: { key: 'ide-projects' },
            types: {
                '#': {
                    valid_children: ['project']
                },
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
                project: {
                    icon: 'jstree-project',
                    valid_children: ['folder', 'file', 'spinner'],
                },
                spinner: {
                    icon: 'jstree-spinner',
                    valid_children: [],
                },
            },
        };

        $scope.keyboardShortcuts = function (keySet, event) {
            event.preventDefault();
            let nodes;
            switch (keySet) {
                case 'enter':
                    const focused = $scope.jstreeWidget.jstree(true).get_node(document.activeElement);
                    if (focused && !focused.state.selected) {
                        $scope.jstreeWidget.jstree(true).deselect_all();
                        $scope.jstreeWidget.jstree(true).select_node(focused);
                        openSelected([focused]);
                    } else openSelected();
                    break;
                case 'shift+enter':
                    const toSelect = $scope.jstreeWidget.jstree(true).get_node(document.activeElement);
                    if (toSelect && !toSelect.state.selected) $scope.jstreeWidget.jstree(true).select_node(toSelect);
                    else $scope.jstreeWidget.jstree(true).deselect_node(toSelect);
                    break;
                case 'f2':
                    nodes = $scope.jstreeWidget.jstree(true).get_selected(true);
                    if (nodes.length < 2) {
                        $scope.renameNodeData = nodes[0];
                        openRenameDialog();
                    }
                    break;
                case 'delete':
                case 'meta+backspace':
                    nodes = $scope.jstreeWidget.jstree(true).get_top_selected(true);
                    if (nodes.length === 1) openDeleteDialog(nodes[0]);
                    else if (nodes.length > 1) openDeleteDialog(nodes);
                    break;
                case 'ctrl+f':
                    $scope.$apply(() => $scope.toggleSearch());
                    break;
                case 'ctrl+c':
                    $scope.jstreeWidget.jstree(true).copy($scope.jstreeWidget.jstree(true).get_top_selected(true));
                    break;
                case 'ctrl+x':
                    $scope.jstreeWidget.jstree(true).cut($scope.jstreeWidget.jstree(true).get_top_selected(true));
                    break;
                case 'ctrl+v':
                    nodes = $scope.jstreeWidget.jstree(true).get_selected(true);
                    if (nodes.length === 1 && $scope.jstreeWidget.jstree(true).can_paste()) {
                        if (nodes[0].type === 'folder' || nodes[0].type === 'project') {
                            $scope.jstreeWidget.jstree(true).paste(nodes[0]);
                        }
                    }
                    break;
                default:
                    break;
            }
        };

        $scope.jstreeWidget.on('select_node.jstree', function (_event, data) {
            if (data.event && data.event.type === 'click' && data.node.type === 'file') {
                messageHub.announceFileSelected({
                    name: data.node.text,
                    path: data.node.data.path,
                    contentType: data.node.data.contentType,
                    workspace: data.node.data.workspace,
                });
            }
        });

        $scope.jstreeWidget.on('dblclick.jstree', function (event) {
            const node = $scope.jstreeWidget.jstree(true).get_node(event.target);
            if (node.type === 'file') openFile(node);
        });

        $scope.jstreeWidget.on('paste.jstree', function (_event, pasteObj) {
            const parent = $scope.jstreeWidget.jstree(true).get_node(pasteObj.parent);
            const spinnerId = showSpinner(parent);
            const targetWorkspace = parent.data.workspace;
            const sourceWorkspace = pasteObj.node[0].data.workspace;
            const targetPath = (parent.data.path.endsWith('/') ? parent.data.path : parent.data.path + '/');
            let pasteArray = [];
            for (let i = 0; i < pasteObj.node.length; i++) {
                if (pasteObj.node[i].data.path === targetPath) return;
                pasteArray.push(pasteObj.node[i].data.path);
            }
            function onResponse(response) {
                if (response.status === 200) { // Move
                    messageHub.getCurrentlyOpenedFiles().then(function (result) {
                        for (let r = 0; r < response.data.length; r++) {
                            for (let f = 0; f < result.data.files.length; f++) {
                                if (result.data.files[f].startsWith(`/${sourceWorkspace}/${response.data[r].from}`)) {
                                    messageHub.announceFileMoved({
                                        name: result.data.files[f].substring(result.data.files[f].lastIndexOf('/') + 1, result.data.files[f].length),
                                        path: result.data.files[f].replace(`/${sourceWorkspace}`, '').replace(response.data[r].from, response.data[r].to),
                                        oldPath: result.data.files[f].replace(`/${sourceWorkspace}`, ''),
                                        workspace: targetWorkspace,
                                    });
                                }
                            }
                        }
                    });
                    $scope.jstreeWidget.jstree(true).load_node(parent);
                } else if (response.status === 201) { // Copy
                    $scope.jstreeWidget.jstree(true).load_node(parent);
                } else {
                    console.error(response);
                    $scope.reloadWorkspace();
                    if (pasteObj.mode !== 'copy_node' && pasteObj.node.length > 1)
                        messageHub.setStatusError(`Unable to move ${pasteObj.node.length} objects.`);
                    else if (pasteObj.mode !== 'copy_node' && pasteObj.node.length === 1)
                        messageHub.setStatusError(`Unable to move '${pasteObj.node[0].text}'.`);
                    if (pasteObj.mode === 'copy_node' && pasteObj.node.length > 1)
                        messageHub.setStatusError(`Unable to copy ${pasteObj.node.length} objects.`);
                    else if (pasteObj.mode === 'copy_node' && pasteObj.node.length === 1)
                        messageHub.setStatusError(`Unable to copy '${pasteObj.node[0].text}'.`);
                }
                hideSpinner(spinnerId);
            }
            if (pasteObj.mode === 'copy_node') {
                workspaceApi.copy(
                    pasteArray,
                    targetPath,
                    sourceWorkspace,
                    targetWorkspace,
                ).then(onResponse);
            } else {
                for (let pIndex = 0; pIndex < pasteObj.node.length; pIndex++) { // Temp solution
                    for (let i = 0; i < parent.children.length; i++) {
                        const node = $scope.jstreeWidget.jstree(true).get_node(parent.children[i]);
                        if (node.text === pasteObj.node[pIndex].text && node.id !== pasteObj.node[pIndex].id) {
                            messageHub.showAlertError('Could not move', 'The destination contains a file/folder with the same name.');
                            $scope.reloadWorkspace();
                            return;
                        }
                    }
                }
                workspaceApi.move(
                    pasteArray,
                    pasteArray.length === 1 ? targetPath + pasteObj.node[0].text : targetPath,
                    sourceWorkspace,
                    targetWorkspace,
                ).then(onResponse);
            }
        });

        $(document).bind('dnd_stop.vakata', function (_e, data) { //Triggered on drag complete
            const target = $scope.jstreeWidget.jstree(true).get_node(data.event.target);
            for (let i = 0; i < data.data.nodes.length; i++) {
                if (!target || target.children.includes(data.data.nodes[i])) {
                    $scope.dndCancel = true;
                    return;
                }
            }
            $scope.dndCancel = false;
            if (!data.data.nodes.includes(target.id)) {
                $scope.jstreeWidget.jstree(true).cut(data.data.nodes);
                $scope.jstreeWidget.jstree(true).paste(target);
                $scope.jstreeWidget.jstree(true).delete_node(data.data.nodes);
            } else $scope.dndCancel = true;
        });

        function setMenuTemplateItems(parent, menuObj, workspace, nodePath) {
            let priorityTemplates = true;
            let childExtensions = {};
            const children = getChildrenNames(parent, 'file');
            for (let i = 0; i < children.length; i++) {
                let lastIndex = children[i].lastIndexOf('.');
                if (lastIndex !== -1) childExtensions[children[i].substring(lastIndex + 1)] = true;
            }
            for (let i = 0; i < $scope.menuTemplates.length; i++) {
                let item = {
                    id: $scope.menuTemplates[i].id,
                    label: $scope.menuTemplates[i].label,
                };
                if ($scope.menuTemplates[i].oncePerFolder) {
                    if (childExtensions[$scope.menuTemplates[i].extension]) {
                        item.isDisabled = true;
                    }
                }
                if (!item.isDisabled) {
                    item.data = {
                        parent: parent,
                        extension: $scope.menuTemplates[i].extension,
                        path: nodePath,
                        content: $scope.menuTemplates[i].data,
                        workspace: workspace,
                        name: $scope.menuTemplates[i].name,
                        staticName: $scope.menuTemplates[i].staticName || false,
                        nameless: $scope.menuTemplates[i].nameless || false,
                    };
                }
                if (priorityTemplates && !$scope.menuTemplates[i].hasOwnProperty('order')) {
                    item.divider = true;
                    priorityTemplates = false;
                }
                menuObj.items[0].items.push(item);
            }
            menuObj.items[0].items[2].divider = true;
        }

        function getProjectNode(parents) {
            for (let i = 0; i < parents.length; i++) {
                if (parents[i] !== '#') {
                    const parent = $scope.jstreeWidget.jstree(true).get_node(parents[i]);
                    if (parent.type === 'project') {
                        return parent;
                    }
                }
            }
        }

        function getChildrenNames(node, type = '') {
            const root = $scope.jstreeWidget.jstree(true).get_node(node);
            let names = [];
            if (type) {
                for (let i = 0; i < root.children.length; i++) {
                    let child = $scope.jstreeWidget.jstree(true).get_node(root.children[i]);
                    if (child.type === type) names.push(child.text);
                }
            } else {
                for (let i = 0; i < root.children.length; i++) {
                    names.push($scope.jstreeWidget.jstree(true).get_text(root.children[i]));
                }
            }
            return names;
        }

        $scope.contextMenuContent = function (element) {
            if ($scope.jstreeWidget[0].contains(element)) {
                let id;
                if (element.tagName !== 'LI') {
                    const closest = element.closest('li');
                    if (closest) {
                        id = closest.id;
                    } else {
                        let items = [];
                        items.push({
                            id: 'newProject',
                            label: 'New Project',
                            icon: 'sap-icon--create',
                        });
                        if (publisherApi.isEnabled()) {
                            items.push({
                                id: 'publishAll',
                                label: 'Publish All',
                                icon: 'sap-icon--arrow-top',
                                divider: true,
                            });
                            items.push({
                                id: 'unpublishAll',
                                label: 'Unpublish All',
                                icon: 'sap-icon--arrow-bottom',
                            });
                        }
                        items.push({
                            id: 'exportProjects',
                            label: 'Export all',
                            icon: 'sap-icon--download-from-cloud',
                            divider: true,
                        });
                        return {
                            callbackTopic: 'projects.tree.contextmenu',
                            hasIcons: true,
                            items: items
                        }
                    }
                } else {
                    id = element.id;
                }
                if (id) {
                    const node = $scope.jstreeWidget.jstree(true).get_node(id);
                    if (!node.state.selected) {
                        $scope.jstreeWidget.jstree(true).deselect_all();
                        $scope.jstreeWidget.jstree(true).select_node(node, false, true);
                    }
                    let nodes = $scope.jstreeWidget.jstree(true).get_selected(false);
                    if (nodes.length === 1) nodes.length = 0;
                    const newSubmenu = {
                        id: 'new',
                        label: 'New',
                        icon: 'sap-icon--create',
                        items: [{
                            id: 'file',
                            label: 'File',
                            data: {
                                workspace: node.data.workspace,
                                path: node.data.path,
                                parent: node.id
                            },
                        }, {
                            id: 'folder',
                            label: 'Folder',
                            data: {
                                workspace: node.data.workspace,
                                path: node.data.path,
                                parent: node.id
                            },
                        }]
                    };
                    const cutObj = {
                        id: 'cut',
                        label: 'Cut',
                        shortcut: platform.isMac ? '⌘X' : 'Ctrl+X',
                        divider: true,
                        icon: 'sap-icon--scissors',
                    };
                    const copyObj = {
                        id: 'copy',
                        label: 'Copy',
                        shortcut: platform.isMac ? '⌘C' : 'Ctrl+C',
                        icon: 'sap-icon--copy',
                    };
                    const pasteObj = {
                        id: 'paste',
                        label: 'Paste',
                        shortcut: platform.isMac ? '⌘V' : 'Ctrl+V',
                        icon: 'sap-icon--paste',
                        isDisabled: !$scope.jstreeWidget.jstree(true).can_paste() || nodes.length > 1,
                        data: node,
                    };
                    const renameObj = {
                        id: 'rename',
                        label: 'Rename',
                        shortcut: 'F2',
                        divider: true,
                        icon: 'sap-icon--edit',
                        isDisabled: nodes.length > 1,
                        data: node,
                    };
                    const deleteObj = {
                        id: 'delete',
                        label: (nodes.length) ? `Delete ${nodes.length} items` : 'Delete',
                        shortcut: platform.isMac ? '⌘⌫' : 'Del',
                        icon: 'sap-icon--delete',
                        data: (nodes.length) ? nodes : node,
                    };
                    let publishObj;
                    let unpublishObj;
                    if (publisherApi.isEnabled()) {
                        publishObj = {
                            id: 'publish',
                            label: 'Publish',
                            divider: true,
                            icon: 'sap-icon--arrow-top',
                        };
                        unpublishObj = {
                            id: 'unpublish',
                            label: 'Unpublish',
                            icon: 'sap-icon--arrow-bottom',
                        };
                    }
                    let generateObj;
                    if (generateApi.isEnabled()) {
                        generateObj = {
                            id: 'generateGeneric',
                            label: 'Generate',
                            icon: 'sap-icon-TNT--operations',
                            divider: true,
                            data: node,
                        };
                    }
                    const importObj = {
                        id: 'import',
                        label: 'Import',
                        icon: 'sap-icon--attachment',
                        divider: true,
                        data: node,
                    };
                    const importZipObj = {
                        id: 'importZip',
                        label: 'Import from zip',
                        icon: 'sap-icon--attachment-zip-file',
                        data: node,
                    };
                    if (node.type === 'project') {
                        let items = [
                            newSubmenu,
                            {
                                id: 'duplicateProject',
                                label: 'Duplicate',
                                divider: true,
                                icon: 'sap-icon--duplicate',
                                data: node,
                            },
                            pasteObj,
                            renameObj,
                            deleteObj,
                        ]
                        if (publisherApi.isEnabled()) {
                            items.push(publishObj);
                            items.push(unpublishObj);
                        }
                        let menuObj = {
                            callbackTopic: 'projects.tree.contextmenu',
                            hasIcons: true,
                            items: items
                        };
                        if ($scope.menuTemplates.length && generateApi.isEnabled()) {
                            menuObj.items.push(generateObj);
                            setMenuTemplateItems(node.id, menuObj, node.data.workspace, node.data.path);
                        }
                        menuObj.items.push(importObj);
                        menuObj.items.push(importZipObj);
                        menuObj.items.push({
                            id: 'exportProject',
                            label: 'Export',
                            icon: 'sap-icon--download-from-cloud',
                            divider: true,
                            data: node,
                        });
                        if (actionsApi.isEnabled()) {
                            menuObj.items.push({
                                id: 'actionsProject',
                                label: 'Actions',
                                icon: 'sap-icon--media-play',
                                divider: true,
                                data: node,
                            });
                        }
                        return menuObj;
                    } else if (node.type === 'folder') {
                        let items = [
                            newSubmenu,
                            cutObj,
                            copyObj,
                            pasteObj,
                            renameObj,
                            deleteObj,
                            generateObj,
                            importObj,
                            importZipObj,
                        ]
                        if (publisherApi.isEnabled()) {
                            items.push(publishObj);
                            items.push(unpublishObj);
                        }
                        let menuObj = {
                            callbackTopic: 'projects.tree.contextmenu',
                            hasIcons: true,
                            items: items
                        };
                        setMenuTemplateItems(node.id, menuObj, node.data.workspace, node.data.path);
                        return menuObj;
                    } else if (node.type === 'file') {
                        let items = [
                            {
                                id: 'open',
                                label: 'Open',
                                icon: 'sap-icon--action',
                            },
                            cutObj,
                            copyObj,
                            renameObj,
                            deleteObj,
                        ]
                        if (nodes.length <= 1) {
                            items[0].data = node;
                            items.splice(1, 0, {
                                id: 'openWith',
                                label: 'Open With',
                                icon: 'sap-icon--action',
                                items: getEditorsForType(node)
                            });
                        }
                        if (publisherApi.isEnabled()) {
                            items.push(publishObj);
                            items.push(unpublishObj);
                        }
                        let menuObj = {
                            callbackTopic: 'projects.tree.contextmenu',
                            hasIcons: true,
                            items: items
                        };
                        if (generateApi.isEnabled()) {
                            const fileExt = getFileExtension(node.text);
                            if (fileExt === 'gen') {
                                let regenObj = {
                                    id: 'regenerateModel',
                                    label: 'Regenerate',
                                    icon: 'sap-icon--refresh',
                                    divider: true,
                                    data: undefined,
                                    isDisabled: false,
                                };
                                if (node.parents.length > 2) {
                                    regenObj.isDisabled = true;
                                } else {
                                    regenObj.data = node;
                                }
                                menuObj.items.push(regenObj);
                            }
                            else if ($scope.modelTemplates.length && $scope.modelTemplateExtensions.includes(fileExt)) {
                                let genObj = {
                                    id: 'generateModel',
                                    label: 'Generate',
                                    icon: 'sap-icon-TNT--operations',
                                    divider: true,
                                    data: undefined,
                                    isDisabled: false,
                                };
                                if (node.parents.length > 2) {
                                    genObj.isDisabled = true;
                                } else {
                                    genObj.data = node;
                                }
                                menuObj.items.push(genObj);
                            }
                            return menuObj;
                        }
                    }
                }
                return;
            } else return;
        };

        $scope.toggleSearch = function () {
            $scope.searchField.text = '';
            $scope.jstreeWidget.jstree(true).clear_search();
            $scope.searchVisible = !$scope.searchVisible;
        };

        $scope.isSelectedWorkspace = function (name) {
            if ($scope.selectedWorkspace.name === name) return true;
            return false;
        };

        $scope.reloadWorkspaceList = function () {
            workspaceApi.listWorkspaceNames().then(function (response) {
                if (response.status === 200) {
                    $scope.workspaceNames = response.data;
                    $scope.state.error = false;
                    $scope.reloadWorkspace(true);
                    $scope.loadTemplates();
                } else {
                    $scope.state.error = true;
                    $scope.errorMessage = 'Unable to load workspace list.';
                    messageHub.setStatusError('Unable to load workspace list');
                }
            });
        };

        $scope.reloadWorkspace = function (setConfig = false) {
            $scope.projects.length = 0;
            $scope.state.isBusy = true;
            workspaceApi.load($scope.selectedWorkspace.name).then(function (response) {
                if (response.status === 200) {
                    for (let i = 0; i < response.data.projects.length; i++) {
                        let project = {
                            text: response.data.projects[i].name,
                            type: response.data.projects[i].type,
                            data: {
                                git: response.data.projects[i].git,
                                gitName: response.data.projects[i].gitName,
                                path: response.data.projects[i].path.substring(response.data.path.length, response.data.projects[i].path.length), // Back-end should not include workspase name in path
                                workspace: response.data.name,
                            },
                            li_attr: { git: response.data.projects[i].git },
                        };
                        if (response.data.projects[i].folders && response.data.projects[i].files) {
                            project['children'] = processChildren(response.data.projects[i].folders.concat(response.data.projects[i].files));
                        } else if (response.data.projects[i].folders) {
                            project['children'] = processChildren(response.data.projects[i].folders);
                        } else if (response.data.projects[i].files) {
                            project['children'] = processChildren(response.data.projects[i].files);
                        }
                        $scope.projects.push(project);
                    }
                    $scope.state.isBusy = false;
                    if (setConfig) $scope.jstreeWidget.jstree($scope.jstreeConfig);
                    else $scope.jstreeWidget.jstree(true).refresh();
                } else {
                    $scope.state.isBusy = false;
                    $scope.state.error = true;
                    $scope.errorMessage = 'Unable to load workspace data.';
                    messageHub.setStatusError('Unable to load workspace data');
                }
            });
        };

        $scope.loadTemplates = function () {
            $scope.menuTemplates.length = 0;
            $scope.genericTemplates.length = 0;
            $scope.modelTemplates.length = 0;
            $scope.modelTemplateExtensions.length = 0;
            templatesApi.menuTemplates().then(function (response) {
                if (response.status === 200) {
                    $scope.menuTemplates = response.data
                    for (let i = 0; i < response.data.length; i++) {
                        if (response.data[i].isModel) {
                            $scope.modelFileExts.push(response.data[i].extension);
                        } else if (response.data[i].isImage) {
                            $scope.imageFileExts.push(response.data[i].extension);
                        }
                    }
                } else messageHub.setStatusError('Unable to load menu template list');
            });
            templatesApi.listTemplates().then(function (response) {
                if (response.status === 200) {
                    for (let i = 0; i < response.data.length; i++) {
                        if (response.data[i].hasOwnProperty('extension')) {
                            $scope.modelTemplates.push(response.data[i]);
                            $scope.modelTemplateExtensions.push(response.data[i].extension);
                        } else {
                            $scope.genericTemplates.push(response.data[i]);
                        }
                    }
                } else messageHub.setStatusError('Unable to load template list');
            });
        };

        $scope.publishAll = function () {
            messageHub.showStatusBusy('Publishing projects...');
            publisherApi.publish(`/${$scope.selectedWorkspace.name}/*`).then(function (response) {
                if (response.status !== 200)
                    messageHub.setStatusError(`Unable to publish projects in '${$scope.selectedWorkspace.name}'`);
                else messageHub.setStatusMessage(`Published all projects in '${$scope.selectedWorkspace.name}'`);
                messageHub.hideStatusBusy();
                messageHub.announcePublish();
            });

        };

        function unpublishAll() {
            messageHub.showStatusBusy('Unpublishing projects...');
            publisherApi.unpublish(`/${$scope.selectedWorkspace.name}/*`).then(function (response) {
                if (response.status !== 200)
                    messageHub.setStatusError(`Unable to unpublish projects in '${$scope.selectedWorkspace.name}'`);
                else messageHub.setStatusMessage(`Unpublished all projects in '${$scope.selectedWorkspace.name}'`);
                messageHub.hideStatusBusy();
                messageHub.announceUnpublish();
            });
        };

        function publish(path, workspace, fileDescriptor, callback) {
            messageHub.showStatusBusy(`Publishing '${path}'...`);
            publisherApi.publish(path, workspace).then(function (response) {
                if (response.status !== 200) {
                    messageHub.setStatusError(`Unable to publish '${path}'`);
                } else {
                    messageHub.setStatusMessage(`Published '${path}'`);
                    if (callback) callback();
                }
                messageHub.hideStatusBusy();
                if (fileDescriptor) messageHub.announcePublish(fileDescriptor);
            });
        };

        function unpublish(path, workspace, fileDescriptor, callback) {
            messageHub.showStatusBusy(`Unpublishing '${path}'...`);
            publisherApi.unpublish(path, workspace).then(function (response) {
                if (response.status !== 200) {
                    messageHub.setStatusError(`Unable to unpublish '${path}'`);
                } else {
                    messageHub.setStatusMessage(`Unpublished '${path}'`);
                    if (callback) callback();
                }
                messageHub.hideStatusBusy();
                if (fileDescriptor) messageHub.announceUnpublish(fileDescriptor);
            });
        };

        $scope.switchWorkspace = function (workspace) {
            if ($scope.selectedWorkspace.name !== workspace) {
                $scope.selectedWorkspace.name = workspace;
                saveSelectedWorkspace();
                $scope.reloadWorkspace();
            }
        };

        $scope.isPublishEnabled = function () {
            return publisherApi.isEnabled();
        };

        $scope.saveAll = function () {
            messageHub.triggerEvent('editor.file.save.all', true);
        };

        function deleteFileFolder(workspace, path, nodeId) {
            workspaceApi.remove(workspace + path).then(function (response) {
                if (response.status !== 204) {
                    messageHub.setStatusError(`Unable to delete '${path}'.`);
                } else {
                    messageHub.setStatusMessage(`Deleted '${path}'.`);
                    messageHub.announceFileDeleted({
                        name: path.substring(path.lastIndexOf('/') + 1, path.length),
                        path: path,
                        workspace: workspace
                    });
                    $scope.jstreeWidget.jstree(true).delete_node(nodeId);
                }
            });
        };

        function deleteProject(workspace, project, nodeId) {
            workspaceApi.deleteProject(workspace, project).then(function (response) {
                if (response.status !== 204) {
                    messageHub.setStatusError(`Unable to delete '${project}'.`);
                } else {
                    messageHub.setStatusMessage(`Deleted '${project}'.`);
                    $scope.jstreeWidget.jstree(true).delete_node(nodeId);
                }
            });
        };

        $scope.exportProjects = function () {
            transportApi.exportProject($scope.selectedWorkspace.name, '*');
        };

        // $scope.linkProject = function () {
        //     messageHub.showFormDialog(
        //         'linkProjectForm',
        //         'Link project',
        //         [{
        //             id: 'pgfi1',
        //             type: 'input',
        //             label: 'Name',
        //             required: true,
        //             placeholder: 'project name',
        //             inputRules: {
        //                 excluded: getChildrenNames('#'),
        //                 patterns: ['^[^/:]*$'],
        //             },
        //         }, {
        //             id: 'pgfi2',
        //             type: 'input',
        //             label: 'Path',
        //             required: true,
        //             placeholder: '/absolute/path/to/project',
        //         }],
        //         [{
        //             id: 'b1',
        //             type: 'emphasized',
        //             label: 'Create',
        //             whenValid: true,
        //         }, {
        //             id: 'b2',
        //             type: 'transparent',
        //             label: 'Cancel',
        //         }],
        //         'projects.link.project',
        //         'Linking...',
        //     );
        //     const handler = messageHub.onDidReceiveMessage(
        //         'projects.link.project',
        //         function (msg) {
        //             if (msg.data.isMenu) {
        //                 $scope.linkProject();
        //             } else if (msg.data.buttonId === 'b1') {
        //                 workspaceApi.linkProject($scope.selectedWorkspace.name, msg.data.formData[0].value, msg.data.formData[1].value).then(function (response) {
        //                     messageHub.hideFormDialog('linkProjectForm');
        //                     if (response.status !== 201) {
        //                         messageHub.showAlertError(
        //                             'Failed to link project',
        //                             `An unexpected error has occurred while trying to link project '${msg.data.formData[0].value}'`
        //                         );
        //                         messageHub.setStatusError(`Unable to link project '${msg.data.formData[0].value}'`);
        //                     } else {
        //                         $scope.reloadWorkspace();
        //                         messageHub.setStatusMessage(`Linked project '${msg.data.formData[0].value}'`);
        //                     }
        //                 });
        //             } else messageHub.hideFormDialog('linkProjectForm');
        //             messageHub.unsubscribe(handler);
        //         },
        //         true
        //     );
        // };

        $scope.createProject = function () {
            messageHub.showFormDialog(
                'createProjectForm',
                'Create project',
                [
                    {
                        id: 'pgfi1',
                        type: 'input',
                        submitOnEnterId: 'b1',
                        label: 'Name',
                        required: true,
                        placeholder: 'project name',
                        inputRules: {
                            excluded: getChildrenNames('#'),
                            patterns: ['^[^/:]*$'],
                        },
                    },
                ],
                [{
                    id: 'b1',
                    type: 'emphasized',
                    label: 'Create',
                    whenValid: true,
                },
                {
                    id: 'b2',
                    type: 'transparent',
                    label: 'Cancel',
                }],
                'projects.create.project',
                'Creating...',
            );
            const handler = messageHub.onDidReceiveMessage(
                'projects.create.project',
                function (msg) {
                    if (msg.data.buttonId === 'b1') {
                        workspaceApi.createProject($scope.selectedWorkspace.name, msg.data.formData[0].value).then(function (response) {
                            messageHub.hideFormDialog('createProjectForm');
                            if (response.status !== 201) {
                                messageHub.showAlertError(
                                    'Failed to create project',
                                    `An unexpected error has occurred while trying create a project named '${msg.data.formData[0].value}'`
                                );
                                messageHub.setStatusError(`Unable to create project '${msg.data.formData[0].value}'`);
                            } else {
                                messageHub.setStatusMessage(`Created project '${msg.data.formData[0].value}'`);
                                $scope.reloadWorkspace();
                            }
                        });
                    } else messageHub.hideFormDialog('createProjectForm');
                    messageHub.unsubscribe(handler);
                },
                true
            );
        };

        $scope.duplicateProject = function (node) {
            let title = 'Duplicate project';
            let projectName = '';
            let workspaces = [];
            for (let i = 0; i < $scope.workspaceNames.length; i++) {
                workspaces.push({
                    label: $scope.workspaceNames[i],
                    value: $scope.workspaceNames[i],
                });
            }
            let dialogItems = [{
                id: 'pgfd1',
                type: 'dropdown',
                label: 'Duplicate in workspace',
                required: true,
                value: $scope.selectedWorkspace.name,
                items: workspaces,
            }];
            if (!node) {
                let projectNames = [];
                let root = $scope.jstreeWidget.jstree(true).get_node('#');
                for (let i = 0; i < root.children.length; i++) {
                    let name = $scope.jstreeWidget.jstree(true).get_text(root.children[i])
                    projectNames.push({
                        label: name,
                        value: name,
                    });
                }
                dialogItems.push({
                    id: 'pgfd2',
                    type: 'dropdown',
                    label: 'Project',
                    required: true,
                    value: '',
                    items: projectNames,
                });
            } else {
                projectName = `${node.text} 2`;
                $scope.duplicateProjectData.originalPath = node.data.path;
                $scope.duplicateProjectData.originalWorkspace = node.data.workspace;
                title = `Duplicate project '${node.text}'`;
            }
            dialogItems.push({
                id: 'pgfi1',
                type: 'input',
                submitOnEnterId: 'b1',
                label: 'Duplicated project name',
                required: true,
                placeholder: 'project name',
                inputRules: {
                    excluded: getChildrenNames('#'),
                    patterns: ['^[^/:]*$'],
                },
                value: projectName,
            });
            messageHub.showFormDialog(
                'duplicateProjectForm',
                title,
                dialogItems,
                [{
                    id: 'b1',
                    type: 'emphasized',
                    label: 'Duplicate',
                    whenValid: true,
                }, {
                    id: 'b2',
                    type: 'transparent',
                    label: 'Cancel',
                }],
                'projects.duplicate.project',
                'Duplicating...',
            );
            const handler = messageHub.onDidReceiveMessage(
                'projects.duplicate.project',
                function (msg) {
                    if (msg.data.buttonId === 'b1') {
                        let originalPath;
                        let originalWorkspace;
                        let duplicatePath;
                        if (msg.data.formData[1].type === 'dropdown') {
                            let root = $scope.jstreeWidget.jstree(true).get_node('#');
                            for (let i = 0; i < root.children.length; i++) {
                                let child = $scope.jstreeWidget.jstree(true).get_node(root.children[i]);
                                if (child.text === msg.data.formData[1].value) {
                                    originalPath = child.data.path;
                                    originalWorkspace = child.data.workspace;
                                    break;
                                }
                            }
                            duplicatePath = `/${msg.data.formData[2].value}`;
                        } else {
                            originalWorkspace = $scope.duplicateProjectData.originalWorkspace
                            originalPath = $scope.duplicateProjectData.originalPath;
                            duplicatePath = `/${msg.data.formData[1].value}`;
                        }
                        workspaceApi.copy(
                            originalPath,
                            duplicatePath,
                            originalWorkspace,
                            msg.data.formData[0].value,
                        ).then(function (response) {
                            if (response.status === 201) {
                                if (msg.data.formData[0].value === $scope.selectedWorkspace.name)
                                    $scope.reloadWorkspace(); // Temp
                                messageHub.setStatusMessage(`Duplicated '${originalPath}'`);
                            } else {
                                messageHub.setStatusError(`Unable to duplicate '${originalPath}'`);
                                messageHub.showAlertError(
                                    'Failed to duplicate project',
                                    `An unexpected error has occurred while trying duplicate '${originalPath}'`,
                                );
                            }
                            messageHub.hideFormDialog('duplicateProjectForm');
                        });
                    } else messageHub.hideFormDialog('duplicateProjectForm');
                    messageHub.unsubscribe(handler);
                },
                true
            );
        };

        $scope.createWorkspace = function () {
            messageHub.showFormDialog(
                'createWorkspaceForm',
                'Create workspace',
                [{
                    id: 'pgfi1',
                    type: 'input',
                    submitOnEnterId: 'b1',
                    label: 'Name',
                    required: true,
                    placeholder: 'workspace name',
                    inputRules: {
                        excluded: $scope.workspaceNames,
                        patterns: ['^[^/:]*$'],
                    },
                }],
                [{
                    id: 'b1',
                    type: 'emphasized',
                    label: 'Create',
                    whenValid: true,
                }, {
                    id: 'b2',
                    type: 'transparent',
                    label: 'Cancel',
                }],
                'projects.create.workspace',
                'Creating...',
            );
            const handler = messageHub.onDidReceiveMessage(
                'projects.create.workspace',
                function (msg) {
                    if (msg.data.buttonId === 'b1') {
                        workspaceApi.createWorkspace(msg.data.formData[0].value).then(function (response) {
                            messageHub.hideFormDialog('createWorkspaceForm');
                            if (response.status !== 201) {
                                messageHub.showAlertError(
                                    'Failed to create workspace',
                                    `An unexpected error has occurred while trying create a workspace named '${msg.data.formData[0].value}'`
                                );
                                messageHub.setStatusError(`Unable to create workspace '${msg.data.formData[0].value}'`);
                            } else {
                                $scope.reloadWorkspaceList();
                                messageHub.setStatusMessage(`Created workspace '${msg.data.formData[0].value}'`);
                                messageHub.announceWorkspacesModified();
                            }
                        });
                    } else messageHub.hideFormDialog('createWorkspaceForm');
                    messageHub.unsubscribe(handler);
                },
                true
            );
        };

        $scope.deleteWorkspace = function () {
            if ($scope.selectedWorkspace.name !== 'workspace') {
                messageHub.showDialogAsync(
                    'Delete workspace?',
                    `Are you sure you want to delete workspace "${$scope.selectedWorkspace.name}"? This action cannot be undone.`,
                    [{
                        id: 'b1',
                        type: 'emphasized',
                        label: 'Yes',
                    }, {
                        id: 'b3',
                        type: 'normal',
                        label: 'No',
                    }],
                ).then(function (msg) {
                    if (msg.data === 'b1') {
                        workspaceApi.deleteWorkspace($scope.selectedWorkspace.name).then(function (response) {
                            if (response.status === 204) {
                                $scope.switchWorkspace('workspace');
                                $scope.reloadWorkspaceList();
                                messageHub.announceWorkspacesModified();
                            } else {
                                messageHub.setStatusError(`Unable to delete workspace '${$scope.selectedWorkspace.name}'`);
                            }
                        });
                    }
                });
            }
        };

        let to = 0;
        $scope.search = function (event) {
            if (to) { clearTimeout(to); }
            if (event.originalEvent.key === "Escape") {
                $scope.toggleSearch();
                return;
            }
            to = setTimeout(function () {
                $scope.jstreeWidget.jstree(true).search($scope.searchField.text);
            }, 250);
        };

        function showSpinner(parent) {
            return $scope.jstreeWidget.jstree(true).create_node(parent, $scope.spinnerObj, 0);
        }

        function hideSpinner(spinnerId) {
            $scope.jstreeWidget.jstree(true).delete_node($scope.jstreeWidget.jstree(true).get_node(spinnerId));
        }

        function processChildren(children) {
            let treeChildren = [];
            for (let i = 0; i < children.length; i++) {
                let child = {
                    text: children[i].name,
                    type: children[i].type,
                    state: {
                        status: children[i].status
                    },
                    data: {
                        path: children[i].path.substring($scope.selectedWorkspace.name.length + 1, children[i].path.length), // Back-end should not include workspase name in path
                        workspace: $scope.selectedWorkspace.name,
                    }
                };
                if (children[i].type === 'file') {
                    child.data.contentType = children[i].contentType;
                    let icon = getFileIcon(children[i].name);
                    if (icon) child.icon = icon;
                }
                if (children[i].folders && children[i].files) {
                    child['children'] = processChildren(children[i].folders.concat(children[i].files));
                } else if (children[i].folders) {
                    child['children'] = processChildren(children[i].folders);
                } else if (children[i].files) {
                    child['children'] = processChildren(children[i].files);
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
            } else if ($scope.imageFileExts.indexOf(ext) !== -1) {
                icon = 'sap-icon--picture';
            } else if ($scope.modelFileExts.indexOf(ext) !== -1) {
                icon = 'sap-icon--document-text';
            } else {
                icon = 'jstree-file';
            }
            return icon;
        }

        function getEditorsForType(node) {
            let editors = [{
                id: 'openWith',
                label: Editors.defaultEditor.label,
                data: {
                    node: node,
                    editorId: Editors.defaultEditor.id,
                }
            }];
            const editorsForContentType = Editors.editorsForContentType;
            if (Object.keys(editorsForContentType).indexOf(node.data.contentType) > -1) {
                for (let i = 0; i < editorsForContentType[node.data.contentType].length; i++) {
                    if (editorsForContentType[node.data.contentType][i].id !== Editors.defaultEditor.id)
                        editors.push({
                            id: 'openWith',
                            label: editorsForContentType[node.data.contentType][i].label,
                            data: {
                                node: node,
                                editorId: editorsForContentType[node.data.contentType][i].id,
                            }
                        });
                }
            }
            return editors;
        }

        function openSelected(nodes) {
            if (!nodes) nodes = $scope.jstreeWidget.jstree(true).get_selected(true)
            for (let i = 0; i < nodes.length; i++) {
                if (nodes[i].type === 'file') {
                    openFile(nodes[i]);
                } else if (nodes[i].type === 'folder' || nodes[i].type === 'project') {
                    if (nodes[i].state.opened) $scope.jstreeWidget.jstree(true).close_node(nodes[i]);
                    else $scope.jstreeWidget.jstree(true).open_node(nodes[i]);
                }
            }
        }

        function openFile(node, editor = undefined) {
            let parent = node;
            let extraArgs = { gitName: undefined, workspaceName: node.data.workspace };
            for (let i = 0; i < node.parents.length - 1; i++) {
                parent = $scope.jstreeWidget.jstree(true).get_node(parent.parent);
            }
            if (parent.data.git) {
                extraArgs.gitName = parent.data.gitName;
            }
            messageHub.openEditor(
                `/${node.data.workspace}${node.data.path}`,
                node.text,
                node.data.contentType,
                editor,
                extraArgs
            );
        }

        function saveSelectedWorkspace() {
            localStorage.setItem('DIRIGIBLE.workspace', JSON.stringify($scope.selectedWorkspace));
        }

        function createFile(parent, name, workspace, path, content = '') {
            workspaceApi.createNode(name, `/${workspace}${path}`, false, content).then(function (response) {
                if (response.status === 201) {
                    workspaceApi.getMetadata(response.data).then(function (metadata) {
                        if (metadata.status === 200) {
                            $scope.jstreeWidget.jstree(true).deselect_all(true);
                            $scope.jstreeWidget.jstree(true).select_node(
                                $scope.jstreeWidget.jstree(true).create_node(
                                    parent,
                                    {
                                        text: metadata.data.name,
                                        type: 'file',
                                        state: {
                                            status: metadata.data.status
                                        },
                                        icon: getFileIcon(metadata.data.name),
                                        data: {
                                            path: metadata.data.path.substring($scope.selectedWorkspace.name.length + 1, metadata.data.path.length),
                                            workspace: $scope.selectedWorkspace.name,
                                            contentType: metadata.data.contentType,
                                        }
                                    },
                                )
                            );
                        } else {
                            messageHub.showAlertError('Could not create a file', `There was an error while creating '${name}'`);
                        }
                    });
                } else {
                    messageHub.showAlertError('Could not create a file', `There was an error while creating '${name}'`);
                }
            });
        }

        function createFolder(parent, name, workspace, path) {
            workspaceApi.createNode(name, `/${workspace}${path}`, true).then(function (response) {
                if (response.status === 201) {
                    $scope.jstreeWidget.jstree(true).deselect_all(true);
                    $scope.jstreeWidget.jstree(true).select_node(
                        $scope.jstreeWidget.jstree(true).create_node(
                            parent,
                            {
                                text: name,
                                type: 'folder',
                                data: {
                                    path: (path.endsWith('/') ? path : path + '/') + name,
                                    workspace: workspace,
                                }
                            },
                        )
                    );
                } else {
                    messageHub.showAlertError('Could not create a folder', `There was an error while creating '${name}'`);
                }
            });
        }

        function executeAction(workspace, project, name) {
            actionsApi.executeAction(workspace, project, name).then(function (response) {
                if (response.status === 200) {
                    messageHub.showAlertInfo('Execute action', `Action '${name}' executed successfully`);
                } else {
                    messageHub.showAlertError('Execute action', `There was an error while executing action '${name}'`);
                }
            });
        }

        function openNewFileDialog(excluded, value) {
            messageHub.showFormDialog(
                'projectsNewFileForm',
                'Create a new file',
                [{
                    id: 'fdti1',
                    type: 'input',
                    submitOnEnterId: 'b1',
                    label: 'Name',
                    required: true,
                    inputRules: {
                        excluded: excluded ? excluded : getChildrenNames($scope.newNodeData.parent, 'file'),
                        patterns: ['^[^/:]*$'],
                    },
                    value: value ? value : '',
                }],
                [{
                    id: 'b1',
                    type: 'emphasized',
                    label: 'Create',
                    whenValid: true
                }, {
                    id: 'b2',
                    type: 'transparent',
                    label: 'Cancel',
                }],
                'projects.formDialog.create.file',
                'Creating...'
            );
            const handler = messageHub.onDidReceiveMessage(
                'projects.formDialog.create.file',
                function (msg) {
                    if (msg.data.buttonId === 'b1') {
                        createFile($scope.newNodeData.parent, msg.data.formData[0].value, $scope.newNodeData.workspace, $scope.newNodeData.path, $scope.newNodeData.content);
                        $scope.newNodeData.content = '';
                    }
                    messageHub.hideFormDialog('projectsNewFileForm');
                    messageHub.unsubscribe(handler);
                },
                true
            );
        }

        function openRenameDialog() {
            messageHub.showFormDialog(
                'projectsRenameForm',
                `Rename ${$scope.renameNodeData.type}`,
                [{
                    id: 'fdti1',
                    type: 'input',
                    submitOnEnterId: 'b1',
                    label: 'Name',
                    required: true,
                    inputRules: {
                        excluded: getChildrenNames($scope.renameNodeData.parent, 'file'),
                        patterns: ['^[^/:]*$'],
                    },
                    value: $scope.renameNodeData.text,
                }],
                [{
                    id: 'b1',
                    type: 'emphasized',
                    label: 'Rename',
                    whenValid: true
                }, {
                    id: 'b2',
                    type: 'transparent',
                    label: 'Cancel',
                }],
                'projects.formDialog.rename',
                'Renaming...'
            );
            const handler = messageHub.onDidReceiveMessage(
                'projects.formDialog.rename',
                function (msg) {
                    if (msg.data.buttonId === 'b1') {
                        workspaceApi.rename(
                            $scope.renameNodeData.text,
                            msg.data.formData[0].value,
                            $scope.renameNodeData.data.path.substring($scope.renameNodeData.data.path.length - $scope.renameNodeData.text.length, 0),
                            $scope.renameNodeData.data.workspace
                        ).then(function (response) {
                            if (response.status === 200) {
                                const newPath = `/${response.data[0].to}`;
                                let node = $scope.jstreeWidget.jstree(true).get_node($scope.renameNodeData);
                                if ($scope.renameNodeData.type === 'file') {
                                    workspaceApi.getMetadataByPath($scope.renameNodeData.data.workspace, newPath).then(function (metadata) {
                                        if (metadata.status === 200) {
                                            node.text = metadata.data.name;
                                            node.data.path = metadata.data.path.substring($scope.selectedWorkspace.name.length + 1, metadata.data.path.length);
                                            node.data.contentType = metadata.data.contentType;
                                            node.state.status = metadata.data.status;
                                            node.icon = getFileIcon(metadata.data.name);
                                            messageHub.announceFileRenamed({
                                                oldName: $scope.renameNodeData.text,
                                                name: node.text,
                                                oldPath: $scope.renameNodeData.data.path,
                                                path: node.data.path,
                                                contentType: node.data.contentType,
                                                workspace: node.data.workspace,
                                            });
                                            $scope.jstreeWidget.jstree(true).redraw_node(node);
                                        } else {
                                            messageHub.setStatusError(`Unable to rename '${$scope.renameNodeData.text}'.`);
                                        }
                                    });
                                } else {
                                    messageHub.getCurrentlyOpenedFiles(`/${$scope.renameNodeData.data.workspace}${$scope.renameNodeData.data.path}`).then(function (result) {
                                        for (let i = 0; i < result.data.files.length; i++) {
                                            messageHub.announceFileMoved({
                                                name: result.data.files[i].substring(result.data.files[i].lastIndexOf('/') + 1, result.data.files[i].length),
                                                path: result.data.files[i].replace(`/${$scope.renameNodeData.data.workspace}`, '').replace($scope.renameNodeData.data.path, newPath),
                                                oldPath: result.data.files[i].replace(`/${$scope.renameNodeData.data.workspace}`, ''),
                                                workspace: $scope.renameNodeData.data.workspace,
                                            });
                                        }
                                    });
                                    for (let i = 0; i < $scope.renameNodeData.children_d.length; i++) {
                                        let child = $scope.jstreeWidget.jstree(true).get_node($scope.renameNodeData.children_d[i]);
                                        child.data.path = newPath + child.data.path.substring($scope.renameNodeData.data.path.length);
                                    }
                                    node.text = msg.data.formData[0].value;
                                    node.data.path = newPath;
                                    $scope.jstreeWidget.jstree(true).redraw_node(node);
                                }
                            } else messageHub.setStatusError(`Unable to rename '${$scope.renameNodeData.text}'.`);
                            messageHub.hideFormDialog('projectsRenameForm');
                        });
                    } else messageHub.hideFormDialog('projectsRenameForm');
                    messageHub.unsubscribe(handler);
                },
                true
            );
        }

        function openDeleteDialog(selected) {
            const isMultiple = Array.isArray(selected);
            messageHub.showDialogAsync(
                (isMultiple) ? `Delete ${selected.length} items?` : `Delete '${selected.text}'?`,
                'This action cannot be undone. It is recommended that you unpublish and delete.',
                [{
                    id: 'b1',
                    type: 'negative',
                    label: 'Delete',
                }, {
                    id: 'b2',
                    type: 'emphasized',
                    label: 'Delete & Unpublish',
                }, {
                    id: 'b3',
                    type: 'transparent',
                    label: 'Cancel',
                }],
            ).then(function (dialogResponse) {
                function deleteNode(node) {
                    if (node.type === 'project') {
                        deleteProject(node.data.workspace, node.text, node.id);
                    } else {
                        deleteFileFolder(node.data.workspace, node.data.path, node.id);
                    }
                };
                if (dialogResponse.data === 'b1') {
                    if (isMultiple) {
                        for (let i = 0; i < selected.length; i++) {
                            let node = $scope.jstreeWidget.jstree(true).get_node(selected[i]);
                            deleteNode(node);
                        }
                    } else {
                        deleteNode(selected);
                    }
                } else if (dialogResponse.data === 'b2') {
                    if (isMultiple) {
                        for (let i = 0; i < selected.length; i++) {
                            let node = $scope.jstreeWidget.jstree(true).get_node(selected[i]);
                            unpublish(node.data.path, node.data.workspace, node, function () {
                                deleteNode(node);
                            });
                        }
                    } else {
                        unpublish(selected.data.path, selected.data.workspace, selected, function () {
                            deleteNode(selected);
                        });
                    }
                }
            });
        }

        function getGenericTemplates() {
            const templateItems = [];
            for (let i = 0; i < $scope.genericTemplates.length; i++) {
                templateItems.push({
                    label: $scope.genericTemplates[i].name,
                    value: $scope.genericTemplates[i].id,
                });
            }
            return templateItems;
        }

        function getModelTemplates(extension) {
            const templateItems = [];
            for (let i = 0; i < $scope.modelTemplates.length; i++) {
                if ($scope.modelTemplates[i].extension === extension) {
                    templateItems.push({
                        label: $scope.modelTemplates[i].name,
                        value: $scope.modelTemplates[i].id,
                    });
                }
            }
            return templateItems;
        }

        function generateModel(isRegenerating = false) {
            generateApi.generateFromModel(
                $scope.selectedWorkspace.name,
                $scope.gmodel.project,
                $scope.gmodel.model,
                $scope.gmodel.templateId,
                $scope.gmodel.parameters
            ).then(function (response) {
                if (isRegenerating) messageHub.hideLoadingDialog('projectsRegenerateModel');
                else messageHub.hideFormDialog('projectGenerateForm2');
                if (response.status !== 201) {
                    messageHub.showAlertError(
                        'Failed to generate from model',
                        `An unexpected error has occurred while trying generate from model '${$scope.gmodel.model}'`
                    );
                    messageHub.setStatusError(`Unable to generate from model '${$scope.gmodel.model}'`);
                } else {
                    messageHub.setStatusMessage(`Generated from model '${$scope.gmodel.model}'`);
                }
                if (isRegenerating && $scope.gmodel.nodeParents.length) {
                    $scope.jstreeWidget.jstree(true).refresh_node(getProjectNode($scope.gmodel.nodeParents));
                } else $scope.reloadWorkspace();
                for (let key in $scope.gmodel.parameters) {
                    delete $scope.gmodel.parameters[key];
                }
                $scope.gmodel.model = '';
                $scope.gmodel.project = '';
                $scope.gmodel.templateId = '';
            });
        }

        messageHub.onFileSaved(function (data) {
            const { topic, ...fileDescriptor } = data;
            publish(fileDescriptor.path, fileDescriptor.workspace, fileDescriptor);
            if (data.status) {
                const instance = $scope.jstreeWidget.jstree(true);
                for (let item in instance._model.data) { // Uses the unofficial '_model' property but this is A LOT faster then using 'get_json()'
                    if (item !== '#' && instance._model.data[item].data.path === fileDescriptor.path) {
                        for (let i = 0; i < instance._model.data[item].parents.length; i++) {
                            if (instance._model.data[item].parents[i] !== '#') {
                                if (instance._model.data[instance._model.data[item].parents[i]].type === 'project') {
                                    if (instance._model.data[instance._model.data[item].parents[i]].li_attr.git) {
                                        if (data.status === 'modified')
                                            instance._model.data[item].state.status = 'M';
                                        else instance._model.data[item].state.status = undefined;
                                        instance.redraw_node(instance._model.data[item], false, false, false, data.status === 'modified');
                                    }
                                    return;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        });

        messageHub.onWorkspaceChanged(function (workspace) {
            if (workspace.data.name === $scope.selectedWorkspace.name) {
                if (workspace.data.projectsViewId) {
                    $scope.jstreeWidget.jstree(true).refresh_node(workspace.data.projectsViewId);
                } else $scope.reloadWorkspace();
            }
            if (workspace.data.publish) {
                if (workspace.data.publish.workspace) {
                    publish(`/${workspace.data.name}/*`);
                } else if (workspace.data.publish.path) {
                    publish(workspace.data.publish.path, workspace.data.name);
                }
            }
        });

        messageHub.onDidReceiveMessage(
            'projects.tree.refresh',
            function (msg) {
                if (msg.data.workspace === $scope.selectedWorkspace.name) {
                    if (msg.data.partial) {
                        const instance = $scope.jstreeWidget.jstree(true);
                        for (let item in instance._model.data) {
                            if (item !== '#' && instance._model.data[item].data.path === `/${msg.data.project}`) {
                                instance.refresh_node(item);
                            }
                        }
                    } else $scope.reloadWorkspace();
                }
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'projects.export.all',
            function () {
                $scope.exportProjects();
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'projects.tree.select',
            function (msg) {
                if (msg.data.filePath.startsWith(`/${$scope.selectedWorkspace.name}/`)) {
                    let filePath = msg.data.filePath.slice($scope.selectedWorkspace.name.length + 1);
                    const instance = $scope.jstreeWidget.jstree(true);
                    for (let item in instance._model.data) { // Uses the unofficial '_model' property but this is A LOT faster then using 'get_json()'
                        if (item !== '#' && instance._model.data[item].data.path === filePath) {
                            instance.deselect_all();
                            instance._open_to(item).focus();
                            instance.select_node(item);
                            const objNode = instance.get_node(item, true);
                            objNode[0].scrollIntoView({ behavior: 'smooth', block: 'center' });
                            break;
                        }
                    }
                }
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'projects.generate.model',
            function (msg) {
                if (msg.data.buttonId === 'b1') {
                    if ($scope.gmodel.model === '') {
                        $scope.gmodel.templateId = msg.data.formData[0].value;
                        $scope.gmodel.project = msg.data.formData[1].value;
                        $scope.gmodel.model = msg.data.formData[2].value;
                        let formData = [];
                        for (let i = 0; i < $scope.modelTemplates.length; i++) {
                            if ($scope.modelTemplates[i].id === $scope.gmodel.templateId) {
                                for (let j = 0; j < $scope.modelTemplates[i].parameters.length; j++) {
                                    let formItem = {
                                        id: $scope.modelTemplates[i].parameters[j].name,
                                        type: ($scope.modelTemplates[i].parameters[j].type === 'checkbox' ? 'checkbox' : 'input'),
                                        label: $scope.modelTemplates[i].parameters[j].label,
                                        required: $scope.modelTemplates[i].parameters[j].required ?? true,
                                        placeholder: $scope.modelTemplates[i].parameters[j].placeholder,
                                        value: $scope.modelTemplates[i].parameters[j].value || ($scope.modelTemplates[i].parameters[j].type === 'checkbox' ? false : ''),
                                    };
                                    if ($scope.modelTemplates[i].parameters[j].hasOwnProperty('ui')) {
                                        if ($scope.modelTemplates[i].parameters[j].ui.hasOwnProperty('hide')) {
                                            formItem.visibility = {
                                                hidden: false,
                                                id: $scope.modelTemplates[i].parameters[j].ui.hide.property,
                                                value: $scope.modelTemplates[i].parameters[j].ui.hide.value,
                                            };
                                        } else {
                                            // TODO
                                        }
                                    }
                                    formData.push(formItem);
                                }
                                break;
                            }
                        }
                        if (formData.length > 0) {
                            messageHub.updateFormDialog(
                                'projectGenerateForm2',
                                formData,
                                'Generating...',
                            );
                        } else {
                            generateModel();
                        }
                    } else {
                        $scope.gmodel.parameters = {};
                        for (let i = 0; i < msg.data.formData.length; i++) {
                            if (msg.data.formData[i].value)
                                $scope.gmodel.parameters[msg.data.formData[i].id] = msg.data.formData[i].value;
                        }
                        generateModel();
                    }
                } else {
                    messageHub.hideFormDialog('projectGenerateForm2');
                    $scope.gmodel.model = '';
                }
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'projects.tree.contextmenu',
            function (msg) {
                if (msg.data.itemId === 'open') {
                    if (msg.data.data) openFile(msg.data.data);
                    else openSelected();
                } else if (msg.data.itemId === 'openWith') {
                    openFile(msg.data.data.node, msg.data.data.editorId);
                } else if (msg.data.itemId === 'file') {
                    $scope.newNodeData.parent = msg.data.data.parent;
                    $scope.newNodeData.workspace = msg.data.data.workspace;
                    $scope.newNodeData.path = msg.data.data.path;
                    $scope.newNodeData.content = '';
                    openNewFileDialog();
                } else if (msg.data.itemId === 'folder') {
                    $scope.newNodeData.parent = msg.data.data.parent;
                    $scope.newNodeData.workspace = msg.data.data.workspace;
                    $scope.newNodeData.path = msg.data.data.path;
                    messageHub.showFormDialog(
                        'projectsNewFolderForm',
                        'Create new folder',
                        [{
                            id: 'fdti1',
                            type: 'input',
                            submitOnEnterId: 'b1',
                            label: 'Name',
                            required: true,
                            inputRules: {
                                excluded: getChildrenNames(msg.data.data.parent, 'folder'),
                                patterns: ['^[^/:]*$'],
                            },
                            value: '',
                        }],
                        [{
                            id: 'b1',
                            type: 'emphasized',
                            label: 'Create',
                            whenValid: true
                        },
                        {
                            id: 'b2',
                            type: 'transparent',
                            label: 'Cancel',
                        }],
                        'projects.formDialog.create.folder',
                        'Creating...'
                    );
                    const handler = messageHub.onDidReceiveMessage(
                        'projects.formDialog.create.folder',
                        function (resp) {
                            if (resp.data.buttonId === 'b1') {
                                createFolder($scope.newNodeData.parent, resp.data.formData[0].value, $scope.newNodeData.workspace, $scope.newNodeData.path);
                            }
                            messageHub.hideFormDialog('projectsNewFolderForm');
                            messageHub.unsubscribe(handler);
                        },
                        true
                    );
                } else if (msg.data.itemId === 'rename') {
                    $scope.renameNodeData = msg.data.data;
                    openRenameDialog();
                } else if (msg.data.itemId === 'delete') {
                    openDeleteDialog(msg.data.data)
                } else if (msg.data.itemId === 'cut') {
                    $scope.jstreeWidget.jstree(true).cut($scope.jstreeWidget.jstree(true).get_top_selected(true));
                } else if (msg.data.itemId === 'copy') {
                    $scope.jstreeWidget.jstree(true).copy($scope.jstreeWidget.jstree(true).get_top_selected(true));
                } else if (msg.data.itemId === 'paste') {
                    $scope.jstreeWidget.jstree(true).paste(msg.data.data);
                } else if (msg.data.itemId === 'newProject') {
                    $scope.createProject();
                } else if (msg.data.itemId === 'duplicateProject') {
                    $scope.duplicateProject(msg.data.data);
                } else if (msg.data.itemId === 'exportProjects') {
                    $scope.exportProjects();
                } else if (msg.data.itemId === 'exportProject') {
                    transportApi.exportProject(msg.data.data.data.workspace, msg.data.data.text);
                } else if (msg.data.itemId === 'actionsProject') {
                    $scope.actionData.workspace = msg.data.data.data.workspace;
                    $scope.actionData.project = msg.data.data.text;
                    messageHub.showFormDialog(
                        'projectsExecuteActionForm',
                        'Enter the action to execute',
                        [{
                            id: 'fdti1',
                            type: 'input',
                            submitOnEnterId: 'b1',
                            label: 'Name',
                            required: true,
                            inputRules: '',
                            value: '',
                        }],
                        [{
                            id: 'b1',
                            type: 'emphasized',
                            label: 'Execute',
                            whenValid: true
                        },
                        {
                            id: 'b2',
                            type: 'transparent',
                            label: 'Cancel',
                        }],
                        'projects.formDialog.action.execute',
                        'Executing...'
                    );
                    const handler = messageHub.onDidReceiveMessage(
                        'projects.formDialog.action.execute',
                        function (resp) {
                            if (resp.data.buttonId === 'b1') {
                                executeAction($scope.actionData.workspace, $scope.actionData.project, resp.data.formData[0].value);
                            }
                            messageHub.hideFormDialog('projectsExecuteActionForm');
                            messageHub.unsubscribe(handler);
                        },
                        true
                    );
                } else if (msg.data.itemId === 'import') {
                    messageHub.showDialogWindow(
                        'import',
                        {
                            importType: 'file',
                            uploadPath: msg.data.data.data.path,
                            workspace: msg.data.data.data.workspace,
                            projectsViewId: msg.data.data.id
                        }
                    );
                } else if (msg.data.itemId === 'importZip') {
                    messageHub.showDialogWindow(
                        'import',
                        {
                            uploadPath: msg.data.data.data.path,
                            workspace: msg.data.data.data.workspace,
                            projectsViewId: msg.data.data.id
                        }
                    );
                } else if (msg.data.itemId === 'unpublishAll') {
                    unpublishAll();
                } else if (msg.data.itemId === 'publishAll') {
                    $scope.publishAll();
                } else if (msg.data.itemId === 'publish') {
                    const selected = $scope.jstreeWidget.jstree(true).get_top_selected(true);
                    for (let i = 0; i < selected.length; i++) {
                        publish(selected[i].data.path, selected[i].data.workspace, {
                            name: selected[i].text,
                            path: selected[i].data.path,
                            type: selected[i].type,
                            workspace: selected[i].data.workspace
                        });
                    }
                } else if (msg.data.itemId === 'unpublish') {
                    const selected = $scope.jstreeWidget.jstree(true).get_top_selected(true);
                    for (let i = 0; i < selected.length; i++) {
                        unpublish(selected[i].data.path, selected[i].data.workspace, {
                            name: selected[i].text,
                            path: selected[i].data.path,
                            type: selected[i].type,
                            workspace: selected[i].data.workspace
                        });
                    }
                } else if (msg.data.itemId === 'regenerateModel') {
                    messageHub.showLoadingDialog('projectsRegenerateModel', 'Regenerating', 'Loading data');
                    workspaceApi.loadContent(msg.data.data.data.workspace, msg.data.data.data.path).then(function (response) {
                        if (response.status === 200) {
                            $scope.gmodel.nodeParents = msg.data.data.parents;
                            $scope.gmodel.project = response.data.projectName;
                            $scope.gmodel.model = response.data.filePath;
                            let { models, perspectives, templateId, filePath, workspaceName, projectName, ...params } = response.data;
                            $scope.gmodel.parameters = params;
                            if (!response.data.templateId) {
                                messageHub.hideLoadingDialog('projectsRegenerateModel');
                                messageHub.showFormDialog(
                                    'projectRegenerateChooseTemplate',
                                    'Choose template',
                                    [{
                                        id: 'pgfd1',
                                        type: 'dropdown',
                                        label: 'Choose template',
                                        required: true,
                                        value: '',
                                        items: getModelTemplates('model'),
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
                                    'projects.regenerate.template',
                                    'Setting template...',
                                );
                                const handler = messageHub.onDidReceiveMessage(
                                    'projects.regenerate.template',
                                    function (response) {
                                        if (response.data.buttonId === 'b1') {
                                            $scope.gmodel.templateId = response.data.formData[0].value;
                                            messageHub.hideFormDialog('projectRegenerateChooseTemplate');
                                            messageHub.showLoadingDialog('projectsRegenerateModel', 'Regenerating', 'Regenerating from model');
                                            generateModel(true);
                                        } else messageHub.hideFormDialog('projectRegenerateChooseTemplate');
                                        messageHub.unsubscribe(handler);
                                    },
                                    true
                                );
                            } else {
                                $scope.gmodel.templateId = response.data.templateId;
                                messageHub.updateLoadingDialog('projectsRegenerateModel', 'Regenerating from model');
                                generateModel(true);
                            }
                        } else {
                            messageHub.hideLoadingDialog('projectsRegenerateModel');
                            messageHub.showAlertError('Unable to load file', 'There was an error while loading the file. See the log for more information.');
                            console.error(response);
                        }
                    });
                } else if (msg.data.itemId.startsWith('generate')) {
                    let project;
                    let projectNames = [];
                    let root = $scope.jstreeWidget.jstree(true).get_node('#');
                    for (let i = 0; i < root.children.length; i++) {
                        let name = $scope.jstreeWidget.jstree(true).get_text(root.children[i])
                        projectNames.push({
                            label: name,
                            value: name,
                        });
                    }
                    if (msg.data.itemId === 'generateGeneric') {
                        let generatePath;
                        const nodeParents = msg.data.data.parents;
                        const templateItems = getGenericTemplates();
                        if (msg.data.data.type !== 'project') {
                            const pnode = getProjectNode(msg.data.data.parents);
                            project = pnode.text;
                            generatePath = msg.data.data.data.path.substring(pnode.text.length + 1);
                            if (generatePath.endsWith('/')) generatePath += 'filename';
                            else generatePath += '/filename';
                        } else {
                            project = msg.data.data.text;
                            generatePath = '/filename';
                        }
                        messageHub.showFormDialog(
                            'projectGenerateForm1',
                            'Generate from template',
                            [{
                                id: 'pgfd1',
                                type: 'dropdown',
                                label: 'Choose template',
                                required: true,
                                value: '',
                                items: templateItems,
                            }, {
                                id: 'pgfd2',
                                type: 'dropdown',
                                label: 'Choose project',
                                required: true,
                                value: project,
                                items: projectNames,
                            }, {
                                id: 'pgfi1',
                                type: 'input',
                                submitOnEnterId: 'b1',
                                label: 'File path in project',
                                required: true,
                                placeholder: '/path/file',
                                value: generatePath,
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
                            'projects.generate.generic',
                            'Generating...',
                        );
                        const handler = messageHub.onDidReceiveMessage(
                            'projects.generate.generic',
                            function (response) {
                                if (response.data.buttonId === 'b1') {
                                    let template;
                                    for (let i = 0; i < $scope.genericTemplates.length; i++) {
                                        if ($scope.genericTemplates[i].id === response.data.formData[0].value) {
                                            template = $scope.genericTemplates[i];
                                            break;
                                        }
                                    }
                                    console.log($scope.selectedWorkspace.name,
                                        response.data.formData[1].value,
                                        response.data.formData[2].value,
                                        template.id,
                                        template.parameters);
                                    generateApi.generateFromTemplate(
                                        $scope.selectedWorkspace.name,
                                        response.data.formData[1].value,
                                        response.data.formData[2].value,
                                        template.id,
                                        template.parameters
                                    ).then(function (response) {
                                        if (response.status === 201) {
                                            messageHub.setStatusMessage('Successfully generated from template.');
                                            $scope.jstreeWidget.jstree(true).refresh_node(getProjectNode(nodeParents));
                                        } else {
                                            messageHub.showAlertError(
                                                'Failed to generate from template',
                                                `An unexpected error has occurred while trying generate from template '${template.name}'`
                                            );
                                            messageHub.setStatusError(`Unable to generate from template '${template.name}'`);
                                        }
                                    });
                                    messageHub.hideFormDialog('projectGenerateForm1');
                                } else messageHub.hideFormDialog('projectGenerateForm1');
                                messageHub.unsubscribe(handler);
                            },
                            true
                        );
                    } else if (msg.data.itemId === 'generateModel') {
                        let pnode = getProjectNode(msg.data.data.parents);
                        project = pnode.text;
                        const templateItems = getModelTemplates(getFileExtension(msg.data.data.text))
                        messageHub.showFormDialog(
                            'projectGenerateForm2',
                            'Generate from template',
                            [{
                                id: 'pgfd1',
                                type: 'dropdown',
                                label: 'Choose template',
                                required: true,
                                value: '',
                                items: templateItems,
                            }, {
                                id: 'pgfd2',
                                type: 'dropdown',
                                label: 'Choose project',
                                required: true,
                                value: project,
                                items: projectNames,
                            }, {
                                id: 'pgfi1',
                                type: 'input',
                                submitOnEnterId: 'b1',
                                label: 'Model (must be in the root of the project)',
                                required: true,
                                inputRules: {
                                    // excluded: [], //TODO
                                    patterns: ['^[^/:]*$'],
                                },
                                placeholder: 'file.model',
                                value: msg.data.data.data.path.substring(project.length + 2),
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
                            'projects.generate.model',
                            'Loading parameters...',
                        );
                    }
                } else {
                    if (msg.data.data.nameless) {
                        createFile(
                            msg.data.data.parent,
                            `.${msg.data.data.extension}`,
                            msg.data.data.workspace,
                            msg.data.data.path,
                            msg.data.data.content
                        );
                    } else {
                        let name;
                        let excludedNames = getChildrenNames(msg.data.data.parent, 'file');
                        let i = 1;
                        if (msg.data.data.extension) {
                            name = `${msg.data.data.name}.${msg.data.data.extension}`;
                            while (excludedNames.includes(name)) {
                                name = `${msg.data.data.name} ${i++}.${msg.data.data.extension}`;
                            }
                        } else {
                            name = msg.data.data.name;
                            while (excludedNames.includes(name)) {
                                name = `${msg.data.data.name} ${i++}`;
                            }
                        }
                        if (msg.data.data.staticName) {
                            createFile(
                                msg.data.data.parent,
                                name,
                                msg.data.data.workspace,
                                msg.data.data.path,
                                msg.data.data.content
                            );
                        } else {
                            $scope.newNodeData.parent = msg.data.data.parent;
                            $scope.newNodeData.workspace = msg.data.data.workspace;
                            $scope.newNodeData.path = msg.data.data.path;
                            $scope.newNodeData.content = msg.data.data.content;
                            openNewFileDialog(excludedNames, name);
                        }
                    }
                }
            },
            true
        );

        angular.element($document[0]).ready(function () {
            $scope.reloadWorkspaceList();
        });
    }]);
