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
blimpkit.directive('bkList', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        compact: '<?',
        noBorder: '<?',
        listType: '@?',
        fixedHeight: '@?',
        byline: '<?',
        hasMessage: '<?',
        dropdownMode: '<?'
    },
    link: function (scope) {
        const parseHeight = function (height) {
            if (Number.isFinite(height)) {
                return `${height}px`;
            }
            return height === 'true' ? true :
                height === 'false' ? false : height;
        };

        scope.getClasses = () => classNames('fd-list', {
            'fd-list--compact': scope.compact === true,
            'fd-list--byline': scope.byline === true,
            'fd-list--no-border': scope.noBorder === true,
            'fd-list--has-message': scope.hasMessage === true,
            'fd-list--dropdown': scope.dropdownMode === true,
            'fd-list--navigation fd-list--navigation-indication': scope.listType === 'navigation-indication',
            'fd-list--navigation': scope.listType === 'navigation',
            'fd-list--selection': scope.listType === 'selection',
            'fd-list__infinite-scroll': parseHeight(scope.fixedHeight),
        });

        scope.getStyles = function () {
            let height = parseHeight(scope.fixedHeight);
            if (height && typeof height === 'string') {
                return { height };
            }
        };
    },
    template: `<ul ng-class="getClasses()" ng-style="getStyles()" role="{{listType === 'selection' ? 'listbox' : 'list'}}" tabindex="-1" ng-transclude>`
})).directive('bkListItem', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        interactive: '<?',
        inactive: '<?',
        selected: '<?'
    },
    controller: ['$scope', '$element', '$attrs', function ($scope, $element, $attrs) {
        $scope.focusable = true;
        this.addClass = function (className) {
            $element.addClass(className);
        }
        this.setRole = function (role) {
            $element.attr('role', role);
        }
        this.canFocus = function (focusable) {
            $scope.focusable = focusable;
        }

        if (!$attrs.role)
            this.setRole('listitem');

        $scope.getClasses = () => {
            if ($scope.selected) {
                $element[0].setAttribute('aria-selected', 'true');
            } else {
                $element[0].removeAttribute('aria-selected');
            }
            return classNames('fd-list__item', {
                'fd-list__item--interractive': $scope.interactive === true,
                'fd-list__item--inactive': $scope.inactive === true,
                'is-selected': $scope.selected === true,
            })
        };
    }],
    template: '<li ng-class="getClasses()" tabindex="{{focusable ? 0 : -1}}" ng-transclude></li>'
})).directive('bkListTitle', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: '<span class="fd-list__title" ng-transclude></span>'
})).directive('bkListSecondary', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        type: "@?"
    },
    link: function (scope) {
        const types = ['positive', 'critical', 'negative', 'informative'];

        scope.getClasses = () => {
            if (scope.type && !types.includes(scope.type)) {
                console.error(`bk-list-secondary error: 'type' must be one of: ${types.join(', ')}`);
            }
            return classNames('fd-list__secondary', {
                [`fd-list__secondary--${scope.type}`]: scope.type && types.includes(scope.type),
            })
        };
    },
    template: '<span ng-class="getClasses()" ng-transclude></span>'
})).directive('bkListMessage', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        type: "@"
    },
    link: (scope) => {
        const types = ['success', 'error', 'warning', 'information'];
        if (scope.type && !types.includes(scope.type)) {
            console.error(`bk-list-message error: 'type' must be one of: ${types.join(', ')}`);
        }

        scope.getClasses = () => classNames('fd-list__message', {
            [`fd-list__message--${scope.type}`]: types.includes(scope.type),
        });
    },
    template: '<span ng-class="getClasses()" ng-transclude></span>'
})).directive('bkListButton', () => ({
    restrict: 'A',
    link: function (_scope, element) { element.addClass('fd-list__button') }
})).directive('bkListLink', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    require: '^^bkListItem',
    scope: {
        navigationIndicator: '<',
        navigated: '<',
        selected: '<'
    },
    link: (scope, _element, _attrs, listItemCtrl) => {
        listItemCtrl.addClass('fd-list__item--link');
        listItemCtrl.canFocus(false);

        scope.getClasses = () => classNames('fd-list__link', {
            'fd-list__link--navigation-indicator': scope.navigationIndicator === true,
            'is-navigated': scope.navigated === true,
            'is-selected': scope.selected === true,
        });
    },
    template: '<a tabindex="0" ng-class="getClasses()" ng-transclude></a>'
})).directive('bkListIcon', (classNames) => ({
    restrict: 'EA',
    replace: true,
    scope: {
        glyph: '@?',
        svgPath: '@?',
    },
    link: (scope) => {
        scope.getClasses = () => {
            if (!scope.glyph && !scope.svgPath) {
                console.error('bk-list-icon error: You must provide a glpyh or an svg icon');
            }
            return classNames('fd-list__icon', {
                [scope.glyph]: scope.glyph && !scope.svgPath,
                'bk-icon--svg sap-icon': scope.svgPath,
            })
        };
    },
    template: '<i role="presentation" ng-class="getClasses()"><ng-include ng-if="svgPath" src="svgPath"></ng-include></i>'
})).directive('bkListActionItem', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: `<li role="listitem" class="fd-list__item fd-list__item--action"><button class="fd-list__title" ng-transclude></button></li>`
})).directive('bkListFormItem', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    require: '?^^bkListItem',
    link: (_scope, _element, _attrs, listItemCtrl) => {
        if (listItemCtrl) listItemCtrl.setRole('option');
    },
    template: '<div class="fd-form-item fd-list__form-item" ng-transclude></div>'
})).directive('bkListContent', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        itemTitle: '@',
        itemTitleId: '@?',
        contentWrap: '<?',
        titleWrap: '<?'
    },
    controller: ['$scope', '$element', function ($scope, $element) {
        this.addClass = function (className) {
            $element.children().last().addClass(className);
        };

        $scope.getBylineClasses = () => classNames('fd-list__byline', {
            'fd-list__byline--wrap': $scope.contentWrap === true,
        });

        $scope.getTitleClasses = () => classNames('fd-list__title', {
            'fd-list__title--wrap': $scope.titleWrap === true,
        });

        if ($scope.itemTitleId) {
            $element.children().first().attr('id', $scope.itemTitleId);
        }
    }],
    template: `<div class="fd-list__content">
        <div ng-class="getTitleClasses()">{{itemTitle}}</div>
        <div ng-class="getBylineClasses()" ng-transclude></div>
    </div>`
})).directive('bkListGroupHeader', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: '<li role="listitem" class="fd-list__group-header" ng-transclude></li>'
})).directive('bkListByline', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        align: '@',
        contentWrap: '<?',
        semanticStatus: '@?'
    },
    require: '^^bkListContent',
    link: function (scope, _element, _attrs, ctrl) {
        ctrl.addClass('fd-list__byline--2-col');

        const semanticStatuses = ['neutral', 'positive', 'negative', 'critical', 'informative'];

        if (scope.semanticStatus && !semanticStatuses.includes(scope.semanticStatus)) {
            console.error(`bk-list-byline error: semantic-status must be one of: ${semanticStatuses.join(', ')}`);
        }

        if (scope.align !== 'left' && scope.align !== 'right') {
            console.error(`bk-list-byline error: 'align' must be 'left' or 'right' `);
        }

        scope.getClasses = () => classNames('fd-list__title', {
            'fd-list__byline-left': scope.align === 'left',
            'fd-list__byline-left--wrap': scope.align === 'left' && scope.contentWrap === true,
            'fd-list__byline-right': scope.align === 'right',
            [`fd-list__byline-right--${scope.semanticStatus}`]: scope.align === 'right' && semanticStatuses.includes(scope.semanticStatus),
        });
    },
    template: '<div ng-class="getClasses()" ng-transclude></div>'
})).directive('bkListThumbnail', (classNames) => ({
    restrict: 'EA',
    replace: true,
    scope: {
        glyph: '@?',
        imageUrl: '@?'
    },
    link: (scope) => {
        if (!scope.glyph && !scope.imageUrl) {
            console.error('bk-list-thumbnail error: You should provide either glpyh icon or image');
        }

        scope.getClasses = () => classNames('fd-list__thumbnail', {
            'fd-image--s': scope.imageUrl,
        });

        scope.getStyles = function () {
            if (scope.imageUrl) return {
                backgroundImage: `url('${scope.imageUrl}')`,
                backgroundSize: 'cover'
            }
            return {};
        };
    },
    template: '<span ng-class="getClasses()" ng-style="getStyles()"><i ng-if="glyph" role="presentation" ng-class="glyph"></i></span>'
}));