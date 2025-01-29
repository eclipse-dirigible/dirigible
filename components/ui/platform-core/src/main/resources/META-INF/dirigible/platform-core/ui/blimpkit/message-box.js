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
blimpkit.directive('bkMessageBox', (classNames) => ({
    restrict: 'E',
    transclude: {
        footer: 'bkMessageBoxFooter'
    },
    replace: true,
    scope: {
        visible: '<',
        title: '@',
        type: '@?',
    },
    controller: ['$scope', function ($scope) {
        const types = ['confirmation', 'error', 'success', 'warning', 'information'];
        $scope.getClasses = () => classNames('fd-message-box', {
            'fd-message-box--active': $scope.visible === true,
            [`fd-message-box--${$scope.type}`]: $scope.type && types.includes($scope.type),
        });
        $scope.getIconClass = () => classNames({
            'sap-icon--sys-help-2': $scope.type === types[0],
            'sap-icon--error': $scope.type === types[1],
            'sap-icon--sys-enter-2': $scope.type === types[2],
            'sap-icon--alert': $scope.type === types[3],
            'sap-icon--information': $scope.type === types[4],
        });
    }],
    template: `<div ng-class="getClasses()"><section class="fd-message-box__content">
        <header class="fd-bar fd-bar--header fd-message-box__header"><div class="fd-bar__left"><div class="fd-bar__element">
            <i ng-if="type" ng-class="getIconClass()"></i>
            <h2 class="fd-title fd-title--h5">{{title}}</h2>
        </div></div></header>
        <div class="fd-message-box__body" ng-transclude></div>
        <ng-transclude ng-transclude-slot="footer"></ng-transclude>
    </section></div>`,
})).directive('bkMessageBoxFooter', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: { compact: '<?' },
    template: `<footer class="fd-bar fd-bar--footer fd-message-box__footer{{compact === true ? ' fd-bar--compact' : ''}}"><div class="fd-bar__right" ng-transclude></div></footer>`
}));