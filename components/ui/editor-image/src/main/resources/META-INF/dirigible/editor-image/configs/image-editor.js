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
	id: 'image-editor',
	region: 'center',
	label: 'Image Viewer',
	path: '/services/web/editor-image/editor.html',
	contentTypes: [
		'image/jpeg',
		'image/svg+xml',
		'image/png',
		'image/x-icon',
		'image/gif',
		'image/bmp',
		'image/webp',
	]
};
if (typeof exports !== 'undefined') {
	exports.getEditor = () => editorData;
}