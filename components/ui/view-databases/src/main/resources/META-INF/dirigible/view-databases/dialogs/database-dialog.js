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
const dbdialog = angular.module('dbdialog', ['blimpKit', 'platformView']);
dbdialog.constant('Dialogs', new DialogHub());
dbdialog.controller('DBDialogController', ($scope, ViewParameters, Dialogs) => {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };

    $scope.forms = {
        dbForm: {},
    };

    $scope.inputRules = {
        patterns: ['^(?! ).*(?<! )$']
    };

    $scope.urls = {
        "org.h2.Driver": "jdbc:h2:<path>/<name>",
        "org.postgresql.Driver": "jdbc:postgresql://<host>:<port>/<database>",
        "com.mysql.cj.jdbc.Driver": "jdbc:mysql://<host>:<port>/<database>",
        "org.mariadb.jdbc.Driver": "jdbc:mariadb://<host>:<port>/<database>",
        "com.microsoft.sqlserver.jdbc.SQLServerDriver": "jdbc:sqlserver://<host>:<port>;databaseName=<database>;encrypt=true;trustServerCertificate=true",
        "com.sap.db.jdbc.Driver": "jdbc:sap://<host>:<port>/?encrypt=true&validateCertificate=false",
        "net.snowflake.client.jdbc.SnowflakeDriver": "jdbc:snowflake://<account_identifier>.snowflakecomputing.com",
        "org.eclipse.dirigible.mongodb.jdbc.Driver": "jdbc:mongodb://<host>:<port>/<database>",
    };

    $scope.parameters = {
        'org.h2.Driver': '',
        'org.postgresql.Driver': '',
        'com.mysql.cj.jdbc.Driver': '',
        'org.mariadb.jdbc.Driver': '',
        'com.microsoft.sqlserver.jdbc.SQLServerDriver': '',
        'com.sap.db.jdbc.Driver': '',
        'net.snowflake.client.jdbc.SnowflakeDriver': 'db=<database>,schema=<schema>',
        'org.eclipse.dirigible.mongodb.jdbc.Driver': '',
    };

    $scope.drivers = [
        { text: 'H2 - org.h2.Driver', value: 'org.h2.Driver' },
        { text: 'PostgreSQL - org.postgresql.Driver', value: 'org.postgresql.Driver' },
        { text: 'MySQL - com.mysql.cj.jdbc.Driver', value: 'com.mysql.cj.jdbc.Driver' },
        { text: 'MariaDB - org.mariadb.jdbc.Driver', value: 'org.mariadb.jdbc.Driver' },
        { text: 'MSSQL - com.microsoft.sqlserver.jdbc.SQLServerDriver', value: 'com.microsoft.sqlserver.jdbc.SQLServerDriver' },
        { text: 'SAP HANA - com.sap.db.jdbc.Driver', value: 'com.sap.db.jdbc.Driver' },
        { text: 'Snowflake - net.snowflake.client.jdbc.SnowflakeDriver', value: 'net.snowflake.client.jdbc.SnowflakeDriver' },
        { text: 'MongoDB - org.eclipse.dirigible.mongodb.jdbc.Driver', value: 'org.eclipse.dirigible.mongodb.jdbc.Driver' }
    ];

    $scope.database = {
        name: '',
        driver: '',
        url: '',
        username: '',
        password: '',
        parameters: '',
    };

    $scope.driverChanged = () => {
        $scope.database.url = $scope.urls[$scope.database.driver];
        $scope.database.username = '';
        $scope.database.password = '';
        $scope.database.parameters = $scope.parameters[$scope.database.driver];
    };

    $scope.save = () => {
        $scope.state.busyText = 'Sending data...'
        $scope.state.isBusy = true;
        Dialogs.postMessage({
            topic: 'view-databases.dialog.submit', data: {
                editMode: $scope.dataParameters.editMode,
                database: $scope.database,
            }
        });
    };

    $scope.cancel = () => {
        Dialogs.closeWindow();
    };

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('editMode')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The "editMode" parameter is missing.';
    } else {
        if ($scope.dataParameters.editMode) {
            if (!$scope.dataParameters.hasOwnProperty('database')) {
                $scope.state.error = true;
                $scope.errorMessage = 'The "database" parameter is missing.';
            } else {
                $scope.database = $scope.dataParameters.database;
            }
        }
        $scope.state.isBusy = false;
    }
});
