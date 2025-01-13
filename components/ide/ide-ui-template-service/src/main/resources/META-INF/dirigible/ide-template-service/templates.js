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
angular.module('ideTemplates', [])
    .provider('templatesApi', function PublisherApiProvider() {
        this.templatesServiceUrl = '/services/js/ide-template-service/api/templates.js';
        this.$get = ['$http', function publisherApiFactory($http) {

            let listTemplates = function () {
                let url = new UriBuilder().path(this.templatesServiceUrl.split('/')).build();
                return $http.get(url).then(function successCallback(response) {
                    return { status: response.status, data: response.data };
                }, function errorCallback(response) {
                    console.error('Template service:', response);
                    return { status: response.status };
                });
            }.bind(this);

            let fileExtensions = function () {
                let url = new UriBuilder().path(this.templatesServiceUrl.split('/')).path('extensions').build();
                return $http.get(url).then(function successCallback(response) {
                    return { status: response.status, data: response.data };
                }, function errorCallback(response) {
                    console.error('Template service:', response);
                    return { status: response.status };
                });
            }.bind(this);

            let menuTemplates = function () {
                let url = new UriBuilder().path(this.templatesServiceUrl.split('/')).path('menu').build();
                return $http.get(url).then(function successCallback(response) {
                    return { status: response.status, data: response.data };
                }, function errorCallback(response) {
                    console.error('Template service:', response);
                    return { status: response.status };
                });
            }.bind(this);

            return {
                listTemplates: listTemplates,
                fileExtensions: fileExtensions,
                menuTemplates: menuTemplates,
            };
        }];
    });