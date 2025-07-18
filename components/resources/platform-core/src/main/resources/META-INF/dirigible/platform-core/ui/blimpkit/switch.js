/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
blimpkit.directive('bkSwitch', (classNames) => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: {
        semantic: '<?',
        withText: '<?',
        compact: '<?',
    },
    controller: ['$scope', function ($scope) {
        $scope.getClasses = () => classNames('fd-switch', {
            'fd-switch--semantic': $scope.semantic,
            'fd-switch--text': $scope.withText,
            'is-compact': $scope.compact,
            'is-disabled': $scope.disabled === true,
        });
        this.isWithText = function () {
            return $scope.withText === true;
        };
        this.setDisabled = function (value) {
            $scope.disabled = value;
        };
    }],
    template: '<label ng-class="getClasses()"><span class="fd-switch__control" ng-transclude></span></label>',
})).directive('bkSwitchSlider', () => ({
    restrict: 'E',
    replace: true,
    require: '^^bkSwitch',
    link: (scope, _element, _attrs, switchCtrl) => {
        scope.withText = switchCtrl.isWithText;
    },
    template: `<div class="fd-switch__slider">
        <div class="fd-switch__track">
            <span ng-if="withText()" class="fd-switch__text fd-switch__text--on">on</span>
            <i ng-if="!withText()" role="presentation" class="fd-switch__icon fd-switch__icon--on sap-icon--accept"></i>
            <span class="fd-switch__handle" role="presentation"></span>
            <i ng-if="!withText()" role="presentation" class="fd-switch__icon fd-switch__icon--off sap-icon--less"></i>
            <span ng-if="withText()" class="fd-switch__text fd-switch__text--off">off</span>
        </div>
    </div>`,
})).directive('bkSwitchInput', () => ({
    restrict: 'A',
    require: '^^bkSwitch',
    link: (_scope, element, attrs, switchCtrl) => {
        if (Object.prototype.hasOwnProperty.call(attrs, 'disabled') && (attrs.disabled === 'true' || attrs.disabled === '')) {
            switchCtrl.setDisabled(true);
        }
        attrs.$observe('disabled', (value) => {
            switchCtrl.setDisabled(value);
        });
        element.addClass('fd-switch__input');
        if (!Object.prototype.hasOwnProperty.call(attrs, 'type')) console.error('bk-switch-input: "type" must be set to "checkbox".');
        if (!Object.prototype.hasOwnProperty.call(attrs, 'ariaLabelledby')) console.error('bk-switch-input: "aria-labelledby" must be provided.');
    },
}));