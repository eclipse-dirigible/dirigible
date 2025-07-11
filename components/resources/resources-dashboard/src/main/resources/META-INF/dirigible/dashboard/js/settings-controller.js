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
const settings = angular.module('settings', ['platformView', 'platformSplit', 'blimpKit', 'platformLocale']);
settings.controller('SettingsController', ($scope, Extensions, LocaleService) => {
    const Dialog = new DialogHub();
    $scope.search = { text: '' };
    $scope.settings = [];

    $scope.switchSetting = (id) => {
        $scope.activeId = id;
    };

    $scope.clearSearch = () => {
        $scope.search.text = '';
        for (let i = 0; i < $scope.settings.length; i++) {
            $scope.settings[i].hide = false;
        }
    };

    $scope.filter = () => {
        for (let i = 0; i < $scope.settings.length; i++) {
            if ($scope.settings[i].label.toLocaleLowerCase().includes($scope.search.text.toLocaleLowerCase())) {
                $scope.settings[i].hide = false;
            } else $scope.settings[i].hide = true;
        }
    };

    let to = 0;
    $scope.searchContent = () => {
        if (to) { clearTimeout(to); }
        to = setTimeout(() => {
            $scope.$evalAsync(() => {
                $scope.filter();
            });
        }, 150);
    };

    Extensions.getSettings().then((response) => {
        $scope.settings.push(...response.data);
        if ($scope.settings.length) $scope.activeId = $scope.settings[0].id;
    }, (error) => {
        console.log(error);
        Dialog.showAlert({
            title: LocaleService.t('dashboard:errMsg.reportLoadTitle', 'Failed to load settings'),
            message: LocaleService.t('dashboard:errMsg.reportLoad', 'There was an error while trying to load the settings list.'),
            type: AlertTypes.Error,
            preformatted: false,
        });
    });
});