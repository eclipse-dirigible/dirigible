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
const gitProjectsView = angular.module('gitProjects', ['ideUI', 'ideView', 'ideWorkspace', 'ideGit']);

gitProjectsView.controller('GitProjectsViewController', [
    '$scope',
    'messageHub',
    'workspaceApi',
    'gitApi',
    function (
        $scope,
        messageHub,
        workspaceApi,
        gitApi,
    ) {
        $scope.selectedRepository = {
            name: '',
            fetchUrl: '',
            pushUrl: '',
        };
        $scope.credentials = {
            username: '',
            email: '',
            password: '',
        };
        $scope.searchVisible = false;
        $scope.searchField = { text: '' };
        $scope.workspaceNames = [];
        $scope.imageFileExts = ['ico', 'bmp', 'png', 'jpg', 'jpeg', 'gif', 'svg'];
        $scope.modelFileExts = ['extension', 'edm', 'model', 'dsm', 'schema', 'bpmn', 'job', 'listener', 'websocket', 'roles', 'constraints', 'table', 'view'];

        $scope.selectedWorkspace = JSON.parse(localStorage.getItem('DIRIGIBLE.workspace') || '{}');
        if (!$scope.selectedWorkspace.name) {
            $scope.selectedWorkspace = { name: 'workspace' }; // Default
            saveSelectedWorkspace();
        }

        $scope.projects = [];

        $scope.jstreeWidget = angular.element('#dgProjects');
        $scope.spinnerObj = {
            text: "Loading...",
            type: "spinner",
            li_attr: { spinner: true },
        };
        $scope.jstreeConfig = {
            core: {
                check_callback: true,
                multiple: false,
                themes: {
                    name: "fiori",
                    variant: "compact",
                },
                data: function (node, cb) {
                    cb($scope.projects);
                },
            },
            search: {
                case_sensitive: false,
            },
            plugins: ["wholerow", "search", "state", "types", "indicator"],
            dnd: {
                large_drop_target: true,
                large_drag_target: true,
                is_draggable: function (nodes) {
                    for (let i = 0; i < nodes.length; i++) {
                        if (nodes[i].type === 'project') return false;
                    }
                    return true;
                },
            },
            state: { key: 'ide-git-projects' },
            types: {
                '#': {
                    valid_children: ["project"]
                },
                "default": {
                    icon: "sap-icon--question-mark",
                    valid_children: [],
                },
                file: {
                    icon: "jstree-file",
                    valid_children: [],
                },
                folder: {
                    icon: "jstree-folder",
                    valid_children: ['folder', 'file', 'spinner'],
                },
                project: {
                    icon: "jstree-project",
                    valid_children: ['folder', 'file', 'spinner'],
                },
                spinner: {
                    icon: "jstree-spinner",
                    valid_children: [],
                },
            },
        };

        function getProjectNode(parents) {
            for (let i = 0; i < parents.length; i++) {
                if (parents[i] !== '#') {
                    let parent = $scope.jstreeWidget.jstree(true).get_node(parents[i]);
                    if (parent.type === 'project') {
                        return parent;
                    }
                }
            }
        }

        $scope.jstreeWidget.on('select_node.jstree', function (event, data) {
            if (data.event && data.event.type === 'click') {
                let project;
                let isGit = false;
                if (data.node.type === 'project') {
                    project = data.node.text;
                    isGit = data.node.data.git;
                } else {
                    projectNode = getProjectNode(data.node.parents);
                    project = projectNode.text;
                    isGit = projectNode.data.git;
                }
                messageHub.postMessage(
                    'git.repository.selected',
                    {
                        workspace: $scope.selectedWorkspace.name,
                        project: project,
                        isGitProject: isGit,
                    }
                );
            }
        });

        $scope.contextMenuContent = function (element) {
            if ($scope.jstreeWidget[0].contains(element)) {
                let id;
                if (element.tagName !== "LI") {
                    let closest = element.closest("li");
                    if (closest) id = closest.id;
                    else return {
                        callbackTopic: "git-projects.tree.contextmenu",
                        hasIcons: true,
                        items: [{
                            id: "refresh",
                            label: "Refresh",
                            icon: "sap-icon--refresh",
                        },
                        {
                            id: "clone",
                            label: "Clone",
                            icon: "sap-icon--duplicate",
                            divider: true,
                        }]
                    }
                } else {
                    id = element.id;
                }
                if (id) {
                    let node = $scope.jstreeWidget.jstree(true).get_node(id);
                    if (node.type === 'project') {
                        if (node.data.git) {
                            return {
                                callbackTopic: 'git-projects.tree.contextmenu',
                                hasIcons: true,
                                items: [{
                                    id: "pull",
                                    label: "Pull",
                                    icon: "sap-icon--download",
                                    data: node.text,
                                },
                                {
                                    id: "push",
                                    label: "Push",
                                    icon: "sap-icon--upload",
                                    data: node.text,
                                },
                                {
                                    id: "reset",
                                    label: "Reset",
                                    icon: "sap-icon--reset",
                                    data: node.text,
                                },
                                {
                                    id: "editUrls",
                                    label: "Edit URLs",
                                    divider: true,
                                    icon: "sap-icon--edit",
                                    data: node.text,
                                },
                                {
                                    id: "importProjects",
                                    label: "Import Project(s)",
                                    divider: true,
                                    icon: "sap-icon--sys-add",
                                    data: node.text,
                                },
                                {
                                    id: "delete",
                                    label: "Delete",
                                    divider: true,
                                    icon: "sap-icon--delete",
                                    data: node,
                                }]
                            };
                        } else {
                            return {
                                callbackTopic: 'git-projects.tree.contextmenu',
                                hasIcons: true,
                                items: [{
                                    id: "share",
                                    label: "Share",
                                    icon: "sap-icon--share",
                                    data: node.text,
                                }]
                            };
                        }
                    }
                    // else if (node.type === "folder") {
                    // } else if (node.type === "file") {}
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
                if (response.status === 200)
                    $scope.workspaceNames = response.data;
                else messageHub.setStatusError('Unable to load workspace list');
            });
        };

        $scope.reloadProjects = function (setConfig = false) {
            $scope.projects.length = 0;
            gitApi.listProjects($scope.selectedWorkspace.name).then(function (response) {
                if (response.status === 200) {
                    for (let i = 0; i < response.data.length; i++) {
                        let project = {
                            text: response.data[i].name,
                            type: response.data[i].type,
                            data: {
                                git: response.data[i].git,
                                path: response.data[i].path,
                            },
                            li_attr: { git: response.data[i].git },
                        };
                        if (response.data[i].folders && response.data[i].files) {
                            project['children'] = processChildren(response.data[i].folders.concat(response.data[i].files));
                        } else if (response.data[i].folders) {
                            project['children'] = processChildren(response.data[i].folders);
                        } else if (response.data[i].files) {
                            project['children'] = processChildren(response.data[i].files);
                        }
                        $scope.projects.push(project);
                    }
                    if (setConfig) $scope.jstreeWidget.jstree($scope.jstreeConfig);
                    else $scope.jstreeWidget.jstree(true).refresh();
                } else {
                    messageHub.setStatusError('Unable to load workspace data');
                }
            });
        };

        $scope.switchWorkspace = function (workspace) {
            if ($scope.selectedWorkspace.name !== workspace) {
                $scope.selectedWorkspace.name = workspace;
                saveSelectedWorkspace();
                $scope.reloadProjects();
            }
        };

        $scope.cloneDialog = function () {
            messageHub.showFormDialog(
                'cloneGitProjectForm',
                'Clone project',
                [{
                    id: "curli",
                    type: "input",
                    label: "HTTPS Url",
                    inputRules: {
                        excluded: [],
                        patterns: ['^https?://'],
                    },
                    required: true,
                    placeholder: "https://github.com/myspace/myproject.git",
                },
                {
                    id: "cuni",
                    type: "input",
                    label: "Username",
                    autocomplete: "git-username",
                    value: $scope.credentials.username,
                },
                {
                    id: "cpwi",
                    type: "input",
                    inputType: 'password',
                    label: "Password",
                    autocomplete: "git-pass",
                    value: $scope.credentials.password,
                },
                {
                    id: "cbi",
                    type: "input",
                    label: "Branch",
                    placeholder: 'main',
                }],
                [{
                    id: 'b1',
                    type: 'emphasized',
                    label: 'Clone',
                    whenValid: true,
                },
                {
                    id: 'b2',
                    type: 'transparent',
                    label: 'Cancel',
                }],
                'git-projects.clone.repository',
                'Cloning...',
            );
        };

        $scope.pushDialog = function (multiple = false) {
            messageHub.showFormDialog(
                'pushGitProjectForm',
                (multiple ? 'Push all repositories' : `Push '${$scope.selectedRepository.name}'`),
                [{
                    id: "puni",
                    type: "input",
                    label: "Username",
                    required: true,
                    value: $scope.credentials.username,
                },
                {
                    id: "pei",
                    type: "input",
                    label: "Email",
                    required: true,
                    value: $scope.credentials.email,
                },
                {
                    id: "ppwi",
                    type: "input",
                    inputType: 'password',
                    label: "Password or Token",
                    required: true,
                    value: $scope.credentials.password,
                }],
                [{
                    id: 'b1',
                    type: 'emphasized',
                    label: 'Push',
                    whenValid: true,
                },
                {
                    id: 'b2',
                    type: 'transparent',
                    label: 'Cancel',
                }],
                (multiple ? 'git-projects.push.repository.all' : 'git-projects.push.repository'),
                'Pushing...',
            );
        };

        $scope.pullDialog = function (multiple = false) {
            messageHub.showFormDialog(
                'pullGitProjectForm',
                (multiple ? 'Pull all repositories' : `Pull '${$scope.selectedRepository.name}'`),
                [{
                    id: "puni",
                    type: "input",
                    label: "Username",
                    value: $scope.credentials.username,
                },
                {
                    id: "ppwi",
                    type: "input",
                    inputType: 'password',
                    label: "Password or Token",
                    value: $scope.credentials.password,
                }],
                [{
                    id: 'b1',
                    type: 'emphasized',
                    label: 'Pull',
                    whenValid: true,
                },
                {
                    id: 'b2',
                    type: 'transparent',
                    label: 'Cancel',
                }],
                (multiple ? 'git-projects.pull.repository.all' : 'git-projects.pull.repository'),
                'Pulling...',
            );
        };

        let to = 0;
        $scope.search = function () {
            if (to) { clearTimeout(to); }
            to = setTimeout(function () {
                $scope.jstreeWidget.jstree(true).search($scope.searchField.text);
            }, 250);
        };

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
                        path: children[i].path,
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

        function saveSelectedWorkspace() {
            localStorage.setItem('DIRIGIBLE.workspace', JSON.stringify($scope.selectedWorkspace));
        }

        messageHub.onWorkspaceChanged(function (workspace) {
            if (workspace.data.name === $scope.selectedWorkspace.name)
                $scope.reloadProjects();
            if (workspace.data.publish) {
                if (workspace.data.publish.workspace) {
                    $scope.publish(`/${workspace.data.name}/*`);
                } else if (workspace.data.publish.path) {
                    $scope.publish(workspace.data.publish.path, workspace.data.name);
                }
            }
        });

        messageHub.onDidReceiveMessage(
            'git-projects.push.repository.all',
            function (msg) {
                if (msg.data.buttonId === "b1") {
                    $scope.credentials.username = msg.data.formData[0].value;
                    $scope.credentials.email = msg.data.formData[1].value;
                    $scope.credentials.password = msg.data.formData[2].value;
                    gitApi.pushAllRepositories(
                        $scope.selectedWorkspace.name,
                        $scope.credentials.username,
                        $scope.credentials.email,
                        $scope.credentials.password,
                    ).then(function (response) {
                        if (response.status !== 200) {
                            messageHub.showAlertError('Could not push repositories', response.message);
                        }
                        messageHub.hideFormDialog('pushGitProjectForm');
                    });
                } else messageHub.hideFormDialog('pushGitProjectForm');
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'git-projects.push.repository',
            function (msg) {
                if (msg.data.buttonId === "b1") {
                    $scope.credentials.username = msg.data.formData[0].value;
                    $scope.credentials.email = msg.data.formData[1].value;
                    $scope.credentials.password = msg.data.formData[2].value;
                    gitApi.pushRepository(
                        $scope.selectedWorkspace.name,
                        $scope.selectedRepository.name,
                        '',
                        $scope.credentials.username,
                        $scope.credentials.email,
                        $scope.credentials.password,
                    ).then(function (response) {
                        if (response.status !== 200) {
                            messageHub.showAlertError('Could not push repository', response.message);
                        }
                        messageHub.hideFormDialog('pushGitProjectForm');
                    });
                } else messageHub.hideFormDialog('pushGitProjectForm');
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'git-projects.pull.repository.all',
            function (msg) {
                if (msg.data.buttonId === "b1") {
                    $scope.credentials.username = msg.data.formData[0].value;
                    $scope.credentials.password = msg.data.formData[1].value;
                    let projects = [];
                    for (let i = 0; i < $scope.projects.length; i++) {
                        projects.push($scope.projects[i].text);
                    }
                    gitApi.pullRepositories(
                        $scope.selectedWorkspace.name,
                        projects,
                        $scope.credentials.username,
                        $scope.credentials.password,
                        function (response) {
                            if (response.status !== 200) {
                                messageHub.showAlertError('Could not pull repository', response.message);
                            }
                            messageHub.hideFormDialog('pullGitProjectForm');
                        }
                    );
                } else messageHub.hideFormDialog('pullGitProjectForm');
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'git-projects.pull.repository',
            function (msg) {
                if (msg.data.buttonId === "b1") {
                    $scope.credentials.username = msg.data.formData[0].value;
                    $scope.credentials.password = msg.data.formData[1].value;
                    gitApi.pullRepository(
                        $scope.selectedWorkspace.name,
                        $scope.selectedRepository.name,
                        '',
                        $scope.credentials.username,
                        $scope.credentials.password,
                    ).then(function (response) {
                        if (response.status !== 200) {
                            messageHub.showAlertError('Could not push repository', response.message);
                        }
                        messageHub.hideFormDialog('pullGitProjectForm');
                    });
                } else messageHub.hideFormDialog('pullGitProjectForm');
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'git-projects.clone.repository',
            function (msg) {
                if (msg.data.buttonId === "b1") {
                    $scope.credentials.username = msg.data.formData[1].value;
                    $scope.credentials.password = msg.data.formData[2].value;
                    gitApi.cloneRepository(
                        $scope.selectedWorkspace.name,
                        msg.data.formData[0].value,
                        msg.data.formData[3].value,
                        $scope.credentials.username,
                        $scope.credentials.password,
                    ).then(function (response) {
                        if (response.status === 200) {
                            $scope.reloadProjects();
                        } else {
                            messageHub.showAlertError('Could not clone repository', response.message);
                        }
                        messageHub.hideFormDialog('cloneGitProjectForm');
                    });
                } else messageHub.hideFormDialog('cloneGitProjectForm');
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'git-projects.share.repository',
            function (msg) {
                if (msg.data.buttonId === "b1") {
                    $scope.credentials.username = msg.data.formData[3].value;
                    $scope.credentials.email = msg.data.formData[4].value;
                    $scope.credentials.password = msg.data.formData[5].value;
                    gitApi.shareProject(
                        $scope.selectedWorkspace.name,
                        $scope.selectedRepository.name,
                        msg.data.formData[0].value,
                        msg.data.formData[1].value,
                        msg.data.formData[2].value,
                        $scope.credentials.username,
                        $scope.credentials.password,
                        $scope.credentials.email,
                        msg.data.formData[6].value,
                    ).then(function (response) {
                        if (response.status === 200) $scope.reloadProjects();
                        else messageHub.showAlertError('Could not share repository', response.message);
                        messageHub.hideFormDialog('shareGitProjectForm');
                    });
                } else messageHub.hideFormDialog('shareGitProjectForm');
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'git-projects.edit.repository',
            function (msg) {
                if (msg.data.buttonId === "b1") {
                    if ($scope.selectedRepository.fetchUrl !== msg.data.formData[0].value) {
                        gitApi.setFetchUrl(
                            $scope.selectedWorkspace.name,
                            $scope.selectedRepository.name,
                            msg.data.formData[0].value
                        ).then(function (response) {
                            if (response.status !== 200) {
                                messageHub.showAlertError('Could not change fetch url', response.message);
                            } else messageHub.setStatusMessage('Successfully changed fetch url');
                        })
                    }
                    if ($scope.selectedRepository.pushUrl !== msg.data.formData[1].value) {
                        gitApi.setPushUrl(
                            $scope.selectedWorkspace.name,
                            $scope.selectedRepository.name,
                            msg.data.formData[1].value
                        ).then(function (response) {
                            if (response.status !== 200) {
                                messageHub.showAlertError('Could not change push url', response.message);
                            } else messageHub.setStatusMessage('Successfully changed push url');
                        })
                    }
                    messageHub.hideFormDialog('editUrlsGitProjectForm');
                } else messageHub.hideFormDialog('editUrlsGitProjectForm');
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'git-projects.tree.contextmenu',
            function (msg) {
                if (msg.data.itemId === 'refresh') {
                    $scope.reloadProjects()
                } else if (msg.data.itemId === 'clone') {
                    $scope.cloneDialog();
                } else if (msg.data.itemId === 'push') {
                    $scope.selectedRepository.name = msg.data.data;
                    $scope.pushDialog();
                } else if (msg.data.itemId === 'pull') {
                    $scope.selectedRepository.name = msg.data.data;
                    $scope.pullDialog();
                } else if (msg.data.itemId === 'reset') {
                    messageHub.showBusyDialog(
                        'gitProjectsResetBusyDialog',
                        'Resetting...',
                    );
                    gitApi.resetRepository($scope.selectedWorkspace.name, msg.data.data).then(function (response) {
                        if (response.status === 200) {
                            $scope.reloadProjects();
                        } else {
                            messageHub.showAlertError('Could not reset repository', response.message);
                        }
                        messageHub.hideBusyDialog('gitProjectsResetBusyDialog');
                    });
                } else if (msg.data.itemId === 'delete') {
                    messageHub.showDialogAsync(
                        `Delete '${msg.data.data.text}'?`,
                        'This action cannot be undone. It is recommended that you unpublish and delete.',
                        [{
                            id: 'b1',
                            type: 'negative',
                            label: 'Delete',
                        },
                        {
                            id: 'b2',
                            type: 'emphasized',
                            label: 'Delete & Unpublish',
                        },
                        {
                            id: 'b3',
                            type: 'transparent',
                            label: 'Cancel',
                        }],
                    ).then(function (dialogResponse) {
                        if (dialogResponse.data !== 'b3') {
                            let unpublishOnDelete = dialogResponse.data === 'b2';
                            gitApi.deleteRepository($scope.selectedWorkspace.name, msg.data.data.text, unpublishOnDelete).then(function (response) {
                                if (response.status === 200) {
                                    $scope.jstreeWidget.jstree(true).delete_node(msg.data.data);
                                } else {
                                    messageHub.showAlertError('Could not delete repository', response.message);
                                }
                            });
                        }
                    });
                } else if (msg.data.itemId === 'importProjects') {
                    messageHub.showDialogAsync(
                        `Import from '${msg.data.data}'?`,
                        `Import all projects from '${msg.data.data}'`,
                        [{
                            id: 'b1',
                            type: 'emphasized',
                            label: 'Import',
                        },
                        {
                            id: 'b2',
                            type: 'transparent',
                            label: 'Cancel',
                        }],
                    ).then(function (dialogResponse) {
                        if (dialogResponse.data !== 'b2') {
                            messageHub.showBusyDialog(
                                'gitProjectsImportBusyDialog',
                                'Importing...',
                            );
                            gitApi.importProjects($scope.selectedWorkspace.name, msg.data.data).then(function (response) {
                                if (response.status === 200) {
                                    messageHub.hideBusyDialog('gitProjectsImportBusyDialog');
                                    messageHub.showAlertSuccess('Successfully imported project(s)', 'You can now go to Workbench to see the imported project(s).');
                                } else {
                                    messageHub.showAlertError('Could not import from repository', response.message);
                                }
                            });
                        }
                    });
                } else if (msg.data.itemId === 'share') {
                    $scope.selectedRepository.name = msg.data.data;
                    messageHub.showFormDialog(
                        'shareGitProjectForm',
                        `Share '${$scope.selectedRepository.name}'`,
                        [{
                            id: "surli",
                            type: "input",
                            label: "Repository HTTPS Url",
                            inputRules: {
                                excluded: [],
                                patterns: ['^https?://'],
                            },
                            required: true,
                            placeholder: "https://github.com/myspace/myproject.git",
                        },
                        {
                            id: "sbi",
                            type: "input",
                            label: "Branch",
                            required: true,
                            placeholder: "main",
                        },
                        {
                            id: "smi",
                            type: "input",
                            label: "Commit message",
                            required: true,
                        },
                        {
                            id: "suni",
                            type: "input",
                            label: "Username",
                            required: true,
                            value: $scope.credentials.username,
                        },
                        {
                            id: "sei",
                            type: "input",
                            label: "Email",
                            required: true,
                            value: $scope.credentials.email,
                        },
                        {
                            id: "spwi",
                            type: "input",
                            inputType: 'password',
                            label: "Password or Token",
                            required: true,
                            value: $scope.credentials.password,
                        },
                        {
                            id: "srpc",
                            type: "checkbox",
                            label: "Root project",
                            value: false,
                        }],
                        [{
                            id: 'b1',
                            type: 'emphasized',
                            label: 'Share',
                            whenValid: true,
                        },
                        {
                            id: 'b2',
                            type: 'transparent',
                            label: 'Cancel',
                        }],
                        'git-projects.share.repository',
                        'Sharing...',
                    );
                } else if (msg.data.itemId === 'editUrls') {
                    $scope.selectedRepository.name = msg.data.data;
                    gitApi.getOriginUrls($scope.selectedWorkspace.name, $scope.selectedRepository.name).then(function (response) {
                        if (response.status === 200) {
                            $scope.selectedRepository.fetchUrl = response.data.fetchUrl;
                            $scope.selectedRepository.pushUrl = response.data.pushUrl;
                            messageHub.showFormDialog(
                                'editUrlsGitProjectForm',
                                'Edit Origin Urls',
                                [{
                                    id: 'furli',
                                    type: 'input',
                                    label: 'Fetch HTTPS Url',
                                    inputRules: {
                                        excluded: [],
                                        patterns: ['^https?://'],
                                    },
                                    required: true,
                                    placeholder: 'https://github.com/myspace/myproject.git',
                                    value: response.data.fetchUrl,
                                },
                                {
                                    id: 'purli',
                                    type: 'input',
                                    label: 'Push HTTPS Url',
                                    inputRules: {
                                        excluded: [],
                                        patterns: ['^https?://'],
                                    },
                                    required: true,
                                    placeholder: 'https://github.com/myspace/myproject.git',
                                    value: response.data.pushUrl,
                                }],
                                [{
                                    id: 'b1',
                                    type: 'emphasized',
                                    label: 'Save',
                                    whenValid: true,
                                },
                                {
                                    id: 'b2',
                                    type: 'transparent',
                                    label: 'Cancel',
                                }],
                                'git-projects.edit.repository',
                                'Changing...',
                            );
                        } else messageHub.showAlertError('Could not get origin urls', response.message);
                    });
                }
            },
            true
        );

        // Initialization
        $scope.reloadProjects(true);
        $scope.reloadWorkspaceList();
    }]);
