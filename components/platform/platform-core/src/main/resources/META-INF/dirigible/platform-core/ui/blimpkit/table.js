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
blimpkit.directive('bkTable', (classNames) => ({
    restrict: 'A',
    transclude: true,
    replace: true,
    scope: {
        innerBorders: '@?',
        outerBorders: '@?',
        displayMode: '@?',
        fixed: '<?',
    },
    controller: ['$scope', '$element', function ($scope, $element) {
        this.setAriaDescribedBy = function (id) {
            $element[0].setAttribute('aria-describedby', id);
        };
        $scope.getClasses = () => classNames('fd-table', {
            'fd-table--top-border fd-table--no-horizontal-borders fd-table--no-vertical-borders': $scope.innerBorders === 'top',
            'fd-table--no-horizontal-borders fd-table--no-vertical-borders': $scope.innerBorders === 'none',
            'fd-table--no-horizontal-borders': $scope.innerBorders === 'horizontal',
            'fd-table--no-vertical-borders': $scope.innerBorders === 'vertical',
            'fd-table--compact': $scope.displayMode === 'compact',
            'fd-table--condensed': $scope.displayMode === 'condensed',
            'bk-table--no-outer-horizontal-borders': $scope.outerBorders === 'vertical',
            'bk-table--no-outer-vertical-borders': $scope.outerBorders === 'horizontal',
            'fd-table--no-outer-border bk-list-border--top': $scope.outerBorders === 'top',
            'fd-table--no-outer-border bk-list-border--bottom': $scope.outerBorders === 'bottom',
            'fd-table--no-outer-border': $scope.outerBorders === 'none',
            'bk-table--fixed': $scope.fixed,
        });
    }],
    template: '<table ng-class="getClasses()" ng-transclude></table>'
})).directive('bkTableCaption', (uuid) => ({
    restrict: 'A',
    transclude: true,
    replace: true,
    require: '^^bkTable',
    link: (_scope, element, _attrs, tableCtrl) => {
        const id = `fdt-${uuid.generate()}`;
        element[0].setAttribute('id', id);
        tableCtrl.setAriaDescribedBy(id);
    },
    template: '<caption class="fd-table__caption" aria-live="polite" ng-transclude></caption>'
})).directive('bkTableFixed', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: '<div class="fd-table--fixed" ng-transclude></div>'
})).directive('bkTableHeader', (classNames) => ({
    restrict: 'A',
    transclude: true,
    replace: true,
    scope: {
        sticky: '<?'
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-table__header', {
            'bk-table__header-sticky': scope.sticky === true
        });
    },
    template: '<thead ng-class="getClasses()" ng-transclude></thead>'
})).directive('bkTableBody', (classNames) => ({
    restrict: 'A',
    transclude: true,
    replace: true,
    scope: {
        innerBorders: '@?'
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-table__body', {
            'fd-table__body--no-horizontal-borders': scope.innerBorders === 'horizontal' || scope.innerBorders === 'none',
            'fd-table__body--no-vertical-borders': scope.innerBorders === 'vertical' || scope.innerBorders === 'none'
        });
    },
    template: '<tbody ng-class="getClasses()" ng-transclude></tbody>'
})).directive('bkTableFooter', (classNames) => ({
    restrict: 'A',
    transclude: true,
    replace: true,
    scope: {
        sticky: '<?'
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-table__footer', {
            'bk-table__footer-sticky': scope.sticky === true
        });
    },
    template: '<tfoot ng-class="getClasses()" ng-transclude></tfoot>'
})).directive('bkTableRow', (classNames) => ({
    restrict: 'A',
    transclude: true,
    replace: true,
    require: '?^^bkTableGroup',
    scope: {
        selected: '<?',
        activable: '<?',
        hoverable: '<?'
    },
    link: (scope, element, _attrs, tableGroupCtrl) => {
        scope.getClasses = () => classNames('fd-table__row', {
            'fd-table__cell--activable': scope.activable,
            'fd-table__cell--hoverable': scope.hoverable,
            'bk-hidden': tableGroupCtrl && tableGroupCtrl.shouldHideRow(element[0])
        });

        scope.isRowExpanded = () => tableGroupCtrl && tableGroupCtrl.isRowExpanded(element[0])
        scope.getAriaSelected = () => scope.selected ? 'true' : undefined;
    },
    template: '<tr ng-class="getClasses()" ng-attr-aria-selected="{{ getAriaSelected() }}" ng-transclude></tr>'
})).directive('bkTableHeaderCell', (classNames) => ({
    restrict: 'A',
    transclude: true,
    replace: true,
    scope: {
        contentType: '@?',
        fixed: '@?',
        activable: '<?',
        hoverable: '<?',
    },
    link: (scope, element) => {
        scope.getClasses = () => classNames('fd-table__cell', {
            'fd-table__cell--checkbox': scope.contentType === 'checkbox',
            'fd-table__cell--status-indicator': scope.contentType === 'statusIndicator',
            'fd-table__cell--fixed': scope.fixed === true,
            'fd-table__cell--fixed fd-table__cell--fixed-last': scope.fixed === 'last',
            'fd-table__cell--activable': scope.activable === true,
            'fd-table__cell--hoverable': scope.hoverable === true,
        });

        if (element.closest('tbody').length > 0) {
            element[0].setAttribute('scope', 'row');
        } else if (element.closest('thead').length > 0) {
            element[0].setAttribute('scope', 'col');
        }
    },
    template: '<th ng-class="getClasses()" ng-transclude></th>'
})).directive('bkTableCell', (classNames) => ({
    restrict: 'A',
    transclude: true,
    replace: true,
    require: '?^^bkTableGroup',
    scope: {
        contentType: '@?',
        fitContent: '<?',
        activable: '<?',
        hoverable: '<?',
        navigated: '<?',
        noData: '<?',
        statusIndicator: '@?',
        nestingLevel: '<?'
    },
    link: (scope, element, _attrs, tableGroupCtrl) => {
        scope.getClasses = () => classNames('fd-table__cell', {
            'fd-table__cell--no-data': scope.noData,
            'fd-table__cell--checkbox': scope.contentType === 'checkbox',
            'fd-table__cell--status-indicator': scope.contentType === 'statusIndicator',
            [`fd-table__cell--status-indicator--${scope.statusIndicator}`]: scope.statusIndicator,
            'fd-table__cell--fit-content': scope.fitContent,
            'fd-table__cell--activable': scope.activable,
            'fd-table__cell--hoverable': scope.hoverable,
            'fd-table__cell--navigated': scope.navigated
        });

        if (scope.nestingLevel) {
            element[0].setAttribute('data-nesting-level', scope.nestingLevel);
            if (tableGroupCtrl) {
                let rowEl = element.parent()[0];
                tableGroupCtrl.addRow(rowEl, scope.nestingLevel);

                scope.$on('$destroy', function () {
                    tableGroupCtrl.removeRow(rowEl);
                });
            }
        }
    },
    template: `<td ng-class="getClasses()" ng-attr-colspan="{{ noData ? '100%' : undefined }}"  ng-transclude></td>`
})).directive('bkTableGroup', function () {
    return {
        restrict: 'A',
        controller: ['$element', function ($element) {
            $element.addClass('fd-table--group');

            const findRowIndex = (rowElement) => rows.findIndex(r => r.element === rowElement);
            let rows = [];

            this.addRow = function (rowElement, nestingLevel) {
                let index = $(rowElement).index();
                rows.splice(index, 0, {
                    element: rowElement,
                    nestingLevel
                });
            };

            this.removeRow = function (rowElement) {
                let index = findRowIndex(rowElement);
                if (index >= 0) {
                    rows.splice(index, 1);
                }
            }

            this.addGroupRow = function (rowElement, nestingLevel, expanded) {
                let index = $(rowElement).index();
                rows.splice(index, 0, {
                    groupRow: true,
                    element: rowElement,
                    nestingLevel,
                    expanded
                });
            };

            this.setGroupRowExpanded = function (rowElement, expanded) {
                let index = findRowIndex(rowElement);
                if (index >= 0) {
                    rows[index].expanded = expanded;
                }
            }

            const getParentGroupRowIndex = function (index) {
                if (index >= 0) {
                    let nestingLevel = rows[index].nestingLevel;
                    if (nestingLevel > 1) {
                        for (let i = index - 1; i >= 0; i--) {
                            let row = rows[i];

                            if (row.groupRow && row.nestingLevel < nestingLevel)
                                return i;
                        }
                    }
                }

                return -1;
            }

            this.shouldHideRow = function (rowElement) {
                let index = findRowIndex(rowElement);
                if (index >= 0) {
                    let parentRowIndex = index;

                    while (parentRowIndex >= 0) {
                        parentRowIndex = getParentGroupRowIndex(parentRowIndex);

                        if (parentRowIndex >= 0) {
                            let parentRow = rows[parentRowIndex];

                            if (!parentRow.expanded)
                                return true;
                        }
                    }
                }

                return false;
            }

            this.isRowExpanded = function (rowElement) {
                let index = findRowIndex(rowElement);
                if (index >= 0) {
                    let row = rows[index];
                    return row.groupRow && row.expanded;
                }

                return false;
            }
        }]
    };
}).directive('bkTableGroupCell', (classNames) => ({
    restrict: 'A',
    transclude: true,
    replace: true,
    scope: {
        nestingLevel: '<?',
        expanded: '<?'
    },
    require: '^^bkTableGroup',
    link: (scope, element, _attrs, tableGroupCtrl) => {
        let rowEl = element.parent()[0];
        tableGroupCtrl.addGroupRow(rowEl, scope.nestingLevel, scope.expanded);

        scope.getClasses = () => classNames('fd-table__expand', {
            'fd-table__expand--open': scope.expanded
        });

        scope.toggleExpanded = function () {
            scope.expanded = !scope.expanded;
        };

        const expandedWatch = scope.$watch('expanded', function () {
            tableGroupCtrl.setGroupRowExpanded(element.parent()[0], scope.expanded);
        });

        if (scope.nestingLevel) {
            element[0].setAttribute('data-nesting-level', scope.nestingLevel);
        }

        scope.$on('$destroy', function () {
            tableGroupCtrl.removeRow(rowEl);
            expandedWatch();
        });
    },
    template: `<td class="fd-table__cell fd-table__cell--group fd-table__cell--expand" colspan="100%" ng-click="toggleExpanded()">
        <span ng-class="getClasses()"></span>
        <span class="fd-table__text--no-wrap" ng-transclude></span>
    </td>`
})).directive('bkTableIcon', (classNames) => ({
    restrict: 'A',
    replace: true,
    scope: {
        navigation: '<?',
        glyph: '@'
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-table__icon', scope.glyph, {
            'fd-table__icon--navigation': scope.navigation
        });
    },
    template: '<i ng-class="getClasses()" role="presentation"></i>'
}));