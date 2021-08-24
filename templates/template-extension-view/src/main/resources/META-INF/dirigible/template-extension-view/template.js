/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
exports.getTemplate = function() {
	return {
		"name": "Extension (View)",
		"description": "Extension view for the IDE",
		"sources": [{
			"location": "/template-extension-view/views/view/index.html.template", 
			"action": "copy",
			"rename": "views/{{fileName}}/index.html"
		}, {
			"location": "/template-extension-view/views/view/controller.js.template", 
			"action": "generate",
			"rename": "views/{{fileName}}/controller.js"
		}, {
			"location": "/template-extension-view/views/view/view.js.template", 
			"action": "generate",
			"rename": "views/{{fileName}}/view.js"
		}, {
			"location": "/template-extension-view/extensions/views/view.extension.template", 
			"action": "generate",
			"rename": "extensions/views/{{fileName}}.extension"
		}],
		"parameters": [{
			"name": "viewName",
			"label": "Name"
		}]
	};
};
