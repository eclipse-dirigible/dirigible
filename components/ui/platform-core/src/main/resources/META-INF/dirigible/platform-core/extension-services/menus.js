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
import { getHelpMenu } from './modules/help-menu.mjs';
import { getWindowMenu } from './modules/window-menu.mjs'
import { request, response } from 'sdk/http';
import { extensions } from 'sdk/extensions';
import { uuid } from 'sdk/utils';

let mainmenu = [];
const extensionPoints = (request.getParameter('extensionPoints') || 'platform-menus').split(',');
const perspectiveExtPoints = (request.getParameter('perspectiveExtPoints') || 'platform-perspectives').split(',');
const viewExtPoints = (request.getParameter('viewExtPoints') || 'platform-views').split(',');
const shellExtPoints = (request.getParameter('shellExtPoints') || 'platform-shells').split(',');

let menuExtensions = [];
for (let i = 0; i < extensionPoints.length; i++) {
	const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
	for (let e = 0; e < extensionList.length; e++) {
		menuExtensions.push(extensionList[e]);
	}
}

function setETag() {
	const maxAge = 30 * 24 * 60 * 60;
	const etag = uuid.random();
	response.setHeader('ETag', etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

for (let i = 0; i < menuExtensions?.length; i++) {
	const menu = menuExtensions[i].getMenu();
	mainmenu.push(menu);
}
// System menus
mainmenu.push(await getWindowMenu(perspectiveExtPoints, viewExtPoints, shellExtPoints));
mainmenu.push(getHelpMenu());

response.setContentType('application/json');
setETag();
response.println(JSON.stringify(mainmenu));
response.flush();
response.close();