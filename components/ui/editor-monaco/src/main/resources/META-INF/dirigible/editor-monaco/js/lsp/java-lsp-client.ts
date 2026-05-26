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
    type CompletionItem,
    type CompletionList,
    type Diagnostic,
    type Hover,
    type Location,
    type ParameterInformation,
    type SignatureHelp,
    type SignatureInformation,
} from 'vscode-languageserver-types';

// -------------------------------------------------------------------------
// Singleton state
// -------------------------------------------------------------------------

let _conn: MessageConnection | null = null;
let _fileUri = '';
let _providersRegistered = false;

// -------------------------------------------------------------------------
// Public API
// -------------------------------------------------------------------------

/** Called by editor.js when a Java file is opened. Safe to call multiple times. */
export async function connect(resourcePath: string): Promise<void> {
    if (_conn) return;

    const parts = resourcePath.replace(/^\//, '').split('/');
    const workspace = parts[0];
    const project   = parts[1];
    _fileUri = `file:///workspace/${workspace}/${project}/${parts.slice(2).join('/')}`;

    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    const wsUrl = `${proto}://${location.host}/websockets/ide/java-lsp`
                + `?workspace=${encodeURIComponent(workspace)}&project=${encodeURIComponent(project)}`;

    const ws = new WebSocket(wsUrl);
    await new Promise<void>((resolve, reject) => {
        ws.onopen  = () => resolve();
        ws.onerror = () => reject(new Error(`[java-lsp] WebSocket connect failed: ${wsUrl}`));
    });

    const socket = toSocket(ws);
    const reader = new WebSocketMessageReader(socket);
    const writer = new WebSocketMessageWriter(socket);
    _conn = createMessageConnection(reader, writer);

    // Diagnostics notification → Monaco markers
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

    _conn.listen();

    const rootUri = `file:///workspace/${workspace}/${project}/`;
    await _conn.sendRequest('initialize', {
        processId: null,
        rootUri,
        initializationOptions: {
            settings: jdtlsSettings(),
            extendedClientCapabilities: {
                progressReportProvider:       false,
                classFileContentsSupport:     true,
                resolveAdditionalTextEditsSupport: true,
                inferSelectionSupport:        [],
            },
        },
        workspaceFolders: [{ uri: rootUri, name: project }],
        capabilities: {
            textDocument: {
                synchronization: { dynamicRegistration: true, willSave: false, didSave: true, willSaveWaitUntil: false },
                completion: {
                    dynamicRegistration: true,
                    completionItem: { snippetSupport: true, documentationFormat: ['markdown', 'plaintext'], deprecatedSupport: true, commitCharactersSupport: true },
                    contextSupport: true,
                },
                hover:          { dynamicRegistration: true, contentFormat: ['markdown', 'plaintext'] },
                signatureHelp:  { dynamicRegistration: true, signatureInformation: { documentationFormat: ['markdown', 'plaintext'], parameterInformation: { labelOffsetSupport: true } } },
                definition:     { dynamicRegistration: true },
                publishDiagnostics: { relatedInformation: true },
            },
            workspace: { configuration: true, didChangeConfiguration: { dynamicRegistration: true } },
        },
    });

    _conn.sendNotification('initialized', {});
    _conn.sendNotification('workspace/didChangeConfiguration', { settings: jdtlsSettings() });
    _conn.sendNotification('textDocument/didOpen', {
        textDocument: {
            uri:        _fileUri,
            languageId: 'java',
            version:    1,
            text:       monaco.editor.getModels().find(m => m.uri.toString() === _fileUri)?.getValue() ?? '',
        },
    });

    // Track changes with debounce
    const model = monaco.editor.getModels().find(m => m.uri.toString() === _fileUri);
    if (model) {
        let timer: ReturnType<typeof setTimeout>;
        model.onDidChangeContent(() => {
            clearTimeout(timer);
            timer = setTimeout(() => {
                _conn?.sendNotification('textDocument/didChange', {
                    textDocument:  { uri: _fileUri, version: model.getVersionId() },
                    contentChanges: [{ text: model.getValue() }],
                });
            }, 400);
        });
    }

    if (!_providersRegistered) {
        _providersRegistered = true;
        registerProviders();
    }
}

// -------------------------------------------------------------------------
// Monaco provider registration
// -------------------------------------------------------------------------

function registerProviders(): void {

    monaco.languages.registerCompletionItemProvider('java', {
        triggerCharacters: ['.', '@', '<'],
        provideCompletionItems: async (model, position) => {
            if (!_conn || model.uri.toString() !== _fileUri) return null;
            const result: CompletionList | CompletionItem[] | null = await _conn.sendRequest('textDocument/completion', {
                textDocument: { uri: _fileUri },
                position:     { line: position.lineNumber - 1, character: position.column - 1 },
                context:      { triggerKind: 1 },
            });
            const items = Array.isArray(result) ? result : (result?.items ?? []);
            return {
                suggestions: items.map(item => lspCompletionToMonaco(item, model, position)),
            };
        },
    });

    monaco.languages.registerHoverProvider('java', {
        provideHover: async (model, position) => {
            if (!_conn || model.uri.toString() !== _fileUri) return null;
            const result: Hover | null = await _conn.sendRequest('textDocument/hover', {
                textDocument: { uri: _fileUri },
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
            if (!_conn || model.uri.toString() !== _fileUri) return null;
            const result: SignatureHelp | null = await _conn.sendRequest('textDocument/signatureHelp', {
                textDocument: { uri: _fileUri },
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
            if (!_conn || model.uri.toString() !== _fileUri) return null;
            const result: Location | Location[] | null = await _conn.sendRequest('textDocument/definition', {
                textDocument: { uri: _fileUri },
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
): monaco.languages.CompletionItem {
    const word = model.getWordUntilPosition(position);
    const range: monaco.IRange = {
        startLineNumber: position.lineNumber,
        startColumn:     word.startColumn,
        endLineNumber:   position.lineNumber,
        endColumn:       word.endColumn,
    };
    return {
        label:           item.label,
        kind:            lspCompletionKind(item.kind),
        detail:          item.detail,
        documentation:   item.documentation ? { value: markupToString(item.documentation), isTrusted: false } : undefined,
        insertText:      item.insertText ?? item.label,
        insertTextRules: item.insertTextFormat === InsertTextFormat.Snippet
                             ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
                             : undefined,
        range,
    };
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
