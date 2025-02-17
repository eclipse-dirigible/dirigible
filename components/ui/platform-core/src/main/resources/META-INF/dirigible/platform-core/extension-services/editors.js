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

const editors = [];
const extensionPoints = (request.getParameter('extensionPoints') || 'platform-editors').split(',');
const editorExtensions = [];
for (let i = 0; i < extensionPoints.length; i++) {
	// @ts-ignore
	const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
	for (let e = 0; e < extensionList.length; e++) {
		editorExtensions.push(extensionList[e]);
	}
}

function setETag() {
	const maxAge = 30 * 24 * 60 * 60;
	const etag = uuid.random();
	response.setHeader("ETag", etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

editorLoop: for (let i = 0; i < editorExtensions?.length; i++) {
	const editor = editorExtensions[i].getEditor();
	for (let e = 0; e < editors.length; e++) {
		if (editors[e].id === editor.id) {
			console.error(`Duplication at editor with id: ['${editors[e].id}'] pointing to paths: ['${editors[e].path}'] and ['${editor.path}']`);
			continue editorLoop;
		}
	}
	if (editor.roles && Array.isArray(editor.roles)) {
		let hasRoles = true;
		for (const next of editor.roles) {
			if (!user.isInRole(next)) {
				hasRoles = false;
				break;
			}
		}
		if (hasRoles) {
			editors.push(editor);
		}
	} else if (editor.role && user.isInRole(editor.role)) {
		editors.push(editor);
	} else if (editor.role === undefined) {
		editors.push(editor);
	}
}

response.setContentType("application/json");
setETag();
response.println(JSON.stringify(editors));
response.flush();
response.close();