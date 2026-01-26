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
const remoteBranchesView = angular.module('remoteBranches', ['blimpKit', 'platformView', 'GitService']);
remoteBranchesView.controller('RemoteBranchesViewController', ($scope, GitService) => {
    const dialogHub = new DialogHub();
    const statusBarHub = new StatusBarHub();
    const notificationHub = new NotificationHub();
    $scope.searchVisible = false;
    $scope.loadingBranches = false;
    $scope.searchField = { text: '' };
    $scope.branches = [];
    $scope.activeBranch = {
        name: 'main',
    };
    $scope.selectedBranch = {
        index: -1,
        name: '',
        commitShortId: '',
        commitAuthor: '',
        commitDate: '',
        commitMessage: '',
    };
    $scope.credentials = {
        username: '',
        password: '',
    };

    $scope.selected = (index, branch) => {
        $scope.selectedBranch.index = index;
        $scope.selectedBranch.name = branch.name;
        $scope.selectedBranch.commitShortId = branch.commitShortId;
        $scope.selectedBranch.commitAuthor = branch.commitAuthor;
        $scope.selectedBranch.commitDate = branch.commitDate;
        $scope.selectedBranch.commitMessage = branch.commitMessage;
    };

    $scope.createBranch = () => {
        dialogHub.showFormDialog({
            title: 'Create branch',
            form: {
                'cbn': {
                    label: 'Name',
                    controlType: 'input',
                    type: 'text',
                    placeholder: 'develop',
                    focus: true,
                    required: true,
                    submitOnEnter: true,
                },
                'dbui': {
                    label: 'Username',
                    controlType: 'input',
                    type: 'text',
                    focus: true,
                    required: true,
                    value: $scope.credentials.username,
                },
                'dpwi': {
                    label: 'Password or Token',
                    controlType: 'input',
                    type: 'password',
                    required: true,
                    value: $scope.credentials.password,
                },
            },
            submitLabel: 'Create',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.loadingBranches = true;
                });
                $scope.credentials.username = form['dbui'];
                $scope.credentials.password = form['dpwi'];
                GitService.createBranch(
                    $scope.selectedWorkspace,
                    $scope.selectedRepository,
                    form['cbn'],
                    false,
                    $scope.credentials.username,
                    $scope.credentials.password,
                ).then(() => {
                    $scope.$evalAsync(() => {
                        $scope.checkout(-1, form['cbn']);
                    });
                    notificationHub.triggerEvent('git-branches.reload.local');
                }, (response) => {
                    console.error(response);
                    notificationHub.show({
                        type: 'negative',
                        title: 'Could not create branch',
                        description: response.message ?? 'There was an error while creating a new branch.',
                    });
                }).finally(() => {
                    $scope.$evalAsync(() => {
                        $scope.loadingBranches = false;
                    });
                });
            }
        }, (error) => {
            console.error(error);
        });
    };

    $scope.deleteBranch = () => {
        if ($scope.selectedBranch.index >= 0) {
            dialogHub.showFormDialog({
                title: `Delete branch '${$scope.selectedBranch.name}'`,
                subheader: 'This will delete the branch both locally and remotely! This action cannot be undone.',
                form: {
                    'dbui': {
                        label: 'Username',
                        controlType: 'input',
                        type: 'text',
                        focus: true,
                        required: true,
                        value: $scope.credentials.username,
                    },
                    'dpwi': {
                        label: 'Password or Token',
                        controlType: 'input',
                        type: 'password',
                        required: true,
                        value: $scope.credentials.password,
                    },
                },
                submitLabel: 'Delete',
                cancelLabel: 'Cancel'
            }).then((form) => {
                if (form) {
                    $scope.$evalAsync(() => {
                        $scope.loadingBranches = true;
                    });
                    $scope.credentials.username = form['dbui'];
                    $scope.credentials.password = form['dpwi'];
                    GitService.deleteBranch(
                        $scope.selectedWorkspace,
                        $scope.selectedRepository,
                        $scope.selectedBranch.name,
                        false,
                        $scope.credentials.username,
                        $scope.credentials.password,
                    ).then(() => {
                        notificationHub.show({
                            type: 'positive',
                            title: 'Branch deleted',
                            description: `Deleted branch '${$scope.selectedBranch.name}'`,
                        });
                        notificationHub.postMessage({
                            topic: 'git.repository.branch.delete',
                            data: { branch: $scope.selectedBranch.name }
                        });
                        $scope.$evalAsync(() => {
                            $scope.branches.splice($scope.selectedBranch.index, 1);
                            $scope.selectedBranch.index = -1;
                            $scope.selectedBranch.name = '';
                        });
                    }, (response) => {
                        console.error(response);
                        dialogHub.showAlert({
                            title: 'Could not delete branch',
                            message: response.message || 'There was an error while deleting the branch.',
                            type: AlertTypes.Error,
                            preformatted: false,
                        });
                    }).finally(() => {
                        $scope.$evalAsync(() => {
                            $scope.loadingBranches = false;
                        });
                    });
                }
            }, (error) => {
                console.error(error);
                statusBarHub.showError(`An error occurred - ${error}`);
            });
        }
    };

    $scope.checkout = (index, branch) => {
        $scope.loadingBranches = true;
        if (branch) {
            $scope.activeBranch.name = branch;
        } else $scope.activeBranch.name = $scope.branches[index].name;
        GitService.checkoutBranch(
            $scope.selectedWorkspace,
            $scope.selectedRepository,
            $scope.activeBranch.name,
        ).then(() => {
            dialogHub.postMessage({
                topic: 'git.repository.branch.checkout',
                data: {
                    branch: $scope.activeBranch.name,
                    type: 'remote',
                }
            });
            statusBarHub.showMessage(`Switched to branch '${$scope.activeBranch.name}'`);
            if (branch) $scope.loadBranches();
        }, (response) => {
            console.error(response);
            dialogHub.showAlert({
                title: 'Could not checkout to branch',
                message: response.message || 'There was an error while switching branches.',
                type: AlertTypes.Error,
            });
        }).finally(() => {
            $scope.$evalAsync(() => {
                $scope.loadingBranches = false;
            });
        });
    };

    $scope.toggleSearch = () => {
        $scope.searchField.text = '';
        for (let i = 0; i < $scope.branches.length; i++) {
            $scope.branches[i]['hidden'] = false;
        }
        $scope.searchVisible = !$scope.searchVisible;
    };

    $scope.search = (event) => {
        if (event.originalEvent.key === 'Escape') {
            $scope.toggleSearch();
            return;
        }
        for (let i = 0; i < $scope.branches.length; i++) {
            if ($scope.branches[i].name.toLowerCase().includes($scope.searchField.text.toLowerCase())) {
                $scope.branches[i]['hidden'] = false;
            } else $scope.branches[i]['hidden'] = true;
        }
    };

    $scope.loadBranches = () => {
        $scope.selectedBranch.index = -1;
        $scope.selectedBranch.name = '';
        $scope.$evalAsync(() => {
            $scope.loadingBranches = true;
        });
        GitService.branches($scope.selectedWorkspace, $scope.selectedRepository, false).then(
            (response) => {
                $scope.$evalAsync(() => {
                    $scope.branches = response.data.remote;
                    for (let i = 0; i < $scope.branches.length; i++) {
                        if ($scope.branches[i].current) {
                            $scope.activeBranch.name = $scope.branches[i].name;
                            break;
                        }
                    }
                });
            }, (response) => {
                console.error(response);
                notificationHub.show({
                    type: 'negative',
                    title: 'Could not get remote branches',
                    description: response.message ?? 'There was an error while loading the branches.',
                });
            }
        ).finally(() => {
            $scope.$evalAsync(() => {
                $scope.loadingBranches = false;
            });
        });
    };

    $scope.clearList = () => {
        $scope.selectedWorkspace = '';
        $scope.selectedRepository = '';
        $scope.branches.length = 0;
        $scope.selectedBranch.index = -1;
        $scope.selectedBranch.name = '';
    };

    const repoSelectedListener = notificationHub.addMessageListener({
        topic: 'git.repository.selected',
        handler: (data) => {
            if ($scope.selectedWorkspace !== data.workspace || $scope.selectedRepository !== data.project) {
                $scope.$evalAsync(() => {
                    if (data.isGitProject) {
                        $scope.selectedWorkspace = data.workspace;
                        $scope.selectedRepository = data.project;
                        $scope.loadBranches();
                    } else {
                        $scope.clearList();
                    }
                });
            }
        }
    });

    // Used for matching with the current local branch
    const currentBranchListener = notificationHub.addMessageListener({
        topic: 'git.repository.branch.current',
        handler: (data) => {
            if (data.type === 'local') {
                $scope.$evalAsync(() => {
                    $scope.activeBranch.name = data.branch;
                });
            }
        }
    });

    const checkoutListener = notificationHub.addMessageListener({
        topic: 'git.repository.branch.checkout',
        handler: (data) => {
            if (data.type === 'local') {
                if ($scope.activeBranch.name !== data.branch) {
                    $scope.$evalAsync(() => {
                        $scope.activeBranch.name = '';
                        for (let i = 0; i < $scope.branches.length; i++) {
                            if ($scope.branches[i].name === data.branch) {
                                $scope.activeBranch.name = $scope.branches[i].name;
                                $scope.selectedBranch.index = -1;
                                $scope.selectedBranch.name = '';
                                break;
                            }
                        }
                    });
                    if (!$scope.activeBranch.name) $scope.loadBranches();
                }
            }
        }
    });

    $scope.$on('$destroy', () => {
        notificationHub.removeMessageListener(repoSelectedListener);
        notificationHub.removeMessageListener(currentBranchListener);
        notificationHub.removeMessageListener(checkoutListener);
    });
});