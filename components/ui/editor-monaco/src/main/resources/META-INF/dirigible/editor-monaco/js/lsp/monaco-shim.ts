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
 * Lazy Proxy shim for Monaco Editor loaded via AMD.
 *
 * editor.js sets globalThis.monaco inside the AMD require() callback, before any
 * JavaLspClientLib.connect() call. All property accesses go through Proxy.get() traps
 * so they resolve against the live window.monaco at call time, not at bundle load time.
 *
 * TypeScript resolves types from the real monaco-editor devDependency; esbuild replaces
 * the "monaco-editor" import with this file at bundle time via the alias option.
 */

type M = typeof import('monaco-editor');

function m(): M {
    return (globalThis as any).monaco as M;
}

function ns<T extends object>(getter: () => T): T {
    return new Proxy({} as T, {
        get: (_, k) => (getter() as any)[k as string],
    });
}

function cls<T>(getter: () => T): T {
    return new Proxy(function () {} as any, {
        construct: (_, args) => new (getter() as any)(...args),
        get:       (_, k)    => (getter() as any)[k as string],
    }) as T;
}

export const editor         = ns(() => m().editor);
export const languages      = ns(() => m().languages);
export const MarkerSeverity = ns(() => m().MarkerSeverity);
export const MarkerTag      = ns(() => (m() as any).MarkerTag);
export const Uri            = cls(() => m().Uri);
export const Range          = cls(() => m().Range);
export const Position       = cls(() => m().Position);
export const Selection      = cls(() => m().Selection);
export const KeyCode        = ns(() => m().KeyCode);
export const KeyMod         = ns(() => m().KeyMod);
