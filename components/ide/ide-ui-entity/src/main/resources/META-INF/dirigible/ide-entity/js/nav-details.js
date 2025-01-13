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
angular.module('edmDetails', ['ideUI', 'ideView'])
    .directive('stringToNumber', function () {
        return {
            require: 'ngModel',
            link: function (scope, element, attrs, ngModel) {
                ngModel.$parsers.push(function (value) {
                    return '' + value;
                });
                ngModel.$formatters.push(function (value) {
                    return parseFloat(value);
                });
            }
        };
    })
    .controller('DetailsController', ['$scope', '$http', 'messageHub', 'ViewParameters', function ($scope, $http, messageHub, ViewParameters) {
        $scope.state = {
            isBusy: true,
            error: false,
            busyText: "Loading...",
        };
        $scope.tabNumber = 0;
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
        $scope.loadIcons = function () {
            $http({
                method: 'GET',
                url: '/services/web/resources/unicons/list.json',
                headers: {
                    'Dirigible-Editor': 'EntityDataModeler'
                },
            }).then(function (response) {
                $scope.icons = response.data;
                $scope.state.isBusy = false;
            }, function (response) {
                if (response.data) {
                    if ("error" in response.data) {
                        $scope.state.error = true;
                        $scope.errorMessage = response.data.error.message;
                        console.log(response.data.error);
                        return;
                    }
                }
                $scope.state.error = true;
                $scope.errorMessage = "There was an error while loading the icons.";
            });
        };
        $scope.cancel = function () {
            if (!$scope.state.error && $scope.showInnerDialog) $scope.showInnerDialog = false;
            else messageHub.closeDialogWindow("edmNavDetails");
        };
        $scope.add = function () {
            $scope.inputRules.excluded.length = 0;
            $scope.editElement.editType = 'Add';
            if ($scope.tabNumber === 0) {
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
        $scope.edit = function (index) {
            $scope.inputRules.excluded.length = 0;
            if ($scope.tabNumber === 0) {
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
        $scope.delete = function (index) {
            if ($scope.tabNumber === 0)
                $scope.dataParameters.perspectives.splice(index, 1);
            else $scope.dataParameters.navigations.splice(index, 1);
        };
        $scope.save = function () {
            if (!$scope.state.error) {
                $scope.state.busyText = "Saving";
                $scope.state.isBusy = true;
                messageHub.postMessage('edmEditor.navigation.details', {
                    perspectives: $scope.dataParameters.perspectives,
                    navigations: $scope.dataParameters.navigations,
                }, true);
            }
        };
        $scope.innerAction = function () {
            if ($scope.editElement.editType === 'Add') {
                if ($scope.tabNumber === 0)
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
    }]);
