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
angular.module('locale', ['blimpKit', 'platformView', 'platformLocale']).controller('LocaleController', ($scope, LocaleService) => {
    const Dialog = new DialogHub();
    LocaleService.onLoad(() => {
        $scope.language = LocaleService.getLanguage();
        $scope.languages = LocaleService.getLanguages();
    });
    $scope.setLang = (lang) => {
        $scope.language = lang;
        LocaleService.changeLanguage(lang).catch((err) => {
            console.error(err);
            Dialog.showAlert({
                title: LocaleService.t('settings-locale:errMsg.langChangeTitle', 'Unable to change language'),
                message: LocaleService.t('settings-locale:errMsg.langChange', 'Plase look at the console for more information'),
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };
});