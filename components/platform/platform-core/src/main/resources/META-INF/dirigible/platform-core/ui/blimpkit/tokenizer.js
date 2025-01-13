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
blimpkit.directive('bkTokenizer', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    scope: {
        compact: '<?',
        isFocus: '<?',
        scrollable: '<?'
    },
    controller: ['$scope', '$element', 'classNames', function ($scope, $element, classNames) {
        $scope.tokenElements = [];
        $scope.numberOfVisibleTokens = 0;

        const tokenElementWidths = new Map();
        const scrollable = !!$scope.scrollable;
        let tokenizerRO, tokensRO;

        function findNumberOfVisibleTokens() {
            let width = 0;
            const indicatorWidth = 50;
            const inputMinWidth = getInputMinWidth();
            const containerWidth = $element.width();
            const availableWidth = containerWidth - inputMinWidth - indicatorWidth;
            for (let i = 0; i < $scope.tokenElements.length; i++) {
                let el = $scope.tokenElements[i];
                width += tokenElementWidths.get(el[0]);
                if (availableWidth - width < 0) {
                    $scope.numberOfVisibleTokens = i;
                    return;
                }
            }
            $scope.numberOfVisibleTokens = $scope.tokenElements.length;
        }

        function getInputMinWidth() {
            const input = $element.find('input');
            const minWidth = input.css('min-width');
            return minWidth ? parseInt(minWidth, 10) : 0;
        }

        this.addToken = function (tokenElement) {
            $scope.tokenElements.push(tokenElement);

            if (!scrollable) {
                const el = tokenElement[0];
                tokenElementWidths.set(el, tokenElement.outerWidth(true));
                tokensRO.observe(el);
                findNumberOfVisibleTokens();
            }
        };

        this.removeToken = function (tokenElement) {
            const index = $scope.tokenElements.indexOf(tokenElement);
            if (index >= 0) {
                $scope.tokenElements.splice(index, 1);

                if (!scrollable) {
                    const el = tokenElement[0];
                    tokenElementWidths.delete(el);
                    tokensRO.unobserve(el);
                    findNumberOfVisibleTokens();
                }
            }
        };

        this.isTokenVisible = function (tokenElement) {
            if (scrollable) return true;
            const index = $scope.tokenElements.indexOf(tokenElement);
            return index < $scope.numberOfVisibleTokens;
        };

        this.getNumberOfHiddenTokens = function () {
            if (scrollable) return 0;

            const count = $scope.tokenElements.length - $scope.numberOfVisibleTokens;
            return count < 0 ? 0 : count;
        };

        $scope.getClasses = () => classNames('fd-tokenizer', {
            'fd-tokenizer--compact': $scope.compact === true,
            'is-focus': $scope.isFocus,
            'fd-tokenizer--scrollable': $scope.scrollable
        });

        if (!scrollable) {
            tokenizerRO = new ResizeObserver(() => {
                $scope.$apply(findNumberOfVisibleTokens);
            });

            tokenizerRO.observe($element[0]);

            tokensRO = new ResizeObserver(entries => {
                for (let entry of entries) {
                    if (entry.contentRect.width > 0)
                        tokenElementWidths.set(entry.target, $(entry.target).outerWidth(true));
                }
                $scope.$apply(findNumberOfVisibleTokens);
            });

            $scope.$on('$destroy', function () {
                tokenizerRO.disconnect();
                tokensRO.disconnect();
            });
        }
    }],
    template: '<div ng-class="getClasses()"><div class="fd-tokenizer__inner" tabindex="-1" ng-transclude></div></div>'
}));