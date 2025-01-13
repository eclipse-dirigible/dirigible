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
const artefactsView = angular.module('artefacts', ['ideUI', 'ideView']);

artefactsView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'artefacts-view';
}]);

artefactsView.controller('ArtefactsController', ['$scope', '$http', function ($scope, $http) {

    $http.get('/services/js/ide-operations/services/artefacts/artefacts.mjs').then(function (response) {
        $scope.artefacts = response.data;
    });

}]);