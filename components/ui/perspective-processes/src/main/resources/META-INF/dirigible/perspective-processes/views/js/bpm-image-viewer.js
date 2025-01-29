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
const bpmImageView = angular.module('bpm-image-app', ['platformView', 'blimpKit']);
bpmImageView.constant('MessageHub', new MessageHubApi());
bpmImageView.controller('BpmImageViewController', ($scope, MessageHub) => {
    $scope.imageLink = '';
    $scope.state = {
        isBusy: false,
        error: false,
        busyText: 'Loading...',
    };

    $scope.loadDefinitionImageLink = (definition) => {
        $scope.imageLink = `/services/bpm/bpm-processes/diagram/definition/${definition}`;
        $scope.state.isBusy = false;
    };

    $scope.loadInstanceImageLink = (instance) => {
        $scope.imageLink = `/services/bpm/bpm-processes/diagram/instance/${instance}`;
        $scope.state.isBusy = false;
    };

    MessageHub.addMessageListener({
        topic: 'bpm.diagram.definition',
        handler: (data) => {
            $scope.$evalAsync(() => {
                $scope.state.isBusy = true;
                if (!data.hasOwnProperty('definition')) {
                    $scope.state.error = true;
                    $scope.errorMessage = 'The \'definition\' parameter is missing.';
                } else {
                    $scope.state.error = false;
                    $scope.loadDefinitionImageLink(data.definition);
                }
            });
        }
    });

    MessageHub.addMessageListener({
        topic: 'bpm.diagram.instance',
        handler: (data) => {
            $scope.$evalAsync(() => {
                $scope.state.isBusy = true;
                if (!data.hasOwnProperty('instance')) {
                    $scope.state.error = true;
                    $scope.errorMessage = 'The \'definition\' parameter is missing.';
                } else {
                    $scope.state.error = false;
                    $scope.loadInstanceImageLink(data.instance);
                }
            });
        }
    });
});