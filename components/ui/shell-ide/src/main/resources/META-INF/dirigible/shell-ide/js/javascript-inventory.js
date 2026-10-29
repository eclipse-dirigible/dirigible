/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
// What is left to port of one project's server-side JavaScript and TypeScript (#8051), opened from the
// deprecation banner: the generated files regeneration replaces, the hand-written ones the assistant
// ports - each opens in the editor - and the pages that call their URLs.
angular.module('javascriptInventory', ['blimpKit', 'platformView']).controller('JavascriptInventoryController', ($scope, $http, ViewParameters) => {
    const KINDS = {
        TYPESCRIPT_FILE: 'TypeScript file',
        JAVASCRIPT_FILE: 'JavaScript file',
        EXTENSION_MODULE: 'Extension module',
    };
    const params = ViewParameters.get();
    // A window lives in the shell, outside every perspective, so it names the one whose editors it opens.
    const WORKBENCH = 'workbench';
    const shellHub = new ShellHub();
    const layoutHub = new LayoutHub(WORKBENCH);
    const dialogHub = new DialogHub();

    $scope.project = params.project;
    $scope.loading = true;
    $scope.generated = [];
    $scope.handWritten = [];
    $scope.pages = [];

    const currentWorkspace = () => {
        try {
            return localStorage.getItem(`${getBrandingInfo().prefix}.workspace.selected`) || 'workspace';
        } catch (e) {
            console.warn('Unable to read the selected workspace', e);
            return 'workspace';
        }
    };

    $http.get(`/services/ide/javascript-deprecation/projects/${encodeURIComponent($scope.project)}`).then((response) => {
        $scope.inventory = response.data;
        $scope.generated = response.data.scripts.filter((script) => script.generated);
        $scope.handWritten = response.data.scripts.filter((script) => !script.generated);
        $scope.pages = response.data.references.filter((reference) => !reference.generated);
    }, (response) => {
        console.error('Unable to read what is left to port', response);
        $scope.error = response.status === 404
            ? `${$scope.project} is not published: publish it to see what is left to port.`
            : `Unable to read what is left to port of ${$scope.project}.`;
    }).finally(() => $scope.loading = false);

    $scope.isEmpty = () => !$scope.generated.length && !$scope.handWritten.length && !$scope.pages.length;

    $scope.kindLabel = (kind) => KINDS[kind] || kind.charAt(0) + kind.slice(1).toLowerCase().replaceAll('_', ' ');

    $scope.uses = (script) => [
        ...script.imports,
        ...script.decorators.map((decorator) => `@${decorator}`),
        ...(script.extensionPoint ? [`extension point ${script.extensionPoint}`] : []),
        ...script.runs,
    ].join(', ');

    $scope.open = (path) => {
        shellHub.showPerspective({ id: WORKBENCH });
        layoutHub.openEditor({ path: `/${currentWorkspace()}/${$scope.project}/${path}` });
        dialogHub.closeWindow({ id: 'javascript-inventory' });
    };

    $scope.openGuide = () => window.open(params.guide, '_blank', 'noopener,noreferrer');
});
