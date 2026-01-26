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
const settings = angular.module('settings', ['platformView', 'platformSplit', 'blimpKit']);
settings.constant('Dialog', new DialogHub());
settings.controller('SettingsController', ($scope, Extensions, Dialog) => {
    $scope.settings = [];

    $scope.switchSetting = (id) => {
        $scope.activeId = id;
    };

    Extensions.getSettings().then((response) => {
        $scope.settings.push(...response.data);
        $scope.activeId = $scope.settings[0].id;
    }, (error) => {
        console.error(error);
        Dialog.showAlert({
            title: 'Failed to load settings',
            message: 'There was an error while trying to load the settings list.',
            type: AlertTypes.Error,
            preformatted: false,
        });
    });
});