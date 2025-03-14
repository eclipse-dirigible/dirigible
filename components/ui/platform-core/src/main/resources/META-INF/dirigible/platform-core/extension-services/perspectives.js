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
import { getPerspectives } from './modules/perspectives.mjs'
import { request, response } from 'sdk/http';
import { uuid } from 'sdk/utils';


function setETag() {
	const maxAge = 30 * 24 * 60 * 60;
	const etag = uuid.random();
	response.setHeader('ETag', etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

setETag();
response.setContentType('application/json');
response.println(JSON.stringify(await getPerspectives((request.getParameter('extensionPoints') || 'platform-perspectives').split(','))));
response.flush();
response.close();