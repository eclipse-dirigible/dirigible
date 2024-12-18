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

const shells = [];
const extensionPoints = (request.getParameter('extensionPoints') || 'platform-shells').split(',');
const shellExtensions = [];
for (let i = 0; i < extensionPoints.length; i++) {
	// @ts-ignore
	const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
	for (let e = 0; e < extensionList.length; e++) {
		shellExtensions.push(extensionList[e]);
	}
}

function setETag() {
	const maxAge = 30 * 24 * 60 * 60;
	const etag = uuid.random();
	response.setHeader('ETag', etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

shellLoop: for (let i = 0; i < shellExtensions?.length; i++) {
	const shell = shellExtensions[i].getShell();
	if (!shell.id) {
		console.error(`Shell ['${shell.label || shell.path}'] does not have an id.`);
	} else if (!shell.label) {
		console.error(`Shell ['${shell.id}'] does not have a label.`);
	} else if (!shell.path) {
		console.error(`Shell ['${shell.id}'] does not have a path.`);
	} else {
		for (let v = 0; v < shells.length; v++) {
			if (shells[v].id === shell.id) {
				console.error(`Duplication at shell with id: ['${shells[v].id}'] pointing to paths: ['${shells[v].path}'] and ['${shell.path}']`);
				continue shellLoop;
			}
		}
		if (shell.roles && Array.isArray(shell.roles)) {
			let hasRoles = true;
			for (const next of shell.roles) {
				if (!user.isInRole(next)) {
					hasRoles = false;
					break;
				}
			}
			if (hasRoles) {
				shells.push(shell);
			}
		} else if (shell.role && user.isInRole(shell.role)) {
			shells.push(shell);
		} else if (shell.role === undefined) {
			shells.push(shell);
		}
	}
}

function sortShells(a, b) {
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

shells.sort(sortShells);

response.setContentType("application/json");
setETag();
response.println(JSON.stringify(shells));
response.flush();
response.close();