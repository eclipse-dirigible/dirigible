/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('statements', ['blimpKit', 'platformView', 'platformTheming']).controller('StatementsController', ($scope, $http, $document, $window, $timeout, Theme) => {
    const statusBarHub = new StatusBarHub();
    const dialogHub = new DialogHub();
    const themingHub = new ThemingHub();
    const DB_EXPORT_SERVICE_URL = '/services/data/export-async/';
    $scope.EXPORT_BASE_URL = '/public/cms/__EXPORTS/';
    $scope.ExportStatus = {
        TRIGGRED: 'TRIGGRED',
        FINISHED: 'FINISHED',
        FAILED: 'FAILED',
        UNKNOWN: 'UNKNOWN'
    };
    const lastSelectedDatabaseKey = `${getBrandingInfo().prefix}.view-db-explorer.database`;
    let selectedDatabase = JSON.parse(localStorage.getItem(lastSelectedDatabaseKey) ?? 'null');
    if (!selectedDatabase) {
        selectedDatabase = {
            name: 'DefaultDB', // Datasource
            type: 'metadata' // Database
        };
    }
    const datasourceChangedListener = dialogHub.addMessageListener({
        topic: 'database.datasource.selection.changed',
        handler: (datasource) => {
            selectedDatabase.name = datasource;
        },
    });

    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };
    $scope.exports = [];
    getExports(false);

    let monacoTheme = 'vs-light';
    let autoListener = false;
    let theme = Theme.getTheme();
    let exportButton;

    function onThemeChange(event) {
        if (autoListener) {
            if (event.matches) {
                if (theme.id.startsWith('classic')) monacoTheme = 'classic-dark';
                else monacoTheme = 'blimpkit-dark';
            } else monacoTheme = 'vs-light';
            monaco.editor.setTheme(monacoTheme);
        }
    }

    function setMonacoTheme() {
        if (theme.type === 'light') {
            monacoTheme = 'vs-light';
            monaco.editor.setTheme(monacoTheme);
        }
        else if (theme.type === 'dark') {
            if (theme.id === 'classic-dark') monacoTheme = 'classic-dark';
            else monacoTheme = 'blimpkit-dark';
            monaco.editor.setTheme(monacoTheme);
        } else {
            if ($window.matchMedia && $window.matchMedia('(prefers-color-scheme: dark)').matches) {
                if (theme.id.startsWith('classic')) monacoTheme = 'classic-dark';
                else monacoTheme = 'blimpkit-dark';
            } else monacoTheme = 'vs-light';
            autoListener = true;
            monaco.editor.setTheme(monacoTheme);
        }
    }

    function createEditorInstance() {
        return new Promise((resolve, reject) => {
            try {
                let containerEl = document.getElementById("embeddedEditor");
                if (containerEl.childElementCount > 0) {
                    for (let i = 0; i < containerEl.childElementCount; i++)
                        containerEl.removeChild(containerEl.children.item(i));
                }
                let editor = monaco.editor.create(containerEl, {
                    value: '',
                    automaticLayout: true,
                    language: "sql",
                    minimap: {
                        enabled: false,
                    }
                });
                resolve(editor);
                window.onresize = function () {
                    editor.layout();
                };
            } catch (err) {
                reject(err);
            }
        });
    }

    function createSaveAction() {
        return {
            id: 'dirigible-sql-save',
            label: 'Save',
            keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS],
            precondition: null,
            keybindingContext: null,
            contextMenuGroupId: 'fileIO',
            contextMenuOrder: 1.5,
            run: () => statusBarHub.showMessage('SQL commands saved'),
        };
    }

    function createExecuteAction() {
        return {
            id: "dirigible-sql-execute",
            label: "Execute",
            keybindings: [monaco.KeyCode.F8],
            run: function (editor) {
                const sqlCommand = editor.getModel().getValueInRange(editor.getSelection());
                if (sqlCommand.length > 0) {
                    themingHub.postMessage({ topic: "database.sql.execute", data: sqlCommand });
                } else {
                    dialogHub.showAlert({
                        type: AlertTypes.Warning,
                        title: 'No statement is selected',
                        message: 'You must select the command you want to execute.\nUse Ctrl+A (or Cmd+A) if you want to execute everything in the Statements view.',
                        preformatted: true,
                    });
                    themingHub.postMessage({ topic: "database.sql.error", data: "No text selected for execution." });
                }
            },
        };
    }

    function createExportAction() {
        return {
            id: "dirigible-sql-export",
            label: "Export",
            keybindings: [monaco.KeyCode.F9],
            run: function (editor) {
                const sqlCommand = editor.getModel().getValueInRange(editor.getSelection());
                if (sqlCommand.length > 0) {
                    exportQuery(sqlCommand);
                } else {
                    dialogHub.showAlert({
                        type: AlertTypes.Warning,
                        title: 'No statement is selected',
                        message: 'You must select the command you want to execute.\nUse Ctrl+A (or Cmd+A) if you want to execute and/or export everything.',
                        preformatted: true,
                    });
                    themingHub.postMessage({ topic: "database.sql.error", data: "No text selected for execution." });
                }
            },
        };
    }

    function getExports(open = true) {
        $http({
            method: 'GET',
            url: DB_EXPORT_SERVICE_URL,
            headers: { 'X-Requested-With': 'Fetch' }
        }).then((result) => {
            $scope.exports.length = 0;
            $scope.exports.push(...result.data.reverse());
            if (open) {
                exportButton.focus();
                exportButton.click();
            }
            if (open && !$scope.exports.length) {
                $timeout(() => { getExports(false) }, 500);
            } else {
                for (let i = 0; i < $scope.exports.length; i++) {
                    if ($scope.exports[i].status === $scope.ExportStatus.TRIGGRED) {
                        $timeout(() => { getExports(false) }, 1000);
                        break;
                    }
                }
            }
        }, (reject) => {
            dialogHub.showAlert({
                type: AlertTypes.Error,
                title: 'Could not get export list',
                message: 'Please check the console log for more information.',
                preformatted: true,
            });
            console.error(reject);
        });
    }

    function exportQuery(command) {
        const url = DB_EXPORT_SERVICE_URL + encodeURIComponent(selectedDatabase.name);
        const sql = command.trim().toLowerCase();
        if (sql.startsWith('select')) {
            $http({
                method: 'POST',
                url: url,
                data: command,
                headers: {
                    'Content-Type': 'text/plain',
                    'X-Requested-With': 'Fetch',
                }
            }).then(() => {
                getExports();
            }, (reject) => {
                dialogHub.showAlert({
                    type: AlertTypes.Error,
                    title: 'Export error',
                    message: reject.data.message ?? 'Unknown error',
                    preformatted: true,
                });
                console.error(reject);
            });
        } else if (sql.startsWith('query: ')) {
            $http({
                method: 'POST',
                url: url,
                data: command.substring(7).trim(),
                headers: {
                    'Content-Type': 'text/plain',
                    'X-Requested-With': 'Fetch',
                }
            }).then(() => {
                getExports();
            }, (reject) => {
                dialogHub.showAlert({
                    type: AlertTypes.Error,
                    title: 'Export error',
                    message: reject.data.message ?? 'Unknown error',
                    preformatted: true,
                });
                console.error(reject);
            });
        }
    }

    const savedSqlCommandKey = `${getBrandingInfo().prefix}.view-sql.command`;

    function saveSQLCommand(sqlCommands) {
        localStorage.setItem(savedSqlCommandKey, sqlCommands);
    }

    function loadSQLCommand() {
        const sqlCommand = localStorage.getItem(savedSqlCommandKey);
        return sqlCommand ? sqlCommand : '';
    }

    let themeChangeListener;

    let _editor;

    $scope.deleteExport = (id) => {
        $http({
            method: 'DELETE',
            url: DB_EXPORT_SERVICE_URL + encodeURIComponent(id),
            headers: { 'X-Requested-With': 'Fetch' }
        }).then(() => {
            for (let i = 0; i < $scope.exports.length; i++) {
                if ($scope.exports[i].id === id) {
                    $scope.exports.splice(i, 1);
                    break;
                }
            }
        }, (reject) => {
            dialogHub.showAlert({
                type: AlertTypes.Error,
                title: 'Could not get export list',
                message: 'Please check the console log for more information.',
                preformatted: true,
            });
            console.error(reject);
        });
    };

    $scope.executeSQL = () => {
        const executionObject = createExecuteAction();
        executionObject.run(_editor);
    };

    $scope.exportSQL = () => {
        const executionObject = createExportAction();
        executionObject.run(_editor);
    };

    $scope.deleteExports = () => {
        $http({
            method: 'DELETE',
            url: DB_EXPORT_SERVICE_URL,
            headers: { 'X-Requested-With': 'Fetch' }
        }).then(() => {
            $scope.exports.length = 0;
        }, (reject) => {
            dialogHub.showAlert({
                type: AlertTypes.Error,
                title: 'Could not get export list',
                message: 'Please check the console log for more information.',
                preformatted: true,
            });
            console.error(reject);
        });
    };

    $scope.getExportDate = (date) => {
        return new Intl.DateTimeFormat(undefined, {
            dateStyle: 'short',
            timeStyle: 'short',
        }).format(new Date(date));
    };

    const scriptListener = dialogHub.addMessageListener({
        topic: "database.sql.script",
        handler: (command) => {
            //_editor.trigger('keyboard', 'type', {text: command.data});
            let line = _editor.getPosition();
            let range = new monaco.Range(line.lineNumber + 1, 1, line.lineNumber + 1, 1);
            let id = { major: 1, minor: 1 };
            let text;
            if (command.startsWith('\n')) text = command;
            else text = `\n${command}`;
            let op = { identifier: id, range: range, text: text, forceMoveMarkers: true };
            _editor.executeEdits("source", [op]);
        }
    });

    angular.element($document[0]).ready(() => {
        exportButton = $document[0].querySelector(`#exports`);
        require.config({ paths: { vs: "/webjars/monaco-editor/min/vs" } });

        // @ts-ignore
        require(["vs/editor/editor.main"], function () {
            createEditorInstance().then((editor) => {
                _editor = editor;
                $scope.$evalAsync(() => {
                    $scope.state.isBusy = false;
                });
                return loadSQLCommand();
            }).then((fileText) => {
                let model = monaco.editor.createModel(fileText, "sql");
                _editor.setModel(model);
                _editor.addAction(createExecuteAction());
                _editor.addAction(createExportAction());
                _editor.addAction(createSaveAction());
                _editor.onDidChangeCursorPosition(function (e) {
                    statusBarHub.showLabel("Line " + e.position.lineNumber + " : Column " + e.position.column);
                });
                _editor.onDidChangeModelContent(function (_e) {
                    saveSQLCommand(_editor.getValue());
                });
            });
            monaco.editor.defineTheme('blimpkit-dark', {
                base: 'vs-dark',
                inherit: true,
                rules: [{ background: '1d1d1d' }],
                colors: {
                    'editor.background': '#1d1d1d',
                    'breadcrumb.background': '#1d1d1d',
                    'minimap.background': '#1d1d1d',
                    'editorGutter.background': '#1d1d1d',
                    'editorMarkerNavigation.background': '#1d1d1d',
                    'input.background': '#242424',
                    'input.border': '#4e4e4e',
                    'editorWidget.background': '#1d1d1d',
                    'editorWidget.border': '#313131',
                    'editorSuggestWidget.background': '#262626',
                    'dropdown.background': '#262626',
                }
            });

            monaco.editor.defineTheme('classic-dark', {
                base: 'vs-dark',
                inherit: true,
                rules: [{ background: '1c2228' }],
                colors: {
                    'editor.background': '#1c2228',
                    'breadcrumb.background': '#1c2228',
                    'minimap.background': '#1c2228',
                    'editorGutter.background': '#1c2228',
                    'editorMarkerNavigation.background': '#1c2228',
                    'input.background': '#29313a',
                    'input.border': '#8696a9',
                    'editorWidget.background': '#1c2228',
                    'editorWidget.border': '#495767',
                    'editorSuggestWidget.background': '#29313a',
                    'dropdown.background': '#29313a',
                }
            });

            setMonacoTheme();

            $window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', onThemeChange);

            themeChangeListener = themingHub.onThemeChange((newTheme) => {
                theme = newTheme;
                setMonacoTheme();
            });
        });
    });

    $scope.$on('$destroy', () => {
        themingHub.removeMessageListener(themeChangeListener);
        dialogHub.removeMessageListener(datasourceChangedListener);
        dialogHub.removeMessageListener(scriptListener);
        $window.matchMedia('(prefers-color-scheme: dark)').removeMessageListener('change', onThemeChange);
    });
});