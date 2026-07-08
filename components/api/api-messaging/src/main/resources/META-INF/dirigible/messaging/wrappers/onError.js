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
let handlerPath = __context.get("handler");
if (!handlerPath || typeof handlerPath !== "string") {
    throw new Error("Invalid handler path");
}
if (handlerPath.includes("..") || handlerPath.includes("//") || handlerPath.startsWith("/")) {
    throw new Error("Invalid handler path: " + handlerPath);
}

let handler;

try {
    handler = dirigibleRequire(handlerPath);
} catch (e) {
    handler = await import(handlerPath);
}

handler.onError(__context.get("error"));
