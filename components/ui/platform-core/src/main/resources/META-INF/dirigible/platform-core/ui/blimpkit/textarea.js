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
blimpkit.directive('bkTextarea', (classNames) => ({
    restrict: 'E',
    transclude: true,
    require: '?^^bkFormInputMessage',
    replace: true,
    scope: {
        compact: '<?',
        state: '@?',
    },
    link: (scope, _element, attrs, ctrl) => {
        const states = {
            'error': 'error',
            'success': 'success',
            'warning': 'warning',
            'information': 'information'
        };
        scope.getClasses = () => {
            if (ctrl) ctrl.setReadonly(Object.prototype.hasOwnProperty.call(attrs, 'readonly'));
            return classNames('fd-textarea', {
                'fd-textarea--compact': scope.compact === true,
                'is-disabled': Object.prototype.hasOwnProperty.call(attrs, 'disabled') && attrs.disabled === true,
                [`is-${states[scope.state]}`]: scope.state && states[scope.state] && !Object.prototype.hasOwnProperty.call(attrs, 'readonly'),
            });
        };
    },
    template: '<textarea ng-class="getClasses()" ng-transclude></textarea>',
}));