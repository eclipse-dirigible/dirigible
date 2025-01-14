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
angular.module('edmDetails', ['blimpKit', 'platformView'])
    .constant('Dialogs', new DialogHub())
    .directive('stringToNumber', () => ({
        require: 'ngModel',
        restrict: 'A',
        replace: false,
        link: (_scope, _element, _attrs, ngModel) => {
            ngModel.$parsers.push((value) => {
                return '' + value;
            });
            ngModel.$formatters.push((value) => {
                return parseFloat(value);
            });
        }
    }))
    .controller('DetailsController', ($scope, $http, Dialogs, ViewParameters) => {
        $scope.state = {
            isBusy: true,
            error: false,
            busyText: 'Loading...',
        };
        $scope.selectedTab = 'p';
        $scope.switchTab = (tabId) => {
            $scope.showInnerDialog = false;
            $scope.selectedTab = tabId;
        };
        $scope.showInnerDialog = false;
        $scope.editElement = {
            editType: 'Add', // Update
            index: 0,
            path: '',
            id: '',
            label: '',
            icon: '',
            order: 0,
            url: '',
        };
        $scope.forms = {
            newForm: {},
        };
        $scope.inputRules = {
            excluded: [],
        };
        $scope.inputRulesName = {
            patterns: ['^[A-Za-z0-9_]+$'],
            excluded: [],
        };
        $scope.icons = [];
        $scope.loadIcons = () => {
            $http({
                method: 'GET',
                url: '/services/web/resources/unicons/list.json',
                headers: {
                    'Dirigible-Editor': 'EntityDataModeler'
                },
            }).then((response) => {
                $scope.icons = response.data;
                $scope.state.isBusy = false;
            }, (response) => {
                if (response.data) {
                    if ('error' in response.data) {
                        $scope.state.error = true;
                        $scope.errorMessage = response.data.error.message;
                        console.error(response.data.error);
                        return;
                    }
                }
                $scope.state.error = true;
                $scope.errorMessage = 'There was an error while loading the icons.';
            });
        };
        $scope.cancel = () => {
            if (!$scope.state.error && $scope.showInnerDialog) $scope.showInnerDialog = false;
            else Dialogs.closeWindow();
        };
        $scope.add = () => {
            $scope.inputRules.excluded.length = 0;
            $scope.editElement.editType = 'Add';
            if ($scope.selectedTab === 'p') {
                for (let i = 0; i < $scope.dataParameters.perspectives.length; i++) {
                    $scope.inputRules.excluded.push($scope.dataParameters.perspectives[i].id);
                }
                $scope.editElement.id = '';
                $scope.editElement.order = 0;
            } else {
                for (let i = 0; i < $scope.dataParameters.navigations.length; i++) {
                    $scope.inputRules.excluded.push($scope.dataParameters.navigations[i].path);
                }
                $scope.editElement.path = '';
                $scope.editElement.url = '';
            }
            $scope.editElement.label = '';
            $scope.editElement.icon = '';
            $scope.showInnerDialog = true;
        };
        $scope.edit = (index) => {
            $scope.inputRules.excluded.length = 0;
            if ($scope.selectedTab === 'p') {
                for (let i = 0; i < $scope.dataParameters.perspectives.length; i++) {
                    if (i !== index)
                        $scope.inputRules.excluded.push($scope.dataParameters.perspectives[i].id);
                }
            } else {
                for (let i = 0; i < $scope.dataParameters.navigations.length; i++) {
                    if (i !== index)
                        $scope.inputRules.excluded.push($scope.dataParameters.navigations[i].path);
                }
            }
            $scope.editElement.editType = 'Update';
            $scope.editElement.index = index;
            $scope.showInnerDialog = true;
        };
        $scope.delete = (index) => {
            if ($scope.selectedTab === 'p')
                $scope.dataParameters.perspectives.splice(index, 1);
            else $scope.dataParameters.navigations.splice(index, 1);
        };
        $scope.save = () => {
            if (!$scope.state.error) {
                $scope.state.busyText = 'Saving...';
                $scope.state.isBusy = true;
                Dialogs.postMessage({
                    topic: 'edmEditor.navigation.details',
                    data: {
                        perspectives: $scope.dataParameters.perspectives,
                        navigations: $scope.dataParameters.navigations,
                    }
                });
            }
        };
        $scope.innerAction = () => {
            if ($scope.editElement.editType === 'Add') {
                if ($scope.selectedTab === 'p')
                    $scope.dataParameters.perspectives.push({
                        id: $scope.editElement.id,
                        label: $scope.editElement.label,
                        icon: $scope.editElement.icon,
                        order: $scope.editElement.order,
                    });
                else $scope.dataParameters.navigations.push({
                    path: $scope.editElement.path,
                    label: $scope.editElement.label,
                    icon: $scope.editElement.icon,
                    url: $scope.editElement.url,
                });
            }
            $scope.showInnerDialog = false;
        };
        $scope.dataParameters = ViewParameters.get();
        $scope.loadIcons();
    });