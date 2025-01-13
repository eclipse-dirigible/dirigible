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
const problemsView = angular.module('problems', ['ideUI', 'ideView']);

problemsView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'problems-view';
}]);

problemsView.controller('ProblemsController', ['$http', '$timeout', 'messageHub', function ($http, $timeout, messageHub) {
    this.selectAll = false;
    this.searchText = "";
    this.filterBy = "";
    this.displaySearch = false;
    this.problemsList = [];
    this.pageSize = 10;
    this.currentPage = 1;

    const fetchData = (args) => {
        const pageNumber = (args && args.pageNumber) || this.currentPage;
        const pageSize = (args && args.pageSize) || this.pageSize;
        const limit = pageNumber * pageSize;
        const startIndex = (pageNumber - 1) * pageSize;
        if (startIndex >= this.totalRows) {
            return;
        }

        $http.get('/services/ide/problems/search', { params: { 'condition': this.filterBy, 'limit': limit } })
            .then((response) => {
                const { result, totalRows } = response.data;
                const pageItems = result.slice(startIndex);// to be removed when the pagination is fixed
                for (let problem of this.problemsList) {
                    if (problem.selected) {
                        const item = pageItems.find(x => x.id === problem.id);
                        if (item)
                            item.selected = true;
                    }
                }

                this.problemsList = pageItems;
                this.totalRows = totalRows;

                this.selectionChanged();
            });
    }

    fetchData();

    this.toggleSearch = function () {
        this.displaySearch = !this.displaySearch;
    }

    this.selectAllChanged = function () {
        for (let problem of this.problemsList) {
            problem.selected = this.selectAll;
        }
    }

    this.selectionChanged = function () {
        this.selectAll = this.problemsList.every(x => x.selected);
    }

    this.clearSearch = function () {
        this.searchText = "";
        this.filterBy = "";
        fetchData();
    }

    this.getSelectedCount = function () {
        return this.problemsList.reduce((c, problem) => {
            if (problem.selected) c++;
            return c;
        }, 0);
    }

    this.hasSelected = function () {
        return this.problemsList.some(x => x.selected);
    }

    this.applyFilter = function () {
        this.filterBy = this.searchText;
        fetchData();
    }

    this.getNoDataMessage = function () {
        return this.filterBy ? 'No problems found.' : 'No problems have been detected.';
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
        const selectedIds = this.problemsList.reduce((ret, problem) => {
            if (problem.selected)
                ret.push(problem.id);
            return ret;
        }, []);

        if (selectedIds.length > 0) {
            $http.post('/services/ide/problems/delete/selected', selectedIds).then(() => {
                fetchData();
            });
        }
    }

    // this.openFile = function (fullPath) {
    //     const fullName = fullPath.split("/").pop();
    //     const [name, extension] = fullName.split('.');
    //     messageHub.openEditor("/workspace" + fullPath, fullName, extension);
    // }

    this.showInfo = function (problem) {
        messageHub.showDialogWindow(
            "problem-details",
            { problemDetails: problem }
        );
    }
}]);
