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
angular.module('PublisherService', []).provider('PublisherService', function PublisherServiceProvider() {
    this.publisherServiceUrl = '/services/ide/publisher';
    this.$get = ['$http', function publisherApiFactory($http) {
        /**
         * Publishes a resource.
         * @param {string} resourcePath - Full resource path, including workspace name.
         */
        const publish = function (resourcePath) {
            const url = UriBuilder().path(this.publisherServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.post(url, {}, {
                headers: { 'Dirigible-Editor': 'Publish' }
            });
        }.bind(this);

        /**
         * Unpublishes a resource.
         * @param {string} resourcePath - Full resource path, including workspace name.
         */
        const unpublish = function (resourcePath) {
            const url = UriBuilder().path(this.publisherServiceUrl.split('/')).path(resourcePath.split('/')).build();
            return $http.delete(url, { headers: { 'Dirigible-Editor': 'Publish' } });
        }.bind(this);

        const isEnabled = function () {
            return true;
        }.bind(this);

        return {
            publish: publish,
            unpublish: unpublish,
            isEnabled: isEnabled
        };
    }];
});