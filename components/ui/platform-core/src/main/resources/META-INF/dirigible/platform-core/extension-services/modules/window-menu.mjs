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
import { getPerspectives } from './perspectives.mjs';
import { getViews } from './views.mjs';
import { getShells } from './shells.mjs';

export async function getWindowMenu(perspectiveExtPoints = [], viewExtPoints = [], shellExtPoints = []) {
	const perspectives = await getPerspectives(perspectiveExtPoints);
	const views = await getViews(viewExtPoints);
	const shells = await getShells(shellExtPoints);

	const menu = {
		label: 'Window',
		items: [
			{
				label: 'Shells',
				items: [],
			},
			{
				label: 'Perspectives',
				items: [],
			},
			{
				label: 'Views',
				items: [],
			},
		]
	};

	for (let i = 0; i < shells.length; i++) {
		menu.items[0].items.push({
			id: shells[i].id,
			label: shells[i].label,
			link: shells[i].path,
			action: 'open',
		});
	}

	for (let p = 0; p < perspectives.perspectives.length; p++) {
		menu.items[1].items.push({
			id: perspectives.perspectives[p].id,
			label: perspectives.perspectives[p].label,
			action: 'showPerspective',
		});
	}

	if (menu.items[1].items.length) {
		menu.items[1].items[menu.items[1].items.length - 1].separator = true;
	}

	for (let u = 0; u < perspectives.utilities.length; u++) {
		menu.items[1].items.push({
			id: perspectives.utilities[u].id,
			label: perspectives.utilities[u].label,
			action: 'showPerspective',
		});
	}

	for (let i = 0; i < views.length; i++) {
		menu.items[2].items.push({
			id: views[i].id,
			label: views[i].label,
			action: 'openView',
		});
	}

	return {
		systemMenu: true,
		id: 'window',
		menu: menu
	};
}