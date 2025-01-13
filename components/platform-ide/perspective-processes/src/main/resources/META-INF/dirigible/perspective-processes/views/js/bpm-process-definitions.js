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
const ideBpmProcessDefinitionsView = angular.module('ide-bpm-process-definitions', ['platformView', 'blimpKit']);
ideBpmProcessDefinitionsView.constant('Notifications', new NotificationHub());
ideBpmProcessDefinitionsView.controller('IDEBpmProcessDefinitionsViewController', ($scope, $http, $timeout, Notifications) => {
    $scope.selectAll = false;
    $scope.searchText = "";
    $scope.filterBy = "";
    $scope.displaySearch = false;
    $scope.definitionsList = [];
    $scope.pageSize = 10;
    $scope.currentPage = 1;

    $scope.currentFetchDataDefinition = null;

    const fetchData = (args) => {
        if ($scope.currentFetchDataDefinition) {
            clearInterval($scope.currentFetchDataDefinition);
        }

        $scope.currentFetchDatadDefinition = setInterval(() => {
            const pageNumber = (args && args.pageNumber) || $scope.currentPage;
            const pageSize = (args && args.pageSize) || $scope.pageSize;
            const limit = pageNumber * pageSize;
            const startIndex = (pageNumber - 1) * pageSize;
            if (startIndex >= $scope.totalRows) {
                return;
            }

            $http.get('/services/bpm/bpm-processes/definitions', { params: { 'condition': $scope.filterBy, 'limit': limit } })
                .then((response) => {
                    if ($scope.definitionsList.length < response.data.length) {
                        Notifications.show({
                            type: 'information',
                            title: 'User definitions',
                            description: 'A new user task has been added.'
                        });
                    }
                    $scope.definitionsList = response.data;
                });
        }, 10000);
    }

    fetchData();

    $scope.reload = () => {
        fetchData();
    };

    $scope.toggleSearch = () => {
        $scope.displaySearch = !$scope.displaySearch;
    };

    $scope.selectAllChanged = () => {
        for (let definition of $scope.definitionsList) {
            definition.selected = $scope.selectAll;
        }
    };

    $scope.selectionChanged = (definition) => {
        $scope.selectAll = $scope.definitionsList.every(x => x.selected = false);
        Notifications.postMessage({ topic: 'bpm.diagram.definition', data: { definition: definition.key } });
        Notifications.postMessage({ topic: 'bpm.definition.selected', data: { definition: definition.key } });
        definition.selected = true;
    };

    $scope.clearSearch = () => {
        $scope.searchText = "";
        $scope.filterBy = "";
        fetchData();
    };

    $scope.getSelectedCount = () => {
        return $scope.definitionsList.reduce((c, definition) => {
            if (definition.selected) c++;
            return c;
        }, 0);
    };

    $scope.hasSelected = () => $scope.definitionsList.some(x => x.selected);

    $scope.applyFilter = () => {
        $scope.filterBy = $scope.searchText;
        fetchData();
    };

    $scope.getNoDataMessage = () => {
        return $scope.filterBy ? 'No definitions found.' : 'No definitions have been detected.';
    };

    $scope.inputSearchKeyUp = (e) => {
        if ($scope.lastSearchKeyUp) {
            $timeout.cancel($scope.lastSearchKeyUp);
            $scope.lastSearchKeyUp = null;
        }

        switch (e.key) {
            case 'Escape':
                $scope.searchText = $scope.filterBy || '';
                break;
            case 'Enter':
                $scope.applyFilter();
                break;
            default:
                if ($scope.filterBy !== $scope.searchText) {
                    $scope.lastSearchKeyUp = $timeout(() => {
                        $scope.lastSearchKeyUp = null;
                        $scope.applyFilter();
                    }, 250);
                }
                break;
        }
    };

    $scope.onPageChange = (pageNumber) => {
        fetchData({ pageNumber });
    };

    $scope.onItemsPerPageChange = (itemsPerPage) => {
        fetchData({ pageSize: itemsPerPage });
    };

    $scope.refresh = () => {
        fetchData();
    };

    $scope.deleteSelected = () => {
        $scope.definitionsList.reduce((ret, definition) => {
            if (definition.selected)
                ret.push(definition.id);
            return ret;
        }, []);
    };
});