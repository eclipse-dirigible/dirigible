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
angular.module('ideActions', [])
    .provider('actionsApi', function ActionsApiProvider() {
        this.workspaceActionsServiceUrl = '/services/ide/workspace-actions';
        this.$get = ['$http', function actionsApiFactory($http) {
	
		let executeAction = function (workspace, project, action) {
                let url = new UriBuilder().path(this.workspaceActionsServiceUrl.split('/')).path(workspace).path(project).path(action).build();
                return $http.post(url, {})
                    .then(function successCallback(response) {
                        return { status: response.status, data: response.data };
                    }, function errorCallback(response) {
                        console.error('Actions service:', response);
                        return { status: response.status };
                    });
            }.bind(this);
            
            let isEnabled = function() {
				return true;
			}.bind(this);

            return {
				executeAction: executeAction,
                isEnabled: isEnabled
            };
        }];
    });