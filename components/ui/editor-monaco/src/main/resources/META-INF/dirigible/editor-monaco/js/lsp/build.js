#!/usr/bin/env node
/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
import * as esbuild from 'esbuild';

// ../java-lsp-client.js is committed and rebuilt by every Maven build, so its output must not depend on
// when `npm install` ran: package.json pins esbuild and every package that ends up in the bundle
// (vscode-ws-jsonrpc, vscode-jsonrpc via "overrides", vscode-languageserver-types) to an exact version,
// because there is no lockfile (package-lock.json is git-ignored). Bump a pin and commit the rebuilt
// bundle in the same change (#8754).

await esbuild.build({
    entryPoints: ['java-lsp-client.ts'],
    bundle: true,
    format: 'iife',
    globalName: 'JavaLspClientLib',
    // Replace monaco-editor imports with a lazy-Proxy shim so the bundle does not
    // embed Monaco (already loaded via AMD) and resolves all Monaco APIs at runtime
    // via window.monaco, which editor.js sets before calling JavaLspClientLib.connect().
    alias: { 'monaco-editor': './monaco-shim.ts' },
    outfile: '../java-lsp-client.js',
    platform: 'browser',
    target: 'es2020',
    sourcemap: 'inline',
    logLevel: 'info',
});
