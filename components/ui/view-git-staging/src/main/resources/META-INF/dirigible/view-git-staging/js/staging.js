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
const stagingView = angular.module('staging', ['blimpKit', 'platformView', 'GitService']);
stagingView.controller('StagingViewController', ($scope, GitService, ButtonStates) => {
    const dialogHub = new DialogHub();
    const statusBarHub = new StatusBarHub();
    const notificationHub = new NotificationHub();
    $scope.forms = {
        credentialsFieldset: {}
    };
    $scope.loadingGitState = false;
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

    // function arrangeIntoTree(rawData) {
    //     let tree = [];
    //     let paths = [];
    //     for (let i = 0; i < rawData.length; i++) {
    //         paths.push(rawData[i].path.split('/'));
    //     }

    //     for (let i = 0; i < paths.length; i++) {
    //         let path = paths[i];
    //         let currentLevel = tree;
    //         for (let j = 0; j < path.length; j++) {
    //             let part = path[j];

    //             let existingPath = findWhere(currentLevel, 'text', part);

    //             if (existingPath) {
    //                 currentLevel = existingPath.children;
    //             } else {
    //                 let newPart;
    //                 if (j === path.length - 1) {
    //                     newPart = {
    //                         text: part,
    //                         type: rawData[i].type,
    //                         data: { path: rawData[i].path },
    //                     }
    //                 } else {
    //                     newPart = {
    //                         text: part,
    //                         type: 'node',
    //                         children: [],
    //                     }
    //                 }

    //                 currentLevel.push(newPart);
    //                 currentLevel = newPart.children;
    //             }
    //         }
    //     }
    //     return tree;

    //     function findWhere(array, key, value) {
    //         let counter = 0;
    //         while (counter < array.length && array[counter][key] !== value) { counter++; };

    //         if (counter < array.length) {
    //             return array[counter]
    //         } else {
    //             return false;
    //         }
    //     }
    // }

    $scope.jstreeUnstaged.jstree({
        core: {
            check_callback: true,
            themes: {
                name: 'fiori',
                variant: 'compact',
            },
            data: (_node, cb) => {
                const data = [];
                // if ($scope.listType === 'path') {
                for (let i = 0; i < $scope.unstagedFiles.length; i++) {
                    data.push({
                        text: $scope.unstagedFiles[i].path,
                        type: $scope.unstagedFiles[i].type,
                    });
                }
                // } else {
                //     data = arrangeIntoTree($scope.unstagedFiles);
                // }
                cb(data);
            },
        },
        plugins: ['wholerow', 'types'],
        types: $scope.jstreeTypes,
    });

    $scope.jstreeStaged.jstree({
        core: {
            check_callback: true,
            themes: {
                name: 'fiori',
                variant: 'compact',
            },
            data: (_node, cb) => {
                const data = [];
                // if ($scope.listType === 'path') {
                for (let i = 0; i < $scope.stagedFiles.length; i++) {
                    data.push({
                        text: $scope.stagedFiles[i].path,
                        type: $scope.stagedFiles[i].type,
                    });
                }
                // } else {
                //     data = arrangeIntoTree($scope.stagedFiles);
                // }
                cb(data);
            },
        },
        plugins: ['wholerow', 'types'],
        types: $scope.jstreeTypes,
    });

    $scope.jstreeUnstaged.on('dblclick.jstree', (event) => {
        const node = $scope.jstreeUnstaged.jstree(true).get_node(event.target);
        showDiff(node);
    });

    $scope.jstreeStaged.on('dblclick.jstree', (event) => {
        const node = $scope.jstreeStaged.jstree(true).get_node(event.target);
        showDiff(node);
    });

    $scope.getSelectedDiff = (unstaged = true) => {
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
            path = node.text;
            // if ($scope.listType === 'path') path = node.text;
            // else path = node.data.path;
            notificationHub.postMessage({
                topic: 'git.staging.file.diff',
                data: {
                    project: `${$scope.selectedWorkspace}/${$scope.selectedRepository}`,
                    file: path,
                }
            });
        }
    }

    $scope.addToIndex = () => {
        const paths = getSelectedUnstagedPaths();
        if (paths.length) {
            statusBarHub.showBusy('Adding files to index...');
            GitService.addToIndex(
                $scope.selectedWorkspace,
                $scope.selectedRepository,
                paths,
            ).then(() => {
                $scope.$evalAsync(() => {
                    $scope.loadRepositoryStatus();
                });
            }, (response) => {
                console.error(response);
                dialogHub.showAlert({
                    title: 'Could not add to index',
                    message: response.message || 'There was an error while adding the selected path(s) to index.',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            }).finally(() => {
                statusBarHub.hideBusy();
            });
        }
    };

    $scope.removeFromIndex = () => {
        const paths = getSelectedStagedPaths();
        if (paths.length) {
            statusBarHub.showBusy('Removing files to index...');
            GitService.removeFiles(
                $scope.selectedWorkspace,
                $scope.selectedRepository,
                paths,
            ).then(() => {
                $scope.$evalAsync(() => {
                    $scope.loadRepositoryStatus();
                });
            }, (response) => {
                console.error(response);
                dialogHub.showAlert({
                    title: 'Could not remove from index',
                    message: response.message || 'There was an error while removing the selected path(s) from index.',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            }).finally(() => {
                statusBarHub.hideBusy();
            });
        }
    };

    $scope.revert = () => {
        const paths = getSelectedUnstagedPaths();
        if (paths) {
            dialogHub.showDialog({
                title: 'Revert file(s)?',
                message: paths.length > 1 ? 'This action cannot be undone.' : `Revert '${paths[0]}'?\nThis action cannot be undone.`,
                preformatted: true,
                buttons: [
                    { id: 'b1', label: 'Revert', state: ButtonStates.Negative },
                    { id: 'b2', label: 'Cancel', state: ButtonStates.Transparent },
                ]
            }).then((buttonId) => {
                if (buttonId === 'b1') {
                    dialogHub.showBusyDialog('Reverting...');
                    GitService.revertFiles($scope.selectedWorkspace, $scope.selectedRepository, paths).then(() => {
                        $scope.$evalAsync(() => {
                            $scope.loadRepositoryStatus();
                        });
                    }, (response) => {
                        console.error(response);
                        dialogHub.showAlert({
                            title: 'Could not revert file(s)',
                            message: response.message || 'There was an error during the reversal of file states.',
                            type: AlertTypes.Error,
                            preformatted: false,
                        });
                    }).finally(() => {
                        dialogHub.closeBusyDialog();
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

    $scope.push = (afterCommit = false) => {
        dialogHub.showBusyDialog('Pushing...');
        GitService.pushRepository(
            $scope.selectedWorkspace,
            $scope.selectedRepository,
            '',
            $scope.commitData.username,
            $scope.commitData.email,
            $scope.commitData.password,
        ).then(() => {
            notificationHub.show({
                type: 'positive',
                title: 'Push successful',
                description: `Pushed all commits from '${$scope.selectedRepository}'.`,
            });
            if (afterCommit) {
                $scope.$evalAsync(() => {
                    $scope.commitData.commitMessage = '';
                    $scope.loadRepositoryStatus();
                });
            }
        }, (response) => {
            console.error(response);
            dialogHub.showAlert({
                title: 'Could not push repository',
                message: response.message || 'There was an error while pushing the changes.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        }).finally(() => {
            dialogHub.closeBusyDialog();
        });
    };

    $scope.commit = (push = false) => {
        dialogHub.showBusyDialog('Committing changes...');
        GitService.commit(
            $scope.selectedWorkspace,
            $scope.selectedRepository,
            $scope.commitData.commitMessage,
            $scope.commitData.username,
            $scope.commitData.password,
            $scope.commitData.email,
        ).then(() => {
            if (push) {
                $scope.push(true);
            } else {
                $scope.$evalAsync(() => {
                    $scope.commitData.commitMessage = '';
                    $scope.loadRepositoryStatus();
                });
            }
        }, (response) => {
            console.error(response);
            dialogHub.showAlert({
                title: 'Could not commit changes',
                message: response.message || 'There was an error while committing the changes.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        }).finally(() => {
            if (!push) dialogHub.closeBusyDialog();
        });
    };

    // $scope.changeListType = (type) => {
    //     $scope.listType = type;
    //     $scope.jstreeUnstaged.jstree(true).refresh();
    //     $scope.jstreeStaged.jstree(true).refresh();
    // };

    $scope.loadRepositoryStatus = () => {
        $scope.loadingBranches = true;
        $scope.unstagedFiles.length = 0;
        $scope.stagedFiles.length = 0;
        GitService.getUnstagedFiles($scope.selectedWorkspace, $scope.selectedRepository).then(
            (response) => {
                $scope.$evalAsync(() => {
                    $scope.unstagedFiles = response.data.files;
                    $scope.jstreeUnstaged.jstree(true).refresh();
                });
            }, (response) => {
                dialogHub.showAlert({
                    title: 'Could not get unstaged files',
                    message: response.message || 'There was an error while getting the unstaged files.',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            }
        ).finally(() => {
            $scope.$evalAsync(() => {
                $scope.loadingBranches = false;
            });
        });
        GitService.getStagedFiles($scope.selectedWorkspace, $scope.selectedRepository).then(
            (response) => {
                $scope.$evalAsync(() => {
                    $scope.stagedFiles = response.data.files;
                    $scope.jstreeStaged.jstree(true).refresh();
                });
            }, (response) => {
                console.error(response);
                dialogHub.showAlert({
                    title: 'Could not get staged files',
                    message: response.message || 'There was an error while getting the staged files.',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            }
        ).finally(() => {
            $scope.$evalAsync(() => {
                $scope.loadingBranches = false;
            });
        });
    };

    $scope.clearLists = () => {
        $scope.selectedWorkspace = '';
        $scope.selectedRepository = '';
        $scope.unstagedFiles.length = 0;
        $scope.stagedFiles.length = 0;
        $scope.jstreeUnstaged.jstree(true).refresh();
        $scope.jstreeStaged.jstree(true).refresh();
    };

    const repoSelectedListener = notificationHub.addMessageListener({
        topic: 'git.repository.selected',
        handler: (data) => {
            if ($scope.selectedWorkspace !== data.workspace || $scope.selectedRepository !== data.project) {
                $scope.$evalAsync(() => {
                    if (data.isGitProject) {
                        $scope.selectedWorkspace = data.workspace;
                        $scope.selectedRepository = data.project;
                        $scope.loadRepositoryStatus();
                    } else {
                        $scope.clearList();
                    }
                });
            }
        }
    });

    $scope.$on('$destroy', () => {
        notificationHub.removeMessageListener(repoSelectedListener);
    });
});