
/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('platformLocale', []).provider('LocaleService', function LocaleServiceProvider() {
    if (!top.hasOwnProperty('i18next')) throw Error('LocaleService: i18next is not loaded');
    this.defaultLanguage = 'en-US';
    this.namespaces = [];
    const callbacksListeners = [];
    this.$get = ['$rootScope', 'Extensions', function localeFactory($rootScope, Extensions) {
        const language = this.defaultLanguage;
        const initialize = (locales, resources) => {
            i18next['locales'] = locales;
            i18next.init({
                lng: language,
                fallbackLng: language,
                load: 'currentOnly',
                debug: false,
                defaultNS: 'common',
                interpolation: {
                    skipOnVariables: false
                },
                resources: resources
            }).then((_, err) => {
                if (err) console.error(err);
                $rootScope.$applyAsync(() => {
                    for (let l = 0; l < callbacksListeners.length; l++) {
                        callbacksListeners[l]();
                    }
                    callbacksListeners.length = 0;
                });
            });
        };
        if (window === top) {
            Extensions.getTranslations({
                langs: language,
                namespaces: this.namespaces
            }).then((response) => {
                initialize(response.data.locales, response.data.translations);
            }, (error) => {
                console.error(error);
                initialize([], {});
            });
        }
        return {
            getLanguage: () => language,
            getLanguages: () => top.i18next.locales ?? [],
            t: (key, options, fallback) => {
                const keyOptions = angular.isDefined(options) ? options : fallback;
                return top.i18next.t(key ?? '', keyOptions);
            },
            onInit: (callback) => {
                if (top.i18next.isInitialized) callback();
                else callbacksListeners.push(callback);
            },
        };
    }];
}).filter('t', ['LocaleService', (LocaleService) => {
    function filter(key, options, fallback) {
        const keyOptions = angular.isDefined(options) ? options : fallback;
        return LocaleService.t(key ?? '', keyOptions);
    }
    filter.$stateful = true;
    return filter;
}]);