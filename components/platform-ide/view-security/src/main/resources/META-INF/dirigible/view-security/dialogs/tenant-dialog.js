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
const tenantdialog = angular.module('tenantdialog', ['platformView', 'blimpKit']);
tenantdialog.constant('Dialogs', new DialogHub());
tenantdialog.controller('TenantDialogController', ($scope, Dialogs, ViewParameters) => {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };

    $scope.forms = {
        tenantForm: {},
    };

    $scope.inputRules = {
        patterns: ['^(?! ).*(?<! )$']
    };

    $scope.editMode = false;

    $scope.tenant = {
        name: '',
        subdomain: ''
    };

    function getTopic() {
        if ($scope.editMode) return 'ide-security.tenant.edit';
        return 'ide-security.tenant.create';
    }

    $scope.save = () => {
        $scope.state.busyText = 'Sending data...';
        $scope.state.isBusy = true;
        Dialogs.postMessage({
            topic: getTopic(),
            data: $scope.tenant
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
            if (!$scope.dataParameters.hasOwnProperty('tenant')) {
                $scope.state.error = true;
                $scope.errorMessage = "The 'tenant' parameter is missing.";
            } else {
                $scope.tenant = $scope.dataParameters.tenant;
            }
        }
        $scope.state.isBusy = false;
    }
});