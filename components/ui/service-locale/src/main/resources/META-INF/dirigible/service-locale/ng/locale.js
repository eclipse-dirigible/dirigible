
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
angular.module('LocaleService', []).provider('LocaleService', function LocaleServiceProvider() {
    let localePath = '';
    let loaded = false;
    this.debug = false;
    this.setPath = (path) => {
        localePath = path;
        i18next.use(i18nextHttpBackend).init({
            lng: 'en-US',
            supportedLngs: ['en-US', 'bg-BG'],
            fallbackLng: 'en-US',
            load: 'currentOnly',
            preload: ['en-US'],
            debug: this.debug,
            ns: ['welcome'],
            maxRetries: 1,
            backend: {
                loadPath: `/services/js/service-locale/locale.js?path=${localePath}&lng={{lng}}&ns={{ns}}`
            }
        }, () => {
            loaded = true;
        });
    };
    this.getPath = () => {
        return localePath;
    };
    this.getTranslation = (key) => {
        if (loaded) return i18next.t(key);
    };
    this.$get = function localeFactory() {
        return {
            setPath: this.setPath,
            getPath: this.getPath,
            isLoaded: () => loaded,
            getTranslation: this.getTranslation,
        };
    };
}).filter('t', (LocaleService) => {
    function localesLoaded(key) {
        return LocaleService.getTranslation(key);
    }
    localesLoaded.$stateful = true;
    return localesLoaded;
});