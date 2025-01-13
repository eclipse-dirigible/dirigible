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
const workbench = angular.module('workbench', ['platformView', 'platformLayout', 'blimpKit']);
workbench.controller('WorkbenchController', ($scope, Layout) => {
    const contextMenuHub = new ContextMenuHub();
    let rightClickTabId;

    $scope.layoutConfig = {
        views: ['welcome', 'projects', 'import', 'search', 'properties', 'console', 'terminal', 'preview', 'problems'],
        viewSettings: {},
        layoutSettings: {
            hideCenterPane: false,
            leftPaneMinSize: 355
        },
    };

    $scope.showContextMenu = (event) => {
        event.preventDefault();
        if (event.target.tagName !== 'LI') {
            let closest = event.target.closest('li');
            if (closest && closest.hasAttribute('tab-id') && closest.hasAttribute('data-file-path')) {
                rightClickTabId = closest.getAttribute('data-file-path');
            } else return;
        } else {
            if (event.target.hasAttribute('tab-id') && event.target.hasAttribute('data-file-path')) {
                rightClickTabId = event.target.getAttribute('data-file-path');
            } else return;
        }
        contextMenuHub.showContextMenu({
            ariaLabel: 'editor tab contextmenu',
            posX: event.clientX,
            posY: event.clientY,
            icons: false,
            items: [
                {
                    id: 'close',
                    label: 'Close',
                },
                {
                    id: 'closeOthers',
                    label: 'Close Others',
                },
                {
                    id: 'closeAll',
                    label: 'Close All',
                    separator: true,
                },
                {
                    id: 'reveal',
                    label: 'Reveal in Projects',
                }
            ]
        }).then((id) => {
            if (id === 'reveal') {
                contextMenuHub.postMessage({ topic: 'projects.tree.select', data: { filePath: rightClickTabId } });
            } else if (id === 'close') {
                Layout.closeEditor({
                    path: rightClickTabId,
                });
            } else if (id === 'closeOthers') {
                Layout.closeEditor({
                    path: rightClickTabId,
                    params: { closeOthers: true }
                });
            } else if (id === 'closeAll') {
                Layout.closeAllEditors();
            }
        });
    };
});