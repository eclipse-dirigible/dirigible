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
import { extensions } from "sdk/extensions";

const allThemes = [];
const themeExtensions = await extensions.loadExtensionModules('ide-themes');
try {
	for (let i = 0; i < themeExtensions?.length; i++) {
		allThemes.push(themeExtensions[i].getTheme());
	}
} catch (e) {
	console.error('Error while loading theme modules: ' + e);
}

export const getThemes = (legacy = true) => {
	let themes = [];
	for (const theme of allThemes) {
		if (legacy) {
			if (!('type' in theme)) themes.push(theme);
		}
		else if ('type' in theme) themes.push(theme);
	}
	return sort(themes);
};

function sort(themes) {
	return themes.sort(function (a, b) {
		if (a.order !== undefined && b.order !== undefined) {
			return a.order - b.order;
		} else if (a.order !== undefined) {
			return -1;
		} else if (b.order !== undefined) {
			return 1;
		} else if (a.name > b.name) {
			return 1;
		} else if (a.name < b.name) {
			return -1;
		}
		return 0;
	});
}
