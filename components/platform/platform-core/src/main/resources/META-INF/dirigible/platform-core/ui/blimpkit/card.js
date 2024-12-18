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
blimpkit.directive('bkCard', (classNames, uuid) => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: {
        cardType: '@?',
        compact: '<?',
        inList: '<?'
    },
    controller: ['$scope', '$attrs', function ($scope, $attrs) {
        if (!Object.prototype.hasOwnProperty.call($attrs, 'ariaRoledescription'))
            console.error('bk-card: You should provide a description of the card using the "aria-roledescription" attribute');
        $scope.cardId = uuid.generate();
        this.getCardId = function () {
            return $scope.cardId;
        };
        this.isInList = function () {
            return $scope.inList === true;
        };
        $scope.getClasses = () => classNames('fd-card', {
            'fd-card--object': $scope.cardType === 'object',
            'fd-card--table': $scope.cardType === 'table',
            'fd-card--compact': $scope.compact === true
        });
    }],
    template: `<div ng-class="getClasses()" role="{{inList === true ? 'listitem' : 'region'}}" ng-attr-tabindex="{{ inList === true ? 0 : undefined}}" aria-labelledby="{{cardId}}" ng-transclude></div>`,
})).directive('bkCardMedia', (classNames) => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    scope: {
        withPadding: '<?',
        link: "@",
    },
    link: (scope, _element, attrs) => {
        if (!Object.prototype.hasOwnProperty.call(attrs, 'ariaRoledescription'))
            console.error('bk-card-media: You should provide a description of the media using the "aria-roledescription" attribute');
        scope.getClasses = () => classNames('fd-card__media', {
            'fd-card__media--with-padding': scope.withPadding === true
        });
    },
    template: `<div ng-class="getClasses()" role="group"><div class="fd-card__media-image-container">
        <img class="fd-card__media-image" ng-src="{{link}}" role="presentation" />
    </div></div>`,
})).directive('bkCardHeader', (classNames) => ({
    restrict: 'E',
    replace: true,
    require: '^^bkCard',
    transclude: {
        'avatar': '?bkAvatar'
    },
    scope: {
        title: '@',
        subtitle: '@?',
        interactive: '<?',
        status: '@?',
        statusType: '@?',
        isCounter: '<?',
        state: '@?',
        roleDescription: '@?',
        description: '@?'
    },
    link: function (scope, _element, _attrs, cardCtrl, $transclude) {
        scope.cardId = cardCtrl.getCardId();
        const cardInList = cardCtrl.isInList();
        if (cardInList && scope.interactive === true) console.error('bk-card-header: header cannot be interactive when the card is in a list.');

        scope.getClasses = () => classNames('fd-card__header', {
            'fd-card__header--interactive': scope.interactive === true && !cardInList
        });

        const states = ['hover', 'active', 'focus'];

        scope.getContainerClasses = () => classNames('fd-card__header-main-container', {
            [`is-${scope.status}`]: scope.state && states.includes(scope.state),
        });

        scope.isAvatarFilled = () => $transclude.isSlotFilled('avatar');

        const statuses = ['negative', 'critical', 'positive', 'informative'];

        scope.getStatusClasses = () => {
            if (scope.interactive && !scope.description) console.error('bk-card-header: you must provide a description when the header is interactive.');
            return classNames('fd-object-status', {
                [`fd-object-status--${scope.statusType}`]: scope.statusType && statuses.includes(scope.statusType),
                'fd-card__counter': scope.isCounter === true
            })
        };
    },
    template: `<div ng-class="getClasses()" role="group" aria-roledescription="{{roleDescription || 'Card Header'}}">
        <div class="fd-card__header-main">
            <div ng-class="getContainerClasses()" ng-attr-role="{{ interactive === true ? 'button' : undefined}}" ng-attr-tabindex="{{ interactive === true ? 0 : undefined}}" aria-description="{{description}}">
                <ng-transclude ng-if="isAvatarFilled()" ng-transclude-slot="avatar"></ng-transclude>
                <div class="fd-card__header-text">
                    <div class="fd-card__title-area">
                        <div class="fd-card__title" id={{cardId}} role="heading" aria-level="3">{{title}}</div>
                    </div>
                    <div ng-if="subtitle" class="fd-card__subtitle-area">
                        <div class="fd-card__subtitle">{{subtitle}}</div>
                    </div>
                </div>
            </div>
            <div class="fd-card__header-main-actions">
                <ng-transclude></ng-transclude>
                <span ng-class="getStatusClasses()">{{status}}</span>
            </div>
        </div>
    </div>`
})).directive('bkCardContent', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    link: (_scope, _element, attrs) => {
        if (!Object.prototype.hasOwnProperty.call(attrs, 'ariaRoledescription'))
            console.error('bk-card-content: You should provide a description of the group using the "aria-roledescription" attribute');
    },
    template: '<div class="fd-card__content" role="group" ng-transclude></div>',
})).directive('bkCardFooter', () => ({
    restrict: 'E',
    replace: true,
    transclude: true,
    template: '<div class="fd-card__footer"><div class="fd-card__footer-actions" ng-transclude></div></div>',
}));