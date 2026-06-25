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
const problemsView = angular.module('problems', ['blimpKit', 'platformView', 'WorkspaceService']);

// Hosts the two sub-tabs of the Problems view: "Java" (live JDT.LS diagnostics) and
// "Synchronization" (publish-time problems). Tab state is an object so child panes read it through
// prototypal inheritance without the primitive-shadowing pitfall.
problemsView.controller('ProblemsTabsController', ($scope) => {
    $scope.tabs = { selected: 'java' };
    $scope.switchTab = (id) => $scope.tabs.selected = id;
});

// "Java" sub-tab — workspace-wide live diagnostics from JDT.LS.
problemsView.controller('JavaProblemsController', ($scope, $http, $interval, WorkspaceService) => {
    const Workspace = new WorkspaceHub();
    const Layout = new LayoutHub();

    const VIRTUAL_FILE_PREFIX = 'file:///workspace';
    const POLL_INTERVAL_MS = 4000;

    // LSP DiagnosticSeverity: 1 Error, 2 Warning, 3 Information, 4 Hint.
    const SEVERITY = {
        1: { icon: 'sap-icon--error', cssClass: 'jp-error', order: 0 },
        2: { icon: 'sap-icon--alert', cssClass: 'jp-warning', order: 1 },
        3: { icon: 'sap-icon--information', cssClass: 'jp-info', order: 2 },
        // Hint is merged into Information (JDT.LS rarely emits it for Java); render it like Info.
        4: { icon: 'sap-icon--information', cssClass: 'jp-info', order: 3 },
    };
    const severityMeta = (sev) => SEVERITY[sev] || SEVERITY[3];

    $scope.scope = 'all';                       // 'all' | 'current'
    $scope.filter = { 1: true, 2: true, 3: true, 4: true };
    $scope.counts = { 1: 0, 2: 0, 3: 0, 4: 0 };
    $scope.rows = [];
    $scope.loading = false;
    $scope.error = '';
    $scope.selectedUid = -1;

    let allRows = [];                            // every diagnostic, unfiltered
    let activeFilePath = null;                   // /<ws>/<proj>/File.java of the focused editor
    let uidSeq = 0;

    const toWorkspacePath = (uri) => {
        if (!uri || !uri.startsWith(VIRTUAL_FILE_PREFIX)) return uri;
        try { return decodeURIComponent(uri.substring(VIRTUAL_FILE_PREFIX.length)); }
        catch (e) { return uri.substring(VIRTUAL_FILE_PREFIX.length); }
    };

    const applyView = () => {
        const counts = { 1: 0, 2: 0, 3: 0, 4: 0 };
        const inScope = allRows.filter((r) => $scope.scope === 'all' || r.path === activeFilePath);
        for (const r of inScope) counts[r.severity] = (counts[r.severity] || 0) + 1;
        $scope.counts = counts;
        $scope.rows = inScope.filter((r) => $scope.filter[r.severity])
                             .sort((a, b) => a.order - b.order || a.path.localeCompare(b.path) || a.line - b.line);
    };

    $scope.refresh = () => {
        const workspace = WorkspaceService.getCurrentWorkspace();
        $scope.loading = true;
        $http.post('/services/ide/java-lsp/diagnostics', { workspace }).then((response) => {
            $scope.loading = false;
            $scope.error = '';
            const files = Array.isArray(response.data) ? response.data : [];
            const rows = [];
            for (const file of files) {
                const path = toWorkspacePath(file.uri || '');
                const fileName = path ? path.substring(path.lastIndexOf('/') + 1) : '';
                for (const d of (file.diagnostics || [])) {
                    const start = (d.range && d.range.start) || { line: 0, character: 0 };
                    const meta = severityMeta(d.severity);
                    rows.push({
                        uid: ++uidSeq,
                        severity: d.severity || 3,
                        order: meta.order,
                        icon: meta.icon,
                        cssClass: meta.cssClass,
                        message: d.message,
                        path,
                        file: fileName,
                        line: (start.line || 0) + 1,
                        column: (start.character || 0) + 1,
                    });
                }
            }
            allRows = rows;
            applyView();
        }, (response) => {
            $scope.loading = false;
            if (response.status === 503) {
                $scope.error = 'Java language server is still starting — try again in a moment';
            } else {
                console.error(response);
                $scope.error = 'Could not load Java problems';
            }
        });
    };

    $scope.setScope = (scope) => {
        if ($scope.scope === scope) return;
        $scope.scope = scope;
        applyView();
    };

    $scope.toggleSeverity = (sev) => {
        // The single Info chip controls both Information (3) and Hint (4).
        if (sev === 3) {
            const next = !$scope.filter[3];
            $scope.filter[3] = next;
            $scope.filter[4] = next;
        } else {
            $scope.filter[sev] = !$scope.filter[sev];
        }
        applyView();
    };

    $scope.select = (row) => $scope.selectedUid = row.uid;

    $scope.open = (row) => {
        if (!row.path) return;
        Layout.openEditor({ path: row.path });
        const data = { filePath: row.path, line: row.line, column: row.column };
        Layout.postMessage({ topic: 'editor.reveal.line', data });
        setTimeout(() => Layout.postMessage({ topic: 'editor.reveal.line', data }), 1200);
    };

    // Track the focused editor so the "Current file" scope can filter to it.
    const focusListener = Layout.onFocusEditor((data) => {
        if (data && data.path) {
            activeFilePath = data.path;
            if ($scope.scope === 'current') $scope.$evalAsync(applyView);
        }
    });

    const reload = () => $scope.$evalAsync($scope.refresh);
    const workspaceChangedListener = Workspace.onWorkspaceChanged(reload);

    // Poll only while the Java sub-tab is selected (the endpoint is a cheap in-memory read, but a poll
    // also starts JDT.LS — no need to do that while the user is on the Synchronization tab). The watch
    // fires immediately with the initial selection.
    let poll = null;
    const startPolling = () => {
        if (poll) return;
        $scope.refresh();
        poll = $interval($scope.refresh, POLL_INTERVAL_MS);
    };
    const stopPolling = () => {
        if (poll) { $interval.cancel(poll); poll = null; }
    };
    $scope.$watch(() => $scope.tabs && $scope.tabs.selected, (selected) => {
        if (selected === 'java') startPolling(); else stopPolling();
    });

    $scope.$on('$destroy', () => {
        stopPolling();
        Layout.removeMessageListener(focusListener);
        Workspace.removeMessageListener(workspaceChangedListener);
    });
});

// "Synchronization" sub-tab — publish-time problems persisted in DIRIGIBLE_PROBLEMS.
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

    // Surface the Problems view after an action that introduces new problems. Publishing / generating
    // kicks off server-side synchronization (which compiles client Java) - the resulting problems land
    // a moment later (the first compile is slow: classpath extraction + javac), so re-poll a few times
    // and, the first time the total rises above the pre-action count, bring this view to the front.
    // Reducing or unchanged counts never steal focus. Triggered by both the manual Publish action
    // (platform.publisher.published) and generation (projects.tree.refresh - the intent editor's
    // Generate publishes server-side, which does not raise the client publish topic).
    const REFRESH_DELAYS_MS = [1500, 4000, 8000, 15000];
    const pendingTimers = [];

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

    const scheduleRefreshAndFocus = () => {
        for (const timer of pendingTimers) $timeout.cancel(timer);
        pendingTimers.length = 0;
        const baseline = $scope.totalRows || 0;
        const alreadyFocused = { value: false };
        for (const delay of REFRESH_DELAYS_MS) {
            pendingTimers.push($timeout(() => reloadAndFocusIfWorse(baseline, alreadyFocused), delay));
        }
    };

    const onPublished = messageHub.addMessageListener({ topic: 'platform.publisher.published', handler: scheduleRefreshAndFocus });
    const onGenerated = messageHub.addMessageListener({ topic: 'projects.tree.refresh', handler: scheduleRefreshAndFocus });

    $scope.$on('$destroy', () => {
        messageHub.removeMessageListener(onPublished);
        messageHub.removeMessageListener(onGenerated);
        for (const timer of pendingTimers) $timeout.cancel(timer);
    });

    // Open a problem's file in the editor and place the cursor at its line/column (double-click a row).
    const currentWorkspace = () => {
        try {
            return localStorage.getItem(`${getBrandingInfo().prefix}.workspace.selected`) || 'workspace';
        } catch (e) {
            return 'workspace';
        }
    };

    $scope.openProblem = (problem) => {
        if (!problem || !problem.location) return;
        const location = problem.location.startsWith('/') ? problem.location : `/${problem.location}`;
        const path = `/${currentWorkspace()}${location}`;
        layoutHub.openEditor({ path: path });
        const line = parseInt(problem.line, 10);
        if (line > 0) {
            const column = parseInt(problem.column, 10) || 1;
            // Re-post after a beat so a just-opened editor (still loading) also receives it.
            const reveal = () => messageHub.postMessage({ topic: 'editor.reveal.line', data: { filePath: path, line: line, column: column } });
            $timeout(reveal, 500);
            $timeout(reveal, 1500);
        }
    };

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
