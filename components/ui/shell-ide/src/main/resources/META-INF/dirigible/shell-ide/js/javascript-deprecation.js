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
// The deprecation banner of server-side JavaScript and TypeScript (#8049): shown while a published project
// has a deprecation entry in the Problems view, refreshed on every publish and unpublish, and gone for good
// in this browser once dismissed. An instance where no project uses them shows nothing.
angular.module('javascriptDeprecation', ['blimpKit'])
    .directive('javascriptDeprecationBanner', ['$http', ($http) => ({
        restrict: 'E',
        scope: {},
        link: (scope) => {
            const dismissedKey = `${getBrandingInfo().prefix}.javascript-deprecation.dismissed`;
            const messageHub = new MessageHubApi();

            const isDismissed = () => {
                try {
                    return localStorage.getItem(dismissedKey) === 'true';
                } catch (e) {
                    console.warn('Unable to read whether the JavaScript deprecation banner was dismissed', e);
                    return false;
                }
            };

            scope.dismissed = isDismissed();
            scope.projects = [];
            scope.guide = '';
            // Each project opens what is left to port of it, file by file (#8051).
            const dialogHub = new DialogHub();
            scope.showInventory = (project) => dialogHub.showWindow({ id: 'javascript-inventory', params: { project: project, guide: scope.guide } });

            const refresh = () => {
                if (scope.dismissed) return;
                $http.get('/services/ide/javascript-deprecation').then((response) => {
                    scope.projects = response.data.projects;
                    scope.guide = response.data.guide;
                }, (response) => {
                    console.error('Unable to read the JavaScript deprecation notice', response);
                });
            };

            scope.dismiss = () => {
                scope.dismissed = true;
                try {
                    localStorage.setItem(dismissedKey, 'true');
                } catch (e) {
                    console.warn('Unable to remember that the JavaScript deprecation banner was dismissed', e);
                }
            };

            const listeners = [
                messageHub.addMessageListener({ topic: 'platform.publisher.published', handler: refresh }),
                messageHub.addMessageListener({ topic: 'platform.publisher.unpublished', handler: refresh }),
            ];
            scope.$on('$destroy', () => listeners.forEach((listener) => messageHub.removeMessageListener(listener)));

            refresh();
        },
        template: `<div class="fd-margin--tiny" ng-if="!dismissed && projects.length">
    <bk-message-strip state="warning" glyph="sap-icon--message-warning" on-dismiss="dismiss()">
        Server-side JavaScript and TypeScript are deprecated and will be removed in 16.0. Used by:
        <span ng-repeat="project in projects"><a class="fd-link" href="" ng-click="showInventory(project)" title="What is left to port of {{ project }}">{{ project }}</a>{{ $last ? '.' : ', ' }}</span>
        See the Problems view and the <a class="fd-link" href="{{ guide }}" target="_blank" rel="noopener noreferrer">migration guide</a>.
    </bk-message-strip>
</div>`
    })]);
