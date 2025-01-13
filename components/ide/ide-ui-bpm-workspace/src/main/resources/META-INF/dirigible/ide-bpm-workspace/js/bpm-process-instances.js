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
let ideBpmProcessInstancesView = angular.module('ide-bpm-process-instances', ['ideUI', 'ideView']);

ideBpmProcessInstancesView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'bpm';
}]);

ideBpmProcessInstancesView.controller('IDEBpmProcessInstancesViewController', ['$scope', '$http', '$timeout', 'messageHub', function ($scope, $http, $timeout, messageHub,) {

    this.selectAll = false;
    this.searchText = "";
    this.displaySearch = false;
    this.instancesList = [];
    this.pageSize = 10;
    this.currentPage = 1;
    this.selectedProcessInstanceId = null;
    this.selectedProcessDefinitionKey = null;

    this.currentFetchDataInstance = null;

    const fetchData = (args) => {
        if (this.currentFetchDataInstance) {
            clearInterval(this.currentFetchDataInstance);
        }

        this.currentFetchDataInstance = setInterval(() => {
            const pageNumber = (args && args.pageNumber) || this.currentPage;
            const pageSize = (args && args.pageSize) || this.pageSize;
            const limit = pageNumber * pageSize;
            const startIndex = (pageNumber - 1) * pageSize;
            if (startIndex >= this.totalRows) {
                return;
            }

            $http.get('/services/bpm/bpm-processes/instances', { params: { 'id': this.searchText, 'key': this.selectedProcessDefinitionKey, 'limit': 100 } })
                .then((response) => {
                    if (this.instancesList.length < response.data.length) {
                        //messageHub.showAlertInfo("User instances", "A new user task has been added");
                    }

                    this.instancesList = response.data;
                });
        }, 5000);

    }

    fetchData();

    $scope.reload = function () {
        fetchData();
    };

    this.toggleSearch = function () {
        this.displaySearch = !this.displaySearch;
    }

    this.retry = function() {
        this.executeAction({ 'action': 'RETRY'}, 'RETRY');
    }

    this.skip = function() {
        this.executeAction({ 'action': 'SKIP'}, 'SKIP');
    }

    this.executeAction = function(requestBody, actionName) {
        const apiUrl = '/services/bpm/bpm-processes/instance/' + this.selectedProcessInstanceId;

        $http({
            method: 'POST',
            url: apiUrl,
            data: requestBody,
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then((response) => {
            messageHub.showAlertSuccess("Action confirmation", actionName + " triggered successfully!")
            console.log('Successfully executed ' + actionName + ' process instance with id [' + $scope.processInstanceId + ']');
            $scope.reload();
        })
        .catch((error) => {
            messageHub.showAlertError("Action failed", actionName + ' operation failed. Error message ' + error.message);
            console.error('Error making POST request:', error);
        });
    }

    this.selectAllChanged = function () {
        for (let instance of this.instancesList) {
            instance.selected = this.selectAll;
        }
    }

    this.selectionChanged = function (instance) {
        this.selectAll = this.instancesList.every(x => x.selected = false);
        messageHub.postMessage('diagram.instance', { instance: instance.id });
        messageHub.postMessage('instance.selected', { instance: instance.id });
        instance.selected = true;
        this.selectedProcessInstanceId = instance.id;

    }

        messageHub.onDidReceiveMessage('definition.selected', function (msg) {
            $scope.$apply(function () {
                if (msg.data.hasOwnProperty('definition')) {
                    $scope.instances.selectedProcessDefinitionKey = msg.data.definition;
                    $scope.instances.applyFilter();
                } else {
                    console.log("Process definition is missing from event!")
                }
            });
        });

    this.getSelectedCount = function () {
        return this.instancesList.reduce((c, instance) => {
            if (instance.selected) c++;
            return c;
        }, 0);
    }

    this.hasSelected = function () {
        return this.instancesList.some(x => x.selected);
    }

    this.applyFilter = function () {
        $http.get('/services/bpm/bpm-processes/instances', { params: { 'id': this.searchText, 'key': this.selectedProcessDefinitionKey, 'limit': 100 } })
            .then((response) => {
                this.instancesList = response.data;
            });
    }

    this.getNoDataMessage = function () {
        return this.searchText ? 'No instances found.' : 'No instances have been detected.';
    }

    this.inputSearchKeyUp = function (e) {
        switch (e.key) {
            case 'Escape':
                this.searchText = '';
                break;
            case 'Enter':
                this.applyFilter();
                break;
        }
    }

    this.onPageChange = function (pageNumber) {
        fetchData({ pageNumber });
    }

    this.onItemsPerPageChange = function (itemsPerPage) {
        fetchData({ pageSize: itemsPerPage });
    }

    this.refresh = function () {
        fetchData();
    }

    this.deleteSelected = function () {
        const selectedIds = this.instancesList.reduce((ret, instance) => {
            if (instance.selected)
                ret.push(instance.id);
            return ret;
        }, []);
    }

}]);