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
const userdialog = angular.module('userdialog', ['ideUI', 'ideView']);

userdialog.controller('UserDialogController', ['$scope', '$http', 'messageHub', 'ViewParameters', function ($scope, $http, messageHub, ViewParameters) {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: "Loading...",
    };

    $scope.forms = {
        userForm: {},
    };

    $scope.inputRules = {
        patterns: ['^(?! ).*(?<! )$']
    };

    $scope.editMode = false;

    $scope.listTenants = function () {
        $http.get('/services/security/tenants').then(function (response) {
            $scope.tenants = response.data;
        });
    }
    $scope.listTenants();

    $scope.listRoles = function () {
        $http.get('/services/security/roles').then(function (response) {
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

    $scope.save = function () {
        $scope.state.busyText = "Sending data..."
        $scope.state.isBusy = true;
        messageHub.postMessage(getTopic(), $scope.user, true);
    };

    $scope.cancel = function () {
        messageHub.closeDialogWindow('user-create-edit');
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

}]);