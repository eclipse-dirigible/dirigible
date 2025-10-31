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

const DataStoreFacade = Java.type("org.eclipse.dirigible.components.api.db.DataStoreFacade");

export class Store {

	public static save(name: string, entry: any): string | number {
		return DataStoreFacade.save(name, JSON.stringify(entry));
	}
	
	public static upsert(name: string, entry: any): void {
		DataStoreFacade.upsert(name, JSON.stringify(entry));
	}
	
	public static update(name: string, entry: any): void {
		DataStoreFacade.update(name, JSON.stringify(entry));
	}
	
	public static list(name: string, options?: Options): any[] {
		const result = DataStoreFacade.list(name, options ? JSON.stringify(options) : null);
		return JSON.parse(result);
	}
	
	public static count(name: string, options?: Options): number {
		const result = DataStoreFacade.count(name, options ? JSON.stringify(options) : null);
		return result;
	}

	public static get(name: string, id: any): any | undefined {
		const result = DataStoreFacade.get(name, id);
		return JSON.parse(result);
	};

	public static remove(name: string, id: any): void {
		DataStoreFacade.deleteEntry(name, id);
	}
	
	public static find(name: string, example: any, limit: number = 100, offset: number = 0): any[] {
			const result = DataStoreFacade.find(name, JSON.stringify(example), limit, offset);
			return JSON.parse(result);
		}
	
	public static query(name: string, limit: number = 100, offset: number = 0): any[] {
		const result = DataStoreFacade.query(name, limit, offset);
		return JSON.parse(result);
	}
	
	public static queryNative(name: string): any[] {
		const result = DataStoreFacade.queryNative(name);
		return JSON.parse(result);
	}
	
	public static getEntityName(name: string): string {
		return DataStoreFacade.getEntityName(name);
	}
	
	public static getTableName(name: string): string {
		return DataStoreFacade.getTableName(name);
	}
		
	public static getIdName(name: string): string {
		return DataStoreFacade.getIdName(name);
	}
			
	public static getIdColumn(name: string): string {
		return DataStoreFacade.getIdColumn(name);
	}

}

export interface Options {
	conditions?: Condition[],
	sorts?: Sort[],
	limit?: number,
	offset?: number,
	language?: string
}

export interface Condition {
	propertyName: string,
	operator: Operator,
	value: any | any[]
}

export enum Operator {
	EQ = "=", // Equals
	NE = "<>", // Not Equals
	GT = ">", // Greater Than
	LT = "<", // Less Than
	GE = ">=", // Greater Than or Equals
	LE = "<=", // Less Than or Equals
	LIKE = "LIKE", // SQL LIKE operator
	BETWEEN = "BETWEEN", // SQL BETWEEN operator (requires two values)
	IN = "IN" // SQL IN operator (requires a List or Array of values)
}

export interface Sort {
	propertyName: string,
	direction: Direction
}

export enum Direction {
	ASC = "ASC", // Ascending
	DESC = "DESC" // Descending
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Store;
}