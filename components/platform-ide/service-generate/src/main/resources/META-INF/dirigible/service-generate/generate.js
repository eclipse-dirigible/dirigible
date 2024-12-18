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
angular.module('GenerateService', []).provider('GenerateService', function GenerateServiceProvider() {
    this.generateServiceUrl = '/services/ide/generate';
    this.generateModelServiceUrl = '/services/js/service-generate/generate.mjs';
    this.$get = ['$http', function generateApiFactory($http) {
        const generateFromTemplate = function (workspace, project, file, template, parameters = { '__empty': '' }) {
            const url = UriBuilder().path(this.generateServiceUrl.split('/')).path('file').path(workspace).path(project).path(file.split('/')).build();
            return $http.post(url, { 'template': template, 'parameters': parameters });
        }.bind(this);

        const generateFromModel = function (workspace, project, file, template, parameters = { '__empty': '' }) {
            let url = UriBuilder().path(this.generateModelServiceUrl.split('/')).path('model').path(workspace).path(project).build();
            url = `${url}?path=${file.split('/')}`;
            return $http.post(url, { 'template': template, 'parameters': parameters, 'model': file });
        }.bind(this);

        const isEnabled = function () {
            return true;
        }.bind(this);

        return {
            generateFromTemplate: generateFromTemplate,
            generateFromModel: generateFromModel,
            isEnabled: isEnabled
        };
    }];
});