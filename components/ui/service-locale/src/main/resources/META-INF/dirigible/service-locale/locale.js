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
import { request, response } from 'sdk/http';
import { registry } from 'sdk/platform';
import { uuid } from 'sdk/utils';
import { getKeyPrefix } from '/platform-branding/branding.mjs';

const COOKIE_PREFIX = `${getKeyPrefix()}.locale.`;

const basePath = request.getParameter('path');
const lang = request.getParameter('lng');
const ns = request.getParameter('ns');
const localePath = `${basePath}/${lang}/${ns}.json`;

if (localePath) {
    if (isCached()) {
        responseNotModified();
    } else {
        getLocale();
    }
} else {
    responseBadRequest('Provide the \'path\' parameter');
}

response.flush();
response.close();

function setETag() {
    const maxAge = 3600; // Temp
    const etag = uuid.random();
    response.addCookie({
        'name': getCacheKey(),
        'value': etag,
        'path': '/',
        'maxAge': maxAge
    });
    response.setHeader('ETag', etag);
    response.setHeader('Cache-Control', `private, must-revalidate, max-age=${maxAge}`);
}

function getCacheKey() {
    return COOKIE_PREFIX + localePath.replaceAll(',', '.');
}

function isCached() {
    let cookie = null;
    let cookies = request.getCookies();
    if (cookies) {
        cookie = cookies.filter(e => e.name === getCacheKey())[0];
    }
    if (cookie) {
        return cookie.value === request.getHeader('If-None-Match');
    }
    return false;
}

function getLocale() {
    const common = JSON.parse(registry.getText(`/resources-locale/${lang}/common.json`));
    const requested = JSON.parse(registry.getText(localePath));
    const responseContent = {
        ...common,
        ...requested
    };
    response.setContentType('application/json');
    setETag();
    response.println(JSON.stringify(responseContent));
}

function responseNotModified() {
    response.setStatus(response.NOT_MODIFIED);
}

function responseBadRequest(message) {
    response.setContentType('text/plain');
    response.setStatus(response.BAD_REQUEST);
    response.println(message);
}
