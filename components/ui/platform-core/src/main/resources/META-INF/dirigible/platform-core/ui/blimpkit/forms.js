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
blimpkit.directive('bkFieldset', () => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: { label: '@?' },
    template: `<fieldset class="fd-fieldset">
        <legend ng-if="label" class="fd-fieldset__legend">{{ label }}</legend>
        <ng-transclude></ng-transclude>
    </fieldset>`,
})).directive('bkFormGroup', (uuid) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        inline: '<?',
        label: '@?',
        compact: '<?',
        transcludeClasses: '@?',
    },
    link: {
        pre: (scope) => {
            if (scope.label) scope.headerId = `fgh${uuid.generate()}`;
        }
    },
    template: `<div class="fd-form-group" ng-class="{'true': 'fd-form-group--inline'}[inline]" role="group" ng-attr-aria-labelledby="{{headerId}}">
        <bk-form-group-header ng-if="label" header-id="{{ headerId }}" compact="compact">{{ label }}</bk-form-group-header>
        <ng-transclude class="{{transcludeClasses}}"></ng-transclude>
    </div>`,
})).directive('bkFormGroupHeader', () => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        compact: '<?',
        headerId: '@'
    },
    template: `<div class="fd-form-group__header" ng-class="{'true': 'fd-form-group__header--compact'}[compact]"><h1 id="{{ headerId }}" class="fd-form-group__header-text" ng-transclude></h1></div>`,
})).directive('bkFormItem', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        horizontal: '<?',
        inList: '<?',
    },
    link: (scope) => {
        scope.getClasses = () => classNames({
            'fd-form-item--horizontal': scope.horizontal === true,
            'bk-form-item--horizontal': scope.horizontal === true, // see widgets.css
            'fd-list__form-item': scope.inList === true,
        });
    },
    template: '<div class="fd-form-item" ng-class="getClasses()" ng-transclude></div>',
})).directive('bkFormLabel', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: { colon: '<?' },
    link: (scope, _elem, attrs) => {
        scope.getClasses = () => classNames({
            'fd-form-label--colon': scope.colon === true,
            'fd-form-label--required': Object.prototype.hasOwnProperty.call(attrs, 'required') && (attrs.required === 'true' || attrs.required === ''),
        });
    },
    template: '<label class="fd-form-label" ng-class="getClasses()" ng-transclude></label>',
})).directive('bkFormHeader', () => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    template: '<div class="fd-form-header"><span class="fd-form-header__text" ng-transclude></span></div>',
})).directive('bkFormInputMessage', (uuid, classNames) => ({
    restrict: 'E',
    transclude: true,
    scope: {
        state: '@?',
        message: '<?',
        messageFixed: '<?',
    },
    replace: true,
    link: (scope, element) => {
        scope.expanded = false;
        scope.popoverId = `fim${uuid.generate()}`;
        scope.getClasses = () => classNames({
            [`fd-form-message--${scope.state}`]: scope.state,
        });
        scope.getStyle = () => {
            if (scope.messageFixed === true) {
                const pos = element[0].getBoundingClientRect();
                return {
                    transition: 'none',
                    transform: 'none',
                    position: 'fixed',
                    top: `${pos.bottom}px`,
                    left: `${pos.left}px`,
                };
            } else return {};
        };
        function focusoutEvent() {
            if (scope.state && scope.expanded) scope.$apply(() => scope.expanded = false);
        }
        function focusinEvent() {
            if (scope.state && !scope.expanded) scope.$apply(() => scope.expanded = true);
        }
        function inputChange() {
            if (scope.state === 'error' && !scope.expanded) scope.$apply(() => scope.expanded = true);
            else if (!scope.state && scope.expanded) scope.$apply(() => scope.expanded = false);
        }
        element.on('focusout', focusoutEvent);
        element.on('focusin', focusinEvent);
        element.on('input', inputChange);
        function cleanUp() {
            element.off('focusout', focusoutEvent);
            element.off('focusin', focusinEvent);
            element.off('input', inputChange);
        }
        scope.$on('$destroy', cleanUp);
    },
    template: `<div class="fd-popover fd-form-input-message-group" tabindex="-1">
        <div class="fd-popover__control" aria-controls="{{ popoverId }}" aria-expanded="{{expanded}}" aria-haspopup="true" tabindex="-1" ng-transclude></div>
        <div id="{{ popoverId }}" class="fd-popover__body fd-popover__body--no-arrow fd-popover__body--input-message-group" aria-hidden="{{!expanded}}" ng-style="getStyle()">
            <div class="fd-form-message" ng-class="getClasses()">{{message}}</div>
        </div>
    </div>`,
}));