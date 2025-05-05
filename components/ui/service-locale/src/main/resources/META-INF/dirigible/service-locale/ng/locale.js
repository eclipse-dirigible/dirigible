
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
    let localePath = viewData.localePath ?? '';
    let localeNs = viewData.localeNs ?? ''
    let loaded = false;
    this.debug = false;
    function init() {
        i18next.use(i18nextHttpBackend).init({
            lng: 'en-US',
            supportedLngs: ['en-US', 'bg-BG'],
            fallbackLng: 'en-US',
            load: 'currentOnly',
            preload: ['en-US'],
            debug: this.debug,
            ns: localeNs,
            maxRetries: 1,
            backend: {
                loadPath: `/services/js/service-locale/locale.js?path=${localePath}&lng={{lng}}&ns={{ns}}`
            }
        }, (_, getError) => {
            if (getError()) console.error(getError());
            loaded = true;
        });
    }
    this.setPath = (path) => {
        localePath = path;
        loaded = false;
        init();
    };
    this.getPath = () => {
        return localePath;
    };
    this.setNs = (ns) => {
        localeNs = ns;
        loaded = false;
        init();
    };
    this.getNs = () => {
        return localeNs;
    };
    this.getTranslation = (key) => {
        if (loaded) return i18next.t(key);
    };
    this.$get = function localeFactory() {
        init();
        return {
            setPath: this.setPath,
            getPath: this.getPath,
            setNs: this.setNs,
            getNs: this.getNs,
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