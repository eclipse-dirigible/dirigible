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
        $scope.dialogType = 'entity'; // property
        $scope.tabNumber = 0;
        $scope.entityTypesModel = "";
        $scope.entityTypes = [
            { value: "PRIMARY", label: "Primary Entity" },
            { value: "DEPENDENT", label: "Dependent Entity" },
            { value: "REPORT", label: "Report Entity" },
            { value: "FILTER", label: "Filter Entity" },
            { value: "SETTING", label: "Setting Entity" },
            { value: "PROJECTION", label: "Projection Entity" },
            { value: "EXTENSION", label: "Extension Entity" }
        ];
        $scope.layoutTypes = [
            { value: "MANAGE", label: "Manage Entities" },
            { value: "MANAGE_MASTER", label: "Manage Master Entities" },
            { value: "MANAGE_DETAILS", label: "Manage Details Entities" },
            { value: "LIST", label: "List Entities" },
            { value: "LIST_MASTER", label: "List Master Entities" },
            { value: "LIST_DETAILS", label: "List Details Entities" },
            { value: "REPORT_TABLE", label: "Report in a Table Format" },
            { value: "REPORT_BAR", label: "Report in a Bar Chart Format" },
            { value: "REPORT_LINE", label: "Report in a Line Chart Format" },
            { value: "REPORT_DOUGHNUT", label: "Report in a Doughnut Chart Format" },
            { value: "REPORT_PIE", label: "Report in a Pie Chart Format" },
            { value: "REPORT_POLARAREA", label: "Report in a Polar Area Format" },
            { value: "REPORT_RADAR", label: "Report in a Radar Format" },
        ];
        $scope.dataTypes = [
            { value: "VARCHAR", label: "VARCHAR" },
            { value: "CHAR", label: "CHAR" },
            { value: "DATE", label: "DATE" },
            { value: "TIME", label: "TIME" },
            { value: "TIMESTAMP", label: "TIMESTAMP" },
            { value: "INTEGER", label: "INTEGER" },
            { value: "TINYINT", label: "TINYINT" },
            { value: "BIGINT", label: "BIGINT" },
            { value: "SMALLINT", label: "SMALLINT" },
            { value: "REAL", label: "REAL" },
            { value: "DOUBLE", label: "DOUBLE" },
            { value: "BOOLEAN", label: "BOOLEAN" },
            { value: "BLOB", label: "BLOB" },
            { value: "DECIMAL", label: "DECIMAL" },
            { value: "BIT", label: "BIT" }
        ];
        $scope.dataOrderByOptions = [
            { value: "ASC", label: "ASCENDING" },
            { value: "DESC", label: "DESCENDING" }
        ];
        $scope.widgetTypes = [
            { value: "TEXTBOX", label: "Text Box" },
            { value: "TEXTAREA", label: "Text Area" },
            { value: "DATE", label: "Date Picker" },
            { value: "DROPDOWN", label: "Dropdown" },
            { value: "CHECKBOX", label: "Check Box" },
            { value: "LOOKUPDIALOG", label: "Lookup Dialog" },
            { value: "NUMBER", label: "Number" },
            { value: "COLOR", label: "Color" },
            { value: "DATETIME-LOCAL", label: "Datetime Local" },
            { value: "EMAIL", label: "e-mail" },
            { value: "MONTH", label: "Month" },
            { value: "RANGE", label: "Range" },
            { value: "SEARCH", label: "Search" },
            { value: "TEL", label: "Telephone" },
            { value: "TIME", label: "Time" },
            { value: "URL", label: "URL" },
            { value: "WEEK", label: "Week" }
        ];
        $scope.widgetSizes = [
            { value: "fd-col-md--2 fd-col--3", label: "Small" },
            { value: "fd-col-md--4 fd-col--6", label: "Medium" },
            { value: "fd-col-md--6 fd-col--9", label: "Large" },
            { value: "fd-col-md--8 fd-col--12", label: "XLarge" }
        ];
        $scope.majorTypes = [
            { value: "true", label: "Show in table header" },
            { value: "false", label: "Show in form only" }
        ];
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
        $scope.save = function () {
            if (!$scope.state.error) {
                $scope.state.busyText = "Saving";
                $scope.state.isBusy = true;
                if ($scope.dialogType === 'entity') {
                    messageHub.postMessage('edm.editor.entity', {
                        cellId: $scope.dataParameters.cellId,
                        name: $scope.dataParameters.name,
                        entityType: $scope.dataParameters.entityType,
                        dataName: $scope.dataParameters.dataName,
                        dataCount: $scope.dataParameters.dataCount,
                        dataQuery: $scope.dataParameters.dataQuery,
                        title: $scope.dataParameters.title,
                        caption: $scope.dataParameters.caption,
                        description: $scope.dataParameters.description,
                        tooltip: $scope.dataParameters.tooltip,
                        icon: $scope.dataParameters.icon,
                        menuKey: $scope.dataParameters.menuKey,
                        menuLabel: $scope.dataParameters.menuLabel,
                        menuIndex: $scope.dataParameters.menuIndex,
                        layoutType: $scope.dataParameters.layoutType,
                        perspectiveName: $scope.dataParameters.perspectiveName,
                        perspectiveLabel: $scope.dataParameters.perspectiveLabel,
                        navigationPath: $scope.dataParameters.navigationPath,
                        feedUrl: $scope.dataParameters.feedUrl,
                        feedUsername: $scope.dataParameters.feedUsername,
                        feedPassword: $scope.dataParameters.feedPassword,
                        feedSchedule: $scope.dataParameters.feedSchedule,
                        feedPath: $scope.dataParameters.feedPath,
                        roleRead: $scope.dataParameters.roleRead,
                        roleWrite: $scope.dataParameters.roleWrite,
                        importsCode: $scope.dataParameters.importsCode,
                        generateReport: $scope.dataParameters.generateReport,
                    }, true);
                } else {
                    messageHub.postMessage('edm.editor.property', {
                        cellId: $scope.dataParameters.cellId,
                        name: $scope.dataParameters.name,
                        description: $scope.dataParameters.description,
                        tooltip: $scope.dataParameters.tooltip,
                        isRequiredProperty: $scope.dataParameters.isRequiredProperty,
                        isCalculatedProperty: $scope.dataParameters.isCalculatedProperty,
                        calculatedPropertyExpressionCreate: $scope.dataParameters.calculatedPropertyExpressionCreate,
                        calculatedPropertyExpressionUpdate: $scope.dataParameters.calculatedPropertyExpressionUpdate,
                        dataName: $scope.dataParameters.dataName,
                        dataType: $scope.dataParameters.dataType,
                        dataOrderBy: $scope.dataParameters.dataOrderBy,
                        dataLength: $scope.dataParameters.dataLength,
                        dataPrimaryKey: $scope.dataParameters.dataPrimaryKey,
                        dataAutoIncrement: $scope.dataParameters.dataAutoIncrement,
                        dataNotNull: $scope.dataParameters.dataNotNull,
                        dataUnique: $scope.dataParameters.dataUnique,
                        dataPrecision: $scope.dataParameters.dataPrecision,
                        dataScale: $scope.dataParameters.dataScale,
                        dataDefaultValue: $scope.dataParameters.dataDefaultValue,
                        widgetType: $scope.dataParameters.widgetType,
                        widgetSize: $scope.dataParameters.widgetSize,
                        widgetLength: $scope.dataParameters.widgetLength,
                        widgetLabel: $scope.dataParameters.widgetLabel,
                        widgetShortLabel: $scope.dataParameters.widgetShortLabel,
                        widgetPattern: $scope.dataParameters.widgetPattern,
                        widgetFormat: $scope.dataParameters.widgetFormat,
                        widgetService: $scope.dataParameters.widgetService,
                        widgetSection: $scope.dataParameters.widgetSection,
                        widgetIsMajor: $scope.dataParameters.widgetIsMajor,
                        widgetDropDownKey: $scope.dataParameters.widgetDropDownKey,
                        widgetDropDownValue: $scope.dataParameters.widgetDropDownValue,
                        widgetDependsOnProperty: $scope.dataParameters.widgetDependsOnProperty,
                        widgetDependsOnEntity: $scope.dataParameters.widgetDependsOnEntity,
                        widgetDependsOnValueFrom: $scope.dataParameters.widgetDependsOnValueFrom,
                        widgetDependsOnFilterBy: $scope.dataParameters.widgetDependsOnFilterBy,
                        feedPropertyName: $scope.dataParameters.feedPropertyName,
                        roleRead: $scope.dataParameters.roleRead,
                        roleWrite: $scope.dataParameters.roleWrite,
                    }, true);
                }
            }
        };
        $scope.cancel = function () {
            messageHub.closeDialogWindow("edmDetails");
        };
        $scope.dataParameters = ViewParameters.get();
        if ($scope.dataParameters.dialogType === "entity") {
            $scope.dialogType = 'entity';
            $scope.loadIcons();
        } else {
            $scope.dialogType = 'property';
            $scope.state.isBusy = false;
        }
    }]);
