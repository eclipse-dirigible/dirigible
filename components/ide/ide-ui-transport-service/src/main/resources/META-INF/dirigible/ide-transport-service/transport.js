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
angular.module('ideTransport', [])
    .provider('transportApi', function TransportApiProvider() {
        this.transportServiceUrl = '/services/ide/transport';
        this.$get = ['$http', '$window', function transportApiFactory($http, $window) {

            let exportProject = function (workspace, projectName) {
                if (!workspace) throw Error("Transport API: You must provide a workspace name");
                if (!projectName) throw Error("Transport API: You must provide a project name");
                let url = new UriBuilder().path(this.transportServiceUrl.split('/')).path('project').path(workspace).path(projectName).build();
                $window.open(url, '_blank');
            }.bind(this);

            let getProjectImportUrl = function () {
                return new UriBuilder().path(this.transportServiceUrl.split('/')).path('project').build();
            }.bind(this);

            let getZipImportUrl = function () {
                return new UriBuilder().path(this.transportServiceUrl.split('/')).path('zipimport').build();
            }.bind(this);

            let getFileImportUrl = function () {
                return new UriBuilder().path(this.transportServiceUrl.split('/')).path('fileimport').build();
            }.bind(this);

            let getSnapshotUrl = function () {
                return new UriBuilder().path(this.transportServiceUrl.split('/')).path('snapshot').build();
            }.bind(this);

            let exportRepository = function () {
                let url = new UriBuilder().path(this.transportServiceUrl.split('/')).path('snapshot').build();
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