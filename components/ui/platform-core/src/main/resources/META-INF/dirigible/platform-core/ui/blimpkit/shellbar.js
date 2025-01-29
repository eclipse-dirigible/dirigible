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
blimpkit.directive('bkShellbar', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        size: '@?',
    },
    sizes: {
        'xl': 'xl',
        'm': 'm',
        'l': 'l',
        's': 's',
        'responsive': 'responsive-paddings'
    },
    link: function (scope) {
        scope.getClasses = () => classNames('fd-shellbar', {
            [`fd-shellbar--${this.sizes[scope.size]}`]: scope.size && this.sizes[scope.size]
        });
    },
    template: '<div ng-class="getClasses()" style="min-height: var(--fdShellbar_Height);" ng-transclude></div>'
})).directive('bkShellbarGroup', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        position: '@?',
        shrink: '<?'
    },
    positions: {
        'left': 'product',
        'center': 'center',
        'right': 'actions',
    },
    link: function (scope) {
        scope.getClasses = () => classNames('fd-shellbar__group', {
            [`fd-shellbar__group--${this.positions[scope.position]}`]: scope.position && this.positions[scope.position],
            'fd-shellbar__group--shrink': scope.shrink,
            'fd-shellbar__group--basis-auto': scope.shrink === false,
        });
    },
    template: '<div ng-class="getClasses()" ng-transclude></div>'
})).directive('bkShellbarAction', (classNames) => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: {
        grow: '<?',
        shrink: '<?',
        mobile: '<?',
        desktop: '<?'
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-shellbar__action', {
            'fd-shellbar__action--grow': scope.grow,
            'fd-shellbar__action--shrink': scope.shrink,
            'fd-shellbar__action--mobile': scope.mobile,
            'fd-shellbar__action--desktop': scope.desktop,
        });
    },
    template: '<div ng-class="getClasses()" ng-transclude>'
})).directive('bkShellbarTitle', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-shellbar__title');
    },
})).directive('bkShellbarLogo', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-shellbar__logo');
    },
})).directive('bkShellbarButton', () => ({
    restrict: 'A',
    link: (_scope, element, attrs) => {
        element.addClass('fd-shellbar__button');
        if (Object.prototype.hasOwnProperty.call(attrs, 'isMenu') && attrs.isMenu === 'true')
            element.addClass('fd-shellbar__button--menu')
    },
})).directive('bkShellbarSearchField', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-shellbar__search-field');
    },
})).directive('bkShellbarSearchFieldHelper', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-shellbar__search-field-helper');
    },
})).directive('bkShellbarSearchFieldAddonSubmit', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-shellbar__search-field-addon');
        element.addClass('fd-shellbar__search-submit');
    },
})).directive('bkShellbarSearchFieldAddonCancel', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-shellbar__search-field-addon');
        element.addClass('fd-shellbar__search-cancel');
    },
})).directive('bkShellbarSearchInput', () => ({
    restrict: 'A',
    link: (_scope, element) => {
        element.addClass('fd-shellbar__search-field-input');
    },
}));