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
const gitProjectsView = angular.module('gitProjects', ['blimpKit', 'platformView', 'WorkspaceService', 'GitService']);
gitProjectsView.constant('StatusBar', new StatusBarHub());
gitProjectsView.constant('Dialogs', new DialogHub());
gitProjectsView.constant('Workspace', new WorkspaceHub());
gitProjectsView.constant('ContextMenu', new ContextMenuHub());
gitProjectsView.constant('Notifications', new NotificationHub());
gitProjectsView.controller('GitProjectsController', ($scope, StatusBar, Dialogs, Workspace, ContextMenu, Notifications, WorkspaceService, GitService, ButtonStates) => {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };
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
    const imageFileExts = ['ico', 'bmp', 'png', 'jpg', 'jpeg', 'gif', 'svg'];
    const modelFileExts = ['extension', 'extensionpoint', 'edm', 'model', 'dsm', 'schema', 'bpmn', 'job', 'listener', 'websocket', 'roles', 'constraints', 'table', 'view'];

    $scope.selectedWorkspace = WorkspaceService.getCurrentWorkspace();

    $scope.projects = [];

    const jstreeWidget = angular.element('#dgProjects');
    $scope.spinnerObj = {
        text: 'Loading...',
        type: 'spinner',
        li_attr: { spinner: true },
    };
    const jstreeConfig = {
        core: {
            check_callback: true,
            multiple: false,
            themes: {
                name: 'fiori',
                variant: 'compact',
            },
            data: (_node, cb) => {
                cb($scope.projects);
            },
        },
        search: {
            case_sensitive: false,
        },
        plugins: ['wholerow', 'search', 'state', 'types'],
        dnd: {
            large_drop_target: true,
            large_drag_target: true,
            is_draggable: (nodes) => {
                for (let i = 0; i < nodes.length; i++) {
                    if (nodes[i].type === 'project') return false;
                }
                return true;
            },
        },
        state: { key: `${brandingInfo.keyPrefix}.view-git-projects.state` },
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

    function getProjectNode(parents) {
        for (let i = 0; i < parents.length; i++) {
            if (parents[i] !== '#') {
                let parent = jstreeWidget.jstree(true).get_node(parents[i]);
                if (parent.type === 'project') {
                    return parent;
                }
            }
        }
    }

    jstreeWidget.on('select_node.jstree', (_event, data) => {
        if (data.event && data.event.type === 'click') {
            let project;
            let isGit = false;
            if (data.node.type === 'project') {
                project = data.node.text;
                isGit = data.node.data.git;
                Workspace.postMessage({
                    topic: 'git.repository.selected',
                    data: {
                        workspace: $scope.selectedWorkspace,
                        project: project,
                        isGitProject: isGit,
                    }
                });
            } else {
                const projectNode = getProjectNode(data.node.parents);
                project = projectNode.text;
                isGit = projectNode.data.git;
                Workspace.postMessage({
                    topic: 'git.repository.file.selected',
                    data: {
                        workspace: $scope.selectedWorkspace,
                        project: project,
                        isGitProject: isGit,
                        file: data.node.text,
                    }
                });
            }
        }
    });

    let selectedNode;

    $scope.showContextMenu = (event) => {
        selectedNode = undefined;
        const items = [];
        if (jstreeWidget[0].contains(event.target)) {
            event.preventDefault();
            let id;
            if (event.target.tagName !== 'LI') {
                let closest = event.target.closest('li');
                if (closest) id = closest.id;
                else items.push(
                    {
                        id: 'refresh',
                        label: 'Refresh',
                        leftIconClass: 'sap-icon--refresh',
                        separator: true,
                    },
                    {
                        id: 'clone',
                        label: 'Clone',
                        leftIconClass: 'sap-icon--duplicate',
                    }
                );
            } else {
                id = event.target.id;
            }
            if (id) {
                selectedNode = jstreeWidget.jstree(true).get_node(id);
                if (!selectedNode.state.selected) {
                    jstreeWidget.jstree(true).deselect_all();
                    jstreeWidget.jstree(true).select_node(selectedNode, false, true);
                }
                if (selectedNode.type === 'project') {
                    if (selectedNode.data.git) {
                        items.push(
                            {
                                id: 'pull',
                                label: 'Pull',
                                leftIconClass: 'sap-icon--download',
                            },
                            {
                                id: 'push',
                                label: 'Push',
                                leftIconClass: 'sap-icon--upload',
                            },
                            {
                                id: 'reset',
                                label: 'Reset',
                                leftIconClass: 'sap-icon--reset',
                                separator: true,
                            },
                            {
                                id: 'editUrls',
                                label: 'Edit URLs',
                                separator: true,
                                leftIconClass: 'sap-icon--edit',
                            },
                            {
                                id: 'importProjects',
                                label: 'Import Project(s)',
                                separator: true,
                                leftIconClass: 'sap-icon--sys-add',
                            },
                            {
                                id: 'delete',
                                label: 'Delete',
                                leftIconClass: 'sap-icon--delete',
                            }
                        );
                    } else {
                        items.push({
                            id: 'share',
                            label: 'Share',
                            leftIconClass: 'sap-icon--share',
                        });
                    }
                }
                // else if (selectedNode.type === 'folder') {
                // } else if (selectedNode.type === 'file') {}
            }
            ContextMenu.showContextMenu({
                ariaLabel: 'git projects view contextmenu',
                posX: event.clientX,
                posY: event.clientY,
                icons: true,
                items: items
            }).then((id) => {
                if (id === 'refresh') {
                    $scope.reloadProjects()
                } else if (id === 'clone') {
                    $scope.cloneDialog();
                } else if (id === 'push') {
                    $scope.selectedRepository.name = selectedNode.text;
                    $scope.pushDialog();
                } else if (id === 'pull') {
                    $scope.selectedRepository.name = selectedNode.text;
                    $scope.pullDialog();
                } else if (id === 'reset') {
                    Dialogs.showDialog({
                        title: `Reset '${selectedNode.text}'?`,
                        message: 'This action cannot be undone.',
                        buttons: [
                            { id: 'b1', label: 'Reset', state: ButtonStates.Negative },
                            { id: 'b2', label: 'Cancel', state: ButtonStates.Transparent },
                        ]
                    }).then((buttonId) => {
                        if (buttonId === 'b1') {
                            Dialogs.showBusyDialog('Resetting...');
                            GitService.resetRepository($scope.selectedWorkspace, selectedNode.text).then(() => {
                                $scope.reloadProjects();
                            }, (response) => {
                                console.error(response);
                                Dialogs.showAlert({
                                    title: 'Could not reset repository',
                                    message: response.message || 'There was an error during the reset.',
                                    type: AlertTypes.Error,
                                    preformatted: false,
                                });
                            }).finally(() => {
                                Dialogs.closeBusyDialog();
                            });
                        }
                    });
                } else if (id === 'delete') {
                    Dialogs.showDialog({
                        title: `Delete '${selectedNode.text}'?`,
                        message: `This action cannot be undone.\nIt is recommended that you delete and unpublish.`,
                        preformatted: true,
                        buttons: [
                            { id: 'b1', label: 'Delete', state: ButtonStates.Negative },
                            { id: 'b2', label: 'Delete & Unpublish', state: ButtonStates.Emphasized },
                            { id: 'b3', label: 'Cancel', state: ButtonStates.Transparent },
                        ]
                    }).then((buttonId) => {
                        if (buttonId !== 'b3') {
                            Dialogs.showBusyDialog('Deleting...');
                            const unpublishOnDelete = buttonId === 'b2';
                            GitService.deleteRepository($scope.selectedWorkspace, selectedNode.text, unpublishOnDelete).then(() => {
                                Workspace.announceWorkspaceChanged({
                                    workspace: $scope.selectedWorkspace,
                                    params: { gitAction: 'delete' },
                                });
                                jstreeWidget.jstree(true).delete_node(selectedNode);
                            }, (response) => {
                                console.error(response);
                                Dialogs.showAlert({
                                    title: 'Could not delete repository',
                                    message: response.message || 'There was an error while deleting the repository.',
                                    type: AlertTypes.Error,
                                    preformatted: false,
                                });
                            }).finally(() => {
                                Dialogs.closeBusyDialog();
                            });
                        }
                    }, (response) => {
                        console.error(response);
                    });
                } else if (id === 'importProjects') {
                    Dialogs.showDialog({
                        title: `Import from '${selectedNode.text}'?`,
                        message: `Import all projects from '${selectedNode.text}'`,
                        buttons: [
                            { id: 'b1', label: 'Import', state: ButtonStates.Emphasized },
                            { id: 'b2', label: 'Cancel', state: ButtonStates.Transparent },
                        ]
                    }).then((buttonId) => {
                        if (buttonId !== 'b2') {
                            $scope.$evalAsync(() => {
                                $scope.state.busyText = 'Importing...';
                                $scope.state.isBusy = true;
                            });
                            GitService.importProjects($scope.selectedWorkspace, selectedNode.text).then(() => {
                                Notifications.show({
                                    type: 'positive',
                                    title: 'Successfully imported project(s)',
                                    description: 'Go to Workbench to see your imported project(s).',
                                });
                            }, (response) => {
                                console.error(response);
                                Dialogs.showAlert({
                                    title: 'Could not import from repository',
                                    message: response.message || 'There was an error while importing from the repository.',
                                    type: AlertTypes.Error,
                                    preformatted: false,
                                });
                            }).finally(() => {
                                $scope.$evalAsync(() => {
                                    $scope.state.busyText = 'Loading...';
                                    $scope.state.isBusy = false;
                                });
                            });
                        }
                    }, (response) => {
                        console.error(response);
                    });
                } else if (id === 'share') {
                    $scope.selectedRepository.name = selectedNode.text;
                    Dialogs.showFormDialog({
                        title: `Share '${$scope.selectedRepository.name}'`,
                        form: {
                            'surli': {
                                label: 'Repository HTTPS Url',
                                controlType: 'input',
                                type: 'text',
                                placeholder: 'https://github.com/myspace/myproject.git',
                                inputRules: {
                                    excluded: [],
                                    patterns: ['^https?://'],
                                },
                                focus: true,
                                required: true
                            },
                            'sbi': {
                                label: 'Branch',
                                controlType: 'input',
                                type: 'text',
                                placeholder: 'main',
                                required: true
                            },
                            'smi': {
                                label: 'Commit message',
                                controlType: 'input',
                                type: 'text',
                                required: true
                            },
                            'suni': {
                                label: 'Username',
                                controlType: 'input',
                                type: 'text',
                                required: true,
                                value: $scope.credentials.username,
                            },
                            'sei': {
                                label: 'Email',
                                controlType: 'input',
                                type: 'text',
                                required: true,
                                value: $scope.credentials.email,
                            },
                            'spwi': {
                                label: 'Password or Token',
                                controlType: 'input',
                                type: 'password',
                                required: true,
                                value: $scope.credentials.password,
                            },
                            'srpc': {
                                label: 'Root project',
                                controlType: 'checkbox',
                                value: false
                            },
                        },
                        submitLabel: 'Share',
                        cancelLabel: 'Cancel'
                    }).then((form) => {
                        if (form) {
                            Dialogs.showBusyDialog('Sharing...');
                            $scope.credentials.username = form['suni'];
                            $scope.credentials.email = form['sei'];
                            $scope.credentials.password = form['spwi'];
                            GitService.shareProject(
                                $scope.selectedWorkspace,
                                $scope.selectedRepository.name,
                                form['surli'],
                                form['sbi'],
                                form['smi'],
                                $scope.credentials.username,
                                $scope.credentials.password,
                                $scope.credentials.email,
                                form['srpc'],
                            ).then(() => {
                                $scope.reloadProjects();
                            }, (response) => {
                                console.error(response);
                                Dialogs.showAlert({
                                    title: 'Could not share repository',
                                    message: response.message || 'There was an error while sharing the repository.',
                                    type: AlertTypes.Error,
                                    preformatted: false,
                                });
                            }).finally(() => {
                                Dialogs.closeBusyDialog();
                            });
                        }
                    }, (error) => {
                        console.error(error);
                    });
                } else if (id === 'editUrls') {
                    $scope.selectedRepository.name = selectedNode.text;
                    GitService.getOriginUrls($scope.selectedWorkspace, $scope.selectedRepository.name).then((response) => {
                        $scope.selectedRepository.fetchUrl = response.data.fetchUrl;
                        $scope.selectedRepository.pushUrl = response.data.pushUrl;
                        Dialogs.showFormDialog({
                            title: `Share '${$scope.selectedRepository.name}'`,
                            form: {
                                'furli': {
                                    label: 'Fetch HTTPS Url',
                                    controlType: 'input',
                                    type: 'text',
                                    placeholder: 'https://github.com/myspace/myproject.git',
                                    inputRules: {
                                        excluded: [],
                                        patterns: ['^https?://'],
                                    },
                                    value: response.data.fetchUrl,
                                    focus: true,
                                    required: true
                                },
                                'purli': {
                                    label: 'Push HTTPS Url',
                                    controlType: 'input',
                                    type: 'text',
                                    placeholder: 'https://github.com/myspace/myproject.git',
                                    inputRules: {
                                        excluded: [],
                                        patterns: ['^https?://'],
                                    },
                                    value: response.data.pushUrl,
                                    required: true
                                },
                            },
                            submitLabel: 'Save',
                            cancelLabel: 'Cancel'
                        }).then((form) => {
                            if (form) {
                                if ($scope.selectedRepository.fetchUrl !== form['furli']) {
                                    GitService.setFetchUrl(
                                        $scope.selectedWorkspace,
                                        $scope.selectedRepository.name,
                                        form['furli']
                                    ).then(() => {
                                        Notifications.show({
                                            type: 'positive',
                                            title: 'Successfully changed fetch url',
                                            description: 'You can now pull from the new repository origin.'
                                        });
                                    }, (response) => {
                                        console.error(response);
                                        Dialogs.showAlert({
                                            title: 'Could not change fetch url',
                                            message: response.message || 'There was an error while changing the fetch url.',
                                            type: AlertTypes.Error,
                                            preformatted: false,
                                        });
                                    });
                                }
                                if ($scope.selectedRepository.pushUrl !== form['purli']) {
                                    GitService.setPushUrl(
                                        $scope.selectedWorkspace,
                                        $scope.selectedRepository.name,
                                        form['purli']
                                    ).then(() => {
                                        Notifications.show({
                                            type: 'positive',
                                            title: 'Successfully changed push url',
                                            description: 'You can now push to the new repository origin.'
                                        });
                                    }, (response) => {
                                        console.error(response);
                                        Dialogs.showAlert({
                                            title: 'Could not change push url',
                                            message: response.message || 'There was an error while changing the push url.',
                                            type: AlertTypes.Error,
                                            preformatted: false,
                                        });
                                    });
                                }
                            }
                        }, (error) => {
                            console.error(error);
                        });
                    }, (response) => {
                        console.error(response);
                        Dialogs.showAlert({
                            title: 'Could not get origin urls',
                            message: response.message || 'There was an error while getting the origin urls.',
                            type: AlertTypes.Error,
                            preformatted: false,
                        });
                    });
                }
            }, (error) => {
                console.error(error);
                StatusBar.showError('Unable to process context menu data');
            });
        };
    };

    $scope.toggleSearch = () => {
        $scope.searchField.text = '';
        jstreeWidget.jstree(true).clear_search();
        $scope.searchVisible = !$scope.searchVisible;
    };

    $scope.isSelectedWorkspace = (name) => $scope.selectedWorkspace === name;

    $scope.reloadWorkspaceList = () => {
        WorkspaceService.listWorkspaceNames().then((response) => {
            $scope.$evalAsync(() => {
                $scope.workspaceNames = response.data;
                $scope.state.error = false;
            });
        }, (response) => {
            console.error(response);
            $scope.state.error = true;
            $scope.errorMessage = 'Unable to load workspace list.';
            Notifications.show({
                type: 'negative',
                title: 'Unable to load workspace list',
                description: 'There was an error while trying to load the workspace list.'
            });
        });
    };

    $scope.reloadProjects = (setConfig = false) => {
        $scope.projects.length = 0;
        $scope.state.isBusy = true;
        GitService.listProjects($scope.selectedWorkspace).then((response) => {
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
            $scope.$evalAsync(() => {
                $scope.state.isBusy = false;
            });
            if (setConfig) jstreeWidget.jstree(jstreeConfig);
            else jstreeWidget.jstree(true).refresh();
        }, (response) => {
            console.error(response);
            $scope.$evalAsync(() => {
                $scope.state.isBusy = false;
                $scope.state.error = true;
                $scope.errorMessage = 'Unable to load project data.';
            });
            Notifications.show({
                type: 'negative',
                title: 'Unable to load workspace data',
                description: 'There was an error while trying to load the workspace content.'
            });
        });
    };

    $scope.switchWorkspace = (workspace) => {
        if ($scope.selectedWorkspace !== workspace) {
            $scope.selectedWorkspace = workspace;
            WorkspaceService.setWorkspace(workspace);
            $scope.reloadProjects();
        }
    };

    $scope.cloneDialog = () => {
        Dialogs.showFormDialog({
            title: 'Clone project',
            form: {
                'curli': {
                    label: 'HTTPS Url',
                    controlType: 'input',
                    type: 'text',
                    placeholder: 'https://github.com/myspace/myproject.git',
                    inputRules: {
                        excluded: [],
                        patterns: ['^https?://'],
                    },
                    focus: true,
                    required: true
                },
                'cuni': {
                    label: 'Username',
                    controlType: 'input',
                    type: 'text',
                    value: $scope.credentials.username,
                },
                'cpwi': {
                    label: 'Password',
                    controlType: 'input',
                    type: 'password',
                    value: $scope.credentials.password,
                },
                'cbi': {
                    label: 'Branch',
                    controlType: 'input',
                    type: 'text',
                    placeholder: 'main',
                },
            },
            width: '420px',
            submitLabel: 'Clone',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.state.busyText = 'Cloning...';
                    $scope.state.isBusy = true;
                });
                $scope.credentials.username = form['cuni'];
                $scope.credentials.password = form['cpwi'];
                GitService.cloneRepository(
                    $scope.selectedWorkspace,
                    form['curli'],
                    form['cbi'],
                    $scope.credentials.username,
                    $scope.credentials.password,
                ).then(() => {
                    Notifications.show({
                        type: 'positive',
                        title: 'Repository cloned',
                        description: `Repository '${form['curli'].split('/').pop().replace('.git', '')}'`
                    });
                    Workspace.announceWorkspaceChanged({
                        workspace: $scope.selectedWorkspace,
                        params: { gitAction: 'clone' },
                    });
                    $scope.$evalAsync(() => {
                        $scope.state.busyText = 'Loading...';
                        $scope.reloadProjects();
                    });
                }, (response) => {
                    $scope.$evalAsync(() => {
                        $scope.state.isBusy = false;
                    });
                    console.error(response);
                    Dialogs.showAlert({
                        title: 'Could not clone repository',
                        message: response.message || 'There was an error while cloning the repository.',
                        type: AlertTypes.Error,
                        preformatted: false,
                    });
                });
            }
        }, (error) => {
            console.error(error);
        });
    };

    $scope.pushDialog = (multiple = false) => {
        Dialogs.showFormDialog({
            title: (multiple ? 'Push all repositories' : `Push '${$scope.selectedRepository.name}'`),
            form: {
                'puni': {
                    label: 'Username',
                    controlType: 'input',
                    type: 'text',
                    required: true,
                    value: $scope.credentials.username,
                },
                'pei': {
                    label: 'Email',
                    controlType: 'input',
                    type: 'text',
                    required: true,
                    value: $scope.credentials.email,
                },
                'cpwi': {
                    label: 'Password or Token',
                    controlType: 'input',
                    type: 'password',
                    required: true,
                    value: $scope.credentials.password,
                },
                'cbi': {
                    label: 'Branch',
                    controlType: 'input',
                    type: 'text',
                    placeholder: 'main',
                },
            },
            submitLabel: 'Push',
            cancelLabel: 'Cancel'
        }).then((form) => {
            $scope.$evalAsync(() => {
                $scope.state.busyText = 'Pushing...';
                $scope.state.isBusy = true;
            });
            $scope.credentials.username = form['puni'] ?? '';
            $scope.credentials.email = form['pei'] ?? '';
            $scope.credentials.password = form['cpwi'] ?? '';
            if (multiple) {
                GitService.pushAllRepositories(
                    $scope.selectedWorkspace,
                    $scope.credentials.username,
                    $scope.credentials.email,
                    $scope.credentials.password,
                ).then(() => {
                    Notifications.show({
                        type: 'positive',
                        title: 'Push successful',
                        description: `Pushed all repositories in '${$scope.selectedWorkspace}'.`
                    });
                }, (response) => {
                    console.error(response);
                    Notifications.show({
                        type: 'negative',
                        title: 'Could not push repositories',
                        description: response.message || 'There was an error while pushing the repositories.',
                    });
                }).finally(() => {
                    $scope.$evalAsync(() => {
                        $scope.state.busyText = 'Loading...';
                        $scope.state.isBusy = false;
                    });
                });
            } else {
                GitService.pushRepository(
                    $scope.selectedWorkspace,
                    $scope.selectedRepository.name,
                    form['cbi'] ?? '',
                    $scope.credentials.username,
                    $scope.credentials.email,
                    $scope.credentials.password,
                ).then(() => {
                    Notifications.show({
                        type: 'positive',
                        title: 'Push successful',
                        description: `Pushed '${$scope.selectedRepository.name}'.`
                    });
                }, (response) => {
                    console.error(response);
                    Notifications.show({
                        type: 'negative',
                        title: 'Could not push repository',
                        description: response.message || 'There was an error while pushing the repository.'
                    });
                }).finally(() => {
                    $scope.$evalAsync(() => {
                        $scope.state.busyText = 'Loading...';
                        $scope.state.isBusy = false;
                    });
                });
            }
        }, (error) => {
            console.error(error);
        });
    };

    $scope.pullDialog = (multiple = false) => {
        Dialogs.showFormDialog({
            title: (multiple ? 'Pull all repositories' : `Pull '${$scope.selectedRepository.name}'`),
            form: {
                'puni': {
                    label: 'Username',
                    controlType: 'input',
                    type: 'text',
                    value: $scope.credentials.username,
                },
                'ppwi': {
                    label: 'Password or Token',
                    controlType: 'input',
                    type: 'password',
                    value: $scope.credentials.password,
                },
            },
            submitLabel: 'Pull',
            cancelLabel: 'Cancel'
        }).then((form) => {
            $scope.$evalAsync(() => {
                $scope.state.busyText = 'Pulling...';
                $scope.state.isBusy = true;
            });
            $scope.credentials.username = form['puni'] ?? '';
            $scope.credentials.password = form['ppwi'] ?? '';
            if (multiple) {
                const projects = [];
                for (let i = 0; i < $scope.projects.length; i++) {
                    projects.push($scope.projects[i].text);
                }
                GitService.pullRepositories(
                    $scope.selectedWorkspace,
                    projects,
                    $scope.credentials.username,
                    $scope.credentials.password,
                    (response) => {
                        if (response.status !== 200) {
                            Notifications.show({
                                type: 'negative',
                                title: 'Could not pull repositories',
                                description: response.message || 'There was an error while pulling the repositories.',
                            });
                        } else {
                            Notifications.show({
                                type: 'positive',
                                title: 'Pull successful',
                                description: 'Pulled all repositories.',
                            });
                        }
                        $scope.$evalAsync(() => {
                            $scope.state.busyText = 'Loading...';
                            $scope.state.isBusy = false;
                        });
                    }
                );
            } else {
                GitService.pullRepository(
                    $scope.selectedWorkspace,
                    $scope.selectedRepository.name,
                    '',
                    $scope.credentials.username,
                    $scope.credentials.password,
                ).then(() => {
                    Notifications.show({
                        type: 'positive',
                        title: 'Pull successful',
                        description: `Pulled '${$scope.selectedRepository.name}'.`,
                    });
                }, (response) => {
                    Notifications.show({
                        type: 'negative',
                        title: 'Could not pull repository',
                        description: response.message || 'There was an error while pulling the repository.',
                    });
                }).finally(() => {
                    $scope.$evalAsync(() => {
                        $scope.state.busyText = 'Loading...';
                        $scope.state.isBusy = false;
                    });
                });
            }
        }, (error) => {
            console.error(error);
        });
    };

    let to = 0;
    $scope.search = () => {
        if (to) { clearTimeout(to); }
        to = setTimeout(() => {
            jstreeWidget.jstree(true).search($scope.searchField.text);
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
        } else if (imageFileExts.indexOf(ext) !== -1) {
            icon = 'sap-icon--picture';
        } else if (modelFileExts.indexOf(ext) !== -1) {
            icon = 'sap-icon--document-text';
        } else {
            icon = 'jstree-file';
        }
        return icon;
    }

    Workspace.onWorkspaceChanged((changed) => {
        if (changed.workspace === $scope.selectedWorkspace) {
            if (changed.params && !changed.params.gitAction)
                $scope.reloadProjects();
        }
    });

    Workspace.onWorkspaceCreated((data) => {
        $scope.$evalAsync(() => {
            $scope.workspaceNames.push(data.workspace);
        });
    });

    Workspace.onWorkspaceDeleted((data) => {
        $scope.$evalAsync(() => {
            $scope.workspaceNames.splice($scope.workspaceNames.indexOf(data.workspace), 1);
            if (data.workspace === $scope.selectedWorkspace) $scope.switchWorkspace('workspace');
        });
    });

    // Initialization
    $scope.reloadProjects(true);
    $scope.reloadWorkspaceList();
});