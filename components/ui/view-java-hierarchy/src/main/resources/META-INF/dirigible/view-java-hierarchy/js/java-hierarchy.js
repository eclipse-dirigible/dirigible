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
const app = angular.module('javaHierarchy', ['blimpKit', 'platformView']);
app.constant('Layout', new LayoutHub());
app.controller('JavaHierarchyController', ($scope, $http, Layout) => {

    const BASE = '/services/ide/java-lsp';
    const VIRTUAL_FILE_PREFIX = 'file:///workspace';
    const TYPE_KINDS = new Set([2, 3, 4, 5, 10, 11, 23, 26]); // Module, Namespace, Package, Class, Enum, Interface, Struct, TypeParameter
    const iconFor = (kind) => (TYPE_KINDS.has(kind) ? 'sap-icon--syntax' : 'sap-icon--bullet-text');

    let uidSeq = 0;
    let trigger = null; // { uri, line, character }

    $scope.kind = null;        // 'call' | 'type'
    $scope.direction = null;   // call: incoming|outgoing ; type: supertypes|subtypes
    $scope.workspace = null;
    $scope.rows = [];
    $scope.loading = false;
    $scope.error = '';
    $scope.title = 'Hierarchy';
    $scope.selectedUid = -1;

    const toWorkspacePath = (uri) => {
        if (!uri || !uri.startsWith(VIRTUAL_FILE_PREFIX)) return uri;
        try { return decodeURIComponent(uri.substring(VIRTUAL_FILE_PREFIX.length)); }
        catch (e) { return uri.substring(VIRTUAL_FILE_PREFIX.length); }
    };

    const detailOf = (item) => {
        const path = toWorkspacePath(item.uri || '');
        const file = path ? path.substring(path.lastIndexOf('/') + 1) : '';
        return item.detail ? item.detail + '  •  ' + file : file;
    };

    const makeRow = (item, depth) => ({
        uid: ++uidSeq,
        item,
        name: item.name,
        detail: detailOf(item),
        icon: iconFor(item.kind),
        depth,
        expanded: false,
        loading: false,
        isLeaf: false,
    });

    const prepareMethod = () => ($scope.kind === 'call' ? '/call-hierarchy/prepare' : '/type-hierarchy/prepare');
    const childMethod = () => {
        if ($scope.kind === 'call') return $scope.direction === 'outgoing' ? '/call-hierarchy/outgoing' : '/call-hierarchy/incoming';
        return $scope.direction === 'subtypes' ? '/type-hierarchy/subtypes' : '/type-hierarchy/supertypes';
    };
    // Extracts the hierarchy item from a child result entry (call results wrap it in from/to).
    const childItem = (entry) => ($scope.kind === 'call' ? (entry.from || entry.to) : entry);

    $scope.refresh = () => {
        if (!$scope.kind || !trigger) return;
        $scope.rows = [];
        $scope.error = '';
        $scope.selectedUid = -1;
        $scope.loading = true;
        $http.post(BASE + prepareMethod(), {
            workspace: $scope.workspace,
            uri: trigger.uri,
            line: trigger.line,
            character: trigger.character,
        }).then((response) => {
            $scope.loading = false;
            const roots = Array.isArray(response.data) ? response.data : [];
            if (!roots.length) {
                $scope.error = 'No hierarchy available for the symbol at the cursor';
                return;
            }
            $scope.rows = roots.map((it) => makeRow(it, 0));
            if ($scope.rows.length) $scope.expand($scope.rows[0]);
        }, (response) => {
            $scope.loading = false;
            $scope.error = response.status === 503 ? 'Java language server is still starting — try again in a moment'
                : 'Could not resolve hierarchy';
            if (response.status !== 503) console.error(response);
        });
    };

    $scope.expand = (row) => {
        if (row.loading || row.isLeaf) return;
        row.loading = true;
        $http.post(BASE + childMethod(), { workspace: $scope.workspace, item: row.item }).then((response) => {
            row.loading = false;
            row.expanded = true;
            const entries = Array.isArray(response.data) ? response.data : [];
            const childRows = entries.map((e) => childItem(e)).filter(Boolean).map((it) => makeRow(it, row.depth + 1));
            if (!childRows.length) { row.isLeaf = true; return; }
            const i = $scope.rows.indexOf(row);
            $scope.rows.splice(i + 1, 0, ...childRows);
        }, (response) => {
            row.loading = false;
            console.error(response);
        });
    };

    const collapse = (row) => {
        const i = $scope.rows.indexOf(row);
        let j = i + 1;
        while (j < $scope.rows.length && $scope.rows[j].depth > row.depth) j++;
        $scope.rows.splice(i + 1, j - (i + 1));
        row.expanded = false;
    };

    $scope.toggle = (row, event) => {
        if (event) event.stopPropagation();
        if (row.isLeaf) return;
        if (row.expanded) collapse(row); else $scope.expand(row);
    };

    $scope.select = (row) => $scope.selectedUid = row.uid;

    $scope.open = (row) => {
        const item = row.item;
        const path = toWorkspacePath(item.uri || '');
        if (!path) return;
        const range = item.selectionRange || item.range || { start: { line: 0, character: 0 } };
        Layout.openEditor({ path });
        const data = { filePath: path, line: (range.start.line || 0) + 1, column: (range.start.character || 0) + 1 };
        Layout.postMessage({ topic: 'editor.reveal.line', data });
        setTimeout(() => Layout.postMessage({ topic: 'editor.reveal.line', data }), 1200);
    };

    $scope.setDirection = (dir) => {
        if ($scope.direction === dir) return;
        $scope.direction = dir;
        $scope.refresh();
    };

    // The editor re-posts the show request a few times to beat this lazily-loaded view's startup
    // race; ignore an identical request seen within a short window so the retries don't refetch.
    let lastSig = null;
    let lastSigAt = 0;
    const showListener = Layout.addMessageListener({
        topic: 'java.hierarchy.show',
        handler: (data) => {
            if (!data || !data.kind) return;
            const sig = [data.kind, data.uri, data.line, data.character].join('|');
            const now = Date.now();
            if (sig === lastSig && now - lastSigAt < 3000) return;
            lastSig = sig;
            lastSigAt = now;
            $scope.$evalAsync(() => {
                $scope.kind = data.kind;
                $scope.workspace = data.workspace;
                $scope.direction = data.kind === 'call' ? 'incoming' : 'subtypes';
                $scope.title = data.kind === 'call' ? 'Call Hierarchy' : 'Type Hierarchy';
                trigger = { uri: data.uri, line: data.line, character: data.character };
                $scope.refresh();
            });
        },
    });

    $scope.$on('$destroy', () => {
        Layout.removeMessageListener(showListener);
    });
});
