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
angular.module('TemplatesService', []).provider('TemplatesService', function TemplateServiceProvider() {
    this.templatesServiceUrl = '/services/js/service-template/api/templates.js';
    this.$get = ['$http', function templateApiFactory($http) {

        const listTemplates = function () {
            const url = UriBuilder().path(this.templatesServiceUrl.split('/')).build();
            return $http.get(url);
        }.bind(this);

        const fileExtensions = function () {
            const url = UriBuilder().path(this.templatesServiceUrl.split('/')).path('extensions').build();
            return $http.get(url);
        }.bind(this);

        const menuTemplates = function () {
            const url = UriBuilder().path(this.templatesServiceUrl.split('/')).path('menu').build();
            return $http.get(url);
        }.bind(this);

        return {
            listTemplates: listTemplates,
            fileExtensions: fileExtensions,
            menuTemplates: menuTemplates,
        };
    }];
});