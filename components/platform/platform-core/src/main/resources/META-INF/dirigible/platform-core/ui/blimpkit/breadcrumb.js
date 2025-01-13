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
blimpkit.directive('bkBreadcrumb', () => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: false,
    template: '<ul class="fd-breadcrumb" ng-transclude></ul>',
})).directive('bkBreadcrumbItem', () => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: false,
    template: `<li class="fd-breadcrumb__item" ng-transclude></li>`
}));