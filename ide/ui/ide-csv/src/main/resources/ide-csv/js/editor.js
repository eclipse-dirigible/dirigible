/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
agGrid.initialiseAgGridWithAngular1(angular);
let csvView = angular.module('csv-editor', ["agGrid"]);

csvView.controller('CsvViewController', ['$scope', '$window', function ($scope, $window) {
    let messageHub = new FramesMessageHub();
    let contents;
    let csrfToken;
    let manual = false;
    let isMac = false;
    let isFileChanged = false;
    $scope.menuStyle = { 'display': 'none' };
    $scope.menuContext = { // Used for context menu content visibility
        viewport: false,
        row: false,
        column: false
    };
    let focusedCellIndex = -1;
    let focusedColumnIndex = -1;
    let headerEditMode = false;
    let csvData = {
        columns: [],
        data: []
    };
    let ctrlDown = false;
    $scope.delimiter = ',';
    $scope.dataLoaded = false;
    const ctrlKey = 17;
    $scope.papaConfig = {
        columnIndex: 0, // Custom property, needed for duplicated column names
        delimitersToGuess: [',', '\t', '|', ';'],
        header: true,
        skipEmptyLines: true,
        dynamicTyping: true,
        transformHeader: function (headerName) {
            this.columnIndex++;
            return `${headerName}_${this.columnIndex}`;
        },
        complete: function () {
            this.columnIndex = 0;
        }
    };

    function checkPlatform() {
        let platform = window.navigator.platform;
        let macosPlatforms = ['Macintosh', 'MacIntel', 'MacPPC', 'Mac68K', 'darwin', 'Mac', 'mac', 'macOS'];
        if (macosPlatforms.indexOf(platform) !== -1) isMac = true;
    }

    function sizeToFit() {
        manual = false;
        $scope.gridOptions.api.sizeColumnsToFit();
    }

    function getResource(resourcePath) {
        let xhr = new XMLHttpRequest();
        xhr.open('GET', resourcePath, false);
        xhr.setRequestHeader('X-CSRF-Token', 'Fetch');
        xhr.send();
        if (xhr.status === 200) {
            csrfToken = xhr.getResponseHeader("x-csrf-token");
            return xhr.responseText;
        }
    }

    function loadContents(file) {
        if (file) {
            return getResource('../../../../../../services/v4/ide/workspaces' + file);
        }
        console.error('file parameter is not present in the URL');
    }

    function load(reload = false) {
        if (!reload) {
            let searchParams = new URLSearchParams(window.location.search);
            $scope.file = searchParams.get('file');
            contents = loadContents($scope.file);
        }
        let parsedData = Papa.parse(contents, $scope.papaConfig);
        if ($scope.papaConfig.header) {
            if (parsedData.meta.fields.length == 0) {
                parsedData = Papa.parse('"Column"', $scope.papaConfig);
            }
            csvData.data = parsedData.data;
            csvData.columns = parsedData.meta.fields;
        }
        else {
            if (parsedData.data.length == 0) {
                parsedData = Papa.parse('"Column"', $scope.papaConfig);
            }
            csvData.data = generateCorrectCsvData(parsedData.data);
            let columns = [];
            for (const property in csvData.data[0]) {
                columns.push(property)
            }
            csvData.columns = columns;
        }
        $scope.delimiter = parsedData.meta.delimiter;
        loadGrid(reload);
    }

    function fileChanged() {
        isFileChanged = true;
        messageHub.post({ data: $scope.file }, 'editor.file.dirty');
    }

    function loadGrid(reload = false) {
        let columnDefs = csvData.columns.map(
            (name, index) => (
                {
                    headerName: name.split(/\_(?=[^\_]+$)/)[0], // Get the name without the index
                    field: name,
                    cid: index, // Custom property
                    headerComponentParams: {
                        template:
                            `<div cid="${index}" class="ag-cell-label-container" role="presentation">` +
                            '  <span ref="eMenu" class="ag-header-icon ag-header-cell-menu-button"></span>' +
                            `  <div cid="${index}" ref="eLabel" class="ag-header-cell-label" role="presentation">` +
                            `    <input id="iid_${index}" class="header-input" type="text">` +
                            `    <span cid="${index}" id="tid_${index}" ref="eText" class="ag-header-cell-text" role="columnheader"></span>` +
                            '    <span ref="eSortOrder" class="ag-header-icon ag-sort-order" ></span>' +
                            '    <span ref="eSortAsc" class="ag-header-icon ag-sort-ascending-icon" ></span>' +
                            '    <span ref="eSortDesc" class="ag-header-icon ag-sort-descending-icon" ></span>' +
                            '    <span ref="eSortNone" class="ag-header-icon ag-sort-none-icon" ></span>' +
                            '    <span ref="eFilter" class="ag-header-icon ag-filter-icon"></span>' +
                            '  </div>' +
                            '</div>'
                    }
                }
            )
        );
        columnDefs[0].rowDrag = true; // Adding drag handle to first column only
        columnDefs[0].headerCheckboxSelection = true; // Adding checkbox to first column only
        if (reload) {
            $scope.gridOptions.api.setHeaderHeight(
                (($scope.papaConfig.header) ? undefined : 0)
            );
            $scope.gridOptions.api.setColumnDefs(columnDefs);
            $scope.gridOptions.api.setRowData(csvData.data);
        } else {
            $scope.gridOptions = {
                defaultColDef: {
                    sortable: true,
                    filter: true,
                    resizable: true,
                    editable: true,
                    flex: 1
                },
                undoRedoCellEditing: true,
                undoRedoCellEditingLimit: 10,
                headerHeight: (($scope.papaConfig.header) ? undefined : 0),
                columnDefs: columnDefs,
                rowData: csvData.data,
                rowDragManaged: true,
                suppressMoveWhenRowDragging: true,
                enableMultiRowDragging: true,
                animateRows: false,
                rowSelection: 'multiple',
                suppressExcelExport: true,
                suppressPropertyNamesCheck: true, // Because of custom properties
                onColumnResized: function (params) {
                    if (params.finished && manual) {
                        manual = false;
                    }
                },
                onGridReady: function (/*$event*/) {
                    sizeToFit();
                    $scope.dataLoaded = true;
                },
                onCellValueChanged: function (/*$event*/) {
                    fileChanged();
                },
                onColumnMoved: function (/*$event*/) {
                    fileChanged();
                },
                onRowDragEnd: function (/*$event*/) {
                    fileChanged();
                },
                onSortChanged: function (/*$event*/) {
                    fileChanged();
                },
            };
        }
    }

    /*
     * When parsing a csv with PapaParse without header = true,
     * the data we get is structured differently and cannot
     * be used with AG-Grid easily.
     * This function takes the headerless data and transforms it,
     * as if it did have headers.
     */
    function generateCorrectCsvData(rawData) {
        let data = [];
        for (let i = 0; i < rawData.length; i++) {
            let obj = {};
            for (let j = 0; j < rawData[i].length; j++) {
                obj[`c_${j}`] = rawData[i][j];
            }
            data.push(obj);
        }
        return data;
    }

    function saveContents(text) {
        console.log('Save called...');
        if ($scope.file) {
            let xhr = new XMLHttpRequest();
            xhr.open('PUT', '../../../../../../services/v4/ide/workspaces' + $scope.file);
            xhr.setRequestHeader('X-Requested-With', 'Fetch');
            xhr.setRequestHeader('X-CSRF-Token', csrfToken);
            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4) {
                    console.log('file saved: ' + $scope.file);
                }
            };
            xhr.send(text);
            contents = text;
            isFileChanged = false;
            messageHub.post({ data: $scope.file }, 'editor.file.saved');
            messageHub.post({ data: 'File [' + $scope.file + '] saved.' }, 'status.message');
        } else {
            console.error('file parameter is not present in the request');
        }
    }

    function hideContextMenu() {
        if ($scope.menuStyle.display !== "none") {
            $scope.menuContext.viewport = false;
            $scope.menuContext.row = false;
            $scope.menuContext.column = false;
            $scope.menuStyle = {
                display: "none"
            };
        }
    }

    /**
     * Simulating named parameters using a params object
     * It can contain the following parameters:
     * {
     *  x: 10, // Required, X position
     *  y: 10, // Required, Y position
     *  viewport: true, // Optional, when the menu is for the empty viewport
     *  row: true, // Optional, when the menu is for a row
     *  column: true // Optional, when the menu is for a column header
     * }
     * The last 3 are optional but one must be specified or the menu will not be shown.
     */
    function showContextMenu(params) {
        if (
            "viewport" in params &&
            !("row" in params) &&
            !("column" in params)
        ) {
            $scope.menuContext.viewport = true;
            $scope.menuContext.row = false;
            $scope.menuContext.column = false;
        } else if (
            !("viewport" in params) &&
            "row" in params &&
            !("column" in params)
        ) {
            $scope.menuContext.viewport = false;
            $scope.menuContext.row = true;
            $scope.menuContext.column = false;
        } else if (
            !("viewport" in params) &&
            !("row" in params) &&
            "column" in params
        ) {
            $scope.menuContext.viewport = false;
            $scope.menuContext.row = false;
            $scope.menuContext.column = true;
        } else {
            return
        }
        if ("x" in params && "y" in params) {
            $scope.menuStyle = {
                position: "fixed",
                display: "block",
                left: params.x + 'px',
                top: params.y + 'px'
            };
        } else {
            hideContextMenu();
        }
    };

    function showColumnInput() {
        let columnInput = $(`#iid_${focusedColumnIndex}`);
        let columnText = $(`#tid_${focusedColumnIndex}`);
        columnInput.val(columnText.text());
        columnInput.css({
            'display': 'inline-block'
        });
        columnText.css({
            'display': 'none'
        });
        columnInput.on('keypress', function (e) {
            if (e.which == 13) {
                hideColumnInput();
            }
        });
        // Unless we do this, we will not be able to use the arrow keys in the input box.
        $scope.gridOptions.navigateToNextHeader = function () { };
    }

    function hideColumnInput() {
        if (headerEditMode) {
            let columnInput = $(`#iid_${focusedColumnIndex}`);
            let newTitle = columnInput.val();
            let columnText = $(`#tid_${focusedColumnIndex}`);
            columnInput.css({
                'display': 'none'
            });
            columnText.css({
                'display': 'inline-block'
            });
            columnInput.off();
            let columnDefs = $scope.gridOptions.api.getColumnDefs();
            for (let i = 0; i < columnDefs.length; i++) {
                if (columnDefs[i].cid == focusedColumnIndex) {
                    columnDefs[i].sortable = true;
                    columnDefs[i].filter = true;
                    if (newTitle != columnText.text()) {
                        columnDefs[i].headerName = newTitle;
                    }
                    break;
                }
            }
            $scope.gridOptions.api.setColumnDefs(columnDefs);
            // Unless we do this, we will not be able to use the arrow keys to navigate the grid.
            $scope.gridOptions.navigateToNextHeader = undefined;
            if (newTitle != columnText.text()) {
                fileChanged();
            }
            headerEditMode = false;
        }
    };

    $scope.handleClick = function (event) {
        if (event.which === 3) {
            if (
                event.originalTarget.className.includes("ag-header-cell-label") ||
                event.originalTarget.className.includes("ag-header-cell-text") ||
                event.originalTarget.className.includes("ag-cell-label-container")
            ) {
                focusedColumnIndex = parseInt(event.originalTarget.attributes.cid.value);
                showContextMenu({ x: event.clientX, y: event.clientY, column: true });
            } else if (event.originalTarget.className.includes("ag-cell")) {
                focusedCellIndex = $scope.gridOptions.api.getFocusedCell().rowIndex;
                showContextMenu({ x: event.clientX, y: event.clientY, row: true });
            } else if (event.originalTarget.className.includes("ag-center-cols-viewport")) {
                showContextMenu({ x: event.clientX, y: event.clientY, viewport: true });
            } else if (
                !event.originalTarget.className.includes("dropdown-item") &&
                !event.originalTarget.className.includes("header-input")
            ) {
                hideContextMenu();
                hideColumnInput();
            }
        } else {
            try {
                if (
                    !event.originalTarget.className.includes("dropdown-item") &&
                    !event.originalTarget.className.includes("header-input")
                ) {
                    hideContextMenu();
                    hideColumnInput();
                }
            } catch (error) {
                if (error.toString() != 'Error: Permission denied to access property "className"') { // Firefox bug
                    console.log(error);
                }
            }
        }
    }

    $scope.keyDownFunc = function ($event) {
        if (
            ctrlDown &&
            String.fromCharCode($event.which).toLowerCase() == 's'
        ) {
            $event.preventDefault();
            if (isFileChanged)
                $scope.save();
        }
    };

    angular.element($window).bind("keyup", function (/*$event*/) {
        ctrlDown = false;
    });

    angular.element($window).bind("keydown", function ($event) {
        if (isMac && "metaKey" in $event && $event.metaKey)
            ctrlDown = true;
        else if ($event.keyCode == ctrlKey)
            ctrlDown = true;
    });

    $scope.downloadCsv = function () {
        $scope.searchInput = "";
        $scope.gridOptions.api.setQuickFilter(undefined);
        $scope.gridOptions.api.setFilterModel(undefined);
        $scope.gridOptions.api.exportDataAsCsv({
            skipColumnHeaders: (($scope.papaConfig.header) ? false : true),
            columnSeparator: $scope.delimiter
        });
    };

    $scope.save = function () {
        $scope.searchInput = "";
        $scope.gridOptions.api.setQuickFilter(undefined);
        $scope.gridOptions.api.setFilterModel(undefined);
        contents = $scope.gridOptions.api.getDataAsCsv({
            skipColumnHeaders: (($scope.papaConfig.header) ? false : true),
            columnSeparator: $scope.delimiter
        });
        saveContents(contents);
    };

    $scope.searchCsv = function () {
        $scope.gridOptions.api.setQuickFilter($scope.searchInput);
    };

    $scope.hasHeader = function (enabled) {
        $scope.papaConfig.header = enabled;
        load(true);
    };

    $scope.addRowAbove = function () {
        hideContextMenu();
        let row = {};
        let columns = $scope.gridOptions.columnApi.getAllColumns();
        for (let i = 0; i < columns.length; i++) {
            row[columns[i].userProvidedColDef.field] = "";
        }
        csvData.data.splice(focusedCellIndex, 0, row);
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.addRowBelow = function () {
        hideContextMenu();
        let row = {};
        let columns = $scope.gridOptions.columnApi.getAllColumns();
        for (let i = 0; i < columns.length; i++) {
            row[columns[i].userProvidedColDef.field] = "";
        }
        csvData.data.splice(focusedCellIndex + 1, 0, row);
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.addRow = function () {
        hideContextMenu();
        let row = {};
        let columns = $scope.gridOptions.columnApi.getAllColumns();
        for (let i = 0; i < columns.length; i++) {
            row[columns[i].userProvidedColDef.field] = "";
        }
        csvData.data.push(row);
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.deleteRow = function () {
        hideContextMenu();
        let rows = $scope.gridOptions.api.getSelectedNodes();
        let indexes = [];
        for (let i = 0; i < rows.length; i++) {
            indexes.push(rows[i].rowIndex);
        }
        if (!indexes.includes(focusedCellIndex)) {
            indexes.push(focusedCellIndex);
        }
        indexes.sort(function (a, b) { return a - b; });
        for (let i = indexes.length - 1; i >= 0; i--) {
            csvData.data.splice(indexes[i], 1);
        }
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.addColumn = function () {
        hideContextMenu();
        let columnDefs = $scope.gridOptions.columnDefs;
        let column = {
            headerName: 'New column',
            field: `New column_${columnDefs.length + 1}`,
            headerComponentParams: {
                template:
                    `<div cid="${columnDefs.length}" class="ag-cell-label-container" role="presentation">` +
                    '  <span ref="eMenu" class="ag-header-icon ag-header-cell-menu-button"></span>' +
                    `  <div cid="${columnDefs.length}" ref="eLabel" class="ag-header-cell-label" role="presentation">` +
                    `    <input id="iid_${columnDefs.length}" class="header-input" type="text">` +
                    `    <span cid="${columnDefs.length}" id="tid_${columnDefs.length}" ref="eText" class="ag-header-cell-text" role="columnheader"></span>` +
                    '    <span ref="eSortOrder" class="ag-header-icon ag-sort-order" ></span>' +
                    '    <span ref="eSortAsc" class="ag-header-icon ag-sort-ascending-icon" ></span>' +
                    '    <span ref="eSortDesc" class="ag-header-icon ag-sort-descending-icon" ></span>' +
                    '    <span ref="eSortNone" class="ag-header-icon ag-sort-none-icon" ></span>' +
                    '    <span ref="eFilter" class="ag-header-icon ag-filter-icon"></span>' +
                    '  </div>' +
                    '</div>'
            }
        };
        columnDefs.push(column);
        $scope.gridOptions.api.setColumnDefs(columnDefs);
        fileChanged();
    };

    $scope.editColumn = function () {
        hideContextMenu();
        headerEditMode = true;
        let columnDefs = $scope.gridOptions.api.getColumnDefs();
        for (let i = 0; i < columnDefs.length; i++) {
            if (columnDefs[i].cid == focusedColumnIndex) {
                columnDefs[i].sortable = false;
                columnDefs[i].filter = false;
                break;
            }
        }
        $scope.gridOptions.api.setColumnDefs(columnDefs);
        showColumnInput();
    };

    $scope.deleteColumn = function () {
        hideContextMenu();
        let columnDefs = $scope.gridOptions.columnDefs;
        for (let i = 0; i < csvData.data.length; i++) {
            delete csvData.data[i][columnDefs[focusedColumnIndex].field];
        }
        columnDefs.splice(focusedColumnIndex, 1);
        $scope.gridOptions.api.setRowData(csvData.data);
        $scope.gridOptions.api.setColumnDefs(columnDefs);
        fileChanged();
    };

    checkPlatform();
    load();

}]);