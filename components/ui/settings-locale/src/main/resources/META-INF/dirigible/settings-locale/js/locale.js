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
angular.module('locale', ['blimpKit', 'platformView', 'platformLocale']).controller('LocaleController', ($scope, LocaleService, ButtonStates) => {
    const dialogHub = new DialogHub();
    LocaleService.onInit(() => {
        $scope.language = LocaleService.getLanguage();
        $scope.languages = LocaleService.getLanguages();
    });
    $scope.setLang = (lang) => {
        if (lang !== $scope.language) {
            $scope.language = lang;
            try {
                LocaleService.changeLanguage(lang);
                dialogHub.showDialog({
                    title: LocaleService.t('settings-locale:langChanged', 'Language changed'),
                    message: LocaleService.t('settings-locale:langChangedReload', 'Changes will take effect after a refresh'),
                    buttons: [
                        { id: 'ref', label: LocaleService.t('refresh', 'Refresh'), state: ButtonStates.Emphasized },
                        { id: 'later', label: LocaleService.t('settings-locale:later', 'Later') }
                    ],
                    closeButton: false,
                }).then((buttonId) => {
                    if (buttonId === 'ref') top.window.location.reload();
                });
            } catch (err) {
                if (err.notRegistered) {
                    const message = LocaleService.t('settings-locale:errMsg.notRegistered', 'Please look at the console for more information');
                    console.error(message);
                    dialogHub.showAlert({
                        title: LocaleService.t('settings-locale:errMsg.langChangeTitle', 'Unable to change language'),
                        message: message,
                        type: AlertTypes.Error,
                        preformatted: false,
                    });
                } else {
                    console.error(err);
                    dialogHub.showAlert({
                        title: LocaleService.t('settings-locale:errMsg.langChangeTitle', 'Unable to change language'),
                        message: LocaleService.t('settings-locale:errMsg.langChange', 'Please look at the console for more information'),
                        type: AlertTypes.Error,
                        preformatted: false,
                    });
                }
            }
        }
    };
});