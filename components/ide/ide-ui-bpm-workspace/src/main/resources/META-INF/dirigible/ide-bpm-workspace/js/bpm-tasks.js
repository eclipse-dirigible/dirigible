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
const tasksView = angular.module('tasks', ['ideUI', 'ideView']);

tasksView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'bpm';
}]);

tasksView.controller('TasksController', ['$scope','$http', '$timeout', '$window', 'messageHub', function ($scope, $http, $timeout, $window, messageHub) {
    $scope.tasksList = [];
    $scope.tasksListAssignee = [];
    $scope.currentProcessInstanceId;
    $scope.selectedClaimTask = null;
    $scope.selectedUnclaimTask = null;

    $scope.currentFetchDataTask = null;

    $scope.reload = function () {
        console.log("Reloading user tasks for current process instance id: " + $scope.currentProcessInstanceId)
        $scope.fetchData($scope.currentProcessInstanceId);
    };

    $scope.fetchData = function(processInstanceId) {
        $http.get('/services/bpm/bpm-processes/instance/' + processInstanceId + '/tasks?type=groups', { params: { 'limit': 100 } })
             .then((response) => {
                $scope.tasksList = response.data;
              });

        $http.get('/services/bpm/bpm-processes/instance/' + processInstanceId + '/tasks?type=assignee', { params: { 'limit': 100 } })
             .then((response) => {
                $scope.tasksListAssignee = response.data;
              });
    }

    messageHub.onDidReceiveMessage('instance.selected', function (msg) {
        const processInstanceId = msg.data.instance;
        $scope.fetchData(processInstanceId);
        $scope.$apply(function () {
            $scope.currentProcessInstanceId = processInstanceId;
        });
    });

    $scope.selectionClaimChanged = function (variable) {
        if (variable) {
            $scope.selectedClaimTask = variable;
        }
    }

    $scope.selectionUnclaimChanged = function (variable) {
        if (variable) {
            $scope.selectedUnclaimTask = variable;
        }
    }

    $scope.openForm = function(url) {
        $window.open(url, '_blank');
    }

    $scope.claimTask = function() {
        this.executeAction($scope.selectedClaimTask.id, { 'action': 'CLAIM'}, 'claimed', () => { $scope.selectedClaimTask = null });
    }

    $scope.unclaimTask = function() {
        this.executeAction($scope.selectedUnclaimTask.id, { 'action': 'UNCLAIM'}, 'unclaimed', () => { $scope.selectedUnclaimTask = null });
    }

    $scope.executeAction = function(taskId, requestBody, actionName, clearCallback)  {
        const apiUrl = '/services/bpm/bpm-processes/tasks/' + taskId;

        $http({
            method: 'POST',
            url: apiUrl,
            data: requestBody,
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then((response) => {
            messageHub.showAlertSuccess("Action confirmation", "Task " + actionName + " successfully!")
            $scope.reload();
            console.log('Successfully ' + actionName + ' task with id ' + taskId);
            clearCallback()
        })
        .catch((error) => {
            messageHub.showAlertError("Action failed", "Failed to " + actionName + " task " + error.message);
            console.error('Error making POST request:', error);
        });
    }

    $scope.getNoDataMessage = function () {
        return 'No tasks found.';
    }
}]);
