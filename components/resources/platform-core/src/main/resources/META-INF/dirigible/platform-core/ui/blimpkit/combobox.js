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
blimpkit.directive('bkComboboxInput', function (uuid, classNames, $window, $timeout, $injector, ButtonStates) {
    if (!$injector.has('bkListDirective') || !$injector.has('bkInputDirective') || !$injector.has('bkTokenizerDirective') || !$injector.has('bkTokenDirective') || !$injector.has('bkButtonDirective') || !$injector.has('bkScrollbarDirective')) {
        console.error('bk-combobox-input requires the bk-list, bk-input, bk-button, bk-token, bk-tokenizer and bk-scrollbar widgets to be loaded.');
        return {};
    }
    return {
        restrict: 'EA',
        replace: true,
        require: '?ngModel',
        scope: {
            dropdownItems: '<',
            placeholder: '@?',
            compact: '<?',
            isDisabled: '<?',
            isReadonly: '<?',
            state: '@?',
            message: '@?',
            inputId: '@',
            btnAriaLabel: '@',
            listAriaLabel: '@',
            multiSelect: '<?',
            maxBodyHeight: '@?',
            filter: '@?'
        },
        link: function (scope, element, _attrs, ngModel) {
            scope.defaultHeight = 16;
            if (!angular.isDefined(scope.btnAriaLabel) || !angular.isDefined(scope.listAriaLabel)) {
                console.error('bk-combobox-input error: Provide the "btn-aria-label" and "list-aria-label" attributes');
            }
            let collectionWatch;
            if (ngModel) {
                const onSelectedValueChanged = (value) => {
                    if (!angular.equals(value, ngModel.$viewValue)) {
                        ngModel.$setViewValue(scope.multiSelect ? [...value] : value);
                    }
                    ngModel.$validate();
                };

                if (scope.multiSelect) {
                    collectionWatch = scope.$watchCollection('selectedValue', onSelectedValueChanged);
                } else {
                    scope.$watch('selectedValue', onSelectedValueChanged);
                }

                ngModel.$isEmpty = (value) => {
                    return (value === null || value === undefined || value === '') || (scope.multiSelect && value.length === 0);
                };

                ngModel.$render = () => {
                    let selectedValue = ngModel.$viewValue;

                    if (!scope.multiSelect) {
                        const selectedItem = scope.dropdownItems.find(x => x.value === selectedValue);
                        scope.search.term = selectedItem ? selectedItem.text.toString() : '';
                        scope.clearFilter();
                    } else {
                        if (selectedValue === undefined) {
                            selectedValue = [];
                        } else if (!Array.isArray(selectedValue)) {
                            console.error(`bk-combobox-input error: When multi-select is true 'selected-value' must be an array`);
                            selectedValue = [];
                        } else {
                            selectedValue = [...selectedValue];
                        }
                    }

                    scope.selectedValue = selectedValue;
                };

                // we have to add an extra watch since ngModel doesn't work well with arrays - it
                // doesn't trigger rendering if only an item in the array changes.

                if (scope.multiSelect) {
                    // we have to do it on each watch since ngModel watches reference, but
                    // we need to work of an array, so we need to see if anything was inserted/removed
                    var lastView, lastViewRef = NaN;
                    scope.$watch(function selectMultipleWatch() {
                        if (lastViewRef === ngModel.$viewValue && !angular.equals(lastView, ngModel.$viewValue)) {
                            lastView = [...ngModel.$viewValue];
                            ngModel.$render();
                        }
                        lastViewRef = ngModel.$viewValue;
                    });
                }
            }

            scope.search = { term: '' };
            scope.dropdownItems = scope.dropdownItems || [];
            scope.filteredDropdownItems = scope.dropdownItems;
            scope.bodyId = `cb-body-${uuid.generate()}`;
            scope.checkboxIds = {};
            scope.bodyExpanded = false;

            scope.onControlClick = () => {
                if (!scope.bodyExpanded) {
                    scope.setDefaultSize();
                    element.find('input').focus();
                }
                scope.bodyExpanded = !scope.bodyExpanded;
            };

            scope.closeDropdown = () => {
                scope.bodyExpanded = false;
            };

            scope.openDropdown = () => {
                if (!scope.bodyExpanded) {
                    scope.setDefaultSize();
                    scope.bodyExpanded = true;
                }
            };

            scope.isBodyExpanded = () => {
                return scope.bodyExpanded && scope.filteredDropdownItems.length > 0;
            };

            scope.onInputChange = () => {
                if (scope.search.term === undefined) scope.search.term = '';
                scope.filterValues();

                if (!scope.multiSelect) {
                    const item = scope.dropdownItems.find(x => x.text.toString().toLowerCase() === scope.search.term.toLowerCase());
                    scope.selectedValue = item ? item.value : null;
                }
            };

            const filterStartsWith = (x) => x.text.toString().toLowerCase().startsWith(scope.search.term.toLowerCase());
            const filterContains = (x) => x.text.toString().toLowerCase().includes(scope.search.term.toLowerCase());
            const filterContainsEach = (x) => {
                const terms = scope.search.term.toLowerCase().split(' ');
                const label = x.text.toString().toLowerCase();
                if (terms.every(term => label.includes(term))) return true;
                return false;
            };

            scope.filterValues = () => {
                if (scope.search.term) {
                    if (scope.filter === 'Contains') scope.filteredDropdownItems = scope.dropdownItems.filter(filterContains);
                    else if (scope.filter === 'ContainsEach') scope.filteredDropdownItems = scope.dropdownItems.filter(filterContainsEach);
                    else scope.filteredDropdownItems = scope.dropdownItems.filter(filterStartsWith);
                } else {
                    scope.filteredDropdownItems = scope.dropdownItems;
                }
            };

            scope.clearFilter = () => {
                scope.filteredDropdownItems = scope.dropdownItems;
            };

            scope.onKeyDown = (event) => {
                const ARROW_UP = 38;
                const ARROW_DOWN = 40;
                if (event.keyCode === ARROW_UP || event.keyCode === ARROW_DOWN) {
                    const inputEl = element.find('input');
                    const elements = [inputEl[0], ...element.find('.fd-popover__body li')];
                    const index = elements.findIndex(x => x === event.target);
                    if (index === -1) return;

                    if (event.keyCode === ARROW_DOWN) {
                        if (index < elements.length - 1) {
                            elements[index + 1].focus();
                            event.preventDefault();
                        }
                    } else if (event.keyCode === ARROW_UP) {
                        if (index > 0) {
                            elements[index - 1].focus();
                            event.preventDefault();
                        }
                    }
                }
            };

            scope.onSearchKeyDown = (event) => {
                switch (event.key) {
                    case 'Backspace':
                        if (scope.search.term.length === 0 && scope.selectedValue.length)
                            scope.selectedValue.splice(-1, 1);
                        break;
                    case 'Enter':
                        if (scope.search.term.length && scope.filteredDropdownItems.length) {
                            scope.addItemToSelection(scope.filteredDropdownItems[0]);
                            scope.search.term = '';
                            scope.clearFilter();
                        }
                        break;
                }
            };

            scope.removeItemFromSelection = (item) => {
                const itemIndex = scope.selectedValue.indexOf(item.value);
                if (itemIndex >= 0)
                    scope.selectedValue.splice(itemIndex, 1);
            };

            scope.addItemToSelection = (item) => {
                const itemIndex = scope.selectedValue.indexOf(item.value);
                if (itemIndex === -1)
                    scope.selectedValue.push(item.value);
            };

            scope.onItemClick = (item) => {
                if (scope.multiSelect) {
                    const itemIndex = scope.selectedValue.indexOf(item.value);
                    if (itemIndex >= 0)
                        scope.selectedValue.splice(itemIndex, 1);
                    else
                        scope.selectedValue.push(item.value);
                } else {
                    scope.selectedValue = item.value;
                    scope.search.term = item.text.toString();
                    scope.clearFilter();
                }

                if (!scope.multiSelect)
                    scope.closeDropdown();
            };

            scope.isSelected = (item) => {
                if (scope.multiSelect) {
                    return scope.selectedValue.includes(item.value);
                } else {
                    return item.value === scope.selectedValue;
                }
            };

            scope.getHighlightedText = (value) => {
                return scope.shouldRenderHighlightedText(value) ? value.substring(0, scope.search.term.length) : null;
            };

            scope.shouldRenderHighlightedText = (value) => {
                if (scope.filter && scope.filter !== 'StartsWith') return false;
                return value.toLowerCase().startsWith(scope.search.term.toLowerCase()) && value.length > scope.search.term.length;
            };

            scope.getLabel = (value) => {
                return scope.search.term && scope.shouldRenderHighlightedText(value) ? value.substring(scope.search.term.length) : value;
            };

            scope.getListClasses = () => classNames({
                'fd-list--multi-input': scope.multiSelect
            });

            scope.getCheckboxId = (value) => {
                let id = scope.checkboxIds[value];
                if (!id) scope.checkboxIds[value] = id = `cb-checkbox-${uuid.generate()}`;
                return id;
            };

            scope.getSelectedItems = () => {
                return scope.selectedValue.map(value => scope.dropdownItems.find(item => item.value === value));
            };

            scope.onTokenClick = (item) => {
                if (scope.bodyExpanded) element.find('input').focus();
                scope.removeItemFromSelection(item);
            };
            function focusoutEvent(e) {
                if (!scope.bodyExpanded) return;

                if (!e.relatedTarget || !element[0].contains(e.relatedTarget)) {
                    scope.$apply(scope.closeDropdown);
                }
            }
            element.on('focusout', focusoutEvent);

            const dropdownWatch = scope.$watchCollection('dropdownItems', (items) => {
                if (items === undefined || items === null)
                    scope.dropdownItems = [];

                scope.filteredDropdownItems = items || [];
            });

            scope.setDefaultSize = () => {
                const rect = element[0].getBoundingClientRect();
                scope.defaultHeight = $window.innerHeight - rect.bottom;
            };
            function resizeEvent() {
                scope.$apply(scope.setDefaultSize);
            }
            $window.addEventListener('resize', resizeEvent);
            function cleanUp() {
                element.off('focusout', focusoutEvent);
                $window.removeEventListener('resize', resizeEvent);
                dropdownWatch();
                if (ngModel && scope.multiSelect) collectionWatch();
            }
            scope.$on('$destroy', cleanUp);
        },
        template: `<div class="fd-popover" ng-keydown="onKeyDown($event)">
            <div class="fd-popover__control" ng-attr-disabled="{{isDisabled === true}}" ng-attr-aria-disabled="{{isDisabled === true}}" aria-expanded="{{ isBodyExpanded() }}" aria-haspopup="{{isReadonly ? 'false' : 'true'}}" aria-controls="{{ bodyId }}" ng-readonly="isReadonly">
                <bk-input-group ng-if="!isReadonly" compact="compact" class="fd-input-group--control" state="{{ state }}" is-disabled="isDisabled">
                    <bk-tokenizer ng-if="multiSelect">
                        <bk-token ng-repeat="item in getSelectedItems() track by item.value" close-clicked="onTokenClick(item)" label="{{item.text}}" close-aria-label="unselect option: {{item.text}}" tabindex="0"></bk-token>
                        <bk-token-indicator></bk-token-indicator>
                        <bk-input ng-attr-id="{{ inputId }}" type="text" autocomplete="off" placeholder="{{ placeholder }}" ng-focus="openDropdown()" ng-change="onInputChange()" ng-model="search.term" ng-keydown="onSearchKeyDown($event)"></bk-input>
                    </bk-tokenizer>
                    <bk-input ng-if="!multiSelect" ng-attr-id="{{ inputId }}" type="text" autocomplete="off" placeholder="{{ placeholder }}" ng-focus="openDropdown()" ng-change="onInputChange()" ng-model="search.term" tabindex="0" ng-readonly="isReadonly"></bk-input>
                    <bk-input-group-addon>
                        <bk-button class="fd-select__button" glyph="sap-icon--navigation-down-arrow" state="${ButtonStates.Transparent}" ng-disabled="isDisabled" ng-click="onControlClick()" aria-label="{{btnAriaLabel}}" aria-controls="{{ bodyId }}" aria-haspopup="true"></bk-button>
                    </bk-input-group-addon>
                </bk-input-group>
                <bk-input ng-if="isReadonly" ng-attr-id="{{ inputId }}" type="text" autocomplete="off" placeholder="{{ placeholder }}" ng-model="search.term" tabindex="0" aria-readonly="true" readonly></bk-input>
            </div>
            <div ng-if="isDisabled !== true && isReadonly !== true" id="{{ bodyId }}" class="fd-popover__body fd-popover__body--no-arrow fd-popover__body--dropdown fd-popover__body--dropdown-fill" aria-hidden="{{ !isBodyExpanded() }}">
                <div class="fd-popover__wrapper" bk-scrollbar style="max-height:{{ maxBodyHeight || defaultHeight }}px;">
                    <bk-list-message ng-if="message" type="{{ state }}">{{ message }}</bk-list-message>
                    <bk-list class="{{getListClasses()}}" dropdown-mode="true" compact="compact" has-message="!!message" aria-label="{{listAriaLabel}}">
                        <bk-list-item ng-repeat="item in filteredDropdownItems track by item.value" role="option" tabindex="0" selected="isSelected(item)" ng-click="onItemClick(item)">
                            <bk-list-form-item ng-if="multiSelect">
                                <bk-checkbox id="{{getCheckboxId(item.value)}}" compact="compact" ng-checked="isSelected(item)"></bk-checkbox>
                                <bk-checkbox-label empty="true" compact="compact" for="{{getCheckboxId(item.value)}}" ng-click="$event.preventDefault()" tabindex="-1"></bk-checkbox-label>
                            </bk-list-form-item>
                            <bk-list-icon ng-if="item.glyph || item.svg" glyph="{{item.glyph}}" svg-path="{{item.svg}}"></bk-list-icon>
                            <bk-list-title>
                                <span ng-if="search.term && shouldRenderHighlightedText(item.text.toString())" class="fd-list__bold">{{ getHighlightedText(item.text.toString()) }}</span>{{ getLabel(item.text.toString()) }}
                            </bk-list-title>
                            <bk-list-seconday ng-if="item.secondaryText">{{ item.secondaryText }}</bk-list-seconday>
                        </bk-list-item>
                    </bk-list>
                </div>
            </div>
        </div>`
    }
});