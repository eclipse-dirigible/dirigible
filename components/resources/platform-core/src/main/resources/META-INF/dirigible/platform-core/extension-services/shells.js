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
import { getShells } from './modules/shells.mjs';
import { request, response } from '@aerokit/sdk/http';

/*
 * The registered shells are CONTENT, not an asset: publishing a module, renaming a shell or removing
 * one changes this list, and every one of those changes has to reach an already-open browser. This
 * used to answer `public, must-revalidate, max-age=30 days` with a random-UUID ETag - so the browser
 * served a month-old list without asking, and the ETag could never match even when it did ask. A
 * renamed shell kept its old name in the IDE's Window menu until the cache expired.
 */
function setCacheControl() {
	response.setHeader('Cache-Control', 'private, no-cache');
}

response.setContentType('application/json');
setCacheControl();
response.println(JSON.stringify(await getShells(request.getParameterValues('extensionPoints') ?? ['platform-shells'])));
response.flush();
response.close();
