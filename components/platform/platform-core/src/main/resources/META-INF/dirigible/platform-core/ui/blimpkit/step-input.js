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
blimpkit.directive('bkStepInput', (classNames) => ({
    restrict: 'E',
    transclude: false,
    replace: true,
    require: '?ngModel',
    scope: {
        compact: '<?',
        inputId: '@',
        min: '=?',
        max: '=?',
        step: '=?',
        placeholder: '@?',
        name: '@?',
        state: '@?',
        isFocus: '<?',
        isReadonly: '<?',
    },
    link: (scope, element, attrs, ngModel) => {
        if (!scope.inputId)
            console.error('bk-step-input error: You must provide an ID using the "input-id" attribute');
        const input = element[0].querySelector(`input`);
        scope.model = { value: undefined };
        let valueWatch;
        if (ngModel) {
            valueWatch = scope.$watch('model.value', (value) => {
                ngModel.$setViewValue(value);
                ngModel.$validate();
            });
            ngModel.$render = () => {
                scope.model.value = ngModel.$viewValue;
            }
        }
        scope.getInputClasses = () => {
            if (scope.compact === true) return 'fd-input--compact';
        };
        scope.getButtonClasses = () => {
            if (scope.compact === true) return 'fd-button--compact';
        };
        scope.getClasses = () => classNames({
            'fd-step-input--compact': scope.compact === true,
            'is-disabled': Object.prototype.hasOwnProperty.call(attrs, 'disabled') && attrs.disabled === true,
            'is-readonly': scope.isReadonly === true,
            'is-focus': scope.isFocus === true,
            [`is-${scope.state}`]: scope.state,
        });
        scope.stepDown = () => {
            input.stepDown();
            scope.model.value = Number(input.value);
        };
        scope.stepUp = () => {
            input.stepUp();
            scope.model.value = Number(input.value);
        };
        scope.$on('$destroy', () => {
            if (ngModel) valueWatch();
        });
    },
    template: `<div class="fd-step-input" ng-class="getClasses()"><button aria-label="Step down" class="fd-button fd-button--transparent fd-step-input__button" ng-class="getButtonClasses()" tabindex="-1" type="button" ng-click="stepDown()"><i class="sap-icon--less"></i></button>
<input ng-attr-id="{{inputId}}" class="fd-input fd-input--no-number-spinner fd-step-input__input" ng-class="getClasses(true)" type="number" ng-attr-name="{{name}}" placeholder="{{placeholder}}" ng-model="model.value" ng-attr-max="{{max}}" ng-attr-min="{{min}}" ng-attr-step="{{step}}" ng-readonly="isReadonly === true"/>
<button aria-label="Step up" class="fd-button fd-button--transparent fd-step-input__button" ng-class="getButtonClasses()" tabindex="-1" type="button" ng-click="stepUp()"><i class="sap-icon--add"></i></button></div>`,
}));