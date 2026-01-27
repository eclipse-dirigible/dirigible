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
bpmProcessViewer.constant('Dialogs', new DialogHub());
bpmProcessViewer.controller('BpmProcessViewerController', ($scope, $http, Dialogs) => {
    $scope.state = {
        isBusy: false,
        busyText: 'Loading...',
    };

    $scope.processId = '';
    let instanceId = '';
    const badgeIds = [];

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

    function convertMailServiceTasksToSendTasks(xmlDoc) {
        const serviceTasks = Array.from(xmlDoc.getElementsByTagName('serviceTask'));

        for (let task of serviceTasks) {
            if (task.getAttribute('flowable:type') === 'mail') {
                const sendTask = xmlDoc.createElementNS(
                    task.namespaceURI,
                    'sendTask'
                );

                for (const attr of task.attributes) {
                    if (attr.name !== 'flowable:type') {
                        sendTask.setAttribute(attr.name, attr.value);
                    }
                }

                while (task.firstChild) {
                    sendTask.appendChild(task.firstChild);
                }

                task.parentNode.replaceChild(sendTask, task);
            }
        };

        return xmlDoc;
    }


    function loadActivities(historic = false) {
        let endpoint;
        if (historic) endpoint = `/services/bpm/bpm-processes/historic-instances/${instanceId}/variables`;
        else endpoint = `/services/bpm/bpm-processes/instance/${instanceId}/active`;
        $http.get(endpoint).then((response) => {
            if (!historic) {
                clearBadges();
                for (const [key, value] of Object.entries(response.data)) {
                    setBadges(key, getBadgeConfig(value));
                }
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
        return new XMLSerializer().serializeToString(convertMailServiceTasksToSendTasks(bpmn));
    }

    function loadBpmnFromApi() {
        $http.get(`/services/bpm/bpm-processes/definition/bpmn?id=${$scope.processId}`).then((response) => {
            bpmnVisualization.load(prepareXmlString(response.data), { fit: { type: bpmnvisu.FitType.None, margin: 16 } });
            // makeClickable();
            loadBadges();
            if (instanceId) loadActivities();
            else $scope.$evalAsync(() => {
                $scope.state.isBusy = false;
            });
        }, (error) => {
            console.error(`Failed to fetch active activities for process instance: ${instanceId}`, error);
            $scope.$evalAsync(() => {
                $scope.state.isBusy = false;
            });
        });
    }

    // function makeClickable() {
    //     const elements = bpmnVisualization.bpmnElementsRegistry.getElementsByKinds([
    //         bpmnvisu.ShapeBpmnElementKind.TASK,
    //         bpmnvisu.ShapeBpmnElementKind.TASK_USER,
    //         bpmnvisu.ShapeBpmnElementKind.TASK_MANUAL,
    //         bpmnvisu.ShapeBpmnElementKind.TASK_SERVICE
    //     ]);
    //     for (let e = 0; e < elements.length; e++) {
    //         bpmnVisualization.bpmnElementsRegistry.addCssClasses(elements[e].bpmnSemantic.id, ['clickable']);
    //         elements[e].htmlElement.onclick = () => {
    //             bpmnVisualization.bpmnElementsRegistry.toggleCssClasses(elements[e].bpmnSemantic.id, ['highlight']);
    //         };
    //     }
    // }

    function loadBadges(hideBusy = false) {
        $http.get(`/services/bpm/bpm-processes/definition/${$scope.processId}/active`).then((response) => {
            clearBadges();
            for (const [key, value] of Object.entries(response.data)) {
                setBadges(key, getBadgeConfig(value));
            }
        }, (error) => {
            console.error(`Failed to fetch badge data for definition: ${$scope.processId}`, error);
        }).finally(() => {
            if (hideBusy) $scope.$evalAsync(() => {
                $scope.state.isBusy = false;
            })
        });
    }

    function getBadgeConfig(data) {
        let badges = [];
        if (data.negative) {
            badges.push({
                position: 'bottom-right',
                label: data.negative.toString(),
                style: {
                    font: { color: 'var(--font_color)', size: 14 },
                    fill: { color: 'var(--badgeNegative)' },
                    stroke: { color: 'var(--badgeNegative)' }
                }
            });
        }
        if (data.positive) {
            badges.push({
                position: 'bottom-left',
                label: data.positive.toString(),
                style: {
                    font: { color: 'var(--font_color)', size: 14 },
                    fill: { color: 'var(--badgePositive)' },
                    stroke: { color: 'var(--badgePositive)' }
                }
            });
        }
        return badges;
    }

    function clearBadges() {
        for (let i = 0; i < badgeIds.length; i++) {
            bpmnVisualization.bpmnElementsRegistry.removeAllOverlays(badgeIds[i]);
        }
        badgeIds.length = 0;
    }

    function setBadges(id, badges) {
        badgeIds.push(id);
        bpmnVisualization.bpmnElementsRegistry.addOverlays(id, badges);
    }

    $scope.zoomIn = () => bpmnVisualization.navigation.graph.zoomIn();

    $scope.zoomOut = () => bpmnVisualization.navigation.graph.zoomOut();

    $scope.actualSize = () => bpmnVisualization.navigation.graph.zoomActual();

    $scope.fit = () => bpmnVisualization.navigation.fit({ type: bpmnvisu.FitType.Center, margin: 16 });

    $scope.refresh = () => {
        $scope.state.isBusy = true;
        loadBpmnFromApi();
    };

    let defIntervalId = setInterval(() => {
        if (!$scope.processId) Dialogs.triggerEvent('bpm.process.instances.get-definition');
        else cancelInterval();
    }, 300);

    function cancelInterval() {
        defIntervalId = clearInterval(defIntervalId);
    }

    Dialogs.addMessageListener({
        topic: 'bpm.definition.selected',
        handler: (data) => {
            if (data.noData) cancelInterval();
            else $scope.$evalAsync(() => {
                if (!data.hasOwnProperty('id')) {
                    console.error('The definition \'id\' parameter is missing.');
                    Dialogs.showAlert({
                        title: 'Missing data',
                        message: 'Process definition id is missing from event!',
                        type: AlertTypes.Error,
                        preformatted: false,
                    });
                } else {
                    if (defIntervalId) cancelInterval();
                    if ($scope.processId !== data.id) {
                        $scope.state.isBusy = true;
                        $scope.processId = data.id;
                        instanceId = '';
                        loadBpmnFromApi();
                    }
                }
            });
        }
    });

    Dialogs.addMessageListener({
        topic: 'bpm.instance.selected',
        handler: (data) => {
            if ($scope.processId) $scope.$evalAsync(() => {
                $scope.state.isBusy = true;
                if (data.deselect) {
                    loadBadges(true);
                } else {
                    if (!data.hasOwnProperty('instance')) {
                        console.error('The \'instance\' parameter is missing.');
                    } else {
                        instanceId = data.instance;
                        loadActivities();
                    }
                }
            });
        }
    });

    Dialogs.addMessageListener({
        topic: 'bpm.historic.instance.selected',
        handler: (data) => {
            $scope.$evalAsync(() => {
                $scope.state.isBusy = true;
                if (data.deselect) {
                    loadBadges(true);
                } else {
                    $scope.processId === data.definition
                    $scope.state.isBusy = true;
                    instanceId = '';
                    loadBpmnFromApi();
                }
            });
        }
    });
});