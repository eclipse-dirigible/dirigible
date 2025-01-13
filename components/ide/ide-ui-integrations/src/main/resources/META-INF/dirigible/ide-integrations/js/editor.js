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
const editorView = angular.module('integrations', ['ideUI', 'ideView', 'ideWorkspace']);

let editorScope;

editorView.controller('EditorViewController', function ($scope, $window, workspaceApi, messageHub, ViewParameters) {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: "Loading...",
    };
    $scope.fileContent = '';
    $scope.errorMessage = '';
    $scope.workspaceApiBaseUrl = workspaceApi.getFullURL();
    $scope.messageHub = messageHub;

    angular.element($window).bind("focus", function () {
        messageHub.setFocusedEditor($scope.dataParameters.file);
        messageHub.setStatusCaret('');
    });

    messageHub.onEditorFocusGain(function (msg) {
        if (msg.resourcePath === $scope.dataParameters.file) messageHub.setStatusCaret('');
    });

    messageHub.onEditorReloadParameters(
        function (event) {
            $scope.$apply(() => {
                if (event.resourcePath === $scope.dataParameters.file) {
                    $scope.dataParameters = ViewParameters.get();
                }
            });
        }
    );

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('file')) {
        $scope.state.error = true;
        $scope.errorMessage = "The 'file' data parameter is missing.";
    } else {
        editorScope = $scope;
        const script = document.createElement('script');
        script.src = "designer/static/js/main.0447b661.js";
        document.getElementsByTagName('head')[0].appendChild(script);
    }

    $scope.saveContents = function () {
        workspaceApi.saveContent('', $scope.dataParameters.file, $scope.fileContent).then(function (response) {
            if (response.status === 200) {
                messageHub.announceFileSaved({
                    name: $scope.dataParameters.file.substring($scope.dataParameters.file.lastIndexOf('/') + 1),
                    path: $scope.dataParameters.file.substring($scope.dataParameters.file.indexOf('/', 1)),
                    contentType: $scope.dataParameters.contentType,
                    workspace: $scope.dataParameters.file.substring(1, $scope.dataParameters.file.indexOf('/', 1)),
                });
                messageHub.setStatusMessage(`File '${$scope.dataParameters.file}' saved`);
                messageHub.setEditorDirty(editorScope.dataParameters.file, false);
                $scope.$apply(function () {
                    $scope.state.isBusy = false;
                });
            } else {
                messageHub.setStatusError(`Error saving '${$scope.dataParameters.file}'`);
                messageHub.showAlertError('Error while saving the file', 'Please look at the console for more information');
                $scope.$apply(function () {
                    $scope.state.isBusy = false;
                });
            }
        });
    }

//    messageHub.subscribe(async function () {
//        $scope.saveContents();
//    }, "editor.file.save.all");

    messageHub.onDidReceiveMessage(
        'editor.file.save.all',
        function () {
            if (!$scope.state.error) {
                $scope.saveContents();
            }
        },
        true,
    );

    messageHub.onDidReceiveMessage(
        'editor.file.save',
        function (msg) {
            if (!$scope.state.error) {
                let file = msg.data && typeof msg.data === 'object' && msg.data.file;
                if (file && file === $scope.dataParameters.file) $scope.saveContents();
            }
        },
        true,
    );

    window.addEventListener('keydown',
        function (event) {
            if ((event.ctrlKey || event.metaKey) && event.key == 's') {
                event.preventDefault();
                if(!$scope.state.error) {
                    $scope.saveContents();
                }
            }
        }
    );
});

function getBaseUrl() {
    return editorScope.workspaceApiBaseUrl;
}

function getFileName() {
    return editorScope.dataParameters.file;
}

function setStateBusy(isBusy, text) {
    editorScope.$apply(function () {
        editorScope.state.isBusy = isBusy;
        if (text) editorScope.state.text = text;
    });
}

function setStateError(isError, message) {
    editorScope.$apply(function () {
        editorScope.state.error = isError;
        editorScope.errorMessage = message;
    });
}

function onFileChanged(yaml) {
    if(editorScope.fileContent !== '') {
        editorScope.messageHub.setEditorDirty(editorScope.dataParameters.file, true);
    }
    editorScope.fileContent = yaml;
}
