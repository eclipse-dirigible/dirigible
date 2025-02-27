/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const clientRegistrationsView = angular.module('clientRegistrations', ['platformView', 'blimpKit']);
clientRegistrationsView.constant('Dialogs', new DialogHub());
clientRegistrationsView.controller('ClientRegistrationsController', ($scope, $http, Dialogs, ButtonStates) => {
    $scope.listClientRegistrations = () => {
        $http.get('/services/security/client-registrations').then((response) => {
            $scope.list = response.data;
        });
    };
    $scope.listClientRegistrations();

    $scope.newClientRegistration = () => {
        Dialogs.showWindow({
            hasHeader: true,
            id: 'client-registration-create-edit',
            params: { editMode: false },
            closeButton: false,
            maxWidth: '600px',
            // maxHeight: '240px'
        });
    };

    Dialogs.addMessageListener({
        topic: 'ide-security.client-registration.create',
        handler: (data) => {
            $http.post('/services/security/client-registrations', JSON.stringify(data)).then(() => {
                $scope.listClientRegistrations();
                // Dialogs.triggerEvent('ide-security.explorer.refresh');
            }, (response) => {
                console.error(response);
                Dialogs.showAlert({
                    title: 'Error while creating client registration',
                    message: response.message ?? 'Please look at the console for more information',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            });
            Dialogs.closeWindow();
        }
    });

    $scope.editClientRegistration = (clientRegistration) => {
        $scope.clientRegistration = {
            id: clientRegistration.id,
            name: clientRegistration.name,
            clientId: clientRegistration.clientId,
            clientSecret: clientRegistration.clientSecret,
            redirectUri: clientRegistration.redirectUri,
            authorizationGrantType: clientRegistration.authorizationGrantType,
            scope: clientRegistration.scope,
            tokenUri: clientRegistration.tokenUri,
            authorizationUri: clientRegistration.authorizationUri,
            userInfoUri: clientRegistration.userInfoUri,
            issuerUri: clientRegistration.issuerUri,
            jwkSetUri: clientRegistration.jwkSetUri,
            userNameAttributeName: clientRegistration.userNameAttributeName
        };
        Dialogs.showWindow({
            hasHeader: true,
            id: 'client-registration-create-edit',
            params: {
                editMode: true,
                clientRegistration: {
                    id: '',
                    name: clientRegistration.name,
                    clientId: clientRegistration.clientId,
                    clientSecret: clientRegistration.clientSecret,
                    redirectUri: clientRegistration.redirectUri,
                    authorizationGrantType: clientRegistration.authorizationGrantType,
                    scope: clientRegistration.scope,
                    tokenUri: clientRegistration.tokenUri,
                    authorizationUri: clientRegistration.authorizationUri,
                    userInfoUri: clientRegistration.userInfoUri,
                    issuerUri: clientRegistration.issuerUri,
                    jwkSetUri: clientRegistration.jwkSetUri,
                    userNameAttributeName: clientRegistration.userNameAttributeName
                }
            },
            closeButton: false,
            maxWidth: '600px',
            // maxHeight: '240px'
        });
    };

    Dialogs.addMessageListener({
        topic: 'ide-security.client-registration.edit',
        handler: (data) => {
            let clientRegistration = data;
            clientRegistration.name = $scope.clientRegistration.name;
            $http.put('/services/security/client-registrations/' + $scope.clientRegistration.id, JSON.stringify(clientRegistration)).then(() => {
                $scope.listClientRegistrations();
                // Dialogs.triggerEvent('ide-security.explorer.refresh');
            }, (response) => {
                console.error(response);
                Dialogs.showAlert({
                    title: 'Error while updating client registration',
                    message: response.message ?? 'Please look at the console for more information',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            });
            Dialogs.closeWindow();
        }
    });

    $scope.deleteClientRegistration = (clientRegistration) => {
        $scope.clientRegistration = {
            id: clientRegistration.id
        };
        Dialogs.showDialog({
            title: 'Delete Client Registration',
            message: 'Are you sure you want to delete the selected client registration?',
            buttons: [
                { id: 'b1', label: 'Delete', state: ButtonStates.Negative },
                { id: 'b3', label: 'Cancel', state: ButtonStates.Transparent },
            ]
        }).then((buttonId) => {
            if (buttonId === 'b1') {
                $http.delete('/services/security/client-registrations/' + $scope.clientRegistration.id)
                    .then(() => {
                        $scope.listClientRegistrations();
                        // Dialogs.triggerEvent('ide-security.explorer.refresh');
                    }, (response) => {
                        console.error(response.data);
                        Dialogs.showAlert({
                            title: 'Error while deleting client registration',
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
                message: 'Error while deleting client registration.\nPlease look at the console for more information.',
                type: AlertTypes.Error,
                preformatted: true,
            });
        });
    };

    $scope.showAccessUrl = (clientRegistration) => {
        Dialogs.showAlert({
            title: 'Access URL',
            message: `Access URL: '${window.location.protocol}//${window.location.host}/oauth2/authorization/${clientRegistration.name}'`,
            type: AlertTypes.Information,
            preformatted: false,
        });
    };
});