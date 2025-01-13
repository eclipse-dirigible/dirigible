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
angular.module('ideView', ['ngResource', 'ideExtensions', 'ideTheming'])
    .constant('view', (typeof viewData != 'undefined') ? viewData : (typeof editorData != 'undefined' ? editorData : ''))
    .constant('extensionPoint', {})
    .factory('baseHttpInterceptor', function () {
        let csrfToken = null;
        return {
            request: function (config) {
                if (config.disableInterceptors) return config;
                config.headers['X-Requested-With'] = 'Fetch';
                config.headers['X-CSRF-Token'] = csrfToken ? csrfToken : 'Fetch';
                return config;
            },
            response: function (response) {
                if (response.config.disableInterceptors) return response;
                let token = response.headers()['x-csrf-token'];
                if (token) {
                    csrfToken = token;
                    uploader.headers['X-CSRF-Token'] = csrfToken;
                }
                return response;
            }
        };
    }).factory('Views', ['Extensions', 'extensionPoint', function (Extensions, extensionPoint) {
        let cachedViews;
        let cachedSubviews;
        let get = function () {
            if (cachedViews) {
                return cachedViews;
            } else {
                return Extensions.get('view', extensionPoint.views).then(function (data) {
                    data = data.map(function (v) {
                        if (!v.id) {
                            console.error(`Views: view '${v.label || 'undefined'}' does not have an id`);
                            return;
                        }
                        if (!v.label) {
                            console.error(`Views: view '${v.id}' does not have a label`);
                            return;
                        }
                        if (!v.link) {
                            console.error(`Views: view '${v.id}' does not have a link`);
                            return;
                        }
                        v.factory = v.factory || 'frame';
                        v.settings = {
                            path: v.link,
                            loadType: (v.lazyLoad ? 'lazy' : 'eager'),
                        };
                        v.region = v.region || 'left';
                        return v;
                    });
                    cachedViews = data;
                    return data;
                });
            }
        };
        let getSubviews = function () {
            if (cachedSubviews) {
                return cachedSubviews;
            } else {
                return Extensions.get('subview', extensionPoint.subviews).then(function (data) {
                    data = data.map(function (v) {
                        if (!v.id) {
                            console.error(`Subviews: view '${v.label || 'undefined'}' does not have an id`);
                            return;
                        }
                        if (!v.label) {
                            console.error(`Subviews: view '${v.id}' does not have a label`);
                            return;
                        }
                        if (!v.link) {
                            console.error(`Subviews: view '${v.id}' does not have a link`);
                            return;
                        }
                        v.settings = {
                            path: v.link,
                            loadType: (v.lazyLoad ? 'lazy' : 'eager'),
                        };
                        return v;
                    });
                    cachedSubviews = data;
                    return data;
                });
            }
        };
        return {
            get: get,
            getSubviews: getSubviews
        };
    }]).config(['$httpProvider', function ($httpProvider) {
        $httpProvider.interceptors.push('baseHttpInterceptor');
    }]).factory('ViewParameters', ['$window', function ($window) {
        return {
            get: function () {
                if ($window.frameElement && $window.frameElement.hasAttribute("data-parameters")) {
                    return JSON.parse($window.frameElement.getAttribute("data-parameters"));
                }
                return {};
            }
        };
    }]).service('Subviews', ['Views', function (Views) {
        return {
            getIdList: function (startsWith) {
                return new Promise((resolve, reject) => {
                    Views.getSubviews().then(
                        function (subviews) {
                            let idList = [];
                            if (startsWith) {
                                for (let i = 0; i < subviews.length; i++) {
                                    if (subviews[i].id.startsWith(startsWith)) idList.push(subviews[i].id);
                                }
                            } else {
                                for (let i = 0; i < subviews.length; i++) {
                                    idList.push(subviews[i].id);
                                }
                            }
                            resolve(idList);
                        },
                        function (error) {
                            console.error(error);
                            reject(error);
                        }
                    );
                });
            }
        };
    }]).directive('embeddedView', ['Views', 'view', function (Views, view) {
        /**
         * viewId: String - ID of the view you want to show.
         * params: JSON - JSON object containing extra parameters/data.
         * dgType: String - Type of the view. Available options - 'view' (default) and 'subview'.
         */
        return {
            restrict: 'E',
            transclude: false,
            replace: true,
            scope: {
                viewId: '@',
                params: '<',
                dgType: '@?',
            },
            link: {
                pre: function (scope) {
                    if (scope.params !== undefined && !(typeof scope.params === 'object' && !Array.isArray(scope.params) && scope.params !== null))
                        throw Error("embeddedView: view-parameters must be an object");

                    function getView(views) {
                        const embeddedView = views.find(v => v.id === scope.viewId);
                        if (embeddedView) {
                            scope.path = embeddedView.settings.path;
                            scope.loadType = embeddedView.settings.loadType;
                            scope.parameters = {
                                container: 'embedded',
                                viewId: view.id
                            };
                            if (embeddedView.params) {
                                scope.parameters = {
                                    ...scope.parameters,
                                    ...embeddedView.params,
                                };
                            }
                            if (scope.params) {
                                scope.parameters = {
                                    ...scope.parameters,
                                    ...scope.params,
                                }
                            }
                        } else {
                            if (scope.dgType === 'subview')
                                throw Error(`embeddedView: subview with id '${scope.viewId}' not found`);
                            else throw Error(`embeddedView: view with id '${scope.viewId}' not found`);
                        }
                    }

                    if (scope.dgType === 'subview') Views.getSubviews().then((views) => (getView(views)));
                    else Views.get().then((views) => (getView(views)));

                    scope.getParams = function () {
                        return JSON.stringify(scope.parameters);
                    };
                },
            },
            template: '<iframe loading="{{loadType}}" ng-src="{{path}}" data-parameters="{{getParams()}}"></iframe>'
        }
    }]).directive('dgContextmenu', ['messageHub', '$window', function (messageHub, $window) {
        return {
            restrict: 'A',
            replace: false,
            scope: {
                callback: '&dgContextmenu',
                includedElements: '=',
                excludedElements: '=',
            },
            link: function (scope, element) {
                scope.callback = scope.callback();
                element.on('contextmenu', function (event) {
                    if (scope.includedElements) {
                        let isIncluded = false;
                        if (scope.includedElements.ids && scope.includedElements.ids.includes(event.target.id)) isIncluded = true;
                        if (!isIncluded && scope.includedElements.classes) {
                            for (let i = 0; i < scope.includedElements.classes.length; i++) {
                                if (event.target.classList.contains(scope.includedElements.classes[i]))
                                    isIncluded = true;
                            }
                        }
                        if (!isIncluded && scope.includedElements.types && scope.includedElements.types.includes(event.target.tagName)) isIncluded = true;
                        if (!isIncluded) return;
                    } else if (scope.excludedElements) {
                        if (scope.excludedElements.ids && scope.excludedElements.ids.includes(event.target.id)) return;
                        if (scope.excludedElements.classes) {
                            for (let i = 0; i < scope.excludedElements.classes.length; i++) {
                                if (event.target.classList.contains(scope.excludedElements.classes[i])) return;
                            }
                        }
                        if (scope.excludedElements.types && scope.excludedElements.types.includes(event.target.tagName)) return;
                    }
                    event.preventDefault();
                    let menu = scope.callback(event.target);
                    if (menu) {
                        let posX;
                        let posY;
                        if ($window.frameElement) {
                            let frame = $window.frameElement.getBoundingClientRect();
                            posX = frame.x + event.clientX;
                            posY = frame.y + event.clientY;
                        } else {
                            posX = event.clientX;
                            posY = event.clientY;
                        }
                        messageHub.postMessage(
                            'ide-contextmenu.open',
                            {
                                posX: posX,
                                posY: posY,
                                callbackTopic: menu.callbackTopic,
                                hasIcons: menu.hasIcons || false,
                                items: menu.items
                            },
                            true
                        );
                        const onCloseHandler = messageHub.onDidReceiveMessage(
                            'ide-contextmenu.close',
                            function () {
                                if ($window.frameElement) {
                                    $window.frameElement.contentWindow.focus();
                                }
                                messageHub.unsubscribe(onCloseHandler);
                            },
                            true
                        );
                    }
                });
            }
        };
    }]).directive('dgViewTitle', ['view', function (view) {
        return {
            restrict: 'A',
            transclude: false,
            replace: true,
            link: function (scope) {
                if (!view)
                    throw Error("dgViewTitle: Not in view, missing data");
                scope.label = view.label;
            },
            template: '<title>{{label}}</title>'
        };
    }]);
