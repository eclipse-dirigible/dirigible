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
const loggersView = angular.module('loggers', ['blimpKit', 'platformView']);
loggersView.constant('Dialogs', new DialogHub());
loggersView.controller('LoggersController', ($scope, $http, $document, Dialogs) => {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };

    $scope.search = {
        name: '',
        searching: false
    };

    const loggersApi = '/services/ide/loggers/';

    $scope.clearSearch = () => {
        $scope.search.name = '';
        $scope.search.searching = false;
    };

    const findByName = () => {
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
    $scope.searchContent = () => {
        if (to) { clearTimeout(to); }
        to = setTimeout(() => {
            $scope.$evalAsync(() => findByName());
        }, 300);
    };

    $scope.setSeverity = (loggerIndex, loggerLevel) => {
        if ($scope.loggers[loggerIndex].name === 'ROOT' && $scope.loggers[loggerIndex].severity === loggerLevel) {
            Dialogs.showAlert({
                title: 'Action blocked',
                message: 'The \'ROOT\' logger cannot be disabled, you can only choose a different log level. This does not apply to other loggers.',
                type: AlertTypes.Warning,
                preformatted: false,
            });
        } else {
            $http({
                method: 'POST',
                url: loggersApi + 'severity/' + $scope.loggers[loggerIndex].name,
                data: loggerLevel,
                headers: { 'Content-Type': 'text/plain' }
            }).then((data) => {
                if ($scope.loggers[loggerIndex].severity === loggerLevel) {
                    $scope.loggers[loggerIndex].severity = '';
                } else {
                    $scope.loggers[loggerIndex].severity = data.data;
                }
            }, (errorData) => {
                console.error(errorData);
                Dialogs.showAlert({
                    title: 'Error while setting severity level',
                    message: `There was an error while setting '${loggerLevel}' level to '${$scope.loggers[loggerIndex].name}'.`,
                    type: AlertTypes.Warning,
                    preformatted: false,
                });
            })
        }
    };

    angular.element($document[0]).ready(() => {
        $http.get(loggersApi)
            .then((data) => {
                $scope.loggers = data.data;
                $scope.state.isBusy = false;
            }, (errorData) => {
                console.error(errorData);
                $scope.state.error = true;
            });
    });
});