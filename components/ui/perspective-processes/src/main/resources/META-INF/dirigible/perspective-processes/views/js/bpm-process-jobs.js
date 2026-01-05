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
const ideBpmProcessJobsView = angular.module('ide-bpm-process-jobs', ['platformView', 'blimpKit']);
ideBpmProcessJobsView.constant('Dialogs', new DialogHub());
ideBpmProcessJobsView.controller('IDEBpmProcessJobsViewController', ($scope, $http, Dialogs) => {
    $scope.jobsList = [];

    $scope.reload = () => {
        // console.log("Reloading data for current process instance id: " + $scope.currentProcessInstanceId)
        $scope.fetchData($scope.currentProcessInstanceId);
    };

    $scope.fetchData = (processInstanceId) => {
        $http.get('/services/bpm/bpm-processes/instance/' + processInstanceId + '/jobs', { params: { 'limit': 100 } })
            .then((response) => {
                $scope.jobsList = response.data;
            }, (error) => {
                console.error(error);
            });
    };

    $scope.getRelativeTime = (dateTimeString) => {
        return formatRelativeTime(new Date(dateTimeString));
    };

    $scope.openDialog = (job) => {
        Dialogs.showWindow({
            id: 'bpm-process-jobs-details',
            params: {
                job: job,
            },
            closeButton: true,
        });
    };

    Dialogs.addMessageListener({
        topic: 'bpm.instance.selected',
        handler: (data) => {
            if (data.deselect) {
                $scope.$evalAsync(() => {
                    $scope.jobsList.length = 0;
                    $scope.currentProcessInstanceId = null;
                });
            } else {
                $scope.fetchData(data.instance);
            }
        }
    });

    Dialogs.addMessageListener({
        topic: 'bpm.historic.instance.selected',
        handler: (data) => {
            if (data.deselect) {
                $scope.$evalAsync(() => {
                    $scope.jobsList.length = 0;
                    $scope.currentProcessInstanceId = null;
                });
            } else {
                $scope.fetchData(data.instance);
            }
        }
    });
});