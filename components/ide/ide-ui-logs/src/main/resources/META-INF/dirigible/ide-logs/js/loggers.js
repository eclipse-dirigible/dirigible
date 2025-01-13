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
const loggersView = angular.module('loggers', ['ideUI', 'ideView', 'ngRoute']);

loggersView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'loggers-view';
}]);

loggersView.controller('LoggersController', ['$scope', '$http', '$document', 'messageHub', function ($scope, $http, $document, messageHub) {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: "Loading...",
    };

    $scope.search = {
        name: '',
        searching: false
    };

    const loggersApi = '/services/ide/loggers/';

    $scope.clearSearch = function () {
        $scope.search.name = '';
        $scope.search.searching = false;
    };

    $scope.findByName = function () {
        if ($scope.search.name !== '') {
            $scope.search.searching = true;
            for (let i = 0; i < $scope.loggers.length; i++) {
                if ($scope.loggers[i].name.toLowerCase().includes($scope.search.name.toLowerCase())) {
                    $scope.loggers[i].hidden = false;
                } else {
                    $scope.loggers[i].hidden = true;
                }
            }
        } else $scope.search.searching = false;
    };

    let to = 0;
    $scope.searchContent = function () {
        if (to) { clearTimeout(to); }
        to = setTimeout(function () {
            $scope.findByName();
            $scope.$digest();
        }, 300);
    };

    $scope.setSeverity = function (loggerIndex, loggerLevel) {
        if ($scope.loggers[loggerIndex].name === 'ROOT' && $scope.loggers[loggerIndex].severity === loggerLevel) {
            messageHub.showAlertWarning(
                'Action blocked',
                "The 'ROOT' logger cannot be disabled, you can only choose a different log level. This does not apply to other loggers."
            );
        } else {
            $http({
                method: "POST",
                url: loggersApi + "severity/" + $scope.loggers[loggerIndex].name,
                data: loggerLevel,
                headers: { "Content-Type": "text/plain" }
            }).then(function (data) {
                if ($scope.loggers[loggerIndex].severity === loggerLevel) {
                    $scope.loggers[loggerIndex].severity = '';
                } else {
                    $scope.loggers[loggerIndex].severity = data.data;
                }
            }, function (errorData) {
                console.error(errorData);
                messageHub.showAlertError(
                    'Error while setting severity level',
                    `There was an error while setting '${loggerLevel}' level to '${$scope.loggers[loggerIndex].name}'.`
                );
            })
        }
    };

    angular.element($document[0]).ready(function () {
        $http.get(loggersApi)
            .then(function (data) {
                $scope.loggers = data.data;
                $scope.state.isBusy = false;
            }, function (errorData) {
                console.error(errorData);
                $scope.state.error = true;
            });
    });
}]);