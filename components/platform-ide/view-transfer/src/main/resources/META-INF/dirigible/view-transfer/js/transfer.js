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
const transferView = angular.module('transfer', ['blimpKit', 'platformView']);
transferView.controller('TransferController', ($scope, $http) => {
    const statusBarHub = new StatusBarHub();
    const fallbackMsg = 'Encountered a transfer error. See console.';

    let databasesSvcUrl = "/services/data/definition/";
    let transferWsUrl = "/websockets/data/transfer";

    $scope.databases = [];
    $scope.definition = {};
    $scope.definition.selectedSourceDatabase = 0;
    $scope.definition.selectedTargetDatabase = 0;
    $scope.sourceDatasources = [];
    $scope.targetDatasources = [];
    $scope.definition.selectedSourceDatasource = 0;
    $scope.definition.selectedTargetDatasource = 0;
    $scope.sourceSchemes = [];
    $scope.targetSchemes = [];
    $scope.definition.selectedSourceScheme = 0;
    $scope.definition.selectedTargetScheme = 0;

    $scope.allLogs = [];
    $scope.logs = [];
    $scope.autoScroll = true;

    let transferWebsocket = null;

    $scope.getDatabases = () => {
        $http.get(databasesSvcUrl).then((data) => {
            if (data.data.length > 0) {
                for (let i = 0; i < data.data.length; i++) {
                    $scope.databases.push({ text: data.data[i], value: i });
                }
                if ($scope.databases[$scope.definition.selectedSourceDatabase]) {
                    $http.get(databasesSvcUrl + $scope.databases[$scope.definition.selectedSourceDatabase].text).then((sourceData) => {
                        if (sourceData.data.length > 0) {
                            for (let i = 0; i < sourceData.data.length; i++) {
                                $scope.sourceDatasources.push({ text: sourceData.data[i], value: i });
                                $scope.targetDatasources.push({ text: sourceData.data[i], value: i });
                            }
                        }
                    });
                }
            }
        });
    };

    setTimeout($scope.getDatabases, 500); // This is absolutely awful. Must be fixed.
    connect();

    $scope.databaseSourceChanged = () => {
        $http.get(databasesSvcUrl + $scope.databases[$scope.definition.selectedSourceDatabase].text)
            .then((data) => {
                $scope.sourceDatasources.length = 0;
                for (let i = 0; i < data.data.length; i++) {
                    $scope.sourceDatasources.push({ text: data.data[i], value: i });
                }
                if ($scope.sourceDatasources.length > 0) {
                    $scope.definition.selectedSourceDatasource = 0;
                } else {
                    $scope.definition.selectedSourceDatasource = undefined;
                }
                //$scope.refreshDatabase();
            });
    };

    $scope.databaseTargetChanged = () => {
        $http.get(databasesSvcUrl + $scope.databases[$scope.definition.selectedTargetDatabase].text)
            .then((data) => {
                $scope.targetDatasources.length = 0;
                for (let i = 0; i < data.data.length; i++) {
                    $scope.targetDatasources.push({ text: data.data[i], value: i });
                }
                if ($scope.targetDatasources.length > 0) {
                    $scope.definition.selectedTargetDatasource = 0;
                } else {
                    $scope.definition.selectedDatasource = undefined;
                }
                //$scope.refreshDatabase();
            });
    };

    $scope.startTransfer = () => {
        if ($scope.databases[$scope.definition.selectedSourceDatabase]
            && $scope.sourceDatasources[$scope.definition.selectedSourceDatasource]
            && $scope.databases[$scope.definition.selectedTargetDatabase]
            && $scope.targetDatasources[$scope.definition.selectedTargetDatasource]) {
            const config = {
                "source": $scope.databases[$scope.definition.selectedSourceDatabase].text,
                "target": $scope.databases[$scope.definition.selectedTargetDatabase].text,
                "configuration": {
                    "sourceSchema": $scope.sourceDatasources[$scope.definition.selectedSourceDatasource].text,
                    "targetSchema": $scope.targetDatasources[$scope.definition.selectedTargetDatasource].text
                }
            };

            transferData(config);
        }
    };

    function transferData(config) {
        $scope.clearLog();
        transferWebsocket.send(JSON.stringify(config));
    }

    function connect() {
        try {
            transferWebsocket = new WebSocket(
                ((location.protocol === 'https:') ? "wss://" : "ws://")
                + window.location.host
                + window.location.pathname.substr(0, window.location.pathname.indexOf('/services/'))
                + transferWsUrl);
        } catch (e) {
            consoleLogMessage({
                message: e.message,
                level: "ERROR",
                date: new Date().toISOString()
            });
        }
        if (transferWebsocket) {
            transferWebsocket.onmessage = (message) => {
                let level = "INFO";
                if (message.data.indexOf("[ERROR]") >= 0) {
                    level = "ERROR";
                } else if (message.data.indexOf("[WARNING]") >= 0) {
                    level = "WARN";
                }
                consoleLogMessage({
                    message: message.data,
                    level: level,
                    date: new Date().toISOString()
                });

                if (level === "ERROR" || level === "WARN") {
                    statusBarHub.showError(message.data || fallbackMsg);
                }
            };

            transferWebsocket.onerror = (error) => {
                consoleLogMessage({
                    message: error.data,
                    level: "ERROR",
                    date: new Date().toISOString()
                });
                statusBarHub.showError(error.data || fallbackMsg);
            };

        }
    }

    function consoleLogMessage(record) {
        $scope.$evalAsync(() => {
            $scope.logs.push(record);
        });
    }

    $scope.clearLog = () => {
        $scope.logs.length = 0;
    };
});