/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const problemsView = angular.module('problems', ['blimpKit', 'platformView']);
problemsView.controller('ProblemsController', ($scope, $http, $timeout) => {
    const dialogHub = new DialogHub();
    const messageHub = new MessageHubApi();
    const layoutHub = new LayoutHub();
    $scope.models = {
        search: '',
        selectAll: false,
    }
    $scope.filterBy = '';
    $scope.problemsList = [];
    $scope.pageSize = 10;
    $scope.currentPage = 1;

    const fetchData = (args) => {
        const pageNumber = (args && args.pageNumber) || $scope.currentPage;
        const pageSize = (args && args.pageSize) || $scope.pageSize;
        const limit = pageNumber * pageSize;
        const startIndex = (pageNumber - 1) * pageSize;
        if (startIndex >= $scope.totalRows) return;

        $http.get('/services/ide/problems/search', { params: { 'condition': $scope.filterBy, 'limit': limit } })
            .then((response) => {
                const { result, totalRows } = response.data;
                const pageItems = result.slice(startIndex);// to be removed when the pagination is fixed
                for (let problem of $scope.problemsList) {
                    if (problem.selected) {
                        const item = pageItems.find(x => x.id === problem.id);
                        if (item)
                            item.selected = true;
                    }
                }

                $scope.problemsList = pageItems;
                $scope.totalRows = totalRows;
                $scope.selectionChanged();
            });
    }

    fetchData();

    // Surface the Problems view after a publish that introduces new problems. Publishing kicks off
    // server-side synchronization (which compiles client Java); the resulting problems land a moment
    // later, so re-poll a few times and, the first time the total rises above the pre-publish count,
    // bring this view to the front. Reducing or unchanged counts never steal focus.
    const POST_PUBLISH_REFRESH_DELAYS_MS = [1500, 4000, 8000];
    const pendingPublishTimers = [];

    const reloadAndFocusIfWorse = (baseline, alreadyFocused) => {
        const limit = $scope.currentPage * $scope.pageSize;
        const startIndex = ($scope.currentPage - 1) * $scope.pageSize;
        return $http.get('/services/ide/problems/search', { params: { 'condition': $scope.filterBy, 'limit': limit } })
            .then((response) => {
                const { result, totalRows } = response.data;
                $scope.problemsList = result.slice(startIndex);
                $scope.totalRows = totalRows;
                $scope.selectionChanged();
                if (!alreadyFocused.value && totalRows > baseline) {
                    alreadyFocused.value = true;
                    layoutHub.focusView({ id: 'problems', region: 'bottom' });
                }
            });
    };

    const onPublished = messageHub.addMessageListener({
        topic: 'platform.publisher.published',
        handler: () => {
            const baseline = $scope.totalRows || 0;
            const alreadyFocused = { value: false };
            for (const delay of POST_PUBLISH_REFRESH_DELAYS_MS) {
                pendingPublishTimers.push($timeout(() => reloadAndFocusIfWorse(baseline, alreadyFocused), delay));
            }
        }
    });

    $scope.$on('$destroy', () => {
        messageHub.removeMessageListener(onPublished);
        for (const timer of pendingPublishTimers) $timeout.cancel(timer);
    });

    $scope.hasSelected = () => $scope.problemsList.some(x => x.selected);

    $scope.selectAllChanged = () => {
        for (let problem of $scope.problemsList) {
            problem.selected = $scope.models.selectAll;
        }
    };

    $scope.selectionChanged = () => {
        $scope.models.selectAll = $scope.problemsList.every(x => x.selected);
    };

    $scope.clearSearch = () => {
        $scope.models.search = '';
        $scope.filterBy = '';
        fetchData();
    };

    $scope.getSelectedCount = () => $scope.problemsList.reduce((c, problem) => {
        if (problem.selected) c++;
        return c;
    }, 0);

    $scope.applyFilter = () => {
        $scope.filterBy = $scope.models.search;
        fetchData();
    };

    $scope.getNoDataMessage = () => $scope.filterBy ? 'No problems found.' : 'No problems have been detected.';

    $scope.inputSearchKeyUp = (e) => {
        if ($scope.lastSearchKeyUp) {
            $timeout.cancel($scope.lastSearchKeyUp);
            $scope.lastSearchKeyUp = null;
        }

        switch (e.key) {
            case 'Escape':
                $scope.$evalAsync(() => {
                    $scope.models.search = $scope.filterBy || '';
                });
                break;
            case 'Enter':
                $scope.$evalAsync($scope.applyFilter());
                break;
            default:
                if ($scope.filterBy !== $scope.models.search) {
                    $scope.lastSearchKeyUp = $timeout(() => {
                        $scope.$evalAsync(() => {
                            $scope.lastSearchKeyUp = null;
                            $scope.applyFilter();
                        });
                    }, 250);
                }
                break;
        }
    };

    $scope.onPageChange = (pageNumber) => fetchData({ pageNumber });

    $scope.onItemsPerPageChange = (itemsPerPage) => fetchData({ pageSize: itemsPerPage });

    $scope.refresh = () => fetchData();

    $scope.deleteSelected = () => {
        const selectedIds = $scope.problemsList.reduce((ret, problem) => {
            if (problem.selected)
                ret.push(problem.id);
            return ret;
        }, []);

        if (selectedIds.length > 0) {
            $http.post('/services/ide/problems/delete/selected', selectedIds).then(() => {
                fetchData();
            });
        }
    };

    $scope.showInfo = (problem) => {
        dialogHub.showWindow({
            hasHeader: true,
            id: 'problem-details',
            params: { problemDetails: problem },
            maxHeight: '400px',
        });
    };
});
