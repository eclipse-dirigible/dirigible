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
// @ts-nocheck
import { extensions } from "sdk/extensions";

let perspectiveExtensions = await extensions.loadExtensionModules('platform-perspectives');
let perspectiveExtensionDefinitions = [];

for (let i = 0; i < perspectiveExtensions?.length; i++) {
	if (typeof perspectiveExtensions[i].getPerspective === "function")
		perspectiveExtensionDefinitions.push(perspectiveExtensions[i].getPerspective());
}

let viewExtensions = await extensions.loadExtensionModules('platform-views');
let viewExtensionDefinitions = [];
for (let i = 0; i < viewExtensions.length; i++) {
	viewExtensionDefinitions.push(viewExtensions[i].getView());
}
export const getMenu = () => {
	const menu = {
		label: "Window",
		items: [
			{
				label: "Perspectives",
				items: [],
			},
			{
				label: "Views",
				items: [],
			},
		]
	};

	perspectiveExtensionDefinitions = perspectiveExtensionDefinitions.sort((a, b) => {
		if (a.order !== undefined && b.order !== undefined) {
			return (parseInt(a.order) - parseInt(b.order));
		} else if (a.order === undefined && b.order === undefined) {
			return a.name.toLowerCase().localeCompare(b.name.toLowerCase());
		} else if (a.order === undefined) {
			return 1;
		} else if (b.order === undefined) {
			return -1;
		}
	});

	for (let i = 0; i < perspectiveExtensionDefinitions.length; i++) {
		let perspectiveInfo = perspectiveExtensionDefinitions[i];
		menu.items[0].items.push({
			id: perspectiveInfo.id,
			label: perspectiveInfo.label,
			action: 'showPerspective',
		});
	}

	viewExtensionDefinitions = viewExtensionDefinitions.sort((a, b) => {
		if (a.order !== undefined && b.order !== undefined) {
			return (parseInt(a.order) - parseInt(b.order));
		} else if (a.order === undefined && b.order === undefined) {
			return a.label.toLowerCase().localeCompare(b.label.toLowerCase());
		} else if (a.order === undefined) {
			return 1;
		} else if (b.order === undefined) {
			return -1;
		}
	});

	for (let i = 0; i < viewExtensionDefinitions.length; i++) {
		let viewInfo = viewExtensionDefinitions[i];
		menu.items[1].items.push({
			id: viewInfo.id,
			label: viewInfo.label,
			action: 'openView',
		});
	}

	return {
		systemMenu: true,
		id: 'window',
		menu: menu
	};
}