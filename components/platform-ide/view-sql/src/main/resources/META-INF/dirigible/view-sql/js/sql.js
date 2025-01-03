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
const themingHub = new ThemingHub();
const statusBarHub = new StatusBarHub();
const dialogHub = new DialogHub();
let csrfToken;
const loadingOverview = document.getElementById('loadingOverview');
let monacoTheme = 'vs-light';
let themeId = 'vs-light';
let headElement = document.getElementsByTagName('head')[0];

let autoThemeListener = false;

function setTheme(theme, monaco) {
    if (!theme) theme = themingHub.getSavedTheme();
    themeId = theme.id;
    let themeLinks = headElement.querySelectorAll("link[data-type='theme']");
    for (let i = 0; i < themeLinks.length; i++) {
        headElement.removeChild(themeLinks[i]);
    }
    for (let i = 0; i < theme.links.length; i++) {
        const link = document.createElement('link');
        link.type = 'text/css';
        link.href = theme.links[i];
        link.rel = 'stylesheet';
        link.setAttribute("data-type", "theme");
        headElement.appendChild(link);
    }
    if (theme.type === 'light') {
        monacoTheme = 'vs-light';
        if (monaco) monaco.editor.setTheme(monacoTheme);
        autoThemeListener = false;
    } else if (theme.type === 'dark') {
        if (themeId === 'classic-dark') monacoTheme = 'classic-dark';
        else monacoTheme = 'blimpkit-dark';
        if (monaco) monaco.editor.setTheme(monacoTheme);
        autoThemeListener = false;
    } else {
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            if (themeId.startsWith('classic')) monacoTheme = 'classic-dark';
            else monacoTheme = 'blimpkit-dark';
        } else monacoTheme = 'vs-light';
        if (monaco) monaco.editor.setTheme(monacoTheme);
        autoThemeListener = true;
    }
}

setTheme();

function createEditorInstance() {
    return new Promise((resolve, reject) => {
        setTimeout(function () {
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
                });
                resolve(editor);
                window.onresize = function () {
                    editor.layout();
                };
            } catch (err) {
                reject(err);
            }
        });
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
        // An unique identifier of the contributed action.
        id: "dirigible-sql-execute",

        // A label of the action that will be presented to the user.
        label: "Execute",

        // An optional array of keybindings for the action.
        keybindings: [monaco.KeyCode.F8],

        // Method that will be executed when the action is triggered.
        // @param editor The editor instance is passed in as a convinience
        run: function (editor) {
            const text = editor.getModel().getValueInRange(editor.getSelection());
            const sqlCommand = getSQLCommand(text);
            if (sqlCommand.length > 0) {
                themingHub.postMessage({ topic: "database.sql.execute", data: sqlCommand });
            } else {
                dialogHub.showAlert({
                    type: AlertTypes.Warning,
                    title: 'No SQL command selected',
                    message: 'You must select the command you want to execute.\nUse Ctrl+A (or Cmd+A) if you want to execute everything in the SQL command view.',
                    preformatted: true,
                });
                themingHub.postMessage({ topic: "database.sql.error", data: "No text selected for execution." });
            }
        },
    };
}

(function init() {
    require.config({ paths: { vs: "/webjars/monaco-editor/min/vs" } });

    // @ts-ignore
    require(["vs/editor/editor.main"], function () {
        let _editor;
        createEditorInstance().then((editor) => {
            _editor = editor;
            themingHub.addMessageListener({
                topic: "database.sql.run",
                handler: () => {
                    const executionObject = createExecuteAction();
                    executionObject.run(_editor);
                }
            });
            themingHub.addMessageListener({
                topic: "database.sql.script",
                handler: (command) => {
                    //_editor.trigger('keyboard', 'type', {text: command.data});
                    let line = editor.getPosition();
                    let range = new monaco.Range(line.lineNumber + 1, 1, line.lineNumber + 1, 1);
                    let id = { major: 1, minor: 1 };
                    let text;
                    if (command.startsWith('\n')) text = command;
                    else text = `\n${command}`;
                    let op = { identifier: id, range: range, text: text, forceMoveMarkers: true };
                    editor.executeEdits("source", [op]);
                }
            });
            if (loadingOverview) loadingOverview.classList.add("bk-hidden");
            else console.log(loadingOverview)
            return loadSQLCommand();
        }).then((fileText) => {
            let model = monaco.editor.createModel(fileText, "sql");
            _editor.setModel(model);
            _editor.addAction(createExecuteAction());
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

        monaco.editor.setTheme(monacoTheme);
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', event => {
            if (autoThemeListener) {
                if (event.matches) {
                    if (themeId.startsWith('classic')) monacoTheme = 'classic-dark';
                    else monacoTheme = 'blimpkit-dark';
                } else monacoTheme = 'vs-light';
                this.monaco.editor.setTheme(monacoTheme);
            }
        });

        themingHub.onThemeChange((theme) => {
            setTheme(theme, this.monaco);
        });
    });
})();

const savedSqlCommandKey = `${brandingInfo.keyPrefix}.view-sql.command`;

function saveSQLCommand(sqlCommands) {
    localStorage.setItem(savedSqlCommandKey, sqlCommands);
}

function loadSQLCommand() {
    const sqlCommand = localStorage.getItem(savedSqlCommandKey);
    return sqlCommand ? sqlCommand : '';
}