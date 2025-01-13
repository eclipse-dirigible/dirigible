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
angular.module('idePublisher', [])
    .provider('publisherApi', function PublisherApiProvider() {
        this.publisherServiceUrl = '/services/ide/publisher';
        this.$get = ['$http', function publisherApiFactory($http) {

            let publish = function (resourcePath, workspace) {
                let url;
                if (workspace) {
                    url = new UriBuilder().path(this.publisherServiceUrl.split('/')).path(workspace).path(resourcePath.split('/')).build();
                } else url = new UriBuilder().path(this.publisherServiceUrl.split('/')).path(resourcePath.split('/')).build();
                return $http.post(url, {}, {
                    headers: {
                        "Dirigible-Editor": "Publish"
                    }
                }).then(function successCallback(response) {
                    return { status: response.status, data: response.data };
                }, function errorCallback(response) {
                    console.error('Publisher service:', response);
                    return { status: response.status };
                });
            }.bind(this);

            let unpublish = function (resourcePath, workspace) {
                let url;
                if (workspace) {
                    url = new UriBuilder().path(this.publisherServiceUrl.split('/')).path(workspace).path(resourcePath.split('/')).build();
                } else url = new UriBuilder().path(this.publisherServiceUrl.split('/')).path(resourcePath.split('/')).build();
                return $http.delete(url, {
                    headers: {
                        "Dirigible-Editor": "Publish"
                    }
                }).then(function successCallback(response) {
                    return { status: response.status, data: response.data };
                }, function errorCallback(response) {
                    console.error('Publisher service:', response);
                    return { status: response.status };
                });
            }.bind(this);

            let isEnabled = function () {
                return true;
            }.bind(this);

            return {
                publish: publish,
                unpublish: unpublish,
                isEnabled: isEnabled
            };
        }];
    });