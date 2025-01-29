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

const views = [];
const extensionPoints = (request.getParameter('extensionPoints') || 'platform-views').split(',');
const viewExtensions = [];
for (let i = 0; i < extensionPoints.length; i++) {
	// @ts-ignore
	const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
	for (let e = 0; e < extensionList.length; e++) {
		viewExtensions.push(extensionList[e]);
	}
}

function setETag() {
	const maxAge = 30 * 24 * 60 * 60;
	const etag = uuid.random();
	response.setHeader("ETag", etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

viewLoop: for (let i = 0; i < viewExtensions?.length; i++) {
	const view = viewExtensions[i].getView();
	if (!view.id) {
		console.error(`View ['${view.label || view.path}'] does not have an id.`);
	} else if (!view.label) {
		console.error(`View ['${view.id}'] does not have a label.`);
	} else if (!view.path) {
		console.error(`View ['${view.id}'] does not have a path.`);
	} else {
		for (let v = 0; v < views.length; v++) {
			if (views[v].id === view.id) {
				console.error(`Duplication at view with id: ['${views[v].id}'] pointing to paths: ['${views[v].path}'] and ['${view.path}']`);
				continue viewLoop;
			}
		}
		if (!view.region) view.autoFocusTab = false;
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
	}
}

function sortViews(a, b) {
	if (a.order !== undefined && b.order !== undefined) {
		return (parseInt(a.order) - parseInt(b.order));
	} else if (a.order === undefined && b.order === undefined) {
		return a.label < b.label ? -1 : 1
	} else if (a.order === undefined) {
		return 1;
	} else if (b.order === undefined) {
		return -1;
	}
	return 0;
}

views.sort(sortViews);

response.setContentType("application/json");
setETag();
response.println(JSON.stringify(views));
response.flush();
response.close();