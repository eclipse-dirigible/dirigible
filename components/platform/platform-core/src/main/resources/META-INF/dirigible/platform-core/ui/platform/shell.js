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
angular.module('platformShell', ['ngCookies', 'platformUser', 'platformExtensions', 'platformDialogs', 'platformContextMenu'])
    .value('shellState', {
        perspectiveInternal: {
            id: '',
            label: ''
        },
        perspectiveListeners: [],
        set perspective(newData) {
            this.perspectiveInternal = newData;
            for (let l = 0; l < this.perspectiveListeners.length; l++) {
                this.perspectiveListeners[l](newData);
            }
        },
        get perspective() {
            return this.perspectiveInternal;
        },
        registerStateListener: function (listener) {
            this.perspectiveListeners.push(listener);
        }
    })
    .value('notifications', {
        notificationListInternal: [],
        notificationListListeners: [],
        add(notification) {
            this.notificationListInternal.unshift(notification);
            for (let l = 0; l < this.notificationListListeners.length; l++) {
                this.notificationListListeners[l](notification);
            }
        },
        set list(newData) {
            this.notificationListInternal = newData;
            for (let l = 0; l < this.notificationListListeners.length; l++) {
                this.notificationListListeners[l](newData);
            }
        },
        get list() {
            return this.notificationListInternal;
        },
        registerStateListener: function (listener) {
            this.notificationListListeners.push(listener);
        }
    })
    .constant('MessageHub', new MessageHubApi())
    .constant('Shell', new ShellHub())
    .config(function config($compileProvider) {
        $compileProvider.debugInfoEnabled(false);
        $compileProvider.commentDirectivesEnabled(false);
        $compileProvider.cssClassDirectivesEnabled(false);
    }).directive('shellHeader', ($window, User, Extensions, shellState, notifications, MessageHub, Shell) => ({
        restrict: 'E',
        replace: true,
        link: (scope, element) => {
            const notificationStateKey = `${brandingInfo.keyPrefix}.notifications`;
            const dialogHub = new DialogHub();
            scope.perspectiveId = shellState.perspective.id;
            shellState.registerStateListener((data) => {
                scope.perspectiveId = data.id;
                scope.collapseMenu = false;
            });
            notifications.list = JSON.parse(localStorage.getItem(notificationStateKey) || '[]');
            scope.notifications = notifications.list;
            notifications.registerStateListener(() => {
                scope.notifications = notifications.list;
                scope.saveNotifications();
            });
            scope.saveNotifications = () => {
                localStorage.setItem(notificationStateKey, JSON.stringify(scope.notifications));
            };
            scope.selectedNotification = '';
            scope.branding = brandingInfo;
            scope.username = undefined;
            User.getName().then((data) => {
                scope.username = data.data;
            });
            scope.menus = {};
            scope.systemMenus = {
                help: undefined,
                window: undefined
            };
            scope.menu = [];
            scope.collapseMenu = false;

            Extensions.getMenus().then((response) => {
                for (let i = 0; i < response.data.length; i++) {
                    if (!response.data[i].systemMenu) {
                        scope.menus[response.data[i].perspectiveId] = {
                            include: response.data[i].include,
                            items: response.data[i].items
                        }
                    } else {
                        if (response.data[i].id === 'help') {
                            scope.systemMenus.help = response.data[i].menu;
                        } else if (response.data[i].id === 'window') {
                            scope.systemMenus.window = response.data[i].menu;
                        }
                    }
                }
            });

            let thresholdWidth = 0;
            const resizeObserver = new ResizeObserver((entries) => {
                if (scope.collapseMenu && element[0].offsetWidth > thresholdWidth) {
                    return scope.$apply(() => scope.collapseMenu = false);
                } else if (entries[0].contentRect.width === 0) {
                    thresholdWidth = element[0].offsetWidth;
                    scope.$evalAsync(() => scope.collapseMenu = true);
                }
            });
            resizeObserver.observe(element.find('#spacer')[0]);

            scope.getLocalTime = (timestamp) => new Date(timestamp).toLocaleDateString(
                navigator.languages, {
                hour: 'numeric',
                minute: 'numeric',
                day: 'numeric',
                month: 'numeric',
                year: '2-digit'
            });

            scope.notificationSelect = (id) => {
                scope.selectedNotification = id;
            };

            scope.notificationsPanelOpen = () => {
                scope.selectedNotification = '';
            };

            scope.clearNotifications = () => {
                notifications.list.length = 0;
                scope.saveNotifications();
            };

            scope.deleteNotification = (id) => {
                for (let i = 0; i < scope.notifications.length; i++) {
                    if (scope.notifications[i].id === id) {
                        scope.notifications.splice(i, 1);
                        break;
                    }
                }
                scope.saveNotifications();
            };

            scope.menuClick = (item) => {
                if (item.action === 'openView') {
                    new LayoutHub(scope.perspectiveId).openView({ id: item.id, params: item.data });
                } else if (item.action === 'showPerspective') {
                    Shell.showPerspective({ id: item.id });
                } else if (item.action === 'openWindow') {
                    dialogHub.showWindow({
                        hasHeader: item.hasHeader,
                        id: item.id
                    });
                } else if (item.action === 'open') {
                    $window.open(item.link, '_blank');
                } else if (item.action === 'event') {
                    MessageHub.postMessage({ topic: item.data.topic, data: item.data.message || '' });
                }
            };

            scope.isScrollable = (items) => {
                if (items) {
                    for (let i = 0; i < items.length; i++)
                        if (items[i].items) return false;
                }
                return true;
            };

            scope.logout = () => {
                location.replace('/logout');
            };
        },
        templateUrl: '/services/web/platform-core/ui/templates/header.html',
    })).directive('submenu', () => ({
        restrict: "E",
        replace: false,
        scope: {
            sublist: '<',
            menuHandler: '&',
        },
        link: (scope) => {
            scope.menuHandler = scope.menuHandler();
            scope.isScrollable = (index) => {
                for (let i = 0; i < scope.sublist[index].items.length; i++)
                    if (scope.sublist[index].items[i].items) return false;
                return true;
            };
        },
        template: `<bk-menu-item ng-repeat-start="item in sublist track by $index" ng-if="!item.items" has-separator="::item.divider" title="{{ ::item.label }}" ng-click="::menuHandler(item)"></bk-menu-item>
        <bk-menu-sublist ng-if="item.items" has-separator="::item.divider" title="{{ ::item.label }}" can-scroll="::isScrollable($index)" ng-repeat-end><submenu sublist="::item.items" menu-handler="::menuHandler"></submenu></bk-menu-sublist>`,
    })).directive('perspectiveContainer', (Extensions, shellState, Shell) => ({
        restrict: 'E',
        transclude: true,
        replace: true,
        scope: {
            condensed: '<?', // Boolean - If the side navigation should show both icons and labels. This is not dynamic!
            config: '=?' // Object - Sidebar configuration containing perspectives and perspective groups. 
        },
        link: {
            pre: (scope, element) => {
                scope.noStatusbar = true;
                for (let i = 0; i < element[0].parentElement.children.length; i++) {
                    if (element[0].parentElement.children[i].classList.contains('bk-statusbar')) {
                        scope.noStatusbar = false;
                        break;
                    }
                }
            },
            post: (scope) => {
                const selectedPerspectiveKey = `${brandingInfo.keyPrefix ?? 'DIRIGIBLE'}.shell.selected-perspective`;
                scope.activeId = localStorage.getItem(selectedPerspectiveKey);
                shellState.registerStateListener((data) => {
                    scope.activeId = data.id;
                    saveSelectedPerspective(data.id);
                });
                function saveSelectedPerspective(id) {
                    localStorage.setItem(selectedPerspectiveKey, id);
                }
                function setDefaultPerspective() {
                    if (scope.config.perspectives.length) {
                        const label = getPerspectiveLabel(scope.activeId, scope.config.perspectives) ?? getPerspectiveLabel(scope.activeId, scope.config.utilities);
                        if (label) {
                            shellState.perspective = {
                                id: scope.activeId,
                                label: label,
                            };
                        } else if (scope.config.perspectives[0].items) {
                            scope.activeId = scope.config.perspectives[0].items[0].id;
                            shellState.perspective = {
                                id: scope.config.perspectives[0].items[0].id,
                                label: scope.config.perspectives[0].items[0].label
                            };
                            saveSelectedPerspective(scope.config.perspectives[0].items[0].id);
                        } else {
                            scope.activeId = scope.config.perspectives[0].id;
                            shellState.perspective = {
                                id: scope.config.perspectives[0].id,
                                label: scope.config.perspectives[0].label
                            };
                            saveSelectedPerspective(scope.config.perspectives[0].id);
                        }
                    }
                }
                if (!scope.config) {
                    Extensions.getPerspectives().then((response) => {
                        scope.config = response.data;
                        setDefaultPerspective();
                    });
                } else setDefaultPerspective();
                scope.isActive = (id, groupId) => {
                    if (id === scope.activeId) {
                        scope.activeGroupId = groupId;
                        return true;
                    }
                    return false;
                };
                scope.getIcon = (icon) => {
                    if (icon) return icon;
                    return '/services/web/platform-core/ui/images/unknown.svg';
                };
                scope.switchPerspective = (id, label) => {
                    shellState.perspective = {
                        id: id,
                        label: label
                    };
                };

                function getPerspectiveLabel(pId, plist) {
                    let label;
                    for (let i = 0; i < plist.length; i++) {
                        if (plist[i].path && plist[i].id === pId) {
                            label = plist[i].label;
                            break;
                        } else if (plist[i].items) {
                            label = getPerspectiveLabel(pId, plist[i].items);
                            if (label !== undefined) break;
                        }
                    }
                    return label;
                }

                const showPerspectiveListener = Shell.onShowPerspective((data) => {
                    const label = getPerspectiveLabel(data.id, scope.config.perspectives);
                    if (label) {
                        scope.$evalAsync(() => {
                            shellState.perspective = {
                                id: data.id,
                                label: label
                            };
                        });
                    }
                });

                scope.getDataParams = (params = {}) => JSON.stringify({
                    container: 'shell',
                    ...params
                });

                scope.$on('$destroy', () => {
                    Shell.removeMessageListener(showPerspectiveListener);
                });
            },
        },
        template: `<div class="main-container" no-statusbar="{{::noStatusbar}}">
            <bk-vertical-nav class="sidebar" condensed="condensed" can-scroll="true">
                <bk-vertical-nav-main-section aria-label="perspective navigation">
                    <bk-list aria-label="Perspective list" ng-if="!condensed">
                        <bk-list-navigation-group-header ng-repeat-start="navItem in config.perspectives track by navItem.id" ng-if="navItem.items && navItem.headerLabel">{{::navItem.headerLabel}}</bk-list-navigation-group-header>
                        <bk-list-navigation-item ng-if="navItem.items" expandable="true" ng-click="navItem.expanded = !navItem.expanded" is-expanded="navItem.expanded">
                            <bk-list-navigation-item-icon icon-size="lg" svg-path="{{getIcon(navItem.icon)}}"></bk-list-navigation-item-icon>
                            <span bk-list-navigation-item-text>{{::navItem.label}}</span>
                            <bk-list-navigation-item-arrow aria-label="expand perspective group" is-expanded="navItem.expanded"></bk-list-navigation-item-arrow>
                            <bk-list>
                                <bk-list-navigation-item ng-repeat="navGroupItem in navItem.items track by navGroupItem.id" ng-click="$event.stopPropagation();switchPerspective(navGroupItem.id, navGroupItem.label)">
                                    <span bk-list-navigation-item-text>{{navGroupItem.label}}</span>
                                    <bk-list-navigation-item-indicator ng-if="isActive(navGroupItem.id, navItem.id)"></bk-list-navigation-item-indicator>
                                </bk-list-navigation-item>
                            </bk-list>
                            <bk-list-navigation-item-indicator ng-if="navItem.id === activeGroupId"></bk-list-navigation-item-indicator>
                        </bk-list-navigation-item>
                        <bk-list-navigation-group-header ng-if="!navItem.items && navItem.headerLabel">{{::navItem.headerLabel}}</bk-list-navigation-group-header>
                        <bk-list-navigation-item ng-repeat-end ng-if="!navItem.items" indicated="navItem.id === activeId" ng-click="switchPerspective(navItem.id, navItem.label)" title="{{::navItem.label}}">
                            <bk-list-navigation-item-icon icon-size="lg" svg-path="{{getIcon(navItem.icon)}}"></bk-list-navigation-item-icon>
                            <span bk-list-navigation-item-text>{{::navItem.label}}</span>
                            <bk-list-navigation-item-indicator ng-if="navItem.id === activeId"></bk-list-navigation-item-indicator>
                        </bk-list-navigation-item>
                    </bk-list>
                    <bk-list aria-label="Perspective list" ng-if="condensed">
                        <bk-list-navigation-item ng-repeat-start="navItem in config.perspectives track by navItem.id" ng-if="!navItem.items" indicated="navItem.id === activeId" ng-click="switchPerspective(navItem.id, navItem.label)" title="{{::navItem.label}}">
                            <bk-list-navigation-item-icon icon-size="lg" svg-path="{{getIcon(navItem.icon)}}"></bk-list-navigation-item-icon>
                            <span bk-list-navigation-item-text>{{::navItem.label}}</span>
                            <bk-list-navigation-item-indicator ng-if="navItem.id === activeId"></bk-list-navigation-item-indicator>
                        </bk-list-navigation-item>
                        <bk-list-navigation-item ng-repeat-end ng-if="navItem.items" ng-repeat="subNavItem in navItem.items track by subNavItem.id" indicated="subNavItem.id === activeId" ng-click="switchPerspective(subNavItem.id, subNavItem.label)" title="{{::subNavItem.label}}">
                            <bk-list-navigation-item-icon icon-size="lg" svg-path="{{getIcon(subNavItem.icon)}}"></bk-list-navigation-item-icon>
                            <span bk-list-navigation-item-text>{{::subNavItem.label}}</span>
                            <bk-list-navigation-item-indicator ng-if="subNavItem.id === activeId"></bk-list-navigation-item-indicator>
                        </bk-list-navigation-item>
                    </bk-list>
                </bk-vertical-nav-main-section>
                <bk-vertical-nav-utility-section ng-if="config.utilities.length" aria-label="utility navigation">
                    <bk-list>
                        <bk-list-navigation-item ng-repeat="navItem in config.utilities track by navItem.id" ng-click="switchPerspective(navItem.id, navItem.label)" title="{{::navItem.label}}">
                            <bk-list-navigation-item-icon icon-size="lg" svg-path="{{getIcon(navItem.icon)}}"></bk-list-navigation-item-icon>
                            <span bk-list-navigation-item-text>{{::navItem.label}}</span>
                            <bk-list-navigation-item-indicator ng-if="navItem.id === activeId"></bk-list-navigation-item-indicator>
                        </bk-list-navigation-item>
                    </bk-list>
                </bk-vertical-nav-utility-section>
            </bk-vertical-nav>
            <iframe ng-repeat-start="perspective in config.perspectives track by perspective.id" ng-if="!perspective.items" ng-show="perspective.id === activeId" title="{{::perspective.label}}" ng-src="{{::perspective.path}}" data-parameters="{{getDataParams(perspective.params)}}" loading="lazy"></iframe>
            <iframe ng-repeat-end ng-if="perspective.items" ng-repeat="subperspective in perspective.items track by subperspective.id" ng-show="subperspective.id === activeId" title="{{::subperspective.label}}" ng-src="{{::subperspective.path}}" data-parameters="{{getDataParams(subperspective.params)}}" loading="lazy"></iframe>
            <iframe ng-repeat="perspective in config.utilities track by perspective.id" ng-show="perspective.id === activeId" title="{{::perspective.label}}" ng-src="{{::perspective.path}}" data-parameters="{{getDataParams(perspective.params)}}" loading="lazy"></iframe>
        </div>`,
    })).directive('notifications', (notifications) => ({
        restrict: 'E',
        replace: true,
        link: (scope) => {
            const notificationHub = new NotificationHub();
            scope.notification = {
                type: '',
                title: '',
                description: '',
            };
            const onNotificationListener = notificationHub.onShow((data) => {
                scope.$applyAsync(() => {
                    scope.notification.type = data.type ?? 'information';
                    scope.notification.title = data.title;
                    scope.notification.description = data.description;
                });
                notifications.add({
                    id: new Date().valueOf(),
                    type: data.type ?? 'information',
                    title: data.title,
                    description: data.description,
                });
            });
            scope.hide = () => {
                scope.notification.type = '';
            };
            scope.$on('$destroy', () => notificationHub.removeMessageListener(onNotificationListener));
        },
        template: `<div ng-if="notification.type" class="notification-overlay">
            <bk-notification is-banner="true" style="width: 100%; max-width:33rem">
                <div bk-notification-content>
                    <div bk-notification-header>
                        <span bk-notification-icon="{{notification.type}}"></span>
                        <h4 bk-notification-title is-unread="true">{{notification.title}}</h4>
                    </div>
                    <p bk-notification-paragraph="">{{notification.description}}</p>
                </div>
                <div bk-notification-actions>
                    <bk-button aria-label="Close" state="transparent" glyph="sap-icon--decline" ng-click="hide()">
                    </bk-button>
                </div>
            </bk-notification>
        </div>`,
    })).directive('statusBar', () => ({
        restrict: 'E',
        replace: true,
        link: (scope) => {
            const statusBarHub = new StatusBarHub();
            scope.busy = '';
            scope.message = '';
            scope.label = '';
            scope.error = '';
            const busyListener = statusBarHub.onBusy((text) => scope.$apply(() => scope.busy = text));
            const messageListener = statusBarHub.onMessage((message) => scope.$apply(() => scope.message = message));
            const errorListener = statusBarHub.onError((message) => scope.$apply(() => scope.error = message));
            const labelListener = statusBarHub.onLabel((label) => scope.$apply(() => scope.label = label));
            scope.$on('$destroy', () => {
                statusBarHub.removeMessageListener(busyListener);
                statusBarHub.removeMessageListener(messageListener);
                statusBarHub.removeMessageListener(errorListener);
                statusBarHub.removeMessageListener(labelListener);
            });
        },
        template: `<div class="statusbar">
            <div class="statusbar-busy" ng-if="busy">
                <bk-busy-indicator size="m"></bk-busy-indicator>
                <span class="statusbar--text">{{ busy }}</span>
            </div>
            <div class="statusbar-message" ng-style="{'visibility': message ? 'visible':'hidden'}">
                <i class="statusbar--icon sap-icon--information"></i>
                <span class="statusbar--text">{{ message }}</span>
                <i class="statusbar--icon statusbar--link sap-icon--delete" ng-click="message = ''"></i>
            </div>
            <div class="statusbar-error" ng-style="{'visibility': error ? 'visible':'hidden'}">
                <i class="statusbar--icon sap-icon--message-warning"></i>
                <span class="statusbar--text">{{ error }}</span>
                <i class="statusbar--icon statusbar--link sap-icon--delete" ng-click="error = ''"></i>
            </div>
            <div class="statusbar-label" ng-if="label">{{ label }}</div>
        </div>`,
    }));