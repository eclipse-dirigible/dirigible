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
const app = angular.module('javaProblems', ['blimpKit', 'platformView', 'WorkspaceService']);
app.constant('Workspace', new WorkspaceHub());
app.constant('Layout', new LayoutHub());
app.controller('JavaProblemsController', ($scope, $http, $interval, WorkspaceService, Workspace, Layout) => {

    const VIRTUAL_FILE_PREFIX = 'file:///workspace';
    const POLL_INTERVAL_MS = 4000;

    // LSP DiagnosticSeverity: 1 Error, 2 Warning, 3 Information, 4 Hint.
    const SEVERITY = {
        1: { icon: 'sap-icon--error', cssClass: 'jp-error', order: 0 },
        2: { icon: 'sap-icon--alert', cssClass: 'jp-warning', order: 1 },
        3: { icon: 'sap-icon--information', cssClass: 'jp-info', order: 2 },
        4: { icon: 'sap-icon--hint', cssClass: 'jp-hint', order: 3 },
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
    let workspace = WorkspaceService.getCurrentWorkspace();

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
        workspace = WorkspaceService.getCurrentWorkspace();
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
        $scope.filter[sev] = !$scope.filter[sev];
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

    // Poll while mounted — the endpoint is a cheap in-memory read; this catches the workspace build
    // finishing and edits made in files that are not currently open.
    const poll = $interval($scope.refresh, POLL_INTERVAL_MS);

    $scope.refresh();

    $scope.$on('$destroy', () => {
        $interval.cancel(poll);
        Layout.removeMessageListener(focusListener);
        Workspace.removeMessageListener(workspaceChangedListener);
    });
});
