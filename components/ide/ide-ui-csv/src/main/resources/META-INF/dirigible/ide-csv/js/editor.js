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
agGrid.initialiseAgGridWithAngular1(angular);
const csvView = angular.module('csv-editor', ['ideUI', 'ideView', 'ideWorkspace', 'agGrid']);
csvView.controller('CsvViewController', function ($scope, $window, messageHub, workspaceApi, ViewParameters) {
    let contents;
    let manual = false;
    let isFileChanged = false;
    $scope.menuStyle = { 'display': 'none' };
    $scope.menuContext = { // Used for context menu content visibility
        viewport: false,
        row: false,
        column: false
    };
    $scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };
    let focusedCellIndex = -1;
    let focusedColumnIndex = -1;
    let headerEditMode = false;
    let csvData = {
        columns: [],
        data: []
    };
    $scope.delimiter = ',';
    $scope.gridLoaded = false;
    $scope.search = { text: '' };
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
        columnDefs: undefined,
        rowData: undefined,
        rowDragManaged: true,
        suppressMoveWhenRowDragging: true,
        rowDragMultiRow: true,
        animateRows: false,
        rowSelection: 'multiple',
        suppressExcelExport: true,
        suppressPropertyNamesCheck: true, // Because of custom properties
        onColumnResized: function (params) {
            if (params.finished && manual) manual = false;
        },
        onGridReady: function (/*$event*/) {
            if (!$scope.gridLoaded) { // Execute this only once on first grid load
                $scope.gridLoaded = true;
                loadFileContents();
            }
            sizeToFit();
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
        }
    };
    $scope.papaConfig = {
        columnIndex: 0, // Custom property, needed for duplicated column names
        delimitersToGuess: [',', '\t', '|', ';', '#', '~', Papa.RECORD_SEP, Papa.UNIT_SEP],
        header: true,
        skipEmptyLines: true,
        dynamicTyping: true,
        transformHeader: function (headerName) {
            return `${headerName}_${this.columnIndex++}`;
        },
        complete: function () {
            this.columnIndex = 0;
        }
    };
    $scope.rowsCount = 0;

    function setRowsCount(rowsCount) {
        $scope.rowsCount = rowsCount;
    }

    angular.element($window).bind('focus', function () {
        messageHub.setFocusedEditor($scope.dataParameters.file);
        messageHub.setStatusCaret('');
    });

    function sizeToFit() {
        manual = false;
        $scope.gridOptions.api.sizeColumnsToFit();
    }

    function parseContent() {
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
        if ($scope.papaConfig.delimiter === undefined) {
            $scope.delimiter = parsedData.meta.delimiter;
        }
        setRowsCount(csvData.data.length);
    }

    function loadFileContents() {
        if ($scope.dataParameters.file) {
            workspaceApi.loadContent('', $scope.dataParameters.file).then(function (response) {
                if (response.status === 200) {
                    contents = response.data;
                    parseContent();
                    loadGrid();
                    $scope.$apply(function () {
                        $scope.state.isBusy = false;
                    });
                } else if (response.status === 404) {
                    messageHub.closeEditor($scope.dataParameters.file);
                } else {
                    $scope.$apply(function () {
                        $scope.state.error = true;
                        $scope.errorMessage = 'There was a problem with loading the file';
                        $scope.state.isBusy = false;
                    });
                }
            });
        }
    }

    function fileChanged() {
        isFileChanged = true;
        messageHub.setEditorDirty($scope.dataParameters.file, isFileChanged);
        setRowsCount(csvData.data.length);
    }

    function loadGrid() {
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
                            `    <input id="iid_${index}" class="header-input fd-input" type="text">` +
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
        $scope.gridOptions.api.setHeaderHeight(
            (($scope.papaConfig.header) ? undefined : 0)
        );
        $scope.gridOptions.api.setColumnDefs(columnDefs);
        $scope.gridOptions.api.setRowData(csvData.data);
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
        workspaceApi.saveContent('', $scope.dataParameters.file, text).then(function (response) {
            if (response.status === 200) {
                messageHub.announceFileSaved({
                    name: $scope.dataParameters.file.substring($scope.dataParameters.file.lastIndexOf('/') + 1),
                    path: $scope.dataParameters.file.substring($scope.dataParameters.file.indexOf('/', 1)),
                    contentType: $scope.dataParameters.contentType,
                    workspace: $scope.dataParameters.file.substring(1, $scope.dataParameters.file.indexOf('/', 1)),
                });
                messageHub.setStatusMessage(`File '${$scope.dataParameters.file}' saved`);
                messageHub.setEditorDirty($scope.dataParameters.file, false);
                $scope.$apply(function () {
                    $scope.state.isBusy = false;
                    $scope.isFileChanged = false;
                });
            } else {
                messageHub.setStatusError(`Error saving '${$scope.dataParameters.file}'`);
                messageHub.showAlertError('Error while saving the file', 'Please look at the console for more information');
                $scope.$apply(function () {
                    $scope.state.isBusy = false;
                });
            }
        });
    }

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
        let defs = $scope.gridOptions.api.getColumnDefs();
        defs[focusedColumnIndex].suppressMovable = true;
        $scope.gridOptions.api.setColumnDefs(defs);
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
                        fileChanged();
                    }
                    break;
                }
            }
            $scope.gridOptions.api.setColumnDefs(columnDefs);
            // Unless we do this, we will not be able to use the arrow keys to navigate the grid.
            $scope.gridOptions.navigateToNextHeader = undefined;
            let defs = $scope.gridOptions.api.getColumnDefs();
            defs[focusedColumnIndex].suppressMovable = false;
            $scope.gridOptions.api.setColumnDefs(defs);
            headerEditMode = false;
        }
    };

    $scope.handleClick = function (event) {
        if (headerEditMode && event.which !== 3) {
            try {
                if (!event.target.className.includes('header-input')) hideColumnInput();
            } catch (error) {
                if (error.toString() != 'Error: Permission denied to access property "className"') { // Firefox bug
                    console.log(error);
                }
            }
        }
    };

    $scope.contextMenuContent = function (element) {
        let items;
        if (
            element.className.includes('ag-header-cell-label') ||
            element.className.includes('ag-header-cell-text') ||
            element.className.includes('ag-cell-label-container')
        ) {
            focusedColumnIndex = parseInt(element.attributes.cid.value);
            items = [{
                id: 'addColumn',
                label: 'Add Column',
                icon: 'sap-icon--add'
            }, {
                id: 'editColumn',
                label: 'Edit Column',
                icon: 'sap-icon--edit'
            }, {
                id: 'deleteColumn',
                label: 'Delete Column',
                divider: true,
                icon: 'sap-icon--delete'
            }];
        } else if (element.className.includes('ag-cell')) {
            focusedCellIndex = $scope.gridOptions.api.getFocusedCell().rowIndex;
            items = [{
                id: 'addRowAbove',
                label: 'Add Row Above',
            }, {
                id: 'addRowBelow',
                label: 'Add Row Below',
            }, {
                id: 'deleteRows',
                label: 'Delete Row(s)',
                divider: true,
                icon: 'sap-icon--delete'
            }];
        } else if (element.className.includes('ag-center-cols-viewport') || element.className.includes('ag-row')) {
            items = [{
                id: 'addRow',
                label: 'Add Row',
                divider: true,
                icon: 'sap-icon--add'
            }];
        } else return;
        return {
            callbackTopic: 'csvEditor.contextmenu',
            hasIcons: true,
            items: items,
        }
    };

    $scope.downloadCsv = function () {
        $scope.search.text = '';
        $scope.gridOptions.api.setQuickFilter(undefined);
        $scope.gridOptions.api.setFilterModel(undefined);
        $scope.gridOptions.api.exportDataAsCsv({
            skipColumnHeaders: (($scope.papaConfig.header) ? false : true),
            columnSeparator: $scope.delimiter
        });
    };

    $scope.save = function (_keySet, event) {
        if (event) event.preventDefault();
        $scope.state.busyText = 'Saving...';
        $scope.state.isBusy = true;
        $scope.search.text = '';
        $scope.gridOptions.api.setQuickFilter(undefined);
        $scope.gridOptions.api.setFilterModel(undefined);
        contents = $scope.gridOptions.api.getDataAsCsv({
            skipColumnHeaders: (($scope.papaConfig.header) ? false : true),
            columnSeparator: $scope.delimiter
        });
        saveContents(contents);
    };

    $scope.searchCsv = function () {
        $scope.gridOptions.api.setQuickFilter($scope.search.text);
    };

    $scope.hasHeader = function () {
        parseContent();
        loadGrid();
    };

    $scope.addRowAbove = function () {
        let row = {};
        let columns = $scope.gridOptions.columnApi.getAllColumns();
        for (let i = 0; i < columns.length; i++) {
            row[columns[i].userProvidedColDef.field] = '';
        }
        csvData.data.splice(focusedCellIndex, 0, row);
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.addRowBelow = function () {
        let row = {};
        let columns = $scope.gridOptions.columnApi.getAllColumns();
        for (let i = 0; i < columns.length; i++) {
            row[columns[i].userProvidedColDef.field] = '';
        }
        csvData.data.splice(focusedCellIndex + 1, 0, row);
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.addRow = function () {
        let row = {};
        let columns = $scope.gridOptions.columnApi.getAllColumns();
        for (let i = 0; i < columns.length; i++) {
            row[columns[i].userProvidedColDef.field] = '';
        }
        csvData.data.push(row);
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.deleteRow = function () {
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
        let columnDefs = $scope.gridOptions.api.getColumnDefs();
        let column = {
            headerName: 'New column',
            field: `New column_${columnDefs.length}`,
            cid: columnDefs.length, // Custom property
            headerComponentParams: {
                template:
                    `<div cid="${columnDefs.length}" class="ag-cell-label-container" role="presentation">` +
                    '  <span ref="eMenu" class="ag-header-icon ag-header-cell-menu-button"></span>' +
                    `  <div cid="${columnDefs.length}" ref="eLabel" class="ag-header-cell-label" role="presentation">` +
                    `    <input id="iid_${columnDefs.length}" class="header-input fd-input" type="text">` +
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
        let columnDefs = $scope.gridOptions.api.getColumnDefs();
        let field = '';
        for (let i = 0; i < columnDefs.length; i++) {
            if (columnDefs[i].cid == focusedColumnIndex) {
                field = columnDefs[i].field;
                columnDefs.splice(i, 1);
                break;
            }
        }
        for (let i = 0; i < csvData.data.length; i++) {
            delete csvData.data[i][field];
        }
        $scope.gridOptions.api.setRowData(csvData.data);
        $scope.gridOptions.api.setColumnDefs(columnDefs);
        fileChanged();
    };

    messageHub.onEditorFocusGain(function (msg) {
        if (msg.resourcePath === $scope.dataParameters.file) messageHub.setStatusCaret('');
    });

    messageHub.onEditorReloadParameters(
        function (event) {
            $scope.$apply(() => {
                if (event.resourcePath === $scope.dataParameters.file) {
                    $scope.dataParameters = ViewParameters.get();
                }
            });
        }
    );

    messageHub.onDidReceiveMessage(
        'editor.file.save.all',
        function () {
            if (!$scope.state.error && isFileChanged) {
                let extension = JSON.stringify($scope.extension, null, 4);
                if (contents !== extension) {
                    $scope.save();
                }
            }
        },
        true,
    );

    messageHub.onDidReceiveMessage(
        'editor.file.save',
        function (msg) {
            if (!$scope.state.error && isFileChanged) {
                let file = msg.data && typeof msg.data === 'object' && msg.data.file;
                if (file && file === $scope.dataParameters.file) $scope.save();
            }
        },
        true,
    );

    messageHub.onDidReceiveMessage(
        'csvEditor.contextmenu',
        function (msg) {
            if (msg.data.itemId === 'addColumn') {
                $scope.addColumn();
            } else if (msg.data.itemId === 'editColumn') {
                $scope.editColumn();
            } else if (msg.data.itemId === 'deleteColumn') {
                $scope.deleteColumn();
            } else if (msg.data.itemId === 'addRowAbove') {
                $scope.addRowAbove();
            } else if (msg.data.itemId === 'addRowBelow') {
                $scope.addRowBelow();
            } else if (msg.data.itemId === 'deleteRows') {
                $scope.deleteRow();
            } else if (msg.data.itemId === 'addRow') {
                $scope.addRow();
            }
        }
    );

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('file')) {
        $scope.state.error = true;
        $scope.errorMessage = "The 'file' data parameter is missing.";
    } else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
        $scope.state.error = true;
        $scope.errorMessage = "The 'contentType' data parameter is missing.";
    } else {
        if ($scope.dataParameters.hasOwnProperty('header')) $scope.papaConfig.header = $scope.dataParameters.header;
        if ($scope.dataParameters.hasOwnProperty('quotechar')) $scope.papaConfig.quoteChar = $scope.dataParameters.quotechar;
        if ($scope.dataParameters.hasOwnProperty('delimiter')) {
            $scope.papaConfig.delimiter = $scope.dataParameters.delimiter;
            $scope.delimiter = $scope.dataParameters.delimiter;
        }
    }
});