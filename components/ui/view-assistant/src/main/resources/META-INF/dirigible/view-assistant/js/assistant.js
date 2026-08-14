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
const assistantApp = angular.module('assistant', ['blimpKit', 'platformView']);
assistantApp.controller('AssistantController', ($scope, $http) => {
    const ASSIST_URL = '/services/ide/intent/assist';
    const CONVERSATIONS_URL = '/services/ide/intent/conversations';
    const WORKSPACE_API = '/services/ide/workspaces';
    const SURFACE = 'workbench';
    /** Asked of the open editor; answered on the reply topic below with its CURRENT (possibly dirty) buffer. */
    const CONTENT_REQUEST_TOPIC = 'editor.content.request';
    const CONTENT_REPLY_TOPIC = 'assistant.editor.content';
    /** How long to wait for an editor to answer before falling back to the file on disk. */
    const CONTENT_TIMEOUT_MS = 700;

    const messageHub = new MessageHubApi();
    const layoutHub = new LayoutHub();
    const themingHub = new ThemingHub();

    /** The file the assistant is helping with; null until an editor with one is focused. */
    $scope.target = null;
    /** Why the focused file cannot be helped with - shown instead of a silent nothing. */
    $scope.unsupported = null;
    /** `messages` is the display list (it holds notes and errors too); `turns` is the clean
     * user/assistant transcript sent upstream - the model API requires strictly alternating roles. */
    $scope.chat = { busy: false, input: '', messages: [], turns: [], proposalPending: false };
    $scope.diagnostics = [];

    let proposedSource = null;
    let currentSource = '';
    let editorWasDirty = false;
    let diffEditor = null;
    let monacoApi = null;
    /** The last editor path we were told about, supported or not - what Refresh re-evaluates. */
    let lastEditorPath = null;
    /** How many of chat.messages the server has accepted; everything past it is still unsaved. */
    let persistedMessages = 0;

    // ----- Monaco (the proposal diff) --------------------------------------------

    // The same resolution the Intent Editor and editor-monaco use, so a diff shown here reads like the
    // editor it will land in.
    const monacoThemeFor = (theme) => {
        if (!theme) theme = themingHub.getSavedTheme();
        if (theme && theme.type === 'light') return 'vs-light';
        const classic = theme && typeof theme.id === 'string' && theme.id.startsWith('classic');
        if (theme && theme.type === 'dark') return classic ? 'classic-dark' : 'blimpkit-dark';
        const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (!prefersDark) return 'vs-light';
        return classic ? 'classic-dark' : 'blimpkit-dark';
    };

    /** The platform's dark editor themes - editor-monaco defines these too; redefining is idempotent. */
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

    if (typeof require !== 'undefined') {
        require.config({ paths: { vs: '/webjars/monaco-editor/min/vs' } });
        require(['vs/editor/editor.main'], (monaco) => {
            monacoApi = monaco;
            defineMonacoThemes(monaco);
            themingHub.onThemeChange((theme) => monaco.editor.setTheme(monacoThemeFor(theme)));
        });
    }

    const disposeDiff = () => {
        if (!diffEditor) return;
        const model = diffEditor.getModel();
        if (model) {
            if (model.original) model.original.dispose();
            if (model.modified) model.modified.dispose();
        }
        diffEditor.dispose();
        diffEditor = null;
    };

    const showDiff = () => {
        if (!monacoApi) return;
        disposeDiff();
        const container = document.getElementById('as-diff');
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
            original: monacoApi.editor.createModel(currentSource || '', 'java'),
            modified: monacoApi.editor.createModel(proposedSource || '', 'java'),
        });
    };

    // ----- Which file are we helping with? ---------------------------------------
    // An editor path is `/<workspace>/<project>/<path within the project>`; the assistant needs all
    // three, so it parses rather than guesses. Two files are refused for the same reason the endpoint
    // refuses them: gen/ is regenerated (a proposal there could only be lost) and a non-Java file is
    // not this assistant's business. Saying so beats an input box that silently does nothing.

    const scrollToBottom = () => {
        setTimeout(() => {
            const list = document.getElementById('as-messages');
            if (list) list.scrollTop = list.scrollHeight;
        }, 0);
    };

    /** The three coordinates the endpoint needs, or a reason this file is not one we can help with. */
    const resolve = (filePath) => {
        const tokens = (filePath || '').split('/');
        if (tokens.length < 4) return null;
        const path = tokens.slice(3)
                           .join('/');
        if (!path.endsWith('.java')) {
            return { reason: 'The assistant helps with Java files; ' + path + ' is not one.' };
        }
        if (path.startsWith('gen/')) {
            return {
                reason: path + ' is generated from the model and is rewritten on the next Generate, so it cannot be edited here. '
                    + 'Change the model, or the custom/ class it points at.'
            };
        }
        return {
            workspace: tokens[1], project: tokens[2], path: path, filePath: filePath, label: tokens[2] + '/' + path
        };
    };

    const setTarget = (filePath) => {
        if (!filePath) return;
        lastEditorPath = filePath;
        const resolved = resolve(filePath);
        if (!resolved) return;
        if (resolved.reason) {
            $scope.target = null;
            $scope.unsupported = resolved.reason;
            return;
        }
        if ($scope.target && $scope.target.filePath === filePath) return;
        $scope.target = resolved;
        $scope.unsupported = null;
        $scope.chat.messages = [];
        $scope.chat.turns = [];
        $scope.chat.input = '';
        persistedMessages = 0;
        $scope.rejectProposal();
        restoreConversation();
    };

    layoutHub.onFocusEditor((data) => {
        $scope.$evalAsync(() => setTarget(data && data.path));
    });

    layoutHub.onOpenEditor((data) => {
        $scope.$evalAsync(() => setTarget(data && data.path));
    });

    /**
     * Ask the layout which editors are open and take one we can help with. The focus/open events above
     * carry the developer's intent, but they only fire while this view is alive - a view opened after
     * the file would otherwise sit empty until the developer clicked back into the editor. The last
     * editor we were told about wins when it is still open; the strip always names the file, so a pick
     * made for the developer is visible rather than assumed.
     */
    const probeOpenEditors = () => {
        layoutHub.getCurrentlyOpenedEditors()
                 .then((paths) => {
                     const open = Array.isArray(paths) ? paths : [];
                     const eligible = open.filter((p) => {
                         const resolved = resolve(p);
                         return resolved && !resolved.reason;
                     });
                     if (!eligible.length) return;
                     const preferred = eligible.indexOf(lastEditorPath) >= 0 ? lastEditorPath : eligible[0];
                     $scope.$evalAsync(() => setTarget(preferred));
                 });
    };

    $scope.retrack = () => {
        $scope.target = null;
        $scope.unsupported = null;
        probeOpenEditors();
    };

    probeOpenEditors();

    // ----- Conversation history ---------------------------------------------------
    // Persisted server-side per (project, surface, file), exactly like the Intent Editor's: the
    // dialogue about why a class looks the way it does outlives the tab, the browser and the machine.

    const conversationQuery = () => `?project=${encodeURIComponent($scope.target.project)}&surface=${SURFACE}`
        + `&path=${encodeURIComponent($scope.target.path)}`;

    const restoreConversation = () => {
        const at = $scope.target;
        $http.get(CONVERSATIONS_URL + conversationQuery())
             .then((response) => {
                 if ($scope.target !== at) return; // the developer moved on while this was in flight
                 const stored = response.data || {};
                 $scope.chat.messages = (stored.messages || []).map((m) => ({ role: m.role, text: m.content }));
                 // The transcript is DERIVED server-side from the stored roles rather than stored twice,
                 // so a failed turn's unanswered message is kept as a record without being replayed.
                 $scope.chat.turns = (stored.turns || []).map((t) => ({ role: t.role, content: t.content }));
                 persistedMessages = $scope.chat.messages.length;
                 scrollToBottom();
             }, (response) => {
                 // No history is a degraded pane, never a broken one.
                 console.error(response);
             });
    };

    /**
     * Append whatever this file's conversation has said but not yet saved. Called once per turn, after
     * it has fully resolved - a failed turn's error bubble is part of the record too. On failure the
     * count is deliberately left where it is, so the next turn re-sends this tail.
     */
    const flushConversation = () => {
        if (!$scope.target) return;
        const pending = $scope.chat.messages.slice(persistedMessages);
        if (!pending.length) return;
        const messages = pending.map((m) => ({ role: m.role, content: m.text }));
        $http.post(CONVERSATIONS_URL + '/messages' + conversationQuery(), { messages: messages })
             .then(() => {
                 persistedMessages += messages.length;
             }, (response) => {
                 console.error(response);
             });
    };

    // ----- The turn ---------------------------------------------------------------

    /**
     * The file as the developer currently sees it. The open editor owns the buffer - which may hold
     * unsaved edits - and lives in another iframe, so we ask it and fall back to the file on disk when
     * nothing answers (no editor open, or one that is not Monaco).
     */
    const loadSource = () => new Promise((resolve) => {
        const at = $scope.target;
        let settled = false;
        const finish = (result) => {
            if (settled) return;
            settled = true;
            messageHub.removeMessageListener(listener);
            resolve(result);
        };
        const listener = messageHub.addMessageListener({
            topic: CONTENT_REPLY_TOPIC,
            handler: (data) => {
                if (!data || data.path !== at.filePath) return;
                finish({ source: data.content || '', dirty: !!data.dirty });
            },
        });
        messageHub.postMessage({
            topic: CONTENT_REQUEST_TOPIC,
            data: { path: at.filePath, replyTopic: CONTENT_REPLY_TOPIC },
        });
        setTimeout(() => {
            if (settled) return;
            $http.get(WORKSPACE_API + at.filePath, { transformResponse: [(data) => data] })
                 .then((response) => finish({ source: response.data || '', dirty: false }),
                     () => finish({ source: '', dirty: false }));
        }, CONTENT_TIMEOUT_MS);
    });

    $scope.send = () => {
        const message = ($scope.chat.input || '').trim();
        if (!message || $scope.chat.busy || !$scope.target) return;
        $scope.rejectProposal();
        const at = $scope.target;
        const history = $scope.chat.turns.slice();
        $scope.chat.messages.push({ role: 'user', text: message });
        $scope.chat.turns.push({ role: 'user', content: message });
        $scope.chat.input = '';
        $scope.chat.busy = true;
        scrollToBottom();

        loadSource().then((loaded) => {
            currentSource = loaded.source;
            editorWasDirty = loaded.dirty;
            return $http.post(ASSIST_URL, {
                workspace: at.workspace,
                project: at.project,
                path: at.path,
                source: currentSource,
                message: message,
                history: history,
            });
        })
                    .then((response) => {
                        $scope.chat.busy = false;
                        const data = response.data || {};
                        if (data.reply) {
                            $scope.chat.messages.push({ role: 'assistant', text: data.reply });
                            $scope.chat.turns.push({ role: 'assistant', content: data.reply });
                        }
                        if (data.proposedSource) {
                            proposedSource = data.proposedSource;
                            $scope.diagnostics = data.diagnostics || [];
                            $scope.chat.proposalPending = true;
                            setTimeout(showDiff, 0); // defer until ng-if renders the diff container
                        }
                        scrollToBottom();
                        flushConversation();
                    }, (response) => {
                        $scope.chat.busy = false;
                        // The turn never completed - drop its unanswered user turn so the transcript stays alternating.
                        $scope.chat.turns.pop();
                        let text = 'The AI assistant request failed. Please look at the console for more information.';
                        if (response.status === 412) {
                            text = 'The AI assistant is not configured. Set DIRIGIBLE_INTENT_AI_API_KEY.';
                        } else console.error(response);
                        $scope.chat.messages.push({ role: 'error', text: text });
                        scrollToBottom();
                        flushConversation();
                    });
    };

    $scope.onChatKey = (event) => {
        // Enter sends; Shift+Enter inserts a newline.
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            $scope.send();
        }
    };

    /**
     * Write the proposal to the file and ask the open editor to reload it. A dirty editor deliberately
     * refuses to reload (unsaved work is never clobbered), so in that case the developer is told the
     * file changed under an editor that is still showing their own edits - the one thing that would
     * otherwise be silently confusing.
     */
    $scope.acceptProposal = () => {
        if (!$scope.chat.proposalPending || proposedSource === null || !$scope.target) return;
        const at = $scope.target;
        const source = proposedSource;
        $http.put(WORKSPACE_API + at.filePath, source, {
            headers: { 'Content-Type': 'text/plain', 'Dirigible-Editor': 'Assistant' },
            transformRequest: [(data) => data],
        })
             .then(() => {
                 messageHub.postMessage({ topic: 'monaco.file.reload', data: { path: at.filePath } });
                 currentSource = source;
                 // A UI note, NOT an assistant turn: it is displayed and recorded, but the model never
                 // sees it - which is exactly what the `note` role means to the restored transcript.
                 $scope.chat.messages.push({
                     role: 'note',
                     text: editorWasDirty
                         ? 'Written to ' + at.path + '. The open editor had unsaved changes, so it kept them and did not reload - '
                         + 'close and reopen it to see the new content.'
                         : 'Written to ' + at.path + ' and the open editor reloaded.',
                 });
                 $scope.rejectProposal();
                 scrollToBottom();
                 flushConversation();
             }, (response) => {
                 console.error(response);
                 $scope.chat.messages.push({ role: 'error', text: 'Could not write ' + at.path + '. Nothing was changed.' });
                 scrollToBottom();
                 flushConversation();
             });
    };

    $scope.rejectProposal = () => {
        proposedSource = null;
        $scope.diagnostics = [];
        $scope.chat.proposalPending = false;
        disposeDiff();
    };

    $scope.$on('$destroy', disposeDiff);
});
