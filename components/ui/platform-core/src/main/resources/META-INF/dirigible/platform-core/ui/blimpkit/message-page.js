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
blimpkit.directive('bkMessagePage', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        glyph: '@'
    },
    link: function (scope) {
        if (!scope.glyph) {
            console.error('bk-message-page error: You should provide glpyh icon using the "glyph" attribute');
        }

        scope.getIconClasses = () => classNames(scope.glyph, 'fd-message-page__icon');
    },
    template: `<div class="fd-message-page"><div class="fd-message-page__container">
        <div class="fd-message-page__icon-container"><i role="presentation" ng-class="getIconClasses()"></i></div>
        <div role="status" aria-live="polite" class="fd-message-page__content" ng-transclude></div>
    </div></div>`
})).directive('bkMessagePageTitle', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: '<div class="fd-message-page__title" ng-transclude></div>'
})).directive('bkMessagePageSubtitle', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: '<div class="fd-message-page__subtitle" ng-transclude></div>'
})).directive('bkMessagePageActions', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: '<div class="fd-message-page__actions bk-box--gap" ng-transclude></div>'
})).directive('bkMessagePageMore', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: '<div class="fd-message-page__more" ng-transclude></div>'
}));