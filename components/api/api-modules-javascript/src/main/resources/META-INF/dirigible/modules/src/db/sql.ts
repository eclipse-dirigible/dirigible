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

import { Connection } from "./database";

const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");
const DataTypeEnum = Java.type("org.eclipse.dirigible.database.sql.DataType");

export type DataType =
	"VARCHAR"
	| "TEXT"
	| "CHAR"
	| "DATE"
	| "SECONDDATE"
	| "TIME"
	| "DATETIME"
	| "TIMESTAMP"
	| "INTEGER"
	| "INT"
	| "TINYINT"
	| "BIGINT"
	| "SMALLINT"
	| "REAL"
	| "DOUBLE"
	| "DOUBLE PRECISIO"
	| "BOOLEAN"
	| "BLOB"
	| "DECIMAL"
	| "BIT"
	| "NVARCHAR"
	| "FLOAT"
	| "BYTE"
	| "NCLOB"
	| "ARRAY"
	| "VARBINARY"
	| "BINARY VARYIN"
	| "SHORTTEXT"
	| "ALPHANUM"
	| "CLOB"
	| "SMALLDECIMAL"
	| "BINARY"
	| "ST_POINT"
	| "ST_GEOMETRY"
	| "CHARACTER VARYIN"
	| "BINARY LARG OBJECT"
	| "CHARACTER LARG OBJECT"
	| "CHARACTER"
	| "NCHAR"
	| "NUMERIC";

abstract class AbstractSQLBuilder {

	public readonly params: any[] = [];
	protected readonly connection?: Connection;
	protected native: any;

	constructor(connection?: Connection) {
		this.connection = connection;
		this.native = connection ? DatabaseFacade.getNative(connection.native) : DatabaseFacade.getDefault();
		this.native = this.prepareBuilder(this.native);
	}

	protected prepareBuilder(builder: any): any {
		return builder;
	}

	public parameters(): any[] {
		return this.params;
	}

	protected addParameter(value: any | any[]): void {
		if (Array.isArray(value)) {
			this.params.push(...value);
		} else {
			this.params.push(value);
		}
		this.params
	}

	public build(): string {
		return this.native.build();
	}
}

export class SQLBuilder extends AbstractSQLBuilder {

	public static getDialect(connection?: Connection): SQLBuilder {
		return new SQLBuilder(connection);
	}

	public select(): SelectBuilder {
		return new SelectBuilder(this.connection);
	}

	public insert(): InsertBuilder {
		return new InsertBuilder(this.connection);
	}

	public update(): UpdateBuilder {
		return new UpdateBuilder(this.connection);
	}

	public delete(): DeleteBuilder {
		return new DeleteBuilder(this.connection);
	}

	public nextval(name: string): NextvalBuilder {
		return new NextvalBuilder(name, this.connection);
	}

	public create(): CreateBuilder {
		return new CreateBuilder(this.connection);
	}

	public drop(): DropBuilder {
		return new DropBuilder(this.connection);
	}
}

export class SelectBuilder extends AbstractSQLBuilder {

	protected prepareBuilder(builder: any): any {
		return builder.select();
	}

	public distinct(): SelectBuilder {
		this.native.distinct();
		return this;
	}

	public forUpdate(): SelectBuilder {
		this.native.forUpdate();
		return this;
	}

	public column(column: string): SelectBuilder {
		this.native.column(column);
		return this;
	}

	public from(table: string, alias?: string): SelectBuilder {
		this.native.from(table, alias);
		return this;
	}

	public join(table: string, on: string, alias?: string): SelectBuilder {
		this.native.join(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public innerJoin(table: string, on: string, alias?: string): SelectBuilder {
		this.native.innerJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public outerJoin(table: string, on: string, alias?: string): SelectBuilder {
		this.native.outerJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public leftJoin(table: string, on: string, alias?: string): SelectBuilder {
		this.native.leftJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public rightJoin(table: string, on: string, alias?: string): SelectBuilder {
		this.native.rightJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public fullJoin(table: string, on: string, alias?: string): SelectBuilder {
		this.native.fullJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public where(condition: string): SelectBuilder {
		this.native.where(condition);
		this.addParameter(arguments[1]);
		return this;
	}

	public order(column: string, asc?: string): SelectBuilder {
		this.native.order(column, asc);
		return this;
	}

	public group(column: string): SelectBuilder {
		this.native.group(column);
		return this;
	}

	public limit(limit: number): SelectBuilder {
		this.native.limit(limit);
		return this;
	}

	public offset(offset: number): SelectBuilder {
		this.native.offset(offset);
		return this;
	}

	public having(having: string): SelectBuilder {
		this.native.having(having);
		return this;
	}

	public union(select: string): SelectBuilder {
		this.native.union(select);
		return this;
	}
}

export class InsertBuilder extends AbstractSQLBuilder {

	protected prepareBuilder(builder: any): any {
		return builder.insert();
	}

	public into(table: string): InsertBuilder {
		this.native.into(table);
		return this;
	}

	public column(column: string): InsertBuilder {
		this.native.column(column);
		return this;
	}

	public value(value: string): InsertBuilder {
		this.native.value(value);
		this.addParameter(arguments[1]);
		return this;
	}

	public select(select: string): InsertBuilder {
		this.native.select(select);
		return this;
	}
}

export class UpdateBuilder extends AbstractSQLBuilder {

	protected prepareBuilder(builder: any): any {
		return builder.update();
	}

	public table(table: string): UpdateBuilder {
		this.native.table(table);
		return this;
	}

	public set(column: string, value: string): UpdateBuilder {
		this.native.set(column, value);
		this.addParameter(arguments[2]);
		return this;
	}

	public where(condition: string): UpdateBuilder {
		this.native.where(condition);
		this.addParameter(arguments[1]);
		return this;
	}
}

export class DeleteBuilder extends AbstractSQLBuilder {

	protected prepareBuilder(builder: any): any {
		return builder.delete();
	}

	public from(table: string): DeleteBuilder {
		this.native.from(table);
		return this;
	}

	public where(condition: string): DeleteBuilder {
		this.native.where(condition);
		this.addParameter(arguments[1]);
		return this;
	}
}

export class NextvalBuilder extends AbstractSQLBuilder {

	private name: string;

	constructor(name: string, connection?: Connection) {
		super(connection);
		this.name = name;
	}

	protected prepareBuilder(builder: any): any {
		return builder.nextval(this.name);
	}
}

export class CreateBuilder extends AbstractSQLBuilder {

	public table(table: string): CreateTableBuilder {
		console.error(`Table in CreateBuilder is: ${table}`);
		return new CreateTableBuilder(table, this.connection);
	}

	public view(view: string): CreateViewBuilder {
		return new CreateViewBuilder(view, this.connection);
	}

	public sequence(sequence: string): CreateSequenceBuilder {
		return new CreateSequenceBuilder(sequence, this.connection)
	}
}

export class CreateTableBuilder extends AbstractSQLBuilder {

	constructor(table: string, connection?: Connection) {
		super(connection);
		this.native = this.native.create().table(table);
	}

	public column(name: string, type: DataType, isPrimaryKey = false, isNullable = true, isUnique = false, isIdentity = false, isFuzzyIndexEnabled = false, ...args: string[]): CreateTableBuilder {
		const dataType = DataTypeEnum.valueOfByName(type);
		this.native.column(name, dataType, isPrimaryKey, isNullable, isUnique, isIdentity, isFuzzyIndexEnabled, Array.from(args));
		return this;
	}

	public columnVarchar(name: string, length: number, isPrimaryKey = false, isNullable = true, isUnique = false, isIdentity = false, ...args: string[]): CreateTableBuilder {
		this.native.columnVarchar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	public columnNvarchar(name: string, length: number, isPrimaryKey = false, isNullable = true, isUnique = false, isIdentity = false, ...args: string[]): CreateTableBuilder {
		this.native.columnNvarchar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	public columnChar(name: string, length: number, isPrimaryKey = false, isNullable = true, isUnique = false, isIdentity = false, ...args: string[]): CreateTableBuilder {
		this.native.columnChar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	public columnDate = function (name: string, isPrimaryKey = false, isNullable = true, isUnique = false, ...args: string[]): CreateTableBuilder {
		this.native.columnDate(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	public columnTime(name: string, isPrimaryKey = false, isNullable = true, isUnique = false, ...args: string[]): CreateTableBuilder {
		this.native.columnTime(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	public columnTimestamp(name: string, isPrimaryKey = false, isNullable = true, isUnique = false, ...args: string[]): CreateTableBuilder {
		this.native.columnTimestamp(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	public columnInteger(name: string, isPrimaryKey = false, isNullable = true, isUnique = false, isIdentity = false, ...args: string[]): CreateTableBuilder {
		this.native.columnInteger(name, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	public columnTinyint(name: string, isPrimaryKey = false, isNullable = true, isUnique = false, ...args: string[]): CreateTableBuilder {
		this.native.columnTinyint(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	public columnBigint(name: string, isPrimaryKey = false, isNullable = true, isUnique = false, isIdentity = false, ...args: string[]): CreateTableBuilder {
		this.native.columnBigint(name, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	public columnSmallint(name: string, isPrimaryKey = false, isNullable = true, isUnique = false, ...args: string[]): CreateTableBuilder {
		this.native.columnSmallint(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	public columnReal(name: string, isPrimaryKey = false, isNullable = true, isUnique = false, ...args: string[]): CreateTableBuilder {
		this.native.columnReal(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	public columnDouble(name: string, isPrimaryKey = false, isNullable = true, isUnique = false, ...args: string[]): CreateTableBuilder {
		this.native.columnDouble(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	public columnBoolean(name: string, isPrimaryKey = false, isNullable = true, isUnique = false, ...args: string[]): CreateTableBuilder {
		this.native.columnBoolean(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	public columnBlob(name: string, isNullable = true, ...args: string[]): CreateTableBuilder {
		this.native.columnBlob(name, isNullable, Array.from(args));
		return this;
	}

	public columnDecimal(name: string, precision: number, scale: number, isPrimaryKey = false, isNullable = true, isUnique = false, isIdentity = false, ...args: string[]): CreateTableBuilder {
		this.native.columnDecimal(name, precision, scale, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	public primaryKey(columns: string[], name?: string): CreateTableBuilder {
		this.native.primaryKey(name, columns);
		return this;
	}

	public foreignKey(name: string, columns: string[], referencedTable: string, referencedColumns: string[], referencedTableSchema?: string): CreateTableBuilder {
		this.native.foreignKey(name, columns, referencedTable, referencedTableSchema, referencedColumns);
		return this;
	}

	public unique(name: string, columns: string[]): CreateTableBuilder {
		this.native.unique(name, columns);
		return this;
	}

	public check(name: string, expression: string): CreateTableBuilder {
		this.native.check(name, expression);
		return this;
	}
}

export class CreateViewBuilder extends AbstractSQLBuilder {

	constructor(view: string, connection?: Connection) {
		super(connection);
		this.native = this.native.create().view(view);
	}

	public column(column: string): CreateViewBuilder {
		this.native.column(column);
		return this;
	}

	public asSelect(select: string): CreateViewBuilder {
		this.native.asSelect(select);
		return this;
	}
}

export class CreateSequenceBuilder extends AbstractSQLBuilder {

	constructor(sequence: string, connection?: Connection) {
		super(connection);
		this.native = this.native.create().sequence(sequence);
	}
}

export class DropBuilder extends AbstractSQLBuilder {

	public table(table: string): DropTableBuilder {
		return new DropTableBuilder(table, this.connection);
	}

	public view(view: string): DropViewBuilder {
		return new DropViewBuilder(view, this.connection);
	}

	public sequence(sequence: string): DropSequenceBuilder {
		return new DropSequenceBuilder(sequence, this.connection);
	};

}

export class DropTableBuilder extends AbstractSQLBuilder {

	constructor(table: string, connection?: Connection) {
		super(connection);
		this.native = this.native.drop().table(table);
	}
}

export class DropViewBuilder extends AbstractSQLBuilder {

	constructor(view: string, connection?: Connection) {
		super(connection);
		this.native = this.native.drop().view(view);
	}
}

export class DropSequenceBuilder extends AbstractSQLBuilder {

	constructor(sequence: string, connection?: Connection) {
		super(connection);
		this.native = this.native.drop().sequence(sequence);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = SQLBuilder;
}
