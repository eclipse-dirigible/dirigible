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
const IMPORT_SERVICE_URL = '/services/data/schema/importProcesses';
const dialogHub = new DialogHub();
const shellHub = new ShellHub();
transferView.controller('ViewController', ($scope, $http) => {
    $scope.navActive = 'transfer';

    $scope.switchNav = (nav) => {
        $scope.navActive = nav;
    };
    $scope.databases = [];

    const callbacks = [];

    $scope.onDBLoadedCallback = (callback) => {
        callbacks.push(callback);
        if (callbacks.length === 3) {
            getDatabases();
        }
    };

    $scope.goToDocuments = (path) => {
        shellHub.showPerspective({ id: 'perspectiveDocuments', params: { path: path } });
    };

    function getDatabases() {
        $http.get(DB_SERVICE_URL).then((result) => {
            if (result.data.length > 0) {
                $scope.databases.length = 0;
                $scope.databases.push(...result.data);
                for (let i = 0; i < callbacks.length; i++) {
                    callbacks[i]();
                }
            }
        }, (error) => {
            console.error(error);
        });
    };
});

transferView.controller('ExportController', ($scope, $http, $timeout) => {
    $scope.forms = { ep: {} };
    $scope.exportData = {
        db: '',
        source: '',
        path: '',
        type: 'include',
        tables: [],
    };
    $scope.$parent.onDBLoadedCallback(() => {
        $scope.exportData.db = $scope.$parent.databases[0];
        $scope.dbChanged();
    });
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

    let retries = 0;

    const getExports = (historic = false) => {
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
                    getExports(true);
                    if (response.data.length && retries < 3) {
                        retries += 1;
                        $timeout(() => { getExports() }, 1000);
                    }
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
            retries = 0;
            getExports();
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

    getExports();

    $scope.$on('$destroy', () => {
        dialogHub.removeMessageListener(folderHandler);
    });
});

transferView.controller('ImportController', ($scope, $http, $timeout) => {
    $scope.forms = { ep: {} };
    $scope.importData = {
        db: '',
        path: '',
    };
    $scope.$parent.onDBLoadedCallback(() => {
        $scope.importData.db = $scope.$parent.databases[0];
    });
    $scope.datasources = [];
    $scope.tables = [];
    $scope.importLoading = false;
    $scope.imports = [];

    $scope.choosePath = () => {
        dialogHub.showWindow({
            id: 'windowDocuments',
            hasHeader: false,
            params: {
                type: 'folderSelect',
                upload: false,
                download: false,
                multiple: false,
                callbackTopic: 'transfer.import.folder'
            },
            maxWidth: '960px',
            maxHeight: '540px',
            closeButton: true
        });
    };

    const folderHandler = dialogHub.addMessageListener({
        topic: 'transfer.import.folder',
        handler: (path) => {
            $scope.$evalAsync(() => {
                $scope.importData.path = path;
            });
        },
    });

    let retries = 0;

    const getImports = (historic = false) => {
        if (!historic) $scope.imports.length = 0;
        $http.get(historic ? '/services/bpm/bpm-processes/historic-instances' : '/services/bpm/bpm-processes/instances', { params: { definitionKey: 'import-schema', limit: 100 } })
            .then((response) => {
                for (let i = 0; i < response.data.length; i++) {
                    $http.get(historic ? `/services/bpm/bpm-processes/historic-instances/${response.data[i].id}/variables` : `/services/bpm/bpm-processes/instances/${response.data[i].id}/variables`, { params: { 'limit': 100 } }).then((varList) => {
                        let dataSource = '';
                        let path = '';
                        let time = '';
                        for (let l = 0; l < varList.data.length; l++) {
                            if (varList.data[l]['name'] === 'dataSource') {
                                dataSource = varList.data[l]['value'];
                                time = varList.data[l]['createTime'];
                            }
                            else if (varList.data[l]['name'] === 'exportPath') path = varList.data[l]['value'];
                        }
                        $scope.imports.push({
                            id: response.data[i].id,
                            db: dataSource,
                            path: path,
                            importing: !historic,
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
                    getImports(true);
                    if (response.data.length && retries < 2) {
                        retries += 1;
                        $timeout(() => { getImports() }, 1000);
                    }
                }
            }, (error) => {
                console.error(error);
            });
    };

    $scope.startImport = () => {
        $scope.importLoading = true;
        $http.post(IMPORT_SERVICE_URL, {
            dataSource: $scope.importData.db,
            exportPath: $scope.importData.path,
        }).then(() => {
            $scope.importLoading = false;
            retries = 0;
            getImports();
        }, (error) => {
            console.error(error);
        });
    };

    getImports();

    $scope.$on('$destroy', () => {
        dialogHub.removeMessageListener(folderHandler);
    });
});

transferView.controller('TransferController', ($scope, $http) => {
    const statusBarHub = new StatusBarHub();
    const fallbackMsg = 'Encountered a transfer error. See console.';

    let transferWsUrl = "/websockets/data/transfer";

    $scope.definition = {};
    $scope.definition.selectedSourceDatabase = undefined;
    $scope.definition.selectedTargetDatabase = undefined;
    $scope.sourceDatasources = [];
    $scope.targetDatasources = [];
    $scope.definition.selectedSourceDatasource = undefined;
    $scope.definition.selectedTargetDatasource = undefined;
    $scope.sourceSchemes = [];
    $scope.targetSchemes = [];
    $scope.definition.selectedSourceScheme = 0;
    $scope.definition.selectedTargetScheme = 0;

    $scope.$parent.onDBLoadedCallback(() => {
        $scope.definition.selectedSourceDatabase = $scope.$parent.databases[0];
        $scope.definition.selectedTargetDatabase = $scope.$parent.databases[0];
        $http.get(DB_SERVICE_URL + $scope.definition.selectedSourceDatabase).then((sourceData) => {
            if (sourceData.data.length > 0) {
                $scope.sourceDatasources.push(...sourceData.data);
                $scope.definition.selectedSourceDatasource = $scope.sourceDatasources[0];
                $scope.targetDatasources.push(...sourceData.data);
                $scope.definition.selectedTargetDatasource = $scope.targetDatasources[0];
            }
        });
    });

    $scope.allLogs = [];
    $scope.logs = [];
    $scope.autoScroll = true;

    let transferWebsocket = null;

    $scope.databaseSourceChanged = () => {
        $http.get(DB_SERVICE_URL + $scope.definition.selectedSourceDatabase)
            .then((result) => {
                $scope.sourceDatasources.length = 0;
                $scope.sourceDatasources.push(...result.data);
                if ($scope.sourceDatasources.length > 0) {
                    $scope.definition.selectedSourceDatasource = $scope.sourceDatasources[0];
                } else {
                    $scope.definition.selectedSourceDatasource = undefined;
                }
            });
    };

    $scope.databaseTargetChanged = () => {
        $http.get(DB_SERVICE_URL + $scope.definition.selectedTargetDatabase)
            .then((result) => {
                $scope.targetDatasources.length = 0;
                $scope.targetDatasources.push(...result.data);
                if ($scope.targetDatasources.length > 0) {
                    $scope.definition.selectedTargetDatasource = $scope.targetDatasources[0];
                } else {
                    $scope.definition.selectedDatasource = undefined;
                }
            });
    };

    $scope.startTransfer = () => {
        if ($scope.definition.selectedSourceDatabase
            && $scope.definition.selectedSourceDatasource
            && $scope.definition.selectedTargetDatabase
            && $scope.definition.selectedTargetDatasource) {
            transferData({
                "source": $scope.definition.selectedSourceDatabase,
                "target": $scope.definition.selectedTargetDatabase,
                "configuration": {
                    "sourceSchema": $scope.definition.selectedSourceDatasource,
                    "targetSchema": $scope.definition.selectedTargetDatasource
                }
            });
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

    connect();

    function consoleLogMessage(record) {
        $scope.$evalAsync(() => {
            $scope.logs.push(record);
        });
    }

    $scope.clearLog = () => {
        $scope.logs.length = 0;
    };
});