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
blimpkit.directive('bkTitle', function () {
    /**
     * headerSize: Number - If specified overrides the size of the heading element. Must be a number between 1 and 6 inclusive
     * wrap: Boolean - Whether or not the title should wrap
     */
    return {
        restrict: 'A',
        scope: {
            headerSize: '<',
            wrap: '<?'
        },
        link: function (scope, element) {
            element.addClass('fd-title');
            if (scope.wrap) element.addClass(`fd-title--wrap`);

            function setHeaderSize(_newSize, oldSize) {
                if (scope.headerSize) {
                    if (scope.headerSize >= 1 && scope.headerSize <= 6) {
                        if (oldSize) element.removeClass(`fd-title--h${oldSize}`);
                        element.addClass(`fd-title--h${scope.headerSize}`);
                    } else console.error(`bk-title error: 'header-size' must be a number between 1 and 6 inclusive`);
                }
            }
            const headerSizeWatcher = scope.$watch('headerSize', setHeaderSize);
            scope.$on('$destroy', function () {
                headerSizeWatcher();
            });
        }
    }
});