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
const editorView = angular.module('image-app', ['blimpKit', 'platformView', 'WorkspaceService']);
editorView.controller('ImageViewController', function ($scope, $window, WorkspaceService, ViewParameters) {
    const statusBarHub = new StatusBarHub();
    const layoutHub = new LayoutHub();
    $scope.imageLink = '';
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };

    angular.element($window).bind('focus', () => {
        statusBarHub.showLabel('');
    });

    angular.element('#image-view').bind('error', () => {
        messageHub.closeEditor($scope.dataParameters.file);
    });

    const loadFileContents = () => {
        $scope.imageLink = WorkspaceService.getFullURL($scope.dataParameters.filePath);
        $scope.state.isBusy = false;
    };

    layoutHub.onFocusEditor((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) statusBarHub.showLabel('');
    });

    layoutHub.onReloadEditorParams((data) => {
        if (data.path === $scope.dataParameters.filePath) {
            $scope.$evalAsync(() => {
                $scope.dataParameters = ViewParameters.get();
                loadFileContents();
            });
        };
    });

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = "The 'filePath' data parameter is missing.";
    } else {
        loadFileContents();
    }
});