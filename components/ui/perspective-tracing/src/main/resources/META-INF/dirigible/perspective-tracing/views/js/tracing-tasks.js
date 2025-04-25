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
const ideTracingTasksView = angular.module('ide-tracing-tasks', ['platformView', 'blimpKit']);
ideTracingTasksView.constant('Notifications', new NotificationHub());
ideTracingTasksView.controller('IDETracingTasksViewController', ($scope, $http, $timeout, Notifications) => {
    $scope.selectAll = false;
    $scope.searchField = { text: '' };
    $scope.filterBy = "";
    $scope.displaySearch = false;
    $scope.displayTracing = false;
    $scope.tasksList = [];
    $scope.pageSize = 10;
    $scope.currentPage = 1;

    $scope.currentFetchDataTask = null;

    const fetchData = (args) => {
        if ($scope.currentFetchDataTask) {
            clearInterval($scope.currentFetchDataTask);
        }

        const pageNumber = (args && args.pageNumber) || $scope.currentPage;
        const pageSize = (args && args.pageSize) || $scope.pageSize;
        const limit = pageNumber * pageSize;
        const startIndex = (pageNumber - 1) * pageSize;
        if (startIndex >= $scope.totalRows) {
            return;
        }

        if ($scope.filterBy && $scope.filterBy !== '') {
            $http.get('/services/core/tracing/search', { params: { 'execution': $scope.filterBy } })
                .then((response) => {
                    $scope.tasksList = response.data;
                });
        } else {
            $http.get('/services/core/tracing')
                .then((response) => {
                    $scope.tasksList = response.data;
                });
        }



        $http.get('/services/js/perspective-tracing/service/enable-tracing.js')
            .then((response) => {
                $scope.displayTracing = ('true' === response.data);
            });
    }

    fetchData();

    setInterval(() => {
        fetchData();
    }, 10000);

    $scope.reload = () => {
        fetchData();
    };

    $scope.clean = () => {
        $http.delete('/services/core/tracing')
            .then((response) => {
                $scope.tasksList = response.data;
            });
    };

    $scope.toggleSearch = () => {
        $scope.displaySearch = !$scope.displaySearch;
    };

    $scope.toggleTracing = () => {
        $http.post('/services/js/perspective-tracing/service/enable-tracing.js')
            .then((response) => {
                $scope.displayTracing = ('true' === response.data);
            });
    };

    $scope.selectAllChanged = () => {
        for (let task of $scope.tasksList) {
            task.selected = $scope.selectAll;
        }
    };

    $scope.selectionChanged = (task) => {
        $scope.selectAll = $scope.tasksList.every(x => x.selected = false);
        Notifications.postMessage({ topic: 'tracing.task', data: { task: task.id } });
        Notifications.postMessage({ topic: 'tracing.task.selected', data: { task: task.id } });
        task.selected = true;
    };

    $scope.clearSearch = () => {
        $scope.searchField.text = "";
        $scope.filterBy = "";
        fetchData();
    };

    $scope.getSelectedCount = () => {
        return $scope.tasksList.reduce((c, task) => {
            if (task.selected) c++;
            return c;
        }, 0);
    };

    $scope.hasSelected = () => $scope.tasksList.some(x => x.selected);

    $scope.applyFilter = () => {
        $scope.filterBy = $scope.searchField.text;
        fetchData();
    };

    $scope.getNoDataMessage = () => {
        return $scope.filterBy ? 'No tasks found.' : 'No tasks have been detected.';
    };

    $scope.inputSearchKeyUp = (e) => {
        if ($scope.lastSearchKeyUp) {
            $timeout.cancel($scope.lastSearchKeyUp);
            $scope.lastSearchKeyUp = null;
        }

        switch (e.key) {
            case 'Escape':
                $scope.searchField.text = '';
                $scope.applyFilter();
                break;
            case 'Enter':
                $scope.applyFilter();
                break;
            default:
                if ($scope.filterBy !== $scope.searchField.text) {
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
        $scope.tasksList.reduce((ret, task) => {
            if (task.selected)
                ret.push(task.id);
            return ret;
        }, []);
    };

    $scope.getStatusClasses = (status) => {
        let classes = 'sap-icon ';
        switch (status) {
            case 'STARTED':
                classes += 'sap-icon--play';
                break;
            case 'SUCCESSFUL':
                classes += 'sap-icon--status-positive sap-icon--color-positive';
                break;
            case 'FAILED':
                classes += 'sap-icon--status-error sap-icon--color-negative';
                break;
            default:
                classes += 'sap-icon--question-mark';
        }
        return classes;
    };

    $scope.getTypeClasses = (type) => {
        let classes = 'sap-icon ';
        switch (type) {
            case 'JOB':
                classes += 'sap-icon--history';
                break;
            case 'BPM':
                classes += 'sap-icon--process';
                break;
            case 'ETL':
                classes += 'sap-icon--combine';
                break;
            case 'MQ':
                classes += 'sap-icon--email';
                break;
            default:
                classes += 'sap-icon--question-mark';
        }
        return classes;
    };
});