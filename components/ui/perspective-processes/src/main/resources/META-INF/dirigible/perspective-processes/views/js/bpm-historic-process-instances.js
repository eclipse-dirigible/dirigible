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
const historicProcessInstances = angular.module('historic-process-instances', ['platformView', 'blimpKit']);
historicProcessInstances.constant('Dialogs', new DialogHub());
historicProcessInstances.controller('BpmHistoricProcessInstancesView', ($scope, $http, Dialogs) => {
    $scope.instances = [];
    $scope.searchField = { text: '' };
    $scope.displaySearch = false;
    $scope.selectedProcessDefinitionKey = null;
    $scope.selectedId;
    let refreshIntervalId;

    $scope.fetchData = () => {
        $http.get('/services/bpm/bpm-processes/historic-instances', { params: { 'businessKey': $scope.searchField.text, 'definitionKey': $scope.selectedProcessDefinitionKey, 'limit': 100 } })
            .then((response) => {
                $scope.instances = response.data;
            }, (error) => {
                console.error(error);
            });
    };

    $scope.getRelativeTime = (dateTimeString) => {
        return formatRelativeTime(new Date(dateTimeString));
    };

    $scope.openDialog = (instance) => {
        Dialogs.showWindow({
            id: 'bpm-historic-process-instances-details',
            params: {
                instance: instance,
            },
            closeButton: true,
        });
    };

    $scope.selectionChanged = (instance) => {
        if ($scope.selectedId === instance.id) {
            Dialogs.postMessage({ topic: 'bpm.historic.instance.selected', data: { deselect: true } });
            $scope.selectedId = null;
        } else {
            $scope.selectedId = instance.id;
            Dialogs.postMessage({ topic: 'bpm.historic.instance.selected', data: { instance: instance.id, definition: instance.processDefinitionId } });
        }
    };

    $scope.toggleSearch = () => {
        $scope.displaySearch = !$scope.displaySearch;
    };

    let defIntervalId = setInterval(() => {
        if (!$scope.selectedProcessDefinitionKey) Dialogs.triggerEvent('bpm.process.instances.get-definition');
        else cancelIntervalDef();
    }, 500);

    function cancelIntervalDef() {
        defIntervalId = null;
        clearInterval(defIntervalId);
    }

    Dialogs.addMessageListener({
        topic: 'bpm.definition.selected',
        handler: (data) => {
            if (data.noData) cancelIntervalDef();
            else if (data.hasOwnProperty('key')) {
                if (defIntervalId) cancelIntervalDef();
                clearInterval(refreshIntervalId);
                $scope.$evalAsync(() => {
                    $scope.selectedId = null;
                    $scope.selectedProcessDefinitionKey = data.key;
                    $scope.fetchData();
                    refreshIntervalId = setInterval(() => { $scope.fetchData() }, 5000);
                });
            } else {
                Dialogs.showAlert({
                    title: 'Missing data',
                    message: 'Process definition key is missing from event!',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            }

        }
    });

    Dialogs.addMessageListener({
        topic: 'bpm.instance.selected',
        handler: () => {
            $scope.$evalAsync(() => {
                $scope.selectedId = null;
            });
        }
    });

    $scope.inputSearchKeyUp = (e) => {
        switch (e.key) {
            case 'Escape':
                $scope.searchField.text = '';
                toggleSearch();
                $scope.fetchData();
                break;
            case 'Enter':
                $scope.fetchData();
                break;
        }
    };
});