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
blimpkit.directive('bkWizard', () => ({
    restrict: 'EA',
    replace: true,
    transclude: true,
    scope: {
        currentStep: '<',
        completedSteps: '<',
        size: '@?',
    },
    controller: ['$scope', function ($scope) {
        $scope.steps = [];
        const validSizes = ['sm', 'md', 'lg', 'xl'];
        if ($scope.size && !validSizes.includes($scope.size)) {
            console.error(`bk-wizard error: 'size' must be one of: ${validSizes.join(', ')}`);
        }

        this.addStep = function (step) {
            $scope.steps.push(step);
        };

        this.removeStep = function (step) {
            let index = $scope.steps.indexOf(step);
            if (index >= 0)
                $scope.steps.splice(index, 1);
        }

        this.getSteps = function () {
            return $scope.steps;
        }

        this.onStepClick = function (index) {
            $scope.steps[index].stepClick({ step: index + 1 });
        }

        this.isStepCompleted = function (step) {
            let index = $scope.steps.indexOf(step);
            return index + 1 <= $scope.completedSteps;
        }

        this.isStepUpcoming = function (step) {
            let index = $scope.steps.indexOf(step);
            return index + 1 > $scope.currentStep;
        }

        this.isStepUpcomingIncompleted = function (step) {
            let index = $scope.steps.indexOf(step);
            return index + 1 > $scope.completedSteps && index + 1 > $scope.currentStep;
        }

        this.isStepCurrent = function (step) {
            let index = $scope.steps.indexOf(step);
            return index + 1 === $scope.currentStep;
        }

        this.hasCurrentStep = function () {
            return $scope.currentStep >= 1 && $scope.currentStep <= $scope.steps.length;
        }

        this.allStepsCompleted = function () {
            return $scope.completedSteps >= $scope.steps.length;
        }

        this.getSize = function () {
            return $scope.size;
        }

        this.isValidSize = function (size) {
            return validSizes.includes(size);
        }

        this.getValidSizes = function () {
            return validSizes;
        }
    }],
    template: '<section class="fd-wizard" ng-transclude></section>'
})).directive('bkWizardNavigation', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    require: '^^bkWizard',
    link: function (scope, _element, _attrs, wizCtrl) {
        scope.onStepClick = function (step) {
            wizCtrl.onStepClick(step);
        };
        scope.getSteps = () => wizCtrl.getSteps();
        scope.getProgressBarClasses = () => classNames('fd-wizard__progress-bar', {
            [`fd-wizard__progress-bar--${wizCtrl.getSize()}`]: wizCtrl.isValidSize(wizCtrl.getSize())
        });
        scope.getConnectorClasses = (step) => classNames('fd-wizard__connector', {
            'fd-wizard__connector--active': wizCtrl.isStepCompleted(step)
        });
        scope.getLabelContainerClasses = (step) => classNames('fd-wizard__label-container', {
            'fd-wizard__label-container--optional': !!step.optionalLabel
        });
        scope.getStepClasses = (step) => classNames('fd-wizard__step', {
            'fd-wizard__step--upcoming': wizCtrl.isStepUpcoming(step),
            'fd-wizard__step--current': wizCtrl.isStepCurrent(step),
            'fd-wizard__step--completed': wizCtrl.isStepCompleted(step),
            'fd-wizard__step--no-label': step.noLabel === true
        });
        scope.getAriaDisabled = (step) => wizCtrl.isStepUpcomingIncompleted(step) ? 'true' : undefined;
        scope.getAriaCurrent = (step) => wizCtrl.isStepCurrent(step) ? 'step' : undefined;
        scope.getIndicatorLabel = (step) => !step.indicatorGlyph ? step.indicatorLabel : undefined;
    },
    template: `<nav class="fd-wizard__navigation">
        <ul ng-class="getProgressBarClasses()">
            <li ng-repeat="step in getSteps()" ng-class="getStepClasses(step)">
                <div class="fd-wizard__step-wrapper">
                    <a ng-click="onStepClick($index)" class="fd-wizard__step-container" tabindex="0" aria-label="{{ step.label }}" ng-attr-aria-disabled="{{getAriaDisabled(step)}}" ng-attr-aria-current="{{getAriaCurrent(step) }}">
                        <span class="fd-wizard__step-indicator">{{ getIndicatorLabel(step) }}
                            <i ng-if="step.indicatorGlyph" class="fd-wizard__icon {{step.indicatorGlyph}}" role="presentation"></i>
                        </span>
                        <div ng-class="getLabelContainerClasses(step)">
                            <span class="fd-wizard__label">{{ step.label }}</span>
                            <span ng-if="step.optionalLabel" class="fd-wizard__optional-text">{{ step.optionalLabel }}</span>
                        </div>
                    </a>
                    <span ng-if="!$last" ng-class="getConnectorClasses(step)"></span>
                </div>
            </li>
        </ul>
    </nav>`
})).directive('bkWizardContent', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    require: '^^bkWizard',
    scope: {
        background: '@?',
        size: '@',
    },
    link: (scope, _element, _attrs, wizCtrl) => {
        const validBackgrounds = ['solid', 'list', 'transparent'];
        if (scope.size && !wizCtrl.isValidSize(scope.size)) {
            console.error(`bk-wizard-content error: 'size' must be one of: ${wizCtrl.getValidSizes().join(', ')}`);
        }
        if (scope.background && !validBackgrounds.includes(scope.background)) {
            console.error(`bk-wizard-content error: 'background' must be one of: ${validBackgrounds.join(', ')}`);
        }
        scope.getClasses = () => classNames('fd-wizard__content', {
            [`fd-wizard__content--${scope.size}`]: wizCtrl.isValidSize(scope.size),
            [`fd-wizard__content--${scope.background}`]: validBackgrounds.includes(scope.background)
        });
    },
    template: '<section ng-class="getClasses()" ng-transclude></section>'
})).directive('bkWizardStep', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    require: '^^bkWizard',
    scope: {
        label: '@',
        size: '@?',
        optionalLabel: '@?',
        indicatorGlyph: '@',
        indicatorLabel: '@',
        noLabel: '<?',
        stepClick: '&',
    },
    link: (scope, _element, _attrs, wizCtrl) => {
        if (scope.size && !wizCtrl.isValidSize(scope.size)) {
            console.error(`bk-wizard-content error: 'size' must be one of: ${wizCtrl.getValidSizes().join(', ')}`);
        }
        wizCtrl.addStep(scope);
        scope.isStepCurrent = () => wizCtrl.isStepCurrent(scope);
        scope.onNextClick = () => {
            wizCtrl.gotoNextStep();
        };
        scope.getClasses = () => classNames('fd-wizard__step-content-container', {
            [`fd-wizard__step-content-container--${scope.size}`]: wizCtrl.isValidSize(scope.size),
        });
        scope.$on('$destroy', () => {
            wizCtrl.removeStep(scope);
        });
    },
    template: '<div ng-show="isStepCurrent()" ng-class="getClasses()" ng-transclude></div>'
})).directive('bkWizardSummary', (classNames) => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    require: '^^bkWizard',
    scope: {
        size: '@?',
    },
    link: (scope, _element, _attrs, wizCtrl) => {
        if (scope.size && !wizCtrl.isValidSize(scope.size)) {
            console.error(`bk-wizard-content error: 'size' must be one of: ${wizCtrl.getValidSizes().join(', ')}`);
        }
        scope.allStepsCompleted = () => wizCtrl.allStepsCompleted() && !wizCtrl.hasCurrentStep();
        scope.getClasses = () => classNames('fd-wizard__step-content-container', {
            [`fd-wizard__step-content-container--${scope.size}`]: wizCtrl.isValidSize(scope.size),
        });
    },
    template: '<div ng-show="allStepsCompleted()" ng-class="getClasses()" ng-transclude></div>'
})).directive('bkWizardNextStep', () => ({
    restrict: 'EA',
    transclude: true,
    replace: true,
    template: '<div class="fd-wizard__next-step" ng-transclude></div>'
}));