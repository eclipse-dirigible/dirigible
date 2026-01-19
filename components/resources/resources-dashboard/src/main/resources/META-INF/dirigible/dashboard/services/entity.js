/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('EntityService', []).provider('EntityService', function EntityServiceProvider() {
    this.baseUrl = '';
    this.$get = ['$http', function entityApiFactory($http) {

        const count = function (idOrFilter) {
            let url = `${this.baseUrl}/count`;
            let bodyFilter = idOrFilter && typeof idOrFilter === 'object' && idOrFilter.$filter ? idOrFilter : undefined;

            if (!bodyFilter && idOrFilter != null && typeof idOrFilter === 'object') {
                const query = Object.keys(idOrFilter).map(e => idOrFilter[e] ? `${e}=${idOrFilter[e]}` : null).filter(e => e !== null).join('&');
                if (query) {
                    url = `${this.baseUrl}/count?${query}`;
                }
            } else if (!bodyFilter && idOrFilter) {
                url = `${this.baseUrl}/count/${idOrFilter}`;
            } else if (bodyFilter && bodyFilter.$filter && bodyFilter.$filter.conditions) {
                bodyFilter = bodyFilter.$filter;
            }

            if (bodyFilter) {
                return $http.post(url, JSON.stringify(bodyFilter), { headers: { 'describe': 'application/json' } });
            }
            return $http.get(url, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        const list = function (offsetOrFilter, limit) {
            let url = this.baseUrl;
            if (offsetOrFilter != null && typeof offsetOrFilter === 'object') {
                const query = Object.keys(offsetOrFilter).map(e => offsetOrFilter[e] ? `${e}=${offsetOrFilter[e]}` : null).filter(e => e !== null).join('&');
                if (query) {
                    url = `${this.baseUrl}?${query}`;
                }
            } else if (offsetOrFilter != null && limit != null) {
                url = `${url}?$offset=${offsetOrFilter}&$limit=${limit}`;
            }
            return $http.get(url, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        const filter = function (query, offset, limit) {
            const url = `${this.baseUrl}?${query}&$offset=${offset}&$limit=${limit}`;
            return $http.get(url, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        const search = function (entity) {
            const url = `${this.baseUrl}/search`;
            if (entity && entity.$filter && entity.$filter.conditions) {
                entity = entity.$filter;
            }
            const body = JSON.stringify(entity);
            return $http.post(url, body);
        }.bind(this);

        const create = function (entity) {
            const url = this.baseUrl;
            const body = JSON.stringify(entity);
            return $http.post(url, body);
        }.bind(this);

        const update = function (id, entity) {
            const url = `${this.baseUrl}/${id}`;
            const body = JSON.stringify(entity);
            return $http.put(url, body);
        }.bind(this);

        const deleteEntity = function (id) {
            const url = `${this.baseUrl}/${id}`;
            return $http.delete(url, { headers: { 'describe': 'application/json' } });
        }.bind(this);

        return {
            count: count,
            list: list,
            filter: filter,
            search: search,
            create: create,
            update: update,
            'delete': deleteEntity,
        };
    }];
});