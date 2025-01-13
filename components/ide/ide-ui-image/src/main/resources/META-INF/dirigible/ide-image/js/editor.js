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
const editorView = angular.module('image-app', ['ideUI', 'ideView', 'ideWorkspace']);
editorView.controller('ImageViewController', function ($scope, $window, messageHub, workspaceApi, ViewParameters) {
    $scope.imageLink = '';
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };

    angular.element($window).bind('focus', function () {
        messageHub.setFocusedEditor($scope.dataParameters.file);
        messageHub.setStatusCaret('');
    });

    angular.element('#image-view').bind('error', function () {
        messageHub.closeEditor($scope.dataParameters.file);
    });

    $scope.loadFileContents = function () {
        $scope.imageLink = workspaceApi.getFullURL('', $scope.dataParameters.file);
        $scope.state.isBusy = false;
    };

    messageHub.onEditorFocusGain(function (msg) {
        if (msg.resourcePath === $scope.dataParameters.file) messageHub.setStatusCaret('');
    });

    messageHub.onEditorReloadParameters(
        function (event) {
            $scope.$apply(() => {
                if (event.resourcePath === $scope.dataParameters.file) {
                    $scope.dataParameters = ViewParameters.get();
                    $scope.loadFileContents();
                }
            });
        }
    );

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('file')) {
        $scope.state.error = true;
        $scope.errorMessage = "The 'file' data parameter is missing.";
    } else {
        $scope.loadFileContents();
    }
});