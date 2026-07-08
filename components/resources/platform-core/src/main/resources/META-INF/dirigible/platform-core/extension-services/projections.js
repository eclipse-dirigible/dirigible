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
import { getProjections } from './modules/projections.mjs'
import { request, response } from '@aerokit/sdk/http';

// Role-gated entries are filtered per user, so the response must never be shared across users.
response.setHeader('Cache-Control', 'private, no-cache');
response.setContentType('application/json');
response.println(JSON.stringify(await getProjections(request.getParameterValues('extensionPoints') ?? ['application-projections'])));
response.flush();
response.close();
