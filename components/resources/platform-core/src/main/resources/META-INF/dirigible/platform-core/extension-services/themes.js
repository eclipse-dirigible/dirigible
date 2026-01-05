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
import { request, response } from "@aerokit/sdk/http";
import { extensions } from "@aerokit/sdk/extensions";
import { uuid } from "@aerokit/sdk/utils";

const allThemes = [];
const extensionPoints = request.getParameterValues('extensionPoints') ?? ['platform-themes'];

function sortThemes(a, b) {
	if (a.order !== undefined && b.order !== undefined) {
		return (parseInt(a.order) - parseInt(b.order));
	} else if (a.order === undefined && b.order === undefined) {
		return a.name < b.name ? -1 : 1
	} else if (a.order === undefined) {
		return 1;
	} else if (b.order === undefined) {
		return -1;
	}
	return 0;
}

try {
	for (let i = 0; i < extensionPoints.length; i++) {
		// @ts-ignore
		const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
		for (let e = 0; e < extensionList.length; e++) {
			allThemes.push(extensionList[e].getTheme());
		}
	}
	allThemes.sort(sortThemes);
} catch (e) {
	console.error(`Error while loading theme modules: ${e}`);
}

function setETag() {
	const maxAge = 30 * 24 * 60 * 60;
	const etag = uuid.random();
	response.setHeader("ETag", etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

response.setContentType("application/json");
setETag();
response.println(JSON.stringify(allThemes));
response.flush();
response.close();
