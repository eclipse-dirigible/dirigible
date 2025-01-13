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
let ideBpmProcessDefinitionsView = angular.module('ide-bpm-process-definitions', ['ideUI', 'ideView']);

ideBpmProcessDefinitionsView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'bpm';
}]);

ideBpmProcessDefinitionsView.controller('IDEBpmProcessDefinitionsViewController', ['$scope', '$http', '$timeout', 'messageHub', function ($scope, $http, $timeout, messageHub,) {
    this.selectAll = false;
    this.searchText = "";
    this.filterBy = "";
    this.displaySearch = false;
    this.definitionsList = [];
    this.pageSize = 10;
    this.currentPage = 1;

    this.currentFetchDataDefinition = null;

    const fetchData = (args) => {
        if (this.currentFetchDataDefinition) {
            clearInterval(this.currentFetchDataDefinition);
        }

        this.currentFetchDatadDefinition = setInterval(() => {
	        const pageNumber = (args && args.pageNumber) || this.currentPage;
	        const pageSize = (args && args.pageSize) || this.pageSize;
	        const limit = pageNumber * pageSize;
	        const startIndex = (pageNumber - 1) * pageSize;
	        if (startIndex >= this.totalRows) {
	            return;
	        }
	
	        $http.get('/services/bpm/bpm-processes/definitions', { params: { 'condition': this.filterBy, 'limit': limit } })
	            .then((response) => {
	                if (this.definitionsList.length < response.data.length) {
	                    //messageHub.showAlertInfo("User definitions", "A new user task has been added");
	                }
	
	                this.definitionsList = response.data;
	            });
        }, 10000);

    }

    fetchData();

    $scope.reload = function () {
        fetchData();
    };

    this.toggleSearch = function () {
        this.displaySearch = !this.displaySearch;
    }

    this.selectAllChanged = function () {
        for (let definition of this.definitionsList) {
            definition.selected = this.selectAll;
        }
    }

    this.selectionChanged = function (definition) {
        this.selectAll = this.definitionsList.every(x => x.selected = false);
        messageHub.postMessage('diagram.definition', { definition: definition.key });
        messageHub.postMessage('definition.selected', { definition: definition.key });
        definition.selected = true;
    }

    this.clearSearch = function () {
        this.searchText = "";
        this.filterBy = "";
        fetchData();
    }

    this.getSelectedCount = function () {
        return this.definitionsList.reduce((c, definition) => {
            if (definition.selected) c++;
            return c;
        }, 0);
    }

    this.hasSelected = function () {
        return this.definitionsList.some(x => x.selected);
    }

    this.applyFilter = function () {
        this.filterBy = this.searchText;
        fetchData();
    }

    this.getNoDataMessage = function () {
        return this.filterBy ? 'No definitions found.' : 'No definitions have been detected.';
    }

    this.inputSearchKeyUp = function (e) {
        if (this.lastSearchKeyUp) {
            $timeout.cancel(this.lastSearchKeyUp);
            this.lastSearchKeyUp = null;
        }

        switch (e.key) {
            case 'Escape':
                this.searchText = this.filterBy || '';
                break;
            case 'Enter':
                this.applyFilter();
                break;
            default:
                if (this.filterBy !== this.searchText) {
                    this.lastSearchKeyUp = $timeout(() => {
                        this.lastSearchKeyUp = null;
                        this.applyFilter();
                    }, 250);
                }
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
        const selectedIds = this.definitionsList.reduce((ret, definition) => {
            if (definition.selected)
                ret.push(definition.id);
            return ret;
        }, []);
    }

}]);