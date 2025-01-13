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
blimpkit.directive('bkPagination', (uuid, classNames, $injector) => {
    if (!$injector.has('bkSelectDirective') || !$injector.has('bkInputDirective')) {
        console.error('bk-pagination requires the bk-select and bk-input widgets to be loaded.');
        return {};
    }
    return {
        restrict: 'EA',
        replace: true,
        scope: {
            totalItems: '<',
            totalItemsLabel: '@?',
            itemsPerPage: '=?',
            currentPage: '=?',
            compact: '<?',
            displayTotalItems: '<',
            itemsPerPageOptions: '<',
            pageChange: '&',
            itemsPerPageChange: '&',
            itemsPerPagePlacement: '@',
            itemsPerPageLabel: '@?',
        },
        link: (scope) => {
            const maxButtonsInShortMode = 9; // must be an odd number (min 5)
            const maxInnerButtonsInShortMode = maxButtonsInShortMode - 4; //excluding left and right arrow buttons and first and last number buttons

            scope.totalItems = scope.totalItems || 0;
            scope.itemsPerPage = scope.itemsPerPage || 20;
            scope.currentPage = scope.currentPage || 1;
            scope.currentPageInput = scope.currentPage;

            scope.itemsPerPageLabelId = `pag-perpage-label-${uuid.generate()}`;
            scope.currentPageLabelId = `pag-page-label-${uuid.generate()}`;
            scope.currentPageOfLabelId = `pag-of-label-${uuid.generate()}`;

            scope.isShortMode = () => scope.getPageCount() <= maxButtonsInShortMode;

            scope.isCurrentPageValid = (pageNumber) => pageNumber >= 1 && pageNumber <= scope.getPageCount();

            scope.changePage = () => {
                scope.gotoPage(scope.currentPageInput);
            };

            scope.onCurrentPageInputChange = () => {
                scope.currentPageInputState = scope.isCurrentPageValid(scope.currentPageInput) ? null : 'error';
            };

            scope.onCurrentPageInputBlur = () => {
                if (scope.currentPageInput != scope.currentPage) {
                    scope.currentPageInput = scope.currentPage;
                    scope.currentPageInputState = null;
                }
            };

            scope.gotoPage = (pageNumber) => {
                if (scope.isCurrentPageValid(pageNumber)) {
                    scope.currentPage = pageNumber;
                    scope.currentPageInput = pageNumber;

                    scope.pageChange && scope.pageChange({ pageNumber });
                }
            };

            scope.gotoFirstPage = () => {
                scope.gotoPage(1);
            };

            scope.gotoLastPage = () => {
                scope.gotoPage(scope.getPageCount());
            };

            scope.gotoPrevPage = () => {
                scope.gotoPage(scope.currentPage - 1);
            };

            scope.gotoNextPage = () => {
                scope.gotoPage(scope.currentPage + 1);
            };

            scope.getPageCount = () => Math.ceil(scope.totalItems / scope.itemsPerPage);

            scope.isPrevButtonEnabled = () => scope.currentPage > 1;

            scope.isNextButtonEnabled = () => scope.currentPage < scope.getPageCount();

            scope.hasStartEllipsys = () => scope.getPageCount() > maxButtonsInShortMode && scope.currentPage > Math.ceil(maxButtonsInShortMode / 2);

            scope.hasEndEllipsys = () => scope.getPageCount() > maxButtonsInShortMode && scope.currentPage <= scope.getPageCount() - Math.ceil(maxButtonsInShortMode / 2);

            scope.showEllipsys = (index, length) => (index === 0 && scope.hasStartEllipsys()) || (index === length - 2 && scope.hasEndEllipsys());

            scope.getPageNumbers = () => {
                let count = scope.getPageCount();
                const numbers = [1];
                if (count > 2) {
                    const hasStartEllipsys = scope.hasStartEllipsys();
                    const hasEndEllipsys = scope.hasEndEllipsys();
                    let startNumber, endNumber;

                    if (hasStartEllipsys && hasEndEllipsys) {
                        const offset = Math.ceil(maxInnerButtonsInShortMode / 2) - 1;
                        startNumber = scope.currentPage - offset;
                        endNumber = scope.currentPage + offset;

                    } else if (hasStartEllipsys && !hasEndEllipsys) {
                        endNumber = count - 1;
                        startNumber = endNumber - maxInnerButtonsInShortMode;

                    } else if (!hasStartEllipsys && hasEndEllipsys) {
                        startNumber = 2;
                        endNumber = startNumber + maxInnerButtonsInShortMode;

                    } else {
                        startNumber = 2;
                        endNumber = count - 1
                    }

                    for (let i = startNumber; i <= endNumber; i++) {
                        numbers.push(i);
                    }
                }
                if (count > 1) numbers.push(count);

                return numbers;
            };

            scope.getClasses = () => classNames('fd-pagination', {
                'fd-pagination--short': scope.isShortMode(),
            });

            scope.getNumberButtonClasses = (pageNumber) => classNames('fd-button', 'fd-button--transparent', 'fd-pagination__link', {
                'is-active': pageNumber === scope.currentPage,
                'fd-button--compact': scope.compact === true,
            });

            scope.getArrowButtonClassess = () => classNames('fd-button', 'fd-button--transparent', 'fd-pagination__button', {
                'fd-button--compact': scope.compact === true,
            });

            scope.getNumberButtonAriaLabel = (pageNumber) => pageNumber === scope.currentPage ? `Current Page, Page ${pageNumber}` : `Goto page ${pageNumber}`;

            scope.getCurrentPageInputAriaLabelledBy = () => [scope.currentPageLabelId, scope.currentPageOfLabelId].join(' ');

            scope.getTotal = () => `${scope.totalItems} ${scope.totalItemsLabel ?? 'Results'}`;

            const itemsWatch = scope.$watch('itemsPerPage', (newVal, oldVal) => {
                if (newVal !== oldVal) {
                    if (scope.itemsPerPageChange)
                        scope.itemsPerPageChange({ itemsPerPage: scope.itemsPerPage });
                }

                const pageCount = scope.getPageCount();
                if (scope.currentPage > pageCount) {
                    scope.gotoPage(pageCount);
                }
            });

            scope.$on('$destroy', () => { itemsWatch() });
        },
        template: `<div ng-class="getClasses()">
            <div ng-if="itemsPerPageOptions" class="fd-pagination__per-page">
                <label class="fd-form-label fd-pagination__per-page-label" id="{{::itemsPerPageLabelId}}">{{itemsPerPageLabel || 'Results per page:'}}</label>
                <bk-select selected-value="$parent.itemsPerPage" compact="compact" label-id="{{::itemsPerPageLabelId}}" placement="{{ itemsPerPagePlacement }}">
                    <bk-option ng-repeat="option in itemsPerPageOptions track by option" text="{{ option }}" value="option"></bk-option>
                </bk-select>
            </div>
            <nav class="fd-pagination__nav" role="navigation">
                <a href="javascript:void(0)" ng-class="getArrowButtonClassess()" class="fd-pagination__button--mobile" aria-label="First page" aria-disabled="{{ !isPrevButtonEnabled() }}" ng-click="gotoFirstPage()"><i class="sap-icon sap-icon--media-rewind"></i></a>
                <a href="javascript:void(0)" ng-class="getArrowButtonClassess()" aria-label="Previous page" aria-disabled="{{ !isPrevButtonEnabled() }}" ng-click="gotoPrevPage()"><i class="sap-icon sap-icon--navigation-left-arrow"></i></a>
                <a ng-if="pageNumber !== currentPage || isShortMode()" ng-repeat-start="pageNumber in pageNumbers = getPageNumbers()" href="javascript:void(0)" ng-class="getNumberButtonClasses(pageNumber)" aria-label="{{ getNumberButtonAriaLabel(pageNumber) }}" aria-current="{{ currentPage === pageNumber }}" ng-click="gotoPage(pageNumber)">{{ pageNumber }}</a>
                <label ng-if="pageNumber === currentPage" id="{{ currentPageLabelId }}" class="fd-form-label fd-pagination__label" aria-label="Page input, Current page, Page {currentPage}">Page:</label>
                <bk-input ng-if="pageNumber === currentPage" aria-labelledby="{{ getCurrentPageInputAriaLabelledBy() }}" class="fd-pagination__input" type="number" min="1" max="{{ getPageCount() }}" compact="compact" ng-required state="{{ currentPageInputState }}" ng-model="$parent.$parent.currentPageInput" ng-keydown="$event.keyCode === 13 && changePage()" ng-blur="onCurrentPageInputBlur()" ng-change="onCurrentPageInputChange()"></bk-input>
                <label ng-if="pageNumber === currentPage" id="{{ currentPageOfLabelId }}" class="fd-form-label fd-pagination__label">of {{ getPageCount() }}</label>
                <span ng-if="showEllipsys($index, pageNumbers.length)" ng-repeat-end class="fd-pagination__more" role="presentation"></span>
                <a href="javascript:void(0)" ng-class="getArrowButtonClassess()" aria-label="Next page" aria-disabled="{{ !isNextButtonEnabled() }}" ng-click="gotoNextPage()"><i class="sap-icon sap-icon--navigation-right-arrow"></i></a>
                <a href="javascript:void(0)" ng-class="getArrowButtonClassess()" class="fd-pagination__button--mobile" aria-label="Last page" aria-disabled="{{ !isNextButtonEnabled() }}" ng-click="gotoLastPage()"><i class="sap-icon sap-icon--media-forward"></i></a>
            </nav>
            <div ng-if="displayTotalItems" class="fd-pagination__total">
                <span class="fd-form-label fd-pagination__total-label">{{ getTotal() }}</span>
            </div>
        </div>`
    }
});