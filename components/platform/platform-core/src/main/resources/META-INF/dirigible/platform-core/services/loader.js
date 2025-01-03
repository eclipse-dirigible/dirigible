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
import { request, response } from 'sdk/http';
import { registry } from 'sdk/platform';
import { uuid } from 'sdk/utils';

const COOKIE_PREFIX = 'DIRIGIBLE.platform-core.loader.';

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
    return COOKIE_PREFIX + scriptId;
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
        const locations = getLocations(ids[i]);
        if (locations) {
            locations.forEach(function (scriptLocation) {
                let text = registry.getText(scriptLocation);
                if (text.includes('//# sourceMappingURL=')) {
                    text = text.replace('//# sourceMappingURL=', `//# sourceMappingURL=/webjars${scriptLocation.slice(0, scriptLocation.lastIndexOf('/') + 1)}`)
                }
                responseContent += text;
            });
        } else {
            responseBadRequest(`Loader: '${ids[i]}' is not a valid id.`);
        }
    }
    response.setContentType(contentType);
    setETag(scriptIds);
    response.println(responseContent);
}

function getLocations(scriptId) {
    const baseJs = [
        '/jquery/3.7.1/jquery.min.js',
        '/angularjs/1.8.2/angular.min.js',
        '/angularjs/1.8.2/angular-resource.min.js',
        '/angular-aria/1.8.2/angular-aria.min.js',
        '/angularjs/1.8.2/angular-sanitize.min.js',
        '/platform-branding/branding.js',
        '/platform-core/utilities/uri-builder.js',
        '/platform-core/utilities/view-parameters.js',
        '/platform-core/utilities/uuid.js',
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
        '/platform-core/ui/blimpkit/blimpkit.js',
        '/platform-core/ui/blimpkit/avatar.js',
        '/platform-core/ui/blimpkit/badge.js',
        '/platform-core/ui/blimpkit/bar.js',
        '/platform-core/ui/blimpkit/breadcrumb.js',
        '/platform-core/ui/blimpkit/busy-indicator.js',
        '/platform-core/ui/blimpkit/button.js',
        '/platform-core/ui/blimpkit/card.js',
        '/platform-core/ui/blimpkit/checkbox.js',
        '/platform-core/ui/blimpkit/combobox.js',
        '/platform-core/ui/blimpkit/dialog.js',
        '/platform-core/ui/blimpkit/forms.js',
        '/platform-core/ui/blimpkit/icon-tab-bar.js',
        '/platform-core/ui/blimpkit/input.js',
        '/platform-core/ui/blimpkit/link.js',
        '/platform-core/ui/blimpkit/list.js',
        '/platform-core/ui/blimpkit/menu.js',
        '/platform-core/ui/blimpkit/message-box.js',
        '/platform-core/ui/blimpkit/message-page.js',
        '/platform-core/ui/blimpkit/notification.js',
        '/platform-core/ui/blimpkit/object-status.js',
        '/platform-core/ui/blimpkit/pagination.js',
        '/platform-core/ui/blimpkit/panel.js',
        '/platform-core/ui/blimpkit/popover.js',
        '/platform-core/ui/blimpkit/product-switch.js',
        '/platform-core/ui/blimpkit/progress-indicator.js',
        '/platform-core/ui/blimpkit/radio.js',
        '/platform-core/ui/blimpkit/scrollbar.js',
        '/platform-core/ui/blimpkit/select.js',
        '/platform-core/ui/blimpkit/shellbar.js',
        '/platform-core/ui/blimpkit/step-input.js',
        '/platform-core/ui/blimpkit/switch.js',
        '/platform-core/ui/blimpkit/table.js',
        '/platform-core/ui/blimpkit/textarea.js',
        '/platform-core/ui/blimpkit/tile.js',
        '/platform-core/ui/blimpkit/token.js',
        '/platform-core/ui/blimpkit/tokenizer.js',
        '/platform-core/ui/blimpkit/tool-header.js',
        '/platform-core/ui/blimpkit/toolbar.js',
        '/platform-core/ui/blimpkit/upload-collection.js',
        '/platform-core/ui/blimpkit/vertical-navigation.js',
        '/platform-core/ui/blimpkit/wizard.js',
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
            return [
                ...baseJs,
                cookies,
                '/platform-core/ui/platform/shell.js',
            ];
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
            return ['/ide-monaco/embeddable/editor.js', '/monaco-editor/0.40.0/min/vs/loader.js', '/monaco-editor/0.40.0/min/vs/editor/editor.main.nls.js', '/monaco-editor/0.40.0/min/vs/editor/editor.main.js'];
        case 'code-editor-css':
            return ['/ide-monaco/css/embeddable.css', '/monaco-editor/0.40.0/min/vs/editor/editor.main.css'];
        case 'cookies':
            return [cookies];
        case 'jstree-js':
            return ['/jstree/3.3.12/jstree.min.js', '/platform-core/ui/jstree/indicator.plugin.js'];
        case 'jstree-css':
            return ['/platform-core/ui/styles/jstree.css'];
        case 'editors-service':
            return [editorsService];
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
