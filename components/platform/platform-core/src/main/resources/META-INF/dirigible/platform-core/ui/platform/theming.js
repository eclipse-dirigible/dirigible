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
angular.module('platformTheming', ['platformExtensions'])
    .constant('ThemeHub', new ThemingHub())
    .provider('theming', function ThemingProvider() {
        this.$get = ['ThemeHub', 'Extensions', function editorsFactory(ThemeHub, Extensions) {
            let theme = ThemeHub.getSavedTheme();
            let themes = [];

            Extensions.getThemes().then((response) => {
                themes = response.data;
                if (!theme.version) {
                    setTheme('blimpkit-auto');
                } else {
                    for (let i = 0; i < themes.length; i++) {
                        if (themes[i].id === theme.id) {
                            if (themes[i].version !== theme.version) {
                                setThemeObject(themes[i]);
                                break;
                            }
                        }
                    }
                }
                ThemeHub.themesLoaded();
            });

            function setTheme(themeId, sendEvent = true) {
                for (let i = 0; i < themes.length; i++) {
                    if (themes[i].id === themeId) {
                        setThemeObject(themes[i], sendEvent);
                    }
                }
            }

            function setThemeObject(themeObj, sendEvent = true) {
                ThemeHub.setSavedTheme(themeObj);
                theme = themeObj;
                if (sendEvent) ThemeHub.themeChanged({
                    id: themeObj.id,
                    type: themeObj.type,
                    links: themeObj.links
                });
            }

            return {
                setTheme: setTheme,
                getThemes: () => themes.map((item) => ({
                    'id': item['id'],
                    'name': item['name']
                })),
                getCurrentTheme: () => ({
                    id: theme['id'] || 'blimpkit-auto',
                    name: theme['name'] || 'BlimpKit',
                }),
                reset: () => {
                    // setting sendEvent to false because the application will reload anyway
                    setTheme('blimpkit-auto', false);
                }
            }
        }];
    })
    .factory('Theme', ['theming', 'ThemeHub', function (_theming, ThemeHub) { // theming must be injected to set defaults
        let theme = ThemeHub.getSavedTheme();
        return {
            reload: () => {
                theme = ThemeHub.getSavedTheme();
            },
            getLinks: () => {
                return theme.links || [];
            },
            getType: () => {
                return theme.type || 'auto';
            },
            getTheme: () => {
                return theme;
            },
        }
    }]).directive('theme', (Theme, ThemeHub) => ({
        restrict: 'E',
        replace: true,
        transclude: false,
        link: (scope) => {
            scope.links = Theme.getLinks();
            const themeChangeListener = ThemeHub.onThemeChange((themeData) => {
                scope.$applyAsync(() => {
                    scope.links = themeData.links;
                });
            });
            scope.$on('$destroy', () => {
                ThemeHub.removeMessageListener(themeChangeListener);
            });
        },
        template: '<link type="text/css" rel="stylesheet" ng-repeat="link in links" ng-href="{{ link }}">'
    }));