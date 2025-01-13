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
angular.module('platformSplit', []).constant('SplitPaneState', { EXPANDED: 0, COLLAPSED: 1 }).directive('split', ['SplitPaneState', function (SplitPaneState) {
    return {
        restrict: 'E',
        replace: true,
        transclude: true,
        scope: {
            direction: '@',
            width: '@',
            height: '@',
            state: '=?'
        },
        controller: ['$scope', '$element', function ($scope, $element) {
            $scope.panes = [];
            $scope.state = $scope.state || [];

            this.addPane = function (pane) {
                $scope.panes.push(pane);
                $scope.state.push(SplitPaneState.EXPANDED);

                $scope.panes.sort((a, b) => {
                    let elementA = a.element[0];
                    let elementB = b.element[0];
                    if (elementA.previousElementSibling === null || elementB.nextElementSibling === null) return -1;
                    if (elementA.nextElementSibling === null || elementB.previousElementSibling === null) return 1;
                    if (elementA.nextElementSibling === elementB || elementB.previousElementSibling === elementA) return -1;
                    if (elementB.nextElementSibling === elementA || elementA.previousElementSibling === elementB) return 1;
                    return 0;
                });
            };

            this.removePane = function (pane) {
                let index = $scope.panes.indexOf(pane);
                if (index !== -1) {
                    $scope.panes.splice(index, 1);
                }
            };

            function normalizeSizes(sizes, index = -1) {
                let isOpen = (size, i) => {
                    return Math.floor(size) > 0 && (index === -1 || index !== i);
                };

                let totalSize = sizes.reduce((x, y) => x + y, 0);
                if (totalSize !== 100) {
                    let openCount = sizes.reduce((count, size, i) => isOpen(size, i) ? count + 1 : count, 0);
                    if (openCount > 0) {
                        let d = (100 - totalSize) / openCount;
                        for (let i = 0; i < sizes.length; i++) {
                            if (isOpen(sizes[i], i))
                                sizes[i] += d;
                        }
                    }
                }
            }

            function calcAutoSize() {
                let sizes = $scope.panes.map(pane => pane.size);
                let fixedSizeTotal = sizes.reduce((sum, size) => size !== 'auto' ? sum + Number(size) : sum, 0);
                let autoSizeCount = sizes.reduce((count, size) => size === 'auto' ? count + 1 : count, 0);
                let autoSize = 0;
                if (fixedSizeTotal < 100 && autoSizeCount > 0) {
                    autoSize = (100 - fixedSizeTotal) / autoSizeCount;
                }
                return autoSize;
            }

            function getPaneSizes() {
                let autoSize = calcAutoSize();
                return $scope.panes.map(pane => pane.size === 'auto' ? autoSize : Number(pane.size));
            }

            const directionWatch = $scope.$watch('direction', function (newDirection, oldDirection) {
                if (oldDirection)
                    $element.removeClass(oldDirection);

                $element.addClass(newDirection || 'horizontal');
            });

            const panesWatch = $scope.$watchCollection('panes', function () {
                if ($scope.split) {
                    $scope.split.destroy();
                    $scope.split = null;
                }

                if ($scope.panes.length === 0 || $scope.panes.some(a => a.element === undefined)) {
                    return;
                }

                if ($scope.panes.length === 1) {
                    $scope.panes[0].element.css('width', '100%');
                    $scope.panes[0].element.css('height', '100%');
                    return;
                }

                let sizes = getPaneSizes();// $scope.panes.map(pane => pane.size || 0);

                normalizeSizes(sizes);

                let minSizes = $scope.panes.map(pane => pane.minSize);
                let maxSizes = $scope.panes.map(pane => pane.maxSize);
                let elements = $scope.panes.map(pane => pane.element[0]);
                let snapOffsets = $scope.panes.map(pane => pane.snapOffset);

                $scope.split = Split(elements, {
                    direction: $scope.direction,
                    sizes: sizes,
                    minSize: minSizes,
                    maxSize: maxSizes,
                    expandToMin: true,
                    gutterSize: 1,
                    gutterAlign: 'start',
                    snapOffset: snapOffsets,
                    onDragEnd: function (newSizes) {
                        for (let i = 0; i < newSizes.length; i++) {
                            $scope.state[i] = Math.floor(newSizes[i]) === 0 ? SplitPaneState.COLLAPSED : SplitPaneState.EXPANDED;
                        }
                        $scope.$apply();
                    },
                });
            });

            const stateWatch = $scope.$watchCollection('state', function (newState, oldState) {
                if (newState.length === oldState.length) {
                    //Process the collapsing first
                    for (let i = 0; i < newState.length; i++) {
                        if (newState[i] !== oldState[i]) {
                            if (newState[i] === SplitPaneState.COLLAPSED) {
                                let sizes = $scope.split.getSizes();
                                let size = Math.floor(sizes[i]);
                                if (size > 0) {
                                    $scope.panes[i].lastSize = size;
                                    $scope.split.collapse(i);
                                }
                            }
                        }
                    }
                    // ... and then the expanding/restore if necessary
                    for (let i = 0; i < newState.length; i++) {
                        if (newState[i] !== oldState[i]) {
                            if (newState[i] === SplitPaneState.EXPANDED) {
                                let sizes = $scope.split.getSizes();
                                let size = Math.floor(sizes[i]);
                                if (size === 0) {
                                    let pane = $scope.panes[i];
                                    sizes[i] = pane.lastSize || (pane.size == 'auto' ? calcAutoSize() : Number(pane.size));
                                    normalizeSizes(sizes, i);
                                    $scope.split.setSizes(sizes);
                                }
                            }
                        }
                    }
                }
            });
            function cleanUp() {
                directionWatch();
                panesWatch();
                stateWatch();
            }
            $scope.$on('$destroy', cleanUp);
        }],
        template: '<div class="bk-split" ng-transclude></div>'
    };
}]).directive('splitPane', () => ({
    restrict: 'E',
    require: '^split',
    replace: true,
    transclude: true,
    scope: {
        size: '@',
        minSize: '<?',
        maxSize: '<?',
        snapOffset: '<?',
    },
    link: (scope, element, _attrs, bgSplitCtrl) => {
        const paneData = scope.paneData = {
            element: element,
            size: scope.size,
            minSize: scope.minSize,
            maxSize: scope.maxSize,
            snapOffset: Number(scope.snapOffset || 0)
        };

        bgSplitCtrl.addPane(paneData);

        scope.$on('$destroy', () => {
            bgSplitCtrl.removePane(paneData);
        });
    },
    template: '<div class="bk-split-pane" ng-transclude></div>'
}));