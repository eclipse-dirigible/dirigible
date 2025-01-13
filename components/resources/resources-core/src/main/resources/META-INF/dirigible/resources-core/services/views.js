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
import { extensions } from "sdk/extensions";
import { request, response } from "sdk/http";
import { uuid } from "sdk/utils";
import { user } from "sdk/security";

let views = [];
let extensionPoint = request.getParameter('extensionPoint') || 'ide-view';
let viewExtensions = await extensions.loadExtensionModules(extensionPoint);

function setETag() {
	let maxAge = 30 * 24 * 60 * 60;
	let etag = uuid.random();
	response.setHeader("ETag", etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

for (let i = 0; i < viewExtensions?.length; i++) {
	const view = viewExtensions[i].getView();
	if (view.roles && Array.isArray(view.roles)) {
		let hasRoles = true;
		for (const next of view.roles) {
			if (!user.isInRole(next)) {
				hasRoles = false;
				break;
			}
		}
		if (hasRoles) {
			views.push(view);
		}
	} else if (view.role && user.isInRole(view.role)) {
		views.push(view);
	} else if (view.role === undefined) {
		views.push(view);
	}
	let duplication = false;
	for (let i = 0; i < views.length; i++) {
		for (let j = 0; j < views.length; j++) {
			if (i !== j) {
				if (views[i].id === views[j].id) {
					if (views[i].link !== views[j].link) {
						console.error('Duplication at view with id: [' + views[i].id + '] pointing to links: ['
							+ views[i].link + '] and [' + views[j].link + ']');
					}
					duplication = true;
					break;
				}
			}
		}
		if (duplication) {
			break;
		}
	}
}
response.setContentType("application/json");
setETag();
response.println(JSON.stringify(views));
