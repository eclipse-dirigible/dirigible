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
let importView = angular.module('import', ['ideUI', 'ideView', 'ideWorkspace', 'ideTransport', 'angularFileUpload']);

importView.controller('ImportViewController', [
    '$scope',
    '$window',
    'messageHub',
    'ViewParameters',
    'workspaceApi',
    'transportApi',
    'FileUploader',
    function (
        $scope,
        $window,
        messageHub,
        ViewParameters,
        workspaceApi,
        transportApi,
        FileUploader,
    ) {
        const projectImportUrl = transportApi.getProjectImportUrl();
        $scope.selectedWorkspace = { name: 'workspace' }; // Default
        $scope.workspaceNames = [];
        $scope.projectsViewId = undefined;
        $scope.inDialog = false;
        $scope.importRepository = false;
        $scope.inputAccept = '.zip';
        $scope.dropAreaTitle = 'Import Projects';
        $scope.dropAreaSubtitle = 'Drop zip file(s) here, or use the "+" button.';
        $scope.dropAreaMore = '';
        $scope.uploadPath = '';
        $scope.queueLength = 100;
        $scope.uploader = new FileUploader({
            url: projectImportUrl
        });

        $scope.initImport = function () {
            let params = ViewParameters.get();
            if (params.container === 'dialog') {
                $scope.inDialog = true;
                if (params.importRepository) {
                    $scope.importRepository = true;
                    $scope.queueLength = 1;
                    $scope.uploader.url = transportApi.getSnapshotUrl();
                    $scope.dropAreaTitle = 'Import repository from zip';
                    $scope.dropAreaSubtitle = 'Drop snaphot here, or use the "+" button.';
                } else {
                    $scope.uploadPath = params.uploadPath;
                    $scope.selectedWorkspace.name = params.workspace;
                    if (params.importType === 'file') {
                        $scope.inputAccept = '';
                        $scope.importType = params.importType;
                        $scope.dropAreaTitle = 'Import files';
                        $scope.dropAreaSubtitle = 'Drop file(s) here, or use the "+" button.';
                        $scope.dropAreaMore = `Files will be imported in "${params.uploadPath}"`;
                    } if (params.importType === 'data') {
                        $scope.inputAccept = 'csv';
                        $scope.importType = params.importType;
                        $scope.dropAreaTitle = 'Import data files';
                        $scope.dropAreaSubtitle = 'Drop file(s) here, or use the "+" button.';
                        $scope.dropAreaMore = `Files will be imported in "${params.table}"`;
                    } else {
                        $scope.dropAreaTitle = 'Import files from zip';
                        $scope.dropAreaMore = `Files will be extracted in "${params.uploadPath}"`;
                        let pathSegments = params.uploadPath.split('/');
                        $scope.uploader.url = new UriBuilder().path(transportApi.getZipImportUrl().split('/')).path(params.workspace).path(pathSegments).build();
                        if (pathSegments.length <= 2) $scope.uploader.url += '/%252F';
                    }
                }
                $scope.projectsViewId = params.projectsViewId;
            } else $scope.reloadWorkspaceList();
        }

        $scope.uploader.filters.push({
            name: 'customFilter',
            fn: function (item /*{File|FileLikeObject}*/, options) {
                let type = item.type.slice(item.type.lastIndexOf('/') + 1);
                if ($scope.importType !== 'file' && $scope.importType !== 'data')
                    if (type != 'zip' && type != 'x-zip' && type != 'x-zip-compressed') {
                        return false;
                    }
                return this.queue.length < $scope.queueLength;
            }
        });

        $scope.uploader.onBeforeUploadItem = function (item) {
            if (!$scope.importRepository) {
                if (!$scope.inDialog) {
                    item.url = new UriBuilder().path(projectImportUrl.split('/')).path($scope.selectedWorkspace.name).build();
                } else if ($scope.inDialog && $scope.importType === 'file') {
                    item.headers = {
                        'Dirigible-Editor': 'Editor'
                    };
                    item.url = new UriBuilder().path(transportApi.getFileImportUrl().split('/')).path($scope.selectedWorkspace.name).path($scope.uploadPath.split('/')).path(item.name).build();
                } else if ($scope.inDialog && $scope.importType === 'data') {
                    item.headers = {
                        'Dirigible-Editor': 'Editor'
                    };
                    item.url = new UriBuilder().path($scope.selectedWorkspace.name).path($scope.uploadPath.split('/')).path(item.name).build();
                } else {
                    item.url = new UriBuilder().path(transportApi.getZipImportUrl().split('/')).path($scope.selectedWorkspace.name).path($scope.uploadPath.split('/')).build();
                }
            }
        };

        $scope.uploader.onCompleteAll = function () {
            if ($scope.importRepository) {
                messageHub.announceRepositoryModified();
            } else if ($scope.inDialog) {
                // Temporary, publishes all files in the import directory, not just imported ones
                messageHub.announceWorkspaceChanged({ name: $scope.selectedWorkspace.name, projectsViewId: $scope.projectsViewId, publish: { path: $scope.uploadPath } });
            } else {
                messageHub.announceWorkspaceChanged({ name: $scope.selectedWorkspace.name, publish: { workspace: true } });
            }
        };

        $scope.isSelectedWorkspace = function (name) {
            if ($scope.selectedWorkspace.name === name) return true;
            return false;
        };

        $scope.canUpload = function () {
            if ($scope.uploader.getNotUploadedItems().length) return '';
            return 'disabled';
        };

        $scope.addFiles = function () {
            $window.document.getElementById('input').click();
        };

        $scope.switchWorkspace = function (workspace) {
            if ($scope.selectedWorkspace.name !== workspace) {
                $scope.selectedWorkspace.name = workspace;
            }
        };

        $scope.reloadWorkspaceList = function () {
            const userSelected = JSON.parse(localStorage.getItem('DIRIGIBLE.workspace') || '{}');
            if (!userSelected.name) {
                $scope.selectedWorkspace.name = 'workspace'; // Default
            } else {
                $scope.selectedWorkspace.name = userSelected.name;
            }
            workspaceApi.listWorkspaceNames().then(function (response) {
                if (response.status === 200)
                    $scope.workspaceNames = response.data;
                else messageHub.setStatusError('Unable to load workspace list');
            });
        };

        messageHub.onWorkspacesModified(function () {
            $scope.reloadWorkspaceList();
        });

        // Initialization
        $scope.initImport();
    }]);