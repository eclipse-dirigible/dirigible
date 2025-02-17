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
blimpkit.directive('bkPopover', (uuid, $window, $injector, backdrop) => {
    if (!$injector.has('bkScrollbarDirective')) {
        console.error('bk-popover requires the bk-scrollbar widget to be loaded.');
        return {};
    }
    return {
        restrict: 'E',
        transclude: true,
        replace: true,
        scope: {
            closeInnerclick: '<?'
        },
        controller: ['$scope', '$element', function ($scope, $element) {
            let popoverControl;
            const popoverId = `p${uuid.generate()}`;
            if (!angular.isDefined($scope.closeInnerclick))
                $scope.closeInnerclick = true;

            let isHidden = true;
            $scope.pointerHandler = function (e) {
                if (!$element[0].contains(e.target)) {
                    hidePopover();
                }
            };
            function focusoutEvent(e) {
                if (e.relatedTarget && !$element[0].contains(e.relatedTarget)) {
                    hidePopover();
                }
            }
            function pointerupEvent(e) {
                if (e.target.attributes['of-close-btn']) return;
                else if (e.originalEvent && e.originalEvent.isSubmenuItem) return;
                else if (popoverControl && e.target === popoverControl) return;
                else if ($element[0].contains(e.target) && !isHidden) hidePopover();
            }
            if ($scope.closeInnerclick) {
                $element.on('focusout', focusoutEvent);
                $element.on('pointerup', pointerupEvent);
            }

            this.getPopoverId = function () {
                return popoverId;
            };

            let toggleBody;

            this.toggleBody = function (toggle) {
                toggleBody = toggle;
            };

            function hidePopover() {
                if (popoverControl) popoverControl.setAttribute('aria-expanded', 'false');
                toggleBody(false);
                isHidden = true;
                $window.removeEventListener('pointerup', $scope.pointerHandler);
                backdrop.deactivate();
            };

            this.togglePopover = function () {
                if (!popoverControl) popoverControl = $element[0].querySelector(`[aria-controls="${popoverId}"]`);
                if (isHidden) {
                    toggleBody(true);
                    popoverControl.setAttribute('aria-expanded', 'true');
                    isHidden = false;
                    $window.addEventListener('pointerup', $scope.pointerHandler);
                    backdrop.activate();
                } else {
                    hidePopover();
                };
            };

            function cleanUp() {
                $element.off('focusout', focusoutEvent);
                $element.off('pointerup', pointerupEvent);
                $window.removeEventListener('pointerup', $scope.pointerHandler);
                backdrop.cleanUp();
            }
            $scope.$on('$destroy', cleanUp);
        }],
        template: '<div class="fd-popover" ng-transclude></div>',
    }
}).directive('bkPopoverControl', () => ({
    restrict: 'E',
    transclude: true,
    require: '?^^bkPopover',
    replace: true,
    link: (scope, element, _attrs, popoverCtrl) => {
        function clickEvent() {
            popoverCtrl.togglePopover();
        }
        if (popoverCtrl) {
            scope.control = element[0].firstElementChild;
            scope.control.setAttribute('aria-controls', popoverCtrl.getPopoverId());
            scope.control.setAttribute('aria-expanded', 'false');
            scope.control.setAttribute('aria-haspopup', 'true');
            scope.control.addEventListener("click", clickEvent);
            function cleanUp() {
                scope.control.removeEventListener('click', clickEvent);
            }
            scope.$on('$destroy', cleanUp);
        }
    },
    template: '<div class="fd-popover__control" ng-transclude></div>',
})).directive('bkPopoverBody', ($window, $timeout, ScreenEdgeMargin, classNames) => ({
    restrict: 'E',
    transclude: true,
    require: ['?^^bkPopover', '?^^bkSplitButton'],
    replace: true,
    scope: {
        maxHeight: '<?',
        align: '@?',
        noArrow: '<?',
        dropdownFill: '<?',
        canScroll: '<?',
    },
    link: (scope, element, _attrs, popoverCtrls) => {
        const ctrlIndex = popoverCtrls[0] ? 0 : 1;
        scope.defaultHeight = 44;
        scope.hidden = true;
        if (!angular.isDefined(scope.canScroll)) scope.canScroll = true;
        function autoAlign() {
            const rect = element[0].getBoundingClientRect();
            let bottom = 0;
            if (rect.top > $window.innerHeight / 2) {
                bottom = $window.innerHeight - ScreenEdgeMargin.FULL - (scope.maxHeight ? rect.top + parseInt(scope.maxHeight) : rect.bottom);
            }
            const right = $window.innerWidth - rect.right;
            if (ctrlIndex === 1) {
                if (bottom < 0) {
                    scope.align = 'top-right';
                } else scope.align = 'bottom-right';
            } else {
                if (bottom < 0 && right < 0) {
                    scope.align = 'top-right';
                } else if (bottom < 0 && right >= 0) {
                    scope.align = 'top-left';
                } else if (bottom >= 0 && right < 0) {
                    scope.align = 'bottom-right';
                } else scope.align = undefined;
            }
        };

        function toggleBody(show) {
            if (show) {
                const rect = element[0].getBoundingClientRect();
                if (scope.align && scope.align.startsWith('top')) scope.defaultHeight = rect.bottom - ScreenEdgeMargin.FULL;
                else scope.defaultHeight = $window.innerHeight - ScreenEdgeMargin.FULL - rect.top;
                if (scope.maxHeight && scope.defaultHeight > scope.maxHeight) scope.defaultHeight = scope.maxHeight;
            }
            if (ctrlIndex === 1) scope.hidden = !show;
            else scope.$evalAsync(() => { scope.hidden = !show; });
        }

        scope.popoverId = popoverCtrls[ctrlIndex].getPopoverId();
        popoverCtrls[ctrlIndex].toggleBody(toggleBody);

        scope.getClasses = () => classNames('fd-popover__body', {
            'fd-popover__body--no-arrow': scope.noArrow,
            'fd-popover__body--dropdown fd-popover__body--dropdown-fill': scope.dropdownFill,
            'fd-popover__body--above': scope.align === 'top-left',
            'fd-popover__body--arrow-bottom': scope.align === 'top-left' && !scope.noArrow,
            'fd-popover__body--above fd-popover__body--center': scope.align === 'top',
            'fd-popover__body--arrow-bottom fd-popover__body--arrow-x-center': scope.align === 'top' && !scope.noArrow,
            'fd-popover__body--above fd-popover__body--right': scope.align === 'top-right',
            'fd-popover__body--arrow-bottom fd-popover__body--arrow-x-end': scope.align === 'top-right' && !scope.noArrow,
            'fd-popover__body--center': scope.align === 'bottom',
            'fd-popover__body--arrow-x-center': scope.align === 'bottom' && !scope.noArrow,
            'fd-popover__body--right': scope.align === 'bottom-right',
            'fd-popover__body--arrow-x-end': scope.align === 'bottom-right' && !scope.noArrow,
            'fd-popover__body--before': scope.align === 'left-top',
            'fd-popover__body--arrow-right': scope.align === 'left-top' && !scope.noArrow,
            'fd-popover__body--before fd-popover__body--middle': scope.align === 'left',
            'fd-popover__body--arrow-right fd-popover__body--arrow-y-center': scope.align === 'left' && !scope.noArrow,
            'fd-popover__body--before fd-popover__body--bottom': scope.align === 'left-bottom',
            'fd-popover__body--arrow-right fd-popover__body--arrow-y-bottom': scope.align === 'left-bottom' && !scope.noArrow,
            'fd-popover__body--after': scope.align === 'right-top',
            'fd-popover__body--arrow-left': scope.align === 'right-top' && !scope.noArrow,
            'fd-popover__body--after fd-popover__body--middle': scope.align === 'right',
            'fd-popover__body--arrow-left fd-popover__body--arrow-y-center': scope.align === 'right' && !scope.noArrow,
            'fd-popover__body--after fd-popover__body--bottom': scope.align === 'right-bottom',
            'fd-popover__body--arrow-left fd-popover__body--arrow-y-bottom': scope.align === 'right-bottom' && !scope.noArrow,
        });
        const contentLoaded = scope.$watch('$viewContentLoaded', function () {
            $timeout(() => {
                if (!scope.align) autoAlign();
                contentLoaded();
            }, 0);
        });
    },
    template: `<div id="{{ popoverId }}" ng-class="getClasses()" aria-hidden="{{hidden}}">
        <div ng-if="canScroll" class="fd-popover__wrapper" bk-scrollbar style="max-height:{{ defaultHeight }}px;" ng-transclude></div>
        <ng-transclude ng-if="!canScroll"></ng-transclude>
    </div>`,
}));