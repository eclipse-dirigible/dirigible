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

let _providersRegistered = false;

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
    await _conn.sendRequest('initialize', {
        processId: null,
        rootUri,
        initializationOptions: {
            settings: jdtlsSettings(),
            extendedClientCapabilities: {
                progressReportProvider:            false,
                classFileContentsSupport:          true,
                resolveAdditionalTextEditsSupport: true,
                advancedGenerateAccessorsSupport:  true,
                generateToStringPromptSupport:     true,
                generateConstructorsPromptSupport: true,
                generateDelegateMethodsPromptSupport: true,
                hashCodeEqualsPromptSupport:       true,
                advancedOrganizeImportsSupport:    true,
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
                documentSymbol: { dynamicRegistration: true },
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

    if (model) {
        let timer: ReturnType<typeof setTimeout>;
        model.onDidChangeContent(() => {
            clearTimeout(timer);
            timer = setTimeout(() => {
                _conn?.sendNotification('textDocument/didChange', {
                    textDocument:   { uri: fileUri, version: model.getVersionId() },
                    contentChanges: [{ text: model.getValue() }],
                });
            }, 400);
        });
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
            const locations = Array.isArray(result) ? result : [result];
            return locations.map(loc => ({
                uri:   monaco.Uri.parse(loc.uri),
                range: lspRangeToMonaco(loc.range),
            }));
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
            return result.map(loc => ({ uri: monaco.Uri.parse(loc.uri), range: lspRangeToMonaco(loc.range) }));
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
            const diagnostics = (context.markers ?? []).map(markerToLspDiagnostic);
            const result: Array<CodeAction | Command> | null = await _conn.sendRequest('textDocument/codeAction', {
                textDocument: { uri: model.uri.toString() },
                range:        monacoRangeToLsp(range),
                context:      { diagnostics, only: context.only ? [context.only] : undefined },
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

function monacoSeverityToLsp(severity: monaco.MarkerSeverity): DiagnosticSeverity {
    switch (severity) {
        case monaco.MarkerSeverity.Error:   return DiagnosticSeverity.Error;
        case monaco.MarkerSeverity.Warning: return DiagnosticSeverity.Warning;
        case monaco.MarkerSeverity.Info:    return DiagnosticSeverity.Information;
        default:                            return DiagnosticSeverity.Hint;
    }
}

function markerToLspDiagnostic(m: monaco.editor.IMarkerData): Diagnostic {
    return {
        range:    { start: { line: m.startLineNumber - 1, character: m.startColumn - 1 },
                    end:   { line: m.endLineNumber - 1, character: m.endColumn - 1 } },
        message:  m.message,
        severity: monacoSeverityToLsp(m.severity),
        source:   m.source,
    } as Diagnostic;
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
        },
    };
}
