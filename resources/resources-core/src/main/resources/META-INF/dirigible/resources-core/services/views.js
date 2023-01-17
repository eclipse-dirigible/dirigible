/*
 * Copyright (c) 2010-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *   SAP - initial API and implementation
 */
let extensions = require('core/v4/extensions');
let response = require('http/v4/response');
let request = require('http/v4/request');
let uuid = require('utils/v4/uuid');

let views = [];
let extensionPoint = request.getParameter('extensionPoint') || 'ide-view';
let viewExtensions = extensions.getExtensions(extensionPoint);

for (let i = 0; i < viewExtensions.length; i++) {
	let module = viewExtensions[i];
	try {
		let viewExtension = require(module);
		let view = viewExtension.getView();
		views.push(view);

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
	} catch (error) {
		console.error('Error occured while loading metadata for the view: ' + module);
		console.error(error);
	}
}
response.setContentType("application/json");
setETag();
response.println(JSON.stringify(views));

function setETag() {
	let maxAge = 30 * 24 * 60 * 60;
	let etag = uuid.random();
	response.setHeader("ETag", etag);
	response.setHeader('Cache-Control', `public, must-revalidate, max-age=${maxAge}`);
}