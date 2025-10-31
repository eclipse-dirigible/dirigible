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
const transferView = angular.module('transfer', ['blimpKit', 'platformView']);
const DB_SERVICE_URL = '/services/data/definition/';
const EXPORT_SERVICE_URL = '/services/data/schema/exportProcesses';
transferView.controller('ViewController', ($scope) => {
    $scope.navActive = 'export';

    $scope.switchNav = (nav) => {
        $scope.navActive = nav;
    };
});

transferView.controller('ExportController', ($scope, $http, $timeout) => {
    const dialogHub = new DialogHub();
    $scope.forms = { ep: {} };
    $scope.exportData = {
        db: '',
        source: '',
        path: '',
        type: 'include',
        tables: [],
    };
    $scope.databases = [];
    $scope.datasources = [];
    $scope.tables = [];
    $scope.exportLoading = false;
    $scope.exports = [];

    $scope.choosePath = () => {
        dialogHub.showWindow({
            id: 'windowDocuments',
            hasHeader: false,
            params: {
                type: 'folderSelect',
                upload: false,
                download: false,
                multiple: false,
                callbackTopic: 'transfer.export.folder'
            },
            maxWidth: '960px',
            maxHeight: '540px',
            closeButton: true
        });
    };

    const folderHandler = dialogHub.addMessageListener({
        topic: 'transfer.export.folder',
        handler: (path) => {
            $scope.$evalAsync(() => {
                $scope.exportData.path = path;
            });
        },
    });

    $scope.dbChanged = () => {
        $http.get(DB_SERVICE_URL + $scope.exportData.db).then((sourceData) => {
            if (sourceData.data.length > 0) {
                $scope.datasources.length = 0;
                $scope.datasources.push(...sourceData.data);
                $scope.exportData.source = $scope.datasources[0];
                $scope.tables.length = 0;
                $scope.exportData.tables.length = 0;
                getTables();
            } else $scope.exportData.source = '';
        }, (error) => {
            console.error(error);
        });
    };

    $scope.sourceChanged = () => {
        $scope.tables.length = 0;
        $scope.exportData.tables.length = 0;
        if ($scope.exportData.source) {
            getTables();
        }
    };

    $scope.getExports = (historic = false) => {
        if (!historic) $scope.exports.length = 0;
        $http.get(historic ? '/services/bpm/bpm-processes/historic-instances' : '/services/bpm/bpm-processes/instances', { params: { definitionKey: 'export-schema', limit: 100 } })
            .then((response) => {
                for (let i = 0; i < response.data.length; i++) {
                    $http.get(historic ? `/services/bpm/bpm-processes/historic-instances/${response.data[i].id}/variables` : `/services/bpm/bpm-processes/instances/${response.data[i].id}/variables`, { params: { 'limit': 100 } }).then((varList) => {
                        let schema = '';
                        let dataSource = '';
                        let path = '';
                        let time = '';
                        for (let l = 0; l < varList.data.length; l++) {
                            if (varList.data[l]['name'] === 'schema') {
                                schema = varList.data[l]['value'];
                                time = varList.data[l]['createTime'];
                            } else if (varList.data[l]['name'] === 'dataSource') dataSource = varList.data[l]['value'];
                            else if (varList.data[l]['name'] === 'exportPath') path = varList.data[l]['value'];
                        }
                        $scope.exports.push({
                            id: response.data[i].id,
                            source: schema,
                            db: dataSource,
                            path: path,
                            exporting: !historic,
                            time: new Intl.DateTimeFormat(undefined, {
                                dateStyle: 'short',
                                timeStyle: 'short',
                            }).format(new Date(time))
                        });

                    }, (error) => {
                        console.error(error);
                    });
                }
                if (!historic) {
                    $scope.getExports(true);
                    if (response.data.length) $timeout(() => { $scope.getExports() }, 1000);
                }
            }, (error) => {
                console.error(error);
            });
    };

    $scope.startExport = () => {
        $scope.exportLoading = true;
        $http.post(EXPORT_SERVICE_URL, {
            dataSource: $scope.exportData.db,
            schema: $scope.exportData.source,
            exportPath: $scope.exportData.path,
            includedTables: $scope.exportData.type === 'include' ? $scope.exportData.tables : [],
            excludedTables: $scope.exportData.type === 'exclude' ? $scope.exportData.tables : [],
        }).then(() => {
            $scope.exportLoading = false;
            $scope.getExports();
        }, (error) => {
            console.error(error);
        });
    };

    function getTables() {
        $http.get(DB_SERVICE_URL + `${$scope.exportData.db}/${$scope.exportData.source}`).then((result) => {
            for (let i = 0; i < result.data.schema.structures.length; i++) {
                if (result.data.schema.structures[i].type === 'TABLE') {
                    $scope.tables.push({ text: result.data.schema.structures[i].name, value: result.data.schema.structures[i].name });
                }
            }
        }, (error) => {
            console.error(error);
        });
    }

    function getDatabases() {
        $http.get(DB_SERVICE_URL).then((result) => {
            if (result.data.length > 0) {
                $scope.databases.length = 0;
                $scope.databases.push(...result.data);
                if (!$scope.databases.includes($scope.exportData.db)) {
                    $scope.exportData.db = $scope.databases[0];
                }
                $scope.dbChanged();
            }
        }, (error) => {
            console.error(error);
        });
    };

    getDatabases();
    $scope.getExports();

    $scope.$on('$destroy', () => {
        dialogHub.removeMessageListener(folderHandler);
    });
});

transferView.controller('TransferController', ($scope, $http) => {
    const statusBarHub = new StatusBarHub();
    const fallbackMsg = 'Encountered a transfer error. See console.';

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
        $http.get(DB_SERVICE_URL).then((data) => {
            if (data.data.length > 0) {
                for (let i = 0; i < data.data.length; i++) {
                    $scope.databases.push({ text: data.data[i], value: i });
                }
                if ($scope.databases[$scope.definition.selectedSourceDatabase]) {
                    $http.get(DB_SERVICE_URL + $scope.databases[$scope.definition.selectedSourceDatabase].text).then((sourceData) => {
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
        $http.get(DB_SERVICE_URL + $scope.databases[$scope.definition.selectedSourceDatabase].text)
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
        $http.get(DB_SERVICE_URL + $scope.databases[$scope.definition.selectedTargetDatabase].text)
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