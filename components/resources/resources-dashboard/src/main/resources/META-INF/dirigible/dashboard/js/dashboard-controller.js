/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const dashboard = angular.module('dashboard', ['blimpKit', 'platformView', 'platformLocale']);
dashboard.controller('DashboardController', ($scope, Extensions, LocaleService) => {
    $scope.loadingLabel = 'Loading...';
    $scope.errorMessage = 'Failed to load widget list';
    LocaleService.onInit(() => {
        $scope.loadingLabel = `${LocaleService.t('loading')}...`;
        $scope.errorMessage = LocaleService.t('dashboard:errMsg.widgetList');
    });
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: $scope.loadingLabel,
    };

    $scope.smallWidgets = [];
    $scope.mediumWidgets = [];
    $scope.largeWidgets = [];

    Extensions.getSubviews(['dashboard-widgets']).then((response) => {
        response.data.forEach(widget => {
            if (widget.size === 'small') {
                $scope.smallWidgets.push(widget);
            } else if (widget.size === 'medium') {
                $scope.mediumWidgets.push(widget);
            } else {
                $scope.largeWidgets.push(widget);
            }
        });
        $scope.state.isBusy = false;
    }).catch((error) => {
        console.error('Error fetching widget list:', error);
        $scope.state.error = true;
    });
});