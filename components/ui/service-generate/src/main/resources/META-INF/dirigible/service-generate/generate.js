/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
    // Both generation paths are served by the platform's Java generation endpoint - the model path
    // used to be a JavaScript service, which is gone.
    this.generateServiceUrl = '/services/ide/generate';
    this.$get = ['$http', function generateApiFactory($http) {
        const generateFromTemplate = function (workspace, project, file, template, parameters = { '__empty': '' }) {
            const url = UriBuilder().path(this.generateServiceUrl.split('/')).path('file').path(workspace).path(project).path(file.split('/')).build();
            return $http.post(url, { 'template': template, 'parameters': parameters });
        }.bind(this);

        const generateFromModel = function (workspace, project, file, template, parameters = { '__empty': '' }) {
            let url = UriBuilder().path(this.generateServiceUrl.split('/')).path('model').path(workspace).path(project).build();
            url = `${url}?path=${file.split('/')}`;
            return $http.post(url, { 'template': template, 'parameters': parameters });
        }.bind(this);

        // A model whose descriptor names a template the platform no longer ships (the AngularJS and
        // TypeScript stacks) is answered 409 with the Java template that replaces it (#8051): the
        // caller confirms with the developer and regenerates with that one.
        const retiredTemplate = function (response) {
            return response && response.status === 409 && response.data && response.data.successor ? response.data : null;
        };

        const isEnabled = function () {
            return true;
        }.bind(this);

        return {
            generateFromTemplate: generateFromTemplate,
            generateFromModel: generateFromModel,
            retiredTemplate: retiredTemplate,
            isEnabled: isEnabled
        };
    }];
});