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
const tenantsView = angular.module('tenants', ['platformView', 'blimpKit']);
tenantsView.constant('Dialogs', new DialogHub());
tenantsView.controller('TenantsController', ($scope, $http, Dialogs, ButtonStates) => {
    $scope.listTenants = () => {
        $http.get('/services/security/tenants').then((response) => {
            $scope.list = response.data;
        });
    };
    $scope.listTenants();

    $scope.newTenant = () => {
        Dialogs.showWindow({
            hasHeader: true,
            id: 'tenant-create-edit',
            params: { editMode: false },
            closeButton: false,
            maxWidth: '400px',
            maxHeight: '240px'
        });
    };

    Dialogs.addMessageListener({
        topic: 'ide-security.tenant.create',
        handler: (data) => {
            $http.post('/services/security/tenants', JSON.stringify(data)).then(() => {
                $scope.listTenants();
                // Dialogs.triggerEvent('ide-security.explorer.refresh');
            }, (response) => {
                console.error(response);
                Dialogs.showAlert({
                    title: 'Error while creating tenant',
                    message: response.message ?? 'Please look at the console for more information',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            });
            Dialogs.closeWindow();
        }
    });

    $scope.editTenant = (tenant) => {
        $scope.tenant = {
            id: tenant.id,
            name: tenant.name,
            subdomain: tenant.subdomain
        };
        Dialogs.showWindow({
            hasHeader: true,
            id: 'tenant-create-edit',
            params: {
                editMode: true,
                tenant: {
                    id: '',
                    name: tenant.name,
                    subdomain: tenant.subdomain
                }
            },
            closeButton: false,
            maxWidth: '400px',
            maxHeight: '240px'
        });
    };

    Dialogs.addMessageListener({
        topic: 'ide-security.tenant.edit',
        handler: (data) => {
            let tenant = data;
            tenant.name = $scope.tenant.name;
            $http.put('/services/security/tenants/' + $scope.tenant.id, JSON.stringify(tenant)).then(() => {
                $scope.listTenants();
                // Dialogs.triggerEvent('ide-security.explorer.refresh');
            }, (response) => {
                console.error(response);
                Dialogs.showAlert({
                    title: 'Error while updating tenant',
                    message: response.message ?? 'Please look at the console for more information',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            });
            Dialogs.closeWindow();
        }
    });

    $scope.deleteTenant = (tenant) => {
        $scope.tenant = {
            id: tenant.id
        };
        Dialogs.showDialog({
            title: 'Delete Tenant',
            message: 'Are you sure you want to delete the selected tenant?',
            buttons: [
                { id: 'b1', label: 'Delete', state: ButtonStates.Negative },
                { id: 'b3', label: 'Cancel', state: ButtonStates.Transparent },
            ]
        }).then((buttonId) => {
            if (buttonId === 'b1') {
                $http.delete('/services/security/tenants/' + $scope.tenant.id)
                    .then(() => {
                        $scope.listTenants();
                        // Dialogs.triggerEvent('ide-security.explorer.refresh');
                    }, (response) => {
                        console.error(response.data);
                        Dialogs.showAlert({
                            title: 'Error while deleting tenant',
                            message: response.message ?? 'Please look at the console for more information',
                            type: AlertTypes.Error,
                            preformatted: false,
                        });
                    });
            }
        }, (error) => {
            console.error(error);
            Dialogs.showAlert({
                title: 'Delete error',
                message: 'Error while deleting tenant.\nPlease look at the console for more information.',
                type: AlertTypes.Error,
                preformatted: true,
            });
        });
    };
});