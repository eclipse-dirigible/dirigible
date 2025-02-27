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
const clientRegistrationDialog = angular.module('clientRegistrationDialog', ['platformView', 'blimpKit']);
clientRegistrationDialog.constant('Dialogs', new DialogHub());
clientRegistrationDialog.controller('ClientRegistrationDialogController', ($scope, Dialogs, ViewParameters) => {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };

    $scope.forms = {
        clientRegistrationForm: {},
    };

    $scope.inputRules = {
        patterns: ['^(?! ).*(?<! )$']
    };

    $scope.editMode = false;

    $scope.clientRegistration = {
        name: '',
        clientId: '',
        clientSecret: '',
        redirectUri: '',
        authorizationGrantType: '',
        scope: '',
        tokenUri: '',
        authorizationUri: '',
        userInfoUri: '',
        issuerUri: '',
        jwkSetUri: '',
        userNameAttributeName: ''
    };

    function getTopic() {
        if ($scope.editMode) return 'ide-security.client-registration.edit';
        return 'ide-security.client-registration.create';
    }

    $scope.save = () => {
        $scope.state.busyText = 'Sending data...';
        $scope.state.isBusy = true;
        Dialogs.postMessage({
            topic: getTopic(),
            data: $scope.clientRegistration
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
            if (!$scope.dataParameters.hasOwnProperty('clientRegistration')) {
                $scope.state.error = true;
                $scope.errorMessage = "The 'clientRegistration' parameter is missing.";
            } else {
                $scope.clientRegistration = $scope.dataParameters.clientRegistration;
            }
        }
        $scope.state.isBusy = false;
    }
});