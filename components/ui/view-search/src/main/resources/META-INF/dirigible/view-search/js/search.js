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
const searchView = angular.module('search', ['blimpKit', 'platformView', 'WorkspaceService', 'platformEditors']);
searchView.constant('StatusBar', new StatusBarHub());
searchView.constant('Workspace', new WorkspaceHub());
searchView.constant('Layout', new LayoutHub());
searchView.constant('ContextMenu', new ContextMenuHub());
searchView.controller('SearchController', ($scope, WorkspaceService, StatusBar, ContextMenu, Workspace, Layout, Editors) => {
    $scope.selectedWorkspace = WorkspaceService.getCurrentWorkspace();
    $scope.workspaceNames = [];
    $scope.search = { searching: false, text: '', results: [] };
    $scope.searchResults = []
    $scope.searchAreaIcon = 'sap-icon--search';
    $scope.searchAreaTitle = 'Advanced search';
    $scope.searchAreaSubtitle = 'Search in file content';
    $scope.selectedItemIndex = -1;

    $scope.reloadWorkspaceList = () => {
        WorkspaceService.listWorkspaceNames().then((response) => {
            $scope.workspaceNames = response.data;
            if (!$scope.workspaceNames.includes($scope.selectedWorkspace))
                $scope.selectedWorkspace = 'workspace'; // Default
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
            $scope.refresh();
        }
    };

    $scope.isSelectedWorkspace = (name) => $scope.selectedWorkspace === name;

    $scope.itemClick = (index) => $scope.selectedItemIndex = index;

    $scope.openFile = (index, editor = undefined) => Layout.openEditor({
        path: $scope.search.results[index].path,
        contentType: $scope.search.results[index].contentType,
        editorId: editor,
    });

    $scope.clearSearch = () => {
        $scope.search.text = '';
        $scope.search.results.length = 0;
        $scope.search.searching = false;
    };

    $scope.refresh = () => {
        $scope.search.results.length = 0;
        $scope.search.searching = true;
        if ($scope.search.text) {
            WorkspaceService.search($scope.selectedWorkspace, $scope.search.text).then((response) => {
                $scope.$evalAsync(() => {
                    $scope.search.searching = false;
                    for (let i = 0; i < response.data.length; i++) {
                        $scope.search.results.push(response.data[i]);
                    }
                });
            }, (response) => {
                console.error(response);
                $scope.$evalAsync(() => { $scope.search.searching = false; });
                StatusBar.setError('There was an error while performing a search');
            });
        } else $scope.search.searching = false;
    };

    let to = 0;
    $scope.searchContent = () => {
        if (to) { clearTimeout(to); }
        to = setTimeout(() => {
            $scope.$evalAsync(() => {
                $scope.refresh();
            });
        }, 250);
    };

    function getEditorsForType(contentType) {
        const editors = [{
            id: `openWith-${Editors.defaultEditor.id}`,
            label: Editors.defaultEditor.label,
        }];
        const editorsForContentType = Editors.editorsForContentType;
        if (Object.keys(editorsForContentType).indexOf(contentType) > -1) {
            for (let i = 0; i < editorsForContentType[contentType].length; i++) {
                if (editorsForContentType[contentType][i].id !== Editors.defaultEditor.id)
                    editors.push({
                        id: `openWith-${editorsForContentType[contentType][i].id}`,
                        label: editorsForContentType[contentType][i].label,
                    });
            }
        }
        return editors;
    }

    let selectedFileId;

    $scope.showContextMenu = (event) => {
        event.preventDefault();
        let id;
        const items = [];
        if (event.target.tagName !== 'LI') {
            let closest = event.target.closest('li');
            if (closest) id = closest.id;
            else items.push(
                {
                    id: 'refresh',
                    label: 'Refresh',
                    leftIconClass: 'sap-icon--refresh',
                    disabled: ($scope.search.text ? false : true),
                }
            );
        } else {
            id = event.target.id;
        }
        if (id) {
            selectedFileId = id;
            items.push(
                {
                    id: 'open',
                    label: 'Open',
                    icon: 'sap-icon--action',
                },
                {
                    id: 'openWith',
                    label: 'Open With',
                    icon: 'sap-icon--action',
                    items: getEditorsForType($scope.search.results[id].contentType)
                },
            );
        }
        ContextMenu.showContextMenu({
            ariaLabel: 'search contextmenu',
            posX: event.clientX,
            posY: event.clientY,
            icons: true,
            items: items
        }).then((id) => {
            if (id) {
                if (id === 'refresh') {
                    $scope.refresh();
                } else if (id === 'open') {
                    $scope.openFile(selectedFileId);
                } else if (id.startsWith('openWith')) {
                    const editorId = id.slice(9);
                    $scope.openFile(selectedFileId, editorId);
                }
            }
        });
    };

    // Initialization
    $scope.reloadWorkspaceList();

    $scope.$on('$destroy', () => {
        Workspace.removeMessageListener(workspaceCreatedListener);
        Workspace.removeMessageListener(workspaceDeletedListener);
    });
});
