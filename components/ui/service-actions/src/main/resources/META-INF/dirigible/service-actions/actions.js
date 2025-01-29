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
angular.module('ActionsService', []).provider('ActionsService', function ActionsServiceProvider() {
    this.workspaceActionsServiceUrl = '/services/ide/workspace-actions';
    this.$get = ['$http', function actionsApiFactory($http) {
        const executeAction = function (workspace, project, action) {
            const url = UriBuilder().path(this.workspaceActionsServiceUrl.split('/')).path(workspace).path(project).path(action).build();
            return $http.post(url, {});
        }.bind(this);

        const isEnabled = function () {
            return true;
        }.bind(this);

        return {
            executeAction: executeAction,
            isEnabled: isEnabled
        };
    }];
});