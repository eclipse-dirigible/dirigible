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
const ideBpmProcessInstancesView = angular.module('ide-bpm-process-instances', ['platformView', 'blimpKit']);
ideBpmProcessInstancesView.constant('Notifications', new NotificationHub());
ideBpmProcessInstancesView.constant('Dialogs', new DialogHub());
ideBpmProcessInstancesView.controller('IDEBpmProcessInstancesViewController', ($scope, $http, Notifications, Dialogs) => {

    $scope.selectAll = false;
    $scope.searchField = { text: '' };
    $scope.displaySearch = false;
    $scope.instancesList = [];
    $scope.pageSize = 10;
    $scope.currentPage = 1;
    $scope.selectedProcessInstanceId = null;
    $scope.selectedProcessDefinitionKey = null;

    $scope.currentFetchDataInstance = null;

    const fetchData = (args) => {
        if ($scope.currentFetchDataInstance) {
            clearInterval($scope.currentFetchDataInstance);
        }

        $scope.currentFetchDataInstance = setInterval(() => {
            const pageNumber = (args && args.pageNumber) || $scope.currentPage;
            const pageSize = (args && args.pageSize) || $scope.pageSize;
            // const limit = pageNumber * pageSize;
            const startIndex = (pageNumber - 1) * pageSize;
            if (startIndex >= $scope.totalRows) {
                return;
            }

            $http.get('/services/bpm/bpm-processes/instances', { params: { 'businessKey': $scope.searchField.text, 'key': $scope.selectedProcessDefinitionKey, 'limit': 100 } })
                .then((response) => {
                    if ($scope.instancesList.length < response.data.length) {
                        Notifications.show({
                            type: 'information',
                            title: 'Process instances',
                            description: 'A new process instance has been started.'
                        });
                    }
                    $scope.instancesList = response.data;
                }, (error) => {
                    console.error(error);
                });
        }, 5000);

    }

    fetchData();

    $scope.reload = () => {
        fetchData();
    };

    $scope.toggleSearch = () => {
        $scope.displaySearch = !$scope.displaySearch;
    };

    $scope.retry = () => {
        $scope.executeAction({ 'action': 'RETRY' }, 'RETRY');
    };

    $scope.skip = () => {
        $scope.executeAction({ 'action': 'SKIP' }, 'SKIP');
    };

    $scope.executeAction = (requestBody, actionName) => {
        const apiUrl = '/services/bpm/bpm-processes/instance/' + $scope.selectedProcessInstanceId;

        $http({
            method: 'POST',
            url: apiUrl,
            data: requestBody,
            headers: { 'Content-Type': 'application/json' }
        }).then(() => {
            Notifications.show({
                title: 'Action confirmation',
                description: actionName + " triggered successfully!",
                type: 'positive',
            });
            $scope.reload();
        }).catch((error) => {
            console.error('Error making POST request:', error);
            Dialogs.showAlert({
                title: 'Action failed',
                message: actionName + ' operation failed. Error message ' + error.message,
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    }

    // $scope.selectAllChanged = () => {
    //     for (let instance of $scope.instancesList) {
    //         instance.selected = $scope.selectAll;
    //     }
    // };

    $scope.selectionChanged = (instance) => {
        $scope.selectAll = $scope.instancesList.every(x => x.selected = false);
        Notifications.postMessage({ topic: 'bpm.diagram.instance', data: { instance: instance.id } });
        Notifications.postMessage({ topic: 'bpm.instance.selected', data: { instance: instance.id } });
        instance.selected = true;
        $scope.selectedProcessInstanceId = instance.id;
    };

    Notifications.addMessageListener({
        topic: 'bpm.definition.selected',
        handler: (data) => {
            $scope.$evalAsync(() => {
                if (data.hasOwnProperty('definition')) {
                    $scope.selectedProcessDefinitionKey = data.definition;
                    $scope.applyFilter();
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

    $scope.getSelectedCount = () => {
        return $scope.instancesList.reduce((c, instance) => {
            if (instance.selected) c++;
            return c;
        }, 0);
    };

    $scope.hasSelected = () => $scope.instancesList.some(x => x.selected);

    $scope.applyFilter = () => {
        $http.get('/services/bpm/bpm-processes/instances', { params: { 'businessKey': $scope.searchField.text, 'key': $scope.selectedProcessDefinitionKey, 'limit': 100 } })
            .then((response) => {
                $scope.instancesList = response.data;
            }, (error) => {
                console.error(error);
            });
    };

    $scope.getNoDataMessage = () => {
        return $scope.searchField.text ? 'No instances found.' : 'No instances have been detected.';
    };

    $scope.inputSearchKeyUp = (e) => {
        switch (e.key) {
            case 'Escape':
                $scope.searchField.text = '';
                $scope.applyFilter();
                break;
            case 'Enter':
                $scope.applyFilter();
                break;
        }
    }

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
        $scope.instancesList.reduce((ret, instance) => {
            if (instance.selected) ret.push(instance.id);
            return ret;
        }, []);
    };
});