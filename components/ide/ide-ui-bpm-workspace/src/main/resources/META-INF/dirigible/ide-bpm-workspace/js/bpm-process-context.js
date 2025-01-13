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
let ideBpmProcessContextView = angular.module('ide-bpm-process-context', ['ideUI', 'ideView']);

ideBpmProcessContextView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'bpm';
}]);

ideBpmProcessContextView.controller('IDEBpmProcessContextViewController', ['$scope', '$http', '$timeout', 'messageHub', function ($scope, $http, $timeout, messageHub,) {

    $scope.variablesList = [];
    $scope.currentProcessInstanceId = null;
    $scope.selectedVariable = null;
    $scope.disableModificationButtons = false;
    $scope.servicePath = '/services/bpm/bpm-processes/instance/'

    $scope.selectionChanged = function (variable) {
        $scope.variablesList.forEach(variable => variable.selected = false);
        $scope.selectedVariable = variable;
        $scope.selectedVariable.selected = true;
    }

    $scope.reload = function () {
        console.log("Reloading data for current process instance id: " + $scope.currentProcessInstanceId)
        $scope.fetchData($scope.currentProcessInstanceId);
    };

    $scope.fetchData = function(processInstanceId) {
        $http.get($scope.servicePath + processInstanceId + '/variables', { params: { 'limit': 100 } })
                .then((response) => {
                    $scope.variablesList = response.data;
                });
    }

    $scope.upsertProcessVariable = function(processInstanceId, varName, varValue, dialogId) {
        const apiUrl = '/services/bpm/bpm-processes/instance/' + processInstanceId + '/variables';
        const requestBody = { 'name': varName, 'value': varValue };

        $http({
            method: 'POST',
            url: apiUrl,
            data: requestBody,
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then((response) => {
            console.log('Successfully modified variable with name [' + varName + '] and value [' + varValue + ']');
            $scope.reload();
            messageHub.hideFormDialog(dialogId);
        })
        .catch((error) => {
            console.error('Error making POST request:', error);
        });
    }

    $scope.getNoDataMessage = function () {
        return 'No variables have been detected.';
    }

    $scope.openAddDialog = function () {
        messageHub.showFormDialog(
            "processContextVariableAdd",
            "Add new process context variable",
            [{
                id: "prcva",
                type: "input",
                label: "Name",
                placeholder: 'Variable name'
            },
            {
                id: "prcvb",
                type: "input",
                label: "Value",
                placeholder: 'Variable value'
            }],
            [{
                id: "b1",
                type: "emphasized",
                label: "Add",
            },
            {
                id: "b2",
                type: "transparent",
                label: "Cancel",
            }],
            "bpm.dialogs.variable.add",
            "Saving..."
        );
    }

    $scope.openEditDialog = function () {
        messageHub.showFormDialog(
            "processContextVariableEdit",
            `Edit variable [${$scope.selectedVariable.name}]`,
            [{
                id: "prcvb",
                type: "input",
                label: "Value",
                value: `${$scope.selectedVariable.value}`
            }],
            [{
                id: "b1",
                type: "emphasized",
                label: "Save",
            },
            {
                id: "b2",
                type: "transparent",
                label: "Cancel",
            }],
            "bpm.dialogs.variable.edit",
            "Saving..."
        );
    }

    messageHub.onDidReceiveMessage(
        "bpm.dialogs.variable.edit",
        function (msg) {
            if (msg.data.buttonId === "b1") {
                const varValue = msg.data.formData[0].value;
                $scope.upsertProcessVariable($scope.currentProcessInstanceId, $scope.selectedVariable.name, varValue, "processContextVariableEdit");
            } else {
                messageHub.hideFormDialog("processContextVariableEdit");
            }
        },
        true
    );

    messageHub.onDidReceiveMessage(
        "bpm.dialogs.variable.add",
        function (msg) {
            if (msg.data.buttonId === "b1") {
                const varName = msg.data.formData[0].value;
                const varValue = msg.data.formData[1].value;
                $scope.upsertProcessVariable($scope.currentProcessInstanceId, varName, varValue, "processContextVariableAdd");
            } else {
                messageHub.hideFormDialog("processContextVariableAdd");
            }
        },
        true
    );

    messageHub.onDidReceiveMessage('instance.selected', function (msg) {
        const processInstanceId = msg.data.instance;
        $scope.$apply(function () {
            $scope.currentProcessInstanceId = processInstanceId;
            $scope.disableModificationButtons = false
            $scope.servicePath = '/services/bpm/bpm-processes/instance/'
        });
        $scope.fetchData(processInstanceId);
    });

    messageHub.onDidReceiveMessage('historic.instance.selected', function (msg) {
        const processInstanceId = msg.data.instance;
        $scope.$apply(function () {
            $scope.currentProcessInstanceId = processInstanceId;
            $scope.disableModificationButtons = true
            $scope.servicePath = '/services/bpm/bpm-processes/historic-instances/'
        });
        $scope.fetchData(processInstanceId);
    });

}]);