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
angular.module('edmReference', ['ideUI', 'ideView'])
    .controller('ReferenceController', ['$scope', '$http', 'messageHub', 'ViewParameters', function ($scope, $http, messageHub, ViewParameters) {
        let csrfToken;
        $scope.state = {
            isBusy: true,
            error: false,
            busyText: "Loading...",
        };
        $scope.dropdowns = {
            model: '',
            entity: '',
        };
        $scope.forms = {
            newForm: {},
        };
        $scope.availableEntities = [];
        $scope.availableModels = [];
        $scope.loadModels = function () {
            $http({
                method: 'POST',
                url: '/services/ide/workspace-find',
                headers: {
                    'X-CSRF-Token': 'Fetch',
                    'Dirigible-Editor': 'EntityDataModeler',
                },
                data: '*.model',
            }).then(function (response) {
                csrfToken = response.headers("x-csrf-token");
                $scope.availableModels = response.data;
                $scope.state.isBusy = false;
            }, function (response) {
                if (response.data) {
                    if ("error" in response.data) {
                        $scope.state.error = true;
                        $scope.errorMessage = response.data.error.message;
                        console.log(response.data.error);
                        return;
                    }
                }
                $scope.state.error = true;
                $scope.errorMessage = "There was an error while loading the models.";
            });
        };

        $scope.loadEntities = function () {
            $http({
                method: 'GET',
                url: `/services/ide/workspaces${$scope.dropdowns.model}`,
                headers: {
                    'X-CSRF-Token': csrfToken,
                    'Dirigible-Editor': 'EntityDataModeler',
                },
                data: '*.model',
            }).then(function (response) {
                $scope.availableEntities = response.data.model.entities;
                $scope.state.isBusy = false;
            }, function (response) {
                if (response.data) {
                    if ("error" in response.data) {
                        $scope.state.error = true;
                        $scope.errorMessage = response.data.error.message;
                        console.log(response.data.error);
                        return;
                    }
                }
                $scope.state.error = true;
                $scope.errorMessage = "There was an error while loading the entities.";
            });
        };

        $scope.modelSelected = function () {
            $scope.state.isBusy = true;
            $scope.loadEntities();
        };

        $scope.save = function () {
            let referencedEntity;
            for (let i = 0; i < $scope.availableEntities.length; i++) {
                if ($scope.dropdowns.entity === $scope.availableEntities[i].name) {
                    referencedEntity = $scope.availableEntities[i];
                    break;
                }
            }
            if ($scope.dataParameters.dialogType === 'refer')
                messageHub.postMessage('edm.editor.reference', {
                    cellId: $scope.dataParameters.cellId,
                    model: $scope.dropdowns.model,
                    entity: $scope.dropdowns.entity,
                    perspectiveName: referencedEntity.perspectiveName,
                    perspectiveLabel: referencedEntity.perspectiveLabel,
                    perspectiveIcon: referencedEntity.perspectiveIcon,
                    perspectiveOrder: referencedEntity.perspectiveOrder,
                    perspectiveRole: referencedEntity.perspectiveRole,
                    entityProperties: referencedEntity.properties,
                }, true);
            else messageHub.postMessage('edm.editor.copiedEntity', {
                cellId: $scope.dataParameters.cellId,
                model: $scope.dropdowns.model,
                entity: $scope.dropdowns.entity,
                perspectiveName: referencedEntity.perspectiveName,
                perspectiveLabel: referencedEntity.perspectiveLabel,
                perspectiveIcon: referencedEntity.perspectiveIcon,
                perspectiveOrder: referencedEntity.perspectiveOrder,
                perspectiveRole: referencedEntity.perspectiveRole,
                entityProperties: referencedEntity.properties,
            }, true);
        };

        $scope.cancel = function () {
            messageHub.closeDialogWindow("edmReference");
        };
        $scope.dataParameters = ViewParameters.get();
        $scope.loadModels();
    }]);
