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
blimpkit.directive('bkPanel', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        expanded: '=',
        fixed: '<?',
        compact: '<?',
        expandedChange: '&?'
    },
    controller: ['$scope', 'classNames', function ($scope, classNames) {
        $scope.expanded = !!$scope.expanded;

        this.isFixed = () => $scope.fixed;
        this.isExpanded = () => $scope.expanded;
        this.isCompact = () => $scope.compact;
        this.getContentId = () => $scope.contentId;
        this.getTitleId = () => $scope.titleId;

        this.contentExpandedEvent = angular.noop;

        this.setContentId = (id) => {
            $scope.contentId = id;
        }

        this.setTitleId = (id) => {
            $scope.titleId = id;
        }

        this.toggleExpanded = function () {
            $scope.expanded = !$scope.expanded;

            this.contentExpandedEvent();

            if ($scope.expandedChange) {
                $scope.expandedChange({ expanded: $scope.expanded });
            }
        }

        $scope.getClasses = () => classNames('fd-panel', {
            'fd-panel--compact': $scope.compact === true,
            'fd-panel--fixed': $scope.fixed === true,
        });
    }],
    template: '<div ng-class="getClasses()" ng-transclude></div>'
})).directive('bkPanelHeader', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: '<div class="fd-panel__header" ng-transclude></div>'
})).directive('bkPanelExpand', ($injector, ButtonStates) => {
    if (!$injector.has('bkButtonDirective')) {
        console.error('bk-panel-expand requires the bk-button widget to be loaded.');
        return {};
    }
    return {
        restrict: 'EA',
        transclude: true,
        replace: true,
        require: '^^bkPanel',
        scope: {
            hint: '@',
        },
        link: (scope, _element, _attrs, panelCtrl) => {
            if (!scope.hint) console.error('bk-panel-expand: You must provide a value for the "hint" attribute');
            scope.isFixed = () => panelCtrl.isFixed();
            scope.isCompact = () => panelCtrl.isCompact();
            scope.isExpanded = () => panelCtrl.isExpanded();
            scope.getContentId = () => panelCtrl.getContentId();
            scope.getTitleId = () => panelCtrl.getTitleId();
            scope.toggleExpanded = function () {
                panelCtrl.toggleExpanded();
            };
            scope.getExpandButtonIcon = () => panelCtrl.isExpanded() ? 'sap-icon--slim-arrow-down' : 'sap-icon--slim-arrow-right';
        },
        template: `<div ng-show="!isFixed()" class="fd-panel__expand">
        <bk-button ng-click="toggleExpanded()" glyph="{{ getExpandButtonIcon() }}" state="${ButtonStates.Transparent}" compact="isCompact() || false" class="fd-panel__button"
            aria-haspopup="true" aria-expanded="{{ isExpanded() }}" aria-controls="{{ getContentId() }}" aria-labelledby="{{ getTitleId() }}"
            aria-label="{{hint}}" title="{{hint}}"></bk-button>
        </div>`
    }
}).directive('bkPanelTitle', (uuid) => ({
    restrict: 'A',
    require: '^^bkPanel',
    link: (_scope, element, attrs, panelCtrl) => {
        element.addClass('fd-panel__title');

        let id = attrs.id;
        if (!id) {
            id = `pt-${uuid.generate()}`;
            element[0].setAttribute('id', id);
        }

        panelCtrl.setTitleId(id);
    }
})).directive('bkPanelContent', (uuid) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    require: '^bkPanel',
    link: (scope, element, attrs, panelCtrl) => {
        scope.isHidden = () => !panelCtrl.isFixed() && !panelCtrl.isExpanded();

        let id = attrs.id;
        if (!id) {
            id = `ph-${uuid.generate()}`;
            element[0].setAttribute('id', id);
        }

        panelCtrl.setContentId(id);
    },
    template: '<div role="region" class="fd-panel__content" aria-hidden="{{ isHidden() }}" ng-transclude></div>'
})).directive('bkPanelContent', (uuid) => ({
    restrict: 'A',
    require: '^bkPanel',
    link: (_scope, element, attrs, panelCtrl) => {
        element[0].setAttribute('role', 'region');
        element[0].setAttribute('aria-hidden', !panelCtrl.isFixed() && !panelCtrl.isExpanded());
        element[0].classList.add('fd-panel__content');

        let id = attrs.id;
        if (!id) {
            id = `ph-${uuid.generate()}`;
            element[0].setAttribute('id', id);
        }

        panelCtrl.setContentId(id);
        panelCtrl.contentExpandedEvent = () => {
            element[0].setAttribute('aria-hidden', !panelCtrl.isExpanded());
        };
    },
}));