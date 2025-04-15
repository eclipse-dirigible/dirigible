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
const general = angular.module('general', ['ngCookies', 'blimpKit', 'platformView']);
general.controller('GeneralController', ($scope, $http, $cookies, theming, ButtonStates) => {
    const dialogHub = new DialogHub();
    const themingHub = new ThemingHub();
    const brandingInfo = getBrandingInfo();
    $scope.themes = [];

    const themesLoadedListener = themingHub.onThemesLoaded(() => {
        $scope.$apply(() => $scope.themes = theming.getThemes());
        themingHub.removeMessageListener(themesLoadedListener)
    });
    $scope.currentTheme = theming.getCurrentTheme();

    $scope.setTheme = (themeId, name) => {
        $scope.currentTheme.id = themeId;
        $scope.currentTheme.name = name;
        theming.setTheme(themeId);
    };

    $scope.resetAll = () => {
        dialogHub.showDialog({
            title: `Reset ${brandingInfo.brand}`,
            message: `This will clear all general settings, open tabs and cache.\n${brandingInfo.brand} will then reload.\nDo you wish to continue?`,
            buttons: [
                { id: 'yes', label: 'Yes', state: ButtonStates.Emphasized },
                { id: 'no', label: 'No' }
            ],
            closeButton: false
        }).then((buttonId) => {
            if (buttonId === 'yes') {
                dialogHub.showBusyDialog('Resetting...');
                localStorage.clear();
                theming.reset();
                $http.get('/services/js/platform-core/services/clear-cache.js').then(() => {
                    for (let cookie in $cookies.getAll()) {
                        if (cookie.startsWith('DIRIGIBLE')) { // TODO: make this key dynamic
                            $cookies.remove(cookie, { path: '/' });
                        }
                    }
                    location.reload();
                }, (error) => {
                    console.error(error);
                    dialogHub.closeBusyDialog();
                    dialogHub.showAlert({
                        title: 'Failed to reset',
                        message: 'There was an error during the reset process. Please refresh manually.',
                        type: AlertTypes.Error,
                        preformatted: false,
                    });
                });
            }
        });
    };
});