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

const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");

export interface InsertParameter {
	readonly type: string;
	readonly value: any;
}

export class Insert {

	public static execute(sql: string, parameters?: (string | number | boolean | Date | InsertParameter)[], datasourceName?: string): Array<Record<string, any>> {
        const params = parameters ? JSON.stringify(parameters) : undefined;
		return DatabaseFacade.insert(sql, params, datasourceName);
	}

	public static executeMany(sql: string, parameters?: ((string | number | boolean | Date | InsertParameter)[])[], datasourceName?: string): Array<Record<string, any>> {
		const params = parameters ? JSON.stringify(parameters) : undefined;
		return DatabaseFacade.insertMany(sql, params, datasourceName, undefined);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Insert;
}
