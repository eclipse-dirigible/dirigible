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
const processInstances = angular.module('process-instances', ['platformView', 'blimpKit']);
processInstances.constant('Notifications', new NotificationHub());
processInstances.constant('Dialogs', new DialogHub());
processInstances.controller('BpmProcessInstancesView', ($scope, $http, Notifications, Dialogs) => {
    $scope.state = {
        isBusy: true,
    };
    $scope.selectAll = false;
    $scope.searchField = { text: '' };
    $scope.displaySearch = false;
    $scope.instancesList = [];
    let definitions = [];
    $scope.definitionsList = [];
    $scope.definitionVersions = [];
    $scope.pageSize = 10;
    $scope.currentPage = 1;
    $scope.selected = {
        definitionKey: null,
        definitionId: null,
        definitionVersion: null,
        instanceId: null
    };

    let currentFetchDataInstance = null;
    let currentFetchDataDefinition = null;

    const fetchDefinitions = (initialLoad = false) => {
        $http.get('/services/bpm/bpm-processes/definitions').then((response) => {
            if (!angular.equals(definitions, response.data)) {
                definitions.length = 0;
                definitions.push(...response.data);
                $scope.definitionsList.length = 0;
                for (let i = 0; i < definitions.length; i++) {
                    if (!$scope.definitionsList.some(e => e.value === definitions[i].key)) {
                        $scope.definitionsList.push({
                            value: definitions[i].key,
                            text: definitions[i].name,
                            secondaryText: definitions[i].key,
                        });
                    }
                }
                if ($scope.definitionsList.length) {
                    if (!$scope.selected.definitionKey || !$scope.definitionsList.some(e => e.value === $scope.selected.definitionKey))
                        $scope.selected.definitionKey = $scope.definitionsList[0].value;
                    $scope.definitionSelected(initialLoad);
                }
            }
            if (!currentFetchDataInstance) {
                getInstances({ initialLoad: initialLoad });
            } else {
                $scope.state.isBusy = false;
            }
        }, (error) => {
            console.error(error);
            $scope.state.isBusy = false;
            if (initialLoad) Dialogs.showAlert({
                title: 'Definitions load failed',
                message: 'Could not load definitions. See console for more information.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    const getDefinitions = (initialLoad = false) => {
        if (currentFetchDataDefinition) {
            currentFetchDataDefinition = clearInterval(currentFetchDataDefinition);
        }

        fetchDefinitions(initialLoad);

        currentFetchDataDefinition = setInterval(fetchDefinitions, 10000);
    };

    const fetchInstances = (initialLoad = false) => {
        $http.get('/services/bpm/bpm-processes/instances', { params: { 'businessKey': $scope.searchField.text, 'key': $scope.selected.definitionKey, 'limit': 100 } })
            .then((response) => {
                if (!angular.equals($scope.instancesList, response.data)) {
                    $scope.instancesList.length = 0;
                    $scope.instancesList.push(...response.data);
                }
            }, (error) => {
                console.error(error);
                if (initialLoad) Dialogs.showAlert({
                    title: 'Instances load failed',
                    message: 'Could not load instances. See console for more information.',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            }).finally(() => {
                $scope.$evalAsync(() => {
                    $scope.state.isBusy = false;
                });
            });
    };

    const getInstances = ({ pageNumber = $scope.currentPage, pageSize = $scope.pageSize, initialLoad = false } = {}) => {
        const startIndex = (pageNumber - 1) * pageSize;
        if (startIndex >= $scope.totalRows) return;

        if (currentFetchDataInstance) {
            currentFetchDataInstance = clearInterval(currentFetchDataInstance);
        }

        fetchInstances(initialLoad);

        currentFetchDataInstance = setInterval(fetchInstances, 5000);
    };

    $scope.reload = () => {
        $scope.state.isBusy = true;
        definitions.length = 0;
        $scope.instancesList.length = 0;
        $scope.selected.instanceId = null;
        if (currentFetchDataDefinition) {
            clearInterval(currentFetchDataDefinition);
            currentFetchDataDefinition = null;
        }
        if (currentFetchDataInstance) {
            clearInterval(currentFetchDataInstance);
            currentFetchDataInstance = null;
        }
        getDefinitions();
    };

    $scope.getRelativeTime = (dateTimeString) => {
        return formatRelativeTime(new Date(dateTimeString));
    };

    $scope.start = () => {
        Dialogs.showFormDialog({
            title: `Start process [${JSON.stringify($scope.selected.definitionKey)}]`,
            form: {
                'businessKey': {
                    label: 'Business Key',
                    controlType: 'input',
                    rows: 9,
                    type: 'text',
                    placeholder: 'Business Key',
                    value: ``,
                    submitOnEnter: true,
                    focus: true,
                    required: false
                },
                'parameters': {
                    label: 'Parameters',
                    controlType: 'textarea',
                    rows: 9,
                    type: 'text',
                    placeholder: `${JSON.stringify({ param1: 'value1', param2: true, paramsArray: [1, 2, 3] }, null, 4)}`,
                    value: ``,
                    submitOnEnter: true,
                    focus: true,
                    required: false
                },
            },
            submitLabel: 'Start',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.startProcess($scope.selected.definitionKey, form['businessKey'], form['parameters']);
            }
        }, (error) => {
            console.error(error);
            Dialogs.showAlert({
                title: 'Start process error',
                message: 'There was an error while starting new process.\nPlease look at the console for more information.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.startProcess = (processDefinitionKey, businessKey, parameters) => {
        const apiUrl = '/services/bpm/bpm-processes/instance';
        const requestBody = {
            processDefinitionKey: processDefinitionKey,
            businessKey: businessKey,
            parameters: parameters
        };

        $http({
            method: 'POST',
            url: apiUrl,
            data: requestBody,
            headers: { 'Content-Type': 'application/json' }
        }).then(() => {
            // console.log('Successfully modified variable with name [' + varName + '] and value [' + varValue + ']');
            $scope.reload();
        }).catch((error) => {
            console.error('Error making POST request:', error);
            Dialogs.showAlert({
                title: 'Request error',
                message: 'Please look at the console for more information',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.openDialog = (instance) => {
        Dialogs.showWindow({
            id: 'bpm-process-instances-details',
            params: {
                instance: instance,
            },
            closeButton: true,
        });
    };

    $scope.definitionSelected = (initialLoad = false) => {
        $scope.definitionVersions.length = 0;
        $scope.selected.instanceId = null;
        $scope.selected.definitionVersion = null;
        for (let i = 0; i < definitions.length; i++) {
            if (definitions[i].key === $scope.selected.definitionKey) {
                $scope.definitionVersions.push({
                    id: definitions[i].id,
                    label: definitions[i].version
                });
            }
        }
        $scope.definitionVersions.sort((a, b) => b.label - a.label);
        if ($scope.definitionVersions.length) {
            $scope.selected.definitionId = $scope.definitionVersions[0].id;
            $scope.selected.definitionVersion = $scope.definitionVersions[0].label;
            $scope.versionSelected();
        }
        if (!initialLoad) getInstances();
    };

    $scope.versionSelected = () => {
        for (let i = 0; i < $scope.definitionVersions.length; i++) {
            if ($scope.definitionVersions[i].id === $scope.selected.definitionId) {
                $scope.selected.definitionVersion = $scope.definitionVersions[i].label;
            }
        }
        Dialogs.postMessage({ topic: 'bpm.definition.selected', data: { id: $scope.selected.definitionId, key: $scope.selected.definitionKey } });
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
        $http({
            method: 'POST',
            url: `/services/bpm/bpm-processes/instance/${$scope.selected.instanceId}`,
            data: requestBody,
            headers: { 'Accept': 'text/plain', 'Content-Type': 'application/json' },
        }).then(() => {
            Notifications.show({
                title: 'Action confirmation',
                description: `${actionName} triggered successfully!`,
                type: 'positive',
            });
            $scope.reload();
        }, (error) => {
            console.error('Error making POST request:', error);
            Dialogs.showAlert({
                title: 'Action failed',
                message: `${actionName} operation failed.\n${error.data}`,
                type: AlertTypes.Error,
                preformatted: true,
            });
        });
    };

    $scope.selectionChanged = (instance) => {
        if ($scope.selected.instanceId === instance.id) {
            Dialogs.postMessage({ topic: 'bpm.instance.selected', data: { deselect: true } });
            $scope.selected.instanceId = null;
        } else {
            Dialogs.postMessage({ topic: 'bpm.instance.selected', data: { instance: instance.id } });
            $scope.selected.instanceId = instance.id;
        }
    };

    $scope.getNoDataMessage = () => {
        return $scope.searchField.text ? 'No instances found.' : 'No instances have been detected.';
    };

    $scope.inputSearchKeyUp = (e) => {
        switch (e.key) {
            case 'Escape':
                $scope.searchField.text = '';
                toggleSearch();
                getInstances();
                break;
            case 'Enter':
                getInstances();
                break;
        }
    };

    $scope.search = () => {
        getInstances();
    };

    $scope.onPageChange = (pageNumber) => {
        getInstances({ pageNumber });
    };

    $scope.onItemsPerPageChange = (itemsPerPage) => {
        getInstances({ pageSize: itemsPerPage });
    };

    $scope.deleteSelected = () => {
        $scope.instancesList.reduce((ret, instance) => {
            if (instance.selected) ret.push(instance.id);
            return ret;
        }, []);
    };

    Dialogs.addMessageListener({
        topic: 'bpm.process.instances.get-definition',
        handler: () => {
            if ($scope.selected.definitionId) {
                Dialogs.postMessage({ topic: 'bpm.definition.selected', data: { id: $scope.selected.definitionId, key: $scope.selected.definitionKey } });
            } else if (!$scope.state.isBusy) {
                Dialogs.postMessage({ topic: 'bpm.definition.selected', data: { noData: true } });
            }
        }
    });

    Dialogs.addMessageListener({
        topic: 'bpm.historic.instance.selected',
        handler: () => {
            $scope.$evalAsync(() => {
                $scope.selected.instanceId = null;
            });
        }
    });

    getDefinitions(true);
});