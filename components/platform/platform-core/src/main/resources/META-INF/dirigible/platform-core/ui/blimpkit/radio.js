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
blimpkit.directive('bkRadio', (classNames) => ({
    restrict: 'E',
    transclude: false,
    replace: true,
    scope: {
        compact: '<?',
        state: '@?',
    },
    states: {
        'error': 'error',
        'success': 'success',
        'warning': 'warning',
        'information': 'information'
    },
    link: function (scope, _elem, attrs) {
        scope.getClasses = () => classNames({
            'fd-radio--compact': scope.compact === true,
            'is-disabled': Object.prototype.hasOwnProperty.call(attrs, 'disabled') && attrs.disabled === true,
            'is-readonly': Object.prototype.hasOwnProperty.call(attrs, 'readonly') && attrs.readonly === true,
            [`is-${this.states[scope.state]}`]: scope.state && this.states[scope.state],
        });
    },
    template: '<input type="radio" class="fd-radio" ng-class="getClasses()">',
})).directive('bkRadioLabel', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        wrap: '<?',
        topAlign: '<?',
    },
    link: (scope) => {
        scope.getClasses = () => classNames({
            'fd-radio__label--wrap': scope.wrap === true,
            'fd-radio__label--wrap-top-aligned': scope.topAlign === true,
        });
    },
    template: '<label class="fd-radio__label" ng-class="getClasses()"><span class="fd-radio__text" ng-transclude></span></label>',
}));