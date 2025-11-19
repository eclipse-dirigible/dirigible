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
import { extensions } from '@aerokit/sdk/extensions';

function sortViews(a, b) {
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

export async function getViews(extensionPoints = []) {
	const views = [];
	const viewExtensions = [];
	for (let i = 0; i < extensionPoints.length; i++) {
		const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
		viewExtensions.push(...extensionList);
	}

	viewLoop: for (let i = 0; i < viewExtensions?.length; i++) {
		const view = viewExtensions[i].getView();
		if (!view.id) {
			console.error(`View ['${view.label || view.path}'] does not have an id.`);
		} else if (!view.label) {
			console.error(`View ['${view.id}'] does not have a label.`);
		} else if (!view.path) {
			console.error(`View ['${view.id}'] does not have a path.`);
		} else {
			for (let v = 0; v < views.length; v++) {
				if (views[v].id === view.id) {
					console.error(`Duplication at view with id: ['${views[v].id}'] pointing to paths: ['${views[v].path}'] and ['${view.path}']`);
					continue viewLoop;
				}
			}
			if (!view.region) view.autoFocusTab = false;
			else if ((view.region === 'center' || view.region === 'bottom') && !view.hasOwnProperty('autoFocusTab')) view.autoFocusTab = true;
			else if ((view.region === 'left' || view.region === 'right') && !view.hasOwnProperty('autoFocusTab')) view.autoFocusTab = false;
			views.push(view);
		}
	}

	return views.sort(sortViews);
}
