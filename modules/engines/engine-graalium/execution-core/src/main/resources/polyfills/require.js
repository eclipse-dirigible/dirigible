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
const DirigibleSourceProvider = Java.type("org.eclipse.dirigible.graalium.core.modules.DirigibleSourceProvider");
const dirigibleSourceProvider = new DirigibleSourceProvider();
const fileSeparator = Java.type('java.io.File').separator;

const _loadedModules = {};

function _require (initialPath, path) {
    let moduleInfo = _loadedModules[path];
    if (moduleInfo) {
        return moduleInfo;
    }

    let maybeSource = dirigibleSourceProvider.getSource(path);
    if (!maybeSource) {
        maybeSource = dirigibleSourceProvider.getSource(fixPath(initialPath + fileSeparator + "index.js"));
    }

    const code = '(function(exports, module, require) { ' + maybeSource + '\n})';
    moduleInfo = {
        loaded: false,
        id: path,
        exports: {},
        require: require
    };

    const compiledWrapper = load({
        name: path,
        script: code
    });
    const cjsModuleProps = [
        moduleInfo.exports, /* exports */
        moduleInfo, /* module */
        moduleInfo.require /* require */
    ];

    compiledWrapper.apply(moduleInfo.exports, cjsModuleProps);
    moduleInfo.loaded = true;
    _loadedModules[path] = moduleInfo;
    return moduleInfo;
};

function require(path) {
    const module = _require(path, fixPath(path));
    return module.exports;
}

function fixPath(path, mod) {
    const mods = [
        "http",
        "io",
        "bpm",
        "cms",
        "core",
        "db",
        "etcd",
        "extensions",
        "git",
        "indexing",
        "job",
        "kafka",
        "log",
        "mail",
        "messaging",
        "mongodb",
        "net",
        "pdf",
        "platform",
        "qldb",
        "rabbitmq",
        "redis",
        "user",
        "template",
        "utils",
        "junit",
        "integrations",
        "security"
    ];

    let fixedPath = path;
    if (fixedPath.startsWith("sdk")) {
        fixedPath = fixedPath.substring("sdk".length)
    }

    if (fixedPath.startsWith("@aerokit/sdk")) {
        fixedPath = fixedPath.substring("@aerokit/sdk".length)
    }

    for (const mod of mods) {
        if (fixedPath.startsWith(mod + "/") && !fixedPath.includes("path-to-regexp")) {
            fixedPath = `modules/dist/cjs/${mod}/${fixedPath.substring(mod.length + 1)}`;
            break;
        } else if (fixedPath.startsWith("/" + mod + "/") && !fixedPath.includes("path-to-regexp")) {
            fixedPath = `modules/dist/cjs/${mod}/${fixedPath.substring(mod.length + 2)}`;
            break;
        }
    }

    return fixedPath;
}

globalThis.require = require;
globalThis.exports = {};
globalThis.dirigibleRequire = globalThis.require;
