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
blimpkit.directive('bkObjectStatus', (classNames) => ({
    restrict: 'A',
    replace: false,
    scope: {
        status: '@?',
        glyph: '@?',
        text: '@?',
        clickable: '<?',
        inverted: '<?',
        indication: '<?',
        large: '<?',
        truncate: '<?',
    },
    controller: ['$scope', '$element', function (scope, element) {
        const statuses = ['negative', 'critical', 'positive', 'informative'];

        this.setIsUploadCollection = function () {
            scope.isUploadCollection = true;
            element.addClass('fd-upload-collection__status-group-item');
        }

        scope.getIconClasses = () => {
            if (!scope.text) element.addClass('fd-object-status--icon-only');
            else element.removeClass('fd-object-status--icon-only');
            return classNames('fd-object-status__icon', scope.glyph);
        };
        scope.getTextClasses = () => classNames('fd-object-status__text', {
            'fd-upload-collection__status-group-item-text': scope.isUploadCollection
        })

        element.addClass('fd-object-status');

        const statusWatch = scope.$watch('status', function (newStatus, oldStatus) {
            if (newStatus && !statuses.includes(newStatus)) {
                console.error(`bk-object-status error: 'status' must be one of: ${statuses.join(', ')}`);
            }
            if (oldStatus) {
                element.removeClass(`fd-object-status--${oldStatus}`);
            }
            if (statuses.includes(newStatus)) {
                element.addClass(`fd-object-status--${newStatus}`);
            }
        });

        const clickableWatch = scope.$watch('clickable', function () {
            const isLink = element[0].tagName === 'A';
            if (scope.clickable || isLink) {
                element.addClass(`fd-object-status--link`);
                if (!isLink)
                    element[0].setAttribute('role', 'button');
            } else {
                element.removeClass(`fd-object-status--link`);
                element[0].removeAttribute('role');
            }
        });

        const invertedWatch = scope.$watch('inverted', function () {
            if (scope.inverted === true) {
                element.addClass('fd-object-status--inverted');
            } else {
                element.removeClass('fd-object-status--inverted');
            }
        });

        const largeWatch = scope.$watch('large', function () {
            if (scope.large) {
                element.addClass('fd-object-status--large');
            } else {
                element.removeClass('fd-object-status--large');
            }
        });

        const truncateWatch = scope.$watch('truncate', function () {
            if (scope.truncate) {
                element.addClass('fd-object-status--truncate');
            } else {
                element.removeClass('fd-object-status--truncate');
            }
        });

        const indicationWatch = scope.$watch('indication', function (indication, oldIndication) {
            if (oldIndication) {
                element.removeClass(`fd-object-status--indication-${oldIndication}`);
            }

            if (indication && (indication < 1 || indication > 8)) {
                console.error(`bk-object-status error: 'indication' must be a number between 1 and 8 inclusive`);
                return;
            }

            element.addClass(`fd-object-status--indication-${indication}`);
        });

        scope.$on('$destroy', function () {
            statusWatch();
            clickableWatch();
            invertedWatch();
            largeWatch();
            truncateWatch();
            indicationWatch();
        });
    }],
    template: '<i ng-if="glyph" ng-class="getIconClasses()" role="presentation"></i><span ng-if="text" ng-class="getTextClasses()">{{text}}</span>'
}));