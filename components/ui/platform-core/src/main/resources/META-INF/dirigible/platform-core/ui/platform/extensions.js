/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
// @ts-nocheck
if (!top.hasOwnProperty('PlatformExtPoints')) top.PlatformExtPoints = {
    perspectives: ['platform-perspectives'],
    shells: ['platform-shells'],
    views: ['platform-views'],
    subviews: ['platform-subviews'],
    editors: ['platform-editors'],
    menus: ['platform-menus'],
    windows: ['platform-windows'],
    themes: ['platform-themes'],
    settings: ['platform-settings'],
    locales: ['platform-locales'],
};
function getExtPoints() {
    if (top.hasOwnProperty('PlatformExtPoints')) return top.PlatformExtPoints;
    throw Error('PlatformExtPoints is not set!');
}
function setExtPoints({ perspectives, shells, views, subviews, editors, menus, windows, themes, settings, locales } = {}) {
    if (perspectives) top.PlatformExtPoints.perspectives = perspectives;
    if (shells) top.PlatformExtPoints.shells = shells;
    if (views) top.PlatformExtPoints.views = views;
    if (subviews) top.PlatformExtPoints.subviews = subviews;
    if (editors) top.PlatformExtPoints.editors = editors;
    if (menus) top.PlatformExtPoints.menus = menus;
    if (windows) top.PlatformExtPoints.windows = windows;
    if (themes) top.PlatformExtPoints.themes = themes;
    if (settings) top.PlatformExtPoints.settings = settings;
    if (locales) top.PlatformExtPoints.locales = locales;
}
function addExtPoints({ perspective, shell, view, subview, editor, menu, window, theme, setting, locale } = {}) {
    if (perspective && !top.PlatformExtPoints.perspectives.includes(perspective)) top.PlatformExtPoints.perspectives.push(perspective);
    if (shell && !top.PlatformExtPoints.shells.includes(shell)) top.PlatformExtPoints.shells.push(shell);
    if (view && !top.PlatformExtPoints.views.includes(view)) top.PlatformExtPoints.views.push(view);
    if (subview && !top.PlatformExtPoints.subviews.includes(subview)) top.PlatformExtPoints.subviews.push(subview);
    if (editor && !top.PlatformExtPoints.editors.includes(editor)) top.PlatformExtPoints.editors.push(editor);
    if (menu && !top.PlatformExtPoints.menus.includes(menu)) top.PlatformExtPoints.menus.push(menu);
    if (window && !top.PlatformExtPoints.windows.includes(window)) top.PlatformExtPoints.windows.push(window);
    if (theme && !top.PlatformExtPoints.themes.includes(theme)) top.PlatformExtPoints.themes.push(theme);
    if (setting && !top.PlatformExtPoints.settings.includes(setting)) top.PlatformExtPoints.settings.push(setting);
    if (locale && !top.PlatformExtPoints.locales.includes(locale)) top.PlatformExtPoints.locales.push(locale);
}
function removeExtPoints({ perspective, shell, view, subview, editor, menu, window, theme, setting, locale } = {}) {
    if (perspective && top.PlatformExtPoints.perspectives.includes(perspective)) top.PlatformExtPoints.perspectives.splice(top.PlatformExtPoints.perspectives.indexOf(perspective), 1);
    if (shell && top.PlatformExtPoints.shells.includes(shell)) top.PlatformExtPoints.shells.splice(top.PlatformExtPoints.shells.indexOf(shell), 1);
    if (view && top.PlatformExtPoints.views.includes(view)) top.PlatformExtPoints.views.splice(top.PlatformExtPoints.views.indexOf(view), 1);
    if (subview && top.PlatformExtPoints.subviews.includes(subview)) top.PlatformExtPoints.subviews.splice(top.PlatformExtPoints.subviews.indexOf(subview), 1);
    if (editor && top.PlatformExtPoints.editors.includes(editor)) top.PlatformExtPoints.editors.splice(top.PlatformExtPoints.editors.indexOf(editor), 1);
    if (menu && top.PlatformExtPoints.menus.includes(menu)) top.PlatformExtPoints.menus.splice(top.PlatformExtPoints.menus.indexOf(menu), 1);
    if (window && top.PlatformExtPoints.windows.includes(window)) top.PlatformExtPoints.windows.splice(top.PlatformExtPoints.windows.indexOf(window), 1);
    if (theme && top.PlatformExtPoints.themes.includes(theme)) top.PlatformExtPoints.themes.splice(top.PlatformExtPoints.themes.indexOf(theme), 1);
    if (setting && top.PlatformExtPoints.settings.includes(setting)) top.PlatformExtPoints.settings.splice(top.PlatformExtPoints.settings.indexOf(setting), 1);
    if (locale && top.PlatformExtPoints.locales.includes(locale)) top.PlatformExtPoints.locales.splice(top.PlatformExtPoints.locales.indexOf(locale), 1);
}
angular.module('platformExtensions', []).factory('Extensions', ($http) => ({
    getViews: (exPoints = top.PlatformExtPoints.views) => {
        return $http.get('/services/js/platform-core/extension-services/views.js', { params: { extensionPoints: exPoints } });
    },
    getSubviews: (exPoints = top.PlatformExtPoints.subviews) => {
        return $http.get('/services/js/platform-core/extension-services/views.js', { params: { extensionPoints: exPoints } });
    },
    getWindows: (exPoints = top.PlatformExtPoints.windows) => {
        return $http.get('/services/js/platform-core/extension-services/views.js', { params: { extensionPoints: exPoints } });
    },
    getSettings: (exPoints = top.PlatformExtPoints.settings) => {
        return $http.get('/services/js/platform-core/extension-services/views.js', { params: { extensionPoints: exPoints } });
    },
    getEditors: (exPoints = top.PlatformExtPoints.editors) => {
        return $http.get('/services/js/platform-core/extension-services/editors.js', { params: { extensionPoints: exPoints } });
    },
    getPerspectives: (exPoints = top.PlatformExtPoints.perspectives) => {
        return $http.get('/services/js/platform-core/extension-services/perspectives.js', { params: { extensionPoints: exPoints } });
    },
    getShells: (exPoints = top.PlatformExtPoints.shells) => {
        return $http.get('/services/js/platform-core/extension-services/shells.js', { params: { extensionPoints: exPoints } });
    },
    getMenus: (exPoints = top.PlatformExtPoints.menus, perspectiveExPoints = top.PlatformExtPoints.perspectives, viewExPoints = top.PlatformExtPoints.views, shellExPoints = top.PlatformExtPoints.shells) => {
        return $http.get('/services/js/platform-core/extension-services/menus.js', { params: { extensionPoints: exPoints, perspectiveExtPoints: perspectiveExPoints, viewExtPoints: viewExPoints, shellExtPoints: shellExPoints } });
    },
    getThemes: (exPoints = top.PlatformExtPoints.themes) => {
        return $http.get('/services/js/platform-core/extension-services/themes.js', { params: { extensionPoints: exPoints } });
    },
    getLocales: (translations, namespaces, exPoints = top.PlatformExtPoints.locales) => {
        return $http.get('/services/js/platform-core/extension-services/locales.js', { params: { extensionPoints: exPoints, translations: translations, namespaces: namespaces } });
    },
}));