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
const databasesView = angular.module('databases', ['blimpKit', 'platformView']);
databasesView.constant('Dialogs', new DialogHub());
databasesView.controller('DatabaseController', ($scope, $http, Dialogs, ButtonStates) => {
    $scope.listDatabases = () => {
        $http.get('/services/data/sources').then((response) => {
            $scope.list = response.data;
        });
    }
    $scope.listDatabases();

    $scope.joinParameters = (parameters) => {
        let result = '';
        if (parameters) {
            parameters.forEach((current) => {
                result += current.name + '=' + current.value + ',';
            });
            return result.slice(0, -1);
        }
        return result;
    };

    $scope.newDatabase = () => {
        Dialogs.showWindow({
            id: 'database-create-edit',
            params: { editMode: false },
            closeButton: false
        });
    };

    const crudDialogListener = Dialogs.addMessageListener({
        topic: 'view-databases.dialog.submit',
        handler: (data) => {
            if (!data.editMode) {
                $http.post('/services/data/sources', JSON.stringify(data.database)).then(() => {
                    $scope.listDatabases();
                    Dialogs.triggerEvent('view-db-explorer.refresh');
                }, (response) => {
                    console.error(response);
                    Dialogs.showAlert({
                        type: AlertTypes.Error,
                        title: 'Error while creating database',
                        message: 'Please look at the console for more information',
                    });
                }).finally(() => {
                    Dialogs.closeWindow();
                });
            } else {
                let database = data.database;
                database.name = $scope.database.name;
                $http.put('/services/data/sources/' + $scope.database.id, JSON.stringify(database)).then(() => {
                    $scope.listDatabases();
                    Dialogs.triggerEvent('view-db-explorer.refresh');
                }, (response) => {
                    console.error(response);
                    Dialogs.showAlert({
                        type: AlertTypes.Error,
                        title: 'Error while updating database',
                        message: 'Please look at the console for more information',
                    });
                }).finally(() => {
                    Dialogs.closeWindow();
                });
            }
        },
    });

    $scope.editDatabase = (database) => {
        $scope.database = {
            id: database.id,
            name: database.name
        };
        Dialogs.showWindow({
            id: 'database-create-edit',
            params: {
                editMode: true,
                database: {
                    name: '',
                    driver: database.driver,
                    url: database.url,
                    username: database.username,
                    password: database.password,
                    parameters: $scope.joinParameters(database.properties),
                }
            },
            closeButton: false
        });
    };

    $scope.deleteDatabase = (database) => {
        $scope.database = {
            id: database.id
        };

        Dialogs.showDialog({
            title: 'Delete Database',
            message: 'Are you sure you want to delete the selected database?',
            buttons: [{
                id: 'ok',
                type: ButtonStates.Emphasized,
                label: 'OK',
            },
            {
                id: 'Cancel',
                type: ButtonStates.Transparent,
                label: 'Cancel',
            }]
        }).then((buttonId) => {
            if (buttonId === 'btnOK' && $scope.database.id) {
                $http.delete('/services/data/sources/' + $scope.database.id).then(() => {
                    $scope.listDatabases();
                    Dialogs.triggerEvent('view-db-explorer.refresh');
                }, (response) => {
                    console.error(response.data);
                    Dialogs.showAlert({
                        type: AlertTypes.Error,
                        title: 'Error while deleting database',
                        message: 'Please look at the console for more information',
                    });
                });
            }
        });
    };

    $scope.$on('$destroy', () => {
        Dialogs.removeMessageListener(crudDialogListener);
    });
});