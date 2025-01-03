/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const crudDialog = angular.module('crudDialog', ['blimpKit', 'platformView']);
crudDialog.constant('Dialogs', new DialogHub());
crudDialog.controller('CRUDDialogController', ($scope, Dialogs, ViewParameters) => {
    // State management
    $scope.state = {
        dialogType: null, // 'update', 'delete', 'create'
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };

    $scope.forms = {
        crudForm: {},
    };

    $scope.inputRules = {
        patterns: ['^(?! ).*(?<! )$']
    };

    $scope.dialogType = null;

    // Data
    $scope.data = {
        row: {},
        primaryKeys: null,
        specialKeys: null
    };

    $scope.cancel = () => {
        Dialogs.closeWindow();
    };

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('dialogType')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The "type" parameter is missing.';
    } else {
        $scope.dialogType = $scope.dataParameters.dialogType;
        if (!$scope.dataParameters.hasOwnProperty('data')) {
            $scope.state.error = true;
            $scope.errorMessage = 'The "data" parameter is missing.';
        } else {
            if ($scope.dataParameters.dialogType == 'update') {
                $scope.data.row = $scope.dataParameters.data.row;
                $scope.data.primaryKeys = $scope.dataParameters.data.primaryKeys
                    ? $scope.dataParameters.data.primaryKeys
                    : null;
                $scope.data.specialKeys = $scope.dataParameters.data.specialKeys
                    ? $scope.dataParameters.data.specialKeys
                    : null;
            } else {
                $scope.data.row = $scope.dataParameters.data;
            }
        }
        $scope.state.isBusy = false;
    }

    $scope.confirmAction = () => {
        $scope.state.isBusy = true;
        $scope.state.busyText = 'Processing...';
        Dialogs.postMessage({
            topic: 'result-view.dialog.submit', data: {
                type: $scope.dialogType,
                row: $scope.data.row,
            }
        });
    };

    $scope.isPrimaryOrSpecialKey = (key) => ($scope.data.primaryKeys && $scope.data.primaryKeys.includes(key)) ||
        ($scope.data.specialKeys && $scope.data.specialKeys.includes(key));
});