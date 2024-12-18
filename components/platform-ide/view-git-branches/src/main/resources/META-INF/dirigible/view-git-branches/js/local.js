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
const localBranchesView = angular.module('localBranches', ['blimpKit', 'platformView', 'GitService']);
localBranchesView.controller('LocalBranchesViewController', ($scope, GitService, ButtonStates) => {
    const dialogHub = new DialogHub();
    const statusBarHub = new StatusBarHub();
    const notificationHub = new NotificationHub();
    $scope.searchVisible = false;
    $scope.loadingBranches = false;
    $scope.searchField = { text: '' };
    $scope.branches = [];
    $scope.activeBranch = {
        name: '',
    };
    $scope.selectedBranch = {
        index: -1,
        name: '',
        commitShortId: '',
        commitAuthor: '',
        commitDate: '',
        commitMessage: '',
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
            },
            submitLabel: 'Clone',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.loadingBranches = true;
                });
                GitService.createBranch(
                    $scope.selectedWorkspace,
                    $scope.selectedRepository,
                    form['cbn']
                ).then(() => {
                    $scope.$evalAsync(() => {
                        $scope.checkout(-1, form['cbn']);
                    });
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
            dialogHub.showDialog({
                title: `Delete branch '${$scope.selectedBranch.name}'`,
                message: 'This action cannot be undone.',
                buttons: [
                    { id: 'del', label: 'Delete', state: ButtonStates.Negative },
                    { id: 'close', label: 'Close' }
                ]
            }).then((buttonId) => {
                if (buttonId === 'del') {
                    $scope.$evalAsync(() => {
                        $scope.loadingBranches = true;
                    });
                    GitService.deleteBranch(
                        $scope.selectedWorkspace,
                        $scope.selectedRepository,
                        $scope.selectedBranch.name,
                    ).then(() => {
                        notificationHub.show({
                            type: 'positive',
                            title: 'Branch deleted',
                            description: `Deleted branch '${$scope.selectedBranch.name}'`,
                        });
                        $scope.$evalAsync(() => {
                            $scope.loadBranches();
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
                    type: 'local',
                }
            });
            statusBarHub.showMessage(`Switched to branch '${$scope.activeBranch.name}'`);
            if (branch) $scope.loadBranches();
        }, (response) => {
            console.log(response);
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

    $scope.loadBranches = (initRemote = true) => {
        $scope.selectedBranch.index = -1;
        $scope.selectedBranch.name = '';
        $scope.$evalAsync(() => {
            $scope.loadingBranches = true;
        });
        GitService.branches($scope.selectedWorkspace, $scope.selectedRepository, true).then(
            (response) => {
                $scope.$evalAsync(() => {
                    $scope.branches = response.data.local;
                    for (let i = 0; i < $scope.branches.length; i++) {
                        if ($scope.branches[i].current) {
                            $scope.activeBranch.name = $scope.branches[i].name;
                            if (initRemote) notificationHub.postMessage({
                                topic: 'git.repository.branch.current',
                                data: {
                                    workspace: $scope.selectedWorkspace.name,
                                    project: $scope.selectedRepository,
                                    branch: $scope.activeBranch.name,
                                    type: 'local',
                                },
                            });
                            break;
                        }
                    }
                });
            }, (response) => {
                console.error(response);
                notificationHub.show({
                    type: 'negative',
                    title: 'Could not get local branches',
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

    const reloadLocalListener = notificationHub.addMessageListener({
        topic: 'git-branches.reload.local',
        handler: () => {
            $scope.$evalAsync(() => {
                $scope.loadBranches(false);
            });
        }
    });

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

    const checkoutListener = notificationHub.addMessageListener({
        topic: 'git.repository.branch.checkout',
        handler: (data) => {
            if (data.type === 'remote') {
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
                        if (!$scope.activeBranch.name) $scope.loadBranches();
                    });
                }
            }
        }
    });

    const branchDeleteListener = notificationHub.addMessageListener({
        topic: 'git.repository.branch.delete',
        handler: (data) => {
            $scope.$evalAsync(() => {
                for (let i = 0; i < $scope.branches.length; i++) {
                    if ($scope.branches[i].name === data.branch) {
                        $scope.branches.splice(i, 1);
                        $scope.selectedBranch.index = -1;
                        $scope.selectedBranch.name = '';
                        break;
                    }
                }
            });
        }
    });

    $scope.$on('$destroy', () => {
        notificationHub.removeMessageListener(reloadLocalListener);
        notificationHub.removeMessageListener(repoSelectedListener);
        notificationHub.removeMessageListener(checkoutListener);
        notificationHub.removeMessageListener(branchDeleteListener);
    });
});