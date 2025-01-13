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
angular.module('platformExtensions', [])
    /*
     * The 'extensionPoints' constant can be used to replace the default extension points from your controller.
     * Here is an example:
     * .constant('extensionPoints', {
     *     perspectives: ["example-perspectives"],
     *     shells: ["example-shells"],
     *     views: ["example-views"],
     *     subviews: ["example-subviews"],
     *     editors: ["example-editors"],
     *     menus: ["example-menus"],
     *     windows: ["example-windows"],
     *     themes: ["example-themes"],
     *     settings: ["example-settings"],
     * })
     */
    .constant('extensionPoints', {})
    .factory('Extensions', ['$http', 'extensionPoints', ($http, extensionPoints) => ({
        getViews: (exPoints = extensionPoints.views) => {
            return $http.get('/services/js/platform-core/extension-services/views.js', { params: { extensionPoints: exPoints } });
        },
        getSubviews: (exPoints = extensionPoints.subviews) => {
            return $http.get('/services/js/platform-core/extension-services/views.js', { params: { extensionPoints: exPoints ?? ['platform-subviews'] } });
        },
        getWindows: (exPoints = extensionPoints.windows) => {
            return $http.get('/services/js/platform-core/extension-services/views.js', { params: { extensionPoints: exPoints ?? ['platform-windows'] } });
        },
        getSettings: (exPoints = extensionPoints.settings) => {
            return $http.get('/services/js/platform-core/extension-services/views.js', { params: { extensionPoints: exPoints ?? ['platform-settings'] } });
        },
        getEditors: (exPoints = extensionPoints.editors) => {
            return $http.get('/services/js/platform-core/extension-services/editors.js', { params: { extensionPoints: exPoints } });
        },
        getPerspectives: (exPoints = extensionPoints.perspectives) => {
            return $http.get('/services/js/platform-core/extension-services/perspectives.js', { params: { extensionPoints: exPoints } });
        },
        getShells: (exPoints = extensionPoints.shells) => {
            return $http.get('/services/js/platform-core/extension-services/shells.js', { params: { extensionPoints: exPoints } });
        },
        getMenus: (exPoints = extensionPoints.menus) => {
            return $http.get('/services/js/platform-core/extension-services/menus.js', { params: { extensionPoints: exPoints } });
        },
        getThemes: (exPoints = extensionPoints.themes) => {
            return $http.get('/services/js/platform-core/extension-services/themes.js', { params: { extensionPoints: exPoints } });
        },
    })]);