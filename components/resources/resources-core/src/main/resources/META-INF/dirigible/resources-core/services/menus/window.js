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

let perspectiveExtensions = await extensions.loadExtensionModules('ide-perspective');
let perspectiveExtensionDefinitions = [];

for (let i = 0; i < perspectiveExtensions?.length; i++) {
	perspectiveExtensionDefinitions.push(perspectiveExtensions[i].getPerspective());
}

let viewExtensions = await extensions.loadExtensionModules('ide-view');
let viewExtensionDefinitions = [];
for (let i = 0; i < viewExtensions.length; i++) {
	viewExtensionDefinitions.push(viewExtensions[i].getView());
}
export const getMenu = () => {
	let menu = {
		label: "Window",
		order: 800,
		items: [
			{
				label: "Open Perspective",
				order: 1,
				items: [],
			},
			{
				label: "Show View",
				order: 2,
				items: [],
			},
		]
	};

	perspectiveExtensionDefinitions = perspectiveExtensionDefinitions.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
	for (let i = 0; i < perspectiveExtensionDefinitions.length; i++) {
		let perspectiveInfo = perspectiveExtensionDefinitions[i];
		menu.items[0].items.push({
			id: perspectiveInfo.id,
			label: perspectiveInfo.name,
			order: i,
			link: perspectiveInfo.link,
			action: 'openPerspective',
		});
	}

	viewExtensionDefinitions = viewExtensionDefinitions.sort((a, b) => a.label.toLowerCase().localeCompare(b.label.toLowerCase()));

	for (let i = 0; i < viewExtensionDefinitions.length; i++) {
		let viewInfo = viewExtensionDefinitions[i];
		menu.items[1].items.push({
			id: viewInfo.id,
			label: viewInfo.label,
			order: i,
			action: 'openView',
		});
	}

	return menu;
}