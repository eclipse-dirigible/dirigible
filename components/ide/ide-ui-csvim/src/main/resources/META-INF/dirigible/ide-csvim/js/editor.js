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
const editorView = angular.module('csvim-editor', ["ideUI", "ideView", "ideWorkspace"]);
editorView.directive('uniqueField', function ($parse) {
    return {
        require: 'ngModel',
        scope: false,
        link: (scope, _elem, attrs, controller) => {
            let parseFn = $parse(attrs.uniqueField);
            scope.uniqueField = parseFn(scope);
            controller.$validators.forbiddenName = value => {
                let unique = true;
                let correct = RegExp(scope.uniqueField.regex, 'g').test(value);
                if (correct) {
                    if ("index" in attrs) {
                        unique = scope.uniqueField.checkUniqueColumn(attrs.index, value);
                    } else if ("kindex" in attrs && "vindex" in attrs) {
                        unique = scope.uniqueField.checkUniqueValue(attrs.kindex, attrs.vindex, value);
                    }
                }
                return unique;
            };
        }
    };
});
editorView.controller('CsvimViewController', function ($scope, messageHub, workspaceApi, $window, ViewParameters) {
    $scope.isFileChanged = false;
    let workspace = workspaceApi.getCurrentWorkspace();
    $scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
    $scope.forms = {
        editor: {},
    };
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: "Loading...",
    };
    $scope.searchVisible = false;
    $scope.searchField = { text: '' };
    $scope.schemaError = 'Schema can only contain letters (a-z, A-Z), numbers (0-9), hyphens ("-"), dots ("."), underscores ("_"), and dollar signs ("$")';
    $scope.sequenceError = 'Sequence can only contain letters (a-z, A-Z), numbers (0-9), hyphens ("-"), dots ("."), underscores ("_"), and dollar signs ("$")';
    $scope.tableError = 'Table can only contain letters (a-z, A-Z), numbers (0-9), hyphens ("-"), dots ("."), underscores ("_"), dollar signs ("$") and two consecutive colons ("::")';
    $scope.filepathError = ['Path can only contain letters (a-z, A-Z), numbers (0-9), hyphens ("-"), forward slashes ("/"), dots ("."), underscores ("_"), and dollar signs ("$")', 'File does not exist.'];
    $scope.columnError = 'Column keys must be unique and can only contain letters (a-z, A-Z), numbers (0-9), hyphens ("-"), dots ("."), underscores ("_"), and dollar signs ("$")';
    $scope.versionError = 'Version can only contain letters (a-z, A-Z), numbers (0-9), hyphens ("-"), dots ("."), underscores ("_"), and dollar signs ("$")';
    $scope.fileExists = true;
    $scope.editEnabled = false;
    $scope.dataEmpty = true;
    $scope.csvimData = { files: [] };
    $scope.activeItemId = 0;
    $scope.delimiterList = [',', '\\t', '|', ';', '#'];
    $scope.quoteCharList = ["'", "\"", "#"];

    angular.element($window).bind("focus", function () {
        messageHub.setFocusedEditor($scope.dataParameters.file);
        messageHub.setStatusCaret('');
    });

    $scope.toggleSearch = function () {
        $scope.searchField.text = '';
        $scope.searchVisible = !$scope.searchVisible;
    };

    $scope.checkUniqueColumn = function (index, value) {
        for (let i = 0; i < $scope.csvimData.files[$scope.activeItemId].keys.length; i++) {
            if (i != index) {
                if (value === $scope.csvimData.files[$scope.activeItemId].keys[i].column) {
                    return false;
                }
            }
        }
        return true;
    };

    $scope.checkUniqueValue = function (kindex, vindex, value) {
        for (let i = 0; i < $scope.csvimData.files[$scope.activeItemId].keys[kindex].values.length; i++) {
            if (i != vindex) {
                if (value === $scope.csvimData.files[$scope.activeItemId].keys[kindex].values[i]) {
                    return false;
                }
            }
        }
        return true;
    };

    $scope.openFile = function () {
        workspaceApi.resourceExists(`${workspace.name}${$scope.csvimData.files[$scope.activeItemId].file}`).then(function (response) {
            if (response.status === 200) {
                $scope.fileExists = true;
                messageHub.openEditor(
                    `/${workspace.name}${$scope.csvimData.files[$scope.activeItemId].file}`,
                    $scope.csvimData.files[$scope.activeItemId].name,
                    "text/csv",
                    undefined,
                    {
                        "header": $scope.csvimData.files[$scope.activeItemId].header,
                        "delimiter": $scope.csvimData.files[$scope.activeItemId].delimField,
                        "quotechar": $scope.csvimData.files[$scope.activeItemId].delimEnclosing
                    },
                );
            } else {
                $scope.fileExists = false;
            }
        });
    };

    $scope.setEditEnabled = function (enabled) {
        if (enabled != undefined) {
            $scope.editEnabled = enabled;
        } else {
            $scope.editEnabled = !$scope.editEnabled;
        }
    };

    $scope.addNew = function () {
        $scope.searchField.text = "";
        $scope.filterFiles();
        $scope.csvimData.files.push({
            "name": "Untitled",
            "visible": true,
            "table": "",
            "schema": "",
            "sequence": "",
            "file": "",
            "header": false,
            "useHeaderNames": false,
            "delimField": ";",
            "delimEnclosing": "\"",
            "distinguishEmptyFromNull": true,
            "version": ""
        });
        $scope.activeItemId = $scope.csvimData.files.length - 1;
        $scope.dataEmpty = false;
        $scope.setEditEnabled(true);
        $scope.fileChanged();
    };

    $scope.getFileName = function (str, canBeEmpty = true) {
        if (canBeEmpty) {
            return str.split('\\').pop().split('/').pop();
        }
        let title = str.split('\\').pop().split('/').pop();
        if (title) return title;
        else return "Untitled";
    };

    $scope.fileSelected = function (id) {
        if ($scope.forms.editor.$valid) {
            $scope.setEditEnabled(false);
            $scope.fileExists = true;
            $scope.activeItemId = id;
        }
    };

    $scope.isDelimiterSupported = function (delimiter) {
        return $scope.delimiterList.includes(delimiter);
    };

    $scope.isQuoteCharSupported = function (quoteChar) {
        return $scope.quoteCharList.includes(quoteChar);
    };

    $scope.save = function (_keySet, event) {
        if (event) event.preventDefault();
        if ($scope.forms.editor.$valid && $scope.isFileChanged) {
            $scope.csvimData.files[$scope.activeItemId].name = $scope.getFileName($scope.csvimData.files[$scope.activeItemId].file, false);
            saveContents(JSON.stringify($scope.csvimData, cleanForOutput, 2));
        }
    };

    $scope.deleteFile = function (index) {
        messageHub.showDialogAsync(
            'Delete file?',
            `Are you sure you want to delete "${$scope.csvimData.files[index].name}"? This action cannot be undone.`,
            [{
                id: "b1",
                type: "emphasized",
                label: "Yes",
            },
            {
                id: "b2",
                type: "normal",
                label: "No",
            }],
        ).then(function (msg) {
            if (msg.data === "b1") {
                $scope.$apply(function () {
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

    $scope.filterFiles = function () {
        if ($scope.searchField.text) {
            for (let i = 0; i < $scope.csvimData.files.length; i++) {
                if ($scope.csvimData.files[i].name.toLowerCase().includes($scope.searchField.text.toLowerCase())) {
                    $scope.csvimData.files[i].visible = true;
                } else {
                    $scope.csvimData.files[i].visible = false;
                }
            }
        } else {
            for (let i = 0; i < $scope.csvimData.length; i++) {
                $scope.csvimData.files[i].visible = true;
            }
        }
    };

    $scope.fileChanged = function () {
        $scope.isFileChanged = true;
        messageHub.setEditorDirty($scope.dataParameters.file, $scope.isFileChanged);
    };

    // function getNumber(str) {
    //     if (typeof str != "string") return NaN;
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
        if (key === "name" || key === "visible") {
            return undefined;
        }
        if (key === "schema" && value === "") {
            return undefined;
        }
        if (key === "sequence" && value === "") {
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

    function loadFileContents() {
        workspaceApi.loadContent('', $scope.dataParameters.file).then(function (response) {
            if (response.status === 200) {
                let contents = response.data;
                if (!contents || !isObject(contents)) {
                    contents = { files: [] };
                }
                $scope.csvimData = contents;
                $scope.activeItemId = 0;
                if ($scope.csvimData.files && $scope.csvimData.files.length > 0) {
                    $scope.dataEmpty = false;
                    for (let i = 0; i < $scope.csvimData.files.length; i++) {
                        $scope.csvimData.files[i]["name"] = $scope.getFileName($scope.csvimData.files[i].file, false);
                        $scope.csvimData.files[i]["visible"] = true;
                    }
                } else {
                    $scope.$apply(() => $scope.dataEmpty = true);
                }
                $scope.$apply(() => $scope.state.isBusy = false);
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
        "editor.file.save.all",
        function () {
            if ($scope.isFileChanged && !$scope.state.error && $scope.forms.editor.$valid) {
                $scope.save();
            }
        },
        true,
    );

    messageHub.onDidReceiveMessage(
        "editor.file.save",
        function (msg) {
            if (!$scope.state.error && $scope.forms.editor.$valid) {
                let file = msg.data && typeof msg.data === 'object' && msg.data.file;
                if (file && file === $scope.dataParameters.file) {
                    if ($scope.isFileChanged) $scope.save();
                }
            }
        },
        true,
    );

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('file')) {
        $scope.state.error = true;
        $scope.errorMessage = "The 'file' data parameter is missing.";
    } else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
        $scope.state.error = true;
        $scope.errorMessage = "The 'contentType' data parameter is missing.";
    } else {
        loadFileContents();
    }
});