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
angular.module('RegistryService', []).provider('RegistryService', function registryServiceProvider() {
    this.registryServiceUrl = '/services/core/registry';
    this.$get = ['$http', function registryApiFactory($http) {
        const loadRegistry = function (resourcePath = '/') {
            const url = UriBuilder().path(this.registryServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.get(url, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        return {
            load: loadRegistry,
        };
    }];
});