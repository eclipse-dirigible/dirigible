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
import { user } from '@aerokit/sdk/security';

function sortProjections(a, b) {
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

/**
 * Aggregates the `application-projections` extension point: named lenses over the deployed
 * module set, each selecting a subset of the navigation groups (and/or individual perspectives)
 * for the application shell's product switcher.
 *
 * A projection config (`exports.getProjection = () => ({...})`) carries:
 * - id, label, description?, icon?, order? — the product-switch entry;
 * - groups: [group ids included wholesale], items: [perspective ids cherry-picked];
 * - all: true — the "everything" entry (no filtering applied when selected);
 * - roles: [...] — offered only to users holding at least one of these (filtered here,
 *   server-side, so gated entries are never shipped to the browser).
 */
export async function getProjections(extensionPoints = []) {
	const projectionExtensions = [];
	for (let i = 0; i < extensionPoints.length; i++) {
		const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
		projectionExtensions.push(...extensionList);
	}

	const ids = new Set([]);
	const projections = [];

	for (let i = 0; i < projectionExtensions.length; i++) {
		if (typeof projectionExtensions[i].getProjection !== 'function') {
			continue;
		}
		const projection = projectionExtensions[i].getProjection();
		if (ids.has(projection.id)) {
			console.error(`Projection with non-unique id: ['${projection.id}'].`);
			continue;
		}
		ids.add(projection.id);
		if (Array.isArray(projection.roles) && projection.roles.length > 0
				&& !projection.roles.some(role => user.isInRole(role))) {
			continue;
		}
		projections.push(projection);
	}

	projections.sort(sortProjections);
	return { projections: projections };
}
