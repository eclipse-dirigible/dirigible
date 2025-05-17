
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
angular.module('platformLocale', []).provider('LocaleService', function LocaleServiceProvider() {
    if (!top.hasOwnProperty('i18next')) throw Error('LocaleService: i18next is not loaded');
    const callbacksListeners = [];
    const initListeners = []
    const storageKey = `${getBrandingInfo().prefix}.locale.language`;
    let savedLanguage = localStorage.getItem(storageKey);
    if (!savedLanguage) {
        localStorage.setItem(storageKey, 'en-US');
        savedLanguage = 'en-US';
    }
    const init = (translations) => {
        if (!top.i18next.isInitialized) return top.i18next.init({
            lng: savedLanguage,
            load: 'currentOnly',
            fallbackLng: 'en-US',
            debug: false,
            defaultNS: 'common',
            resources: translations
        });
        else return new Promise((resolve, _) => resolve(savedLanguage));
    };
    this.$get = ['$rootScope', '$http', 'Extensions', function localeFactory($rootScope, $http, Extensions) {
        const changeListener = () => {
            $rootScope.$applyAsync(() => {
                for (let l = 0; l < callbacksListeners.length; l++) {
                    callbacksListeners[l]();
                }
            });
        };
        if (!top.i18next['loadingTranslations']) {
            top.i18next['loadingTranslations'] = true;
            Extensions.getLocales().then((response) => {
                top.i18next['locales'] = response.data;
                let langPath = '/services/web';
                for (let l = 0; l < top.i18next.locales.length; l++) {
                    if (top.i18next.locales[l].id === savedLanguage) langPath += top.i18next.locales[l].path;
                }
                $http.get(langPath).then((translations) => {
                    init({ [savedLanguage]: translations.data }).then((_, err) => {
                        if (err) console.error(err);
                        top.i18next.on('languageChanged', changeListener);
                        $rootScope.$applyAsync(() => {
                            for (let l = 0; l < callbacksListeners.length; l++) {
                                callbacksListeners[l]();
                            }
                            for (let l = 0; l < initListeners.length; l++) {
                                initListeners[l]();
                            }
                            initListeners.length = 0;
                        });
                    });
                }, (error) => {
                    console.error(error);
                });
            }, (error) => {
                console.error(error);
            });
        }
        return {
            changeLanguage: (lang) => {
                return new Promise((resolve, reject) => {
                    if (savedLanguage !== lang && top.i18next.locales.find((locale) => locale.id === lang)) {
                        top.i18next.changeLanguage(lang, (err) => {
                            if (err) reject(err);
                            else {
                                resolve(lang);
                                localStorage.setItem(storageKey, lang);
                                savedLanguage = lang;
                            }
                        });
                    } else reject(`Cannot set language '${lang}' as it's not registered`);
                });
            },
            getLanguage: () => savedLanguage,
            getLanguages: () => top.i18next.locales ?? [],
            t: top.i18next.t,
            onLoad: (callback) => {
                callbacksListeners.push(callback);
                if (top.i18next.isInitialized) callback();
            },
            onInit: (callback) => {
                if (top.i18next.isInitialized) callback();
                else initListeners.push(callback);
            },
        };
    }];
}).filter('t', (LocaleService) => {
    function filter(key, options, fallback) {
        const localOptions = angular.isDefined(options) ? options : fallback;
        return LocaleService.t(key ?? '', localOptions);
    }
    filter.$stateful = true;
    return filter;
});