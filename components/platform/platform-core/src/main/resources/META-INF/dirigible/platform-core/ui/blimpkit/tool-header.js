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
blimpkit.directive('bkToolHeader', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        hasMenu: '<?',
        size: '@?',
        responsive: '<?'
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-tool-header', {
            'fd-tool-header--menu': scope.hasMenu,
            [`fd-tool-header--${scope.size}`]: scope.size,
        });
    },
    template: '<div ng-class="getClasses()" ng-transclude></div>'
})).directive('bkToolHeaderGroup', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-tool-header__group');
    },
})).directive('bkToolHeaderGroup', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        hasMenu: '<?',
        position: '@?',
        isHidden: '<?',
    },
    link: (scope) => {
        scope.getClasses = () => classNames({
            'fd-tool-header__group--menu': scope.hasMenu,
            'fd-tool-header__group--hidden': scope.isHidden,
            'fd-tool-header__group--center': scope.position === 'center',
            'fd-tool-header__group--actions': scope.position === 'right'
        })
    },
    template: '<div class="fd-tool-header__group" ng-class="getClasses()" ng-transclude></div>'
})).directive('bkToolHeaderElement', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-tool-header__element');
    },
})).directive('bkToolHeaderButton', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-button--tool-header');
    },
})).directive('bkToolHeaderTitle', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-tool-header__product-name');
    },
})).directive('bkToolHeaderLogo', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-tool-header__logo');
    },
}));