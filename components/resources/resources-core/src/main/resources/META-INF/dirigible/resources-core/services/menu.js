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
import { request, response } from "sdk/http";
import { extensions } from "sdk/extensions";
import { uuid } from "sdk/utils";
import { user } from "sdk/security";

const extensionPoint = request.getParameter('extensionPoint') || 'ide-menu';
let mainmenu = [];
let menuExtensions = await extensions.loadExtensionModules(extensionPoint);

function setETag() {
	const maxAge = 30 * 24 * 60 * 60;
	const etag = uuid.random();
	response.setHeader("ETag", etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}

for (let i = 0; i < menuExtensions?.length; i++) {
	const menu = menuExtensions[i].getMenu();
	if (menu.role && user.isInRole(menu.role)) {
		mainmenu.push(menu);
	} else if (menu.role === undefined) {
		mainmenu.push(menu);
	}
}

mainmenu.sort(function (p, n) {
	return (parseInt(p.order) - parseInt(n.order));
});
response.setContentType("application/json");
setETag();
response.println(JSON.stringify(mainmenu));
