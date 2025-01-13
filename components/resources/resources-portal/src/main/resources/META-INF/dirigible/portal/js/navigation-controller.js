/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const navigation = angular.module("launchpad", ["ngResource", "ideLayout", "ideUI"]);
navigation.controller("LaunchpadViewController", ["$scope", "messageHub", "$http", function ($scope, messageHub, $http) {
    $scope.currentViewId = 'dashboard';

    $scope.extraExtensionPoints = ['app', "dashboard-navigations", "dashboard-widgets"];
    $scope.groups = [];
    $scope.groupItems = [];

    function loadNavigationGroups() {
        return $http.get("/services/js/portal/api/NavigationGroupsExtension/NavigationGroupsService.js")
            .then(function (response) {
                for (itemData of response.data) {
                    if (!itemData || !itemData.icon || !itemData.order || !itemData.label) {
                        console.error(`Invalid navigation group data: ${JSON.stringify(itemData)}. Missing one of the properties: icon, order, label`);
                        return;
                    }
                }

                $scope.groups = response.data;

                $scope.groups.sort((a, b) => a.order - b.order)

                response.data.forEach(elem => {
                    $scope.groupItems[elem.label.toLowerCase()] = [];
                });
            })
            .catch(function (error) {
                console.error('Error fetching navigation groups:', error);
                $scope.state = { error: true, errorMessage: 'Failed to load navigation groups' };
                return async () => { };
            });
    }

    function loadNavigationItems() {
        return $http.get("/services/js/portal/api/NavigationExtension/NavigationService.js")
            .then(function (response) {
                $scope.navigationList = response.data;

                $scope.navigationList.forEach(e => addNavigationItem(e));

                Object.values($scope.groupItems).forEach(items => {
                    items.sort((a, b) => a.order - b.order);
                });
            })
            .catch(function (error) {
                console.error('Error fetching navigation items:', error);
                $scope.state = { error: true, errorMessage: 'Failed to load navigation items' };
            });
    }

    function addNavigationItem(itemData) {
        if (!itemData || !itemData.label || !itemData.group || !itemData.order || !itemData.link) {
            console.error(`Invalid item data: ${JSON.stringify(itemData)} Missing one of the properties: label, group, order, link`);
            return;
        }

        const groupKey = itemData.group.toLowerCase();
        if (!$scope.groupItems[groupKey]) {
            console.error('Group key not found:', groupKey);
            return;
        }

        $scope.groupItems[groupKey].push({
            id: itemData.id,
            label: itemData.label,
            link: itemData.link,
            order: itemData.order
        });
    }

    loadNavigationGroups()
        .then(loadNavigationItems)
        .catch(function (error) {
            console.error('Error during initialization:', error);
        });

    $scope.switchView = function (id, event) {
        if (event) event.stopPropagation();
        $scope.currentViewId = id;
    };

    $scope.isGroupVisible = function (group) {
        const items = $scope.groupItems[group.label.toLowerCase()];
        return items.some(function (item) {
            return $scope.currentViewId === item.id;
        });
    };

    messageHub.onDidReceiveMessage('launchpad.switch.perspective', function (msg) {
        $scope.$apply(function () {
            $scope.switchView(msg.data.viewId);
        });
    }, true)
}]);