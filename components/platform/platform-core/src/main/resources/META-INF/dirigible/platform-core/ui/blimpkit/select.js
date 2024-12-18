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
blimpkit.directive('bkSelect', (uuid, $window, $timeout, ScreenEdgeMargin, classNames) => ({
    restrict: 'EA',
    replace: true,
    transclude: true,
    require: '?ngModel',
    scope: {
        size: '@?',
        compact: '<?',
        isDisabled: '<?',
        selectedValue: '=?',
        state: '@?',
        message: '<?',
        placeholder: '@?',
        dropdownFill: '<?',
        labelId: '@',
        dropdownFixed: '<?',
        placement: '@?',
        isReadonly: '<?',
    },
    link: function (scope, _element, _attrs, ngModel) {
        let selectedValWatch;
        if (ngModel) {
            selectedValWatch = scope.$watch('selectedValue', (value) => {
                ngModel.$setViewValue(value);
                ngModel.$validate();
            });

            ngModel.$render = () => {
                scope.selectedValue = ngModel.$viewValue;
            };

            scope.$on('$destroy', () => {
                selectedValWatch();
            });
        }
    },
    controller: ['$scope', '$element', function ($scope, $element) {
        let control = $element[0].querySelector(`.fd-popover__control`);
        let rect;
        $scope.iconClass = 'sap-icon--slim-arrow-down';
        $scope.setDefault = function () {
            rect = control.getBoundingClientRect();
            $scope.defaultHeight = $window.innerHeight - ScreenEdgeMargin.FULL - rect.bottom;
        };
        function resizeEvent() {
            $scope.$apply(function () { $scope.setDefault() });
        }
        $window.addEventListener('resize', resizeEvent);
        $scope.defaultHeight = 16;
        $scope.items = [];
        $scope.bodyExpanded = false;
        $scope.buttonId = `select-btn-${uuid.generate()}`;
        $scope.textId = `select-text-${uuid.generate()}`;
        $scope.bodyId = `select-body-${uuid.generate()}`;

        const states = ['success', 'error', 'warning', 'information'];
        if ($scope.state && !states.includes($scope.state)) {
            console.error(`fd-select error: 'state' must be one of: ${states.join(', ')}`);
        }

        $scope.getClasses = () => classNames('fd-select', {
            'fd-select--compact': $scope.compact === true,
        });

        $scope.getControlClasses = () => classNames('fd-select__control', {
            'is-readonly': $scope.isReadonly === true,
            'is-disabled': $scope.isDisabled === true,
            [`is-${$scope.state}`]: $scope.state,
        });

        $scope.getPopoverBodyClasses = function () {
            let classList = ['fd-popover__body', 'fd-popover__body--no-arrow', 'fd-popover__body--dropdown', 'fd-scrollbar'];
            if ($scope.dropdownFill) {
                classList.push('fd-popover__body--dropdown-fill');
            }
            if ($scope.placement && $scope.dropdownFixed !== true) {
                switch ($scope.placement) {
                    case 'bottom':
                        classList.push('fd-popover__body--center');
                        $scope.iconClass = 'sap-icon--slim-arrow-down';
                        break;
                    case 'bottom-end':
                        classList.push('fd-popover__body--right');
                        $scope.iconClass = 'sap-icon--slim-arrow-down';
                        break;
                    case 'top':
                        classList.push('fd-popover__body--above fd-popover__body--center');
                        $scope.iconClass = 'sap-icon--slim-arrow-up';
                        break;
                    case 'top-start':
                        classList.push('fd-popover__body--above');
                        $scope.iconClass = 'sap-icon--slim-arrow-up';
                        break;
                    case 'top-end':
                        classList.push('fd-popover__body--above fd-popover__body--right');
                        $scope.iconClass = 'sap-icon--slim-arrow-up';
                        break;
                    case 'right':
                        classList.push('fd-popover__body--after fd-popover__body--middle');
                        $scope.iconClass = 'sap-icon--slim-arrow-right';
                        break;
                    case 'right-start':
                        classList.push('fd-popover__body--after');
                        $scope.iconClass = 'sap-icon--slim-arrow-right';
                        break;
                    case 'right-end':
                        classList.push('fd-popover__body--after fd-popover__body--bottom');
                        $scope.iconClass = 'sap-icon--slim-arrow-right';
                        break;
                    case 'left':
                        classList.push('fd-popover__body--before fd-popover__body--middle');
                        $scope.iconClass = 'sap-icon--slim-arrow-left';
                        break;
                    case 'left-start':
                        classList.push('fd-popover__body--before');
                        $scope.iconClass = 'sap-icon--slim-arrow-left';
                        break;
                    case 'left-end':
                        classList.push('fd-popover__body--before fd-popover__body--bottom');
                        $scope.iconClass = 'sap-icon--slim-arrow-left';
                        break;
                    default:
                        $scope.iconClass = 'sap-icon--slim-arrow-down';
                        break;
                }
            }
            return classList.join(' ');
        };

        $scope.getListClasses = () => classNames('fd-list', 'fd-list--dropdown', {
            'fd-list--has-message': $scope.message,
            'fd-list--compact': $scope.compact === true,
            'fd-list--large-dropdown': $scope.size === 'large'
        });

        $scope.getListMessageClasses = () => classNames('fd-list__message', {
            [`fd-list__message--${$scope.state}`]: $scope.state,
        });

        $scope.getFormMessageClasses = () => classNames('fd-form-message', 'fd-form-message--static', {
            [`fd-form-message--${$scope.state}`]: $scope.state,
        });

        $scope.onControlClick = function ($event) {
            $scope.setDefault();
            $scope.bodyExpanded = !$scope.bodyExpanded;
            $event.currentTarget.focus();
        };

        $scope.closeDropdown = function () {
            $scope.bodyExpanded = false;
        };

        $scope.getSelectedItem = function () {
            if ($scope.selectedValue === undefined || $scope.selectedValue === null)
                return null;

            let index = $scope.items.findIndex(x => x.value === $scope.selectedValue);
            return index >= 0 ? $scope.items[index] : null;
        };

        $scope.getSelectedItemText = function () {
            const selectedItem = $scope.getSelectedItem();
            return selectedItem ? selectedItem.text : $scope.placeholder || '';
        };

        $scope.getSelectedItemId = function () {
            const selectedItem = $scope.getSelectedItem();
            return selectedItem ? selectedItem.optionId : '';
        };

        $scope.setMessageVisibility = (visible) => {
            $scope.messageVisibile = visible;
        };

        this.addItem = function (item) {
            $scope.items.push(item);
        }

        this.removeItem = function (item) {
            let index = $scope.items.findIndex(x => x.optionId === item.optionId);
            if (index >= 0)
                $scope.items.splice(index, 1);
        }

        this.getSelectedValue = function () {
            return $scope.selectedValue;
        }

        this.selectItem = function (item) {
            $scope.selectedValue = item.value;
            $scope.closeDropdown();
        }

        $scope.getStyle = function () {
            if ($scope.dropdownFixed === true && rect !== undefined) {
                rect = control.getBoundingClientRect();
                if ($scope.defaultHeight < ScreenEdgeMargin.QUADRUPLE) {
                    return {
                        transition: 'none',
                        transform: 'none',
                        position: 'fixed',
                        top: 'auto',
                        bottom: `${$window.innerHeight - rect.top}px`,
                        left: `${rect.left}px`,
                        'max-height': `${rect.top - ScreenEdgeMargin.FULL}px`,
                    };
                }
                return {
                    transition: 'none',
                    transform: 'none',
                    position: 'fixed',
                    top: `${rect.bottom}px`,
                    left: `${rect.left}px`,
                    'max-height': `${$scope.defaultHeight}px`,
                };
            }
            return {
                'max-height': `${$scope.defaultHeight}px`,
            };
        };
        function focusoutEvent(e) {
            if (!e.relatedTarget || !$element[0].contains(e.relatedTarget)) {
                $scope.$apply($scope.closeDropdown);
            }
        }
        $element.on('focusout', focusoutEvent);
        function cleanUp() {
            $window.removeEventListener('resize', resizeEvent);
            $element.off('focusout', focusoutEvent);
        }
        $scope.$on('$destroy', cleanUp);
        const contentLoaded = $scope.$watch('$viewContentLoaded', function () {
            $timeout(() => {
                $scope.setDefault();
                contentLoaded();
            }, 0);
        });
    }],
    template: `<div class="fd-popover">
        <div class="fd-popover__control" aria-disabled="{{ !!isDisabled }}" ng-mouseover="setMessageVisibility(true)" ng-mouseleave="setMessageVisibility(false)">
            <div ng-class="getClasses()">
                <button id="{{ buttonId }}" ng-class="getControlClasses()" ng-click="onControlClick($event)" aria-labelledby="{{ [labelId, textId].join(' ') }}" aria-expanded="{{ bodyExpanded }}" aria-haspopup="listbox" aria-disabled="{{ !!isDisabled }}" tabindex="0">
                    <span id="{{ textId }}" class="fd-select__text-content">{{ getSelectedItemText() }}</span>
                    <span class="fd-button fd-button--transparent fd-select__button"><i class="{{iconClass}}"></i></span>
                </button>
            </div>
        </div>
        <div id="{{ bodyId }}" aria-hidden="{{ !bodyExpanded }}" ng-class="getPopoverBodyClasses()" ng-style="getStyle()">
            <div ng-if="message" aria-live="assertive" ng-class="getListMessageClasses()" role="alert">{{ message }}</div>
            <ul ng-class="getListClasses()" aria-activedescendant="{{ getSelectedItemId() }}" aria-labelledby="{{ labelId }}" role="listbox" ng-transclude></ul>
        </div>
        <div ng-if="message && state && messageVisibile" class="fd-popover__body fd-popover__body--no-arrow" aria-hidden="{{ bodyExpanded }}" ng-style="getStyle()">
            <span ng-class="getFormMessageClasses()">{{ message }}</span>
        </div>
    </div>`
})).directive('bkOption', (classNames, uuid) => ({
    restrict: 'EA',
    require: '^^bkSelect',
    replace: true,
    transclude: true,
    scope: {
        text: '@',
        secondaryText: '@?',
        value: '<',
        glyph: '@?',
        noWrap: '<?'
    },
    link: (scope, _element, _attrs, selectCtrl) => {
        scope.optionId = `select-option-${uuid.generate()}`;

        scope.isSelected = () => selectCtrl.getSelectedValue() === scope.value;

        scope.selectItem = () => {
            selectCtrl.selectItem(scope);
        };

        scope.getClasses = () => classNames('fd-list__item', {
            'is-selected': scope.isSelected(),
        });

        scope.getTitleClasses = () => classNames('fd-list__title', {
            'fd-list__title--no-wrap': scope.noWrap === true,
        });

        scope.getIconClasses = () => classNames('fd-list__icon', {
            [scope.glyph]: scope.glyph,
        });

        selectCtrl.addItem(scope);

        scope.$on('$destroy', () => selectCtrl.removeItem(scope));
    },
    template: `<li id="{{ optionId }}" ng-class="getClasses()" role="option" aria-selected="{{ isSelected() }}" ng-click="selectItem()">
        <i ng-if="glyph" role="presentation" ng-class="getIconClasses()"></i>
        <span ng-class="getTitleClasses()">{{ text }}</span>
        <span ng-if="secondaryText" class="fd-list__secondary">{{ secondaryText }}</span>
    </li>`
}));