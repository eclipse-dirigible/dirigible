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
blimpkit.directive('bkCheckbox', (classNames) => ({
    restrict: 'E',
    transclude: false,
    replace: true,
    scope: {
        compact: '<?',
        state: '@?',
        indeterminate: '<?',
        displayMode: '<?',
    },
    link: (scope, elem, attrs) => {
        let keyListener = false;
        function preventChange(event) {
            event.preventDefault();
        }
        scope.getClasses = () => {
            if (!keyListener && elem[0].readOnly) {
                elem[0].addEventListener('keyup', preventChange);
                keyListener = true;
            } else if (keyListener && !elem[0].readOnly) {
                elem[0].removeEventListener('keyup', preventChange);
                keyListener = false;
            }
            if (scope.indeterminate === true) elem[0].indeterminate = true;
            else elem[0].indeterminate = false;
            return classNames({
                [`is-${scope.state}`]: scope.state,
                'fd-checkbox--compact': scope.compact,
                'is-disabled': attrs.disabled,
                'is-display': attrs.displayMode,
            });
        };
        scope.$on('$destroy', () => {
            elem[0].removeEventListener('keyup', preventChange);
        });
    },
    template: '<input type="checkbox" class="fd-checkbox" ng-class="getClasses()" ng-attr-tabindex="{{ displayMode === true ? -1 : undefined}}">',
})).directive('bkCheckboxLabel', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        compact: '<?',
        isHover: '<?',
        empty: '<?',
        wrap: '<?',
    },
    link: (scope, _elem, attrs) => {
        scope.getClasses = () => classNames({
            'fd-checkbox__label--compact': scope.compact,
            'fd-checkbox__label--required': Object.prototype.hasOwnProperty.call(attrs, 'required') && (attrs.required === 'true' || attrs.required === ''),
            'is-hover': scope.isHover,
            'fd-checkbox__label--wrap': scope.wrap,
        });
    },
    template: `<label class="fd-checkbox__label" ng-class="getClasses()">
        <span class="fd-checkbox__checkmark" aria-hidden="true"></span>
        <div ng-if="empty !== true" class="fd-checkbox__label-container"><span class="fd-checkbox__text" ng-transclude></span></div>
    </label>`,
}));