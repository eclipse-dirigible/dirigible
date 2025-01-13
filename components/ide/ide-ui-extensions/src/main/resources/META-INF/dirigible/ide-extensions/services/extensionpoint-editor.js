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
const editorData = {
	id: "extensionpoint",
	label: "Extension Point",
	factory: "frame",
	region: "center",
	link: "../ide-extensions/editors/extensionpoint/editor.html",
	contentTypes: ["application/json+extension-point"]
};
if (typeof exports !== 'undefined') {
	exports.getEditor = function () {
		return editorData;
	}
}