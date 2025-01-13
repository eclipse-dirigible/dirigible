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
const searchView = angular.module('search', ['ideUI', 'ideView', 'ideWorkspace', 'ideEditors']);

searchView.controller('SearchController', ['$scope', 'messageHub', 'workspaceApi', 'Editors', function ($scope, messageHub, workspaceApi, Editors) {
    $scope.selectedWorkspace = { name: 'workspace' }; // Default
    $scope.workspaceNames = [];
    $scope.search = { searching: false, text: '', results: [] };
    $scope.searchResults = []
    $scope.searchAreaIcon = 'sap-icon--search';
    $scope.searchAreaTitle = 'Advanced search';
    $scope.searchAreaSubtitle = 'Search in file content';
    $scope.selectedItemIndex = -1;

    $scope.reloadWorkspaceList = function () {
        let userSelected = JSON.parse(localStorage.getItem('DIRIGIBLE.workspace') || '{}');
        if (!userSelected.name) {
            $scope.selectedWorkspace.name = 'workspace'; // Default
        } else {
            $scope.selectedWorkspace.name = userSelected.name;
        }
        workspaceApi.listWorkspaceNames().then(function (response) {
            if (response.status === 200)
                $scope.workspaceNames = response.data;
            else messageHub.setStatusError('Unable to load workspace list');
        });
    };

    $scope.switchWorkspace = function (workspace) {
        if ($scope.selectedWorkspace.name !== workspace) {
            $scope.selectedWorkspace.name = workspace;
            $scope.refresh();
        }
    };

    $scope.isSelectedWorkspace = function (name) {
        if ($scope.selectedWorkspace.name === name) return true;
        return false;
    };

    $scope.itemClick = function (index) {
        $scope.selectedItemIndex = index;
    };

    $scope.openFile = function (index, editor = undefined) {
        messageHub.openEditor(
            $scope.search.results[index].path,
            $scope.search.results[index].name,
            $scope.search.results[index].contentType,
            editor,
        );
    };

    $scope.clearSearch = function () {
        $scope.search.text = '';
        $scope.search.results.length = 0;
        $scope.search.searching = false;
    };

    $scope.refresh = function () {
        $scope.search.results.length = 0;
        $scope.search.searching = true;
        if ($scope.search.text) {
            workspaceApi.search($scope.selectedWorkspace.name, '', $scope.search.text).then(function (response) {
                $scope.search.searching = false;
                if (response.status === 200) {
                    for (let i = 0; i < response.data.length; i++) {
                        $scope.search.results.push(response.data[i]);
                    }
                } else {
                    messageHub.setStatusError('There was an error while performing a search');
                }
            });
        } else $scope.search.searching = false;
    };

    let to = 0;
    $scope.searchContent = function () {
        if (to) { clearTimeout(to); }
        to = setTimeout(function () {
            $scope.refresh();
            $scope.$digest();
        }, 250);
    };

    function getEditorsForType(index, contentType) {
        let editors = [{
            id: 'openWith',
            label: Editors.defaultEditor.label,
            data: {
                fileIndex: index,
                editorId: Editors.defaultEditor.id,
            }
        }];
        let editorsForContentType = Editors.editorsForContentType;
        if (Object.keys(editorsForContentType).indexOf(contentType) > -1) {
            for (let i = 0; i < editorsForContentType[contentType].length; i++) {
                if (editorsForContentType[contentType][i].id !== Editors.defaultEditor.id)
                    editors.push({
                        id: 'openWith',
                        label: editorsForContentType[contentType][i].label,
                        data: {
                            fileIndex: index,
                            editorId: editorsForContentType[contentType][i].id,
                        }
                    });
            }
        }
        return editors;
    }

    $scope.contextMenuContext = function (element) {
        let id;
        if (element.tagName !== "LI") {
            let closest = element.closest("li");
            if (closest) id = closest.id;
            else return {
                callbackTopic: "search.list.contextmenu",
                hasIcons: true,
                items: [
                    {
                        id: "refresh",
                        label: "Refresh",
                        icon: "sap-icon--refresh",
                        isDisabled: ($scope.search.text ? false : true),
                    },
                ]
            };
        } else {
            id = element.id;
        }
        if (id) {
            return {
                callbackTopic: "search.list.contextmenu",
                hasIcons: true,
                items: [
                    {
                        id: "open",
                        label: "Open",
                        icon: "sap-icon--action",
                        data: id,
                    },
                    {
                        id: "openWith",
                        label: "Open With",
                        icon: "sap-icon--action",
                        items: getEditorsForType(id, $scope.search.results[id].contentType)
                    },
                ]
            };
        }
    };

    messageHub.onDidReceiveMessage(
        'search.list.contextmenu',
        function (msg) {
            if (msg.data.itemId === 'refresh') {
                $scope.refresh();
            } else if (msg.data.itemId === 'open') {
                $scope.openFile(msg.data.data);
            } else if (msg.data.itemId === 'openWith') {
                $scope.openFile(msg.data.data.fileIndex, msg.data.data.editorId);
            }
        },
        true
    );

    messageHub.onWorkspacesModified(function () {
        $scope.reloadWorkspaceList();
    });

    // Initialization
    $scope.reloadWorkspaceList();
}]);
