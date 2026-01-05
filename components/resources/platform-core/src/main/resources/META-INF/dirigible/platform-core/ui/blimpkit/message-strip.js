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
blimpkit.directive('bkMessageStrip', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        id: '@?',
        glyph: '@?',
        state: '@?',
        onDismiss: '&?',
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-message-strip', {
            'fd-message-strip--no-icon': !scope.glyph,
            'fd-message-strip--dismissible': scope.onDismiss,
            [`fd-message-strip--${scope.state}`]: scope.state,
        });
    },
    template: `<div ng-class="getClasses()" role="note" aria-live="assertive" ng-attr-aria-labelledby="{{id}}">
<div ng-if="glyph" class="fd-message-strip__icon-container" aria-hidden="true"><span class="sap-icon {{glyph}}" focusable="false" role="presentation" aria-hidden="true"></span></div>
<p class="fd-message-strip__text" ng-transclude></p>
<bk-button ng-if="onDismiss" in-msg-strip="true" state="transparent" glyph="sap-icon--decline" compact="true" ng-attr-aria-controls="{{id}}" aria-label="Close" title="Close" ng-click="onDismiss()"></bk-button>
</div>`,
}));