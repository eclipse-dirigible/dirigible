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
const bpmProcessViewer = angular.module('bpm-process-viewer', ['platformView', 'blimpKit']);
bpmProcessViewer.constant('MessageHub', new MessageHubApi());
bpmProcessViewer.controller('BpmProcessViewerController', ($scope, $http, MessageHub) => {
    $scope.state = {
        isBusy: false,
        error: false,
        busyText: 'Loading...',
    };

    $scope.processId = '';
    let instanceId = '';
    const activities = [];

    const bpmnVisualization = new bpmnvisu.BpmnVisualization({ container: 'bpmn-container', navigation: { enabled: true } });
    let style = bpmnVisualization.graph.getStylesheet().getDefaultVertexStyle();
    let edgeStyle = bpmnVisualization.graph.getStylesheet().getDefaultEdgeStyle();

    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_FONTCOLOR] = 'var(--font_color)';
    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_LABEL_COLOR] = 'var(--font_color)';
    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_STROKECOLOR] = 'var(--stroke_color)';
    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_STROKEWIDTH] = 2;
    edgeStyle[bpmnvisu.mxgraph.mxConstants.STYLE_ROUNDED] = true;

    style[bpmnvisu.mxgraph.mxConstants.STYLE_FILLCOLOR] = 'var(--fill_color)';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_FONTCOLOR] = 'var(--font_color)';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_STROKECOLOR] = 'var(--stroke_color)';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_FONTFAMILY] = 'var(--font)';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_FONTSIZE] = '12';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_ARCSIZE] = '12';
    style[bpmnvisu.mxgraph.mxConstants.STYLE_ROUNDED] = true;

    function loadActivities(historic = false) {
        let endpoint;
        if (historic) endpoint = `/services/bpm/bpm-processes/historic-instances/${instanceId}/variables`;
        else endpoint = `/services/bpm/bpm-processes/instance/${instanceId}/active`;
        $http.get(endpoint).then((response) => {
            if (historic) {
                // TODO
            } else {
                activities.forEach((activity) => bpmnVisualization.bpmnElementsRegistry.removeCssClasses(activity, ['highlight']));
                activities.length = 0;
                activities.push(...response.data);
                activities.forEach((activity) => bpmnVisualization.bpmnElementsRegistry.addCssClasses(activity, ['highlight']));
            }
        }, (error) => {
            console.error(`Failed to fetch active activities for process instance: ${instanceId}`, error);
        }).finally(() => {
            $scope.$evalAsync(() => {
                $scope.state.isBusy = false;
            });
        });
    }

    function prepareXmlString(raw) {
        const parser = new DOMParser();
        const bpmn = parser.parseFromString(raw, 'application/xml');
        const subprocesses = bpmn.querySelectorAll('subProcess');
        for (let i = 0; i < subprocesses.length; i++) {
            let bpmnElement = bpmn.querySelector(`[bpmnElement="${subprocesses[i].id}"]`);
            bpmnElement.setAttribute('isExpanded', 'true');
        }
        return new XMLSerializer().serializeToString(bpmn);
    }

    function loadBpmnFromApi() {
        $http.get(`/services/bpm/bpm-processes/definition/bpmn?id=${$scope.processId}`).then((response) => {
            bpmnVisualization.load(prepareXmlString(response.data), { fit: { type: bpmnvisu.FitType.None, margin: 16 } });
            if (instanceId) {
                loadActivities();
            } else {
                $scope.$evalAsync(() => {
                    $scope.state.isBusy = false;
                });
            }
        }, (error) => {
            console.error(`Failed to fetch active activities for process instance: ${instanceId}`, error);
            $scope.$evalAsync(() => {
                $scope.state.isBusy = false;
            });
        });
    }

    $scope.zoomIn = () => bpmnVisualization.navigation.graph.zoomIn();

    $scope.zoomOut = () => bpmnVisualization.navigation.graph.zoomOut();

    $scope.actualSize = () => bpmnVisualization.navigation.graph.zoomActual();

    $scope.fit = () => bpmnVisualization.navigation.fit({ type: bpmnvisu.FitType.Center, margin: 16 });

    MessageHub.addMessageListener({
        topic: 'bpm.diagram.definition',
        handler: (data) => {
            $scope.$evalAsync(() => {
                $scope.state.isBusy = true;
                if (!data.hasOwnProperty('definition')) {
                    $scope.state.error = true;
                    $scope.errorMessage = 'The \'definition\' parameter is missing.';
                } else {
                    $scope.processId = data.definition;
                    instanceId = '';
                    $scope.state.error = false;
                    loadBpmnFromApi();
                }
            });
        }
    });

    MessageHub.addMessageListener({
        topic: 'bpm.diagram.instance',
        handler: (data) => {
            if ($scope.processId) $scope.$evalAsync(() => {
                $scope.state.isBusy = true;
                if (!data.hasOwnProperty('instance')) {
                    $scope.state.error = true;
                    $scope.errorMessage = 'The \'instance\' parameter is missing.';
                } else {
                    instanceId = data.instance;
                    $scope.state.error = false;
                    loadActivities();
                }
            });
        }
    });

    MessageHub.addMessageListener({
        topic: 'bpm.historic.instance.selected',
        handler: (data) => {
            if ($scope.processId === data.definition) $scope.$evalAsync(() => {
                $scope.state.isBusy = true;
                if (!data.hasOwnProperty('instance')) {
                    $scope.state.error = true;
                    $scope.errorMessage = 'The \'instance\' parameter is missing.';
                } else {
                    instanceId = data.instance;
                    $scope.state.error = false;
                    loadActivities(true);
                }
            });
        }
    });
});