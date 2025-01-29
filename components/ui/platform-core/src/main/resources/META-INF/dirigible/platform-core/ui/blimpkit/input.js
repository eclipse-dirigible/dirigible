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
blimpkit.directive('bkInput', (classNames) => ({
    restrict: 'E',
    transclude: false,
    require: ['?^^bkInputGroup', '?^^bkTokenizer'],
    replace: true,
    scope: {
        compact: '<?',
        isHover: '<?',
        state: '@?',
    },
    states: {
        'error': 'error',
        'success': 'success',
        'warning': 'warning',
        'information': 'information'
    },
    forbiddenTypes: ['checkbox', 'radio', 'file', 'image', 'range'],
    link: function (scope, _element, attrs, ctrl) {
        if (!Object.prototype.hasOwnProperty.call(attrs, 'type'))
            console.error('bk-input error: Inputs must have the "type" HTML attribute');
        else {
            if (this.forbiddenTypes.includes(attrs.type))
                console.error('bk-input error: Invalid input type. Possible options are "color", "date", "datetime-local", "email", "hidden", "month", "password", "search", "tel", "text", "time", "url" and "week".');
        }
        scope.getClasses = () => {
            if (ctrl[0]) {
                if (Object.prototype.hasOwnProperty.call(attrs, 'disabled') && attrs.disabled === true) ctrl[0].setDisabled(true);
                else ctrl[0].setDisabled(false);
            }
            return classNames({
                'fd-input--compact': scope.compact === true,
                'fd-input-group__input': ctrl[0],
                'fd-tokenizer__input': ctrl[1],
                'is-hover': scope.isHover === true,
                [`is-${this.states[scope.state]}`]: scope.state && this.states[scope.state],
            })
        };
    },
    template: '<input class="fd-input" ng-class="getClasses()" />',
})).directive('bkInputGroup', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        compact: '<?',
        focus: '<?',
        isDisabled: '<?',
        state: '@?',
        isReadonly: '<?',
    },
    controller: ['$scope', '$attrs', function ($scope, $attrs) {
        const states = {
            'error': 'error',
            'success': 'success',
            'warning': 'warning',
            'information': 'information'
        };
        $scope.disabled = false;
        $scope.getClasses = () => classNames({
            'fd-input--compact': $scope.compact === true,
            'is-hover': $scope.isHover === true,
            'is-focus': $scope.focus === true,
            'is-readonly': $scope.isReadonly === true,
            'is-disabled': $scope.isDisabled || (Object.prototype.hasOwnProperty.call($attrs, 'disabled') && $attrs.disabled === true),
            [`is-${states[$scope.state]}`]: $scope.state && states[$scope.state]
        });

        this.setDisabled = function (disabled) {
            $scope.disabled = disabled;
        };
    }],
    template: '<div class="fd-input-group" ng-class="getClasses()" tabindex="-1" ng-transclude></div>',
})).directive('bkInputGroupAddon', () => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    controller: ['$scope', function ($scope) {
        $scope.hasButton = false;
        $scope.getClasses = () => $scope.hasButton === true ? 'fd-input-group__addon--button' : undefined;
        this.setButtonAddon = function (hasButton) {
            $scope.hasButton = hasButton;
        };
    }],
    template: '<span class="fd-input-group__addon" ng-class="getClasses()" ng-transclude></span>',
}));