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
const editorData = {
	id: 'monaco',
	region: 'center',
	label: 'Code Editor',
	path: '/services/web/editor-monaco/editor.html',
	defaultEditor: true,
	contentTypes: [
		'image/svg+xml',
		'text/plain',
		'text/html',
		'text/csv',
		'text/css',
		'application/javascript',
		'application/json',
		'application/json+extension-point',
		'application/json+extension',
		'application/json+table',
		'application/json+view',
		'application/json+job',
		'application/json+xsjob',
		'application/json+xsaccess',
		'application/json+listener',
		'application/json+websocket',
		'application/json+access',
		'application/json+roles',
		'application/json+csvim',
		'application/json+report',
		'application/hdbti',
		'application/xml',
		'application/bpmn+xml',
		'application/database-schema-model+xml',
		'application/entity-data-model+xml',
		'application/data-mapping-model+xml',
		'application/data-mapping-model+xml',
		'application/json+form',
		'application/xml+calculationview',
		'application/json+hdi'
	]
};
if (typeof exports !== 'undefined') {
	exports.getEditor = () => editorData;
}