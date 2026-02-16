/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { request, response } from '@aerokit/sdk/http';
import { registry } from '@aerokit/sdk/platform';
import { uuid } from '@aerokit/sdk/utils';
import { getBrandingJs, getKeyPrefix } from '/platform-branding/branding.mjs';

const COOKIE_PREFIX = `${getKeyPrefix()}.ljs.`;

const scriptIds = request.getParameter('ids') ?? request.getParameter('id');
if (scriptIds) {
    if (isCached(scriptIds)) {
        responseNotModified();
    } else {
        processScriptRequest(scriptIds);
    }
} else {
    responseBadRequest('Provide the \'id\' parameter of the script');
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
    response.setHeader('ETag', etag);
    response.setHeader('Cache-Control', `private, must-revalidate, max-age=${maxAge}`);
}

function getCacheKey(scriptId) {
    return COOKIE_PREFIX + scriptId.replaceAll(',', '.');
}

function isCached(scriptId) {
    let cookie = null;
    let cookies = request.getCookies();
    if (cookies) {
        cookie = cookies.filter(e => e.name === getCacheKey(scriptId))[0];
    }
    if (cookie) {
        return cookie.value === request.getHeader('If-None-Match');
    }
    return false;
}

function processScriptRequest(scriptIds) {
    const ids = scriptIds.split(',');
    let contentType = ids[0].endsWith('-js') ? 'text/javascript;charset=UTF-8' : 'text/css';
    let responseContent = '';
    for (let i = 0; i < ids.length; i++) {
        const scriptList = getScriptList(ids[i]);
        if (scriptList) {
            scriptList.forEach((script) => {
                if (typeof script === 'string') {
                    let text = registry.getText(script);
                    if (text !== null) {
                        if (text.includes('//# sourceMappingURL=')) {
                            text = text.replace('//# sourceMappingURL=', `//# sourceMappingURL=/webjars${script.slice(0, script.lastIndexOf('/') + 1)}`);
                            text += '\n';
                        }
                        responseContent += text;
                    } else {
                        console.error("Cannot load the script: " + script);
                    }
                } else {
                    responseContent += script() + '\n';
                }
            });
        } else {
            responseBadRequest(`Loader: '${ids[i]}' is not a valid id.`);
        }
    }
    response.setContentType(contentType);
    setETag(scriptIds);
    response.println(responseContent);
}

function getScriptList(scriptId) {
    const baseJs = [
        '/jquery/3.7.1/jquery.min.js',
        '/i18next/25.3.0/dist/umd/i18next.min.js',
        '/angularjs/1.8.2/angular.min.js',
        '/angularjs/1.8.2/angular-resource.min.js',
        '/angular-aria/1.8.2/angular-aria.min.js',
        '/angularjs/1.8.2/angular-sanitize.min.js',
        getBrandingJs,
        '/platform-core/utilities/view.js',
        '/platform-core/ui/platform/user.js',
        '/platform-core/ui/platform/message-hub.js',
        '/platform-core/ui/platform/layout-hub.js',
        '/platform-core/ui/platform/shell-hub.js',
        '/platform-core/ui/platform/status-bar-hub.js',
        '/platform-core/ui/platform/extensions.js',
        '/platform-core/ui/platform/theming-hub.js',
        '/platform-core/ui/platform/theming.js',
        '/platform-core/ui/platform/view.js',
        '/platform-core/ui/platform/dialog-hub.js',
        '/platform-core/ui/platform/dialogs.js',
        '/platform-core/ui/platform/notification-hub.js',
        '/platform-core/ui/platform/contextmenu-hub.js',
        '/platform-core/ui/platform/contextmenu.js',
        '/platform-core/ui/platform/shortcuts.js',
        '/webjars/blimpkit__blimpkit/1.8.6/dist/blimpkit.min.js',
        '/platform-core/ui/platform/locale.js',
    ];
    const cookies = '/angularjs/1.8.2/angular-cookies.min.js';
    const editorsService = '/platform-core/ui/platform/editors.js';
    const viewCss = [
        '/fundamental-styles/0.38.0/dist/fundamental-styles.css',
        '/platform-core/ui/styles/blimpkit.css',
        '/platform-core/ui/styles/fonts.css',
    ];
    switch (scriptId) {
        case 'view-js':
            return baseJs;
        case 'editor-js':
            return [
                ...baseJs,
                '/service-workspace/workspace-hub.js',
                '/service-workspace/workspace.js',
                '/service-repository/repository-hub.js',
                '/service-repository/repository.js',
            ];
        case 'perspective-js':
            return [
                ...baseJs,
                '/split.js/1.6.5/dist/split.min.js',
                '/platform-core/ui/platform/split.js',
                '/service-workspace/workspace-hub.js',
                editorsService,
                '/platform-core/ui/platform/layout.js',
            ];
        case 'shell-js':
            const shell = [
                ...baseJs,
                cookies,
                '/platform-core/ui/platform/shell.js',
            ];
            return shell;
        case 'file-upload-js':
            return [
                '/es5-shim/4.6.7/es5-shim.min.js',
                '/angular-file-upload/2.6.1/dist/angular-file-upload.min.js',
            ];
        case 'split-js':
            return ['/split.js/1.6.5/dist/split.min.js', '/platform-core/ui/platform/split.js'];
        case 'split-css':
            return ['/platform-core/ui/styles/split.css'];
        case 'view-css':
            return viewCss;
        case 'shell-css':
            return [...viewCss, '/platform-core/ui/styles/shell.css']
        case 'perspective-css':
            return [...viewCss, '/platform-core/ui/styles/split.css', '/platform-core/ui/styles/layout.css']
        case 'code-editor-js':
            return ['/editor-monaco/embeddable/editor.js', '/monaco-editor/0.40.0/min/vs/loader.js', '/monaco-editor/0.40.0/min/vs/editor/editor.main.nls.js', '/monaco-editor/0.40.0/min/vs/editor/editor.main.js'];
        case 'code-editor-css':
            return ['/editor-monaco/css/embeddable.css', '/monaco-editor/0.40.0/min/vs/editor/editor.main.css'];
        case 'cookies-js':
            return [cookies];
        case 'jstree-js':
            return ['/jstree/3.3.12/jstree.min.js', '/platform-core/ui/jstree/indicator.plugin.js'];
        case 'jstree-css':
            return ['/platform-core/ui/styles/jstree.css'];
        case 'editors-service':
            return [editorsService];
        case 'sortable-js':
            return ['/sortablejs/1.15.2/Sortable.min.js'];
    }
}

function responseNotModified() {
    response.setStatus(response.NOT_MODIFIED);
}

function responseBadRequest(message) {
    response.setContentType('text/plain');
    response.setStatus(response.BAD_REQUEST);
    response.println(message);
}
