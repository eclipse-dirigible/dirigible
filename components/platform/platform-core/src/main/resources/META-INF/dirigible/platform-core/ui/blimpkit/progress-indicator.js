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
blimpkit.directive('bkProgressIndicator', (classNames) => ({
    restrict: 'E',
    transclude: false,
    replace: true,
    scope: {
        minValue: '<?',
        maxValue: '<?',
        currentValue: '<',
        label: '@?',
        state: '@?',
    },
    link: (scope, _element, attrs) => {
        if (!Object.prototype.hasOwnProperty.call(attrs, 'ariaLabel'))
            console.error('bk-progress-indicator error: You should provide a description using the "aria-label" attribute');
        scope.getClasses = () => classNames('fd-progress-indicator', {
            'fd-progress-indicator--informative': scope.state === 'informative',
            'fd-progress-indicator--positive': scope.state === 'positive',
            'fd-progress-indicator--critical': scope.state === 'critical',
            'fd-progress-indicator--negative': scope.state === 'negative',
        });
    },
    template: `<div ng-class="getClasses()" tabindex="-1" role="progressbar" aria-valuemin="{{minValue || 0}}" aria-valuenow="{{currentValue}}" aria-valuemax="{{minValue || 100}}" aria-valuetext="{{currentValue}}%">
        <div class="fd-progress-indicator__container">
            <div class="fd-progress-indicator__progress-bar" style="min-width: {{currentValue}}%; width: {{currentValue}}%;"></div>
            <div class="fd-progress-indicator__remaining"><span class="fd-progress-indicator__label">{{label || currentValue + '%'}}</span></div>
        </div>
    </div>`
}))