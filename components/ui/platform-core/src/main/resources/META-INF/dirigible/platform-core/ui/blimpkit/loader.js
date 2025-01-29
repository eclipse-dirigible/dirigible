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
blimpkit.directive('bkLoader', (classNames) => ({
    restrict: 'E',
    transclude: false,
    replace: true,
    scope: {
        size: '@?',
        contrast: '<?'
    },
    link: (scope) => {
        scope.getClasses = () => classNames('bk-loader', {
            'bk-loader--l': scope.size === 'l',
            'bk-loader--contrast': scope.contrast === true,
        });
    },
    template: '<div ng-class="getClasses()"></div>'
}));