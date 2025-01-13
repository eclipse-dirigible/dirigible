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
let bpmImageView = angular.module('bpm-image-app', ['ideUI', 'ideView']);

bpmImageView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'bpm';
}]);

bpmImageView.controller('BpmImageViewController', ['$scope', 'messageHub', function ($scope, messageHub) {
    $scope.imageLink = "";
    $scope.state = {
        isBusy: false,
        error: false,
        busyText: "Loading...",
    };

    $scope.loadDefinitionImageLink = function (definition) {
        $scope.imageLink = `/services/bpm/bpm-processes/diagram/definition/${definition}`;
        $scope.state.isBusy = false;
    };
    
    $scope.loadInstanceImageLink = function (instance) {
        $scope.imageLink = `/services/bpm/bpm-processes/diagram/instance/${instance}`;
        $scope.state.isBusy = false;
    };

    messageHub.onDidReceiveMessage('diagram.definition', function (msg) {
        $scope.$apply(function () {
            $scope.state.isBusy = true;
            if (!msg.data.hasOwnProperty('definition')) {
                $scope.state.error = true;
                $scope.errorMessage = "The 'definition' parameter is missing.";
            } else {
                $scope.state.error = false;
                $scope.loadDefinitionImageLink(msg.data.definition);
            }
        });
    });
    
    messageHub.onDidReceiveMessage('diagram.instance', function (msg) {
        $scope.$apply(function () {
            $scope.state.isBusy = true;
            if (!msg.data.hasOwnProperty('instance')) {
                $scope.state.error = true;
                $scope.errorMessage = "The 'definition' parameter is missing.";
            } else {
                $scope.state.error = false;
                $scope.loadInstanceImageLink(msg.data.instance);
            }
        });
    });
}]);