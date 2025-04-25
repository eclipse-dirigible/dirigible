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
const ideTracingVariablesView = angular.module('ide-tracing-variables', ['platformView', 'blimpKit']);
ideTracingVariablesView.constant('Notifications', new NotificationHub());
ideTracingVariablesView.constant('Dialogs', new DialogHub());
ideTracingVariablesView.controller('IDETracingVariablesViewController', ($scope, $http, Notifications, Dialogs) => {

    $scope.selectAll = false;
    $scope.searchText = "";
    $scope.displaySearch = false;
    $scope.variablesList = [];
    $scope.pageSize = 10;
    $scope.currentPage = 1;
    $scope.selectedTaskId = null;
    $scope.selectedTask = null;

    // $scope.reload = () => {
    //     fetchData();
    // };

    $scope.toggleSearch = () => {
        $scope.displaySearch = !$scope.displaySearch;
    };

    Notifications.addMessageListener({
        topic: 'tracing.task.selected',
        handler: (data) => {
            $scope.$evalAsync(() => {
                if (data.hasOwnProperty('task')) {
                    $scope.selectedTaskId = data.task;
                    $http.get('/services/core/tracing/' + $scope.selectedTaskId)
                        .then((response) => {
                            $scope.selectedTask = response.data;
                            $scope.variablesList = [];
                            for (const [name, value] of Object.entries(response.data.input)) {
                                $scope.variablesList.push({ name: `${name}`, input: `${value}` });
                            }
                            for (const [name, value] of Object.entries(response.data.output)) {
                                const found = $scope.variablesList.find((element) => element.name === name);
                                if (found) {
                                    found.output = value;
                                } else {
                                    $scope.variablesList.push({ name: `${name}`, input: null, output: `${value}` });
                                }
                            }
                        });
                } else {
                    Dialogs.showAlert({
                        title: 'Missing data',
                        message: 'Process definition is missing from event!',
                        type: AlertTypes.Error,
                        preformatted: false,
                    });
                }
            });
        }
    });

    // $scope.applyFilter = () => {
    //     $http.get('/services/core/tracing', { params: { 'id': $scope.searchText, 'key': $scope.selectedName, 'limit': 100 } })
    //         .then((response) => {
    //             $scope.variablesList = response.data;
    //         });
    // };

    $scope.getNoDataMessage = () => {
        return $scope.searchText ? 'No variables found.' : 'No variables have been detected.';
    };

    // $scope.inputSearchKeyUp = (e) => {
    //     switch (e.key) {
    //         case 'Escape':
    //             $scope.searchText = '';
    //             break;
    //         case 'Enter':
    //             $scope.applyFilter();
    //             break;
    //     }
    // }

    $scope.onPageChange = (pageNumber) => {
        fetchData({ pageNumber });
    };

    $scope.onItemsPerPageChange = (itemsPerPage) => {
        fetchData({ pageSize: itemsPerPage });
    };

    $scope.refresh = () => {
        fetchData();
    };

});