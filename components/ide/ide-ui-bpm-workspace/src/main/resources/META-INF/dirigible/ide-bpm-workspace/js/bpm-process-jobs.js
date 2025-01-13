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
let ideBpmProcessJobsView = angular.module('ide-bpm-process-jobs', ['ideUI', 'ideView']);

ideBpmProcessJobsView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'bpm';
}]);

ideBpmProcessJobsView.controller('IDEBpmProcessJobsViewController', ['$scope', '$http', '$timeout', 'messageHub', function ($scope, $http, $timeout,messageHub,) {

    $scope.jobsList = [];

    $scope.reload = function () {
        console.log("Reloading data for current process instance id: " + $scope.currentProcessInstanceId)
        $scope.fetchData($scope.currentProcessInstanceId);
    };

    $scope.fetchData = function(processInstanceId) {
        $http.get('/services/bpm/bpm-processes/instance/' + processInstanceId + '/jobs', { params: { 'limit': 100 } })
                .then((response) => {
                    $scope.jobsList = response.data;
                });
    }

    $scope.getNoDataMessage = function () {
        return 'No jobs have been found.';
    }

    $scope.openDialog = function(job) {
        messageHub.showAlertError(job.exceptionMessage, job.exceptionStacktrace);
    }

    messageHub.onDidReceiveMessage('instance.selected', function (msg) {
        const processInstanceId = msg.data.instance;
        $scope.fetchData(processInstanceId);
    });

}]);