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
const userdialog = angular.module('userdialog', ['platformView', 'blimpKit']);
userdialog.constant('Dialogs', new DialogHub());
userdialog.controller('UserDialogController', ($scope, $http, Dialogs, ViewParameters) => {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };

    $scope.forms = {
        userForm: {},
    };

    $scope.inputRules = {
        patterns: ['^(?! ).*(?<! )$']
    };

    $scope.editMode = false;

    $scope.listTenants = () => {
        $http.get('/services/security/tenants').then((response) => {
            $scope.tenants = response.data;
        });
    }
    $scope.listTenants();

    $scope.listRoles = () => {
        $http.get('/services/security/roles').then((response) => {
            $scope.roles = response.data;
            $scope.roles.forEach(role => { role.value = role.id; role.text = role.name; });
        });
    }
    $scope.listRoles();

    $scope.user = {
        username: '',
        password: '',
        tenant: '',
        roles: []
    };

    function getTopic() {
        if ($scope.editMode) return 'ide-security.user.edit';
        return 'ide-security.user.create';
    }

    $scope.save = () => {
        $scope.state.busyText = 'Sending data...';
        $scope.state.isBusy = true;
        Dialogs.postMessage({
            topic: getTopic(),
            data: $scope.user
        });
    };

    $scope.cancel = () => {
        Dialogs.closeWindow();
    };

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('editMode')) {
        $scope.state.error = true;
        $scope.errorMessage = "The 'editMode' parameter is missing.";
    } else {
        $scope.editMode = $scope.dataParameters.editMode;
        if ($scope.editMode) {
            if (!$scope.dataParameters.hasOwnProperty('user')) {
                $scope.state.error = true;
                $scope.errorMessage = "The 'user' parameter is missing.";
            } else {
                $scope.user = $scope.dataParameters.user;
            }
        }
        $scope.state.isBusy = false;
    }
});