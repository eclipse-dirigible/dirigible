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
// @ts-nocheck
import { extensions } from 'sdk/extensions';
import { request, response } from 'sdk/http';
import { uuid } from 'sdk/utils';
import { user } from 'sdk/security';


const perspectives = [];
const sidebarConfig = {
	perspectives: [],
	utilities: [],
};
const extensionPoints = (request.getParameter('extensionPoints') || 'platform-perspectives').split(',');
const perspectiveExtensions = [];
for (let i = 0; i < extensionPoints.length; i++) {
	// @ts-ignore
	const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
	for (let e = 0; e < extensionList.length; e++) {
		perspectiveExtensions.push(extensionList[e]);
	}
}

function setETag() {
	const maxAge = 30 * 24 * 60 * 60;
	const etag = uuid.random();
	response.setHeader('ETag', etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

function sortPerspectives(a, b) {
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

const pIds = new Set([]);

perspectiveLoop: for (let i = 0; i < perspectiveExtensions?.length; i++) {
	let perspective;
	if (typeof perspectiveExtensions[i].getPerspectiveGroup === 'function') {
		perspective = perspectiveExtensions[i].getPerspectiveGroup();
	} else if (typeof perspectiveExtensions[i].getUtilityPerspective === 'function') {
		perspective = perspectiveExtensions[i].getUtilityPerspective();
		perspective.isUtility = true;
	} else {
		perspective = perspectiveExtensions[i].getPerspective();
	}
	if (pIds.has(perspective.id)) {
		console.error(`Perspective with non-unique id: ['${perspective.id}'] with path: ['${perspective.path}'].`);
		continue perspectiveLoop;
	}
	pIds.add(perspective.id);
	if (perspective.roles && Array.isArray(perspective.roles)) {
		let hasRoles = true;
		for (const next of perspective.roles) {
			if (!user.isInRole(next)) {
				hasRoles = false;
				break;
			}
		}
		if (hasRoles) {
			if (perspective.isUtility) {
				sidebarConfig.utilities.push(perspective);
			} else if (perspective.items) {
				sidebarConfig.perspectives.push(perspective);
			} else perspectives.push(perspective);
		}
	} else if (perspective.role && user.isInRole(perspective.role)) {
		if (perspective.isUtility) {
			sidebarConfig.utilities.push(perspective);
		} else if (perspective.items) {
			sidebarConfig.perspectives.push(perspective);
		} else perspectives.push(perspective);
	} else if (perspective.role === undefined) {
		if (perspective.isUtility) {
			sidebarConfig.utilities.push(perspective);
		} else if (perspective.items) {
			sidebarConfig.perspectives.push(perspective);
		} else perspectives.push(perspective);
	}
}

response.setContentType('application/json');
for (let i = 0; i < perspectives.length; i++) {
	if (perspectives[i].groupId) {
		for (let g = 0; g < sidebarConfig.perspectives.length; g++) {
			if (perspectives[i].groupId === sidebarConfig.perspectives[g].id) {
				sidebarConfig.perspectives[g].items.push(perspectives[i]);
				sidebarConfig.perspectives[g].items.sort(sortPerspectives);
				break;
			}
		}
	} else sidebarConfig.perspectives.push(perspectives[i]);
}
sidebarConfig.perspectives.sort(sortPerspectives);
sidebarConfig.utilities.sort(sortPerspectives);
setETag();
response.println(JSON.stringify(sidebarConfig));
response.flush();
response.close();