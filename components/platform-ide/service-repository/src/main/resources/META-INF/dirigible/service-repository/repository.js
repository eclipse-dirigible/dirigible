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
angular.module('RepositoryService', []).provider('RepositoryService', function RepositoryServiceProvider() {
    this.repositoryServiceUrl = '/services/core/repository';
    this.$get = ['$http', function repositoryApiFactory($http) {
        const getMetadata = function (resourceUrl) {
            return $http.get(resourceUrl, { headers: { 'describe': 'application/json' } });
        }

        const loadRepository = function (resourcePath = '/') {
            const url = UriBuilder().path(this.repositoryServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.get(url, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        const createCollection = function (path, name) {
            const url = UriBuilder().path(this.repositoryServiceUrl.split('/')).path(path.split('/')).path(name).build() + '/';
            return $http.post(url);
        }.bind(this);

        const createResource = function (path, name) {
            const url = UriBuilder().path(this.repositoryServiceUrl.split('/')).path(path.split('/')).path(name).build();
            return $http.post(url);
        }.bind(this);

        const remove = function (resourcePath) {
            if (resourcePath !== undefined && !(typeof resourcePath === 'string'))
                throw Error("remove: resourcePath must be a path");
            const url = UriBuilder().path(this.repositoryServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.delete(url, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        return {
            getMetadata: getMetadata,
            load: loadRepository,
            createCollection: createCollection,
            createResource: createResource,
            remove: remove,
        };
    }];
});