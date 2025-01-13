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
const tenantsView = angular.module('tenants', ['ideUI', 'ideView']);

tenantsView.config(["messageHubProvider", function (messageHubProvider) {
    messageHubProvider.eventIdPrefix = 'tenants-view';
}]);

tenantsView.controller('TenantsController', ['$scope', '$http', 'messageHub', function ($scope, $http, messageHub) {

    $scope.listTenants = function () {
        $http.get('/services/security/tenants').then(function (response) {
            $scope.list = response.data;
        });
    }
    $scope.listTenants();

    $scope.newTenant = function () {
        messageHub.showDialogWindow(
            "tenant-create-edit",
            { editMode: false },
            null,
            false
        );
    };

    messageHub.onDidReceiveMessage(
        'ide-security.tenant.create',
        function (msg) {
            if (msg.data) {
                $http.post(
                    '/services/security/tenants',
                    JSON.stringify(msg.data)
                ).then(function () {
                    $scope.listTenants();
                    messageHub.triggerEvent('ide-security.explorer.refresh', true);
                }, function (response) {
                    console.error(response);
                    messageHub.showAlertError('Error while creating tenant', 'Please look at the console for more information');
                });
            }
            messageHub.closeDialogWindow('tenant-create-edit');
        },
        true
    );

    $scope.editTenant = function (tenant) {
        $scope.tenant = {
			id: tenant.id,
            name: tenant.name,
            subdomain: tenant.subdomain
        };
        messageHub.showDialogWindow(
            "tenant-create-edit",
            {
                editMode: true,
                tenant: {
					id: '',
                    name: tenant.name,
                    subdomain: tenant.subdomain
                }
            },
            null,
            false
        );
    };

    messageHub.onDidReceiveMessage(
        'ide-security.tenant.edit',
        function (msg) {
            if (msg.data) {
                let tenant = msg.data;
                tenant.name = $scope.tenant.name;
                $http.put('/services/security/tenants/' + $scope.tenant.id, JSON.stringify(tenant))
                    .then(function () {
                        $scope.listTenants();
                        messageHub.triggerEvent('ide-security.explorer.refresh', true);
                    }, function (response) {
                        console.error(response);
                        messageHub.showAlertError('Error while updating tenant', 'Please look at the console for more information');
                    });
            }
            messageHub.closeDialogWindow('tenant-create-edit');
        },
        true
    );

    $scope.deleteTenant = function (tenant) {
        $scope.tenant = {
            id: tenant.id
        };

        messageHub.showDialog(
            'Delete Tenant',
            'Are you sure you want to delete the selected tenant?',
            [{
                id: 'btnOK',
                type: 'emphasized',
                label: 'OK',
            },
            {
                id: 'btnCancel',
                type: 'transparent',
                label: 'Cancel',
            }],
            'ide-security.tenant.delete'
        );
    }

    messageHub.onDidReceiveMessage(
        'ide-security.tenant.delete',
        function (msg) {
            if (msg.data === 'btnOK' && $scope.tenant.id) {
                $http.delete('/services/security/tenants/' + $scope.tenant.id)
                    .then(function () {
                        $scope.listTenants();
                        messageHub.triggerEvent('ide-security.explorer.refresh', true);
                    }, function (response) {
                        console.error(response.data);
                        messageHub.showAlertError('Error while deleting tenant', 'Please look at the console for more information');
                    });
            }
        },
        true
    );

}]);