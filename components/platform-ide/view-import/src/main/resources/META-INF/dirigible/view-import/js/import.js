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
const importView = angular.module('import', ['blimpKit', 'platformView', 'WorkspaceService', 'TransportService', 'angularFileUpload']);
importView.constant('StatusBar', new StatusBarHub());
importView.constant('Workspace', new WorkspaceHub());
importView.constant('Repository', new RepositoryHub());
importView.controller('ImportViewController',
    function (
        $scope,
        $window,
        ViewParameters,
        WorkspaceService,
        TransportService,
        FileUploader,
        StatusBar,
        Workspace,
        Repository
    ) {
        const projectImportUrl = TransportService.getProjectImportUrl();
        $scope.selectedWorkspace = WorkspaceService.getCurrentWorkspace();
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

        $scope.initImport = () => {
            let params = ViewParameters.get();
            if (params.container === 'window') {
                $scope.inDialog = true;
                if (params.importRepository) {
                    $scope.importRepository = true;
                    $scope.queueLength = 1;
                    $scope.uploader.url = TransportService.getSnapshotUrl();
                    $scope.dropAreaTitle = 'Import repository from zip';
                    $scope.dropAreaSubtitle = 'Drop snaphot here, or use the "+" button.';
                } else {
                    $scope.uploadPath = params.uploadPath;
                    if (params.workspace) $scope.selectedWorkspace = params.workspace;
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
                        $scope.uploader.url = UriBuilder().path(TransportService.getZipImportUrl().split('/')).path(pathSegments).build();
                        if (pathSegments.length <= 2) $scope.uploader.url += '/%252F';
                    }
                }
                $scope.projectsViewId = params.projectsViewId;
            } else $scope.reloadWorkspaceList();
        }

        $scope.uploader.filters.push({
            name: 'customFilter',
            fn: function (item /*{File|FileLikeObject}*/, _options) {
                let type = item.type.slice(item.type.lastIndexOf('/') + 1);
                if ($scope.importType !== 'file' && $scope.importType !== 'data')
                    if (type != 'zip' && type != 'x-zip' && type != 'x-zip-compressed') {
                        return false;
                    }
                return this.queue.length < $scope.queueLength;
            }
        });

        $scope.uploader.onBeforeUploadItem = (item) => {
            if (!$scope.importRepository) {
                if (!$scope.inDialog) {
                    item.url = UriBuilder().path(projectImportUrl.split('/')).path($scope.selectedWorkspace).build();
                } else if ($scope.inDialog && $scope.importType === 'file') {
                    item.headers = {
                        'Dirigible-Editor': 'Editor'
                    };
                    item.url = UriBuilder().path(TransportService.getFileImportUrl().split('/')).path($scope.uploadPath.split('/')).path(item.name).build();
                } else if ($scope.inDialog && $scope.importType === 'data') {
                    item.headers = { 'Dirigible-Editor': 'Editor' };
                    item.url = UriBuilder().path($scope.uploadPath.split('/')).path(item.name).build();
                } else {
                    item.url = UriBuilder().path(TransportService.getZipImportUrl().split('/')).path($scope.uploadPath.split('/')).build();
                }
            }
        };

        $scope.uploader.onCompleteAll = () => {
            if ($scope.importRepository) {
                Repository.announceRepositoryModified();
            } else if ($scope.inDialog) {
                // Temporary, publishes all files in the import directory, not just imported ones
                Workspace.announceWorkspaceChanged({
                    workspace: $scope.selectedWorkspace,
                    params: {
                        projectsViewId: $scope.projectsViewId,
                        publish: { path: $scope.uploadPath }
                    }
                });
            } else {
                Workspace.announceWorkspaceChanged({ workspace: $scope.selectedWorkspace, params: { publish: { workspace: true } } });
            }
        };

        $scope.isSelectedWorkspace = (name) => $scope.selectedWorkspace === name;

        $scope.isUploadEnabled = () => {
            if ($scope.uploader.getNotUploadedItems().length) return false;
            return true;
        };

        $scope.addFiles = () => {
            $window.document.getElementById('input').click();
        };

        $scope.switchWorkspace = (workspace) => {
            if ($scope.selectedWorkspace !== workspace) {
                $scope.selectedWorkspace = workspace;
            }
        };

        $scope.reloadWorkspaceList = () => {
            WorkspaceService.listWorkspaceNames().then((response) => {
                $scope.$evalAsync(() => {
                    $scope.workspaceNames = response.data;
                    if (!$scope.workspaceNames.includes($scope.selectedWorkspace))
                        $scope.selectedWorkspace = 'workspace'; // Default
                });
            }, (response) => {
                console.error(response);
                StatusBar.showError('Unable to load workspace list');
            });
        };

        const workspaceCreatedListener = Workspace.onWorkspaceCreated($scope.reloadWorkspaceList);
        const workspaceDeletedListener = Workspace.onWorkspaceDeleted($scope.reloadWorkspaceList);

        // Initialization
        $scope.initImport();

        $scope.$on('$destroy', () => {
            Workspace.removeMessageListener(workspaceCreatedListener);
            Workspace.removeMessageListener(workspaceDeletedListener);
        });
    });