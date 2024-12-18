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
blimpkit.directive('bkBusyIndicator', (classNames) => ({
    restrict: 'E',
    transclude: false,
    replace: true,
    scope: {
        size: '@?',
        contrast: '<?'
    },
    link: (scope) => {
        scope.getClasses = () => classNames({
            'fd-busy-indicator--m': scope.size === 'm',
            'fd-busy-indicator--l': scope.size === 'l',
            'fd-busy-indicator--contrast': scope.contrast === true,
        });
    },
    template: `<div class="fd-busy-indicator" ng-class="getClasses()" aria-label="Loading">
        <div class="fd-busy-indicator__circle"></div>
        <div class="fd-busy-indicator__circle"></div>
        <div class="fd-busy-indicator__circle"></div>
    </div>`
})).directive('bkBusyIndicatorExtended', () => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        size: '@?',
        contrast: '<?'
    },
    template: `<div class="fd-busy-indicator-extended">
        <bk-busy-indicator size="{{size}}" contrast="contrast"></bk-busy-indicator>
        <div class="fd-busy-indicator-extended__label" ng-transclude></div>
    </div>`
}));