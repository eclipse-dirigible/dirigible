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
const databasesView = angular.module('databases', ['ideUI', 'ideView']);

databasesView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'databases-view';
}]);

databasesView.controller('DatabaseController', ['$scope', '$http', 'messageHub', function ($scope, $http, messageHub) {

    $scope.listDatabases = function () {
        $http.get('/services/data/sources').then(function (response) {
            $scope.list = response.data;
        });
    }
    $scope.listDatabases();

    $scope.joinParameters = function (parameters) {
        let result = '';
        if (parameters) {
            parameters.forEach(function (current) {
                result += current.name + '=' + current.value + ',';
            });
            return result.slice(0, -1);
        }
        return result;
    };

    $scope.newDatabase = function () {
        messageHub.showDialogWindow(
            "database-create-edit",
            { editMode: false },
            null,
            false
        );
    };

    messageHub.onDidReceiveMessage(
        'ide-databases.database.create',
        function (msg) {
            if (msg.data) {
                $http.post(
                    '/services/data/sources',
                    JSON.stringify(msg.data)
                ).then(function () {
                    $scope.listDatabases();
                    messageHub.triggerEvent('ide-databases.explorer.refresh', true);
                }, function (response) {
                    console.error(response);
                    messageHub.showAlertError('Error while creating database', 'Please look at the console for more information');
                });
            }
            messageHub.closeDialogWindow('database-create-edit');
        },
        true
    );

    $scope.editDatabase = function (database) {
        $scope.database = {
            id: database.id,
            name: database.name
        };
        messageHub.showDialogWindow(
            "database-create-edit",
            {
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
            null,
            false
        );
    };

    messageHub.onDidReceiveMessage(
        'ide-databases.database.edit',
        function (msg) {
            if (msg.data) {
                let database = msg.data;
                database.name = $scope.database.name;
                $http.put('/services/data/sources/' + $scope.database.id, JSON.stringify(database))
                    .then(function () {
                        $scope.listDatabases();
                        messageHub.triggerEvent('ide-databases.explorer.refresh', true);
                    }, function (response) {
                        console.error(response);
                        messageHub.showAlertError('Error while updating database', 'Please look at the console for more information');
                    });
            }
            messageHub.closeDialogWindow('database-create-edit');
        },
        true
    );

    $scope.deleteDatabase = function (database) {
        $scope.database = {
            id: database.id
        };

        messageHub.showDialog(
            'Delete Database',
            'Are you sure you want to delete the selected database?',
            [{
                id: 'btnOK',
                type: 'emphasized',
                label: 'OK',
            },
            {
                id: 'btnCancel',
                type: 'transparent',
                label: 'Cancel',
            }],
            'ide-databases.database.delete'
        );
    }

    messageHub.onDidReceiveMessage(
        'ide-databases.database.delete',
        function (msg) {
            if (msg.data === 'btnOK' && $scope.database.id) {
                $http.delete('/services/data/sources/' + $scope.database.id)
                    .then(function () {
                        $scope.listDatabases();
                        messageHub.triggerEvent('ide-databases.explorer.refresh', true);
                    }, function (response) {
                        console.error(response.data);
                        messageHub.showAlertError('Error while deleting database', 'Please look at the console for more information');
                    });
            }
        },
        true
    );

}]);