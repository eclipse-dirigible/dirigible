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
blimpkit.directive('bkTileContainer', (classNames) => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: {
        noPadding: '<?',
        noWrap: '<?',
        isList: '<?'
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-tile-container', {
            'fd-tile-container--list': scope.isList === true,
            'bk-flex--nowrap': scope.noWrap === true,
            'fd-padding--none': scope.noPadding === true,
        });
    },
    template: '<div ng-class="getClasses()" ng-transclude></div>'
})).directive('bkTile', (classNames) => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: {
        isSmall: '<?',
        isLong: '<?',
        isSlide: '<?',
        isLine: '<?',
        isAction: '<?',
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-tile', {
            'fd-tile--s': scope.isSmall === true,
            'fd-tile--double': scope.isLong === true,
            'fd-tile--slide': scope.isSlide === true,
            'fd-tile--line': scope.isLine === true,
            'fd-tile--action': scope.isAction === true,
        });
    },
    template: `<div role="button" tabindex="0" ng-class="getClasses()" ng-transclude></div`
})).directive('bkTileBackground', () => ({
    restrict: 'E',
    replace: true,
    scope: {
        link: '@',
    },
    template: `<div class="fd-tile__background-img" ng-style="{'background-image':'url(' + link + ')','background-size':'cover'}"></div>`
})).directive('bkTileSlide', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    template: '<div class="fd-tile__container" ng-transclude></div>'
})).directive('bkTilePageIndicator', () => ({
    restrict: 'E',
    replace: true,
    scope: {
        pages: '<',
        activePage: '<',
    },
    template: `<div class="fd-tile__page-indicator">
        <span class="fd-tile__dot{{$index + 1 === activePage ? ' fd-tile__dot--active' : undefined}}" ng-repeat="x in [].constructor(pages) track by $index"></span>
    </div>`
})).directive('bkTileActionContainer', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    template: '<div class="fd-tile__action-container" ng-transclude></div>'
})).directive('bkTileActionIndicator', () => ({
    restrict: 'A',
    link: function (_scope, element) { element.addClass('fd-tile__action-indicator'); element.addClass('is-compact') },
})).directive('bkTileActionClose', () => ({
    restrict: 'A',
    link: function (_scope, element) { element.addClass('fd-tile__action-close'); element.addClass('is-compact') },
})).directive('bkTileHeader', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: {
        title: '@?',
        subtitle: '@?',
    },
    template: '<div class="fd-tile__header" ng-transclude><div class="fd-tile__title">{{title}}</div><div class="fd-tile__subtitle">{{subtitle}}</div></div>'
})).directive('bkTileContent', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    template: '<div class="fd-tile__content" ng-transclude></div>'
})).directive('bkTileFooter', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    template: '<div class="fd-tile__footer" ng-transclude></div>'
}));