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
blimpkit.directive('bkMenu', function ($window, $timeout, $injector, backdrop, classNames) {
    if (!$injector.has('bkScrollbarDirective')) {
        console.error('bk-menu requires the bk-scrollbar widget to be loaded.');
        return {};
    }
    return {
        restrict: 'E',
        transclude: true,
        replace: true,
        require: ['?^^bkPopover', '?^^bkSplitButton'],
        scope: {
            maxHeight: '@?',
            canScroll: '<?',
            hasIcons: '<?',
            show: '=?',
            noBackdrop: '<?',
            noShadow: '<?',
            closeOnOuterClick: '<?',
        },
        link: {
            pre: function (scope, _element, attrs, parentCtrls) {
                if (!Object.prototype.hasOwnProperty.call(attrs, 'ariaLabel'))
                    console.error('bk-menu error: You must set the "aria-label" attribute');
                if (!angular.isDefined(scope.show))
                    scope.show = true;
                if (!angular.isDefined(scope.noBackdrop))
                    scope.noBackdrop = false;
                if (parentCtrls[0] !== null || parentCtrls[1] !== null) {
                    scope.noBackdrop = true;
                    scope.noShadow = true;
                    scope.closeOnOuterClick = false;
                } else if (scope.noBackdrop)
                    scope.closeOnOuterClick = false;
                else scope.closeOnOuterClick = true;;
                scope.defaultHeight = 16;
            },
            post: function (scope, element) {
                scope.setDefault = function () {
                    let rect = element[0].getBoundingClientRect();
                    scope.defaultHeight = $window.innerHeight - rect.top;
                };
                function resizeEvent() {
                    scope.$apply(function () { scope.setDefault() });
                }
                if (scope.maxHeight)
                    $window.addEventListener('resize', resizeEvent);
                scope.backdropClickEvent = function () {
                    scope.$apply(function () { scope.show = false; });
                };
                scope.backdropRightClickEvent = function (event) {
                    event.stopPropagation();
                    scope.$apply(function () { scope.show = false; });
                };
                const showWatch = scope.$watch('show', function () {
                    if (!scope.noBackdrop) {
                        if (scope.show) {
                            backdrop.activate();
                            if (scope.closeOnOuterClick) {
                                backdrop.element.addEventListener('click', scope.backdropClickEvent);
                                backdrop.element.addEventListener('contextmenu', scope.backdropRightClickEvent);
                            }
                        } else {
                            backdrop.deactivate();
                            if (scope.closeOnOuterClick) {
                                backdrop.element.removeEventListener('click', scope.backdropClickEvent);
                                backdrop.element.removeEventListener('contextmenu', scope.backdropRightClickEvent);
                            }
                        }
                    }
                });
                scope.getMenuClasses = function () {
                    if (scope.canScroll) element[0].style.maxHeight = `${scope.maxHeight || scope.defaultHeight}px`;
                    else element[0].style.removeProperty('max-height');
                    return classNames('fd-menu', { 'fd-menu--overflow': scope.canScroll === true, 'fd-menu--icons': scope.hasIcons === true });
                };
                scope.getListClasses = () => classNames('fd-menu__list', {
                    'fd-menu__list--no-shadow': scope.noShadow === true
                });
                function cleanUp() {
                    $window.removeEventListener('resize', resizeEvent);
                    backdrop.element.removeEventListener('click', scope.backdropClickEvent);
                    backdrop.element.removeEventListener('contextmenu', scope.backdropRightClickEvent);
                    backdrop.cleanUp();
                    showWatch();
                }
                scope.$on('$destroy', cleanUp);
                const contentLoaded = scope.$watch('$viewContentLoaded', function () {
                    $timeout(() => {
                        scope.setDefault();
                        contentLoaded();
                    }, 0);
                });
            },
        },
        template: '<nav ng-show="show" ng-class="getMenuClasses()"><ul ng-class="getListClasses()" role="menu" tabindex="-1" ng-transclude></ul></nav>'
    }
}).directive('bkMenuItem', (classNames) => ({
    restrict: 'E',
    transclude: false,
    replace: true,
    scope: {
        title: '@',
        shortcut: '@?',
        hasSeparator: '<?',
        isActive: '<?',
        isSelected: '<?',
        isDisabled: '<?',
        leftIconClass: '@?',
        leftIconPath: '@?',
        rightIconClass: '@?',
        rightIconPath: '@?',
        link: '@?',
    },
    link: (scope) => {
        scope.getClasses = () => classNames('fd-menu__link', {
            'is-active': scope.isActive === true,
            'is-disabled': scope.isDisabled === true,
            'is-selected': scope.isSelected === true,
            'has-separator': scope.hasSeparator === true,
        });
        scope.getItemClasses = () => classNames('fd-menu__item', {
            'has-separator': scope.hasSeparator === true,
        });
    },
    innerTemplate: `<span ng-if="leftIconClass || leftIconPath" class="fd-menu__addon-before">
        <i class="{{ leftIconPath ? 'bk-icon--svg sap-icon' : leftIconClass }}" role="presentation"><ng-include ng-if="leftIconPath" src="leftIconPath"></ng-include></i>
    </span>
    <span class="fd-menu__title">{{title}}</span>
    <span ng-if="shortcut" class="fd-menu__shortcut">{{shortcut}}</span>
    <span ng-if="rightIconClass || rightIconPath" class="fd-menu__addon-after">
        <i class="{{ rightIconPath ? 'bk-icon--svg sap-icon' : rightIconClass }}" role="presentation"><ng-include ng-if="rightIconPath" src="rightIconPath"></ng-include></i>
    </span>`,
    get template() {
        return `<li ng-class="getItemClasses()" role="presentation" tabindex="-1">
            <a ng-if="link" href="{{link}}" ng-class="getClasses()" role="menuitem" tabindex="{{ isDisabled ? -1 : 0 }}">${this.innerTemplate}</a>
            <span ng-if="!link" ng-class="getClasses()" role="menuitem" tabindex="{{ isDisabled ? -1 : 0 }}">${this.innerTemplate}</span>
        </li>`
    }
})).directive('bkMenuSublist', (uuid, $window, ScreenEdgeMargin, classNames) => ({
    restrict: 'E',
    transclude: true,
    replace: true,
    scope: {
        title: '@',
        hasSeparator: '<?',
        maxHeight: '@?',
        canScroll: '<?',
        isDisabled: '<?',
        iconClass: '@?',
        iconPath: '@?',
    },
    link: {
        pre: function (scope) {
            scope.sublistId = `sl${uuid.generate()}`;
            scope.isExpanded = false;
            scope.defaultHeight = $window.innerHeight - ScreenEdgeMargin.DOUBLE;
        },
        post: function (scope, element) {
            let toHide = 0;
            function resizeEvent() {
                if (!scope.isDisabled) {
                    scope.$apply(function () {
                        scope.defaultHeight = $window.innerHeight - ScreenEdgeMargin.DOUBLE;
                        scope.setPosition();
                    });
                }
            }
            $window.addEventListener('resize', resizeEvent);
            scope.pointerHandler = function (e) {
                if (!element[0].contains(e.target)) {
                    scope.$apply(scope.hideSubmenu());
                }
            };
            function focusoutEvent(event) {
                if (!element[0].contains(event.relatedTarget)) {
                    scope.$apply(scope.hideSubmenu());
                }
            }
            function pointerupEvent(e) {
                let listItem;
                let list;
                if (e.target.tagName !== "LI") {
                    listItem = e.target.closest('li');
                } else {
                    listItem = e.target;
                }
                for (let i = 0; i < listItem.children.length; i++) {
                    if (listItem.children[i].tagName === 'UL') {
                        list = listItem.children[i];
                    }
                }
                if (list && list.id === scope.sublistId) {
                    e.originalEvent.isSubmenuItem = true;
                    if (e.originalEvent.pointerType !== 'mouse')
                        scope.$apply(scope.show());
                }
            }
            element.on('pointerup', pointerupEvent);
            scope.getItemClasses = () => classNames('fd-menu__item', {
                'has-separator': scope.hasSeparator,
            });
            scope.getIconClasses = () => classNames({
                [scope.iconClass]: scope.iconClass && !scope.iconPath,
                'bk-icon--svg sap-icon': !scope.iconClass && scope.iconPath,
            });
            scope.getClasses = () => classNames('fd-menu__link has-child', {
                'is-expanded': scope.isExpanded === true,
                'is-disabled': scope.isDisabled === true,
            });
            scope.setPosition = function () {
                if (!angular.isDefined(scope.menu)) scope.menu = element[0].querySelector(`#${scope.sublistId}`);
                requestAnimationFrame(function () {
                    const rect = scope.menu.getBoundingClientRect();
                    const bottom = $window.innerHeight - ScreenEdgeMargin.FULL - rect.bottom;
                    const right = $window.innerWidth - rect.right;
                    if (bottom < 0) scope.menu.style.top = `${bottom}px`;
                    if (right < 0) {
                        scope.menu.style.left = `${scope.menu.offsetWidth * -1}px`;
                        scope.menu.classList.add('bk-submenu--left');
                    }
                });
            };
            scope.show = function () {
                if (toHide) clearTimeout(toHide);
                if (!scope.isDisabled && !scope.isExpanded) {
                    scope.isExpanded = true;
                    scope.setPosition();
                    $window.addEventListener('pointerup', scope.pointerHandler);
                }
            };
            scope.hideSubmenu = function () {
                scope.isExpanded = false;
                scope.menu.style.removeProperty('top');
                scope.menu.style.removeProperty('left');
                scope.menu.classList.remove('bk-submenu--left');
                element.off('focusout', focusoutEvent);
                $window.removeEventListener('pointerup', scope.pointerHandler);
            };
            scope.hide = function (event) {
                if (!scope.isDisabled && scope.isExpanded) {
                    if (event.relatedTarget) {
                        if (typeof event.relatedTarget.className === 'string' && event.relatedTarget.className.includes('fd-menu__')) {
                            scope.hideSubmenu();
                        } else if (!element[0].contains(event.relatedTarget)) {
                            toHide = setTimeout(function () {
                                scope.$apply(scope.hideSubmenu());
                            }, 300);
                        }
                    } else if (!element[0] === event.currentTarget) { // Firefox tooltip fix
                        scope.hideSubmenu();
                    }
                }
            };

            scope.focus = function () {
                element.on('focusout', focusoutEvent);
                scope.show();
            };

            function cleanUp() {
                element.off('pointerup', pointerupEvent);
                element.off('focusout', focusoutEvent);
                $window.removeEventListener('resize', resizeEvent);
                $window.removeEventListener('pointerup', scope.pointerHandler);
            }
            scope.$on('$destroy', cleanUp);
        }
    },
    template: `<li ng-class="getItemClasses()" role="presentation" ng-mouseenter="show()" ng-mouseleave="hide($event)" tabindex="0" ng-focus="focus()">
        <span aria-controls="{{sublistId}}" aria-expanded="{{isExpanded}}" aria-haspopup="true" role="menuitem" ng-class="getClasses()">
            <span ng-if="iconClass || iconPath" class="fd-menu__addon-before"><i ng-class="getIconClasses()" role="presentation"><ng-include ng-if="iconPath" src="iconPath"></ng-include></i></span>
            <span class="fd-menu__title">{{title}}</span>
            <span class="fd-menu__addon-after fd-menu__addon-after--submenu"></span>
        </span>
        <ul ng-if="canScroll && !isDisabled" class="fd-menu__sublist fd-menu--overflow bk-menu__sublist--overflow" bk-scrollbar id="{{sublistId}}" aria-hidden="{{!isExpanded}}" role="menu" style="max-height:{{ maxHeight || defaultHeight }}px;" ng-transclude></ul>
        <ul ng-if="!canScroll && !isDisabled" class="fd-menu__sublist" id="{{sublistId}}" aria-hidden="{{!isExpanded}}" role="menu" ng-transclude></ul>
    </li>`
}));