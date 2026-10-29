/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('widget', ['blimpKit']).controller('WidgetController', ($scope, $http) => {
    $http.get('/services/ts/ts-books/api/BooksReport.ts/count').then((response) => $scope.count = response.data.count);
});
