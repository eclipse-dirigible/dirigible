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
const reports = angular.module('reports', ['platformView', 'platformSplit', 'blimpKit']);
reports.controller('ReportsController', ($scope, Extensions) => {
    const Dialog = new DialogHub();
    $scope.search = { text: '' };
    $scope.reports = [];

    $scope.switchReport = (id) => {
        $scope.activeId = id;
    };

    $scope.clearSearch = () => {
        $scope.search.text = '';
        for (let i = 0; i < $scope.reports.length; i++) {
            $scope.reports[i].hide = false;
        }
    };

    $scope.filter = () => {
        for (let i = 0; i < $scope.reports.length; i++) {
            if ($scope.reports[i].label.toLocaleLowerCase().includes($scope.search.text.toLocaleLowerCase())) {
                $scope.reports[i].hide = false;
            } else $scope.reports[i].hide = true;
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

    Extensions.getViews(['application-reports']).then((response) => {
        $scope.reports.push(...response.data);
        if ($scope.reports.length) $scope.activeId = $scope.reports[0].id;
    }, (error) => {
        console.log(error);
        Dialog.showAlert({
            title: 'Failed to load reports',
            message: 'There was an error while trying to load the reports list.',
            type: AlertTypes.Error,
            preformatted: false,
        });
    });
});