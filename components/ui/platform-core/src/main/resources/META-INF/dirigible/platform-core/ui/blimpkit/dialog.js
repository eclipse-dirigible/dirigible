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
blimpkit.directive('bkDialog', (classNames, uuid) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        visible: '<',
        windowWidth: '@?',
        windowHeight: '@?',
        windowMaxWidth: '@?',
        windowMaxHeight: '@?',
        windowMinWidth: '@?',
        windowMinHeight: '@?',
    },
    controller: ['$scope', function ($scope) {
        $scope.dialogId = `d${uuid.generate()}`;
        this.getDialogId = function () {
            return $scope.dialogId;
        };
        $scope.getClasses = () => classNames('fd-dialog', {
            'fd-dialog--active': $scope.visible === true,
        });

        $scope.getStyle = () => {
            return {
                width: $scope.windowWidth ?? undefined,
                height: $scope.windowHeight ?? undefined,
                maxWidth: $scope.windowMaxWidth ?? undefined,
                maxHeight: $scope.windowMaxHeight ?? undefined,
                minWidth: $scope.windowMinWidth ?? undefined,
                minHeight: $scope.windowMinHeight ?? undefined
            }
        };
    }],
    template: `<section ng-class="getClasses()">
        <div class="fd-dialog__content" role="dialog" aria-modal="true" aria-labelledby="{{dialogId}}" ng-style="getStyle()" ng-transclude></div>
    </section>`,
})).directive('bkDialogHeader', (classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    require: '^^bkDialog',
    scope: {
        compact: '<?',
        header: '@?',
        title: '@?',
        subheader: '@?',
    },
    link: (scope, _element, _attrs, dialogCtrl) => {
        scope.dialogId = dialogCtrl.getDialogId();
        scope.getHeaderClasses = () => classNames('fd-dialog__header fd-bar', {
            'fd-bar--header-with-subheader': scope.subheader,
            'fd-bar--compact': scope.compact === true,
        });
    },
    template: `<div><header ng-class="getHeaderClasses()">
        <div class="fd-bar__left">
            <div ng-if="header" class="fd-bar__element">{{header}}</div>
            <div class="fd-bar__element">
                <h2 class="fd-title fd-title--h5" id="{{dialogId}}">{{title}}</h2>
            </div>
        </div>
        <div class="fd-bar__right" ng-transclude></div>
    </header>
    <div ng-if="subheader" class="fd-dialog__subheader fd-bar fd-bar--subheader">
        <div class="fd-bar__left"><div class="fd-bar__element">{{subheader}}</div></div>
    </div></div>`
})).directive('bkDialogBody', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    template: '<div class="fd-dialog__body" ng-transclude></div>'
})).directive('bkDialogFooter', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: { compact: '<?' },
    template: `<footer class="fd-dialog__footer fd-bar fd-bar--footer{{compact === true ? ' fd-bar--compact' : ''}}"><div class="fd-bar__right" ng-transclude></div></footer>`
}));