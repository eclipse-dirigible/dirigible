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
 * API Searcher
 *
 */

const IndexingFacade = Java.type("org.eclipse.dirigible.components.api.indexing.IndexingFacade");

export class Searcher {

	public static search(index: string, term: string): { [key: string]: string }[] {
		const results = IndexingFacade.search(index, term);
		return JSON.parse(results);
	}

	public static before(index: string, date: Date): { [key: string]: string }[] {
		const results = IndexingFacade.before(index, '' + date.getTime());
		return JSON.parse(results);
	}

	public static after(index: string, date: Date): { [key: string]: string }[] {
		const results = IndexingFacade.after(index, '' + date.getTime());
		return JSON.parse(results);
	}

	public static between(index: string, lower: Date, upper: Date): { [key: string]: string }[] {
		const results = IndexingFacade.between(index, '' + lower.getTime(), '' + upper.getTime());
		return JSON.parse(results);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Searcher;
}
