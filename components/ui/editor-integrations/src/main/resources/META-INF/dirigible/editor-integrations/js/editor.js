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
const editorView = angular.module('integrations', ['blimpKit', 'platformView', 'platformShortcuts', 'WorkspaceService']);

let editorScope;
const statusBarHub = new StatusBarHub();
const workspaceHub = new WorkspaceHub();
const layoutHub = new LayoutHub();

editorView.controller('EditorViewController', ($scope, $window, WorkspaceService, ViewParameters) => {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: "Loading...",
    };
    $scope.fileContent = '';
    $scope.errorMessage = '';
    $scope.workspaceApiBaseUrl = WorkspaceService.getFullURL();

    angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

    layoutHub.onFocusEditor((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) statusBarHub.showLabel('');
    });

    layoutHub.onReloadEditorParams((data) => {
        if (data.path === $scope.dataParameters.filePath) {
            $scope.$evalAsync(() => {
                $scope.dataParameters = ViewParameters.get();
            });
        };
    });

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'filePath\' data parameter is missing.';
    } else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'contentType\' data parameter is missing.';
    } else {
        editorScope = $scope;
        const script = document.createElement('script');
        script.src = "designer/static/js/main.0447b661.js";
        document.getElementsByTagName('head')[0].appendChild(script);
    }

    function saveContents() {
        WorkspaceService.saveContent($scope.dataParameters.filePath, $scope.fileContent).then(() => {
            layoutHub.setEditorDirty({
                path: $scope.dataParameters.filePath,
                dirty: false,
            });
            workspaceHub.announceFileSaved({
                path: $scope.dataParameters.filePath,
                contentType: $scope.dataParameters.contentType,
            });
            $scope.$evalAsync(() => {
                $scope.changed = false;
                $scope.state.isBusy = false;
            });
        }, (response) => {
            console.error(response);
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = `Error saving '${$scope.dataParameters.filePath}'. Please look at the console for more information.`;
                $scope.state.isBusy = false;
            });
        });
    }

    $scope.save = (keySet = 'ctrl+s', event) => {
        event?.preventDefault();
        if (keySet === 'ctrl+s') {
            if (!$scope.state.error) {
                $scope.state.busyText = 'Saving...';
                $scope.state.isBusy = true;
                saveContents();
            }
        }
    };

    workspaceHub.onSaveAll(() => {
        $scope.save();
    });

    workspaceHub.onSaveFile((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) {
            $scope.save();
        }
    });
});

function getBaseUrl() {
    return editorScope.workspaceApiBaseUrl;
}

function getFileName() {
    return editorScope.dataParameters.filePath;
}

function setStateBusy(isBusy, text) {
    editorScope.$evalAsync(() => {
        editorScope.state.isBusy = isBusy;
        if (text) editorScope.state.text = text;
    });
}

function setStateError(isError, message) {
    editorScope.$evalAsync(() => {
        editorScope.state.error = isError;
        editorScope.errorMessage = message;
    });
}

function onFileChanged(yaml) {
    if (editorScope.fileContent !== '') {
        layoutHub.setEditorDirty({
            path: editorScope.dataParameters.filePath,
            dirty: true,
        });
    }
    editorScope.fileContent = yaml;
}
