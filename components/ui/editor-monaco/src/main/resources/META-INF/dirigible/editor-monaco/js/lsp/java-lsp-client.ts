/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */

/**
 * Java LSP client bundle.
 *
 * Uses vscode-ws-jsonrpc for typed JSON-RPC over WebSocket and registers Monaco
 * providers that delegate to JDT.LS. Exposed as global JavaLspClientLib so that
 * editor.js can call JavaLspClientLib.connect(resourcePath).
 *
 * One JDT.LS process covers the entire workspace, so a single WebSocket connection
 * is shared across all Java files open in the same browser page. The connection is
 * established on the first connect() call and reused for all subsequent calls.
 *
 * editor.js sets window.monaco before calling connect(), so the monaco-shim's lazy
 * Proxies resolve correctly at call time.
 */

import { createMessageConnection, MessageConnection } from 'vscode-jsonrpc/browser';
import { toSocket, WebSocketMessageReader, WebSocketMessageWriter } from 'vscode-ws-jsonrpc';
import * as monaco from 'monaco-editor';
import {
    CompletionItemKind,
    DiagnosticSeverity,
    InsertTextFormat,
    MarkupContent,
    MarkupKind,
    type CodeAction,
    type Command,
    type CompletionItem,
    type CompletionList,
    type Diagnostic,
    type Hover,
    type Location,
    type ParameterInformation,
    type SignatureHelp,
    type SignatureInformation,
    type TextEdit,
    type WorkspaceEdit,
} from 'vscode-languageserver-types';

// -------------------------------------------------------------------------
// Singleton state
// -------------------------------------------------------------------------

/** Shared connection — one per browser page, covering all projects in the workspace. */
let _conn: MessageConnection | null = null;

/**
 * Virtual workspace root URI, e.g. {@code file:///workspace/workspace/}.
 * Set once on first connect(); used to scope Monaco providers to workspace files.
 */
let _workspaceRoot = '';

/** URIs for which textDocument/didOpen has already been sent. */
const _openFiles: Set<string> = new Set();

/** Pending debounced didChange timers per file URI, so a change can be flushed before completion. */
const _changeTimers: Map<string, ReturnType<typeof setTimeout>> = new Map();

/**
 * Last diagnostics published per file URI, kept verbatim (with their LSP code/data). Code-action
 * requests must send these — JDT.LS matches quick-fixes by the diagnostic's code/data, which Monaco's
 * IMarkerData cannot round-trip, so reconstructing diagnostics from markers yields no quick-fixes.
 */
const _diagnostics: Map<string, Diagnostic[]> = new Map();

let _providersRegistered = false;

/** Semantic-token legend reported by JDT.LS in the initialize result; needed to decode token data. */
let _semanticTokensLegend: { tokenTypes: string[]; tokenModifiers: string[] } | null = null;

// -------------------------------------------------------------------------
// Public API
// -------------------------------------------------------------------------

/** Called by editor.js when a Java file is opened. Safe to call multiple times. */
export async function connect(resourcePath: string): Promise<void> {
    const parts = resourcePath.replace(/^\//, '').split('/');
    const workspace = parts[0];
    const project   = parts[1];
    const fileUri = `file:///workspace/${workspace}/${project}/${parts.slice(2).join('/')}`;

    if (_conn) {
        // Connection already established — just open the new file.
        openFile(fileUri);
        return;
    }

    _workspaceRoot = `file:///workspace/${workspace}/`;

    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    const wsUrl = `${proto}://${location.host}/websockets/ide/java-lsp`
                + `?workspace=${encodeURIComponent(workspace)}`;

    const ws = new WebSocket(wsUrl);
    await new Promise<void>((resolve, reject) => {
        ws.onopen  = () => resolve();
        ws.onerror = () => reject(new Error(`[java-lsp] WebSocket connect failed: ${wsUrl}`));
    });

    const socket = toSocket(ws);
    const reader = new WebSocketMessageReader(socket);
    const writer = new WebSocketMessageWriter(socket);
    _conn = createMessageConnection(reader, writer);

    // Diagnostics notification → Monaco markers (applies to any workspace file)
    _conn.onNotification('textDocument/publishDiagnostics', (params: { uri: string; diagnostics: Diagnostic[] }) => {
        _diagnostics.set(params.uri, params.diagnostics ?? []);
        const model = monaco.editor.getModels().find(m => m.uri.toString() === params.uri);
        if (!model) return;
        monaco.editor.setModelMarkers(model, 'java-lsp', params.diagnostics.map(d => ({
            severity:        lspSeverity(d.severity),
            message:         d.message,
            source:          d.source ?? 'java',
            startLineNumber: d.range.start.line + 1,
            startColumn:     d.range.start.character + 1,
            endLineNumber:   d.range.end.line + 1,
            endColumn:       d.range.end.character + 1,
        })));
    });

    // Server -> client requests that drive refactor / generate results and dynamic registration.
    _conn.onRequest('workspace/applyEdit', (params: { edit: WorkspaceEdit }) => {
        applyWorkspaceEdit(params.edit);
        return { applied: true };
    });
    _conn.onRequest('workspace/configuration', (params: { items: Array<{ section?: string }> }) =>
        (params.items ?? []).map(() => jdtlsSettings().java));
    _conn.onRequest('client/registerCapability', () => null);
    _conn.onRequest('client/unregisterCapability', () => null);
    _conn.onRequest('window/showMessageRequest', () => null);
    _conn.onRequest('window/workDoneProgress/create', () => null);
    _conn.onNotification('window/logMessage', (p: { message: string }) => console.debug('[java-lsp]', p?.message));
    _conn.onNotification('window/showMessage', (p: { message: string }) => console.info('[java-lsp]', p?.message));
    // JDT.LS language-status / progress notifications — acknowledged silently.
    _conn.onNotification('language/status', () => { /* indexing/ready status, ignored */ });
    _conn.onNotification('language/progressReport', () => { /* build progress, ignored */ });

    _conn.listen();

    const rootUri = _workspaceRoot;
    const initResult: any = await _conn.sendRequest('initialize', {
        processId: null,
        rootUri,
        initializationOptions: {
            settings: jdtlsSettings(),
            extendedClientCapabilities: {
                progressReportProvider:            false,
                classFileContentsSupport:          true,
                resolveAdditionalTextEditsSupport: true,
                // Do NOT advertise the *PromptSupport flags: those make JDT.LS return source actions
                // (generate toString/constructors/accessors, override/implement, organize imports) as
                // client-side "*Prompt" commands the vscode-java extension implements but we don't.
                // With them off, JDT.LS returns the same actions as resolvable WorkspaceEdits operating
                // on all members, which applyCodeAction resolves and applies directly.
                inferSelectionSupport:             ['extractMethod', 'extractVariable', 'extractField'],
            },
        },
        workspaceFolders: [{ uri: rootUri, name: workspace }],
        capabilities: {
            textDocument: {
                synchronization: { dynamicRegistration: true, willSave: false, didSave: true, willSaveWaitUntil: false },
                completion: {
                    dynamicRegistration: true,
                    completionItem: {
                        snippetSupport:        true,
                        documentationFormat:   ['markdown', 'plaintext'],
                        deprecatedSupport:     true,
                        commitCharactersSupport: true,
                        resolveSupport:        { properties: ['documentation', 'detail', 'additionalTextEdits'] },
                    },
                    contextSupport: true,
                },
                hover:          { dynamicRegistration: true, contentFormat: ['markdown', 'plaintext'] },
                signatureHelp:  { dynamicRegistration: true, signatureInformation: { documentationFormat: ['markdown', 'plaintext'], parameterInformation: { labelOffsetSupport: true } } },
                definition:     { dynamicRegistration: true },
                references:     { dynamicRegistration: true },
                implementation: { dynamicRegistration: true },
                typeDefinition: { dynamicRegistration: true },
                // Advertised so JDT.LS serves call/type hierarchy. The editor doesn't drive them
                // directly; the server-side REST facade (JavaLspQueryEndpoint) does — but JDT.LS only
                // exposes these providers when the *last* initialize advertised them, and a browser
                // editor re-initializes the shared process, so the editor must advertise them too.
                callHierarchy:  { dynamicRegistration: true },
                typeHierarchy:  { dynamicRegistration: true },
                documentHighlight: { dynamicRegistration: true },
                documentSymbol: { dynamicRegistration: true, hierarchicalDocumentSymbolSupport: true },
                foldingRange:   { dynamicRegistration: true, lineFoldingOnly: false },
                selectionRange: { dynamicRegistration: true },
                codeLens:       { dynamicRegistration: true },
                inlayHint:      { dynamicRegistration: true, resolveSupport: { properties: ['label'] } },
                semanticTokens: {
                    dynamicRegistration: true,
                    requests:        { range: false, full: { delta: false } },
                    tokenTypes:      ['namespace', 'type', 'class', 'enum', 'interface', 'struct', 'typeParameter',
                        'parameter', 'variable', 'property', 'enumMember', 'event', 'function', 'method', 'macro',
                        'keyword', 'modifier', 'comment', 'string', 'number', 'regexp', 'operator', 'decorator'],
                    tokenModifiers:  ['declaration', 'definition', 'readonly', 'static', 'deprecated', 'abstract',
                        'async', 'modification', 'documentation', 'defaultLibrary'],
                    formats:         ['relative'],
                    overlappingTokenSupport: false,
                    multilineTokenSupport:   false,
                },
                formatting:     { dynamicRegistration: true },
                rangeFormatting: { dynamicRegistration: true },
                rename:         { dynamicRegistration: true, prepareSupport: true },
                codeAction: {
                    dynamicRegistration: true,
                    codeActionLiteralSupport: {
                        codeActionKind: {
                            valueSet: ['quickfix', 'refactor', 'refactor.extract', 'refactor.inline',
                                'refactor.rewrite', 'source', 'source.organizeImports'],
                        },
                    },
                    isPreferredSupport: true,
                    dataSupport:        true,
                    resolveSupport:     { properties: ['edit'] },
                },
                publishDiagnostics: { relatedInformation: true },
            },
            workspace: {
                applyEdit:              true,
                configuration:          true,
                executeCommand:         { dynamicRegistration: true },
                didChangeConfiguration: { dynamicRegistration: true },
                workspaceEdit:          { documentChanges: true, resourceOperations: ['create', 'rename', 'delete'] },
            },
        },
    });

    _semanticTokensLegend = initResult?.capabilities?.semanticTokensProvider?.legend ?? null;

    _conn.sendNotification('initialized', {});
    _conn.sendNotification('workspace/didChangeConfiguration', { settings: jdtlsSettings() });

    openFile(fileUri);

    if (!_providersRegistered) {
        _providersRegistered = true;
        registerProviders();
    }
}

// -------------------------------------------------------------------------
// File lifecycle
// -------------------------------------------------------------------------

/**
 * Sends textDocument/didOpen for the given URI (if not already sent) and registers a debounced
 * textDocument/didChange listener on the corresponding Monaco model.
 */
function openFile(fileUri: string): void {
    if (_openFiles.has(fileUri) || !_conn) return;
    _openFiles.add(fileUri);

    const model = monaco.editor.getModels().find(m => m.uri.toString() === fileUri);
    _conn.sendNotification('textDocument/didOpen', {
        textDocument: {
            uri:        fileUri,
            languageId: 'java',
            version:    1,
            text:       model?.getValue() ?? '',
        },
    });
    // Tell JDT.LS the file exists on disk so its project model includes it even before a build — this
    // makes a just-created type (e.g. a new interface) resolvable/offered in sibling files immediately.
    _conn.sendNotification('workspace/didChangeWatchedFiles', { changes: [{ uri: fileUri, type: 1 /* Created */ }] });

    if (model) {
        model.onDidChangeContent(() => {
            const existing = _changeTimers.get(fileUri);
            if (existing) clearTimeout(existing);
            _changeTimers.set(fileUri, setTimeout(() => sendDidChange(fileUri), 400));
        });
    }
}

/** Sends the current model content as a didChange and clears any pending debounce for the file. */
function sendDidChange(fileUri: string): void {
    _changeTimers.delete(fileUri);
    const model = monaco.editor.getModel(monaco.Uri.parse(fileUri));
    if (_conn && model) {
        _conn.sendNotification('textDocument/didChange', {
            textDocument:   { uri: fileUri, version: model.getVersionId() },
            contentChanges: [{ text: model.getValue() }],
        });
    }
}

/**
 * Flushes a pending debounced change immediately. Called before a completion request so JDT.LS sees the
 * just-typed text on the first Ctrl+Space, instead of completing against stale content (the debounce
 * otherwise delays the change by up to 400ms — the cause of "first Ctrl+Space shows nothing").
 */
function flushPendingChange(fileUri: string): void {
    if (_changeTimers.has(fileUri)) {
        clearTimeout(_changeTimers.get(fileUri)!);
        sendDidChange(fileUri);
    }
}

// -------------------------------------------------------------------------
// Workspace file predicate
// -------------------------------------------------------------------------

/**
 * Returns {@code true} when the given model URI belongs to the currently connected workspace. Used
 * by Monaco providers to skip non-Java-workspace models without an exact-URI comparison.
 */
function isWorkspaceFile(uri: string): boolean {
    return _workspaceRoot !== '' && uri.startsWith(_workspaceRoot);
}

// -------------------------------------------------------------------------
// Monaco provider registration
// -------------------------------------------------------------------------

function registerProviders(): void {

    monaco.editor.registerCommand(APPLY_ACTION_COMMAND, (_accessor: unknown, action: CodeAction | Command) => {
        applyCodeAction(action);
    });
    monaco.editor.registerCommand(NOOP_COMMAND, () => { /* display-only CodeLens */ });

    // Cross-file navigation: this single-file editor has no model for other workspace files, so Go to
    // Definition / Find References to another file would silently do nothing. Hand those off to the IDE
    // to open the target file (and reveal the line). Same-file targets fall through to Monaco.
    monaco.editor.registerEditorOpener({
        openCodeEditor: (source, resource, selectionOrPosition) => {
            const uri = resource.toString();
            if (!isWorkspaceFile(uri) || !uri.startsWith(VIRTUAL_FILE_PREFIX)) return false;
            const currentModel = source.getModel();
            if (currentModel && currentModel.uri.toString() === uri) return false; // same file — let Monaco jump
            const opener = (globalThis as any).javaLspOpenFile;
            if (typeof opener !== 'function') return false;
            const pos = selectionOrPosition as { startLineNumber?: number; lineNumber?: number; startColumn?: number; column?: number } | undefined;
            const line = pos ? (pos.startLineNumber ?? pos.lineNumber) : undefined;
            const column = pos ? (pos.startColumn ?? pos.column) : undefined;
            opener(uri.substring(VIRTUAL_FILE_PREFIX.length), line, column);
            return true;
        },
    });

    monaco.languages.registerCompletionItemProvider('java', {
        triggerCharacters: ['.', '@', '<'],
        provideCompletionItems: async (model, position, context) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const fileUri = model.uri.toString();
            // Make sure JDT.LS has the just-typed text before completing (see flushPendingChange).
            flushPendingChange(fileUri);
            const result: CompletionList | CompletionItem[] | null = await _conn.sendRequest('textDocument/completion', {
                textDocument: { uri: fileUri },
                position:     { line: position.lineNumber - 1, character: position.column - 1 },
                // Monaco trigger kinds are 0-based (Invoke/TriggerCharacter/ForIncomplete); LSP is 1-based.
                context:      { triggerKind: (context.triggerKind ?? 0) + 1, triggerCharacter: context.triggerCharacter },
            });
            const items = Array.isArray(result) ? result : (result?.items ?? []);
            return {
                suggestions: items.map(item => lspCompletionToMonaco(item, model, position)),
                // JDT.LS returns a truncated list on the first keystrokes; propagating "incomplete" makes
                // Monaco re-query as the user types instead of caching the first (often empty) result.
                incomplete:  Array.isArray(result) ? false : !!result?.isIncomplete,
            };
        },
        // Resolve documentation and, crucially, the auto-import additionalTextEdits which JDT.LS only
        // attaches on resolve — selecting a type then inserts its import statement.
        resolveCompletionItem: async (item) => {
            const lsp = (item as MonacoCompletionItem)._lsp;
            if (!_conn || !lsp) return item;
            try {
                const resolved: CompletionItem = await _conn.sendRequest('completionItem/resolve', lsp);
                if (resolved.documentation) {
                    item.documentation = { value: markupToString(resolved.documentation), isTrusted: false };
                }
                if (resolved.detail) item.detail = resolved.detail;
                if (resolved.additionalTextEdits?.length) {
                    item.additionalTextEdits = resolved.additionalTextEdits.map(textEditToMonaco);
                }
            } catch (e) {
                console.debug('[java-lsp] completion resolve failed:', (e as Error)?.message);
            }
            return item;
        },
    });

    monaco.languages.registerHoverProvider('java', {
        provideHover: async (model, position) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const fileUri = model.uri.toString();
            const result: Hover | null = await _conn.sendRequest('textDocument/hover', {
                textDocument: { uri: fileUri },
                position:     { line: position.lineNumber - 1, character: position.column - 1 },
            });
            if (!result?.contents) return null;
            const contents = Array.isArray(result.contents) ? result.contents : [result.contents];
            return {
                contents: contents.map(c => ({
                    value: typeof c === 'string' ? c : (c as MarkupContent).value,
                    isTrusted: false,
                })),
                range: result.range ? lspRangeToMonaco(result.range) : undefined,
            };
        },
    });

    monaco.languages.registerSignatureHelpProvider('java', {
        signatureHelpTriggerCharacters: ['(', ','],
        provideSignatureHelp: async (model, position) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const fileUri = model.uri.toString();
            const result: SignatureHelp | null = await _conn.sendRequest('textDocument/signatureHelp', {
                textDocument: { uri: fileUri },
                position:     { line: position.lineNumber - 1, character: position.column - 1 },
            });
            if (!result) return null;
            return {
                value: {
                    signatures: result.signatures.map((sig: SignatureInformation) => ({
                        label:         sig.label,
                        documentation: sig.documentation ? markupToString(sig.documentation) : undefined,
                        parameters:    (sig.parameters ?? []).map((p: ParameterInformation) => ({
                            label:         p.label,
                            documentation: p.documentation ? markupToString(p.documentation) : undefined,
                        })),
                    })),
                    activeSignature: result.activeSignature ?? 0,
                    activeParameter: result.activeParameter ?? 0,
                },
                dispose: () => {},
            };
        },
    });

    monaco.languages.registerDefinitionProvider('java', {
        provideDefinition: async (model, position) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const fileUri = model.uri.toString();
            const result: Location | Location[] | null = await _conn.sendRequest('textDocument/definition', {
                textDocument: { uri: fileUri },
                position:     { line: position.lineNumber - 1, character: position.column - 1 },
            });
            if (!result) return null;
            const locations = (Array.isArray(result) ? result : [result]).map(loc => ({
                uri:   monaco.Uri.parse(loc.uri),
                range: lspRangeToMonaco(loc.range),
            }));
            await ensureModelsForLocations(locations);
            return locations;
        },
    });

    monaco.languages.registerReferenceProvider('java', {
        provideReferences: async (model, position, context) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const result: Location[] | null = await _conn.sendRequest('textDocument/references', {
                textDocument: { uri: model.uri.toString() },
                position:     { line: position.lineNumber - 1, character: position.column - 1 },
                context:      { includeDeclaration: context.includeDeclaration },
            });
            if (!result) return null;
            const locations = result.map(loc => ({ uri: monaco.Uri.parse(loc.uri), range: lspRangeToMonaco(loc.range) }));
            // The references peek can only show a code preview for files it has a model for; create
            // in-memory models for the referenced (possibly unopened) files so previews render.
            await ensureModelsForLocations(locations);
            return locations;
        },
    });

    monaco.languages.registerRenameProvider('java', {
        provideRenameEdits: async (model, position, newName) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return { edits: [] };
            const edit: WorkspaceEdit | null = await _conn.sendRequest('textDocument/rename', {
                textDocument: { uri: model.uri.toString() },
                position:     { line: position.lineNumber - 1, character: position.column - 1 },
                newName,
            });
            if (!edit) return { edits: [] };
            // A JDT.LS rename can span many files and even rename the type's own .java file. Monaco's
            // single-file editor would drop everything but the current model, so apply the whole edit
            // through the workspace ourselves when the IDE persistence hook is available.
            if (typeof (globalThis as any).javaLspPersistRename === 'function') {
                try {
                    await applyRenameAcrossWorkspace(model, edit);
                    return { edits: [] };
                } catch (e) {
                    console.error('[java-lsp] cross-file rename failed, applying to the current file only:', e);
                    return workspaceEditToMonaco(edit);
                }
            }
            return workspaceEditToMonaco(edit);
        },
        resolveRenameLocation: async (model, position) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            try {
                const result: { range: LspRange; placeholder?: string } | LspRange | null =
                    await _conn.sendRequest('textDocument/prepareRename', {
                        textDocument: { uri: model.uri.toString() },
                        position:     { line: position.lineNumber - 1, character: position.column - 1 },
                    });
                if (!result) return null;
                const range = 'range' in result ? result.range : result;
                const placeholder = 'placeholder' in result && result.placeholder
                    ? result.placeholder
                    : model.getWordAtPosition(position)?.word ?? '';
                return { range: lspRangeToMonaco(range), text: placeholder };
            } catch {
                const word = model.getWordAtPosition(position);
                return word ? {
                    range: { startLineNumber: position.lineNumber, startColumn: word.startColumn, endLineNumber: position.lineNumber, endColumn: word.endColumn },
                    text: word.word,
                } : null;
            }
        },
    });

    // Registering this provider also enables editor.js's existing format-on-save path for Java: the
    // shared Save action runs editor.action.formatDocument when auto-formatting is on (the same
    // mechanism and global toggle used for TypeScript).
    monaco.languages.registerDocumentFormattingEditProvider('java', {
        provideDocumentFormattingEdits: async (model) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const edits: TextEdit[] | null = await _conn.sendRequest('textDocument/formatting', {
                textDocument: { uri: model.uri.toString() },
                options:      { tabSize: model.getOptions().tabSize, insertSpaces: model.getOptions().insertSpaces },
            });
            return edits ? edits.map(textEditToMonaco) : null;
        },
    });

    monaco.languages.registerCodeActionProvider('java', {
        provideCodeActions: async (model, range, context) => {
            const empty = { actions: [], dispose() { /* nothing to dispose */ } };
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return empty;
            // Send the original LSP diagnostics that overlap the range (they carry the code/data JDT.LS
            // needs to compute quick-fixes), not ones reconstructed from Monaco markers.
            const lspRange = monacoRangeToLsp(range);
            const diagnostics = (_diagnostics.get(model.uri.toString()) ?? []).filter(d => rangesOverlap(d.range, lspRange));
            const result: Array<CodeAction | Command> | null = await _conn.sendRequest('textDocument/codeAction', {
                textDocument: { uri: model.uri.toString() },
                range:        lspRange,
                // Monaco's CodeActionTriggerType (Invoke=1, Auto=2) maps 1:1 to the LSP trigger kind.
                // Forwarding it lets JDT.LS compute only quick-fixes for the passive lightbulb (cheap)
                // and the full assists/refactorings only on explicit Ctrl+. / Refactor… (Invoked).
                context:      { diagnostics, only: context.only ? [context.only] : undefined, triggerKind: context.trigger },
            });
            if (!result?.length) return empty;
            return {
                actions: result.map(lspCodeActionToMonaco),
                dispose() { /* nothing to dispose */ },
            };
        },
    }, {
        providedCodeActionKinds: ['quickfix', 'refactor', 'refactor.extract', 'refactor.inline',
            'refactor.rewrite', 'source', 'source.organizeImports'],
    });

    // --- Navigation & structure (Pack 1) ---

    monaco.languages.registerImplementationProvider('java', {
        provideImplementation: (model, position) => requestLocations('textDocument/implementation', model, position),
    });

    monaco.languages.registerTypeDefinitionProvider('java', {
        provideTypeDefinition: (model, position) => requestLocations('textDocument/typeDefinition', model, position),
    });

    monaco.languages.registerDocumentHighlightProvider('java', {
        provideDocumentHighlights: async (model, position) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const result: any[] | null = await _conn.sendRequest('textDocument/documentHighlight', {
                textDocument: { uri: model.uri.toString() },
                position:     { line: position.lineNumber - 1, character: position.column - 1 },
            });
            if (!result) return null;
            return result.map(h => ({ range: lspRangeToMonaco(h.range), kind: h.kind ? h.kind - 1 : undefined }));
        },
    });

    monaco.languages.registerDocumentSymbolProvider('java', {
        provideDocumentSymbols: async (model) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const result: any[] | null = await _conn.sendRequest('textDocument/documentSymbol', {
                textDocument: { uri: model.uri.toString() },
            });
            return result ? mapDocumentSymbols(result) : null;
        },
    });

    monaco.languages.registerFoldingRangeProvider('java', {
        provideFoldingRanges: async (model) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const result: any[] | null = await _conn.sendRequest('textDocument/foldingRange', {
                textDocument: { uri: model.uri.toString() },
            });
            if (!result) return null;
            return result.map(r => ({ start: r.startLine + 1, end: r.endLine + 1, kind: foldingKind(r.kind) }));
        },
    });

    monaco.languages.registerSelectionRangeProvider('java', {
        provideSelectionRanges: async (model, positions) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const result: any[] | null = await _conn.sendRequest('textDocument/selectionRange', {
                textDocument: { uri: model.uri.toString() },
                positions:    positions.map(p => ({ line: p.lineNumber - 1, character: p.column - 1 })),
            });
            if (!result) return null;
            return result.map(flattenSelectionRange);
        },
    });

    monaco.languages.registerDocumentRangeFormattingEditProvider('java', {
        provideDocumentRangeFormattingEdits: async (model, range) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const edits: TextEdit[] | null = await _conn.sendRequest('textDocument/rangeFormatting', {
                textDocument: { uri: model.uri.toString() },
                range:        monacoRangeToLsp(range),
                options:      { tabSize: model.getOptions().tabSize, insertSpaces: model.getOptions().insertSpaces },
            });
            return edits ? edits.map(textEditToMonaco) : null;
        },
    });

    // --- Inlay hints + semantic highlighting (Pack 2) ---

    monaco.languages.registerInlayHintsProvider('java', {
        provideInlayHints: async (model, range) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
            const result: any[] | null = await _conn.sendRequest('textDocument/inlayHint', {
                textDocument: { uri: model.uri.toString() },
                range:        monacoRangeToLsp(range),
            });
            if (!result) return null;
            return {
                hints: result.map(h => ({
                    position:     { lineNumber: h.position.line + 1, column: h.position.character + 1 },
                    label:        typeof h.label === 'string' ? h.label : (h.label ?? []).map((p: any) => ({ label: p.value })),
                    kind:         h.kind,
                    paddingLeft:  h.paddingLeft,
                    paddingRight: h.paddingRight,
                    tooltip:      h.tooltip ? markupToString(h.tooltip) : undefined,
                })),
                dispose() { /* nothing to dispose */ },
            };
        },
    });

    monaco.languages.registerDocumentSemanticTokensProvider('java', {
        getLegend: () => _semanticTokensLegend ?? { tokenTypes: [], tokenModifiers: [] },
        provideDocumentSemanticTokens: async (model) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString()) || !_semanticTokensLegend) return null;
            const result: { data: number[]; resultId?: string } | null = await _conn.sendRequest('textDocument/semanticTokens/full', {
                textDocument: { uri: model.uri.toString() },
            });
            if (!result?.data) return null;
            return { data: new Uint32Array(result.data), resultId: result.resultId };
        },
        releaseDocumentSemanticTokens: () => { /* nothing to release */ },
    });

    // --- CodeLens (Pack 3) ---

    monaco.languages.registerCodeLensProvider('java', {
        provideCodeLenses: async (model) => {
            if (!_conn || !isWorkspaceFile(model.uri.toString())) return { lenses: [], dispose() { /* */ } };
            const result: any[] | null = await _conn.sendRequest('textDocument/codeLens', {
                textDocument: { uri: model.uri.toString() },
            });
            const lenses = (result ?? []).map((lens, i) => ({
                range:    lspRangeToMonaco(lens.range),
                id:       String(i),
                command:  lens.command ? mapLensCommand(lens.command) : undefined,
                _lsp:     lens,
            }));
            return { lenses, dispose() { /* */ } };
        },
        resolveCodeLens: async (_model, codeLens) => {
            const lsp = (codeLens as any)._lsp;
            if (_conn && lsp && !lsp.command) {
                try {
                    const resolved: any = await _conn.sendRequest('codeLens/resolve', lsp);
                    codeLens.command = resolved?.command ? mapLensCommand(resolved.command) : { id: NOOP_COMMAND, title: '' };
                } catch {
                    codeLens.command = { id: NOOP_COMMAND, title: '' };
                }
            }
            return codeLens;
        },
    });

    // --- Java keyword completion (Pack 1b): always-available, ranked below SDK/LSP results ---

    monaco.languages.registerCompletionItemProvider('java', {
        provideCompletionItems: (model, position) => {
            if (!isWorkspaceFile(model.uri.toString())) return null;
            const word = model.getWordUntilPosition(position);
            const range: monaco.IRange = {
                startLineNumber: position.lineNumber, startColumn: word.startColumn,
                endLineNumber:   position.lineNumber, endColumn:   word.endColumn,
            };
            return {
                suggestions: JAVA_KEYWORDS.map(keyword => ({
                    label:      keyword,
                    kind:       monaco.languages.CompletionItemKind.Keyword,
                    insertText: keyword,
                    range,
                    sortText:   `9_${keyword}`,
                })),
            };
        },
    });
}

// -------------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------------

function lspSeverity(severity: DiagnosticSeverity | undefined): monaco.MarkerSeverity {
    switch (severity) {
        case DiagnosticSeverity.Error:       return monaco.MarkerSeverity.Error;
        case DiagnosticSeverity.Warning:     return monaco.MarkerSeverity.Warning;
        case DiagnosticSeverity.Information: return monaco.MarkerSeverity.Info;
        case DiagnosticSeverity.Hint:        return monaco.MarkerSeverity.Hint;
        default:                             return monaco.MarkerSeverity.Error;
    }
}

function lspRangeToMonaco(r: { start: { line: number; character: number }; end: { line: number; character: number } }): monaco.IRange {
    return {
        startLineNumber: r.start.line + 1,
        startColumn:     r.start.character + 1,
        endLineNumber:   r.end.line + 1,
        endColumn:       r.end.character + 1,
    };
}

function markupToString(c: string | MarkupContent): string {
    return typeof c === 'string' ? c : c.value;
}

function lspCompletionKind(kind: CompletionItemKind | undefined): monaco.languages.CompletionItemKind {
    const K = monaco.languages.CompletionItemKind;
    switch (kind) {
        case CompletionItemKind.Text:          return K.Text;
        case CompletionItemKind.Method:        return K.Method;
        case CompletionItemKind.Function:      return K.Function;
        case CompletionItemKind.Constructor:   return K.Constructor;
        case CompletionItemKind.Field:         return K.Field;
        case CompletionItemKind.Variable:      return K.Variable;
        case CompletionItemKind.Class:         return K.Class;
        case CompletionItemKind.Interface:     return K.Interface;
        case CompletionItemKind.Module:        return K.Module;
        case CompletionItemKind.Property:      return K.Property;
        case CompletionItemKind.Keyword:       return K.Keyword;
        case CompletionItemKind.Snippet:       return K.Snippet;
        case CompletionItemKind.Constant:      return K.Constant;
        case CompletionItemKind.Struct:        return K.Struct;
        case CompletionItemKind.TypeParameter: return K.TypeParameter;
        default:                               return K.Text;
    }
}

function lspCompletionToMonaco(
    item: CompletionItem,
    model: monaco.editor.ITextModel,
    position: monaco.Position,
): MonacoCompletionItem {
    const word = model.getWordUntilPosition(position);
    let range: monaco.IRange = {
        startLineNumber: position.lineNumber,
        startColumn:     word.startColumn,
        endLineNumber:   position.lineNumber,
        endColumn:       word.endColumn,
    };
    let insertText = item.insertText ?? item.label;
    const textEdit = item.textEdit as { range?: LspRange; replace?: LspRange; insert?: LspRange; newText?: string } | undefined;
    if (textEdit) {
        const r = textEdit.range ?? textEdit.replace ?? textEdit.insert;
        if (r) range = lspRangeToMonaco(r);
        if (typeof textEdit.newText === 'string') insertText = textEdit.newText;
    }
    const result: MonacoCompletionItem = {
        label:           item.label,
        kind:            lspCompletionKind(item.kind),
        detail:          item.detail,
        documentation:   item.documentation ? { value: markupToString(item.documentation), isTrusted: false } : undefined,
        insertText,
        insertTextRules: item.insertTextFormat === InsertTextFormat.Snippet
                             ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
                             : undefined,
        range,
        sortText:            sdkPrioritisedSortText(item),
        filterText:          item.filterText,
        preselect:           item.preselect,
        commitCharacters:    item.commitCharacters,
        additionalTextEdits: item.additionalTextEdits?.map(textEditToMonaco),
    };
    result._lsp = item;
    return result;
}

/**
 * Ranks Dirigible SDK suggestions ({@code org.eclipse.dirigible.sdk.*}) above everything else by
 * prefixing the server sortText with a priority bucket, preserving the server order within each bucket.
 */
function sdkPrioritisedSortText(item: CompletionItem): string {
    const base = item.sortText ?? (typeof item.label === 'string' ? item.label : '');
    const description = (item.labelDetails && typeof item.labelDetails.description === 'string')
        ? item.labelDetails.description : '';
    const haystack = `${item.detail ?? ''} ${description}`;
    return haystack.includes('org.eclipse.dirigible.sdk') ? `0_${base}` : `1_${base}`;
}

// -------------------------------------------------------------------------
// Code actions, commands, refactor/generate, workspace edits
// -------------------------------------------------------------------------

/** Monaco command id used to apply a deferred LSP code action when the user selects it. */
const APPLY_ACTION_COMMAND = 'dirigible.java.applyCodeAction';

/** Virtual URI prefix of editor models; stripping it yields the IDE workspace path (/ws/proj/...). */
const VIRTUAL_FILE_PREFIX = 'file:///workspace';

/** Monaco command id used for display-only CodeLenses (e.g. a reference count with no resolved action). */
const NOOP_COMMAND = 'dirigible.java.noopLens';

/** Java SE keywords/literals offered as low-priority completion so they always appear while typing. */
const JAVA_KEYWORDS = ['abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
    'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float',
    'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new',
    'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp', 'super', 'switch',
    'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', 'var',
    'yield', 'record', 'sealed', 'permits', 'true', 'false', 'null'];

/** Shared definition-style location request used by go-to-definition/implementation/type-definition. */
async function requestLocations(method: string, model: monaco.editor.ITextModel, position: monaco.Position) {
    if (!_conn || !isWorkspaceFile(model.uri.toString())) return null;
    const result: Location | Location[] | null = await _conn.sendRequest(method, {
        textDocument: { uri: model.uri.toString() },
        position:     { line: position.lineNumber - 1, character: position.column - 1 },
    });
    if (!result) return null;
    const locations = (Array.isArray(result) ? result : [result]).map(loc => ({ uri: monaco.Uri.parse(loc.uri), range: lspRangeToMonaco(loc.range) }));
    await ensureModelsForLocations(locations);
    return locations;
}

/**
 * Creates in-memory Monaco models for the workspace files referenced by the given locations (fetched
 * over REST) so the references / peek widgets can render a code preview — Monaco can only preview files
 * it has a model for, and this single-file editor otherwise has none for other files.
 */
async function ensureModelsForLocations(locations: Array<{ uri: monaco.Uri }>): Promise<void> {
    const seen = new Set<string>();
    await Promise.all(locations.map(async ({ uri }) => {
        const uriStr = uri.toString();
        if (seen.has(uriStr) || !uriStr.startsWith(VIRTUAL_FILE_PREFIX) || monaco.editor.getModel(uri)) return;
        seen.add(uriStr);
        try {
            const text = await fetchWorkspaceFileText(uriToWorkspacePath(uriStr) ?? uriStr);
            if (!monaco.editor.getModel(uri)) monaco.editor.createModel(text, 'java', uri);
        } catch {
            // preview just won't render for this one
        }
    }));
}

/** Recursively maps LSP hierarchical DocumentSymbols to Monaco's shape (kinds are 1-based vs 0-based). */
function mapDocumentSymbols(symbols: any[]): monaco.languages.DocumentSymbol[] {
    return (symbols ?? []).map(s => ({
        name:           s.name,
        detail:         s.detail ?? '',
        kind:           (s.kind ?? 1) - 1,
        tags:           s.tags ?? [],
        range:          lspRangeToMonaco(s.range),
        selectionRange: lspRangeToMonaco(s.selectionRange ?? s.range),
        children:       s.children ? mapDocumentSymbols(s.children) : [],
    }));
}

function foldingKind(kind: string | undefined): monaco.languages.FoldingRangeKind | undefined {
    const FK = monaco.languages.FoldingRangeKind;
    switch (kind) {
        case 'comment': return FK.Comment;
        case 'imports': return FK.Imports;
        case 'region':  return FK.Region;
        default:        return undefined;
    }
}

/** Flattens an LSP SelectionRange parent-chain into Monaco's innermost-to-outermost array. */
function flattenSelectionRange(selectionRange: any): monaco.languages.SelectionRange[] {
    const ranges: monaco.languages.SelectionRange[] = [];
    let current = selectionRange;
    while (current) {
        ranges.push({ range: lspRangeToMonaco(current.range) });
        current = current.parent;
    }
    return ranges;
}

/** Maps a JDT.LS CodeLens command to a Monaco command, wiring the references/implementations peek. */
function mapLensCommand(cmd: any): monaco.languages.Command {
    const args = cmd.arguments ?? [];
    if ((cmd.command === 'java.show.references' || cmd.command === 'java.show.implementations') && args.length >= 3) {
        const locations = (args[2] ?? []).map((l: any) => ({ uri: monaco.Uri.parse(l.uri), range: lspRangeToMonaco(l.range) }));
        return {
            id:        'editor.action.showReferences',
            title:     cmd.title,
            arguments: [monaco.Uri.parse(args[0]), { lineNumber: args[1].line + 1, column: args[1].character + 1 }, locations],
        };
    }
    return { id: NOOP_COMMAND, title: cmd.title };
}

type LspRange = { start: { line: number; character: number }; end: { line: number; character: number } };

/** Monaco completion item carrying the originating LSP item so resolve can fetch its import edits. */
type MonacoCompletionItem = monaco.languages.CompletionItem & { _lsp?: CompletionItem };

function textEditToMonaco(edit: TextEdit): monaco.languages.TextEdit {
    return { range: lspRangeToMonaco(edit.range), text: edit.newText };
}

function monacoRangeToLsp(r: monaco.IRange): LspRange {
    return {
        start: { line: r.startLineNumber - 1, character: r.startColumn - 1 },
        end:   { line: r.endLineNumber - 1, character: r.endColumn - 1 },
    };
}

/** True when two LSP ranges intersect (used to pick the diagnostics relevant to a code-action request). */
function rangesOverlap(a: LspRange, b: LspRange): boolean {
    const notAfter = (p: { line: number; character: number }, q: { line: number; character: number }) =>
        p.line < q.line || (p.line === q.line && p.character <= q.character);
    return notAfter(a.start, b.end) && notAfter(b.start, a.end);
}

function lspCodeActionToMonaco(action: CodeAction | Command): monaco.languages.CodeAction {
    const isCommand = typeof (action as Command).command === 'string';
    const title = action.title
        ?? (isCommand ? (action as Command).command : (action as CodeAction).command?.title)
        ?? 'Action';
    const kind = isCommand ? 'quickfix' : ((action as CodeAction).kind ?? 'quickfix');
    return {
        title,
        kind,
        diagnostics: [],
        isPreferred: (action as CodeAction).isPreferred,
        // Apply lazily through our command so we can resolve, run server commands and apply edits uniformly.
        command: { id: APPLY_ACTION_COMMAND, title, arguments: [action] },
    };
}

async function applyCodeAction(action: CodeAction | Command): Promise<void> {
    if (!_conn) return;
    try {
        if (typeof (action as Command).command === 'string') {
            await runServerCommand(action as Command);
            return;
        }
        let resolved = action as CodeAction;
        if (!resolved.edit && (resolved as { data?: unknown }).data !== undefined) {
            resolved = await _conn.sendRequest('codeAction/resolve', resolved);
        }
        if (resolved.edit) applyWorkspaceEdit(resolved.edit);
        if (resolved.command) await runServerCommand(resolved.command);
    } catch (e) {
        console.warn('[java-lsp] code action failed:', (e as Error)?.message ?? e);
    }
}

function isWorkspaceEdit(value: unknown): value is WorkspaceEdit {
    return !!value && (!!(value as WorkspaceEdit).changes || !!(value as WorkspaceEdit).documentChanges);
}

async function runServerCommand(cmd: Command): Promise<void> {
    if (!_conn || !cmd?.command) return;
    if (GENERATE[cmd.command]) {
        await runGenerate(cmd.command, cmd.arguments ?? []);
        return;
    }
    const result = await _conn.sendRequest('workspace/executeCommand', { command: cmd.command, arguments: cmd.arguments ?? [] });
    if (isWorkspaceEdit(result)) applyWorkspaceEdit(result);
}

interface GenerateSpec {
    label: string;
    status: string;
    generate: string;
    members: (status: any) => Array<{ label: string; ref: any }>;
    buildArgs: (promptArgs: any[], status: any, selected: any[]) => any[];
}

/** Member-named fields → picker labels. */
function fieldLabel(f: any): string {
    const name = f?.name ?? f?.fieldName ?? '';
    const type = f?.type ?? f?.typeName;
    return type ? `${name}: ${type}` : `${name}`;
}

/**
 * The JDT.LS source-generation commands (constructors, getters/setters, toString, hashCode/equals).
 * Each maps the client "*Prompt" command to the server status + generate delegate commands; the status
 * call yields the candidate fields shown in the member picker, the generate call returns the edit.
 */
const GENERATE: Record<string, GenerateSpec> = {
    'java.action.generateConstructorsPrompt': {
        label: 'Select fields and constructors',
        status: 'java.action.checkConstructorsStatus',
        generate: 'java.action.generateConstructors',
        members: (s) => (s?.fields ?? []).map((f: any) => ({ label: fieldLabel(f), ref: f })),
        buildArgs: (args, s, sel) => [args[0], { constructors: s?.constructors ?? [], fields: sel.map(m => m.ref) }],
    },
    'java.action.generateToStringPrompt': {
        label: 'Select fields to include in toString()',
        status: 'java.action.checkToStringStatus',
        generate: 'java.action.generateToString',
        members: (s) => (s?.fields ?? []).map((f: any) => ({ label: fieldLabel(f), ref: f })),
        buildArgs: (args, _s, sel) => [args[0], sel.map(m => m.ref)],
    },
    'java.action.hashCodeEqualsPrompt': {
        label: 'Select fields for hashCode() and equals()',
        status: 'java.action.checkHashCodeEqualsStatus',
        generate: 'java.action.generateHashCodeEquals',
        members: (s) => (s?.fields ?? []).map((f: any) => ({ label: fieldLabel(f), ref: f })),
        buildArgs: (args, _s, sel) => [args[0], sel.map(m => m.ref), false],
    },
    'java.action.generateAccessorsPrompt': {
        label: 'Select fields to generate getters and setters',
        status: 'java.action.checkAccessorsStatus',
        generate: 'java.action.generateAccessors',
        members: (s) => (s?.accessors ?? s ?? []).map((a: any) => ({ label: fieldLabel(a), ref: a })),
        buildArgs: (args, _s, sel) => [args[0], sel.map(m => m.ref)],
    },
    'java.action.overrideMethodsPrompt': {
        label: 'Select methods to override or implement',
        status: 'java.action.listOverridableMethods',
        generate: 'java.action.addOverridableMethods',
        members: (s) => (s?.methods ?? []).map((m: any) => ({
            label: `${m.name}(${(m.parameters ?? []).join(', ')})${m.declaringClass ? ' : ' + m.declaringClass : ''}`,
            ref: m,
        })),
        buildArgs: (args, status, sel) => [args[0], { overridableMethods: sel.map(m => m.ref), type: status?.type }],
    },
};

async function runGenerate(promptId: string, args: any[]): Promise<void> {
    if (!_conn) return;
    const spec = GENERATE[promptId];
    const status = await _conn.sendRequest('workspace/executeCommand', { command: spec.status, arguments: args });
    if (!status) return;
    const members = spec.members(status);
    let selected = members;
    const picker = (globalThis as any).javaLspMemberPicker;
    if (members.length && typeof picker === 'function') {
        const chosen: string[] | null = await picker(spec.label, members.map(m => m.label));
        if (chosen === null) return; // user cancelled the dialog
        selected = members.filter(m => chosen.includes(m.label));
    }
    const edit = await _conn.sendRequest('workspace/executeCommand', {
        command: spec.generate,
        arguments: spec.buildArgs(args, status, selected),
    });
    if (isWorkspaceEdit(edit)) applyWorkspaceEdit(edit);
}

/** Groups a workspace edit's text edits by document and applies them in place to open Monaco models. */
function applyWorkspaceEdit(edit: WorkspaceEdit | null | undefined): void {
    if (!edit) return;
    const byUri: Record<string, TextEdit[]> = {};
    if (edit.changes) {
        for (const uri in edit.changes) byUri[uri] = (byUri[uri] ?? []).concat(edit.changes[uri]);
    }
    if (edit.documentChanges) {
        for (const dc of edit.documentChanges as any[]) {
            if (dc?.textDocument?.uri && Array.isArray(dc.edits)) {
                byUri[dc.textDocument.uri] = (byUri[dc.textDocument.uri] ?? []).concat(dc.edits);
            }
        }
    }
    for (const uri in byUri) {
        const model = monaco.editor.getModels().find(m => m.uri.toString() === uri);
        if (!model) continue;
        const ops = byUri[uri].map(e => ({ range: lspRangeToMonaco(e.range), text: e.newText, forceMoveMarkers: true }));
        model.pushEditOperations([], ops, () => null);
    }
}

/** Converts an LSP workspace edit into the Monaco shape returned by the rename provider. */
function workspaceEditToMonaco(edit: WorkspaceEdit | null): monaco.languages.WorkspaceEdit {
    const edits: any[] = [];
    const push = (uri: string, list: TextEdit[]) => {
        for (const e of list) {
            edits.push({ resource: monaco.Uri.parse(uri), textEdit: { range: lspRangeToMonaco(e.range), text: e.newText }, versionId: undefined });
        }
    };
    if (edit?.changes) {
        for (const uri in edit.changes) push(uri, edit.changes[uri]);
    }
    if (edit?.documentChanges) {
        for (const dc of edit.documentChanges as any[]) {
            if (dc?.textDocument?.uri && Array.isArray(dc.edits)) push(dc.textDocument.uri, dc.edits);
        }
    }
    return { edits };
}

/** Maps a virtual editor URI back to the IDE workspace path ({@code /ws/proj/...}). */
function uriToWorkspacePath(uri: string): string | null {
    if (!uri.startsWith(VIRTUAL_FILE_PREFIX)) return null;
    return decodeURIComponent(uri.substring(VIRTUAL_FILE_PREFIX.length));
}

/** Reads a workspace file's current text over the IDE REST API. */
async function fetchWorkspaceFileText(idePath: string): Promise<string> {
    const response = await fetch('/services/ide/workspaces' + idePath, { headers: { 'X-Requested-With': 'Fetch' } });
    if (!response.ok) {
        throw new Error(`Could not read ${idePath} (HTTP ${response.status})`);
    }
    return response.text();
}

/** Applies LSP text edits to a string. Offsets are resolved against the original text and edits are
 *  applied from the end backwards, so earlier offsets stay valid. */
function applyEditsToText(text: string, edits: TextEdit[]): string {
    const lineStarts = [0];
    for (let i = 0; i < text.length; i++) {
        if (text.charCodeAt(i) === 10 /* \n */) lineStarts.push(i + 1);
    }
    const offset = (p: { line: number; character: number }) => (lineStarts[p.line] ?? text.length) + p.character;
    const ordered = edits.slice()
                         .sort((a, b) => offset(b.range.start) - offset(a.range.start));
    let result = text;
    for (const e of ordered) {
        result = result.slice(0, offset(e.range.start)) + e.newText + result.slice(offset(e.range.end));
    }
    return result;
}

/**
 * Applies a JDT.LS rename {@link WorkspaceEdit} across the whole workspace: text edits in every
 * affected file plus any {@code RenameFile} operation (a public-type rename renames its own
 * {@code .java} file). The current file's edits go through the live Monaco model; the rest are read,
 * edited and written back over REST. Persistence (CSRF-guarded writes, the tab switch when the current
 * file is renamed, and reloading other open editors) is delegated to the IDE via javaLspPersistRename.
 */
async function applyRenameAcrossWorkspace(model: monaco.editor.ITextModel, edit: WorkspaceEdit): Promise<void> {
    const currentUri = model.uri.toString();
    const textByUri: Record<string, TextEdit[]> = {};
    const renameByUri: Record<string, string> = {};

    if (edit.changes) {
        for (const uri in edit.changes) textByUri[uri] = (textByUri[uri] ?? []).concat(edit.changes[uri]);
    }
    if (edit.documentChanges) {
        for (const dc of edit.documentChanges as any[]) {
            if (dc?.kind === 'rename' && dc.oldUri && dc.newUri) {
                renameByUri[dc.oldUri] = dc.newUri;
            } else if (dc?.textDocument?.uri && Array.isArray(dc.edits)) {
                textByUri[dc.textDocument.uri] = (textByUri[dc.textDocument.uri] ?? []).concat(dc.edits);
            }
        }
    }

    // JDT.LS may key a file's text edits by its POST-rename URI (documentChanges are ordered, and the
    // rename can precede the edit). Re-attribute every edit to the on-disk (old) URI so the content is
    // edited correctly before the file is written/renamed — otherwise the new file keeps the old type
    // name and triggers "The public type X must be defined in its own file".
    const newToOld: Record<string, string> = {};
    for (const oldUri in renameByUri) newToOld[renameByUri[oldUri]] = oldUri;
    const editsByOld: Record<string, TextEdit[]> = {};
    for (const uri in textByUri) {
        const onDiskUri = newToOld[uri] ?? uri;
        editsByOld[onDiskUri] = (editsByOld[onDiskUri] ?? []).concat(textByUri[uri]);
    }

    const payload: {
        currentPath: string | null;
        currentContent: string | null;
        currentNewPath: string | null;
        writes: Array<{ path: string | null; content: string }>;
        renames: Array<{ oldPath: string | null; newPath: string | null; content: string }>;
    } = { currentPath: uriToWorkspacePath(currentUri), currentContent: null, currentNewPath: null, writes: [], renames: [] };

    const watchedChanges: Array<{ uri: string; type: number }> = [];
    const oldUris = new Set<string>([...Object.keys(editsByOld), ...Object.keys(renameByUri)]);
    for (const oldUri of oldUris) {
        const edits = editsByOld[oldUri] ?? [];
        let content: string;
        if (oldUri === currentUri) {
            if (edits.length) {
                model.pushEditOperations([], edits.map(e => ({ range: lspRangeToMonaco(e.range), text: e.newText, forceMoveMarkers: true })), () => null);
            }
            content = model.getValue();
        } else {
            const source = await fetchWorkspaceFileText(uriToWorkspacePath(oldUri) ?? oldUri);
            content = edits.length ? applyEditsToText(source, edits) : source;
        }
        const newUri = renameByUri[oldUri];
        if (oldUri === currentUri) {
            payload.currentContent = content;
            payload.currentNewPath = newUri ? uriToWorkspacePath(newUri) : null;
        } else if (newUri) {
            payload.renames.push({ oldPath: uriToWorkspacePath(oldUri), newPath: uriToWorkspacePath(newUri), content });
        } else {
            payload.writes.push({ path: uriToWorkspacePath(oldUri), content });
        }
        // File-change events for JDT.LS so it re-syncs without a page refresh.
        if (newUri) {
            watchedChanges.push({ uri: oldUri, type: 3 /* Deleted */ }, { uri: newUri, type: 1 /* Created */ });
            _openFiles.delete(oldUri);
            _conn?.sendNotification('textDocument/didClose', { textDocument: { uri: oldUri } });
        } else {
            watchedChanges.push({ uri: oldUri, type: 2 /* Changed */ });
        }
    }

    await (globalThis as any).javaLspPersistRename(payload);

    // Inform JDT.LS of the on-disk changes so the renamed type's diagnostics clear immediately.
    if (_conn && watchedChanges.length) {
        _conn.sendNotification('workspace/didChangeWatchedFiles', { changes: watchedChanges });
    }
}

function jdtlsSettings() {
    return {
        java: {
            import: {
                maven:      { enabled: true },
                gradle:     { enabled: false },
                exclusions: ['**/node_modules/**', '**/.metadata/**', '**/archetype-resources/**'],
            },
            autobuild: { enabled: true },
            completion: {
                overwrite:            true,
                guessMethodArguments: false,
                postfix:              { enabled: true },
                filteredTypes: [
                    'com.sun.*', 'sun.*', 'jdk.*',
                    'org.eclipse.jdt.internal.*',
                    'org.eclipse.core.internal.*',
                    'org.eclipse.osgi.internal.*',
                ],
                importOrder: ['java', 'javax', 'org', 'com', ''],
            },
            signatureHelp:  { enabled: true },
            format:         { enabled: true },
            saveActions:    { organizeImports: false },
            inlayHints:     { parameterNames: { enabled: 'all' } },
            // Off by default: the reference/implementation search behind these CodeLenses runs for every
            // declaration on open and on every edit and dominates JDT.LS load on a large classpath.
            referencesCodeLens:     { enabled: false },
            implementationsCodeLens: { enabled: false },
        },
    };
}
