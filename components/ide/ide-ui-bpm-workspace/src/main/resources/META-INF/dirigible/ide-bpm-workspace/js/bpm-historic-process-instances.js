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
let ideBpmHistoricProcessInstancesView = angular.module('ide-bpm-historic-process-instances', ['ideUI', 'ideView']);

ideBpmHistoricProcessInstancesView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'bpm';
}]);

ideBpmHistoricProcessInstancesView.controller('IDEBpmHistoricProcessInstancesViewController', ['$scope', '$http', '$timeout', 'messageHub', function ($scope, $http, $timeout, messageHub) {

    $scope.instances = [];
    $scope.model = {};
    $scope.model.searchText = '';
    $scope.displaySearch = false;
    $scope.selectedProcessDefinitionKey = null;

    setInterval(() => {
        $scope.fetchData();
    }, 5000);

    $scope.reload = function () {
        console.log("Reloading historic process instances data")
        $scope.fetchData();
    };

    $scope.fetchData = function() {
        $http.get('/services/bpm/bpm-processes/historic-instances', { params: { 'businessKey': $scope.model.searchText, 'definitionKey': $scope.selectedProcessDefinitionKey, 'limit': 100 } })
                .then((response) => {
                    $scope.instances = response.data;
                });
    }

    $scope.getNoDataMessage = function () {
        return 'No historic instances have been found.';
    }

    $scope.selectionChanged = function (instance) {
        messageHub.postMessage('historic.instance.selected', { instance: instance.id });
    }

     $scope.toggleSearch = function () {
         $scope.displaySearch = !$scope.displaySearch;
    }

    $scope.applyFilter = function () {
        $http.get('/services/bpm/bpm-processes/historic-instances', { params: { 'businessKey': $scope.model.searchText, 'definitionKey': $scope.selectedProcessDefinitionKey, 'limit': 100 } })
            .then((response) => {
                $scope.instances = response.data;
            });
    }

    messageHub.onDidReceiveMessage('definition.selected', function (msg) {
        $scope.$apply(function () {
            if (msg.data.hasOwnProperty('definition')) {
                $scope.selectedProcessDefinitionKey = msg.data.definition;
                $scope.applyFilter();
            } else {
                console.log("Process definition is missing from event!")
            }
        });
    });

    $scope.inputSearchKeyUp = function (e) {
        switch (e.key) {
            case 'Escape':
                $scope.model.searchText = '';
                break;
            case 'Enter':
                $scope.applyFilter();
                break;
        }
    }
}]);