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
/*
 * Provides key microservices for constructing and managing the IDE UI
 */

function isEmbedded() {
    const urlParams = new URLSearchParams(window.location.search);
    const embedded = urlParams.get('embedded');
    if (embedded !== null && embedded !== undefined) return true;
    return false;
}
angular.module('idePerspective', ['ngResource', 'ngCookies', 'ideExtensions', 'ideTheming', 'ideMessageHub'])
    .constant('branding', brandingInfo)
    .constant('perspective', perspectiveData)
    .constant('extensionPoint', {})
    .constant('isEmbedded', isEmbedded())
    .config(function config($compileProvider) {
        $compileProvider.debugInfoEnabled(false);
        $compileProvider.commentDirectivesEnabled(false);
    }).factory('User', ['$http', function ($http) {
        return {
            get: function () {
                let user = { name: undefined };
                $http({
                    url: '/services/js/resources-core/services/user-name.js',
                    method: 'GET'
                }).then(function (data) {
                    user.name = data.data;
                });
                return user;
            }
        };
    }]).filter('removeSpaces', [function () {
        return function (string) {
            if (!angular.isString(string)) return string;
            return string.replace(/[\s]/g, '');
        };
    }]).directive('dgBrandTitle', ['perspective', 'branding', function (perspective, branding) {
        return {
            restrict: 'A',
            transclude: false,
            replace: true,
            link: function (scope) {
                scope.name = branding.name;
                scope.perspective = perspective;
            },
            template: '<title>{{::perspective.name || "Loading..."}} | {{::name}}</title>'
        };
    }]).directive('dgBrandIcon', ['branding', function (branding) {
        return {
            restrict: 'A',
            transclude: false,
            replace: true,
            link: function (scope) {
                scope.icon = branding.icons.faviconIco;
            },
            template: `<link rel="icon" type="image/x-icon" ng-href="{{::icon}}">`
        };
    }]).directive('ideContextmenu', ['messageHub', '$window', 'isEmbedded', function (messageHub, $window, isEmbedded) {
        if (isEmbedded) return {
            restrict: 'E',
            replace: false
        };
        else return {
            restrict: 'E',
            replace: true,
            link: function (scope, element) {
                let openedMenuId = "";
                let menu = element[0].querySelector(".fd-menu");
                scope.menuItems = [];
                scope.callbackTopic = "";

                scope.menuClick = function (itemId, data, isDisabled = false) {
                    if (!isDisabled)
                        messageHub.postMessage(scope.callbackTopic, { itemId: itemId, data: data }, true);
                };

                element.on('click', function (event) {
                    event.stopPropagation();
                    element[0].classList.add("dg-hidden");
                    menu.classList.add('dg-invisible');
                    openedMenuId = '';
                    scope.hideAllSubmenus();
                    scope.menuItems.length = 0;
                });

                element.on('contextmenu', function (event) {
                    event.preventDefault();
                    event.stopPropagation();
                    element[0].classList.add("dg-hidden");
                    menu.classList.add('dg-invisible');
                    openedMenuId = '';
                    scope.hideAllSubmenus();
                    scope.menuItems.length = 0;
                });

                scope.hideAllSubmenus = function () {
                    let submenus = element[0].querySelectorAll('.fd-menu__sublist[aria-hidden="false"]');
                    let submenusLinks = element[0].querySelectorAll('.is-expanded');
                    for (let i = 0; i < submenus.length; i++)
                        submenus[i].setAttribute("aria-hidden", true);
                    for (let i = 0; i < submenusLinks.length; i++) {
                        submenusLinks[i].setAttribute("aria-expanded", false);
                        submenusLinks[i].classList.remove("is-expanded");
                    }
                    messageHub.triggerEvent('ide-contextmenu.close', true);
                };

                scope.menuHovered = function () {
                    if (openedMenuId !== "") {
                        let oldSubmenu = element[0].querySelector(`#${openedMenuId}`);
                        let oldSubmenuLink = element[0].querySelector(`span[aria-controls="${openedMenuId}"]`);
                        oldSubmenuLink.setAttribute("aria-expanded", false);
                        oldSubmenuLink.classList.remove("is-expanded");
                        oldSubmenu.setAttribute("aria-hidden", true);
                        openedMenuId = "";
                    }
                };

                scope.showSubmenu = function (submenuId) {
                    scope.hideAllSubmenus();
                    openedMenuId = submenuId;
                    let submenu = element[0].querySelector(`#${submenuId}`);
                    let submenus = submenu.querySelectorAll('.fd-menu__sublist');
                    let submenusLinks = submenu.querySelectorAll('.is-expanded');
                    for (let i = 0; i < submenus.length; i++)
                        submenus[i].setAttribute("aria-hidden", true);
                    for (let i = 0; i < submenusLinks.length; i++) {
                        submenusLinks[i].setAttribute("aria-expanded", false);
                        submenusLinks[i].classList.remove("is-expanded");
                    }
                    let submenuLink = element[0].querySelector(`span[aria-controls="${submenuId}"]`);
                    submenuLink.setAttribute("aria-expanded", true);
                    submenuLink.classList.add("is-expanded");
                    submenu.setAttribute("aria-hidden", false);
                    requestAnimationFrame(function () {
                        let rect = submenu.getBoundingClientRect();
                        let bottom = $window.innerHeight - rect.bottom;
                        let right = $window.innerWidth - rect.right;
                        if (bottom < 0) submenu.style.top = `${bottom}px`;
                        if (right < 0) submenu.style.left = `${right}px`;
                    });
                };

                messageHub.onDidReceiveMessage(
                    'ide-contextmenu.open',
                    function (msg) {
                        scope.$apply(function () {
                            scope.menuItems = msg.data.items;
                            scope.callbackTopic = msg.data.callbackTopic;
                            scope.hasIcons = msg.data.hasIcons;
                            menu.style.top = `${msg.data.posY}px`;
                            menu.style.left = `${msg.data.posX}px`;
                            element[0].classList.remove("dg-hidden");
                            requestAnimationFrame(function () {
                                let rect = menu.getBoundingClientRect();
                                let bottom = $window.innerHeight - rect.bottom;
                                let right = $window.innerWidth - rect.right;
                                if (bottom < 0) menu.style.top = `${rect.top + bottom}px`;
                                if (right < 0) menu.style.left = `${rect.left + right}px`;
                                menu.classList.remove('dg-invisible');
                            });
                        });
                    },
                    true
                );
            },
            templateUrl: '/services/web/resources-core/ui/templates/contextmenu.html'
        };
    }]).directive('ideContextmenuSubmenu', ['$window', function ($window) {
        return {
            restrict: 'E',
            replace: true,
            scope: {
                menuClick: "&",
                menuItems: "<",
                submenuIndex: "<",
                hasIcons: "<?",
            },
            link: function (scope, element, attr) {
                let openedMenuId = "";
                scope.menuClick = scope.menuClick();

                scope.isScrollable = function () {
                    for (let i = 0; i < scope.menuItems.length; i++)
                        if (scope.menuItems[i].items) return "";
                    return "fd-scrollbar fd-menu--overflow dg-menu__sublist--overflow";
                };

                scope.renderIcons = function () {
                    return scope.hasIcons || false;
                };

                scope.menuHovered = function () {
                    if (openedMenuId !== "" && openedMenuId !== attr["id"]) {
                        let oldSubmenu = element[0].querySelector(`#${openedMenuId}`);
                        let oldSubmenuLink = element[0].querySelector(`span[aria-controls="${openedMenuId}"]`);
                        oldSubmenuLink.setAttribute("aria-expanded", false);
                        oldSubmenuLink.classList.remove("is-expanded");
                        oldSubmenu.setAttribute("aria-hidden", true);
                        openedMenuId = "";
                    }
                };

                scope.showSubmenu = function (submenuId) {
                    openedMenuId = submenuId;
                    let submenu = element[0].querySelector(`#${submenuId}`);
                    let submenus = submenu.querySelectorAll('.fd-menu__sublist');
                    let submenusLinks = submenu.querySelectorAll('.is-expanded');
                    for (let i = 0; i < submenus.length; i++)
                        submenus[i].setAttribute("aria-hidden", true);
                    for (let i = 0; i < submenusLinks.length; i++) {
                        submenusLinks[i].setAttribute("aria-expanded", false);
                        submenusLinks[i].classList.remove("is-expanded");
                    }
                    let submenuLink = element[0].querySelector(`span[aria-controls="${submenuId}"]`);
                    submenuLink.setAttribute("aria-expanded", true);
                    submenuLink.classList.add("is-expanded");
                    submenu.setAttribute("aria-hidden", false);
                    requestAnimationFrame(function () {
                        let rect = submenu.getBoundingClientRect();
                        let bottom = $window.innerHeight - rect.bottom;
                        let right = $window.innerWidth - rect.right;
                        if (bottom < 0) submenu.style.top = `${bottom}px`;
                        if (right < 0) submenu.style.left = `${right}px`;
                    });
                };
            },
            templateUrl: '/services/web/resources-core/ui/templates/contextmenuSubmenu.html'
        };
    }]).directive('ideHeader', ['$cookies', '$http', 'branding', 'theming', 'User', 'Extensions', 'messageHub', 'isEmbedded', function ($cookies, $http, branding, theming, User, Extensions, messageHub, isEmbedded) {
        if (isEmbedded) return {
            restrict: 'E',
            replace: false
        };
        else return {
            restrict: 'E',
            replace: true,
            scope: {
                menuExtId: '@',
            },
            link: {
                pre: function (scope) {
                    scope.menuClick = function (item) {
                        if (item.action === 'openView') {
                            messageHub.openView(item.id, item.data);
                        } else if (item.action === 'openPerspective') {
                            messageHub.openPerspective(item.link);
                        } else if (item.action === 'openDialogWindow') {
                            messageHub.showDialogWindow(item.dialogId);
                        } else if (item.action === 'open') {
                            window.open(item.data, '_blank');
                        } else if (item.event) {
                            messageHub.postMessage(item.event, item.data, true);
                        }
                    };
                }, post: function (scope) {
                    scope.themes = [];
                    scope.currentTheme = theming.getCurrentTheme();
                    scope.user = User.get();

                    function loadMenu() {
                        Extensions.get('menu', scope.menuExtId).then(function (data) {
                            scope.menu = data;
                        });
                    }

                    scope.branding = branding;

                    messageHub.onDidReceiveMessage(
                        'ide.themesLoaded',
                        function () {
                            scope.themes = theming.getThemes();
                        },
                        true
                    );

                    if (scope.menuExtId)
                        loadMenu.call(scope);

                    scope.setTheme = function (themeId, name) {
                        scope.currentTheme.id = themeId;
                        scope.currentTheme.name = name;
                        theming.setTheme(themeId);
                    };

                    scope.resetAll = function () {
                        messageHub.showDialogAsync(
                            `Reset ${scope.branding.brand}`,
                            ['This will clear all settings, open tabs and cache.', `${scope.branding.brand} will then reload.`, 'Do you wish to continue?'],
                            [{
                                id: 'btnConfirm',
                                type: 'emphasized',
                                label: 'Yes',
                            }, {
                                id: 'btnCancel',
                                type: 'transparent',
                                label: 'No',
                            }],
                        ).then(function (msg) {
                            if (msg.data === "btnConfirm") {
                                messageHub.showBusyDialog(
                                    'ideResetDialogBusy',
                                    'Resetting',
                                    'ide.dialog.reset.all.done'
                                );
                                localStorage.clear();
                                theming.reset();
                                $http.get('/services/js/resources-core/services/clear-cache.js').then(function () {
                                    for (let cookie in $cookies.getAll()) {
                                        if (cookie.startsWith('DIRIGIBLE')) {
                                            $cookies.remove(cookie, { path: '/' });
                                        }
                                    }
                                    location.reload();
                                }, function (error) {
                                    console.log(error);
                                    messageHub.hideBusyDialog('ideResetDialogBusy');
                                    messageHub.showAlertError('Failed to reset', 'There was an error during the reset process. Please refresh manually.');
                                });
                            }
                        });
                    };

                    scope.logout = function () {
                        location.replace('/logout');
                    };
                }
            },
            templateUrl: '/services/web/resources-core/ui/templates/ideHeader.html',
        };
    }]).directive("headerMenu", [function () {
        return {
            restrict: "A",
            replace: false,
            scope: {
                menuList: "<",
                menuHandler: "&",
            },
            link: function (scope) {
                scope.menuHandler = scope.menuHandler();
                scope.isScrollable = function (items) {
                    if (items) {
                        for (let i = 0; i < items.length; i++)
                            if (items[i].items) return false;
                    } else {
                        for (let i = 0; i < scope.menuList.length; i++)
                            if (scope.menuList[i].items) return false;
                    }
                    return true;
                };
            },
            template: `<div fd-tool-header-element ng-repeat="menuItem in menuList track by $index">
                <fd-popover>
                    <fd-popover-control>
                        <fd-button fd-tool-header-button compact="true" dg-label="{{ ::menuItem.label }}" dg-type="transparent"
                            is-menu="true" aria-label="menu button {{ ::menuItem.label }}">
                        </fd-button>
                    </fd-popover-control>
                    <fd-popover-body no-arrow="true" can-scroll="::isScrollable()">
                        <fd-menu aria-label="header menu">
                            <fd-menu-item ng-repeat-start="item in menuItem.items track by $index" ng-if="!item.items"
                                title="{{ ::item.label }}" ng-click="::menuHandler(item)" has-separator="::item.divider">
                            </fd-menu-item>
                            <fd-menu-sublist ng-if="item.items" has-separator="::item.divider" title="{{ ::item.label }}" can-scroll="::isScrollable(item.items)" ng-repeat-end>
                                <header-submenu sublist="::item.items" menu-handler="::menuHandler"></header-submenu>
                            </fd-menu-sublist>
                        </fd-menu>
                    </fd-popover-body>
                </fd-popover>
            </div>`,
        };
    }]).directive("headerSubmenu", function () {
        return {
            restrict: "E",
            replace: false,
            scope: {
                sublist: '<',
                menuHandler: '&',
            },
            link: function (scope) {
                scope.menuHandler = scope.menuHandler();
                scope.isScrollable = function (index) {
                    for (let i = 0; i < scope.sublist[index].items.length; i++)
                        if (scope.sublist[index].items[i].items) return false;
                    return true;
                };
            },
            template: `<fd-menu-item ng-repeat-start="item in sublist track by $index" ng-if="!item.items" has-separator="::item.divider" title="{{ ::item.label }}" ng-click="::menuHandler(item)"></fd-menu-item>
<fd-menu-sublist ng-if="item.items" has-separator="::item.divider" title="{{ ::item.label }}" can-scroll="::isScrollable($index)" ng-repeat-end><header-submenu sublist="::item.items" menu-handler="::menuHandler"></header-submenu></fd-menu-sublist>`,
        };
    }).directive('ideContainer', ['perspective', 'isEmbedded', function (perspective, isEmbedded) {
        return {
            restrict: 'E',
            transclude: true,
            replace: true,
            link: {
                pre: function (scope) {
                    scope.withSidebar = !isEmbedded;
                    scope.shouldLoad = true;
                    if (!perspective.id || !perspective.name) {
                        console.error('<ide-container> requires perspective service data');
                        scope.shouldLoad = false;
                    }
                },
            },
            template: `<div class="dg-main-container"><ide-sidebar ng-if="withSidebar"></ide-sidebar><ng-transclude ng-if="shouldLoad" class="dg-perspective-container"></ng-transclude></div>`
        }
    }]).directive('ideSidebar', ['Extensions', 'extensionPoint', 'perspective', 'messageHub', function (Extensions, extensionPoint, perspective, messageHub) {
        return {
            restrict: 'E',
            replace: true,
            link: {
                pre: function (scope) {
                    scope.activeId = perspective.id;
                    Extensions.get('perspective', extensionPoint.perspectives).then(function (response) {
                        scope.perspectives = response;
                    });
                    scope.getIcon = function (icon) {
                        if (icon) return icon;
                        return "/services/web/resources/images/unknown.svg";
                    }
                },
                post: function () {
                    messageHub.onDidReceiveMessage(
                        'ide-core.openPerspective',
                        function (data) {
                            let url = data.link;
                            if (data.params) {
                                let urlParams = '';
                                for (const property in data.params) {
                                    urlParams += `${property}=${encodeURIComponent(data.params[property])}&`;
                                }
                                url += `?${urlParams.slice(0, -1)}`;
                            }
                            window.location.href = url;
                        },
                        true
                    );
                },
            },
            templateUrl: '/services/web/resources-core/ui/templates/ideSidebar.html'
        }
    }])
    /**
     * Used for Dialogs and Window Dialogs
     */
    .directive('ideDialogs', ['messageHub', 'Extensions', 'extensionPoint', 'perspective', 'isEmbedded', function (messageHub, Extensions, extensionPoint, perspective, isEmbedded) {
        if (isEmbedded) return {
            restrict: 'E',
            replace: false,
            link: {
                pre: function () {
                    let dialogWindows;
                    Extensions.get('dialogWindow', extensionPoint.dialogWindows).then(function (data) {
                        dialogWindows = data;
                    });
                    messageHub.onDidReceiveMessage(
                        'ide.dialogWindow',
                        function (data) {
                            if (!data.serviceData) {
                                let found = false;
                                for (let i = 0; i < dialogWindows.length; i++) {
                                    if (dialogWindows[i].id === data.dialogWindowId) {
                                        found = true;
                                        messageHub.postMessage('ide.embedded.dialogWindow', {
                                            params: data.params,
                                            callbackTopic: data.callbackTopic,
                                            closable: data.closable,
                                            serviceData: dialogWindows[i]
                                        });
                                        break;
                                    }
                                }
                                if (!found) console.error(
                                    `Dialog Window Error: There is no window dialog with such id: ${data.dialogWindowId}`
                                );
                            }
                        },
                        true
                    );
                }
            }
        };
        else return {
            restrict: 'E',
            replace: true,
            scope: {
                extensionPoints: '<?' // List of extension points. Internal use only, do not document!
            },
            link: function (scope, element) {
                let dialogWindows;
                Extensions.get('dialogWindow', extensionPoint.dialogWindows).then(function (data) {
                    dialogWindows = data;
                    if (scope.extensionPoints) {
                        for (let i = 0; i < scope.extensionPoints.length; i++) {
                            Extensions.get('dialogWindow', scope.extensionPoints[i]).then(function (data) {
                                dialogWindows = dialogWindows.concat(data);
                            });
                        }
                    }
                });
                let messageBox = element[0].querySelector("#dgIdeAlert");
                let ideDialog = element[0].querySelector("#dgIdeDialog");
                let ideBusyDialog = element[0].querySelector("#dgIdeBusyDialog");
                let ideFormDialog = element[0].querySelector("#dgIdeFormDialog");
                let ideSelectDialog = element[0].querySelector("#dgIdeSelectDialog");
                let ideDialogWindow = element[0].querySelector("#dgIdeDialogWindow");
                let alerts = [];
                let windows = [];
                let dialogs = [];
                let busyDialogs = [];
                let loadingDialogs = [];
                let formDialogs = [];
                let selectDialogs = [];
                element.on('contextmenu', event => event.stopPropagation());
                scope.searchInput = { value: "" }; // AngularJS - "If you use ng-model, you have to use an object property, not just a variable"
                scope.activeDialog = null;
                scope.alert = {
                    title: "",
                    message: "",
                    type: "information", // information, error, success, warning
                };
                scope.dialog = {
                    id: null,
                    header: "",
                    subheader: "",
                    title: "",
                    body: [],
                    footer: "",
                    buttons: [],
                    callbackTopic: null,
                    loader: false,
                };
                scope.busyDialog = {
                    id: null,
                    text: '',
                    callbackTopic: '',
                };
                scope.formDialog = {
                    id: null,
                    header: "",
                    subheader: "",
                    title: "",
                    footer: "",
                    buttons: [],
                    loadingMessage: "",
                    loader: false,
                    callbackTopic: null,
                    items: [],
                };
                scope.selectDialog = {
                    title: "",
                    listItems: [],
                    selectedItems: 0,
                    selectedItemId: "",
                    callbackTopic: "",
                    isSingleChoice: true,
                    hasSearch: false
                };
                scope.window = {
                    title: "",
                    dialogWindowId: "",
                    callbackTopic: null,
                    link: "",
                    parameters: "",
                    closable: true,
                };

                scope.showAlert = function () {
                    if (element[0].classList.contains("dg-hidden"))
                        element[0].classList.remove("dg-hidden");
                    scope.alert = alerts[0];
                    messageBox.classList.add("fd-message-box--active");
                    scope.activeDialog = 'alert';
                };

                scope.hideAlert = function () {
                    messageBox.classList.remove("fd-message-box--active");
                    alerts.shift();
                    checkForDialogs();
                };

                scope.showDialog = function () {
                    if (element[0].classList.contains("dg-hidden"))
                        element[0].classList.remove("dg-hidden");
                    scope.dialog = dialogs[0];
                    ideDialog.classList.add("fd-dialog--active");
                    scope.activeDialog = 'dialog';
                };

                scope.hideDialog = function (buttonId) {
                    if (buttonId && scope.dialog.callbackTopic) messageHub.postMessage(scope.dialog.callbackTopic, buttonId, true);
                    ideDialog.classList.remove("fd-dialog--active");
                    dialogs.shift();
                    checkForDialogs();
                };

                scope.showFormDialog = function () {
                    if (element[0].classList.contains("dg-hidden"))
                        element[0].classList.remove("dg-hidden");
                    scope.formDialog = formDialogs[0];
                    ideFormDialog.classList.add("fd-dialog--active");
                    scope.activeDialog = 'form';
                    requestAnimationFrame(function () {
                        if (scope.formDialog.items.length) {
                            for (let i = 0; i < scope.formDialog.items.length; i++) {
                                if (scope.formDialog.items[i].type === 'input' || scope.formDialog.items[i].type === 'textarea') {
                                    let input = ideFormDialog.querySelector(`#${scope.formDialog.items[i].id}`);
                                    if (!scope.formDialog.items[i].visibility || scope.formDialog.items[i].visibility.$isVisible) {
                                        input.focus();
                                        break;
                                    }
                                }
                            }
                        }
                    });
                };

                scope.formDialogAction = function (buttonId) {
                    scope.formDialog.loader = true;
                    messageHub.postMessage(scope.formDialog.callbackTopic, { buttonId: buttonId, formData: scope.formDialog.items }, true);
                };

                scope.hideFormDialog = function (id) {
                    if (id === scope.formDialog.id) {
                        ideFormDialog.classList.remove("fd-dialog--active");
                        formDialogs.shift();
                        checkForDialogs();
                    } else {
                        for (let i = 0; i < formDialogs.length; i++) {
                            if (formDialogs[i].id === id) {
                                formDialogs.splice(i, 1);
                                break;
                            }
                        }
                    }
                };

                scope.showLoadingDialog = function () {
                    if (element[0].classList.contains("dg-hidden"))
                        element[0].classList.remove("dg-hidden");
                    scope.dialog = loadingDialogs[0];
                    ideDialog.classList.add("fd-dialog--active");
                    scope.activeDialog = 'dialog';
                };

                scope.hideLoadingDialog = function (id) {
                    if (id === scope.dialog.id) {
                        ideDialog.classList.remove("fd-dialog--active");
                        loadingDialogs.shift();
                        checkForDialogs();
                    } else {
                        for (let i = 0; i < loadingDialogs.length; i++) {
                            if (loadingDialogs[i].id === id) {
                                loadingDialogs.splice(i, 1);
                                break;
                            }
                        }
                    }
                };

                scope.showBusyDialog = function () {
                    if (element[0].classList.contains("dg-hidden"))
                        element[0].classList.remove("dg-hidden");
                    scope.busyDialog = busyDialogs[0];
                    ideBusyDialog.classList.add("fd-dialog--active");
                    scope.activeDialog = 'busy';
                };

                scope.hideBusyDialog = function (id, fromUser = false) {
                    if (id === scope.busyDialog.id) {
                        if (fromUser) messageHub.triggerEvent(scope.busyDialog.callbackTopic, true);
                        ideBusyDialog.classList.remove("fd-dialog--active");
                        busyDialogs.shift();
                        checkForDialogs();
                    } else {
                        for (let i = 0; i < busyDialogs.length; i++) {
                            if (busyDialogs[i].id === id) {
                                busyDialogs.splice(i, 1);
                                break;
                            }
                        }
                    }
                };

                scope.itemSelected = function (item) {
                    if (scope.selectDialog.isSingleChoice) {
                        scope.selectDialog.selectedItemId = item;
                        scope.selectDialog.selectedItems = 1;
                    } else {
                        if (item) scope.selectDialog.selectedItems += 1;
                        else scope.selectDialog.selectedItems -= 1;
                    }
                };

                scope.searchChanged = function () {
                    let value = scope.searchInput.value.toLowerCase();
                    if (value === "") scope.clearSearch();
                    else for (let i = 0; i < scope.selectDialog.listItems.length; i++) {
                        if (!scope.selectDialog.listItems[i].text.toLowerCase().includes(value))
                            scope.selectDialog.listItems[i].hidden = true;
                    }
                };

                scope.clearSearch = function () {
                    scope.searchInput.value = "";
                    for (let i = 0; i < scope.selectDialog.listItems.length; i++) {
                        scope.selectDialog.listItems[i].hidden = false;
                    }
                };

                scope.showSelectDialog = function () {
                    if (element[0].classList.contains("dg-hidden"))
                        element[0].classList.remove("dg-hidden");
                    scope.selectDialog = selectDialogs[0];
                    ideSelectDialog.classList.add("fd-dialog--active");
                    scope.activeDialog = 'select';
                };

                scope.hideSelectDialog = function (id, action) {
                    if (id === scope.selectDialog.id) {
                        if (action === "select") {
                            if (scope.selectDialog.selectedItems > 0 || scope.selectDialog.selectedItemId !== "")
                                if (scope.selectDialog.isSingleChoice)
                                    messageHub.postMessage(
                                        scope.selectDialog.callbackTopic,
                                        {
                                            selected: scope.selectDialog.selectedItemId
                                        },
                                        true
                                    );
                                else messageHub.postMessage(
                                    scope.selectDialog.callbackTopic,
                                    {
                                        selected: getSelectedItems()
                                    },
                                    true
                                );
                            else return;
                        } else {
                            let selected;
                            if (scope.selectDialog.isSingleChoice) selected = "";
                            else selected = [];
                            messageHub.postMessage(
                                scope.selectDialog.callbackTopic,
                                { selected: selected },
                                true
                            );
                        }
                        ideSelectDialog.classList.remove("fd-dialog--active");
                        element[0].classList.add("dg-hidden");
                        selectDialogs.shift();
                        checkForDialogs();
                    } else {
                        for (let i = 0; i < selectDialogs.length; i++) {
                            if (selectDialogs[i].id === id) {
                                selectDialogs.splice(i, 1);
                                break;
                            }
                        }
                    }
                };

                scope.showWindow = function () {
                    scope.window = windows[0];
                    if (scope.window.link === "") {
                        console.error(
                            "Dialog Window Error: The link property is missing."
                        );
                        windows.shift();
                        checkForDialogs();
                        return;
                    }
                    if (element[0].classList.contains("dg-hidden"))
                        element[0].classList.remove("dg-hidden");
                    ideDialogWindow.classList.add("fd-dialog--active");
                    scope.activeDialog = 'window';
                };

                scope.hideWindow = function () {
                    if (scope.window.callbackTopic) {
                        messageHub.triggerEvent(scope.window.callbackTopic, true);
                        scope.window.callbackTopic = null;
                    }
                    ideDialogWindow.classList.remove("fd-dialog--active");
                    windows.shift();
                    scope.window.link = "";
                    scope.window.parameters = "";
                    scope.window.title = "";
                    scope.window.dialogWindowId = "";
                    scope.window.link = "";
                    scope.window.parameters = "";
                    scope.window.closable = true;
                    checkForDialogs();
                };

                scope.shouldHide = function (item) {
                    if (item.visibility) {
                        for (let i = 0; i < scope.formDialog.items.length; i++) {
                            if (scope.formDialog.items[i].id === item.visibility.id && scope.formDialog.items[i].value === item.visibility.value) {
                                if (item.visibility.hidden) {
                                    item.visibility.$isVisible = true;
                                    return false;
                                } else {
                                    item.visibility.$isVisible = false;
                                    return true;
                                }
                            }
                        }
                        item.visibility.$isVisible = !item.visibility.hidden;
                        return item.visibility.hidden;
                    }
                    return false;
                };

                function checkForDialogs() {
                    scope.activeDialog = null;
                    if (selectDialogs.length > 0) scope.showSelectDialog();
                    else if (formDialogs.length > 0) scope.showFormDialog();
                    else if (dialogs.length > 0) scope.showDialog();
                    else if (alerts.length > 0) scope.showAlert();
                    else if (loadingDialogs.length > 0) scope.showLoadingDialog();
                    else if (busyDialogs.length > 0) scope.showBusyDialog();
                    else if (windows.length > 0) scope.showWindow();
                    else element[0].classList.add("dg-hidden");
                }

                messageHub.onDidReceiveMessage(
                    "ide.alert",
                    function (data) {
                        scope.$apply(function () {
                            let type;
                            if (data.type) {
                                switch (data.type.toLowerCase()) {
                                    case "success":
                                        type = "success";
                                        break;
                                    case "warning":
                                        type = "warning";
                                        break;
                                    case "info":
                                        type = "information";
                                        break;
                                    case "error":
                                        type = "error";
                                        break;
                                    default:
                                        type = "information";
                                        break;
                                }
                            }
                            alerts.push({
                                title: data.title,
                                message: data.message,
                                type: type,
                            });
                            if (!scope.activeDialog && alerts.length < 2) {
                                scope.showAlert();
                            }
                        });
                    },
                    true
                );

                function getDialogBody(body) {
                    if (Array.isArray(body)) return body;
                    return [body];
                }

                messageHub.onDidReceiveMessage(
                    "ide.dialog",
                    function (data) {
                        scope.$apply(function () {
                            dialogs.push({
                                header: data.header,
                                subheader: data.subheader,
                                title: data.title,
                                body: getDialogBody(data.body),
                                footer: data.footer,
                                loader: data.loader,
                                buttons: data.buttons,
                                callbackTopic: data.callbackTopic
                            });
                            if (!scope.activeDialog && dialogs.length < 2) {
                                scope.showDialog();
                            }
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.formDialog.show",
                    function (data) {
                        scope.$apply(function () {
                            formDialogs.push({
                                id: data.id,
                                header: data.header,
                                subheader: data.subheader,
                                title: data.title,
                                items: data.items,
                                loadingMessage: data.loadingMessage,
                                loader: false,
                                footer: data.footer,
                                buttons: data.buttons,
                                callbackTopic: data.callbackTopic,
                            });
                            if (!scope.activeDialog && formDialogs.length < 2) {
                                scope.showFormDialog();
                            }
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.formDialog.update",
                    function (data) {
                        scope.$apply(function () {
                            if (scope.formDialog && data.id === scope.formDialog.id) {
                                scope.formDialog.items = data.items;
                                if (data.subheader)
                                    scope.formDialog.subheader = data.subheader;
                                if (data.footer)
                                    scope.formDialog.footer = data.footer;
                                if (data.loadingMessage)
                                    scope.formDialog.loadingMessage = data.loadingMessage;
                                scope.formDialog.loader = false;
                            } else {
                                for (let i = 0; i < formDialogs.length; i++) {
                                    if (formDialogs[i].id === data.id) {
                                        formDialogs[i].items = data.items;
                                        if (data.subheader)
                                            formDialogs[i].subheader = data.subheader;
                                        if (data.footer)
                                            formDialogs[i].footer = data.footer;
                                        if (data.loadingMessage)
                                            formDialogs[i].loadingMessage = data.loadingMessage;
                                        formDialogs[i].loader = false;
                                        break;
                                    }
                                }
                            }
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.formDialog.hide",
                    function (data) {
                        scope.$apply(function () {
                            scope.hideFormDialog(data.id);
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.loadingDialog.show",
                    function (data) {
                        scope.$apply(function () {
                            loadingDialogs.push({
                                id: data.id,
                                title: data.title,
                                header: '',
                                subheader: '',
                                footer: '',
                                status: data.status,
                                loader: true,
                            });
                            if (!scope.activeDialog && loadingDialogs.length < 2) {
                                scope.showLoadingDialog();
                            }
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.loadingDialog.update",
                    function (data) {
                        scope.$apply(function () {
                            if (scope.dialog && data.id === scope.dialog.id) {
                                scope.dialog.status = data.status;
                            } else {
                                for (let i = 0; i < loadingDialogs.length; i++) {
                                    if (loadingDialogs[i].id === data.id) {
                                        loadingDialogs[i].status = data.status;
                                        break;
                                    }
                                }
                            }
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.loadingDialog.hide",
                    function (data) {
                        scope.$apply(function () {
                            scope.hideLoadingDialog(data.id);
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.busyDialog.show",
                    function (data) {
                        scope.$apply(function () {
                            busyDialogs.push({
                                id: data.id,
                                text: data.text,
                                callbackTopic: data.callbackTopic,
                            });
                            if (!scope.activeDialog && busyDialogs.length < 2) {
                                scope.showBusyDialog();
                            }
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.busyDialog.hide",
                    function (data) {
                        scope.$apply(function () {
                            scope.hideBusyDialog(data.id);
                        });
                    },
                    true
                );

                scope.isRequired = function (visibility = { $isVisible: true }, required = false) {
                    if (visibility.$isVisible === false) return false;
                    return required;
                };

                scope.isValid = function (isValid, item) {
                    if (isValid) {
                        item.error = false;
                    } else {
                        item.error = true;
                    }
                };

                function getSelectedItems() {
                    let selected = [];
                    for (let i = 0; i < scope.selectDialog.listItems.length; i++) {
                        if (scope.selectDialog.listItems[i].selected)
                            selected.push(scope.selectDialog.listItems[i].ownId);
                    }
                    return selected;
                }

                function getSelectDialogList(listItems) {
                    return listItems.map(
                        function (item, index) {
                            return {
                                "id": `idesdl${index}`,
                                "ownId": item.id,
                                "text": item.text,
                                "hidden": false,
                                "selected": false
                            };
                        }
                    );
                }

                messageHub.onDidReceiveMessage(
                    "ide.selectDialog",
                    function (data) {
                        scope.$apply(function () {
                            selectDialogs.push({
                                title: data.title,
                                listItems: getSelectDialogList(data.listItems),
                                selectedItems: 0,
                                callbackTopic: data.callbackTopic,
                                isSingleChoice: data.isSingleChoice,
                                hasSearch: data.hasSearch
                            });
                            if (!scope.activeDialog && selectDialogs.length < 2) {
                                scope.showSelectDialog();
                            }
                        });
                    },
                    true
                );

                function getDialogParams(params) {
                    if (params) {
                        params['container'] = 'dialog';
                        params['perspectiveId'] = perspective.id;
                    } else {
                        params = {
                            container: 'layout',
                            perspectiveId: perspective.id,
                        };
                    }
                    return JSON.stringify(params);
                }

                messageHub.onDidReceiveMessage(
                    "ide.embedded.dialogWindow",
                    function (msg) {
                        scope.$apply(function () {
                            windows.push({
                                title: msg.data.serviceData.label,
                                dialogWindowId: msg.data.serviceData.id,
                                callbackTopic: msg.data.callbackTopic,
                                link: msg.data.serviceData.link,
                                params: getDialogParams(msg.data.params),
                                closable: msg.data.closable,
                            });
                            if (!scope.activeDialog && windows.length < 2) {
                                scope.showWindow();
                            }
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.dialogWindow",
                    function (data) {
                        scope.$apply(function () {
                            if (data.serviceData) {
                                windows.push({
                                    title: data.serviceData.label,
                                    dialogWindowId: data.dialogWindowId,
                                    callbackTopic: data.callbackTopic,
                                    link: data.serviceData.link,
                                    params: getDialogParams(data.params),
                                    closable: data.closable,
                                });
                                if (!scope.activeDialog && windows.length < 2) {
                                    scope.showWindow();
                                }
                            } else {
                                let found = false;
                                for (let i = 0; i < dialogWindows.length; i++) {
                                    if (dialogWindows[i].id === data.dialogWindowId) {
                                        found = true;
                                        windows.push({
                                            title: dialogWindows[i].label,
                                            dialogWindowId: dialogWindows[i].id,
                                            callbackTopic: data.callbackTopic,
                                            link: dialogWindows[i].link,
                                            params: getDialogParams(data.params),
                                            closable: data.closable,
                                        });
                                        break;
                                    }
                                }
                                if (found) {
                                    if (!scope.activeDialog && windows.length < 2) {
                                        scope.showWindow();
                                    }
                                } else console.error(
                                    `Dialog Window Error: There is no window dialog with such id: ${data.dialogWindowId}`
                                );
                            }
                        });
                    },
                    true
                );

                messageHub.onDidReceiveMessage(
                    "ide.dialogWindow.close",
                    function (data) {
                        scope.$apply(function () {
                            if (data.dialogWindowId === scope.window.dialogWindowId) {
                                if (windows.length > 1 && windows[1].dialogWindowId === data.dialogWindowId) windows.splice(1, 1);
                                scope.hideWindow();
                            } else {
                                for (let i = 0; i < windows.length; i++) {
                                    if (windows[i].dialogWindowId === data.dialogWindowId) {
                                        windows.splice(i, 1);
                                        break;
                                    }
                                }
                            }
                        });
                    },
                    true
                );
            },
            templateUrl: '/services/web/resources-core/ui/templates/ideDialogs.html'
        }
    }]).directive('ideStatusBar', ['messageHub', 'isEmbedded', function (messageHub, isEmbedded) {
        if (isEmbedded) return {
            restrict: 'E',
            replace: false
        };
        else return {
            restrict: 'E',
            replace: true,
            link: function (scope) {
                scope.busy = '';
                scope.message = '';
                scope.caret = '';
                scope.error = '';
                messageHub.onDidReceiveMessage(
                    'ide.status.busy',
                    function (data) {
                        scope.$apply(function () {
                            scope.busy = data.message;
                        });
                    },
                    true
                );
                messageHub.onDidReceiveMessage(
                    'ide.status.message',
                    function (data) {
                        scope.$apply(function () {
                            scope.message = data.message;
                        });
                    },
                    true
                );
                messageHub.onDidReceiveMessage(
                    'ide.status.error',
                    function (data) {
                        scope.$apply(function () {
                            scope.error = data.message;
                        });
                    },
                    true
                );
                messageHub.onDidReceiveMessage(
                    'ide.status.caret',
                    function (data) {
                        scope.$apply(function () {
                            scope.caret = data.text;
                        });
                    },
                    true
                );
                scope.cleanStatusMessages = function () {
                    scope.message = null;
                };
                scope.cleanErrorMessages = function () {
                    scope.error = null;
                };
            },
            templateUrl: '/services/web/resources-core/ui/templates/ideStatusBar.html'
        }
    }]);
