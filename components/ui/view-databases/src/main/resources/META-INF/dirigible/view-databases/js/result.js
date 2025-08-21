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
const resultView = angular.module('result', ['blimpKit', 'platformView']);
resultView.constant('Dialogs', new DialogHub());
resultView.constant('StatusBar', new StatusBarHub());
resultView.constant('Layout', new LayoutHub());
resultView.controller('DatabaseResultController', ($scope, $http, Dialogs, StatusBar, Layout) => {
    const lastSelectedDatabaseKey = `${getBrandingInfo().prefix}.view-db-explorer.database`;
    let selectedDatabase = JSON.parse(localStorage.getItem(lastSelectedDatabaseKey) ?? 'null');
    if (!selectedDatabase) {
        selectedDatabase = {
            name: 'DefaultDB', // Datasource
            type: 'metadata' // Database
        }
    }
    $scope.state = {
        isBusy: false,
        error: false,
        busyText: 'Loading...',
    };
    $scope.selectedTab;

    $scope.tabClicked = (tab) => {
        $scope.selectedTab = tab;
    };
    $scope.procedureResults = [];
    $scope.hasMultipleProcedureResults = false;
    $scope.schemaName = null;
    $scope.tableName = null;
    $scope.selectedRow = null;
    $scope.hasResult = false;

    $scope.openCreateDialog = () => {
        newRow = {};

        $scope.columns.forEach((column) => {
            newRow[column] = null;
        });
        Dialogs.showWindow({
            id: 'result-view-crud',
            params: {
                dialogType: 'create',
                data: newRow
            },
            closeButton: false
        });
    };

    $scope.openEditDialog = (row) => {
        const selectedRow = angular.copy(row);
        Dialogs.showWindow({
            id: 'result-view-crud',
            params: {
                dialogType: 'update',
                data: {
                    row: selectedRow,
                    primaryKeys: $scope.primaryKeyColumns,
                    specialKeys: $scope.specialColumns
                }
            },
            closeButton: false
        });
    };

    $scope.openDeleteDialog = (row) => {
        const selectedRow = angular.copy(row);
        Dialogs.showWindow({
            id: 'result-view-crud',
            params: {
                dialogType: 'delete',
                data: selectedRow
            },
            closeButton: false
        });
    };

    const crudDialogListener = Dialogs.addMessageListener({
        topic: 'result-view.dialog.submit',
        handler: (data) => {
            let requestBody = {
                schemaName: $scope.schemaName,
                tableName: $scope.tableName,
                data: data.row
            };
            let method = 'POST';
            if (data.type === 'update') {
                method = 'PUT';
                requestBody['primaryKey'] = $scope.primaryKeyColumns;
            } else if (data.type === 'update') {
                method = 'DELETE';
                requestBody['primaryKey'] = $scope.primaryKeyColumns;
            }
            const url = `/services/js/view-databases/js/databaseTable.js/${selectedDatabase.name}/${$scope.schemaName}/${$scope.tableName}`;
            $http({
                method: method,
                url: url,
                data: requestBody,
                headers: { 'Content-Type': 'application/json' },
            }).then(() => {
                showContent({
                    schemaName: $scope.schemaName,
                    tableName: $scope.tableName
                });
            }, (error) => {
                console.error(`Failed to ${data.type} row:`, error);
                Dialogs.showAlert({
                    type: AlertTypes.Error,
                    title: 'CRUD operation failed',
                    message: error ? `Failed to ${data.type} row: ${error}` : `There was an error during the ${data.type} operation. Check console for more inforamtion.`,
                });
            }).finally(() => {
                Dialogs.closeWindow();
            });
        },
    });

    function extractSpecialAndPrimaryKeys(tableMetadata) {
        const specialColumns = [];
        const primaryKeyColumns = [];
        const columns = [];

        tableMetadata.columns.forEach(column => {
            columns.push(column);

            const type = column.type.toUpperCase();
            if (type === 'BLOB' || type === 'CLOB') {
                specialColumns.push(column.name);
            }

            if (column.primaryKey) {
                primaryKeyColumns.push(column.name);
            }
        });

        return { columns, specialColumns, primaryKeyColumns };
    }

    // const databaseChangedListener = Dialogs.addMessageListener({
    //     topic: 'database.database.selection.changed',
    //     handler: (database) => {
    //         console.log(database);
    //         $scope.$evalAsync(() => {
    //             selectedDatabase.type = database;
    //         });
    //     },
    // });

    const datasourceChangedListener = Dialogs.addMessageListener({
        topic: 'database.datasource.selection.changed',
        handler: (datasource) => {
            $scope.$evalAsync(() => {
                selectedDatabase.name = datasource;
            });
        },
    });

    $scope.showProgress = () => {
        $scope.state.isBusy = true;
        StatusBar.showBusy('Executing query...');
    };

    $scope.hideProgress = () => {
        StatusBar.hideBusy();
        $scope.$evalAsync(() => {
            $scope.state.isBusy = false;
        });
    };

    function populateResultView(result) {
        cleanScope();
        if (result.data != null && result.data.length > 0) {
            $scope.rows = result.data;
            $scope.columns = [];
            for (let i = 0; i < result.data.length; i++) {
                for (let column in result.data[i]) {
                    $scope.columns.push(column);
                }
                break;
            }
            $scope.hasResult = true;
        } else if (result.data !== null && result.data.errorMessage !== null && result.data.errorMessage !== undefined) {
            $scope.state.error = true;
            $scope.errorMessage = result.data.errorMessage;
            $scope.hasResult = false;
        } else {
            $scope.result = 'Empty result';
            $scope.hasResult = false;
        }
        $scope.hideProgress();
    }

    function executeQuery(command) {
        Layout.openView({ id: 'result' });
        $scope.state.error = false;
        $scope.showProgress();
        const url = '/services/data/' + selectedDatabase.name;
        const sql = command.trim().toLowerCase();
        if (sql.startsWith('select')) {
            $http({
                method: 'POST',
                url: url + '/query',
                data: command,
                headers: {
                    'Content-Type': 'text/plain',
                    'X-Requested-With': 'Fetch',
                }
            }).then(populateResultView, (reject) => {
                cleanScope();
                $scope.state.error = true;
                $scope.errorMessage = reject.data.message;
                console.error(reject);
                $scope.hideProgress();
            });
        } else if (sql.startsWith('call')) {
            $http({
                method: 'POST',
                url: url + '/procedure',
                data: command,
                headers: {
                    'Content-Type': 'text/plain',
                    'X-Requested-With': 'Fetch',
                }
            }).then((result) => {
                cleanScope();
                if (result.data != null && result.data.length > 0) {
                    $scope.hasMultipleProcedureResults = result.data.length > 1;
                    if ($scope.hasMultipleProcedureResults) {
                        $scope.procedureResults.length = 0;
                        for (let resultIndex = 0; resultIndex < result.data.length; resultIndex++) {
                            let procedureResult = JSON.parse(result.data[resultIndex]);
                            let data = {
                                rows: procedureResult,
                                columns: []
                            };
                            for (let i = 0; i < procedureResult.length; i++) {
                                for (let column in procedureResult[i]) {
                                    data.columns.push(column);
                                }
                                break;
                            }
                            $scope.procedureResults.push(data);
                        }
                    } else {
                        result = JSON.parse(result.data[0]);
                        $scope.rows = result;
                        $scope.columns = [];
                        for (let i = 0; i < result.length; i++) {
                            for (let column in result[i]) {
                                $scope.columns.push(column);
                            }
                            break;
                        }
                    }
                } else if (result.data !== null && result.data.errorMessage !== null && result.data.errorMessage !== undefined) {
                    $scope.state.error = true;
                    $scope.errorMessage = result.data.errorMessage;
                } else {
                    $scope.result = 'Empty result';
                }
                $scope.hideProgress();
            }, (reject) => {
                cleanScope();
                $scope.state.error = true;
                $scope.errorMessage = reject.data.message;
                console.error(reject);
                $scope.hideProgress();
            });
        } else if (sql.startsWith('query: ')) {
            $http({
                method: 'POST',
                url: url + '/query',
                data: command.substring(7).trim(),
                headers: {
                    'Content-Type': 'text/plain',
                    'X-Requested-With': 'Fetch',
                }
            }).then((result) => {
                cleanScope();
                if (result.data != null && result.data.length > 0) {
                    $scope.rows = result.data;
                    $scope.columns = [];
                    for (let i = 0; i < result.data.length; i++) {
                        for (let column in result.data[i]) {
                            $scope.columns.push(column);
                        }
                        break;
                    }
                } else if (result.data !== null && result.data.errorMessage !== null && result.data.errorMessage !== undefined) {
                    $scope.state.error = true;
                    $scope.errorMessage = result.data.errorMessage;
                } else {
                    $scope.result = 'Empty result';
                }
                $scope.hideProgress();
            }, (reject) => {
                cleanScope();
                $scope.state.error = true;
                $scope.errorMessage = reject.data.message;
                console.error(reject);
                $scope.hideProgress();
            });
        } else if (sql.startsWith('update: ')) {
            $http({
                method: 'POST',
                url: url + '/update',
                data: command.substring(8).trim(),
                headers: {
                    'Content-Type': 'text/plain',
                    'X-Requested-With': 'Fetch',
                }
            }).then((result) => {
                cleanScope();
                if (!isNaN(result.data)) {
                    result = 'Rows updated: ' + result.data;
                } else if (result.data !== null) {
                    $scope.result = result.data;
                } else {
                    $scope.result = 'Empty result';
                }
                $scope.result = result.data;
                $scope.hideProgress();
            }, (reject) => {
                cleanScope();
                $scope.state.error = true;
                $scope.errorMessage = reject.data.message;
                console.error(reject);
                $scope.hideProgress();
            });
        } else {
            $http({
                method: 'POST',
                url: url + '/update',
                data: command,
                headers: {
                    'Content-Type': 'text/plain', 'Accept': 'text/plain',
                    'X-Requested-With': 'Fetch',
                }
            }).then((result) => {
                cleanScope();
                if (!isNaN(result.data)) {
                    result = 'Rows updated: ' + result.data;
                } else if (result.data !== null) {
                    $scope.result = result.data;
                } else {
                    $scope.result = 'Empty result';
                }
                $scope.result = result.data;
                $scope.hideProgress();
            }, (reject) => {
                cleanScope();
                $scope.state.error = true;
                $scope.errorMessage = reject.data.message;
                console.error(reject);
                $scope.hideProgress();
            });
        }
    }

    function triggerDownload(result) {
        const blob = new Blob([result.data], {
            type: "application/octet-stream",
        });
        const contentDisposition = result.headers('Content-Disposition');
        let fileName = 'downloaded-file';

        if (contentDisposition) {
            const match = contentDisposition.match(/filename="?([^"]+)"?/i);
            if (match) {
                fileName = match[1];
            }
        }
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        window.URL.revokeObjectURL(url);
    }

    function exportQuery(command) {
        Layout.openView({ id: 'result' });
        $scope.state.error = false;
        $scope.showProgress();
        const url = '/services/data/export/' + selectedDatabase.name;
        const sql = command.trim().toLowerCase();
        if (sql.startsWith('select')) {
            $http({
                method: 'POST',
                url: url,
                data: command,
                headers: {
                    'Content-Type': 'text/plain',
                    'X-Requested-With': 'Fetch',
                }
            }).then((result) => {
                cleanScope();
                triggerDownload(result);
                $scope.hideProgress();
            }, (reject) => {
                cleanScope();
                $scope.state.error = true;
                $scope.errorMessage = reject.data.message;
                console.error(reject);
                $scope.hideProgress();
            });
        } else if (sql.startsWith('query: ')) {
            $http({
                method: 'POST',
                url: url,
                data: command.substring(7).trim(),
                headers: {
                    'Content-Type': 'text/plain',
                    'X-Requested-With': 'Fetch',
                }
            }).then((result) => {
                cleanScope();
                triggerDownload(result);
                $scope.hideProgress();
            }, (reject) => {
                cleanScope();
                $scope.state.error = true;
                $scope.errorMessage = reject.data.message;
                console.error(reject);
                $scope.hideProgress();
            });
        }
    }

    function showContent(data) {
        $scope.schemaName = data.schemaName;
        $scope.tableName = data.tableName;
        const sqlCommand = 'SELECT * FROM "' + data.schemaName + '"' + '.' + '"' + data.tableName + '";\n';
        executeQuery(sqlCommand);
        $http.get('/services/data/definition/' + selectedDatabase.name + '/' + $scope.schemaName + '/' + btoa($scope.tableName))
            .then((response) => {
                $scope.metadata = response.data; // Set the metadata once the response is received
                const extractedKeys = extractSpecialAndPrimaryKeys($scope.metadata);
                $scope.primaryKeyColumns = extractedKeys.primaryKeyColumns;
                $scope.specialColumns = extractedKeys.specialColumns;

                const url = `/services/js/view-databases/js/databaseTable.js/${selectedDatabase.name}/${$scope.schemaName}/${btoa($scope.tableName)}`;
                $http.get(url).then(populateResultView, (reject) => {
                    cleanScope();
                    console.error(reject);
                    $scope.$evalAsync(() => {
                        $scope.state.error = true;
                        $scope.errorMessage = reject?.data?.message || 'Failed to get all rows';
                        $scope.hideProgress();
                    });
                });
            })
            .catch((error) => {
                console.error('Error fetching metadata:', error);
                $scope.$evalAsync(() => {
                    $scope.state.error = true;
                    $scope.errorMessage = 'Error fetching metadata';
                    $scope.hideProgress();
                });
            });
    }

    const showContentListener = Dialogs.addMessageListener({
        topic: 'result-view.database.sql.showContent',
        handler: (data) => {
            $scope.$evalAsync(() => {
                showContent(data);
            });
        }
    });

    const executeListener = Dialogs.addMessageListener({
        topic: 'database.sql.execute',
        handler: (data) => {
            $scope.$evalAsync(() => {
                executeQuery(data);
            });
        }
    });

    const exportListener = Dialogs.addMessageListener({
        topic: 'database.sql.export',
        handler: (data) => {
            $scope.$evalAsync(() => {
                exportQuery(data);
            });
        }
    });

    const importArtifactListener = Dialogs.addMessageListener({
        topic: 'database.data.import.artifact',
        handler: (command) => {
            const artifact = command.split('.');
            const url = '/services/data/import/' + selectedDatabase.name + '/' + artifact[0] + '/' + artifact[1];
            Dialogs.showWindow({
                id: 'import',
                params: {
                    importType: 'data',
                    uploadPath: url,
                    workspace: '',
                    table: selectedDatabase.name + '.' + artifact[0] + '.' + artifact[1],
                }
            });
        }
    });

    const importSqlListener = Dialogs.addMessageListener({
        topic: 'database.data.import.sql',
        handler: (command) => {
            const artifact = command.split('.');
            const url = '/services/data/sql/' + selectedDatabase.name + '/' + artifact[0];
            Dialogs.showWindow({
                id: 'import',
                params: {
                    importType: 'sql',
                    uploadPath: url,
                    workspace: '',
                    schema: selectedDatabase.name + '.' + artifact[0],
                }
            });
        }
    });

    const exportArtifactListener = Dialogs.addMessageListener({
        topic: 'database.data.export.artifact',
        handler: (command) => {
            const artifact = command.split('.');
            window.open('/services/data/export/' + selectedDatabase.name + '/' + artifact[0] + '/' + artifact[1]);
        }
    });

    const exportSchemaListener = Dialogs.addMessageListener({
        topic: 'database.data.export.schema',
        handler: (command) => {
            window.open('/services/data/export/' + selectedDatabase.name + '/' + command);
        }
    });

    const projectExportSchemaListener = Dialogs.addMessageListener({
        topic: 'database.data.project.export.schema',
        handler: (data) => {
            const schema = data;
            $http({
                method: 'PUT',
                url: '/services/data/project/csv/' + selectedDatabase.name + '/' + schema,
                headers: { 'X-Requested-With': 'Fetch' }
            }).then((_resourceURI) => {
                const fileURL = window.location.protocol + '//' + window.location.host + `/services/ide/workspaces/${schema}/${schema}`
                Dialogs.showAlert({
                    title: 'Export',
                    message: `Created requested files in Project [${schema}] in Workspace [${schema}]. \n To access it it please go to: ${fileURL}`,
                    type: AlertTypes.Success,
                });
            }).catch((err) => {
                Dialogs.showAlert({
                    title: 'Export error',
                    message: 'Error in exporting data in project',
                    type: AlertTypes.Error,
                });
                console.error('Error in exporting data in project', err);
            });
        }
    });

    const projectExportModelListener = Dialogs.addMessageListener({
        topic: 'database.metadata.project.export.model',
        handler: (data) => {
            const schema = data;
            $http({
                method: 'PUT',
                url: '/services/data/project/model/' + selectedDatabase.name + '/' + schema,
                headers: { 'X-Requested-With': 'Fetch' }
            }).then((_resourceURI) => {
                const fileURL = window.location.protocol + '//' + window.location.host + `/services/ide/workspaces/${schema}/${schema}`;
                Dialogs.showAlert({
                    title: 'Export',
                    message: `Created requested file in Project [${schema}] in Workspace [${schema}]. \n To access it it please go to: ${fileURL}`,
                    type: AlertTypes.Success,
                });
            }).catch((err) => {
                Dialogs.showAlert({
                    title: 'Export error',
                    message: 'Error in exporting data in project',
                    type: AlertTypes.Error,
                });
                console.error('Error in exporting data in project', err);
            });
        }
    });

    const metadataExportArtifactListener = Dialogs.addMessageListener({
        topic: 'database.metadata.export.artifact',
        handler: (command) => {
            const artifact = command.split('.');
            window.open('/services/data/definition/' + selectedDatabase.name + '/' + artifact[0] + '/' + artifact[1]);
        }
    });

    const metadataExportSchemaListener = Dialogs.addMessageListener({
        topic: 'database.metadata.export.schema',
        handler: (command) => {
            window.open('/services/data/definition/' + selectedDatabase.name + '/' + command);
        }
    });

    const metadataProjectExportSchemaListener = Dialogs.addMessageListener({
        topic: 'database.metadata.project.export.schema',
        handler: (data) => {
            const schema = data;
            $http({
                method: 'PUT',
                url: '/services/data/project/metadata/' + selectedDatabase.name + '/' + schema,
                headers: { 'X-Requested-With': 'Fetch' }
            }).then((_resourceURI) => {
                const fileURL = window.location.protocol + '//' + window.location.host + `/services/ide/workspaces/${schema}/${schema}/${schema}.schema`;
                Dialogs.showAlert({
                    title: 'Export',
                    message: `Created file [${schema}.schema] in Project [${schema}] in Workspace [${schema}]. \n To access it it please go to: ${fileURL}`,
                    type: AlertTypes.Success,
                });
            }).catch((err) => {
                Dialogs.showAlert({
                    title: 'Export error',
                    message: 'Error in exporting metadata in project',
                    type: AlertTypes.Error,
                });
                console.error('Error in exporting metadata in project', err);
            });
        }
    });

    const metadataProjectExportTopologyListener = Dialogs.addMessageListener({
        topic: 'database.metadata.project.export.topology',
        handler: (command) => {
            window.open('/services/data/project/topology/' + selectedDatabase.name + '/' + command);
        }
    });

    const dataAnonymizeListener = Dialogs.addMessageListener({
        topic: 'database.data.anonymize.column',
        handler: (data) => {
            $http({
                method: 'POST',
                url: '/services/data/anonymize/column',
                data: data,
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'Fetch',
                }
            }).then((result) => {
                $scope.$evalAsync(() => {
                    cleanScope();
                    if (result.data !== null && result.data.errorMessage !== null && result.data.errorMessage !== undefined) {
                        $scope.state.error = true;
                        $scope.errorMessage = result.data.errorMessage;
                    } else {
                        $scope.result = 'Data anonymized.';
                    }
                });
                $scope.hideProgress();
            }, (result) => {
                $scope.$evalAsync(() => {
                    cleanScope();
                    $scope.state.error = true;
                    $scope.errorMessage = result.data.errorMessage;
                });
                $scope.hideProgress();
            });
        }
    });

    function cleanScope() {
        $scope.result = null;
        $scope.columns = null;
        $scope.rows = null;
        $scope.hasMultipleProcedureResults = false;
        $scope.procedureResults.length = 0;
    }

    const sqlErrorListener = Dialogs.addMessageListener({
        topic: 'database.sql.error',
        handler: (error) => {
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = error;
                $scope.hideProgress();
            });
        }
    });

    $scope.$on('$destroy', () => {
        Dialogs.removeMessageListener(crudDialogListener);
        Dialogs.removeMessageListener(datasourceChangedListener);
        Dialogs.removeMessageListener(showContentListener);
        Dialogs.removeMessageListener(executeListener);
        Dialogs.removeMessageListener(exportListener);
        Dialogs.removeMessageListener(importArtifactListener);
        Dialogs.removeMessageListener(importSqlListener);
        Dialogs.removeMessageListener(exportArtifactListener);
        Dialogs.removeMessageListener(exportSchemaListener);
        Dialogs.removeMessageListener(projectExportSchemaListener);
        Dialogs.removeMessageListener(projectExportModelListener);
        Dialogs.removeMessageListener(metadataExportArtifactListener);
        Dialogs.removeMessageListener(metadataExportSchemaListener);
        Dialogs.removeMessageListener(metadataProjectExportSchemaListener);
        Dialogs.removeMessageListener(metadataProjectExportTopologyListener);
        Dialogs.removeMessageListener(dataAnonymizeListener);
        Dialogs.removeMessageListener(sqlErrorListener);
    });
});