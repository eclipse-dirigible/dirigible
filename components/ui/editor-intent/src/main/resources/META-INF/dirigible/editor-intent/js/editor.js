/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const editorView = angular.module('intentEditor', ['blimpKit', 'platformView', 'platformShortcuts', 'platformSplit', 'WorkspaceService']);
editorView.controller('IntentEditorController', ($scope, $http, ViewParameters, WorkspaceService) => {
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    const layoutHub = new LayoutHub();
    const dialogHub = new DialogHub();
    const PARSE_URL = '/services/ide/intent/parse';
    const GENERATE_URL = '/services/ide/intent/generate';
    const AGENT_URL = '/services/ide/intent/agent';
    const CONVERSATIONS_URL = '/services/ide/intent/conversations';

    $scope.state = { isBusy: true, error: false };
    $scope.errorMessage = '';
    $scope.changed = false;
    $scope.text = '';
    $scope.model = { entities: [], processes: [], forms: [], reports: [], permissions: [], seeds: [], notifications: [], schedules: [], integrations: [], inbound: [], rollups: [] };
    $scope.issues = [];
    // Non-fatal notes from the last Generate ("warnings" in its response): glue that could not be
    // emitted, or a report aggregating over a lifecycle entity without a scope. Kept apart from
    // `issues` (validation errors) because the generation itself succeeded.
    $scope.warnings = [];
    let savedText = '';
    let parseTimer = null;

    // ----- File location -----------------------------------------------------

    /** filePath has the shape /<workspace>/<project>/<path/within/project> */
    const fileLocation = () => {
        const parts = $scope.dataParameters.filePath.split('/');
        return {
            workspace: parts[1],
            project: parts[2],
            path: parts.slice(3)
                       .join('/'),
        };
    };

    // ----- Load / save ---------------------------------------------------------

    const loadFileContents = () => {
        WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
            $scope.$evalAsync(() => {
                $scope.text = typeof response.data === 'string' ? response.data : JSON.stringify(response.data, null, 2);
                savedText = $scope.text;
                $scope.state.isBusy = false;
                refreshPreview();
                mountEditor();
                restoreConversation();
            });
        }, (response) => {
            console.error(response);
            if (response && response.status === 404) {
                // The file no longer exists (e.g. the workspace was cleaned by a rebuild) - close the stale editor.
                layoutHub.closeEditor({ path: $scope.dataParameters.filePath });
                return;
            }
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = 'Error while loading the intent file. Please look at the console for more information.';
                $scope.state.isBusy = false;
            });
        });
    };

    $scope.save = (keySet = 'ctrl+s', event) => {
        event?.preventDefault();
        if (!$scope.changed || $scope.state.error) return;
        $scope.state.isBusy = true;
        WorkspaceService.saveContent($scope.dataParameters.filePath, $scope.text).then(() => {
            savedText = $scope.text;
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
    };

    // ----- Monaco source editor ------------------------------------------------
    // The left pane is a Monaco editor (the same engine as the platform's main code editor) with YAML
    // highlighting; $scope.text stays the single source the parse / save / diagram code reads, kept in
    // sync from Monaco's change event. The theme follows the IDE via ThemingHub, mirroring editor-monaco.
    const themingHub = new ThemingHub();
    let monacoEditor = null;
    let monacoApi = null; // the loaded monaco module, reused by the AI assistant's diff editor

    const monacoThemeFor = (theme) => {
        if (!theme) theme = themingHub.getSavedTheme();
        if (theme && theme.type === 'light') return 'vs-light';
        const classic = theme && typeof theme.id === 'string' && theme.id.startsWith('classic');
        if (theme && theme.type === 'dark') return classic ? 'classic-dark' : 'blimpkit-dark';
        const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (!prefersDark) return 'vs-light';
        return classic ? 'classic-dark' : 'blimpkit-dark';
    };

    // The platform's dark editor themes - editor-monaco defines these too; redefining is idempotent.
    const defineMonacoThemes = (monaco) => {
        monaco.editor.defineTheme('blimpkit-dark', {
            base: 'vs-dark', inherit: true, rules: [{ background: '1d1d1d' }],
            colors: { 'editor.background': '#1d1d1d', 'minimap.background': '#1d1d1d', 'editorGutter.background': '#1d1d1d' }
        });
        monaco.editor.defineTheme('classic-dark', {
            base: 'vs-dark', inherit: true, rules: [{ background: '1c2228' }],
            colors: { 'editor.background': '#1c2228', 'minimap.background': '#1c2228', 'editorGutter.background': '#1c2228' }
        });
    };

    const mountEditor = () => {
        if (monacoEditor || typeof require === 'undefined') return;
        require.config({ paths: { vs: '/webjars/monaco-editor/min/vs' } });
        require(['vs/editor/editor.main'], (monaco) => {
            monacoApi = monaco;
            const container = document.getElementById('intent-monaco');
            if (!container) return;
            defineMonacoThemes(monaco);
            monacoEditor = monaco.editor.create(container, {
                value: $scope.text || '',
                language: 'yaml',
                theme: monacoThemeFor(),
                automaticLayout: true,
                fontSize: 13,
                tabSize: 2,
                insertSpaces: true,
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                renderWhitespace: 'selection',
            });
            // Monaco owns the text now; push every edit back into $scope.text and run the same
            // dirty-tracking + debounced re-parse the textarea's ng-change used to drive.
            monacoEditor.onDidChangeModelContent(() => {
                $scope.$evalAsync(() => {
                    $scope.text = monacoEditor.getValue();
                    handleTextChanged();
                });
            });
            themingHub.onThemeChange((theme) => monaco.editor.setTheme(monacoThemeFor(theme)));
            recoverWhenRevealed(container);
        });
    };

    // After a browser refresh every open editor iframe bootstraps at once, but only the focused tab
    // is visible - the rest sit in `display:none` tab panels (the layout toggles them with ng-show).
    // Monaco created inside a hidden, zero-size container measures a zero-width font and lays out to
    // nothing, and unlike the geometry-driven mxGraph diagrams it does not repaint on its own when the
    // tab is finally revealed: the text pane shows up blank. `automaticLayout` re-runs layout on the
    // size change but keeps the stale zero font metrics, so the glyphs stay invisible. Watch for the
    // container gaining real size (the tab being shown) and, once, remeasure the fonts and relayout so
    // the source appears. A no-op for the focused editor, which is already sized when Monaco is created.
    let revealObserver = null;
    const recoverWhenRevealed = (container) => {
        if (typeof ResizeObserver === 'undefined') return;
        if (container.clientWidth > 0 && container.clientHeight > 0) return; // already visible at creation
        revealObserver = new ResizeObserver(() => {
            if (container.clientWidth > 0 && container.clientHeight > 0) {
                revealObserver.disconnect();
                revealObserver = null;
                if (!monacoEditor) return;
                if (monacoApi) monacoApi.editor.remeasureFonts();
                monacoEditor.layout();
            }
        });
        revealObserver.observe(container);
    };

    // ----- Live preview --------------------------------------------------------

    const handleTextChanged = () => {
        const dirty = $scope.text !== savedText;
        if (dirty !== $scope.changed) {
            $scope.changed = dirty;
            layoutHub.setEditorDirty({
                path: $scope.dataParameters.filePath,
                dirty: dirty,
            });
        }
        if (parseTimer) clearTimeout(parseTimer);
        parseTimer = setTimeout(refreshPreview, 600);
    };

    const refreshPreview = () => {
        // The buffer changed, so the last Generate's notes describe a model that no longer exists.
        $scope.warnings = [];
        return $http.post(PARSE_URL, $scope.text || '', { headers: { 'Content-Type': 'text/plain' } }).then((response) => {
            $scope.issues = [];
            $scope.model = normalize(response.data);
            render();
        }, (response) => {
            $scope.$evalAsync(() => {
                if (response.status === 422 && response.data && response.data.issues) {
                    $scope.issues = response.data.issues;
                } else {
                    $scope.issues = ['Unable to parse the intent. Please look at the console for more information.'];
                    console.error(response);
                }
            });
        });
    };

    // Re-parse and re-validate the current buffer on demand. Generate resolves cross-model
    // dependencies against other projects' already-generated .model files; when one is missing it
    // fails with issues that stay pinned in the strip until the buffer is re-parsed (Generate itself
    // stays clickable — it re-validates server-side). After generating the dependency project,
    // Refresh clears those stale issues and re-renders — no browser reload needed.
    $scope.refresh = () => {
        refreshPreview().then(() => {
            statusBarHub.showMessage('Re-validated the intent');
        });
    };

    const normalize = (model) => {
        model = model || {};
        model.entities = model.entities || [];
        model.processes = model.processes || [];
        model.forms = model.forms || [];
        model.reports = model.reports || [];
        model.permissions = model.permissions || [];
        model.seeds = model.seeds || [];
        model.notifications = model.notifications || [];
        model.schedules = model.schedules || [];
        model.integrations = model.integrations || [];
        model.inbound = model.inbound || [];
        model.rollups = model.rollups || [];
        return model;
    };

    // ----- Generate ------------------------------------------------------------

    const finishGenerate = (location, written, scrubbed, codeCount) => {
        dialogHub.closeBusyDialog();
        const code = codeCount ? `, generated code from ${codeCount} model(s)` : '';
        const stale = scrubbed ? `, removed ${scrubbed} stale` : '';
        statusBarHub.showMessage(`Generated ${written} model file(s)${stale}${code} in '${location.project}'`);
        dialogHub.postMessage({ topic: 'projects.tree.refresh', data: { partial: true, project: location.project, workspace: location.workspace } });
        $scope.$evalAsync(() => { $scope.state.isBusy = false; });
    };

    // Report the model-to-code generations the server ran (the templates + parameters registered in
    // the <intent>.settings). Generate is one call now - the models and the code are produced in the
    // same request - so this only reads the outcome each entry carries.
    const reportCodeGenerations = (location, plan, written, scrubbed) => {
        const failed = plan.filter((entry) => entry.generated === false);
        finishGenerate(location, written, scrubbed, plan.length - failed.length);
        if (failed.length) {
            const details = failed.map((entry) => `${entry.path}: ${entry.error || 'unknown error'}`).join('\n');
            dialogHub.showAlert({
                title: 'Failed to generate code',
                message: `Models were generated, but generating code failed for:\n\n${details}`,
                type: AlertTypes.Error,
                preformatted: true,
            });
        }
    };

    $scope.generate = () => {
        const location = fileLocation();
        $scope.state.isBusy = true;
        dialogHub.showBusyDialog('Generating model files and code');
        $http.post(`${GENERATE_URL}?workspace=${encodeURIComponent(location.workspace)}&project=${encodeURIComponent(location.project)}&path=${encodeURIComponent(location.path)}`)
             .then((response) => {
                 $scope.issues = []; // a successful generate clears any pinned cross-model issue from a prior attempt
                 $scope.warnings = response.data.warnings || [];
                 const written = (response.data.written || []).length;
                 const scrubbed = (response.data.scrubbed || []).length;
                 // The server generated the code as part of this call; each entry reports its outcome.
                 reportCodeGenerations(location, response.data.codeGenerations || [], written, scrubbed);
             }, (response) => {
                 console.error(response);
                 dialogHub.closeBusyDialog();
                 $scope.$evalAsync(() => {
                     $scope.state.isBusy = false;
                     if (response.status === 422 && response.data && response.data.issues) {
                         $scope.issues = response.data.issues;
                     } else {
                         dialogHub.showAlert({
                             title: 'Failed to generate',
                             message: 'Please look at the console for more information',
                             type: AlertTypes.Error,
                             preformatted: false,
                         });
                     }
                 });
             });
    };

    // ----- Diagram rendering -----------------------------------------------------
    // The mxGraph rendering lives in the framework-free IntentDiagrams module (js/intent-diagrams.js),
    // shared with the Harmonia Builder shell; the editor only points it at the model and the host.

    const diagramHost = () => document.getElementById('intent-diagrams');

    const render = () => {
        const host = diagramHost();
        if (host) IntentDiagrams.render($scope.model, host);
    };

    // ----- AI assistant (chat + patch preview) -----------------------------------
    // A right-hand pane where the developer asks for changes in natural language. The assistant
    // returns the COMPLETE proposed app.intent (never a re-emitted model file); we show it as a
    // Monaco diff against the current buffer and, on Accept, replace the buffer (still unsaved) so
    // the normal Save + Generate flow stays in the developer's hands - the agent never writes disk.

    // `messages` is the display list (may hold UI-only notes and errors); `turns` is the clean
    // user/assistant transcript sent upstream - the model API requires strictly alternating roles.
    $scope.chat = { open: false, busy: false, input: '', messages: [], turns: [], proposalPending: false };
    let proposedYaml = null;
    let diffEditor = null;
    // How many of chat.messages the server has accepted; everything past it is still unsaved.
    let persistedMessages = 0;

    $scope.toggleChat = () => { $scope.chat.open = !$scope.chat.open; };

    const scrollChatToBottom = () => {
        setTimeout(() => {
            const list = document.getElementById('intent-chat-messages');
            if (list) list.scrollTop = list.scrollHeight;
        }, 0);
    };

    // ----- Conversation history --------------------------------------------------
    // The conversation is the record of WHY the intent looks the way it does, so it is persisted
    // server-side (tenant-aware, append-only) instead of living in this controller until the tab
    // closes. It is keyed by project + surface + intent file - never by workspace or user, so the same
    // app opened on another machine, or by a teammate, restores the same dialogue.

    const conversationQuery = () => {
        const location = fileLocation();
        return `?project=${encodeURIComponent(location.project)}&surface=intent-editor&path=${encodeURIComponent(location.path)}`;
    };

    /** Load the stored conversation of this intent file into the chat pane. */
    const restoreConversation = () => {
        $http.get(CONVERSATIONS_URL + conversationQuery())
             .then((response) => {
                 const stored = response.data || {};
                 $scope.chat.messages = (stored.messages || []).map((m) => ({ role: m.role, text: m.content }));
                 // The transcript is DERIVED from the stored roles rather than stored a second time, so
                 // the two lists cannot drift - and it is derived SERVER-side, because "which messages
                 // may be replayed" is a property of the roles: a failed turn keeps the message that was
                 // sent (support needs it) but replaying that unanswered turn would break the model
                 // API's alternation.
                 $scope.chat.turns = (stored.turns || []).map((t) => ({ role: t.role, content: t.content }));
                 persistedMessages = $scope.chat.messages.length;
                 scrollChatToBottom();
             }, (response) => {
                 // No history is a degraded pane, never a broken editor.
                 console.error(response);
             });
    };

    /**
     * Append whatever this file's conversation has said but not yet saved. Called once per turn, after
     * it has fully resolved - a failed turn's error bubble is part of the record too.
     *
     * On failure the count is deliberately left where it is, so the next turn re-sends this tail and a
     * transient outage costs nothing.
     */
    const flushConversation = () => {
        const pending = $scope.chat.messages.slice(persistedMessages);
        if (!pending.length) return;
        const messages = pending.map((m) => ({ role: m.role, content: m.text }));
        $http.post(CONVERSATIONS_URL + '/messages' + conversationQuery(), { messages: messages })
             .then(() => {
                 // Advance by what was actually sent - a message typed while the call was in flight has
                 // not been saved yet.
                 persistedMessages += messages.length;
             }, (response) => {
                 console.error(response);
             });
    };

    const disposeDiff = () => {
        if (diffEditor) {
            const model = diffEditor.getModel();
            if (model) {
                if (model.original) model.original.dispose();
                if (model.modified) model.modified.dispose();
            }
            diffEditor.dispose();
            diffEditor = null;
        }
    };

    const showDiff = () => {
        if (!monacoApi) return;
        disposeDiff();
        const container = document.getElementById('intent-chat-diff');
        if (!container) return;
        diffEditor = monacoApi.editor.createDiffEditor(container, {
            theme: monacoThemeFor(),
            automaticLayout: true,
            readOnly: true,
            renderSideBySide: false,
            fontSize: 12,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
        });
        diffEditor.setModel({
            original: monacoApi.editor.createModel($scope.text || '', 'yaml'),
            modified: monacoApi.editor.createModel(proposedYaml || '', 'yaml'),
        });
    };

    $scope.sendChat = () => {
        const message = ($scope.chat.input || '').trim();
        if (!message || $scope.chat.busy) return;
        // Discard any still-open proposal when a new turn starts.
        $scope.rejectProposal();
        const history = $scope.chat.turns.slice();
        $scope.chat.messages.push({ role: 'user', text: message });
        $scope.chat.turns.push({ role: 'user', content: message });
        $scope.chat.input = '';
        $scope.chat.busy = true;
        scrollChatToBottom();
        $http.post(AGENT_URL, { yaml: $scope.text || '', message: message, history: history })
             .then((response) => {
                 $scope.chat.busy = false;
                 const reply = (response.data && response.data.reply) || '';
                 if (reply) {
                     $scope.chat.messages.push({ role: 'assistant', text: reply });
                     $scope.chat.turns.push({ role: 'assistant', content: reply });
                 }
                 if (response.data && response.data.proposedYaml) {
                     proposedYaml = response.data.proposedYaml;
                     $scope.chat.proposalPending = true;
                     setTimeout(showDiff, 0); // defer until ng-if renders the diff container
                 }
                 scrollChatToBottom();
                 flushConversation();
             }, (response) => {
                 $scope.chat.busy = false;
                 // The turn never completed - drop its unanswered user turn so the transcript stays alternating.
                 $scope.chat.turns.pop();
                 let text = 'The AI assistant request failed. Please look at the console for more information.';
                 if (response.status === 412) {
                     text = (response.data && response.data.message) || 'The AI assistant is not configured. Set DIRIGIBLE_INTENT_AI_API_KEY.';
                 } else console.error(response);
                 $scope.chat.messages.push({ role: 'error', text: text });
                 scrollChatToBottom();
                 flushConversation();
             });
    };

    $scope.onChatKey = (event) => {
        // Enter sends; Shift+Enter inserts a newline.
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            $scope.sendChat();
        }
    };

    $scope.acceptProposal = () => {
        if (!$scope.chat.proposalPending || proposedYaml === null) return;
        if (monacoEditor) monacoEditor.setValue(proposedYaml); // fires onDidChangeModelContent -> $scope.text, dirty, re-parse
        else { $scope.text = proposedYaml; handleTextChanged(); }
        // A UI note, NOT an assistant turn: it is displayed and recorded, but the model never sees it -
        // which is exactly what the `note` role means to the restored transcript.
        $scope.chat.messages.push({ role: 'note', text: 'Applied to the editor. Review, then Save and Generate.' });
        $scope.rejectProposal();
        scrollChatToBottom();
        flushConversation();
    };

    $scope.rejectProposal = () => {
        proposedYaml = null;
        $scope.chat.proposalPending = false;
        disposeDiff();
    };

    // ----- Editor lifecycle wiring -----------------------------------------------

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

    $scope.$on('$destroy', () => {
        IntentDiagrams.dispose(diagramHost());
        disposeDiff();
        if (revealObserver) {
            revealObserver.disconnect();
            revealObserver = null;
        }
        if (monacoEditor) {
            monacoEditor.dispose();
            monacoEditor = null;
        }
    });

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'filePath\' data parameter is missing.';
        $scope.state.isBusy = false;
    } else loadFileContents();
});
