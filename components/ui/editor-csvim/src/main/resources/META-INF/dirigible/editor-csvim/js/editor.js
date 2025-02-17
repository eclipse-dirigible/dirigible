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
const editorView = angular.module('csvim-editor', ['blimpKit', 'platformView', 'platformShortcuts', 'platformSplit', 'WorkspaceService']);
editorView.directive('uniqueField', ($parse) => ({
    require: 'ngModel',
    scope: false,
    link: (scope, _elem, attrs, controller) => {
        let parseFn = $parse(attrs.uniqueField);
        scope.uniqueField = parseFn(scope);
        controller.$validators.forbiddenName = value => {
            let unique = true;
            let correct = RegExp(scope.uniqueField.regex, 'g').test(value);
            if (correct) {
                if ('index' in attrs) {
                    unique = scope.uniqueField.checkUniqueColumn(attrs.index, value);
                } else if ('kindex' in attrs && 'vindex' in attrs) {
                    unique = scope.uniqueField.checkUniqueValue(attrs.kindex, attrs.vindex, value);
                }
            }
            return unique;
        };
    }
}));
editorView.controller('CsvimController', ($scope, $window, WorkspaceService, ViewParameters, ButtonStates) => {
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    const layoutHub = new LayoutHub();
    const dialogHub = new DialogHub();
    $scope.changed = false;
    let workspace = WorkspaceService.getCurrentWorkspace();
    $scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
    $scope.forms = {
        editor: {},
    };
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };
    $scope.searchVisible = false;
    $scope.searchField = { text: '' };
    $scope.schemaError = "Schema can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), and dollar signs ('$')";
    $scope.sequenceError = "Sequence can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), and dollar signs ('$')";
    $scope.tableError = "Table can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), dollar signs ('$') and two consecutive colons ('::')";
    $scope.filepathError = ["Path can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), forward slashes ('/'), dots ('.'), underscores ('_'), and dollar signs ('$')', 'File does not exist."];
    $scope.columnError = "Column keys must be unique and can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), and dollar signs ('$')";
    $scope.versionError = "Version can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), and dollar signs ('$')";
    $scope.fileExists = true;
    $scope.editEnabled = false;
    $scope.dataEmpty = true;
    $scope.csvimData = { files: [] };
    $scope.activeItemId = 0;
    $scope.delimiterList = [',', '\\t', '|', ';', '#'];
    $scope.quoteCharList = ['\'', '"', '#'];

    angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

    $scope.toggleSearch = () => {
        $scope.searchField.text = '';
        $scope.searchVisible = !$scope.searchVisible;
    };

    $scope.checkUniqueColumn = (index, value) => {
        for (let i = 0; i < $scope.csvimData.files[$scope.activeItemId].keys.length; i++) {
            if (i != index) {
                if (value === $scope.csvimData.files[$scope.activeItemId].keys[i].column) {
                    return false;
                }
            }
        }
        return true;
    };

    $scope.checkUniqueValue = (kindex, vindex, value) => {
        for (let i = 0; i < $scope.csvimData.files[$scope.activeItemId].keys[kindex].values.length; i++) {
            if (i != vindex) {
                if (value === $scope.csvimData.files[$scope.activeItemId].keys[kindex].values[i]) {
                    return false;
                }
            }
        }
        return true;
    };

    $scope.openFile = () => {
        WorkspaceService.resourceExists(`${workspace}${$scope.csvimData.files[$scope.activeItemId].file}`).then(() => {
            $scope.$evalAsync(() => {
                $scope.fileExists = true;
            });
            layoutHub.openEditor({
                path: `/${workspace}${$scope.csvimData.files[$scope.activeItemId].file}`,
                contentType: 'text/csv',
                params: {
                    'header': $scope.csvimData.files[$scope.activeItemId].header,
                    'delimiter': $scope.csvimData.files[$scope.activeItemId].delimField,
                    'quotechar': $scope.csvimData.files[$scope.activeItemId].delimEnclosing
                },
            });
        }, () => {
            $scope.$evalAsync(() => {
                $scope.fileExists = false;
            });
        });
    };

    $scope.setEditEnabled = (enabled) => {
        if (enabled != undefined) {
            $scope.editEnabled = enabled;
        } else {
            $scope.editEnabled = !$scope.editEnabled;
        }
    };

    $scope.addNew = () => {
        $scope.searchField.text = '';
        $scope.filterFiles();
        $scope.csvimData.files.push({
            'name': 'Untitled',
            'visible': true,
            'table': '',
            'schema': '',
            'sequence': '',
            'file': '',
            'header': false,
            'useHeaderNames': false,
            'delimField': ';',
            'delimEnclosing': '\'',
            'distinguishEmptyFromNull': true,
            'version': ''
        });
        $scope.activeItemId = $scope.csvimData.files.length - 1;
        $scope.dataEmpty = false;
        $scope.setEditEnabled(true);
        $scope.fileChanged();
    };

    $scope.getFileName = (str, canBeEmpty = true) => {
        if (canBeEmpty) {
            return str.split('\\').pop().split('/').pop();
        }
        let title = str.split('\\').pop().split('/').pop();
        if (title) return title;
        else return 'Untitled';
    };

    $scope.fileSelected = (id) => {
        if ($scope.forms.editor.$valid) {
            $scope.setEditEnabled(false);
            $scope.fileExists = true;
            $scope.activeItemId = id;
        }
    };

    $scope.isDelimiterSupported = (delimiter) => $scope.delimiterList.includes(delimiter);

    $scope.isQuoteCharSupported = (quoteChar) => $scope.quoteCharList.includes(quoteChar);

    $scope.save = (keySet = 'ctrl+s', event) => {
        event?.preventDefault();
        if (keySet === 'ctrl+s') {
            if ($scope.changed && $scope.forms.editor.$valid && !$scope.state.error) {
                $scope.state.busyText = 'Saving...';
                $scope.state.isBusy = true;
                $scope.csvimData.files[$scope.activeItemId].name = $scope.getFileName($scope.csvimData.files[$scope.activeItemId].file, false);
                saveContents(JSON.stringify($scope.csvimData, cleanForOutput, 2));
            }
        }
    };

    $scope.deleteFile = (index) => {
        dialogHub.showDialog({
            title: 'Delete file?',
            message: `Are you sure you want to delete '${$scope.csvimData.files[index].name}'?\nThis action cannot be undone.`,
            preformatted: true,
            buttons: [{
                id: 'bd',
                state: ButtonStates.Negative,
                label: 'Delete',
            },
            {
                id: 'bc',
                state: ButtonStates.Transparent,
                label: 'Cancel',
            }]
        }).then((buttonId) => {
            if (buttonId === 'bd') {
                $scope.$evalAsync(() => {
                    $scope.csvimData.files.splice(index, 1);
                    $scope.fileExists = true;
                    if ($scope.csvimData.files.length > 0) {
                        $scope.dataEmpty = false;
                        if ($scope.activeItemId === index) {
                            $scope.activeItemId = $scope.csvimData.files.length - 1;
                            $scope.setEditEnabled(false);
                        }
                    } else {
                        $scope.setEditEnabled(false);
                        $scope.dataEmpty = true;
                        $scope.activeItemId = 0;
                    }
                    $scope.fileChanged();
                });
            }
        });
    };

    $scope.filterFiles = (event) => {
        if (event && event.originalEvent.key === 'Escape') {
            $scope.searchField.text = '';
            $scope.toggleSearch();
        } else if ($scope.searchField.text) {
            for (let i = 0; i < $scope.csvimData.files.length; i++) {
                if ($scope.csvimData.files[i].name.toLowerCase().includes($scope.searchField.text.toLowerCase())) {
                    $scope.csvimData.files[i].visible = true;
                } else {
                    $scope.csvimData.files[i].visible = false;
                }
            }
            return;
        }
        for (let i = 0; i < $scope.csvimData.files.length; i++) {
            $scope.csvimData.files[i].visible = true;
        }
    };

    $scope.fileChanged = () => {
        $scope.changed = true;
        layoutHub.setEditorDirty({
            path: $scope.dataParameters.filePath,
            dirty: true,
        });
    };

    // function getNumber(str) {
    //     if (typeof str != 'string') return NaN;
    //     let strNum = parseFloat(str);
    //     // use type coercion to parse the _entirety_ of the string (`parseFloat` alone does not do this) and ensure strings of whitespace fail
    //     let isNumber = !isNaN(str) && !isNaN(strNum);
    //     if (isNumber) return strNum;
    //     else return NaN;
    // }
    /**
     * Used for removing some keys from the object before turning it into a string.
     */
    function cleanForOutput(key, value) {
        if (key === 'name' || key === 'visible') {
            return undefined;
        }
        if (key === 'schema' && value === '') {
            return undefined;
        }
        if (key === 'sequence' && value === '') {
            return undefined;
        }
        return value;
    }

    function isObject(value) {
        return (
            typeof value === 'object' &&
            value !== null &&
            !Array.isArray(value)
        );
    }

    const loadFileContents = () => {
        if (!$scope.state.error) {
            $scope.state.isBusy = true;
            WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
                $scope.$evalAsync(() => {
                    let contents = response.data;
                    if (!contents || !isObject(contents)) {
                        contents = { files: [] };
                    }
                    $scope.csvimData = contents;
                    $scope.activeItemId = 0;
                    if ($scope.csvimData.files && $scope.csvimData.files.length > 0) {
                        $scope.dataEmpty = false;
                        for (let i = 0; i < $scope.csvimData.files.length; i++) {
                            $scope.csvimData.files[i]['name'] = $scope.getFileName($scope.csvimData.files[i].file, false);
                            $scope.csvimData.files[i]['visible'] = true;
                        }
                    } else {
                        $scope.dataEmpty = true;
                    }
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
                $scope.errorMessage = `Error saving "${$scope.dataParameters.filePath}". Please look at the console for more information.`;
                $scope.state.isBusy = false;
            });
        });
    }

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
        if ($scope.changed && !$scope.state.error && $scope.forms.editor.$valid) {
            $scope.save();
        }
    });

    workspaceHub.onSaveFile((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) {
            if ($scope.changed && !$scope.state.error && $scope.forms.editor.$valid) {
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
    } else loadFileContents();
});