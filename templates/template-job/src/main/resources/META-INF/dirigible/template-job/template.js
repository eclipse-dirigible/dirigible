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
		"name": "Scheduled Job",
		"description": "Scheduled Job definition with a simple Javascript handler",
		"sources": [
		{
			"location": "/template-job/job.template", 
			"action": "generate",
			"rename": "{{fileName}}.job"
		},
		{
			"location": "/template-job/handler.js.template", 
			"action": "generate",
			"rename": "{{fileName}}-handler.js"
		}],
		"parameters": []
	};
};
