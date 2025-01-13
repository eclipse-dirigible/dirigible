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
blimpkit.directive('bkLink', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        isActive: '<?',
        isFocus: '<?',
        leftGlyph: '@?',
        rightGlyph: '@?',
        state: '@?',
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-link', {
            'is-active': scope.isActive === true,
            'is-focus': scope.isFocus === true,
            [`fd-link--${scope.state}`]: scope.state,
        });
    },
    template: '<a ng-class="getClasses()" tabindex="0"><span ng-if="leftGlyph" class="{{leftGlyph}}"></span><span class="fd-link__content" ng-transclude></span><span ng-if="rightGlyph" class="{{rightGlyph}}"></span></a>',
}));