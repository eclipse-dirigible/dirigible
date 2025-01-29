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
const ideBpmProcessContextView = angular.module('ide-bpm-process-context', ['platformView', 'blimpKit']);
ideBpmProcessContextView.constant('Dialogs', new DialogHub());
ideBpmProcessContextView.controller('IDEBpmProcessContextViewController', ($scope, $http, Dialogs) => {
    $scope.variablesList = [];
    $scope.currentProcessInstanceId = null;
    $scope.selectedVariable = null;
    $scope.disableModificationButtons = false;
    $scope.servicePath = '/services/bpm/bpm-processes/instance/'

    $scope.selectionChanged = (variable) => {
        $scope.variablesList.forEach(variable => variable.selected = false);
        $scope.selectedVariable = variable;
        $scope.selectedVariable.selected = true;
    };

    $scope.reload = () => {
        // console.log("Reloading data for current process instance id: " + $scope.currentProcessInstanceId)
        $scope.fetchData($scope.currentProcessInstanceId);
    };

    $scope.fetchData = (processInstanceId) => {
        $http.get($scope.servicePath + processInstanceId + '/variables', { params: { 'limit': 100 } })
            .then((response) => {
                $scope.variablesList = response.data;
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
                    label: 'Name',
                    controlType: 'input',
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
                    label: 'Name',
                    controlType: 'input',
                    type: 'text',
                    placeholder: 'Value',
                    value: `${$scope.selectedVariable.value}`,
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

    Dialogs.addMessageListener({
        topic: 'bpm.instance.selected',
        handler: (data) => {
            const processInstanceId = data.instance;
            $scope.$evalAsync(() => {
                $scope.currentProcessInstanceId = processInstanceId;
                $scope.disableModificationButtons = false;
                $scope.servicePath = '/services/bpm/bpm-processes/instance/';
            });
            $scope.fetchData(processInstanceId);
        }
    });

    Dialogs.addMessageListener({
        topic: 'bpm.historic.instance.selected', handler: (data) => {
            const processInstanceId = data.instance;
            $scope.$evalAsync(() => {
                $scope.currentProcessInstanceId = processInstanceId;
                $scope.disableModificationButtons = true;
                $scope.servicePath = '/services/bpm/bpm-processes/historic-instances/';
            });
            $scope.fetchData(processInstanceId);
        }
    });
});