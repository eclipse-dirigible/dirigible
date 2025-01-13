/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2024 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('ideExtensions', ['ngResource']).factory('Extensions', ['$resource', function ($resource) {
    return {
        get: function (type, extensionPoint) {
            let url;
            if (type === 'dialogWindow') {
                url = '/services/js/resources-core/services/dialog-windows.js';
            } else if (type === 'menu') {
                url = '/services/js/resources-core/services/menu.js';
            } else if (type === 'perspective') {
                url = '/services/js/resources-core/services/perspectives.js';
            } else if (type === 'view') {
                url = '/services/js/resources-core/services/views.js';
            } else if (type === 'subview') {
                url = '/services/js/resources-core/services/views.js';
                if (!extensionPoint) extensionPoint = 'ide-subview';
            } else if (type === 'editor') {
                url = '/services/js/resources-core/services/editors.js';
            } else {
                throw new Error('Parameter "type" must be `dialogWindow`, `menu`, `perspective`, `view`, `subview` or `editor`');
            }
            return $resource(url).query({ extensionPoint: extensionPoint }).$promise;
        }
    };
}]);