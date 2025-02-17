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
blimpkit.directive('bkSwitch', (classNames) => ({
    restrict: 'E',
    replace: true,
    scope: {
        switchId: '@?',
        name: '@?',
        semantic: '<?',
        withText: '<?',
        switchLabelledby: '@',
        bindModel: '=?ngModel',
        bindChange: '=?ngChange',
    },
    link: (scope, _element, attrs) => {
        if (!scope.switchLabelledby) console.error('bk-switch: "switch-labelledby" must be provided.')
        scope.getClasses = () => classNames('fd-switch', {
            'fd-switch--semantic': scope.semantic,
            'fd-switch--text': scope.withText,
            'is-disabled': Object.prototype.hasOwnProperty.call(attrs, 'disabled') && (attrs.disabled === 'true' || attrs.disabled === '')
        });
    },
    template: `<label ng-class="getClasses()">
        <span class="fd-switch__control">
            <input ng-attr-id="{{switchId}}" class="fd-switch__input" ng-attr-name="{{name}}" aria-labelledby="{{switchLabelledby}}" type="checkbox" ng-model="bindModel" ng-change="bindChange">
            <div class="fd-switch__slider">
                <div class="fd-switch__track">
                    <span ng-if="withText" class="fd-switch__text fd-switch__text--on">on</span>
                    <i ng-if="!withText" role="presentation" class="fd-switch__icon fd-switch__icon--on sap-icon--accept"></i>
                    <span class="fd-switch__handle" role="presentation"></span>
                    <i ng-if="!withText" role="presentation" class="fd-switch__icon fd-switch__icon--off sap-icon--less"></i>
                    <span ng-if="withText" class="fd-switch__text fd-switch__text--off">off</span>
                </div>
            </div>
        </span>
    </label>`,
}));