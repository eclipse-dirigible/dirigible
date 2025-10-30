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
const previewView = angular.module('preview', ['blimpKit', 'platformView']);
previewView.controller('PreviewController', ($scope, $document, ButtonStates) => {
    const defaultParameters = {
        'container': 'layout',
        'perspectiveId': 'workbench'
    };
    const workspaceHub = new WorkspaceHub();
    const dialogHub = new DialogHub();
    const layoutHub = new LayoutHub();
    const notificationHub = new NotificationHub();
    const customParamsId = `${getBrandingInfo().prefix}.preview.customParameters`;
    const urlLockedId = `${getBrandingInfo().prefix}.preview.urlLocked`;
    let isDebugPreview = false;
    let iframe;

    $scope.urlLocked = localStorage.getItem(urlLockedId) === 'true';
    $scope.previewUrl = {
        value: '',
    };

    function initCustomParameters() {
        const saved = localStorage.getItem(customParamsId);
        if (saved) {
            $scope.customParameters = JSON.parse(saved);
        } else {
            $scope.customParameters = defaultParameters;
            localStorage.setItem(customParamsId, JSON.stringify(defaultParameters));
        }
    }

    initCustomParameters();

    angular.element($document[0]).ready(() => {
        iframe = $document[0].getElementById('preview-iframe');
    });

    $scope.history = {
        idx: -1,
        state: []
    };

    $scope.urlLockedToggle = () => {
        $scope.urlLocked = !$scope.urlLocked;
        localStorage.setItem(urlLockedId, `${$scope.urlLocked}`);
    };

    $scope.reload = () => {
        if (iframe.contentDocument) {
            iframe.contentDocument.location.reload(true);
        }
    };

    $scope.getParams = () => JSON.stringify($scope.customParameters);

    $scope.getCurrentUrl = () => $scope.history.state[$scope.history.idx];

    $scope.hasBack = () => $scope.history.idx > 0;

    $scope.hasForward = () => $scope.history.idx < $scope.history.state.length - 1;

    $scope.goBack = () => {
        if ($scope.hasBack()) {
            const url = $scope.history.state[--$scope.history.idx];
            replaceLocationUrl(url);
        }
    };

    $scope.goForward = () => {
        if ($scope.hasForward()) {
            const url = $scope.history.state[++$scope.history.idx];
            replaceLocationUrl(url);
        }
    };

    const gotoUrl = (url, shouldReload = true) => {
        if ($scope.getCurrentUrl() === url) {
            if (shouldReload || isDebugPreview) $scope.reload();
            return;
        };

        if ($scope.history.idx >= 0)
            $scope.history.state.length = $scope.history.idx + 1;

        $scope.history.state.push(url);
        $scope.history.idx++;

        replaceLocationUrl(url);
    };

    const replaceLocationUrl = (url) => {
        $scope.previewUrl.value = url;
        iframe.contentWindow.location.replace(url);
    };

    $scope.inputUrlKeyUp = (e) => {
        if (e.key === 'Escape') {
            $scope.previewUrl.value = $scope.getCurrentUrl() ?? '';
        } else if (e.key === 'Enter') {
            $scope.previewUrl.value = e.target.value;
            if ($scope.previewUrl.value) {
                gotoUrl($scope.previewUrl.value);
            }
        }
    };

    const makeUrlFromPath = (resourcePath) => {
        let url = window.location.protocol + '//' + window.location.host + window.location.pathname.substring(window.location.pathname.indexOf('/web/'), 0);
        const type = resourcePath.substring(resourcePath.lastIndexOf('.') + 1);
        const isOData = resourcePath.endsWith('.odata');
        const isOpenAPI = resourcePath.endsWith('.openapi');
        if (isOData) {
            url = window.location.protocol + '//' + window.location.host + '/odata/v2/';
        } else if (isOpenAPI) {
            url = `${window.location.protocol}//${window.location.host}/services/web/view-swagger/ui/index.html?openapi=/services/web${resourcePath}`;
        } else {
            switch (type) {
                case 'rhino':
                    url += '/rhino';
                    break;
                case 'nashorn':
                    url += '/nashorn';
                    break;
                case 'v8':
                    url += '/v8';
                    break;
                case 'graalvm':
                    url += '/graalvm';
                    break;
                case 'ts':
                    url += '/ts';
                    break;
                case 'js':
                    url += '/js';
                    break;
                case 'mjs':
                    url += '/js';
                    break;
                case 'ts':
                    url += '/js';
                    break;
                case 'xsjs':
                    url += '/xsjs';
                    break;
                case 'py':
                    url += '/py';
                    break;
                case 'md':
                    url += '/wiki';
                    break;
                case 'markdown':
                    url += '/wiki';
                    break;
                case 'confluence':
                    url += '/wiki';
                    break;
                case 'command':
                    url += '/command';
                    break;
                case 'xsodata':
                    url += '/web';
                    break;
                case 'edm':
                case 'dsm':
                case 'dmm':
                case 'bpmn':
                case 'job':
                case 'xsjob':
                case 'calculationview':
                case 'websocket':
                case 'hdi':
                case 'hdbtable':
                case 'hdbstructurе':
                case 'hdbstructure':
                case 'hdbview':
                case 'hdbtablefunction':
                case 'hdbprocedure':
                case 'hdbschema':
                case 'hdbsynonym':
                case 'hdbdd':
                case 'hdbsequence':
                case 'hdbcalculationview':
                case 'xsaccess':
                case 'xsprivileges':
                case 'xshttpdest':
                case 'listener':
                case 'extensionpoint':
                case 'extension':
                case 'table':
                case 'view':
                case 'access':
                case 'roles':
                case 'sh':
                case 'csv':
                case 'csvim':
                case 'hdbti':
                case 'camel':
                case 'form':
                case 'report':
                    return;
                default:
                    url += '/web';
            }
            url += resourcePath;
        }
        if (isDebugPreview) url += '?debug=true';
        return url;
    };

    $scope.customizeParameters = (custom) => {
        dialogHub.showFormDialog({
            title: 'Set custom ViewParameters',
            subheader: 'Data will be accessible through the "ViewParameters.get()" function.',
            form: {
                'cvp': {
                    label: 'JSON data',
                    controlType: 'textarea',
                    placeholder: defaultParameters,
                    value: custom ?? JSON.stringify($scope.customParameters, null, 2),
                    rows: 10,
                    focus: true,
                },
            },
            submitLabel: 'Apply',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                try {
                    const customData = JSON.parse(form['cvp']);
                    localStorage.setItem(customParamsId, JSON.stringify(customData));
                    $scope.$evalAsync(() => {
                        $scope.customParameters = customData;
                        $scope.reload();
                    });
                } catch (error) {
                    dialogHub.showAlert({
                        title: 'Invalid input',
                        message: 'Not a valid JSON',
                        type: AlertTypes.Error,
                        preformatted: false,
                        buttons: [
                            { id: 'edit', label: 'Edit', state: ButtonStates.Emphasized },
                            { id: 'close', label: 'Close' }
                        ]
                    }).then((buttonId) => {
                        if (buttonId === 'edit') $scope.customizeParameters(form['cvp']);
                    });
                }
            }
        });
    };

    const fileSelected = (fileDescriptor) => {
        if ($scope.urlLocked) return;
        const pathSegments = fileDescriptor.path.split('/');
        pathSegments.splice(1, 1);
        const url = makeUrlFromPath(pathSegments.join('/'));
        if (url) $scope.$evalAsync(gotoUrl(url, false));
    };

    workspaceHub.addMessageListener({
        topic: 'preview.file.debug',
        handler: (fileDescriptor) => {
            layoutHub.isViewOpen({ id: 'debugger' }).then((data) => {
                if (data.isOpen) {
                    isDebugPreview = true;
                    fileSelected(fileDescriptor);
                } else {
                    layoutHub.openView({ id: 'debugger' });
                    notificationHub.show({
                        type: 'information',
                        title: 'Debugger not loaded',
                        description: 'Debugger view is not loaded. Please wait for it to load and then try to debug again.',
                    });
                }
            }, (error) => {
                console.error(error);
                Dialogs.showAlert({
                    title: 'Debug preview failed',
                    message: 'An unexpected error has occurred. Check console for errors.',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            });
        }
    });

    workspaceHub.onFileSelected((fileDescriptor) => {
        isDebugPreview = false;
        fileSelected(fileDescriptor);
    });

    workspaceHub.onPublished((fileDescriptor) => {
        if ($scope.urlLocked) return;

        if (fileDescriptor.path) {
            const pathSegments = fileDescriptor.path.split('/');
            pathSegments.splice(1, 1);
            const url = makeUrlFromPath(pathSegments.join('/'));
            if (url) $scope.$evalAsync(gotoUrl(url));
        } else $scope.$evalAsync($scope.reload());
    });

    workspaceHub.onUnpublished((fileDescriptor) => {
        if ($scope.urlLocked) return;

        if (fileDescriptor.path) {
            const pathSegments = fileDescriptor.path.split('/');
            pathSegments.splice(1, 1);
            const url = makeUrlFromPath(pathSegments.join('/'));
            if (url) $scope.$evalAsync(gotoUrl(url));
        } else $scope.$evalAsync($scope.reload());
    });
});