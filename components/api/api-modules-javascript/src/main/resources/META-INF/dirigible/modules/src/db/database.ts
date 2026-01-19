/**
 * API Database
 *
 */
import { Bytes } from "@aerokit/sdk/io/bytes";
import { InputStream } from "@aerokit/sdk/io/streams";

const DatabaseFacade = Java.type("org.eclipse.dirigible.components.api.db.DatabaseFacade");
const JSqlDate = Java.type("java.sql.Date");
const JSqlTimestamp = Java.type("java.sql.Timestamp");
const JSqlTime = Java.type("java.sql.Time");
const StringWriter = Java.type("java.io.StringWriter");
const WriterOutputStream = Java.type("org.apache.commons.io.output.WriterOutputStream");
const StandardCharsets = Java.type("java.nio.charset.StandardCharsets");

/**
 * Mapping of SQL type names to their java.sql.Types integer constants.
 */
export const SQLTypes = Object.freeze({
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
	UNKNOWN, DERBY, POSTGRESQL, H2, MARIADB, HANA, SNOWFLAKE, MYSQL, MONGODB, SYBASE, MSSQL
}

// --- Metadata Interfaces ---

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
	/** The name. */
	readonly name: string;
	/** The kind. */
	readonly kind: string;
	/** The tables. */
	readonly tables: TableMetadata[];
	/** The views. */
	readonly views: TableMetadata[];
	/** The procedures. */
	readonly procedures: ProcedureMetadata[];
	/** The functions. */
	readonly functions: FunctionMetadata[];
	/** The functions. */
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
	readonly allProceduresAreCallable: boolean;
	readonly allTablesAreSelectable: boolean;
	readonly url: string;
	readonly userName: string;
	readonly isReadOnly: boolean;
	readonly nullsAreSortedHigh: boolean;
	readonly nullsAreSortedLow: boolean;
	readonly nullsAreSortedAtStart: boolean;
	readonly nullsAreSortedAtEnd: boolean;
	readonly databaseProductName: string;
	readonly databaseProductVersion: string;
	readonly driverName: string;
	readonly driverVersion: string;
	readonly driverMajorVersion: number;
	readonly driverMinorVersion: number;
	readonly usesLocalFiles: boolean;
	readonly usesLocalFilePerTable: boolean;
	readonly supportsMixedCaseIdentifiers: boolean;
	readonly storesUpperCaseIdentifiers: boolean;
	readonly storesLowerCaseIdentifiers: boolean;
	readonly storesMixedCaseIdentifiers: boolean;
	readonly supportsMixedCaseQuotedIdentifiers: boolean;
	readonly storesUpperCaseQuotedIdentifiers: boolean;
	readonly storesLowerCaseQuotedIdentifiers: boolean;
	readonly storesMixedCaseQuotedIdentifiers: boolean;
	readonly identifierQuoteString: string;
	readonly sqlKeywords: string;
	readonly numericFunctions: string;
	readonly stringFunctions: string;
	readonly systemFunctions: string;
	readonly timeDateFunctions: string;
	readonly searchStringEscape: string;
	readonly extraNameCharacters: string;
	readonly supportsAlterTableWithAddColumn: boolean;
	readonly supportsAlterTableWithDropColumn: boolean;
	readonly supportsColumnAliasing: boolean;
	readonly nullPlusNonNullIsNull: boolean;
	readonly supportsConvert: boolean;
	readonly supportsTableCorrelationNames: boolean;
	readonly supportsDifferentTableCorrelationNames: boolean;
	readonly supportsExpressionsInOrderBy: boolean;
	readonly supportsOrderByUnrelated: boolean;
	readonly supportsGroupBy: boolean;
	readonly supportsGroupByUnrelated: boolean;
	readonly supportsGroupByBeyondSelect: boolean;
	readonly supportsLikeEscapeClause: boolean;
	readonly supportsMultipleResultSets: boolean;
	readonly supportsMultipleTransactions: boolean;
	readonly supportsNonNullableColumns: boolean;
	readonly supportsMinimumSQLGrammar: boolean;
	readonly supportsCoreSQLGrammar: boolean;
	readonly supportsExtendedSQLGrammar: boolean;
	readonly supportsANSI92EntryLevelSQL: boolean;
	readonly supportsANSI92IntermediateSQL: boolean;
	readonly supportsANSI92FullSQL: boolean;
	readonly supportsIntegrityEnhancementFacility: boolean;
	readonly supportsOuterJoins: boolean;
	readonly supportsFullOuterJoins: boolean;
	readonly supportsLimitedOuterJoins: boolean;
	readonly schemaTerm: string;
	readonly procedureTerm: string;
	readonly catalogTerm: string;
	readonly isCatalogAtStart: boolean;
	readonly catalogSeparator: string;
	readonly supportsSchemasInDataManipulation: boolean;
	readonly supportsSchemasInProcedureCalls: boolean;
	readonly supportsSchemasInTableDefinitions: boolean;
	readonly supportsSchemasInIndexDefinitions: boolean;
	readonly supportsSchemasInPrivilegeDefinitions: boolean;
	readonly supportsCatalogsInDataManipulation: boolean;
	readonly supportsCatalogsInProcedureCalls: boolean;
	readonly supportsCatalogsInTableDefinitions: boolean;
	readonly supportsCatalogsInIndexDefinitions: boolean;
	readonly supportsCatalogsInPrivilegeDefinitions: boolean;
	readonly supportsPositionedDelete: boolean;
	readonly supportsPositionedUpdate: boolean;
	readonly supportsSelectForUpdate: boolean;
	readonly supportsStoredProcedures: boolean;
	readonly supportsSubqueriesInComparisons: boolean;
	readonly supportsSubqueriesInExists: boolean;
	readonly supportsSubqueriesInIns: boolean;
	readonly supportsSubqueriesInQuantifieds: boolean;
	readonly supportsCorrelatedSubqueries: boolean;
	readonly supportsUnion: boolean;
	readonly supportsUnionAll: boolean;
	readonly supportsOpenCursorsAcrossCommit: boolean;
	readonly supportsOpenCursorsAcrossRollback: boolean;
	readonly supportsOpenStatementsAcrossCommit: boolean;
	readonly supportsOpenStatementsAcrossRollback: boolean;
	readonly maxBinaryLiteralLength: number;
	readonly maxCharLiteralLength: number;
	readonly maxColumnNameLength: number;
	readonly maxColumnsInGroupBy: number;
	readonly maxColumnsInIndex: number;
	readonly maxColumnsInOrderBy: number;
	readonly maxColumnsInSelect: number;
	readonly maxColumnsInTable: number;
	readonly maxConnections: number;
	readonly maxCursorNameLength: number;
	readonly maxIndexLength: number;
	readonly maxSchemaNameLength: number;
	readonly maxProcedureNameLength: number;
	readonly maxCatalogNameLength: number;
	readonly maxRowSize: number;
	readonly maxRowSizeIncludeBlobs: boolean;
	readonly maxStatementLength: number;
	readonly maxStatements: number;
	readonly maxTableNameLength: number;
	readonly maxTablesInSelect: number;
	readonly maxUserNameLength: number;
	readonly defaultTransactionIsolation: number;
	readonly supportsTransactions: boolean;
	readonly supportsDataDefinitionAndDataManipulationTransactions: boolean;
	readonly supportsDataManipulationTransactionsOnly: boolean;
	readonly dataDefinitionCausesTransactionCommit: boolean;
	readonly dataDefinitionIgnoredInTransactions: boolean;
	readonly supportsBatchUpdates: boolean;
	readonly supportsSavepoints: boolean;
	readonly supportsNamedParameters: boolean;
	readonly supportsMultipleOpenResults: boolean;
	readonly supportsGetGeneratedKeys: boolean;
	readonly resultSetHoldability: number;
	readonly databaseMajorVersion: number;
	readonly databaseMinorVersion: number;
	readonly jdbcMajorVersion: number;
	readonly jdbcMinorVersion: number;
	readonly sqlStateType: number;
	readonly locatorsUpdateCopy: boolean;
	readonly supportsStatementPooling: boolean;
	readonly supportsStoredFunctionsUsingCallSyntax: boolean;
	readonly autoCommitFailureClosesAllResultSets: boolean;
	readonly generatedKeyAlwaysReturned: boolean;
	readonly maxLogicalLobSize: number;
	readonly supportsRefCursors: boolean;
	readonly schemas: SchemaMetadata[];
	readonly kind: string;
}

// --- Helper Functions ---

function isHanaDatabase(connection: any): boolean {
	let isHanaDatabase = false;
	let metadata = connection.getMetaData();
	if (metadata !== null && metadata !== undefined) {
		isHanaDatabase = metadata.getDatabaseProductName() === "HDB";
	}
	return isHanaDatabase;
}

function readClobValue(value: any): string | any {
	return value ? value.getSubString(1, value.length()) : value;
}

function createClobValue(native: any, value: any): any {
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
	} catch (e: any) {
		throw new Error(`Error occured during creation of 'Clob' value: ${e.message}`);
	}
}

function readNClobValue(value: any): string | any {
	return value ? value.getSubString(1, value.length()) : value;
}

function createNClobValue(native: any, value: any): any {
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
	} catch (e: any) {
		throw new Error(`Error occured during creation of 'NClob' value: ${e.message}`);
	}
}

function createBlobValue(native: any, value: any): any {
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
	} catch (e: any) {
		throw new Error(`Error occured during creation of 'Clob' value: ${e.message}`);
	}
}

function getDateValue(value: string | Date): Date {
	if (typeof value === "string") {
		return new Date(value);
	}
	return value;
}

// --- Statement Classes ---

/**
 * Statement object
 */
export class PreparedStatement {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public close(): void {
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

	public setFloat(index: number, value?: number): void {
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
		const dateInstance = this.native.getDate(parameterIndex);
		return dateInstance !== null && dateInstance !== undefined ? new Date(dateInstance.getTime()) : dateInstance;
	}

	public getTime(parameterIndex: number): Date {
		const dateInstance = this.native.getTime(parameterIndex);
		return dateInstance !== null && dateInstance !== undefined ? new Date(dateInstance.getTime()) : dateInstance;
	}

	public getTimestamp(parameterIndex: number): Date {
		const dateInstance = this.native.getTimestamp(parameterIndex);
		return dateInstance !== null && dateInstance !== undefined ? new Date(dateInstance.getTime()) : dateInstance;
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
		const data = DatabaseFacade.readBlobValue(this.native, parameterIndex);
		return Bytes.toJavaScriptBytes(data);
	}

	public getBlobNative(parameterIndex: number): any /*: sql.Blob*/ {
		return DatabaseFacade.readBlobValue(this.native, parameterIndex);
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
		const sqlType: number = Number.isInteger(sqlTypeStr as number) ? sqlTypeStr as number : SQLTypes[sqlTypeStr as keyof typeof SQLTypes];
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

	public setAsciiStream(parameterIndex: number, inputStream: InputStream, length?: number): void {
		if (length) {
			this.native.setAsciiStream(parameterIndex, inputStream, length);
		} else {
			this.native.setAsciiStream(parameterIndex, inputStream);
		}
	}

	public setBinaryStream(parameterIndex: number, inputStream: InputStream, length?: number): void {
		if (length) {
			this.native.setBinaryStream(parameterIndex, inputStream, length);
		} else {
			this.native.setBinaryStream(parameterIndex, inputStream);
		}
	}

	public setObject(parameterIndex: number, value: any, targetSqlType?: number, scale?: number): void {
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
			throw new Error("Nullable SQLXML type not supported.");
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

	/**
	 * Converts the ResultSet into a JSON array of objects.
	 * @param limited Whether to use limited JSON conversion (optimized).
	 * @param stringify Whether to return the JSON as a string or a parsed array.
	 * @returns A JavaScript array of objects representing the result set, or a string if stringify is true.
	 */
	public toJson(limited = false, stringify = false): any[] {
		const sw = new StringWriter();
		const output = WriterOutputStream
			.builder()
			.setWriter(sw)
			.setCharset(StandardCharsets.UTF_8)
			.get();
		DatabaseFacade.toJson(this.native, limited, stringify, output);
		const jsonString = sw.toString();
		return stringify ? jsonString : JSON.parse(jsonString);
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
		const data = DatabaseFacade.readBlobValue(this.native, identifier);
		return Bytes.toJavaScriptBytes(data);
	}

	public getBlobNative(identifier: number | string): any /*: sql.Blob*/ {
		return DatabaseFacade.readBlobValue(this.native, identifier);
	}

	public getClob(identifier: number | string): any /*: sql.Clob*/ {
		return readClobValue(this.native.getClob(identifier));
	}

	public getNClob(identifier: number | string): any /*: sql.NClob*/ {
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

// --- Connection Class ---

/**
 * Connection object wrapper around a native Java `Connection`.
 */
export class Connection {

	public readonly native: any;

	constructor(datasourceName?: string) {
		this.native = DatabaseFacade.getConnection(datasourceName);
	}

	/**
	 * Checks if the connection is for a specific database system.
	 */
	public isOfType(databaseSystem: DatabaseSystem): boolean {
		return this.getDatabaseSystem() === databaseSystem;
	}

	/**
	 * Returns the type of the underlying database system as a {@link DatabaseSystem} enum.
	 */
	public getDatabaseSystem(): DatabaseSystem {
		const dbSystem: string = this.native.getDatabaseSystem().name();
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
			case "MSSQL":
				return DatabaseSystem.MSSQL;
			case "MYSQL":
				return DatabaseSystem.MYSQL;
			case "MONGODB":
				return DatabaseSystem.MONGODB;
			case "SYBASE":
				return DatabaseSystem.SYBASE;
			case "UNKNOWN":
				return DatabaseSystem.UNKNOWN;
			default:
				throw new Error(`Missing mapping for database system type ${dbSystem}`);
		}
	}

	/**
	 * Creates a new {@link PreparedStatement} object for sending parameterized SQL statements to the database.
	 */
	public prepareStatement(sql: string): PreparedStatement {
		return new PreparedStatement(this.native.prepareStatement(sql));
	}

	/**
	 * Creates a {@link CallableStatement} object for calling database stored procedures or functions.
	 */
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

// --- Database Class ---

export class Database {

	/**
	 * Returns a list of available data source names.
	 */
	public static getDataSources(): string[] {
		const datasources = DatabaseFacade.getDataSources();
		return datasources ? JSON.parse(datasources) : [];
	}

	/**
	 * Returns database metadata for the specified data source.
	 */
	public static getMetadata(datasourceName?: string): DatabaseMetadata | undefined {
		const metadata = DatabaseFacade.getMetadata(datasourceName);
		return metadata ? JSON.parse(metadata) : undefined;
	}

	/**
	 * Returns the product name of the underlying database system.
	 */
	public static getProductName(datasourceName?: string): string {
		return DatabaseFacade.getProductName(datasourceName);
	}

	/**
	 * Gets a new database connection object.
	 */
	public static getConnection(datasourceName?: string): Connection {
		return new Connection(datasourceName);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = {
		Database,
		Connection,
		PreparedStatement,
		CallableStatement,
		ResultSet,
		DatabaseSystem,
		SQLTypes
	};
}
