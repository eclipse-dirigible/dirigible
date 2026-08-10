/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { extensions } from '@aerokit/sdk/extensions';

function sortPerspectives(a, b) {
	if (a.order !== undefined && b.order !== undefined) {
		return (parseInt(a.order) - parseInt(b.order));
	} else if (a.order === undefined && b.order === undefined) {
		return a.label.toLowerCase().localeCompare(b.label.toLowerCase());
	} else if (a.order === undefined) {
		return 1;
	} else if (b.order === undefined) {
		return -1;
	}
	return 0;
}

export async function getPerspectives(extensionPoints = []) {
	const perspectives = [];
	const sidebarConfig = {
		perspectives: [],
		utilities: [],
	};
	const perspectiveExtensions = [];
	for (let i = 0; i < extensionPoints.length; i++) {
		const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
		perspectiveExtensions.push(...extensionList);
	}

	const pIds = new Set([]);

	perspectiveLoop: for (let i = 0; i < perspectiveExtensions?.length; i++) {
		let perspective;
		if (typeof perspectiveExtensions[i].getPerspectiveGroup === 'function') {
			perspective = perspectiveExtensions[i].getPerspectiveGroup();
		} else if (typeof perspectiveExtensions[i].getUtilityPerspective === 'function') {
			perspective = perspectiveExtensions[i].getUtilityPerspective();
			perspective.isUtility = true;
		} else {
			perspective = perspectiveExtensions[i].getPerspective();
		}
		if (pIds.has(perspective.id)) {
			console.error(`Perspective with non-unique id: ['${perspective.id}'] with path: ['${perspective.path}'].`);
			continue perspectiveLoop;
		}
		pIds.add(perspective.id);
		if (perspective.isUtility) {
			sidebarConfig.utilities.push(perspective);
		} else if (perspective.items) {
			sidebarConfig.perspectives.push(perspective);
		} else perspectives.push(perspective);
	}

	// The declared navigation groups, snapshotted before the placement loop so that a perspective
	// handed back un-grouped below can never be mistaken for a group.
	const groups = sidebarConfig.perspectives.filter(group => Array.isArray(group.items));
	// A group may declare itself the default one of its extension point. A shell with a single
	// well-known navigation group owns the placement, so its perspectives need not repeat the group
	// id - and a rename of that group can never invalidate an already-generated module.
	const defaultGroup = groups.find(group => group.isDefault);
	// The platform's catch-all group, used only for a groupId that matches nothing.
	const unknownGroup = groups.find(group => group.id === 'undefined-group');

	for (const perspective of perspectives) {
		let group;
		if (perspective.groupId) {
			group = groups.find(candidate => candidate.id === perspective.groupId);
			if (!group) {
				// A groupId matching no group is authoring/rename drift. Neither the perspective nor the
				// diagnosis may be lost over it: a silently dropped perspective is indistinguishable from
				// a module that contributes none.
				console.error(`Perspective ['${perspective.id}'] with path ['${perspective.path}'] declares groupId ['${perspective.groupId}'], which matches no perspective group of this extension point.`);
				group = defaultGroup ?? unknownGroup;
			}
		} else {
			group = defaultGroup;
		}
		if (group) {
			group.items.push(perspective);
			group.items.sort(sortPerspectives);
		} else {
			// Nothing to place it in: hand the perspective back standalone so the consuming shell can
			// still render it, rather than dropping it here.
			sidebarConfig.perspectives.push(perspective);
		}
	}
	sidebarConfig.perspectives.sort(sortPerspectives);
	sidebarConfig.utilities.sort(sortPerspectives);
	return sidebarConfig;
}
