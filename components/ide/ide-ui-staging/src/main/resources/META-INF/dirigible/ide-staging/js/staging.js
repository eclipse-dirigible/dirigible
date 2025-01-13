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
let stagingView = angular.module('staging', ['ideUI', 'ideView', 'ideGit']);

stagingView.controller('StagingViewController', [
    '$scope',
    'messageHub',
    'gitApi',
    function (
        $scope,
        messageHub,
        gitApi,
    ) {
        $scope.forms = {
            credentialsFieldset: {}
        };
        $scope.listType = 'path';
        $scope.unstagedFiles = [];
        $scope.stagedFiles = [];
        $scope.jstreeUnstaged = angular.element('#unstaged');
        $scope.jstreeStaged = angular.element('#staged');
        $scope.jstreeTypes = {
            0: { //Conflicting
                icon: 'sap-icon--status-critical',
            },
            1: { //Added
                icon: 'sap-icon--sys-add',
            },
            2: { //Changed
                icon: 'sap-icon--edit',
            },
            3: { //Missing
                icon: 'sap-icon--delete'
            },
            4: { //Modified
                icon: 'sap-icon--edit',
            },
            5: { //Removed
                icon: 'sap-icon--delete',
            },
            6: { //Untracked
                icon: 'sap-icon--to-be-reviewed',
            },
            node: {
                icon: 'jstree-folder',
            },
        };
        $scope.commitData = {
            username: '',
            email: '',
            password: '',
            commitMessage: '',
        };

        function arrangeIntoTree(rawData) {
            let tree = [];
            let paths = [];
            for (let i = 0; i < rawData.length; i++) {
                paths.push(rawData[i].path.split('/'));
            }

            for (let i = 0; i < paths.length; i++) {
                let path = paths[i];
                let currentLevel = tree;
                for (let j = 0; j < path.length; j++) {
                    let part = path[j];

                    let existingPath = findWhere(currentLevel, 'text', part);

                    if (existingPath) {
                        currentLevel = existingPath.children;
                    } else {
                        let newPart;
                        if (j === path.length - 1) {
                            newPart = {
                                text: part,
                                type: rawData[i].type,
                                data: { path: rawData[i].path },
                            }
                        } else {
                            newPart = {
                                text: part,
                                type: 'node',
                                children: [],
                            }
                        }

                        currentLevel.push(newPart);
                        currentLevel = newPart.children;
                    }
                }
            }
            return tree;

            function findWhere(array, key, value) {
                let counter = 0;
                while (counter < array.length && array[counter][key] !== value) { counter++; };

                if (counter < array.length) {
                    return array[counter]
                } else {
                    return false;
                }
            }
        }

        $scope.jstreeUnstaged.jstree({
            core: {
                check_callback: true,
                themes: {
                    name: "fiori",
                    variant: "compact",
                },
                data: function (node, cb) {
                    let data = [];
                    if ($scope.listType === 'path') {
                        for (let i = 0; i < $scope.unstagedFiles.length; i++) {
                            data.push({
                                text: $scope.unstagedFiles[i].path,
                                type: $scope.unstagedFiles[i].type,
                            });
                        }
                    } else {
                        data = arrangeIntoTree($scope.unstagedFiles);
                    }
                    cb(data);
                },
            },
            plugins: ["wholerow", "types", "indicator"],
            types: $scope.jstreeTypes,
        });

        $scope.jstreeStaged.jstree({
            core: {
                check_callback: true,
                themes: {
                    name: "fiori",
                    variant: "compact",
                },
                data: function (node, cb) {
                    let data = [];
                    if ($scope.listType === 'path') {
                        for (let i = 0; i < $scope.stagedFiles.length; i++) {
                            data.push({
                                text: $scope.stagedFiles[i].path,
                                type: $scope.stagedFiles[i].type,
                            });
                        }
                    } else {
                        data = arrangeIntoTree($scope.stagedFiles);
                    }
                    cb(data);
                },
            },
            plugins: ["wholerow", "types", "indicator"],
            types: $scope.jstreeTypes,
        });

        $scope.jstreeUnstaged.on('dblclick.jstree', function (event) {
            let node = $scope.jstreeUnstaged.jstree(true).get_node(event.target);
            showDiff(node);
        });

        $scope.jstreeStaged.on('dblclick.jstree', function (event) {
            let node = $scope.jstreeStaged.jstree(true).get_node(event.target);
            showDiff(node);
        });

        $scope.getSelectedDiff = function (unstaged = true) {
            let selectedNodes;
            let node;
            if (unstaged) selectedNodes = $scope.jstreeUnstaged.jstree(true).get_selected(true);
            else selectedNodes = $scope.jstreeStaged.jstree(true).get_selected(true);
            let index = selectedNodes.length - 1;
            while (index >= 0) {
                if (selectedNodes[index].type !== 'node') {
                    node = selectedNodes[index];
                    break;
                } else index--;
            }
            if (node) showDiff(node);
        };

        function showDiff(node) {
            if (node.type !== 'node') {
                let path;
                if ($scope.listType === 'path') path = node.text;
                else path = node.data.path;
                messageHub.postMessage(
                    'git.staging.file.diff',
                    {
                        project: `${$scope.selectedWorkspace}/${$scope.selectedRepository}`,
                        file: path,
                    }
                );
            }
        }

        $scope.addToIndex = function () {
            messageHub.showStatusBusy("Adding files to index...");
            let paths = getSelectedUnstagedPaths();
            gitApi.addToIndex(
                $scope.selectedWorkspace,
                $scope.selectedRepository,
                paths,
            ).then(function (response) {
                if (response.status !== 200) {
                    messageHub.showAlertError('Could not add to index', response.message);
                } else {
                    $scope.loadRepositoryStatus();
                }
                messageHub.hideStatusBusy();
            });
        };

        $scope.removeFromIndex = function () {
            messageHub.showStatusBusy("Removing files to index...");
            let paths = getSelectedStagedPaths();
            gitApi.removeFiles(
                $scope.selectedWorkspace,
                $scope.selectedRepository,
                paths,
            ).then(function (response) {
                if (response.status !== 200) {
                    messageHub.showAlertError('Could not remove from index', response.message);
                } else {
                    $scope.loadRepositoryStatus();
                }
                messageHub.hideStatusBusy();
            });
        };

        $scope.revert = function () {
            let paths = getSelectedUnstagedPaths();
            if (paths) {
                let title;
                if (paths.length > 1) title = 'Revert files?';
                else title = `Revert '${paths[0]}'?`
                messageHub.showDialogAsync(
                    title,
                    'This action cannot be undone. All changes will be lost.',
                    [
                        {
                            id: 'b1',
                            type: 'emphasized',
                            label: 'Revert',
                        },
                        {
                            id: 'b2',
                            type: 'transparent',
                            label: 'Cancel',
                        }],
                ).then(function (dialogResponse) {
                    if (dialogResponse.data === 'b1') {
                        gitApi.revertFiles($scope.selectedWorkspace, $scope.selectedRepository, paths).then(function (response) {
                            if (response.status === 200) {
                                $scope.loadRepositoryStatus();
                            } else {
                                messageHub.showAlertError('Could not revert file(s)', response.message);
                            }
                        });
                    }
                });
            }
        };

        function getSelectedStagedPaths() {
            let paths = [];
            let selectedNodes = $scope.jstreeStaged.jstree(true).get_selected(true);
            if ($scope.listType === 'path') {
                for (let i = 0; i < selectedNodes.length; i++) {
                    paths.push(selectedNodes[i].text);
                }
            } else {
                for (let i = 0; i < selectedNodes.length; i++) {
                    if (selectedNodes[i].type === 'node') {
                        let children = []
                        for (let j = 0; j < selectedNodes[i].children_d.length; j++) {
                            let node = $scope.jstreeStaged.jstree(true).get_node(selectedNodes[i].children_d[j]);
                            if (node.type !== 'node') {
                                if (!paths.includes(node.data.path))
                                    children.push(node.data.path);
                            }
                        }
                        paths = paths.concat(children);
                    } else paths.push(selectedNodes[i].data.path);
                }
            }
            return paths;
        }

        function getSelectedUnstagedPaths() {
            let paths = [];
            let selectedNodes = $scope.jstreeUnstaged.jstree(true).get_selected(true);
            if ($scope.listType === 'path') {
                for (let i = 0; i < selectedNodes.length; i++) {
                    paths.push(selectedNodes[i].text);
                }
            } else {
                for (let i = 0; i < selectedNodes.length; i++) {
                    if (selectedNodes[i].type === 'node') {
                        let children = []
                        for (let j = 0; j < selectedNodes[i].children_d.length; j++) {
                            let node = $scope.jstreeUnstaged.jstree(true).get_node(selectedNodes[i].children_d[j]);
                            if (node.type !== 'node') {
                                if (!paths.includes(node.data.path))
                                    children.push(node.data.path);
                            }
                        }
                        paths = paths.concat(children);
                    } else paths.push(selectedNodes[i].data.path);
                }
            }
            return paths;
        }

        $scope.push = function (afterCommit = false) {
            messageHub.showBusyDialog(
                'stagingPushBusyDialog',
                'Pushing...',
            );
            gitApi.pushRepository(
                $scope.selectedWorkspace,
                $scope.selectedRepository,
                '',
                $scope.commitData.username,
                $scope.commitData.email,
                $scope.commitData.password,
            ).then(function (response) {
                if (response.status !== 200) {
                    messageHub.showAlertError('Could not push repository', response.message);
                }
                if (afterCommit) {
                    $scope.commitData.commitMessage = '';
                    $scope.loadRepositoryStatus();
                }
                messageHub.hideBusyDialog('stagingPushBusyDialog');
            });
        };

        $scope.commit = function (push = false) {
            messageHub.showBusyDialog(
                'stagingCommitBusyDialog',
                'Committing changes...',
            );
            gitApi.commit(
                $scope.selectedWorkspace,
                $scope.selectedRepository,
                $scope.commitData.commitMessage,
                $scope.commitData.username,
                $scope.commitData.password,
                $scope.commitData.email,
            ).then(function (response) {
                if (response.status !== 200) {
                    messageHub.showAlertError('Could not commit changes', response.message);
                } else if (push) {
                    $scope.push(true);
                } else {
                    $scope.commitData.commitMessage = '';
                    $scope.loadRepositoryStatus();
                }
                messageHub.hideBusyDialog('stagingCommitBusyDialog');
            });
        };

        $scope.changeListType = function (type) {
            $scope.listType = type;
            $scope.jstreeUnstaged.jstree(true).refresh();
            $scope.jstreeStaged.jstree(true).refresh();
        };

        $scope.loadRepositoryStatus = function () {
            messageHub.showStatusBusy("Loading git status...");
            $scope.unstagedFiles.length = 0;
            $scope.stagedFiles.length = 0;
            gitApi.getUnstagedFiles($scope.selectedWorkspace, $scope.selectedRepository).then(
                function (response) {
                    if (response.status === 200) {
                        $scope.unstagedFiles = response.data;
                        $scope.jstreeUnstaged.jstree(true).refresh();
                    } else messageHub.showAlertError('Could not get unstaged files', response.message);
                    messageHub.hideStatusBusy();
                }
            );
            gitApi.getStagedFiles($scope.selectedWorkspace, $scope.selectedRepository).then(
                function (response) {
                    if (response.status === 200) {
                        $scope.stagedFiles = response.data;
                        $scope.jstreeStaged.jstree(true).refresh();
                    } else messageHub.showAlertError('Could not get unstaged files', response.message);
                    messageHub.hideStatusBusy();
                }
            );
        };

        $scope.clearLists = function () {
            $scope.selectedWorkspace = '';
            $scope.selectedRepository = '';
            $scope.unstagedFiles.length = 0;
            $scope.stagedFiles.length = 0;
            $scope.jstreeUnstaged.jstree(true).refresh();
            $scope.jstreeStaged.jstree(true).refresh();
        };

        messageHub.onDidReceiveMessage(
            'git.repository.selected',
            function (msg) {
                if (msg.data.isGitProject) {
                    $scope.selectedWorkspace = msg.data.workspace;
                    $scope.selectedRepository = msg.data.project;
                    $scope.loadRepositoryStatus();
                } else {
                    $scope.clearLists();
                }
            }
        );
    }]);
