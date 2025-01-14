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
angular.module('edmReference', ['blimpKit', 'platformView'])
    .constant('Dialogs', new DialogHub())
    .controller('ReferenceController', ($scope, $http, Dialogs, ViewParameters) => {
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
        $scope.loadModels = () => {
            $http({
                method: 'POST',
                url: '/services/ide/workspace-find',
                headers: {
                    'Dirigible-Editor': 'EntityDataModeler',
                },
                data: '*.model',
            }).then((response) => {
                $scope.availableModels = response.data;
                $scope.state.isBusy = false;
            }, (response) => {
                if (response.data) {
                    if ("error" in response.data) {
                        $scope.state.error = true;
                        $scope.errorMessage = response.data.error.message;
                        console.error(response.data.error);
                        return;
                    }
                }
                $scope.state.error = true;
                $scope.errorMessage = "There was an error while loading the models.";
            });
        };

        $scope.loadEntities = () => {
            $http({
                method: 'GET',
                url: `/services/ide/workspaces${$scope.dropdowns.model}`,
                headers: {
                    'Dirigible-Editor': 'EntityDataModeler',
                },
                data: '*.model',
            }).then((response) => {
                $scope.availableEntities = response.data.model.entities;
                $scope.state.isBusy = false;
            }, (response) => {
                if (response.data) {
                    if ("error" in response.data) {
                        $scope.state.error = true;
                        $scope.errorMessage = response.data.error.message;
                        console.error(response.data.error);
                        return;
                    }
                }
                $scope.state.error = true;
                $scope.errorMessage = "There was an error while loading the entities.";
            });
        };

        $scope.modelSelected = () => {
            $scope.state.isBusy = true;
            $scope.loadEntities();
        };

        $scope.save = () => {
            $scope.state.busyText = 'Saving...'
            $scope.state.isBusy = true;
            let referencedEntity;
            for (let i = 0; i < $scope.availableEntities.length; i++) {
                if ($scope.dropdowns.entity === $scope.availableEntities[i].name) {
                    referencedEntity = $scope.availableEntities[i];
                    break;
                }
            }
            if ($scope.dataParameters.dialogType === 'refer')
                Dialogs.postMessage({
                    topic: 'edm.editor.reference',
                    data: {
                        cellId: $scope.dataParameters.cellId,
                        model: $scope.dropdowns.model,
                        entity: $scope.dropdowns.entity,
                        perspectiveName: referencedEntity.perspectiveName,
                        perspectiveLabel: referencedEntity.perspectiveLabel,
                        perspectiveIcon: referencedEntity.perspectiveIcon,
                        perspectiveOrder: referencedEntity.perspectiveOrder,
                        perspectiveRole: referencedEntity.perspectiveRole,
                        entityProperties: referencedEntity.properties,
                    }
                });
            else Dialogs.postMessage({
                topic: 'edm.editor.copiedEntity',
                data: {
                    cellId: $scope.dataParameters.cellId,
                    model: $scope.dropdowns.model,
                    entity: $scope.dropdowns.entity,
                    perspectiveName: referencedEntity.perspectiveName,
                    perspectiveLabel: referencedEntity.perspectiveLabel,
                    perspectiveIcon: referencedEntity.perspectiveIcon,
                    perspectiveOrder: referencedEntity.perspectiveOrder,
                    perspectiveRole: referencedEntity.perspectiveRole,
                    entityProperties: referencedEntity.properties,
                }
            });
        };

        $scope.cancel = () => {
            Dialogs.closeWindow();
        };
        $scope.dataParameters = ViewParameters.get();
        $scope.loadModels();
    });