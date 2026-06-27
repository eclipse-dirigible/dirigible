/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
        busyText: 'Loading...',
    };
    $scope.editScopeIndex = 0;

    angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

    const loadFileContents = () => {
        if (!$scope.state.error) {
            $scope.state.isBusy = true;
            WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
                $scope.$evalAsync(() => {
                    if (response.data === '') $scope.scopes = [];
                    else $scope.scopes = response.data;
                    contents = JSON.stringify($scope.scopes, null, 4);
                    $scope.state.isBusy = false;
                });
            }, (response) => {
                console.error(response);
                if (response && response.status === 404) {
                    // The file no longer exists (e.g. the workspace was cleaned by a rebuild) - close the stale editor.
                    layoutHub.closeEditor({ path: $scope.dataParameters.filePath });
                    return;
                }
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
                saveContents(JSON.stringify($scope.scopes, null, 4));
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

    $scope.$watch('scopes', () => {
        if (!$scope.state.error && !$scope.state.isBusy) {
            const isDirty = contents !== JSON.stringify($scope.scopes, null, 4);
            if ($scope.changed !== isDirty) {
                $scope.changed = isDirty;
                layoutHub.setEditorDirty({
                    path: $scope.dataParameters.filePath,
                    dirty: isDirty,
                });
            }
        }
    }, true);

    $scope.addScope = () => {
        dialogHub.showFormDialog({
            title: 'Add scope',
            form: {
                'seciScope': {
                    label: 'Scope name',
                    controlType: 'input',
                    placeholder: 'Enter scope name',
                    type: 'text',
                    minlength: 1,
                    maxlength: 255,
                    inputRules: {
                        patterns: ['^[a-zA-Z0-9_.-]*$'],
                    },
                    focus: true,
                    required: true
                },
                'seciRoles': {
                    label: 'Roles',
                    controlType: 'input',
                    placeholder: 'Comma separated roles',
                    type: 'text',
                    required: true,
                },
                'seciDescription': {
                    label: 'Description',
                    controlType: 'input',
                    placeholder: 'Enter description',
                    type: 'text',
                },
            },
            submitLabel: 'Add',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.scopes.push({
                        scope: form['seciScope'],
                        roles: form['seciRoles'].split(',').map(element => element.trim()).filter(element => element !== ''),
                        description: form['seciDescription'],
                    });
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'New scope error',
                message: 'There was an error while adding the new scope.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.editScope = (index) => {
        $scope.editScopeIndex = index;
        dialogHub.showFormDialog({
            title: 'Edit scope',
            form: {
                'seciScope': {
                    label: 'Scope name',
                    controlType: 'input',
                    placeholder: 'Enter scope name',
                    type: 'text',
                    minlength: 1,
                    maxlength: 255,
                    inputRules: {
                        patterns: ['^[a-zA-Z0-9_.-]*$'],
                    },
                    value: $scope.scopes[index].scope,
                    focus: true,
                    required: true
                },
                'seciRoles': {
                    label: 'Roles',
                    controlType: 'input',
                    placeholder: 'Comma separated roles',
                    type: 'text',
                    value: $scope.scopes[index].roles ? $scope.scopes[index].roles.join(', ') : '',
                    required: true,
                },
                'seciDescription': {
                    label: 'Description',
                    controlType: 'input',
                    placeholder: 'Enter description',
                    type: 'text',
                    value: $scope.scopes[index].description,
                },
            },
            submitLabel: 'Update',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.scopes[$scope.editScopeIndex].scope = form['seciScope'];
                    $scope.scopes[$scope.editScopeIndex].roles = form['seciRoles'].split(',').map(element => element.trim()).filter(element => element !== '');
                    $scope.scopes[$scope.editScopeIndex].description = form['seciDescription'];
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'Scope update error',
                message: 'There was an error while updating the scope.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.deleteScope = (index) => {
        dialogHub.showDialog({
            title: `Delete ${$scope.scopes[index].scope}?`,
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
                    $scope.scopes.splice(index, 1);
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
