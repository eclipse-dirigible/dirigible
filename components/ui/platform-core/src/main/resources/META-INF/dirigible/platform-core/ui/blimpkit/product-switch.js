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
blimpkit.directive('bkProductSwitch', (classNames, $injector, ButtonStates) => {
    if (!$injector.has('bkPopoverDirective') || !$injector.has('bkButtonDirective')) {
        console.error('bk-product-switch requires the bk-button and bk-popover widgets to be loaded.');
        return {};
    }
    return {
        restrict: 'E',
        transclude: true,
        replace: true,
        scope: {
            btnAriaLabel: '@?',
            align: '@?',
            size: '@?',
            state: '@?',
            noArrow: '<?',
        },
        link: function (scope) {
            if (!scope.btnAriaLabel)
                console.error('bk-product-switch error: Must have the "btn-aria-label" attribute');
            scope.getClasses = () => classNames('fd-product-switch__body', {
                'fd-product-switch__body--col-3': scope.size === 'medium',
                'fd-product-switch__body--mobile': scope.size === 'small',
            });
        },
        template: `<div class="fd-product-switch"><bk-popover>
            <bk-popover-control>
                <bk-button state="{{ state || '${ButtonStates.Transparent}' }}" glyph="sap-icon--grid" aria-label="{{btnAriaLabel}}">
                </bk-button>
            </bk-popover-control>
            <bk-popover-body align="{{ align || 'bottom-right' }}" no-arrow="noArrow">
                <div ng-class="getClasses()">
                    <ul class="fd-product-switch__list" ng-transclude></ul>
                </div>
            </bk-popover-body>
        </bk-popover></div`
    }
}).directive('bkProductSwitchItem', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        selected: '<?',
        title: '@',
        subtitle: '@?',
        glyph: '@?',
        iconSrc: '@?',
    },
    link: function (scope) {
        scope.getClasses = () => classNames('fd-product-switch__item', {
            'selected': scope.selected,
        });
    },
    template: `<li ng-class="getClasses()" tabindex="0">
        <i ng-if="glyph" class="fd-product-switch__icon" ng-class="glyph" role="presentation"></i>
        <div ng-if="iconSrc" class="fd-product-switch__icon sap-icon bk-center"><img ng-src="{{iconSrc}}" alt="{{title}}" width="24" height="24"></div>
        <div class="fd-product-switch__text">
            <div class="fd-product-switch__title">{{title}}</div>
            <div ng-if="subtitle" class="fd-product-switch__subtitle">{{subtitle}}</div>
        </div>
    </li>`
}));