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
angular.module('page', ['blimpKit', 'platformView', 'platformShortcuts', 'WorkspaceService']).controller('PageController', ($scope, $window, WorkspaceService, ViewParameters, ButtonStates) => {
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    const layoutHub = new LayoutHub();
    const dialogHub = new DialogHub();
    let contents;
    $scope.changed = false;
    $scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: "Loading...",
    };
    $scope.editRoleIndex = 0;

    angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

    const loadFileContents = () => {
        if (!$scope.state.error) {
            $scope.state.isBusy = true;
            WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
                $scope.$evalAsync(() => {
                    if (response.data === '') $scope.roles = {};
                    else $scope.roles = response.data;
                    contents = JSON.stringify($scope.roles, null, 4);
                    $scope.state.isBusy = false;
                });
            }, (response) => {
                console.error(response);
                $scope.$evalAsync(() => {
                    $scope.state.error = true;
                    $scope.errorMessage = 'Error while loading file. Please look at the console for more information.';
                    $scope.state.isBusy = false;
                });
            });
        }
    };

    function saveContents(text) {
        WorkspaceService.saveContent($scope.dataParameters.filePath, text).then(() => {
            contents = text;
            layoutHub.setEditorDirty({
                path: $scope.dataParameters.filePath,
                dirty: false,
            });
            workspaceHub.announceFileSaved({
                path: $scope.dataParameters.filePath,
                contentType: $scope.dataParameters.contentType,
            });
            $scope.$evalAsync(() => {
                $scope.changed = false;
                $scope.state.isBusy = false;
            });
        }, (response) => {
            console.error(response);
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = `Error saving '${$scope.dataParameters.filePath}'. Please look at the console for more information.`;
                $scope.state.isBusy = false;
            });
        });
    }

    $scope.save = (keySet = 'ctrl+s', event) => {
        event?.preventDefault();
        if (keySet === 'ctrl+s') {
            if ($scope.changed && !$scope.state.error) {
                $scope.state.busyText = 'Saving...';
                $scope.state.isBusy = true;
                saveContents(JSON.stringify($scope.roles, null, 4));
            }
        }
    };

    layoutHub.onFocusEditor((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) statusBarHub.showLabel('');
    });

    layoutHub.onReloadEditorParams((data) => {
        if (data.path === $scope.dataParameters.filePath) {
            $scope.$evalAsync(() => {
                $scope.dataParameters = ViewParameters.get();
            });
        };
    });

    workspaceHub.onSaveAll(() => {
        if ($scope.changed && !$scope.state.error) {
            $scope.save();
        }
    });

    workspaceHub.onSaveFile((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) {
            if (!$scope.state.error && $scope.changed) {
                $scope.save();
            }
        }
    });

    $scope.$watch('roles', () => {
        if (!$scope.state.error && !$scope.state.isBusy) {
            const isDirty = contents !== JSON.stringify($scope.roles, null, 4);
            if ($scope.changed !== isDirty) {
                $scope.changed = isDirty;
                layoutHub.setEditorDirty({
                    path: $scope.dataParameters.filePath,
                    dirty: isDirty,
                });
            }
        }
    }, true);

    $scope.addRole = () => {
        dialogHub.showFormDialog({
            title: 'Add role',
            form: {
                'reriName': {
                    label: 'Ant path pattern',
                    controlType: 'input',
                    placeholder: 'Enter path',
                    type: 'text',
                    minlength: 1,
                    maxlength: 255,
                    inputRules: {
                        patterns: ['^[a-zA-Z0-9_.-]*$'],
                    },
                    focus: true,
                    required: true
                },
                'reriRoles': {
                    label: 'Description',
                    controlType: 'input',
                    placeholder: "Enter description",
                    type: 'text',
                },
            },
            submitLabel: 'Add',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.roles.push({
                        name: form['reriName'],
                        description: form['reriRoles'],
                    });
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'New role error',
                message: 'There was an error while adding the new role.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.editRole = (index) => {
        $scope.editRoleIndex = index;
        dialogHub.showFormDialog({
            title: 'Add role',
            form: {
                'reriName': {
                    label: 'Ant path pattern',
                    controlType: 'input',
                    placeholder: 'Enter path',
                    type: 'text',
                    minlength: 1,
                    maxlength: 255,
                    inputRules: {
                        patterns: ['^[a-zA-Z0-9_.-]*$'],
                    },
                    value: $scope.roles[index].name,
                    focus: true,
                    required: true
                },
                'reriRoles': {
                    label: 'Description',
                    controlType: 'input',
                    placeholder: "Enter description",
                    type: 'text',
                    value: $scope.roles[index].description,
                },
            },
            submitLabel: 'Update',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.roles[$scope.editRoleIndex].name = form['reriName'];
                    $scope.roles[$scope.editRoleIndex].description = form['reriRoles'];
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'Role update error',
                message: 'There was an error while updating the role.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.deleteRole = (index) => {
        dialogHub.showDialog({
            title: `Delete ${$scope.roles[index].name}?`,
            message: 'This action cannot be undone.',
            buttons: [{
                id: 'bd',
                state: ButtonStates.Negative,
                label: 'Delete',
            },
            {
                id: 'bc',
                state: ButtonStates.Transparent,
                label: 'Cancel',
            }]
        }).then((buttonId) => {
            if (buttonId === 'bd') {
                $scope.$evalAsync(() => {
                    $scope.roles.splice(index, 1);
                });
            }
        });
    };

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'filePath\' data parameter is missing.';
    } else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'contentType\' data parameter is missing.';
    } else loadFileContents();
});
