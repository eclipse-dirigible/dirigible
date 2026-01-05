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
import { cmis } from "@aerokit/sdk/cms";
let cmisSession = cmis.getSession();

export const getObject = (path) => {
	try {
		if (path === null || path === undefined) {
			return null;
		}
		return cmisSession.getObjectByPath(path);
	} catch (e) {
		console.error(`Error [${e.message}] in getting an object by path [${path}]`);
	}
	return null;
};

export const existObject = (path) => {
	try {
		if (path === null || path === undefined) {
			return null;
		}
		return cmisSession.getObjectByPath(path) != null;
	} catch (e) {
		return false
	}
};

export const getById = (id) => {
	return cmisSession.getObject(id);
};

export const deleteObject = (object) => {
	object.delete();
};

export const renameObject = (object, newName) => {
	object.rename(newName);
};
