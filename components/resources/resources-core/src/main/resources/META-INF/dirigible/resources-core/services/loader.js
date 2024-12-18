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
import { request, response } from "sdk/http";
import { registry } from "sdk/platform";
import { uuid } from "sdk/utils";

const COOKIE_PREFIX = "DIRIGIBLE.resources-core.loader.";

const viewJs = [
    "/jquery/3.7.1/jquery.min.js",
    "/angularjs/1.8.2/angular.min.js",
    "/angularjs/1.8.2/angular-resource.min.js",
    "/angular-aria/1.8.2/angular-aria.min.js",
    "/split.js/1.6.5/dist/split.min.js",
    "/resources-core/core/message-hub.js",
    "/resources-core/core/ide-message-hub.js",
    "/resources-core/ui/theming.js",
    "/resources-core/ui/widgets.js",
    "/resources-core/ui/extensions.js",
    "/resources-core/ui/view.js",
    "/resources-core/core/uri-builder.js",
    "/resources-core/ui/entityApi.js",
];

const viewCss = [
    "/fundamental-styles/0.38.0/dist/fundamental-styles.css",
    "/resources/styles/core.css",
    "/resources/styles/widgets.css",
];

let scriptId = request.getParameter("id");
if (scriptId) {
    if (isCached(scriptId)) {
        responseNotModified();
    } else {
        processScriptRequest(scriptId);
    }
} else {
    responseBadRequest("Provide the 'id' parameter of the script");
}

response.flush();
response.close();

function setETag(scriptId) {
    // let maxAge = 30 * 24 * 60 * 60;
    let maxAge = 3600; // Temp
    let etag = uuid.random();
    response.addCookie({
        'name': getCacheKey(scriptId),
        'value': etag,
        'path': '/',
        'maxAge': maxAge
    });
    response.setHeader("ETag", etag);
    response.setHeader('Cache-Control', `private, must-revalidate, max-age=${maxAge}`);
}

function getCacheKey(scriptId) {
    return COOKIE_PREFIX + scriptId;
}

function isCached(scriptId) {
    let cookie = null;
    let cookies = request.getCookies();
    if (cookies) {
        cookie = cookies.filter(e => e.name === getCacheKey(scriptId))[0];
    }
    if (cookie) {
        return cookie.value === request.getHeader("If-None-Match");
    }
    return false;
}

function processScriptRequest(scriptId) {
    let locations = getLocations(scriptId);
    if (locations) {
        let contentType = scriptId.endsWith('-js') ? "text/javascript;charset=UTF-8" : "text/css";
        response.setContentType(contentType);

        setETag(scriptId);
        locations.forEach(function (scriptLocation) {
            response.println(registry.getText(scriptLocation));
        });
    } else {
        responseBadRequest("Script with 'id': " + scriptId + " is not known.");
    }
}

function getLocations(scriptId) {
    switch (scriptId) {
        case "application-view-js":
        case "ide-view-js":
            return viewJs;
        case "ide-editor-js":
            return [...viewJs, "/ide-workspace-service/workspace.js"]
        case "application-perspective-js":
        case "ide-perspective-js":
            return [
                "/jquery/3.7.1/jquery.min.js",
                "/angularjs/1.8.2/angular.min.js",
                "/angularjs/1.8.2/angular-resource.min.js",
                "/angularjs/1.8.2/angular-cookies.min.js",
                "/angular-aria/1.8.2/angular-aria.min.js",
                "/resources-core/core/message-hub.js",
                "/resources-core/core/ide-message-hub.js",
                "/ide-branding/branding.js",
                "/split.js/1.6.5/dist/split.min.js",
                "/resources-core/ui/editors.js",
                "/resources-core/ui/core-modules.js",
                "/resources-core/ui/theming.js",
                "/resources-core/ui/widgets.js",
                "/resources-core/ui/extensions.js",
                "/resources-core/ui/view.js",
                "/resources-core/ui/layout.js",
                "/resources-core/core/uri-builder.js",
            ];
        case "file-upload-js":
            return [
                "/es5-shim/4.6.7/es5-shim.min.js",
                "/angular-file-upload/2.6.1/dist/angular-file-upload.min.js",
            ];
        case "sanitize-js":
            return ["/angularjs/1.8.2/angular-sanitize.min.js"];
        case "application-view-css":
        case "ide-editor-css":
        case "ide-view-css":
            return viewCss;
        case "application-perspective-css":
        case "ide-perspective-css":
            return [...viewCss, "/resources/styles/layout.css", "/resources/styles/perspective.css"]
        case "code-editor-js":
            return ["/ide-monaco/embeddable/editor.js", "/monaco-editor/0.40.0/min/vs/loader.js", "/monaco-editor/0.40.0/min/vs/editor/editor.main.nls.js", "/monaco-editor/0.40.0/min/vs/editor/editor.main.js"];
        case "code-editor-css":
            return ["/ide-monaco/css/embeddable.css", "/monaco-editor/0.40.0/min/vs/editor/editor.main.css"];
    }
}

function responseNotModified() {
    response.setStatus(response.NOT_MODIFIED);
}

function responseBadRequest(message) {
    response.setContentType("text/plain");
    response.setStatus(response.BAD_REQUEST);
    response.println(message);
}
