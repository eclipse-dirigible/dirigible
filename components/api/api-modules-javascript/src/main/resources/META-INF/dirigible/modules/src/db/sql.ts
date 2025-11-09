import { Connection } from "./database";

// Assumed imports for the native Java API calls
const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");
const DataTypeEnum = Java.type("org.eclipse.dirigible.database.sql.DataType");

/**
 * Union type representing all supported SQL data types.
 */
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

/**
 * Abstract base class for all SQL builders. Handles connection, native facade,
 * and parameter collection.
 */
abstract class AbstractSQLBuilder {

	public readonly params: any[] = [];
	protected readonly connection?: Connection;
	protected native: any; // The underlying Java SQL Builder object

	constructor(connection?: Connection) {
		this.connection = connection;
		// Initialize the native builder from the facade
		this.native = connection ? DatabaseFacade.getNative((connection as any).native) : DatabaseFacade.getDefault();
		this.native = this.prepareBuilder(this.native);
	}

	/**
	 * Hook for subclasses to set up the native builder object (e.g., calling .select()).
	 */
	protected prepareBuilder(builder: any): any {
		return builder;
	}

	public parameters(): any[] {
		return this.params;
	}

	/**
	 * Adds parameter(s) to the internal list. Handles single values and arrays of values.
	 */
	protected addParameter(value: any | any[]): void {
		if (value === undefined || value === null) {
			return; // Do not add undefined/null unless explicitly an array
		}
		if (Array.isArray(value)) {
			this.params.push(...value);
		} else {
			this.params.push(value);
		}
	}

	/**
	 * Builds and returns the final SQL string.
	 */
	public build(): string {
		return this.native.build();
	}
}

/**
 * Main entry point for the SQL Builder. Acts as a factory for specific builders.
 */
export class SQLBuilder extends AbstractSQLBuilder {

	/**
	 * Factory method to get a dialect-specific SQLBuilder instance.
	 */
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

/**
 * Builder for SELECT statements.
 */
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

	// NOTE: The original JS logic for parameter handling in joins is complex and relies on
	// argument count. We replicate this behavior by using the optional parameters argument.

	public join(table: string, on: string, alias?: string, parameters?: any | any[]): SelectBuilder {
		this.native.join(table, on, alias);
		// arguments[3] is the 4th argument (parameters when alias is present).
		// arguments[2] is the 3rd argument (parameters when alias is absent).
		// We use arguments array to maintain compatibility with the original JS implementation.
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else if (arguments.length > 2) {
			// This branch is highly dependent on how the original code was used:
			// if (alias === undefined && parameters === array): parameters are at arguments[2]
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public innerJoin(table: string, on: string, alias?: string, parameters?: any | any[]): SelectBuilder {
		this.native.innerJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else if (arguments.length > 2) {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public outerJoin(table: string, on: string, alias?: string, parameters?: any | any[]): SelectBuilder {
		this.native.outerJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else if (arguments.length > 2) {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public leftJoin(table: string, on: string, alias?: string, parameters?: any | any[]): SelectBuilder {
		this.native.leftJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else if (arguments.length > 2) {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public rightJoin(table: string, on: string, alias?: string, parameters?: any | any[]): SelectBuilder {
		this.native.rightJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else if (arguments.length > 2) {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	public fullJoin(table: string, on: string, alias?: string, parameters?: any | any[]): SelectBuilder {
		this.native.fullJoin(table, on, alias);
		if (arguments.length > 3) {
			this.addParameter(arguments[3]);
		} else if (arguments.length > 2) {
			this.addParameter(arguments[2]);
		}
		return this;
	}

	/**
	 * Sets the WHERE condition.
	 * @param condition The SQL condition string (e.g., "column1 = ?").
	 * @param parameters Optional parameters to replace '?' in the condition.
	 */
	public where(condition: string, parameters?: any | any[]): SelectBuilder {
		this.native.where(condition);
		this.addParameter(parameters); // arguments[1] in JS
		return this;
	}

	public order(column: string, asc: boolean = true): SelectBuilder {
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

/**
 * Builder for INSERT statements.
 */
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

	/**
	 * Sets the value for the last column specified.
	 * @param value The value placeholder (e.g., "?") or literal.
	 * @param parameters Optional parameters if a placeholder was used.
	 */
	public value(value: string, parameters?: any | any[]): InsertBuilder {
		this.native.value(value);
		this.addParameter(parameters); // arguments[1] in JS
		return this;
	}

	public select(select: string): InsertBuilder {
		this.native.select(select);
		return this;
	}
}

/**
 * Builder for UPDATE statements.
 */
export class UpdateBuilder extends AbstractSQLBuilder {

	protected prepareBuilder(builder: any): any {
		return builder.update();
	}

	public table(table: string): UpdateBuilder {
		this.native.table(table);
		return this;
	}

	/**
	 * Sets a column to a value.
	 * @param column The column name.
	 * @param value The value placeholder (e.g., "?") or literal.
	 * @param parameters Optional parameters if a placeholder was used.
	 */
	public set(column: string, value: string, parameters?: any | any[]): UpdateBuilder {
		this.native.set(column, value);
		this.addParameter(parameters); // arguments[2] in JS
		return this;
	}

	/**
	 * Sets the WHERE condition for the update.
	 * @param condition The SQL condition string (e.g., "column1 = ?").
	 * @param parameters Optional parameters to replace '?' in the condition.
	 */
	public where(condition: string, parameters?: any | any[]): UpdateBuilder {
		this.native.where(condition);
		this.addParameter(parameters); // arguments[1] in JS
		return this;
	}
}

/**
 * Builder for DELETE statements.
 */
export class DeleteBuilder extends AbstractSQLBuilder {

	protected prepareBuilder(builder: any): any {
		return builder.delete();
	}

	public from(table: string): DeleteBuilder {
		this.native.from(table);
		return this;
	}

	/**
	 * Sets the WHERE condition for the deletion.
	 * @param condition The SQL condition string (e.g., "column1 = ?").
	 * @param parameters Optional parameters to replace '?' in the condition.
	 */
	public where(condition: string, parameters?: any | any[]): DeleteBuilder {
		this.native.where(condition);
		this.addParameter(parameters); // arguments[1] in JS
		return this;
	}
}

/**
 * Builder for selecting the next value from a sequence.
 */
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

/**
 * Builder for CREATE statements (Table, View, Sequence).
 */
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

/**
 * Builder for CREATE TABLE statements.
 */
export class CreateTableBuilder extends AbstractSQLBuilder {

	// Overrides constructor to immediately start the native table builder
	constructor(table: string, connection?: Connection) {
		super(connection);
		this.native = this.native.create().table(table);
	}

	/**
	 * Adds a generic column definition.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public column(
		name: string, 
		type: DataType, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		isIdentity = false, 
		isFuzzyIndexEnabled = false, 
		...args: string[]
	): CreateTableBuilder {
		const dataType = DataTypeEnum.valueOfByName(type);
		this.native.column(name, dataType, isPrimaryKey, isNullable, isUnique, isIdentity, isFuzzyIndexEnabled, Array.from(args));
		return this;
	}

	/**
	 * Adds a VARCHAR column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnVarchar(
		name: string, 
		length: number, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		isIdentity = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnVarchar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	/**
	 * Adds an NVARCHAR column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnNvarchar(
		name: string, 
		length: number, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		isIdentity = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnNvarchar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	/**
	 * Adds a CHAR column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnChar(
		name: string, 
		length: number, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		isIdentity = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnChar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	/**
	 * Adds a DATE column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnDate(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnDate(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	/**
	 * Adds a TIME column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnTime(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnTime(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	/**
	 * Adds a TIMESTAMP column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnTimestamp(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnTimestamp(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	/**
	 * Adds an INTEGER column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnInteger(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		isIdentity = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnInteger(name, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	/**
	 * Adds a TINYINT column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnTinyint(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnTinyint(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	/**
	 * Adds a BIGINT column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnBigint(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		isIdentity = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnBigint(name, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	/**
	 * Adds a SMALLINT column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnSmallint(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnSmallint(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	/**
	 * Adds a REAL column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnReal(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnReal(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	/**
	 * Adds a DOUBLE column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnDouble(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnDouble(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	/**
	 * Adds a BOOLEAN column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnBoolean(
		name: string, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnBoolean(name, isPrimaryKey, isNullable, isUnique, Array.from(args));
		return this;
	}

	/**
	 * Adds a BLOB column.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnBlob(
		name: string, 
		isNullable = true, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnBlob(name, isNullable, Array.from(args));
		return this;
	}

	/**
	 * Adds a DECIMAL column with precision and scale.
	 * @param args Additional dialect-specific arguments passed as an array to native.
	 */
	public columnDecimal(
		name: string, 
		precision: number, 
		scale: number, 
		isPrimaryKey = false, 
		isNullable = true, 
		isUnique = false, 
		isIdentity = false, 
		...args: string[]
	): CreateTableBuilder {
		this.native.columnDecimal(name, precision, scale, isPrimaryKey, isNullable, isUnique, isIdentity, Array.from(args));
		return this;
	}

	public primaryKey(columns: string[], name?: string): CreateTableBuilder {
		this.native.primaryKey(name, columns);
		return this;
	}

	public foreignKey(name: string, columns: string[], referencedTable: string, referencedColumns: string[], referencedTableSchema?: string): CreateTableBuilder {
		// NOTE: The native function expects the schema before the referenced columns
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

/**
 * Builder for CREATE VIEW statements.
 */
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

/**
 * Builder for CREATE SEQUENCE statements.
 */
export class CreateSequenceBuilder extends AbstractSQLBuilder {

	constructor(sequence: string, connection?: Connection) {
		super(connection);
		this.native = this.native.create().sequence(sequence);
	}
}

/**
 * Builder for DROP statements (Table, View, Sequence).
 */
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

/**
 * Builder for DROP TABLE statements.
 */
export class DropTableBuilder extends AbstractSQLBuilder {

	constructor(table: string, connection?: Connection) {
		super(connection);
		this.native = this.native.drop().table(table);
	}
}

/**
 * Builder for DROP VIEW statements.
 */
export class DropViewBuilder extends AbstractSQLBuilder {

	constructor(view: string, connection?: Connection) {
		super(connection);
		this.native = this.native.drop().view(view);
	}
}

/**
 * Builder for DROP SEQUENCE statements.
 */
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