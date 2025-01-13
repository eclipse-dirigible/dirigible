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
let remoteBranchesView = angular.module('remoteBranches', ['ideUI', 'ideView', 'ideGit']);

remoteBranchesView.controller('RemoteBranchesViewController', [
    '$scope',
    'messageHub',
    'gitApi',
    function (
        $scope,
        messageHub,
        gitApi,
    ) {
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

        $scope.selected = function (index, branch) {
            $scope.selectedBranch.index = index;
            $scope.selectedBranch.name = branch.name;
            $scope.selectedBranch.commitShortId = branch.commitShortId;
            $scope.selectedBranch.commitAuthor = branch.commitAuthor;
            $scope.selectedBranch.commitDate = branch.commitDate;
            $scope.selectedBranch.commitMessage = branch.commitMessage;
        };

        $scope.createBranch = function () {
            messageHub.showFormDialog(
                'createGitBranchForm',
                'Create new branch',
                [{
                    id: "cbn",
                    type: "input",
                    label: "Branch name",
                    value: '',
                },
                {
                    id: "dbui",
                    type: "input",
                    label: "Username",
                    required: true,
                    value: $scope.credentials.username,
                },
                {
                    id: "dpwi",
                    type: "input",
                    inputType: 'password',
                    label: "Password",
                    required: true,
                    value: $scope.credentials.password,
                }],
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
                'git-branches.create.remote.branch',
                'Creating...',
            );
        };

        $scope.deleteBranch = function () {
            messageHub.showDialogAsync(
                `Delete branch '${$scope.selectedBranch.name}'?`,
                'This will delete the branch both locally and remotely. This action cannot be undone.',
                [{
                    id: 'b1',
                    type: 'emphasized',
                    label: 'Delete',
                },
                {
                    id: 'b2',
                    type: 'transparent',
                    label: 'Cancel',
                }],
            ).then(function (dialogResponse) {
                if (dialogResponse.data === 'b1') {
                    messageHub.showFormDialog(
                        'deleteGitBranchForm',
                        `Delete branch '${$scope.selectedBranch.name}'`,
                        [{
                            id: "dbui",
                            type: "input",
                            label: "Username",
                            required: true,
                            value: $scope.credentials.username,
                        },
                        {
                            id: "dpwi",
                            type: "input",
                            inputType: 'password',
                            label: "Password",
                            required: true,
                            value: $scope.credentials.password,
                        }],
                        [{
                            id: 'b1',
                            type: 'emphasized',
                            label: 'Delete',
                            whenValid: true,
                        },
                        {
                            id: 'b2',
                            type: 'transparent',
                            label: 'Cancel',
                        }],
                        'git-branches.delete.remote.branch',
                        'Deleting...',
                    );
                }
            });
        };

        $scope.checkout = function (index, branch) {
            if (branch) {
                $scope.activeBranch.name = branch;
            } else $scope.activeBranch.name = $scope.branches[index].name;
            gitApi.checkoutBranch(
                $scope.selectedWorkspace,
                $scope.selectedRepository,
                $scope.activeBranch.name,
            ).then(function (response) {
                if (response.status === 200) {
                    messageHub.postMessage(
                        'git.repository.branch.checkout',
                        {
                            branch: $scope.activeBranch.name,
                            type: 'remote',
                        }
                    );
                    messageHub.setStatusMessage(`Switched to branch '${$scope.activeBranch.name}'`);
                    if (branch) $scope.loadBranches();
                } else messageHub.showAlertError('Could not checkout to branch', response.message);
            });
        };

        $scope.toggleSearch = function () {
            $scope.searchField.text = '';
            for (let i = 0; i < $scope.branches.length; i++) {
                $scope.branches[i]['hidden'] = false;
            }
            $scope.searchVisible = !$scope.searchVisible;
        };

        $scope.search = function () {
            for (let i = 0; i < $scope.branches.length; i++) {
                if ($scope.branches[i].name.toLowerCase().includes($scope.searchField.text.toLowerCase())) {
                    $scope.branches[i]['hidden'] = false;
                } else $scope.branches[i]['hidden'] = true;
            }
        };

        $scope.loadBranches = function () {
            $scope.selectedBranch.index = -1;
            $scope.selectedBranch.name = '';
            $scope.loadingBranches = true;
            gitApi.branches($scope.selectedWorkspace, $scope.selectedRepository, false).then(
                function (response) {
                    if (response.status === 200) {
                        $scope.branches = response.data.remote;
                    } else messageHub.showAlertError('Could not get remote branches', response.message);
                    $scope.loadingBranches = false;
                }
            );
        };

        $scope.clearList = function () {
            $scope.selectedWorkspace = '';
            $scope.selectedRepository = '';
            $scope.branches.length = 0;
            $scope.selectedBranch.index = -1;
            $scope.selectedBranch.name = '';
        };

        messageHub.onDidReceiveMessage(
            'git-branches.create.remote.branch',
            function (msg) {
                $scope.$apply(function () {
                    if (msg.data.buttonId === "b1") {
                        $scope.credentials.username = msg.data.formData[1].value;
                        $scope.credentials.password = msg.data.formData[2].value;
                        gitApi.createBranch(
                            $scope.selectedWorkspace,
                            $scope.selectedRepository,
                            msg.data.formData[0].value,
                            false,
                            $scope.credentials.username,
                            $scope.credentials.password,
                        ).then(function (response) {
                            if (response.status === 200) {
                                $scope.checkout(-1, msg.data.formData[0].value);
                                messageHub.triggerEvent('git-branches.reload.local', true);
                            } else messageHub.showAlertError('Could not create branch', response.message);
                            messageHub.hideFormDialog('createGitBranchForm');
                        });
                    } else messageHub.hideFormDialog('createGitBranchForm');
                });
            },
            true
        );

        messageHub.onDidReceiveMessage(
            'git.repository.selected',
            function (msg) {
                $scope.$apply(function () {
                    if (msg.data.isGitProject) {
                        $scope.selectedWorkspace = msg.data.workspace;
                        $scope.selectedRepository = msg.data.project;
                        $scope.loadBranches();
                    } else {
                        $scope.clearList();
                    }
                });
            }
        );

        messageHub.onDidReceiveMessage(
            'git-branches.delete.remote.branch',
            function (msg) {
                $scope.$apply(function () {
                    if (msg.data.buttonId === "b1") {
                        $scope.credentials.username = msg.data.formData[0].value;
                        $scope.credentials.password = msg.data.formData[1].value;
                        gitApi.deleteBranch(
                            $scope.selectedWorkspace,
                            $scope.selectedRepository,
                            $scope.selectedBranch.name,
                            false,
                            $scope.credentials.username,
                            $scope.credentials.password,
                        ).then(function (response) {
                            if (response.status === 200) {
                                messageHub.setStatusMessage(`Deleted branch '${$scope.selectedBranch.name}'`);
                                messageHub.postMessage(
                                    'git.repository.branch.delete',
                                    {
                                        branch: $scope.selectedBranch.name,
                                    }
                                );
                                $scope.branches.splice($scope.selectedBranch.index, 1);
                                $scope.selectedBranch.index = -1;
                                $scope.selectedBranch.name = '';
                            } else messageHub.showAlertError('Could not delete branch', response.message);
                            messageHub.hideFormDialog('deleteGitBranchForm');
                        });
                    } else messageHub.hideFormDialog('deleteGitBranchForm');
                });
            },
            true
        );

        // Used for matching with the current local branch
        messageHub.onDidReceiveMessage(
            'git.repository.branch.current',
            function (msg) {
                if (msg.data.type === 'local') {
                    $scope.activeBranch.name = msg.data.branch;
                }
            }
        );

        messageHub.onDidReceiveMessage(
            'git.repository.branch.checkout',
            function (msg) {
                if (msg.data.type === 'local') {
                    if ($scope.activeBranch.name !== msg.data.branch) {
                        $scope.$apply(function () {
                            $scope.activeBranch.name = '';
                            for (let i = 0; i < $scope.branches.length; i++) {
                                if ($scope.branches[i].name === msg.data.branch) {
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
        );
    }]);
