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
import { extensions } from '@aerokit/sdk/extensions';
import { uuid } from '@aerokit/sdk/utils';

const exPoints = request.getParameterValues('extensionPoints');

function setETag() {
    const maxAge = 30 * 24 * 60 * 60;
    const etag = uuid.random();
    response.setHeader('ETag', etag);
    response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

export async function getGeneric(extensionPoints = []) {
    const exts = [];
    const genericExtensions = [];
    for (let i = 0; i < extensionPoints.length; i++) {
        const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
        genericExtensions.push(...extensionList);
    }

    extLoop: for (let i = 0; i < genericExtensions?.length; i++) {
        const ext = genericExtensions[i].getExtension();
        if (!ext.id) {
            console.error(`Extension ['${JSON.stringify(ext)}'] does not have an id.`);
        } else {
            for (let v = 0; v < exts.length; v++) {
                if (exts[v].id === ext.id) {
                    console.error(`Duplication at extension with id: '${exts[v].id}'`);
                    continue extLoop;
                }
            }
            exts.push(ext);
        }
    }

    return exts;
}

response.setContentType('application/json');
setETag();

if (exPoints.length) {
    response.println(JSON.stringify(await getGeneric(exPoints)));
} else {
    response.sendError(400, 'Extension points parameter is empty');
}

response.flush();
response.close();
