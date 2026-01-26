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
const tasksView = angular.module('tasks', ['platformView', 'blimpKit']);
tasksView.constant('Notifications', new NotificationHub());
tasksView.constant('Dialogs', new DialogHub());
tasksView.controller('TasksController', ($scope, $http, $window, Dialogs, Notifications) => {
    $scope.state = {
        loadingGroups: false,
        loadingAssignee: false,
        busyText: 'Loading...',
    };
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
                $scope.state.loadingGroups = false;
            }, (error) => {
                console.error(error);
            });

        $http.get('/services/bpm/bpm-processes/instance/' + processInstanceId + '/tasks?type=assignee', { params: { 'limit': 100 } })
            .then((response) => {
                $scope.tasksListAssignee = response.data;
                $scope.state.loadingAssignee = false;
            }, (error) => {
                console.error(error);
            });
    };

    Dialogs.addMessageListener({
        topic: 'bpm.instance.selected',
        handler: (data) => {
            $scope.$evalAsync(() => {
                if (data.deselect) {
                    $scope.tasksList.length = 0;
                    $scope.tasksListAssignee.length = 0;
                    $scope.currentProcessInstanceId = null;
                    $scope.selectedClaimTask = null;
                    $scope.selectedUnclaimTask = null;
                } else {
                    $scope.state.loadingGroups = true;
                    $scope.state.loadingAssignee = true;
                    $scope.currentProcessInstanceId = data.instance;
                    $scope.fetchData(data.instance);
                }
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
        $scope.executeAction($scope.selectedClaimTask.id, { 'action': 'CLAIM' }, true, () => { $scope.selectedClaimTask = null });
    };

    $scope.unclaimTask = () => {
        $scope.executeAction($scope.selectedUnclaimTask.id, { 'action': 'UNCLAIM' }, false, () => { $scope.selectedUnclaimTask = null });
    };

    $scope.executeAction = (taskId, requestBody, claimed, clearCallback) => {
        const apiUrl = '/services/bpm/bpm-processes/tasks/' + taskId;

        $http({
            method: 'POST',
            url: apiUrl,
            data: requestBody,
            headers: { 'Content-Type': 'application/json' }
        }).then(() => {
            Notifications.show({
                title: 'Action confirmation',
                description: `Task ${claimed ? 'claimed' : 'unclaimed'} successfully!`,
                type: 'positive'
            });
            $scope.reload();
            clearCallback();
        }).catch((error) => {
            console.error('Error making POST request:', error);
            Dialogs.showAlert({
                title: 'Action failed',
                message: `Failed to ${claimed ? 'claim' : 'unclaim'} task ${error.message}`,
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };
});