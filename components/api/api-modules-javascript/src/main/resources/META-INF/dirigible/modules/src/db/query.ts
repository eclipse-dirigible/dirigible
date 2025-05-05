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
/**
 * API Query
 *
 */

const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");

export interface QueryParameter {
	readonly type: string;
	readonly value: any;
}

export interface NamedQueryParameter {
	readonly name: string;
	readonly type: string;
	readonly value: any;
}

export class Query {

	public static execute(sql: string, parameters?: (string | number | boolean | Date | QueryParameter)[], datasourceName?: string,  dateFormat?: string): any[] {
		const resultset = DatabaseFacade.query(sql, parameters ? JSON.stringify(parameters) : undefined, datasourceName, dateFormat);
		return JSON.parse(resultset);
	}
	
	public static executeNamed(sql: string, parameters?: NamedQueryParameter[], datasourceName?: string): any[] {
		const resultset = DatabaseFacade.queryNamed(sql, parameters ? JSON.stringify(parameters) : undefined, datasourceName);
		return JSON.parse(resultset);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Query;
}
