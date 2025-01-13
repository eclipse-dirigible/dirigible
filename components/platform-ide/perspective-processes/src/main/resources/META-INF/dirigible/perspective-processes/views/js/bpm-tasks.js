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
const tasksView = angular.module('tasks', ['platformView', 'blimpKit']);
tasksView.constant('Dialogs', new DialogHub());
tasksView.controller('TasksController', ($scope, $http, $window, Dialogs) => {
    $scope.tasksList = [];
    $scope.tasksListAssignee = [];
    $scope.currentProcessInstanceId;
    $scope.selectedClaimTask = null;
    $scope.selectedUnclaimTask = null;

    $scope.currentFetchDataTask = null;

    $scope.reload = () => {
        // console.log("Reloading user tasks for current process instance id: " + $scope.currentProcessInstanceId)
        $scope.fetchData($scope.currentProcessInstanceId);
    };

    $scope.fetchData = (processInstanceId) => {
        $http.get('/services/bpm/bpm-processes/instance/' + processInstanceId + '/tasks?type=groups', { params: { 'limit': 100 } })
            .then((response) => {
                $scope.tasksList = response.data;
            });

        $http.get('/services/bpm/bpm-processes/instance/' + processInstanceId + '/tasks?type=assignee', { params: { 'limit': 100 } })
            .then((response) => {
                $scope.tasksListAssignee = response.data;
            });
    };

    Dialogs.addMessageListener({
        topic: 'bpm.instance.selected',
        handler: (data) => {
            const processInstanceId = data.instance;
            $scope.fetchData(processInstanceId);
            $scope.$evalAsync(() => {
                $scope.currentProcessInstanceId = processInstanceId;
            });
        }
    });

    $scope.selectionClaimChanged = (variable) => {
        if (variable) $scope.selectedClaimTask = variable;
    };

    $scope.selectionUnclaimChanged = (variable) => {
        if (variable) $scope.selectedUnclaimTask = variable;
    };

    $scope.openForm = (url) => {
        $window.open(url, '_blank');
    };

    $scope.claimTask = () => {
        $scope.executeAction($scope.selectedClaimTask.id, { 'action': 'CLAIM' }, 'claimed', () => { $scope.selectedClaimTask = null });
    };

    $scope.unclaimTask = () => {
        $scope.executeAction($scope.selectedUnclaimTask.id, { 'action': 'UNCLAIM' }, 'unclaimed', () => { $scope.selectedUnclaimTask = null });
    };

    $scope.executeAction = (taskId, requestBody, actionName, clearCallback) => {
        const apiUrl = '/services/bpm/bpm-processes/tasks/' + taskId;

        $http({
            method: 'POST',
            url: apiUrl,
            data: requestBody,
            headers: { 'Content-Type': 'application/json' }
        }).then(() => {
            Dialogs.showAlert({
                title: 'Action confirmation',
                message: "Task " + actionName + " successfully!",
                type: AlertTypes.Success,
                preformatted: false,
            });
            $scope.reload();
            // console.log('Successfully ' + actionName + ' task with id ' + taskId);
            clearCallback();
        }).catch((error) => {
            console.error('Error making POST request:', error);
            Dialogs.showAlert({
                title: 'Action failed',
                message: "Failed to " + actionName + " task " + error.message,
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };
});