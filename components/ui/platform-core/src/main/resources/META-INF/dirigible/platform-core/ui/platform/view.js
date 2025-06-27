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
if (typeof viewData === 'undefined' && typeof perspectiveData === 'undefined' && typeof editorData === 'undefined' && typeof shellData === 'undefined') {
    console.error('You must provide one of the following: "viewData", "perspectiveData", "editorData", "shellData"');
} else angular.module('platformView', ['platformExtensions', 'platformTheming', 'platformLocale'])
    .constant('clientOS', { isMac: () => navigator.userAgent.includes('Mac') })
    .factory('baseHttpInterceptor', () => {
        let csrfToken = null;
        return {
            request: (config) => {
                if (config.disableInterceptors) return config;
                config.headers['X-Requested-With'] = 'Fetch';
                config.headers['X-CSRF-Token'] = csrfToken ? csrfToken : 'Fetch';
                return config;
            },
            response: (response) => {
                if (response.config.disableInterceptors) return response;
                const token = response.headers()['x-csrf-token'];
                if (token) {
                    csrfToken = token;
                    uploader.headers['X-CSRF-Token'] = csrfToken;
                }
                return response;
            }
        };
    }).config(['$httpProvider', ($httpProvider) => {
        $httpProvider.interceptors.push('baseHttpInterceptor');
    }]).factory('Views', (Extensions) => {
        let cachedViews;
        let cachedSubviews;
        const errorHandler = (error) => {
            console.error(error);
            reject(error);
        };
        const getViews = (id) => {
            if (cachedViews) {
                return new Promise((resolve) => {
                    if (id) {
                        resolve(cachedViews.find(v => v.id === id));
                    } else resolve(cachedViews);
                });
            } else {
                return Extensions.getViews().then((response) => {
                    cachedViews = response.data;
                    if (id) return cachedViews.find(v => v.id === id);
                    return cachedViews;
                }, errorHandler);
            }
        };
        const getSubviews = (id) => {
            if (cachedSubviews) {
                return new Promise((resolve) => {
                    resolve(cachedSubviews);
                });
            } else {
                return Extensions.getSubviews().then((response) => {
                    cachedSubviews = response.data;
                    if (id) return cachedSubviews.find(v => v.id === id);
                    return cachedSubviews;
                }, errorHandler);
            }
        };
        return {
            getViews: getViews,
            getSubviews: getSubviews,
        };
    }).factory('ViewParameters', () => ({
        get: getViewParameters
    })).directive('embeddedView', (Views, LocaleService) => ({
        restrict: 'E',
        transclude: false,
        replace: true,
        scope: {
            viewId: '@', // String - ID of the view you want to show.
            params: '<?', // JSON - JSON object containing extra parameters/data.
            type: '@?', // String - Type of the view. Available options - 'view' (default), 'subview'.
        },
        link: function (scope) {
            if (scope.params !== undefined && !(typeof scope.params === 'object' && !Array.isArray(scope.params) && scope.params !== null))
                throw Error("embeddedView: params must be an object");
            const types = ['view', 'subview'];
            if (scope.type !== undefined && !types.includes(scope.type))
                throw Error(`embeddedView: wrong view type. Available options - ${types.join(', ')}`);

            function getStandardParams() {
                if (typeof perspectiveData !== 'undefined') return {
                    container: 'perspective',
                    perspectiveId: perspectiveData.id,
                }
                if (typeof shellData !== 'undefined') return {
                    container: 'shell',
                    shellId: shellData.id,
                }
                return {
                    container: 'view',
                };
            }

            function setView(viewConfig) {
                if (viewConfig) {
                    scope.$evalAsync(() => {
                        scope.view = viewConfig;
                        scope.view.params = JSON.stringify({
                            ...viewConfig.params,
                            ...scope.params,
                            ...getStandardParams(),
                        });
                        if (scope.view.translation) LocaleService.onInit(() => {
                            scope.viewLabel = LocaleService.t(scope.view.translation.key, scope.view.translation.options, scope.view.label);
                        });
                        else scope.viewLabel = scope.view.label;
                    });
                } else {
                    throw Error(`embeddedView: ${scope.type ?? types[0]} with id '${scope.viewId}' not found`);
                }
            }

            if (scope.type === types[1]) Views.getSubviews(scope.viewId).then(setView);
            else Views.getViews(scope.viewId).then(setView);
        },
        template: `<iframe title="{{::viewLabel}}" loading="{{::view.lazyLoad ? 'lazy' : 'eager'}}" ng-src="{{::view.path}}" data-parameters="{{::view.params}}"></iframe>`
    })).directive('configIcons', () => ({
        restrict: 'A',
        transclude: false,
        replace: true,
        link: (scope) => { scope.icon = getBrandingInfo().icons.favicon },
        template: '<link rel="icon" type="image/x-icon" ng-href="{{::icon}}">'
    })).directive('shellTitle', (shellState) => ({
        restrict: 'A',
        transclude: false,
        replace: false,
        link: (scope) => {
            scope.perspective = shellState.perspective.label;
            shellState.registerStateListener((data) => {
                scope.perspective = data.label;
            });
            scope.name = getBrandingInfo().name;
        },
        template: '{{perspective || "Loading..."}} | {{::name}}'
    })).directive('configTitle', ($window, LocaleService) => ({
        restrict: 'A',
        transclude: false,
        replace: false,
        link: (scope) => {
            if (typeof viewData !== 'undefined') {
                if (viewData.translation) LocaleService.onInit(() => {
                    scope.label = LocaleService.t(viewData.translation.key, viewData.translation.options, viewData.label);
                });
                else scope.label = viewData.label;
                if (viewData.autoFocusTab !== false) {
                    const layoutHub = new LayoutHub();
                    const onFocus = () => layoutHub.focusView({ id: viewData.id });
                    angular.element($window).on('focus', onFocus);
                    scope.$on('$destroy', () => {
                        angular.element($window).off('focus', onFocus);
                    });
                }
            } else if (typeof perspectiveData !== 'undefined') {
                if (perspectiveData.translation) LocaleService.onInit(() => {
                    scope.label = LocaleService.t(perspectiveData.translation.key, perspectiveData.translation.options, perspectiveData.label);
                });
                else scope.label = perspectiveData.label;
            } else if (typeof editorData !== 'undefined') {
                if (editorData.translation) LocaleService.onInit(() => {
                    scope.label = LocaleService.t(editorData.translation.key, editorData.translation.options, editorData.label);
                });
                else scope.label = editorData.label;
                if ($window.frameElement && $window.frameElement.hasAttribute('tab-id')) {
                    const layoutHub = new LayoutHub();
                    const tabId = $window.frameElement.getAttribute('tab-id');
                    const { filePath } = getViewParameters({ vframe: $window });
                    const onFocus = () => layoutHub.focusEditor({ id: tabId, path: filePath });
                    angular.element($window).on('focus', onFocus);
                    scope.$on('$destroy', () => {
                        angular.element($window).off('focus', onFocus);
                    });
                }
            } else throw Error('configTitle: missing view data');
        },
        template: '{{::label}}'
    }));