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
export const replaceAll = (string, find, replace) => {
	return string.replace(new RegExp(find, 'g'), replace);
};

export const unescapePath = (path) => {
	return path.replace(/\\/g, '');
};

export const getNameFromPath = (path) => {
	let splittedFullName = path.split("/");
	let name = splittedFullName[splittedFullName.length - 1];
	return (!name || name.lenght === 0) ? "root" : name;
};

export const formatPath = (path) => {
	path = replaceAll(path, "//", "/");
	if (!path.startsWith("/")) {
		path = "/" + path;
	}
	if (path.endsWith("/")) {
		path = path.substr(0, path.length - 1);
	}
	return path;
}