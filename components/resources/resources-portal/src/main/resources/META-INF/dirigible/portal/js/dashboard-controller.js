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
const dashboard = angular.module('dashboard', ['ideUI', 'ideView']);

dashboard.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'app';
}]);

dashboard.controller('DashboardController', ['$scope', '$http', 'messageHub', function ($scope, $http, messageHub) {
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: "Loading...",
    };

    $scope.isWidgetContainerUsed = [false, false, false];

    $http.get("/services/js/portal/api/WidgetsExtension/WidgetService.js")
        .then(function (response) {
            $scope.widgetList = response.data;

            $scope.widgetList.forEach(e => createWidget(e));
            $scope.state.isBusy = false;
        })
        .catch(function (error) {
            console.error('Error fetching widget list:', error);
            $scope.state.error = true;
            $scope.errorMessage = 'Failed to load widget list';
        });

    function createWidget(widgetData) {
        if (!widgetData || !widgetData.id || !widgetData.link || !widgetData.size
            || !["small", "medium", "large"].includes(widgetData.size)) {
            console.error('Invalid widget data:', widgetData);
            return;
        }

        const iframe = document.createElement('iframe');

        if (widgetData.redirectViewId)
            iframe.style.pointerEvents = "none";

        iframe.src = widgetData.link;
        iframe.title = widgetData.label;
        iframe.className = 'tile-auto-layout';
        iframe.style.border = 'none';
        iframe.style.overflow = 'hidden';
        iframe.style.display = 'block';

        // @ts-ignore
        iframe.loading = "lazy";

        const widgetContainer = document.createElement('div');
        if (widgetData.size == "small") {
            $scope.isWidgetContainerUsed[0] = true;
            widgetContainer.className = 'fd-col fd-col--6 fd-col-md--3 fd-col-lg--3 fd-col-xl--3';
            iframe.style.width = '100%';
            iframe.style.height = '160px';
        } else if (widgetData.size == "medium") {
            $scope.isWidgetContainerUsed[1] = true;
            widgetContainer.className = 'fd-col fd-col--12 fd-col-md--6 fd-col-lg--6 fd-col-xl--6';
            iframe.style.width = '100%';
            iframe.style.height = '200px';
        } else {
            $scope.isWidgetContainerUsed[2] = true;
            widgetContainer.className = 'fd-col fd-col--12 fd-col-md--6 fd-col-lg--6 fd-col-xl--6';
            iframe.style.width = '100%';
            iframe.style.height = '340px';
        }

        widgetContainer.style.margin = '0.5rem 0'
        widgetContainer.appendChild(iframe);

        const widgetPanel = document.getElementById(`${widgetData.size}-widget-container`);;
        if (widgetPanel) {
            widgetPanel.appendChild(widgetContainer);
        } else {
            console.error('Widget container not found');
        }
    }
}]);
