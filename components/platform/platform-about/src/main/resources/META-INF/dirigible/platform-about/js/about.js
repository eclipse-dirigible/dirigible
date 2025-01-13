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
angular.module('about', ['blimpKit', 'platformView'])
    .controller('AboutController', ($scope, $http) => {
        $scope.blimpKitVersion = angular.module('blimpKit').info().version;
        $scope.jobs = [];

        function getHealthStatus() {
            $http({
                method: 'GET',
                url: '/services/core/healthcheck'
            }).then((healthStatus) => {
                $scope.jobs.length = 0;
                for (const [key, value] of Object.entries(healthStatus.data.jobs.statuses)) {
                    $scope.jobs.push({
                        name: key,
                        status: value,
                    });
                }
            }, (e) => {
                console.error('Error retreiving the health status', e);
            });
        };

        setInterval(() => {
            getHealthStatus();
        }, 10000);

        $http.get('/services/core/version').then((response) => {
            $scope.version = response.data;
        });

        getHealthStatus();
    });