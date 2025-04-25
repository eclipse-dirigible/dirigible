/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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
bpmImageView.constant('ThemingHub', new ThemingHub());
bpmImageView.controller('BpmImageViewController', ($scope, MessageHub, ThemingHub) => {
    $scope.imageLink = '/services/web/perspective-processes/images/process.svg';
    $scope.state = {
        isBusy: false,
        error: false,
        busyText: 'Loading...',
    };

    function invertImage() {
        document.getElementById("bpmn-diagram").style.filter = '';
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            document.getElementById("bpmn-diagram").style.filter = 'invert(1)';
        }
    }

    ThemingHub.onThemeChange((theme) => {
        document.getElementById("bpmn-diagram").style.filter = '';
        if (theme.type === 'dark') {
            document.getElementById("bpmn-diagram").style.filter = 'invert(1)';
        }
        if (theme.type === 'auto' && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            document.getElementById("bpmn-diagram").style.filter = 'invert(1)';
        }
    });

    $scope.loadDefinitionImageLink = (definition) => {
        $scope.imageLink = `/services/bpm/bpm-processes/diagram/definition/${definition}`;
        $scope.state.isBusy = false;
        invertImage();
    };

    $scope.loadInstanceImageLink = (instance) => {
        $scope.imageLink = `/services/bpm/bpm-processes/diagram/instance/${instance}`;
        $scope.state.isBusy = false;
        invertImage();
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