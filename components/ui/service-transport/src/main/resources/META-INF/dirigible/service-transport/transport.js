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
angular.module('TransportService', [])
    .provider('TransportService', function TransportServiceProvider() {
        this.transportServiceUrl = '/services/ide/transport';
        this.$get = ['$window', function transportApiFactory($window) {

            const exportProject = function (workspace, projectName) {
                if (!workspace) throw Error('Transport API: You must provide a workspace name');
                if (!projectName) throw Error('Transport API: You must provide a project name');
                const url = UriBuilder().path(this.transportServiceUrl.split('/')).path('project').path(workspace).path(projectName).build();
                $window.open(url, '_blank');
            }.bind(this);

            const getProjectImportUrl = function () {
                return UriBuilder().path(this.transportServiceUrl.split('/')).path('project').build();
            }.bind(this);

            const getZipImportUrl = function () {
                return UriBuilder().path(this.transportServiceUrl.split('/')).path('zipimport').build();
            }.bind(this);

            const getFileImportUrl = function () {
                return UriBuilder().path(this.transportServiceUrl.split('/')).path('fileimport').build();
            }.bind(this);

            const getSnapshotUrl = function () {
                return UriBuilder().path(this.transportServiceUrl.split('/')).path('snapshot').build();
            }.bind(this);

            const exportRepository = function () {
                const url = UriBuilder().path(this.transportServiceUrl.split('/')).path('snapshot').build();
                $window.open(url, '_blank');
            }.bind(this);

            return {
                exportProject: exportProject,
                getProjectImportUrl: getProjectImportUrl,
                getZipImportUrl: getZipImportUrl,
                getFileImportUrl: getFileImportUrl,
                getSnapshotUrl: getSnapshotUrl,
                exportRepository: exportRepository,
            };
        }];
    });