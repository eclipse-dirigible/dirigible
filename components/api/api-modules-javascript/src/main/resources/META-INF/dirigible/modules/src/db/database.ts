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
 * API Database
 *
 */
import { Bytes } from "sdk/io/bytes";
import { InputStream } from "sdk/io/streams";

const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");
const DatabaseResultSetHelper = Java.type("org.eclipse.dirigible.components.data.management.helpers.DatabaseResultSetHelper");
const JSqlDate = Java.type("java.sql.Date");
const JSqlTimestamp = Java.type("java.sql.Timestamp");
const JSqlTime = Java.type("java.sql.Time");
const StringWriter = Java.type("java.io.StringWriter");
const WriterOutputStream = Java.type("org.apache.commons.io.output.WriterOutputStream");
const StandardCharsets = Java.type("java.nio.charset.StandardCharsets");

const SQLTypes = Object.freeze({
	"BOOLEAN": 16,
	"DATE": 91,
	"TIME": 92,
	"TIMESTAMP": 93,
	"DOUBLE": 8,
	"FLOAT": 6,
	"REAL": 7,
	"TINYINT": -6,
	"SMALLINT": 5,
	"INTEGER": 4,
	"BIGINT": -5,
	"VARCHAR": 12,
	"CHAR": 1,
	"CLOB": 2005,
	"BLOB": 2004,
	"VARBINARY": -3,
	"DECIMAL": 3,
	"ARRAY": 2003,
	"NVARCHAR": -9,
	"NCLOB": 2011,
	"BIT": -7
});

export enum DatabaseSystem {
	UNKNOWN, DERBY, POSTGRESQL, H2, MARIADB, HANA, SNOWFLAKE, MYSQL, MONGODB, SYBASE
}

export interface TableMetadata {
	/** The name. */
	readonly name: string;

	/** The type. */
	readonly type: string;

	/** The remarks. */
	readonly remarks: string;

	/** The columns. */
	readonly columns: ColumnMetadata[];

	/** The indices. */
	readonly indices: IndexMetadata[];

	/** The indices. */
	readonly foreignKeys: ForeignKeyMetadata[];

	/** The kind. */
	readonly kind: string;
}

export interface ColumnMetadata {
	/** The name. */
	readonly name: string;

	/** The type. */
	readonly type: string;

	/** The size. */
	readonly size: number;

	/** The nullable. */
	readonly nullable: boolean;

	/** The key. */
	readonly key: boolean;

	/** The kind. */
	readonly kind: string;

	/** The scale. */
	readonly scale: number;
}

export interface IndexMetadata {

	/** The name. */
	readonly name: string;

	/** The type. */
	readonly type: string;

	/** The column. */
	readonly column: string;

	/** The non unique. */
	readonly nonUnique: boolean;

	/** The qualifier. */
	readonly qualifier: string;

	/** The ordinal position. */
	readonly ordinalPosition: string;

	/** The sort order. */
	readonly sortOrder: string;

	/** The cardinality. */
	readonly cardinality: number;

	/** The pages. */
	readonly pages: number;

	/** The filter condition. */
	readonly filterCondition: string;

	/** The kind. */
	readonly kind: string;
}

export interface ForeignKeyMetadata {

	/** The name. */
	readonly name: string;

	/** The kind. */
	readonly kind: string;
}

export interface SchemaMetadata {
	/**
		 * The name.
		 */
	readonly name: string;

	/**
	 * The kind.
	 */
	readonly kind: string;

	/**
	 * The tables.
	 */
	readonly tables: TableMetadata[];

	/**
	 * The views.
	 */
	readonly views: TableMetadata[];

	/**
	 * The procedures.
	 */
	readonly procedures: ProcedureMetadata[];

	/**
	 * The functions.
	 */
	readonly functions: FunctionMetadata[];

	/**
	 * The functions.
	 */
	readonly sequences: SequenceMetadata[];
}

export interface ProcedureMetadata {
	/** The name. */
	readonly name: string;

	/** The type. */
	readonly type: string;

	/** The remarks. */
	readonly remarks: string;

	/** The columns. */
	readonly columns: ParameterColumnMetadata[];

	/** The kind. */
	readonly kind: string;
}

export interface FunctionMetadata {
	/** The name. */
	readonly name: string;

	/** The type. */
	readonly type: string;

	/** The remarks. */
	readonly remarks: string;

	/** The columns. */
	readonly columns: ParameterColumnMetadata[];

	/** The kind. */
	readonly kind: string;
}

export interface ParameterColumnMetadata {
	/** The name. */
	readonly name: string;

	/** The kind. */
	readonly kind: number;

	/** The type. */
	readonly type: string;

	/** The precision. */
	readonly precision: number;

	/** The length. */
	readonly length: number;

	/** The scale. */
	readonly scale: number;

	/** The radix. */
	readonly radix: number;

	/** The nullable. */
	readonly nullable: boolean;

	/** The remarks. */
	readonly remarks: string;
}

export interface SequenceMetadata {

	/** The name. */
	readonly name: string;

	/** The kind. */
	readonly kind: string;
}

export interface DatabaseMetadata {

	/** The all procedures are callable. */
	readonly allProceduresAreCallable: boolean;

	/** The all tables are selectable. */
	readonly allTablesAreSelectable: boolean;

	/** The URL. */
	readonly url: string;

	/** The user name. */
	readonly userName: string;

	/** The is read only. */
	readonly isReadOnly: boolean;

	/** The nulls are sorted high. */
	readonly nullsAreSortedHigh: boolean;

	/** The nulls are sorted low. */
	readonly nullsAreSortedLow: boolean;

	/** The nulls are sorted at start. */
	readonly nullsAreSortedAtStart: boolean;

	/** The nulls are sorted at end. */
	readonly nullsAreSortedAtEnd: boolean;

	/** The database product name. */
	readonly databaseProductName: string;

	/** The database product version. */
	readonly databaseProductVersion: string;

	/** The driver name. */
	readonly driverName: string;

	/** The driver version. */
	readonly driverVersion: string;

	/** The driver major version. */
	readonly driverMajorVersion: number;

	/** The driver minor version. */
	readonly driverMinorVersion: number;

	/** The uses local files. */
	readonly usesLocalFiles: boolean;

	/** The uses local file per table. */
	readonly usesLocalFilePerTable: boolean;

	/** The supports mixed case identifiers. */
	readonly supportsMixedCaseIdentifiers: boolean;

	/** The stores upper case identifiers. */
	readonly storesUpperCaseIdentifiers: boolean;

	/** The stores lower case identifiers. */
	readonly storesLowerCaseIdentifiers: boolean;

	/** The stores mixed case identifiers. */
	readonly storesMixedCaseIdentifiers: boolean;

	/** The supports mixed case quoted identifiers. */
	readonly supportsMixedCaseQuotedIdentifiers: boolean;

	/** The stores upper case quoted identifiers. */
	readonly storesUpperCaseQuotedIdentifiers: boolean;

	/** The stores lower case quoted identifiers. */
	readonly storesLowerCaseQuotedIdentifiers: boolean;

	/** The stores mixed case quoted identifiers. */
	readonly storesMixedCaseQuotedIdentifiers: boolean;

	/** The identifier quote string. */
	readonly identifierQuoteString: string;

	/** The sql keywords. */
	readonly sqlKeywords: string;

	/** The numeric functions. */
	readonly numericFunctions: string;

	/** The string functions. */
	readonly stringFunctions: string;

	/** The system functions. */
	readonly systemFunctions: string;

	/** The time date functions. */
	readonly timeDateFunctions: string;

	/** The search string escape. */
	readonly searchStringEscape: string;

	/** The extra name characters. */
	readonly extraNameCharacters: string;

	/** The supports alter table with add column. */
	readonly supportsAlterTableWithAddColumn: boolean;

	/** The supports alter table with drop column. */
	readonly supportsAlterTableWithDropColumn: boolean;

	/** The supports column aliasing. */
	readonly supportsColumnAliasing: boolean;

	/** The null plus non null is null. */
	readonly nullPlusNonNullIsNull: boolean;

	/** The supports convert. */
	readonly supportsConvert: boolean;

	/** The supports table correlation names. */
	readonly supportsTableCorrelationNames: boolean;

	/** The supports different table correlation names. */
	readonly supportsDifferentTableCorrelationNames: boolean;

	/** The supports expressions in order by. */
	readonly supportsExpressionsInOrderBy: boolean;

	/** The supports order by unrelated. */
	readonly supportsOrderByUnrelated: boolean;

	/** The supports group by. */
	readonly supportsGroupBy: boolean;

	/** The supports group by unrelated. */
	readonly supportsGroupByUnrelated: boolean;

	/** The supports group by beyond select. */
	readonly supportsGroupByBeyondSelect: boolean;

	/** The supports like escape clause. */
	readonly supportsLikeEscapeClause: boolean;

	/** The supports multiple result sets. */
	readonly supportsMultipleResultSets: boolean;

	/** The supports multiple transactions. */
	readonly supportsMultipleTransactions: boolean;

	/** The supports non nullable columns. */
	readonly supportsNonNullableColumns: boolean;

	/** The supports minimum SQL grammar. */
	readonly supportsMinimumSQLGrammar: boolean;

	/** The supports core SQL grammar. */
	readonly supportsCoreSQLGrammar: boolean;

	/** The supports extended SQL grammar. */
	readonly supportsExtendedSQLGrammar: boolean;

	/** The supports ANSI 92 entry level SQL. */
	readonly supportsANSI92EntryLevelSQL: boolean;

	/** The supports ANSI 92 intermediate SQL. */
	readonly supportsANSI92IntermediateSQL: boolean;

	/** The supports ANSI 92 full SQL. */
	readonly supportsANSI92FullSQL: boolean;

	/** The supports integrity enhancement facility. */
	readonly supportsIntegrityEnhancementFacility: boolean;

	/** The supports outer joins. */
	readonly supportsOuterJoins: boolean;

	/** The supports full outer joins. */
	readonly supportsFullOuterJoins: boolean;

	/** The supports limited outer joins. */
	readonly supportsLimitedOuterJoins: boolean;

	/** The schema term. */
	readonly schemaTerm: string;

	/** The procedure term. */
	readonly procedureTerm: string;

	/** The catalog term. */
	readonly catalogTerm: string;

	/** The is catalog at start. */
	readonly isCatalogAtStart: boolean;

	/** The catalog separator. */
	readonly catalogSeparator: string;

	/** The supports schemas in data manipulation. */
	readonly supportsSchemasInDataManipulation: boolean;

	/** The supports schemas in procedure calls. */
	readonly supportsSchemasInProcedureCalls: boolean;

	/** The supports schemas in table definitions. */
	readonly supportsSchemasInTableDefinitions: boolean;

	/** The supports schemas in index definitions. */
	readonly supportsSchemasInIndexDefinitions: boolean;

	/** The supports schemas in privilege definitions. */
	readonly supportsSchemasInPrivilegeDefinitions: boolean;

	/** The supports catalogs in data manipulation. */
	readonly supportsCatalogsInDataManipulation: boolean;

	/** The supports catalogs in procedure calls. */
	readonly supportsCatalogsInProcedureCalls: boolean;

	/** The supports catalogs in table definitions. */
	readonly supportsCatalogsInTableDefinitions: boolean;

	/** The supports catalogs in index definitions. */
	readonly supportsCatalogsInIndexDefinitions: boolean;

	/** The supports catalogs in privilege definitions. */
	readonly supportsCatalogsInPrivilegeDefinitions: boolean;

	/** The supports positioned delete. */
	readonly supportsPositionedDelete: boolean;

	/** The supports positioned update. */
	readonly supportsPositionedUpdate: boolean;

	/** The supports select for update. */
	readonly supportsSelectForUpdate: boolean;

	/** The supports stored procedures. */
	readonly supportsStoredProcedures: boolean;

	/** The supports subqueries in comparisons. */
	readonly supportsSubqueriesInComparisons: boolean;

	/** The supports subqueries in exists. */
	readonly supportsSubqueriesInExists: boolean;

	/** The supports subqueries in ins. */
	readonly supportsSubqueriesInIns: boolean;

	/** The supports subqueries in quantifieds. */
	readonly supportsSubqueriesInQuantifieds: boolean;

	/** The supports correlated subqueries. */
	readonly supportsCorrelatedSubqueries: boolean;

	/** The supports union. */
	readonly supportsUnion: boolean;

	/** The supports union all. */
	readonly supportsUnionAll: boolean;

	/** The supports open cursors across commit. */
	readonly supportsOpenCursorsAcrossCommit: boolean;

	/** The supports open cursors across rollback. */
	readonly supportsOpenCursorsAcrossRollback: boolean;

	/** The supports open statements across commit. */
	readonly supportsOpenStatementsAcrossCommit: boolean;

	/** The supports open statements across rollback. */
	readonly supportsOpenStatementsAcrossRollback: boolean;

	/** The max binary literal length. */
	readonly maxBinaryLiteralLength: number;

	/** The max char literal length. */
	readonly maxCharLiteralLength: number;

	/** The max column name length. */
	readonly maxColumnNameLength: number;

	/** The max columns in group by. */
	readonly maxColumnsInGroupBy: number;

	/** The max columns in index. */
	readonly maxColumnsInIndex: number;

	/** The max columns in order by. */
	readonly maxColumnsInOrderBy: number;

	/** The max columns in select. */
	readonly maxColumnsInSelect: number;

	/** The max columns in table. */
	readonly maxColumnsInTable: number;

	/** The max connections. */
	readonly maxConnections: number;

	/** The max cursor name length. */
	readonly maxCursorNameLength: number;

	/** The max index length. */
	readonly maxIndexLength: number;

	/** The max schema name length. */
	readonly maxSchemaNameLength: number;

	/** The max procedure name length. */
	readonly maxProcedureNameLength: number;

	/** The max catalog name length. */
	readonly maxCatalogNameLength: number;

	/** The max row size. */
	readonly maxRowSize: number;

	/** The max row size include blobs. */
	readonly maxRowSizeIncludeBlobs: boolean;

	/** The max statement length. */
	readonly maxStatementLength: number;

	/** The max statements. */
	readonly maxStatements: number;

	/** The max table name length. */
	readonly maxTableNameLength: number;

	/** The max tables in select. */
	readonly maxTablesInSelect: number;

	/** The max user name length. */
	readonly maxUserNameLength: number;

	/** The default transaction isolation. */
	readonly defaultTransactionIsolation: number;

	/** The supports transactions. */
	readonly supportsTransactions: boolean;

	/** The supports data definition and data manipulation transactions. */
	readonly supportsDataDefinitionAndDataManipulationTransactions: boolean;

	/** The supports data manipulation transactions only. */
	readonly supportsDataManipulationTransactionsOnly: boolean;

	/** The data definition causes transaction commit. */
	readonly dataDefinitionCausesTransactionCommit: boolean;

	/** The data definition ignored in transactions. */
	readonly dataDefinitionIgnoredInTransactions: boolean;

	/** The supports batch updates. */
	readonly supportsBatchUpdates: boolean;

	/** The supports savepoints. */
	readonly supportsSavepoints: boolean;

	/** The supports named parameters. */
	readonly supportsNamedParameters: boolean;

	/** The supports multiple open results. */
	readonly supportsMultipleOpenResults: boolean;

	/** The supports get generated keys. */
	readonly supportsGetGeneratedKeys: boolean;

	/** The result set holdability. */
	readonly resultSetHoldability: number;

	/** The database major version. */
	readonly databaseMajorVersion: number;

	/** The database minor version. */
	readonly databaseMinorVersion: number;

	/** The JDBC major version. */
	readonly jdbcMajorVersion: number;

	/** The JDBC minor version. */
	readonly jdbcMinorVersion: number;

	/** The SQL state type. */
	readonly sqlStateType: number;

	/** The locators update copy. */
	readonly locatorsUpdateCopy: boolean;

	/** The supports statement pooling. */
	readonly supportsStatementPooling: boolean;

	/** The supports stored functions using call syntax. */
	readonly supportsStoredFunctionsUsingCallSyntax: boolean;

	/** The auto commit failure closes all result sets. */
	readonly autoCommitFailureClosesAllResultSets: boolean;

	/** The generated key always returned. */
	readonly generatedKeyAlwaysReturned: boolean;

	/** The max logical lob size. */
	readonly maxLogicalLobSize: number;

	/** The supports ref cursors. */
	readonly supportsRefCursors: boolean;

	/** The schemas. */
	readonly schemas: SchemaMetadata[];

	/** The kind. */
	readonly kind: string;
}

export class Database {

	public static getDataSources(): string[] {
		const datasources = DatabaseFacade.getDataSources();
		return datasources ? JSON.parse(datasources) : [];
	}

	public static getMetadata(datasourceName?: string): DatabaseMetadata | undefined {
		const metadata = DatabaseFacade.getMetadata(datasourceName);
		return metadata ? JSON.parse(metadata) : undefined;
	}

	public static getProductName(datasourceName?: string): string {
		return DatabaseFacade.getProductName(datasourceName);
	}

	public static getConnection(datasourceName?: string): Connection {
		return new Connection(datasourceName);
	}
}

/**
 * Connection object
 */
export class Connection {

	public readonly native: any;

	constructor(datasourceName?: string) {
		this.native = DatabaseFacade.getConnection(datasourceName);
	}

	public isOfType(databaseSystem: DatabaseSystem): boolean {
		return this.getDatabaseSystem() === databaseSystem;
	}

	public getDatabaseSystem(): DatabaseSystem {
		const dbSystem = this.native.getDatabaseSystem().name();
		switch (dbSystem) {
			case "DERBY":
				return DatabaseSystem.DERBY;
			case "POSTGRESQL":
				return DatabaseSystem.POSTGRESQL;
			case "H2":
				return DatabaseSystem.H2;
			case "MARIADB":
				return DatabaseSystem.MARIADB;
			case "HANA":
				return DatabaseSystem.HANA;
			case "SNOWFLAKE":
				return DatabaseSystem.SNOWFLAKE;
			case "MYSQL":
				return DatabaseSystem.MYSQL;
			case "MONGODB":
				return DatabaseSystem.MONGODB;
			case "SYBASE":
				return DatabaseSystem.SYBASE;
			case "UNKNOWN":
				return DatabaseSystem.UNKNOWN;
			default:
				throw Error(`Missing mapping for database system type ${dbSystem}`);
		}
	}

	public prepareStatement(sql: string): PreparedStatement {
		return new PreparedStatement(this.native.prepareStatement(sql));
	}

	public prepareCall(sql: string): CallableStatement {
		return new CallableStatement(this.native.prepareCall(sql));
	}

	public close(): void {
		if (!this.isClosed()) {
			this.native.close();
		}
	}

	public commit(): void {
		this.native.commit();
	}

	public getAutoCommit(): boolean {
		return this.native.getAutoCommit();
	}

	public getCatalog(): string {
		return this.native.getCatalog();
	}

	public getSchema(): string {
		return this.native.getSchema();
	}

	public getTransactionIsolation(): number {
		return this.native.getTransactionIsolation();
	}

	public isClosed(): boolean {
		return this.native.isClosed();
	}

	public isReadOnly(): boolean {
		return this.native.isReadOnly();
	}

	public isValid(): boolean {
		return this.native.isValid();
	}

	public rollback(): void {
		this.native.rollback();
	}

	public setAutoCommit(autoCommit: boolean): void {
		this.native.setAutoCommit(autoCommit);
	}

	public setCatalog(catalog: string): void {
		this.native.setCatalog(catalog);
	}

	public setReadOnly(readOnly: boolean): void {
		this.native.setReadOnly(readOnly);
	}

	public setSchema(schema: string): void {
		this.native.setSchema(schema);
	}

	public setTransactionIsolation(transactionIsolation: number): void {
		this.native.setTransactionIsolation(transactionIsolation);
	}

	public getMetaData(): any /*: DatabaseMetaData*/ {
		return this.native.getMetaData();
	}
}

/**
 * Statement object
 */
export class PreparedStatement {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public close() {
		this.native.close();
	}

	public getResultSet(): ResultSet {
		return new ResultSet(this.native.getResultSet());
	}

	public execute(): boolean {
		return this.native.execute();
	}

	public executeQuery(): ResultSet {
		return new ResultSet(this.native.executeQuery());
	}

	public executeUpdate(): number {
		return this.native.executeUpdate();
	}

	public setNull(index: number, sqlType: number): void {
		this.native.setNull(index, sqlType);
	}

	public setBinaryStream(parameterIndex: number, inputStream: InputStream, length?: number): void {
		if (length) {
			this.native.setBinaryStream(parameterIndex, inputStream, length);
		} else {
			this.native.setBinaryStream(parameterIndex, inputStream);
		}
	}

	public setBoolean(index: number, value?: boolean): void {
		if (value !== null && value !== undefined) {
			this.native.setBoolean(index, value);
		} else {
			this.setNull(index, SQLTypes.BOOLEAN);
		}
	}

	public setByte(index: number, value?: any /*: Byte*/): void {
		if (value !== null && value !== undefined) {
			this.native.setByte(index, value);
		} else {
			this.setNull(index, SQLTypes.TINYINT);
		}
	}

	public setBlob(index: number, value?: any /**: Blob*/): void {
		if (value !== null && value !== undefined) {
			let blob = createBlobValue(this.native, value);
			this.native.setBlob(index, blob);
		} else {
			this.setNull(index, SQLTypes.BLOB);
		}
	}

	public setClob(index: number, value?: any /*: Clob*/): void {
		if (value !== null && value !== undefined) {
			let clob = createClobValue(this.native, value);
			this.native.setClob(index, clob);
		} else {
			this.setNull(index, SQLTypes.CLOB);
		}
	}

	public setNClob(index: number, value?: any /*: NClob*/): void {
		if (value !== null && value !== undefined) {
			let nclob = createNClobValue(this.native, value);
			this.native.setNClob(index, nclob);
		} else {
			this.setNull(index, SQLTypes.NCLOB);
		}
	}

	public setBytesNative(index: number, value?: any[] /*byte[]*/): void {
		if (value !== null && value !== undefined) {
			this.native.setBytes(index, value);
		} else {
			this.setNull(index, SQLTypes.VARBINARY);
		}
	}

	public setBytes(index: number, value?: any[] /*byte[]*/): void {
		if (value !== null && value !== undefined) {
			var data = Bytes.toJavaBytes(value);
			this.native.setBytes(index, data);
		} else {
			this.setNull(index, SQLTypes.VARBINARY);
		}
	}

	public setDate(index: number, value?: string | Date): void {
		if (value !== null && value !== undefined) {
			const date = getDateValue(value);
			this.native.setDate(index, new JSqlDate(date.getTime()));
		} else {
			this.setNull(index, SQLTypes.DATE);
		}
	}

	public setDouble(index: number, value?: number): void {
		if (value !== null && value !== undefined) {
			this.native.setDouble(index, value);
		} else {
			this.setNull(index, SQLTypes.DOUBLE);
		}
	}

	public setFloat(index: number, value: number): void {
		if (value !== null && value !== undefined) {
			this.native.setFloat(index, value);
		} else {
			this.setNull(index, SQLTypes.FLOAT);
		}
	}

	public setInt(index: number, value?: number): void {
		if (value !== null && value !== undefined) {
			this.native.setInt(index, value);
		} else {
			this.setNull(index, SQLTypes.INTEGER);
		}
	}

	public setLong(index: number, value?: number): void {
		if (value !== null && value !== undefined) {
			this.native.setLong(index, value);
		} else {
			this.setNull(index, SQLTypes.BIGINT);
		}
	}

	public setShort(index: number, value?: number): void {
		if (value !== null && value !== undefined) {
			this.native.setShort(index, value);
		} else {
			this.setNull(index, SQLTypes.SMALLINT);
		}
	}

	public setString(index: number, value?: string): void {
		if (value !== null && value !== undefined) {
			this.native.setString(index, value);
		} else {
			this.setNull(index, SQLTypes.VARCHAR);
		}
	}

	public setTime(index: number, value?: string | Date): void {
		if (value !== null && value !== undefined) {
			const date = getDateValue(value);
			this.native.setTime(index, new JSqlTime(date.getTime()));
		} else {
			this.setNull(index, SQLTypes.TIME);
		}
	}

	public setTimestamp(index: number, value?: string | Date): void {
		if (value !== null && value !== undefined) {
			const date = getDateValue(value);
			this.native.setTimestamp(index, new JSqlTimestamp(date.getTime()));
		} else {
			this.setNull(index, SQLTypes.TIMESTAMP);
		}
	}

	public setBigDecimal(index: number, value?: number /*: BigDecimal*/): void {
		if (value !== null && value !== undefined) {
			this.native.setBigDecimal(index, value);
		} else {
			this.setNull(index, SQLTypes.DECIMAL);
		}
	}

	public setNString(index: number, value?: string): void {
		if (value !== null && value !== undefined) {
			this.native.setNString(index, value);
		} else {
			this.setNull(index, SQLTypes.NVARCHAR);
		}
	}

	public addBatch(): void {
		this.native.addBatch();
	}

	public executeBatch(): number[] {
		return this.native.executeBatch();
	}

	public getMetaData(): any {
		return this.native.getMetaData();
	}

	public getMoreResults(): boolean {
		return this.native.getMoreResults();
	}

	public getParameterMetaData(): any {
		return this.native.getParameterMetaData();
	}

	public getSQLWarning(): any {
		return this.native.getWarnings();
	}

	public isClosed(): boolean {
		return this.native.isClosed();
	}
}

export class CallableStatement {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getResultSet(): ResultSet {
		return new ResultSet(this.native.getResultSet());
	}

	public executeQuery(): ResultSet {
		return new ResultSet(this.native.executeQuery());
	}

	public executeUpdate(): number {
		return this.native.executeUpdate();
	}

	public registerOutParameter(parameterIndex: number, sqlType: keyof typeof SQLTypes | number): void {
		this.native.registerOutParameter(parameterIndex, sqlType);
	}

	public registerOutParameterByScale(parameterIndex: number, sqlType: keyof typeof SQLTypes | number, scale: number): void {
		this.native.registerOutParameter(parameterIndex, sqlType, scale);
	}

	public registerOutParameterByTypeName(parameterIndex: number, sqlType: keyof typeof SQLTypes | number, typeName: string): void {
		this.native.registerOutParameter(parameterIndex, sqlType, typeName);
	}

	public wasNull(): boolean {
		return this.native.wasNull();
	}

	public getString(parameterIndex: number): string {
		return this.native.getString(parameterIndex);
	}

	public getBoolean(parameterIndex: number): boolean {
		return this.native.getBoolean(parameterIndex);
	}

	public getByte(parameterIndex: number): any /*: byte*/ {
		return this.native.getByte(parameterIndex);
	}

	public getShort(parameterIndex: number): number {
		return this.native.getShort(parameterIndex);
	}

	public getInt(parameterIndex: number): number {
		return this.native.getInt(parameterIndex);
	}

	public getLong(parameterIndex: number): number {
		return this.native.getLong(parameterIndex);
	}

	public getFloat(parameterIndex: number): number {
		return this.native.getFloat(parameterIndex);
	}

	public getDouble(parameterIndex: number): number {
		return this.native.getDouble(parameterIndex);
	}

	public getDate(parameterIndex: number): Date {
		return this.native.getDate(parameterIndex);
	}

	public getTime(parameterIndex: number): Date {
		return this.native.getTime(parameterIndex);
	}

	public getTimestamp(parameterIndex: number): Date {
		return this.native.getTimestamp(parameterIndex);
	}

	public getObject(parameterIndex: number): any {
		return this.native.getObject(parameterIndex);
	}

	public getBigDecimal(parameterIndex: number): number /*: sql.BigDecimal*/ {
		return this.native.getBigDecimal(parameterIndex);
	}

	public getRef(parameterIndex: number): any /*: sql.Ref*/ {
		return this.native.getRef(parameterIndex);
	}

	public getBytes(parameterIndex: number): any[] /*: byte[]*/ {
		const data = this.native.getBytes(parameterIndex);
		return Bytes.toJavaScriptBytes(data);
	}

	public getBytesNative(parameterIndex: number): any[] /*: byte[]*/ {
		return this.native.getBytes(parameterIndex);
	}

	public getBlob(parameterIndex: number): any /*: sql.Blob*/ {
		const data = readBlobValue(this.native.getBlob(parameterIndex));
		return Bytes.toJavaScriptBytes(data);
	}

	public getBlobNative(parameterIndex: number): any /*: sql.Blob*/ {
		return readBlobValue(this.native.getBlob(parameterIndex));
	}

	public getClob(parameterIndex: number): any /*: sql.Clob*/ {
		return readClobValue(this.native.getClob(parameterIndex));
	}

	public getNClob(parameterIndex: string | number): any /*: sql.NClob*/ {
		return readNClobValue(this.native.getNClob(parameterIndex));
	}

	public getNString(parameterIndex: string | number): string {
		return this.native.getNString(parameterIndex);
	}

	public getArray(parameterIndex: string | number): any[] /*: sql.Array*/ {
		return this.native.getArray(parameterIndex);
	}

	public getURL(parameterIndex: string | number): any {
		return this.native.getURL(parameterIndex);
	}

	public getRowId(parameterIndex: string | number): any /*: sql.RowId*/ {
		return this.native.getRowId(parameterIndex);
	}

	public getSQLXML(parameterIndex: string | number): any /*: sql.SQLXML*/ {
		return this.native.getSQLXML(parameterIndex);
	}

	public setURL(parameterIndex: number, value: any): void {
		this.native.setURL(parameterIndex, value);
	}

	public setNull(parameterIndex: number, sqlTypeStr: keyof typeof SQLTypes | number, typeName?: string): void {
		const sqlType: number = Number.isInteger(sqlTypeStr) ? sqlTypeStr : SQLTypes[sqlTypeStr];
		if (typeName !== undefined && typeName !== null) {
			this.native.setNull(parameterIndex, sqlType, typeName);
		} else {
			this.native.setNull(parameterIndex, sqlType);
		}
	}

	public setBoolean(parameterIndex: number, value?: boolean): void {
		if (value !== null && value !== undefined) {
			this.native.setBoolean(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.BOOLEAN);
		}
	}

	public setByte(parameterIndex: number, value?: any /*: byte*/): void {
		if (value !== null && value !== undefined) {
			this.native.setByte(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.BIT);
		}
	}

	public setShort(parameterIndex: number, value?: number): void {
		if (value !== null && value !== undefined) {
			this.native.setShort(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.SMALLINT);
		}
	}

	public setInt(parameterIndex: number, value?: number): void {
		if (value !== null && value !== undefined) {
			this.native.setInt(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.INTEGER);
		}
	}

	public setLong(parameterIndex: number, value?: number): void {
		if (value !== null && value !== undefined) {
			this.native.setLong(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.BIGINT);
		}
	}

	public setFloat(parameterIndex: number, value?: number): void {
		if (value !== null && value !== undefined) {
			this.native.setFloat(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.FLOAT);
		}
	}

	public setDouble(parameterIndex: number, value?: number): void {
		if (value !== null && value !== undefined) {
			this.native.setDouble(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.DOUBLE);
		}
	}

	public setBigDecimal(parameterIndex: number, value?: number /*: BigDecimal*/): void {
		if (value !== null && value !== undefined) {
			this.native.setBigDecimal(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.DECIMAL);
		}
	}

	public setString(parameterIndex: number, value?: string): void {
		if (value !== null && value !== undefined) {
			this.native.setString(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.VARCHAR);
		}
	}

	public setBytes(parameterIndex: number, value?: any[] /*byte[]*/): void {
		if (value !== null && value !== undefined) {
			this.native.setBytes(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.ARRAY);
		}
	}

	public setDate(parameterIndex: number, value?: string | Date): void {
		if (value !== null && value !== undefined) {
			const date = getDateValue(value);
			this.native.setDate(parameterIndex, new JSqlDate(date.getTime()));
		} else {
			this.setNull(parameterIndex, SQLTypes.DATE);
		}
	}

	public setTime(parameterIndex: number, value?: string | Date): void {
		if (value !== null && value !== undefined) {
			const date = getDateValue(value);
			this.native.setTime(parameterIndex, new JSqlTime(date.getTime()));
		} else {
			this.setNull(parameterIndex, SQLTypes.TIME);
		}
	}

	public setTimestamp(parameterIndex: number, value?: string | Date): void {
		if (value !== null && value !== undefined) {
			let date = getDateValue(value);
			this.native.setTimestamp(parameterIndex, new JSqlTimestamp(date.getTime()));
		} else {
			this.setNull(parameterIndex, SQLTypes.TIMESTAMP);
		}
	}

	public setAsciiStream(parameterIndex: number, inputStream: InputStream, length: number): void {
		if (length) {
			this.native.setAsciiStream(parameterIndex, inputStream, length);
		} else {
			this.native.setAsciiStream(parameterIndex, inputStream);
		}
	}

	public setBinaryStream(parameterIndex: number, inputStream: InputStream, length: number): void {
		if (length) {
			this.native.setBinaryStream(parameterIndex, inputStream, length);
		} else {
			this.native.setBinaryStream(parameterIndex, inputStream);
		}
	}

	public setObject(parameterIndex: number, value: any, targetSqlType: number, scale: number): void {
		if (scale !== undefined && scale !== null && targetSqlType !== undefined && targetSqlType !== null) {
			this.native.setObject(parameterIndex, value, targetSqlType, scale);
		} else if (targetSqlType !== undefined && targetSqlType !== null) {
			this.native.setObject(parameterIndex, value, targetSqlType);
		} else {
			this.native.setObject(parameterIndex, value);
		}
	}

	public setRowId(parameterIndex: number, value: number /*: RowId*/): void {
		this.native.setRowId(parameterIndex, value);
	}

	public setNString(parameterIndex: number, value: string): void {
		if (value !== null && value !== undefined) {
			this.native.setNString(parameterIndex, value);
		} else {
			this.setNull(parameterIndex, SQLTypes.NVARCHAR);
		}
	}

	public setSQLXML(parameterIndex: number, value: any /*: SQLXML*/): void {
		if (value !== null && value !== undefined) {
			this.native.setSQLXML(parameterIndex, value);
		} else {
			throw Error("Nullable SQLXML type not supported.");
		}
	}

	public setBlob(parameterIndex: number, value: any /*Blob */): void {
		if (value !== null && value !== undefined) {
			const blob = createBlobValue(this.native, value);
			this.native.setBlob(parameterIndex, blob);
		} else {
			this.setNull(parameterIndex, SQLTypes.BLOB);
		}
	}

	public setClob(parameterIndex: number, value: any /*: Clob*/): void {
		if (value !== null && value !== undefined) {
			const clob = createClobValue(this.native, value);
			this.native.setClob(parameterIndex, clob);
		} else {
			this.setNull(parameterIndex, SQLTypes.CLOB);
		}
	}

	public setNClob(parameterIndex: number, value: any /*: NClob*/): void {
		if (value !== null && value !== undefined) {
			const nclob = createNClobValue(this.native, value);
			this.native.setNClob(parameterIndex, nclob);
		} else {
			this.setNull(parameterIndex, SQLTypes.NCLOB);
		}
	}

	public execute(): boolean {
		return this.native.execute();
	}

	public getMoreResults(): boolean {
		return this.native.getMoreResults();
	}

	public getParameterMetaData(): any /*: ParameterMetaData*/ {
		return this.native.getParameterMetaData();
	}

	public isClosed(): boolean {
		return this.native.isClosed();
	}

	public close(): void {
		this.native.close();
	}
}

/**
 * ResultSet object
 */
export class ResultSet {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public toJson(limited = false, stringify = false): any[] {
		const sw = new StringWriter();
		const output = WriterOutputStream
			.builder()
			.setWriter(sw)
			.setCharset(StandardCharsets.UTF_8)
			.get();
		DatabaseResultSetHelper.toJson(this.native, limited, stringify, output);
		return JSON.parse(sw.toString());
	}

	public close(): void {
		this.native.close();
	}

	public getBigDecimal(identifier: number | string): any /*: BigDecimal*/  {
		return this.native.getBigDecimal(identifier);
	}

	public getBoolean(identifier: number | string): boolean {
		return this.native.getBoolean(identifier);
	}

	public getByte(identifier: number | string): any /*: byte*/ {
		return this.native.getByte(identifier);
	}

	public getBytes(identifier: number | string): any[] /*: byte[]*/ {
		const data = this.native.getBytes(identifier);
		return Bytes.toJavaScriptBytes(data);
	};

	public getBytesNative(identifier: number | string): any[] /*: byte[]*/ {
		return this.native.getBytes(identifier);
	};

	public getBlob(identifier: number | string): any /*: sql.Blob*/ {
		const data = readBlobValue(this.native.getBlob(identifier));
		return Bytes.toJavaScriptBytes(data);
	}

	public getBlobNative(identifier: number | string): any /*: sql.Blob*/ {
		return readBlobValue(this.native.getBlob(identifier));
	}

	public getClob(identifier: number | string): any /*: sql.Clob*/ {
		return readClobValue(this.native.getClob(identifier));
	}

	public getNClob = function (identifier: number | string): any /*: sql.NClob*/ {
		return readNClobValue(this.native.getNClob(identifier));
	}

	public getDate(identifier: number | string): Date | undefined {
		const dateInstance = this.native.getDate(identifier);
		return dateInstance !== null && dateInstance !== undefined ? new Date(dateInstance.getTime()) : undefined;
	}

	public getDouble(identifier: number | string): number {
		return this.native.getDouble(identifier);
	}

	public getFloat(identifier: number | string): number {
		return this.native.getFloat(identifier);
	}

	public getInt(identifier: number | string): number {
		return this.native.getInt(identifier);
	}

	public getLong(identifier: number | string): number {
		return this.native.getLong(identifier);
	}

	public getShort(identifier: number | string): number {
		return this.native.getShort(identifier);
	}

	public getString(identifier: number | string): string {
		return this.native.getString(identifier);
	}

	public getTime(identifier: number | string): Date | undefined {
		const dateInstance = this.native.getTime(identifier);
		return dateInstance !== null && dateInstance !== undefined ? new Date(dateInstance.getTime()) : undefined;
	}

	public getTimestamp(identifier: number | string): Date | undefined {
		const dateInstance = this.native.getTimestamp(identifier);
		return dateInstance !== null && dateInstance !== undefined ? new Date(dateInstance.getTime()) : undefined;
	}

	public isAfterLast(): boolean {
		return this.native.isAfterLast();
	}

	public isBeforeFirst(): boolean {
		return this.native.isBeforeFirst();
	}

	public isClosed(): boolean {
		return this.native.isClosed();
	}

	public isFirst(): boolean {
		return this.native.isFirst();
	}

	public isLast(): boolean {
		return this.native.isLast();
	}

	public next(): boolean {
		return this.native.next();
	}

	public getMetaData(): any /*: ResultSetMetaData*/ {
		return this.native.getMetaData();
	}

	public getNString(columnIndex: number): string {
		return this.native.getNString(columnIndex);
	}
}

function isHanaDatabase(connection) {
	let isHanaDatabase = false;
	let metadata = connection.getMetaData();
	if (metadata !== null && metadata !== undefined) {
		isHanaDatabase = metadata.getDatabaseProductName() === "HDB";
	}
	return isHanaDatabase;
}

function readBlobValue(value) {
	return value ? value.getBytes(1, value.length()) : value;
}

function createBlobValue(native, value) {
	try {
		let connection = native.getConnection(); // intentionally not closed
		if (connection === null || connection === undefined) {
			throw new Error("Can't create new 'Blob' value as the connection is null");
		}
		let blob = null;
		if (isHanaDatabase(connection)) {
			let ps = null;
			try {
				ps = connection.prepareStatement("SELECT TO_BLOB (?) FROM DUMMY;");
				ps.setBytes(1, value);
				let rs = ps.executeQuery();
				if (rs.next()) {
					blob = rs.getBlob(1);
				}
			} finally {
				if (ps !== null && ps !== undefined) {
					ps.close();
				}
			}
		} else {
			blob = connection.createBlob();
			blob.setBytes(1, value);
		}
		return blob;
	} catch (e) {
		throw new Error(`Error occured during creation of 'Clob' value: ${e.message}`);
	}
}

function readClobValue(value) {
	return value ? value.getSubString(1, value.length()) : value;
}

function createClobValue(native, value) {
	try {
		let connection = native.getConnection(); // intentionally not closed
		if (connection === null || connection === undefined) {
			throw new Error("Can't create new 'Clob' value as the connection is null");
		}
		let clob = null;
		if (isHanaDatabase(connection)) {
			let ps = null;
			try {
				ps = connection.prepareStatement("SELECT TO_CLOB (?) FROM DUMMY;");
				ps.setString(1, value);
				let rs = ps.executeQuery();
				if (rs.next()) {
					clob = rs.getClob(1);
				}
			} finally {
				if (ps !== null && ps !== undefined) {
					ps.close();
				}
			}
		} else {
			clob = connection.createClob();
			clob.setString(1, value);
		}
		return clob;
	} catch (e) {
		throw new Error(`Error occured during creation of 'Clob' value: ${e.message}`);
	}
}

function readNClobValue(value) {
	return value ? value.getSubString(1, value.length()) : value;
}

function createNClobValue(native, value) {
	try {
		let connection = native.getConnection(); // intentionally not closed
		if (connection === null || connection === undefined) {
			throw new Error("Can't create new 'NClob' value as the connection is null");
		}
		let nclob = null;
		if (isHanaDatabase(connection)) {
			let ps = null;
			try {
				ps = connection.prepareStatement("SELECT TO_NCLOB (?) FROM DUMMY;");
				ps.setString(1, value);
				let rs = ps.executeQuery();
				if (rs.next()) {
					nclob = rs.getNClob(1);
				}
			} finally {
				if (ps !== null && ps !== undefined) {
					ps.close();
				}
			}
		} else {
			nclob = connection.createNClob();
			nclob.setString(1, value);
		}
		return nclob;
	} catch (e) {
		throw new Error(`Error occured during creation of 'NClob' value: ${e.message}`);
	}
}

function getDateValue(value) {
	if (typeof value === "string") {
		return new Date(value);
	}
	return value;
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Database;
}