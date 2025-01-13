/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/**
 * API Writer
 *
 */

const IndexingFacade = Java.type("org.eclipse.dirigible.components.api.indexing.IndexingFacade");

export class Writer {

	public static add(index: string, location: string, contents: string, lastModified: Date = new Date(), parameters?: { [key: string]: string }) {
		let map = "{}";
		if (parameters) {
			map = JSON.stringify(parameters);
		}
		IndexingFacade.add(index, location, contents, '' + lastModified.getTime(), map);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Writer;
}
