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
blimpkit.directive('bkBar', (classNames) => ({
    restrict: 'EA',
    replace: true,
    transclude: true,
    scope: {
        barDesign: '@?',
        compact: '<?',
        inPage: '<?',
        padding: '@?'
    },
    link: (scope) => {
        const barDesigns = ['header', 'subheader', 'header-with-subheader', 'footer', 'floating-footer'];
        const paddings = ['s', 'm_l', 'xl', 'responsive'];

        if (scope.barDesign && !barDesigns.includes(scope.barDesign)) {
            console.error(`bk-bar error: 'bar-design' must be one of: ${barDesigns.join(', ')}`);
        }

        if (scope.padding && scope.compact === true) {
            console.error("bk-bar error: 'padding' and 'compact' attributes are incompatible.");
        }

        if (scope.padding && !paddings.includes(scope.padding)) {
            console.error(`bk-bar error: 'padding' must be one of: ${paddings.join(', ')}`);
        }

        scope.getClasses = () => classNames('fd-bar', {
            [`fd-bar--${scope.barDesign}`]: barDesigns.includes(scope.barDesign),
            'fd-bar--compact': scope.compact === true,
            'fd-bar--page': scope.inPage,
            'fd-bar--page-s': scope.padding === 's',
            'fd-bar--page-m_l': scope.padding === 'm_l',
            'fd-bar--page-xl': scope.padding === 'xl',
            'fd-bar--responsive-paddings': scope.padding === 'responsive',
        });
    },
    template: '<div ng-class="getClasses()" ng-transclude></div>'
})).directive('bkBarLeft', () => ({
    restrict: 'EA',
    replace: true,
    transclude: true,
    template: '<div class="fd-bar__left" ng-transclude></div>'
})).directive('bkBarMiddle', () => ({
    restrict: 'EA',
    replace: true,
    transclude: true,
    template: '<div class="fd-bar__middle" ng-transclude></div>'
})).directive('bkBarRight', () => ({
    restrict: 'EA',
    replace: true,
    transclude: true,
    template: '<div class="fd-bar__right" ng-transclude></div>'
})).directive('bkBarElement', (classNames) => ({
    restrict: 'EA',
    replace: true,
    transclude: true,
    scope: {
        fullWidth: '<?',
        isTitle: '<?'
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-bar__element', {
            'fd-bar__element--title': scope.isTitle === true,
            'fd-bar__element--full-width': scope.fullWidth === true,
        });
    },
    template: '<div ng-class="getClasses()" ng-transclude></div>'
}));