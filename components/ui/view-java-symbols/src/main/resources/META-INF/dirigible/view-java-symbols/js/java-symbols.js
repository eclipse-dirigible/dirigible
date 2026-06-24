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
const app = angular.module('javaSymbols', ['blimpKit', 'platformView', 'WorkspaceService']);
app.constant('StatusBar', new StatusBarHub());
app.constant('Workspace', new WorkspaceHub());
app.constant('Layout', new LayoutHub());
app.controller('JavaSymbolsController', ($scope, $http, WorkspaceService, StatusBar, Workspace, Layout) => {

    // LSP SymbolKind → icon. Types get one glyph, members another; anything else falls back to a type.
    const TYPE_KINDS = new Set([2, 3, 4, 5, 10, 11, 23, 26]); // Module, Namespace, Package, Class, Enum, Interface, Struct, TypeParameter
    const symbolIcon = (kind) => (TYPE_KINDS.has(kind) ? 'sap-icon--syntax' : 'sap-icon--bullet-text');

    $scope.selectedWorkspace = WorkspaceService.getCurrentWorkspace();
    $scope.workspaceNames = [];
    $scope.query = '';
    $scope.results = [];
    $scope.searching = false;
    $scope.error = '';
    $scope.selectedIndex = -1;

    $scope.reloadWorkspaceList = () => {
        WorkspaceService.listWorkspaceNames().then((response) => {
            $scope.workspaceNames = response.data;
            if (!$scope.workspaceNames.includes($scope.selectedWorkspace))
                $scope.selectedWorkspace = 'workspace';
        }, (response) => {
            console.error(response);
            StatusBar.showError('Unable to load workspace list');
        });
    };

    const workspaceCreatedListener = Workspace.onWorkspaceCreated($scope.reloadWorkspaceList);
    const workspaceDeletedListener = Workspace.onWorkspaceDeleted($scope.reloadWorkspaceList);

    $scope.switchWorkspace = (workspace) => {
        if ($scope.selectedWorkspace !== workspace) {
            $scope.selectedWorkspace = workspace;
            $scope.search();
        }
    };

    $scope.isSelectedWorkspace = (name) => $scope.selectedWorkspace === name;

    $scope.select = (index) => $scope.selectedIndex = index;

    $scope.clear = () => {
        $scope.query = '';
        $scope.results = [];
        $scope.error = '';
        $scope.selectedIndex = -1;
    };

    // Strips the virtual file prefix off a symbol's URI to yield the IDE workspace path, which keeps
    // the workspace segment: file:///workspace/<ws>/proj/Foo.java -> /<ws>/proj/Foo.java
    const VIRTUAL_FILE_PREFIX = 'file:///workspace';
    const toWorkspacePath = (uri) => {
        if (!uri || !uri.startsWith(VIRTUAL_FILE_PREFIX)) return uri;
        try { return decodeURIComponent(uri.substring(VIRTUAL_FILE_PREFIX.length)); }
        catch (e) { return uri.substring(VIRTUAL_FILE_PREFIX.length); }
    };

    $scope.open = (index) => {
        const item = $scope.results[index];
        if (!item) return;
        Layout.openEditor({ path: item.path });
        if (item.line > 0) {
            const data = { filePath: item.path, line: item.line, column: item.column };
            Layout.postMessage({ topic: 'editor.reveal.line', data });
            setTimeout(() => Layout.postMessage({ topic: 'editor.reveal.line', data }), 1200);
        }
    };

    $scope.search = () => {
        const query = ($scope.query || '').trim();
        $scope.results = [];
        $scope.error = '';
        $scope.selectedIndex = -1;
        if (!query) { $scope.searching = false; return; }
        $scope.searching = true;
        $http.get('/services/ide/java-lsp/symbol', { params: { workspace: $scope.selectedWorkspace, query } })
            .then((response) => {
                const symbols = Array.isArray(response.data) ? response.data : [];
                $scope.results = symbols.map((s) => {
                    const loc = s.location || {};
                    const start = (loc.range && loc.range.start) || { line: 0, character: 0 };
                    const path = toWorkspacePath(loc.uri || '');
                    return {
                        name: s.name,
                        detail: s.containerName ? s.containerName + '  •  ' + path : path,
                        icon: symbolIcon(s.kind),
                        path,
                        line: (start.line || 0) + 1,
                        column: (start.character || 0) + 1,
                    };
                });
                $scope.searching = false;
            }, (response) => {
                $scope.searching = false;
                if (response.status === 503) {
                    $scope.error = 'Java language server is still starting — try again in a moment';
                } else {
                    console.error(response);
                    $scope.error = 'Symbol search failed';
                }
            });
    };

    let debounce = 0;
    $scope.onType = () => {
        if (debounce) clearTimeout(debounce);
        debounce = setTimeout(() => $scope.$evalAsync($scope.search), 250);
    };

    // The editor's "Go to Symbol in Workspace" action focuses this view and may seed the query.
    const searchRequestListener = Layout.addMessageListener({
        topic: 'java.symbols.search',
        handler: (data) => {
            $scope.$evalAsync(() => {
                if (data && typeof data.query === 'string') {
                    $scope.query = data.query;
                    $scope.search();
                }
            });
        },
    });

    $scope.reloadWorkspaceList();

    $scope.$on('$destroy', () => {
        Workspace.removeMessageListener(workspaceCreatedListener);
        Workspace.removeMessageListener(workspaceDeletedListener);
        Layout.removeMessageListener(searchRequestListener);
    });
});
