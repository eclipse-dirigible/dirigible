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
const general = angular.module('general', ['ngCookies', 'blimpKit', 'platformView', 'platformLocale']);
general.controller('GeneralController', ($scope, $http, $cookies, $window, theming, ButtonStates, LocaleService) => {
    const dialogHub = new DialogHub();
    const themingHub = new ThemingHub();
    const brandingInfo = getBrandingInfo();
    $scope.shellConfig = new ShellHub().getConfig();
    const autoRevealKey = `${brandingInfo.prefix}.settings.general.autoReveal`;
    const navCondensedKey = `${brandingInfo.prefix}.${$scope.shellConfig['id']}.settings.general.nav.condensed`;
    $scope.themes = [];
    $scope.switches = {
        autoReveal: getAutoReveal(),
        navCondensed: $scope.shellConfig.hasOwnProperty('id') ? getNavCondensed() : undefined
    };

    function getAutoReveal() {
        let autoReveal = $window.localStorage.getItem(autoRevealKey);
        if (autoReveal === null) {
            autoReveal = true;
            $window.localStorage.setItem(autoRevealKey, 'true');
        } else autoReveal = JSON.parse(autoReveal);
        return autoReveal;
    }

    function getNavCondensed() {
        let navCondensed = $window.localStorage.getItem(navCondensedKey);
        if (navCondensed === null) {
            navCondensed = true;
            $window.localStorage.setItem(navCondensedKey, 'true');
        } else navCondensed = JSON.parse(navCondensed);
        return navCondensed;
    }

    const themesLoadedListener = themingHub.onThemesLoaded(() => {
        $scope.$evalAsync(() => {
            $scope.themes = theming.getThemes();
        });
        themingHub.removeMessageListener(themesLoadedListener)
    });
    $scope.currentTheme = theming.getCurrentTheme();

    $scope.setTheme = (themeId, name) => {
        $scope.currentTheme.id = themeId;
        $scope.currentTheme.name = name;
        theming.setTheme(themeId);
    };

    $scope.autoRevealChange = () => {
        $window.localStorage.setItem(autoRevealKey, `${$scope.switches.autoReveal}`);
        themingHub.postMessage({
            topic: 'general.settings.projects',
            data: { setting: 'autoReveal', value: $scope.switches.autoReveal }
        });
    };

    $scope.navCondensedChange = () => {
        $window.localStorage.setItem(navCondensedKey, `${$scope.switches.navCondensed}`);
        themingHub.postMessage({
            topic: 'general.settings.navigation',
            data: { setting: 'condensed', value: $scope.switches.navCondensed }
        });
    };

    $scope.resetAll = () => {
        dialogHub.showDialog({
            title: LocaleService.t('reset', 'Reset'),
            message: LocaleService.t('perspective-settings:resetMsg', { brand: brandingInfo.name }),
            buttons: [
                { id: 'yes', label: LocaleService.t('yes', 'Yes'), state: ButtonStates.Emphasized },
                { id: 'no', label: LocaleService.t('no', 'No') }
            ],
            preformatted: true,
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
                    dialogHub.closeBusyDialog();
                    top.location.reload();
                }, (error) => {
                    console.error(error);
                    dialogHub.closeBusyDialog();
                    dialogHub.showAlert({
                        title: LocaleService.t('perspective-settings:errMsg.resetTitle', 'Failed to reset'),
                        message: LocaleService.t('perspective-settings:errMsg.reset', 'There was an error during the reset process. Please refresh manually.'),
                        type: AlertTypes.Error,
                        preformatted: false,
                    });
                });
            }
        });
    };
});