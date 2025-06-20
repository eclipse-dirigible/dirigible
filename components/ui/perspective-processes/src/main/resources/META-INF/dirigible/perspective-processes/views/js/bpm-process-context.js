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
const ideBpmProcessContextView = angular.module('ide-bpm-process-context', ['platformView', 'blimpKit']);
ideBpmProcessContextView.constant('Dialogs', new DialogHub());
ideBpmProcessContextView.controller('IDEBpmProcessContextViewController', ($scope, $http, Dialogs, ButtonStates) => {
    $scope.variablesList = [];
    $scope.searchField = { text: '' };
    $scope.displaySearch = false;
    $scope.currentProcessInstanceId = null;
    $scope.selectedVariable = null;
    $scope.disableModificationButtons = false;
    let servicePath = '/services/bpm/bpm-processes/instance/';

    $scope.selectionChanged = (variable) => {
        $scope.selectedVariable = variable;
    };

    $scope.toggleSearch = () => {
        $scope.displaySearch = !$scope.displaySearch;
    };

    $scope.reload = () => {
        // console.log("Reloading data for current process instance id: " + $scope.currentProcessInstanceId)
        $scope.fetchData($scope.currentProcessInstanceId);
        $scope.selectedVariable = null;
    };

    $scope.fetchData = (processInstanceId) => {
        $http.get(
            servicePath + processInstanceId + '/variables',
            { params: { 'variableName': $scope.searchField.text, 'limit': 100 } }
        ).then((response) => {
            $scope.variablesList = response.data;
            $scope.variablesList.sort((a, b) => a.name < b.name ? -1 : 1);
        }, (error) => {
            console.error(error);
        });
    };

    $scope.openDialog = (variable) => {
        Dialogs.showWindow({
            id: 'bpm-process-context-details',
            params: {
                variable: variable,
            },
            closeButton: true,
        });
    };

    $scope.upsertProcessVariable = (processInstanceId, varName, varValue) => {
        const apiUrl = '/services/bpm/bpm-processes/instance/' + processInstanceId + '/variables';
        const requestBody = { 'name': varName, 'value': varValue };

        $http({
            method: 'POST',
            url: apiUrl,
            data: requestBody,
            headers: { 'Content-Type': 'application/json' }
        }).then(() => {
            // console.log('Successfully modified variable with name [' + varName + '] and value [' + varValue + ']');
            $scope.reload();
        }).catch((error) => {
            console.error('Error making POST request:', error);
            Dialogs.showAlert({
                title: 'Request error',
                message: 'Please look at the console for more information',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.removeProcessVariable = (executionId, variableName) => {
        const apiUrl = `/services/bpm/bpm-processes/execution/${executionId}/variables/${variableName}`;

        $http({
            method: 'DELETE',
            url: apiUrl,
        }).then(() => {
            // console.log('Successfully modified variable with name [' + varName + '] and value [' + varValue + ']');
            $scope.reload();
        }).catch((error) => {
            console.error('Error making DELETE request:', error);
            Dialogs.showAlert({
                title: 'Request error',
                message: 'Please look at the console for more information',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.openAddDialog = () => {
        Dialogs.showFormDialog({
            title: 'Add new process context variable',
            form: {
                'prcva': {
                    label: 'Name',
                    controlType: 'input',
                    type: 'text',
                    placeholder: 'Variable name',
                    focus: true,
                    required: true
                },
                'prcvb': {
                    label: 'Value',
                    controlType: 'textarea',
                    rows: 6,
                    type: 'text',
                    placeholder: 'Variable value',
                    submitOnEnter: true,
                    required: true
                },
            },
            submitLabel: 'Add',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.upsertProcessVariable($scope.currentProcessInstanceId, form['prcva'], form['prcvb']);
            }
        }, (error) => {
            console.error(error);
            Dialogs.showAlert({
                title: 'Add variable error',
                message: 'There was an error while adding the new variable.\nPlease look at the console for more information.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    }

    $scope.openEditDialog = () => {
        Dialogs.showFormDialog({
            title: `Edit variable [${$scope.selectedVariable.name}]`,
            form: {
                'prcvb': {
                    label: 'Value',
                    controlType: 'textarea',
                    rows: 6,
                    type: 'text',
                    placeholder: 'Variable value',
                    value: `${stringifyValue($scope.selectedVariable.value)}`,
                    submitOnEnter: true,
                    focus: true,
                    required: true
                },
            },
            submitLabel: 'Add',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.upsertProcessVariable($scope.currentProcessInstanceId, $scope.selectedVariable.name, form['prcvb']);
            }
        }, (error) => {
            console.error(error);
            Dialogs.showAlert({
                title: 'Add variable error',
                message: 'There was an error while adding the new variable.\nPlease look at the console for more information.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.openRemoveDialog = () => {
        Dialogs.showDialog({
            title: `Remove variable [${$scope.selectedVariable.name}]`,
            message: `Are you sure you want to remove variable ${$scope.selectedVariable.name}? This action cannot be undone.`,
            buttons: [{
                id: 'delete-btn-yes',
                state: ButtonStates.Emphasized,
                label: 'Yes',
            }, {
                id: 'delete-btn-no',
                label: 'No',
            }]
        }).then((buttonId) => {
            if (buttonId === 'delete-btn-yes') {
                $scope.removeProcessVariable($scope.selectedVariable.executionId, $scope.selectedVariable.name);
            }
        });
    };

    function deselect() {
        $scope.variablesList.length = 0;
        $scope.currentProcessInstanceId = null;
        $scope.selectedVariable = null;
        $scope.disableModificationButtons = true;
    }

    Dialogs.addMessageListener({
        topic: 'bpm.instance.selected',
        handler: (data) => {
            if (data.deselect) {
                $scope.$evalAsync(deselect);
            } else {
                const processInstanceId = data.instance;
                servicePath = '/services/bpm/bpm-processes/instance/';
                $scope.$evalAsync(() => {
                    $scope.currentProcessInstanceId = processInstanceId;
                    $scope.disableModificationButtons = false;
                });
                $scope.fetchData(processInstanceId);
            }
        }
    });

    Dialogs.addMessageListener({
        topic: 'bpm.historic.instance.selected', handler: (data) => {
            if (data.deselect) {
                $scope.$evalAsync(deselect);
            } else {
                const processInstanceId = data.instance;
                servicePath = '/services/bpm/bpm-processes/historic-instances/';
                $scope.$evalAsync(() => {
                    $scope.currentProcessInstanceId = processInstanceId;
                    $scope.disableModificationButtons = true;

                });
                $scope.fetchData(processInstanceId);
            }
        }
    });

    $scope.inputSearchKeyUp = (e) => {
        switch (e.key) {
            case 'Escape':
                $scope.searchField.text = '';
                toggleSearch();
                $scope.fetchData($scope.currentProcessInstanceId);
                break;
            case 'Enter':
                $scope.fetchData($scope.currentProcessInstanceId);
                break;
        }
    };
});