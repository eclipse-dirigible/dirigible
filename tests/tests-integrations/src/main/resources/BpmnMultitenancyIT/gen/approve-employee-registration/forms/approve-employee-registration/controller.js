/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('forms', ['blimpKit', 'platformView']).controller('FormController', ($scope, $http, ViewParameters) => {
    $scope.forms = {
        form: {}
    };

    $scope.model = {};

    let url = new URL(window.location);
    let params = new URLSearchParams(url.search);
    let taskId = params.get("taskId");
    
    $scope.onApproveClicked = function () {
        const url = `/services/ts/BpmnMultitenancyIT/ProcessService.ts/requests/${taskId}/approve`;
        $http.put(url)
            .then(function (response) {
            if (response.status != 200) {
                alert(`Unable to approve request: '${response.message}'`);
                return;
            }
            $scope.entity = {};
            alert("Request Approved");
        });
    };
    
    $scope.onDeclineClicked = function () {
        const url = `/services/ts/BpmnMultitenancyIT/ProcessService.ts/requests/${taskId}/decline`;
        $http.put(url)
            .then(function (response) {
            if (response.status != 200) {
                alert(`Unable to decline request: '${response.message}'`);
                return;
            }
            $scope.entity = {};
            alert("Request Declined");
        });
    
    };
    
    

});