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
const ideBpmHistoricProcessInstancesView = angular.module('ide-bpm-historic-process-instances', ['platformView', 'blimpKit']);
ideBpmHistoricProcessInstancesView.constant('Dialogs', new DialogHub());
ideBpmHistoricProcessInstancesView.controller('IDEBpmHistoricProcessInstancesViewController', ($scope, $http, Dialogs) => {
    $scope.instances = [];
    $scope.model = {};
    $scope.model.searchText = '';
    $scope.displaySearch = false;
    $scope.selectedProcessDefinitionKey = null;

    setInterval(() => { $scope.fetchData() }, 5000);

    $scope.fetchData = () => {
        $http.get('/services/bpm/bpm-processes/historic-instances', { params: { 'businessKey': $scope.model.searchText, 'definitionKey': $scope.selectedProcessDefinitionKey, 'limit': 100 } })
            .then((response) => {
                $scope.instances = response.data;
            });
    };

    $scope.selectionChanged = (instance) => {
        MessageHub.postMessage({ topic: 'bpm.historic.instance.selected', data: { instance: instance.id } });
    };

    $scope.toggleSearch = () => {
        $scope.displaySearch = !$scope.displaySearch;
    };

    $scope.applyFilter = () => {
        $http.get('/services/bpm/bpm-processes/historic-instances', { params: { 'businessKey': $scope.model.searchText, 'definitionKey': $scope.selectedProcessDefinitionKey, 'limit': 100 } })
            .then((response) => {
                $scope.instances = response.data;
            });
    };

    Dialogs.addMessageListener({
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

    $scope.inputSearchKeyUp = (e) => {
        switch (e.key) {
            case 'Escape':
                $scope.model.searchText = '';
                break;
            case 'Enter':
                $scope.applyFilter();
                break;
        }
    };
});