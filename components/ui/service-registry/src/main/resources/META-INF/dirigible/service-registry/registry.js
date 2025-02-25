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
angular.module('RegistryService', []).provider('RegistryService', function registryServiceProvider() {
    this.registryServiceUrl = '/services/core/registry';
    this.registryPublicServiceUrl = '/services/core/repository/registry/public'
    this.$get = ['$http', function registryApiFactory($http) {
        /**
         * Loads file content.
         * @param {string} resourcePath - Full resource path.
         */
        const loadContent = function (resourcePath) {
            const url = UriBuilder().path(this.registryPublicServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.get(url);
        }.bind(this);

        /**
         * List the contents of a registry path.
         * @param {string} resourcePath - Full resource path. Default is '/'
         */
        const loadRegistry = function (resourcePath = '/') {
            const url = UriBuilder().path(this.registryServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.get(url, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        return {
            loadContent: loadContent,
            load: loadRegistry,
        };
    }];
});