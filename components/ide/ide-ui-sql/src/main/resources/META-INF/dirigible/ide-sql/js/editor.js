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
let messageHub = new FramesMessageHub();
let sqlPlaceholder = "-- Press F8 to execute the selected text\n";
let csrfToken;
let loadingOverview = document.getElementById('loadingOverview');
let loadingMessage = document.getElementById('loadingMessage');

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
                    value: sqlPlaceholder,
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
            let text = editor.getModel().getValueInRange(editor.getSelection());
            if (text.length > 0) {
                let sqlCommand = getSQLCommand(text);
                messageHub.post({ data: sqlCommand }, "database.sql.execute");
            } else {
                messageHub.post({ data: "No text selected for execution." }, "database.sql.error");
            }
        },
    };
}

(function init() {
    setTheme();
    require.config({ paths: { vs: "/webjars/monaco-editor/min/vs" } });

    require(["vs/editor/editor.main"], function () {
        messageHub.subscribe(function () {
            setTheme(false);
            monaco.editor.setTheme(monacoTheme);
        }, 'ide.themeChange');
        let _editor;
        createEditorInstance()
            .then((editor) => {
                _editor = editor;
                messageHub.subscribe(function () {
                    let executionObject = createExecuteAction();
                    executionObject.run(_editor);
                }, "database.sql.run");
                messageHub.subscribe(function (command) {
                    //_editor.trigger('keyboard', 'type', {text: command.data});
                    var line = editor.getPosition();
                    var range = new monaco.Range(line.lineNumber, 1, line.lineNumber, 1);
                    var id = { major: 1, minor: 1 };
                    var text = command.data;
                    var op = { identifier: id, range: range, text: text, forceMoveMarkers: true };
                    editor.executeEdits("source", [op]);
                }, "database.sql.script");
                let sqlCommand = loadSQLCommand();
                return sqlPlaceholder + sqlCommand;
            })
            .then((fileText) => {
                let model = monaco.editor.createModel(fileText, "sql");
                _editor.setModel(model);
                _editor.addAction(createExecuteAction());
                _editor.onDidChangeCursorPosition(function (e) {
                    let caretInfo = "Line " + e.position.lineNumber + " : Column " + e.position.column;
                    messageHub.post({ data: caretInfo }, "status.caret");
                });
                _editor.onDidChangeModelContent(function (e) {
                    saveSQLCommand(_editor.getValue());
                });
            });
        monaco.editor.defineTheme('quartz-dark', {
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
    });
})();

function getSQLCommand(text) {
    let sqlCommand = text;
    let sqlPlaceHolderIndex = text.indexOf(sqlPlaceholder);
    if (sqlPlaceHolderIndex >= 0) {
        sqlCommand = text.substring(sqlPlaceholder.length);
    }
    return sqlCommand;
}

function saveSQLCommand(text) {
    let sqlCommand = getSQLCommand(text);
    localStorage.setItem("DIRIGIBLE.IDE.DATABASE.sqlCommand", sqlCommand);
}

function loadSQLCommand() {
    let sqlCommand = localStorage.getItem("DIRIGIBLE.IDE.DATABASE.sqlCommand");
    return sqlCommand ? sqlCommand : "";
}