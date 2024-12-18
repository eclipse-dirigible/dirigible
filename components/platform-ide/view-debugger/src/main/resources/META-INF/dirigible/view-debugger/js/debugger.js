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
const debuggerView = angular.module('debugger', ['blimpKit', 'platformView']);
debuggerView.filter('trusted', ['$sce', ($sce) => {
    return (url) => $sce.trustAsResourceUrl(url);
}]);
debuggerView.controller('DebuggerController', ($scope, $document) => {
    const protocol = window.location.protocol === 'http:' ? 'ws' : 'wss';
    const hostPortIndexOf = window.location.host.indexOf(':');
    const host = hostPortIndexOf > 0 ? window.location.host.substring(0, hostPortIndexOf) : window.location.host;
    const devToolsLocation = '/services/web/dev-tools/js_app.html'; // 'devtools://devtools/bundled/js_app.html';
    // TODO: The debug port can be configured
    const debugPort = 8081;
    const debuggerLocation = devToolsLocation + '?' + protocol + '=' + host + ':' + debugPort + '/debug';
    const tokenParam = 'refreshToken=' + new Date().getTime();
    $scope.previewUrl = debuggerLocation;// + resourcePath;
    $scope.previewUrl += ($scope.previewUrl.indexOf('?') > 0 ? ($scope.previewUrl.endsWith('?') ? tokenParam : ('&' + tokenParam)) : ('?' + tokenParam));
    $scope.loading = true;

    let iframe;

    angular.element($document[0]).ready(() => {
        iframe = $document[0].getElementById('debug-iframe');
        iframe.onload = () => $scope.$evalAsync(() => {
            $scope.loading = false;
        });
        iframe.onerror = () => $scope.$evalAsync(() => {
            console.error('Error while loading DevTools');
            $scope.loading = false;
        });
    });
});