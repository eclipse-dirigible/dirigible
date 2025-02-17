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
const csvView = angular.module('csv-editor', ['blimpKit', 'platformView', 'platformShortcuts', 'WorkspaceService', 'agGrid']);
csvView.controller('CsvController', ($scope, $window, WorkspaceService, ViewParameters) => {
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    const layoutHub = new LayoutHub();
    const contextMenuHub = new ContextMenuHub();
    let contents;
    let manual = false;
    $scope.changed = false;
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

    angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

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

    const loadFileContents = () => {
        if (!$scope.state.error) {
            $scope.state.isBusy = true;
            WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
                contents = response.data;
                parseContent();
                loadGrid();
                $scope.$evalAsync(() => {
                    $scope.state.isBusy = false;
                });
            }, (response) => {
                console.error(response);
                $scope.$evalAsync(() => {
                    $scope.state.error = true;
                    $scope.errorMessage = 'Error while loading file. Please look at the console for more information.';
                    $scope.state.isBusy = false;
                });
            });
        }
    };

    function fileChanged() {
        $scope.changed = true;
        layoutHub.setEditorDirty({
            path: $scope.dataParameters.filePath,
            dirty: $scope.changed,
        });
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
        WorkspaceService.saveContent($scope.dataParameters.filePath, text).then(() => {
            contents = text;
            layoutHub.setEditorDirty({
                path: $scope.dataParameters.filePath,
                dirty: false,
            });
            workspaceHub.announceFileSaved({
                path: $scope.dataParameters.filePath,
                contentType: $scope.dataParameters.contentType,
            });
            $scope.$evalAsync(() => {
                $scope.changed = false;
                $scope.state.isBusy = false;
            });
        }, (response) => {
            console.error(response);
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = `Error saving '${$scope.dataParameters.filePath}'. Please look at the console for more information.`;
                $scope.state.isBusy = false;
            });
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

    $scope.handleClick = (event) => {
        if (headerEditMode && event.which !== 3) {
            try {
                if (!event.target.className.includes('header-input')) hideColumnInput();
            } catch (error) {
                if (error.toString() != 'Error: Permission denied to access property "className"') { // Firefox bug
                    console.error(error);
                }
            }
        }
    };

    $scope.showContextMenu = (event) => {
        const items = [];
        if (
            event.target.className.includes('ag-header-cell-label') ||
            event.target.className.includes('ag-header-cell-text') ||
            event.target.className.includes('ag-cell-label-container')
        ) {
            event.preventDefault();
            focusedColumnIndex = parseInt(event.target.attributes.cid.value);
            items.push({
                id: 'addColumn',
                label: 'Add Column',
                leftIconClass: 'sap-icon--add'
            }, {
                id: 'editColumn',
                label: 'Edit Column',
                separator: true,
                leftIconClass: 'sap-icon--edit'
            }, {
                id: 'deleteColumn',
                label: 'Delete Column',
                leftIconClass: 'sap-icon--delete'
            });
        } else if (event.target.className.includes('ag-cell')) {
            event.preventDefault();
            focusedCellIndex = $scope.gridOptions.api.getFocusedCell().rowIndex;
            items.push({
                id: 'addRowAbove',
                label: 'Add Row Above',
            }, {
                id: 'addRowBelow',
                separator: true,
                label: 'Add Row Below',
            }, {
                id: 'deleteRows',
                label: 'Delete Row(s)',
                leftIconClass: 'sap-icon--delete'
            });
        } else if (event.target.className.includes('ag-center-cols-viewport') || event.target.className.includes('ag-row')) {
            event.preventDefault();
            items.push({
                id: 'addRow',
                label: 'Add Row',
                leftIconClass: 'sap-icon--add'
            });
        } else return;
        contextMenuHub.showContextMenu({
            ariaLabel: 'csv editor contextmenu',
            posX: event.clientX,
            posY: event.clientY,
            icons: true,
            items: items
        }).then((id) => {
            $scope.$evalAsync(() => {
                if (id === 'addColumn') {
                    $scope.addColumn();
                } else if (id === 'editColumn') {
                    $scope.editColumn();
                } else if (id === 'deleteColumn') {
                    $scope.deleteColumn();
                } else if (id === 'addRowAbove') {
                    $scope.addRowAbove();
                } else if (id === 'addRowBelow') {
                    $scope.addRowBelow();
                } else if (id === 'deleteRows') {
                    $scope.deleteRow();
                } else if (id === 'addRow') {
                    $scope.addRow();
                }
            });
        }, (error) => {
            console.error(error);
            statusBarHub.showError('Unable to process context menu data');
        })
    };

    $scope.downloadCsv = () => {
        $scope.search.text = '';
        $scope.gridOptions.api.setQuickFilter(undefined);
        $scope.gridOptions.api.setFilterModel(undefined);
        $scope.gridOptions.api.exportDataAsCsv({
            skipColumnHeaders: (($scope.papaConfig.header) ? false : true),
            columnSeparator: $scope.delimiter
        });
    };

    $scope.save = (keySet = 'ctrl+s', event) => {
        event?.preventDefault();
        if (keySet === 'ctrl+s') {
            if ($scope.changed && !$scope.state.error) {
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
            }
        }
    };

    $scope.searchCsv = () => {
        $scope.gridOptions.api.setQuickFilter($scope.search.text);
    };

    $scope.hasHeader = () => {
        parseContent();
        loadGrid();
    };

    $scope.addRowAbove = () => {
        let row = {};
        let columns = $scope.gridOptions.columnApi.getAllColumns();
        for (let i = 0; i < columns.length; i++) {
            row[columns[i].userProvidedColDef.field] = '';
        }
        csvData.data.splice(focusedCellIndex, 0, row);
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.addRowBelow = () => {
        let row = {};
        let columns = $scope.gridOptions.columnApi.getAllColumns();
        for (let i = 0; i < columns.length; i++) {
            row[columns[i].userProvidedColDef.field] = '';
        }
        csvData.data.splice(focusedCellIndex + 1, 0, row);
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.addRow = () => {
        let row = {};
        let columns = $scope.gridOptions.columnApi.getAllColumns();
        for (let i = 0; i < columns.length; i++) {
            row[columns[i].userProvidedColDef.field] = '';
        }
        csvData.data.push(row);
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.deleteRow = () => {
        let rows = $scope.gridOptions.api.getSelectedNodes();
        let indexes = [];
        for (let i = 0; i < rows.length; i++) {
            indexes.push(rows[i].rowIndex);
        }
        if (!indexes.includes(focusedCellIndex)) {
            indexes.push(focusedCellIndex);
        }
        indexes.sort((a, b) => { return a - b; });
        for (let i = indexes.length - 1; i >= 0; i--) {
            csvData.data.splice(indexes[i], 1);
        }
        $scope.gridOptions.api.setRowData(csvData.data);
        fileChanged();
    };

    $scope.addColumn = () => {
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

    $scope.editColumn = () => {
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

    $scope.deleteColumn = () => {
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

    layoutHub.onFocusEditor((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) statusBarHub.showLabel('');
    });

    layoutHub.onReloadEditorParams((data) => {
        if (data.path === $scope.dataParameters.filePath) {
            $scope.$evalAsync(() => {
                $scope.dataParameters = ViewParameters.get();
            });
        };
    });

    workspaceHub.onSaveAll(() => {
        if ($scope.changed && !$scope.state.error) {
            $scope.save();
        }
    });

    workspaceHub.onSaveFile((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) {
            if ($scope.changed && !$scope.state.error) {
                $scope.save();
            }
        }
    });

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'filePath\' data parameter is missing.';
    } else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'contentType\' data parameter is missing.';
    } else {
        if ($scope.dataParameters.hasOwnProperty('header')) $scope.papaConfig.header = $scope.dataParameters.header;
        if ($scope.dataParameters.hasOwnProperty('quotechar')) $scope.papaConfig.quoteChar = $scope.dataParameters.quotechar;
        if ($scope.dataParameters.hasOwnProperty('delimiter')) {
            $scope.papaConfig.delimiter = $scope.dataParameters.delimiter;
            $scope.delimiter = $scope.dataParameters.delimiter;
        }
    }
});