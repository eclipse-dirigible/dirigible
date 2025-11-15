/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.domain;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Database Metadata transport object.
 */
public class DatabaseMetadata {

    /** The Constant NOT_SUPPORTED. */
    private static final String NOT_SUPPORTED = "Not supported: ";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadata.class);

    /** The all procedures are callable. */
    private boolean allProceduresAreCallable;

    /** The all tables are selectable. */
    private boolean allTablesAreSelectable;

    /** The URL. */
    private String url;

    /** The user name. */
    private String userName;

    /** The is read only. */
    private boolean isReadOnly;

    /** The nulls are sorted high. */
    private boolean nullsAreSortedHigh;

    /** The nulls are sorted low. */
    private boolean nullsAreSortedLow;

    /** The nulls are sorted at start. */
    private boolean nullsAreSortedAtStart;

    /** The nulls are sorted at end. */
    private boolean nullsAreSortedAtEnd;

    /** The database product name. */
    private String databaseProductName;

    /** The database product version. */
    private String databaseProductVersion;

    /** The driver name. */
    private String driverName;

    /** The driver version. */
    private String driverVersion;

    /** The driver major version. */
    private int driverMajorVersion;

    /** The driver minor version. */
    private int driverMinorVersion;

    /** The uses local files. */
    private boolean usesLocalFiles;

    /** The uses local file per table. */
    private boolean usesLocalFilePerTable;

    /** The supports mixed case identifiers. */
    private boolean supportsMixedCaseIdentifiers;

    /** The stores upper case identifiers. */
    private boolean storesUpperCaseIdentifiers;

    /** The stores lower case identifiers. */
    private boolean storesLowerCaseIdentifiers;

    /** The stores mixed case identifiers. */
    private boolean storesMixedCaseIdentifiers;

    /** The supports mixed case quoted identifiers. */
    private boolean supportsMixedCaseQuotedIdentifiers;

    /** The stores upper case quoted identifiers. */
    private boolean storesUpperCaseQuotedIdentifiers;

    /** The stores lower case quoted identifiers. */
    private boolean storesLowerCaseQuotedIdentifiers;

    /** The stores mixed case quoted identifiers. */
    private boolean storesMixedCaseQuotedIdentifiers;

    /** The identifier quote string. */
    private String identifierQuoteString;

    /** The sql keywords. */
    private String sqlKeywords;

    /** The numeric functions. */
    private String numericFunctions;

    /** The string functions. */
    private String stringFunctions;

    /** The system functions. */
    private String systemFunctions;

    /** The time date functions. */
    private String timeDateFunctions;

    /** The search string escape. */
    private String searchStringEscape;

    /** The extra name characters. */
    private String extraNameCharacters;

    /** The supports alter table with add column. */
    private boolean supportsAlterTableWithAddColumn;

    /** The supports alter table with drop column. */
    private boolean supportsAlterTableWithDropColumn;

    /** The supports column aliasing. */
    private boolean supportsColumnAliasing;

    /** The null plus non null is null. */
    private boolean nullPlusNonNullIsNull;

    /** The supports convert. */
    private boolean supportsConvert;

    /** The supports table correlation names. */
    private boolean supportsTableCorrelationNames;

    /** The supports different table correlation names. */
    private boolean supportsDifferentTableCorrelationNames;

    /** The supports expressions in order by. */
    private boolean supportsExpressionsInOrderBy;

    /** The supports order by unrelated. */
    private boolean supportsOrderByUnrelated;

    /** The supports group by. */
    private boolean supportsGroupBy;

    /** The supports group by unrelated. */
    private boolean supportsGroupByUnrelated;

    /** The supports group by beyond select. */
    private boolean supportsGroupByBeyondSelect;

    /** The supports like escape clause. */
    private boolean supportsLikeEscapeClause;

    /** The supports multiple result sets. */
    private boolean supportsMultipleResultSets;

    /** The supports multiple transactions. */
    private boolean supportsMultipleTransactions;

    /** The supports non nullable columns. */
    private boolean supportsNonNullableColumns;

    /** The supports minimum SQL grammar. */
    private boolean supportsMinimumSQLGrammar;

    /** The supports core SQL grammar. */
    private boolean supportsCoreSQLGrammar;

    /** The supports extended SQL grammar. */
    private boolean supportsExtendedSQLGrammar;

    /** The supports ANSI 92 entry level SQL. */
    private boolean supportsANSI92EntryLevelSQL;

    /** The supports ANSI 92 intermediate SQL. */
    private boolean supportsANSI92IntermediateSQL;

    /** The supports ANSI 92 full SQL. */
    private boolean supportsANSI92FullSQL;

    /** The supports integrity enhancement facility. */
    private boolean supportsIntegrityEnhancementFacility;

    /** The supports outer joins. */
    private boolean supportsOuterJoins;

    /** The supports full outer joins. */
    private boolean supportsFullOuterJoins;

    /** The supports limited outer joins. */
    private boolean supportsLimitedOuterJoins;

    /** The schema term. */
    private String schemaTerm;

    /** The procedure term. */
    private String procedureTerm;

    /** The catalog term. */
    private String catalogTerm;

    /** The is catalog at start. */
    private boolean isCatalogAtStart;

    /** The catalog separator. */
    private String catalogSeparator;

    /** The supports schemas in data manipulation. */
    private boolean supportsSchemasInDataManipulation;

    /** The supports schemas in procedure calls. */
    private boolean supportsSchemasInProcedureCalls;

    /** The supports schemas in table definitions. */
    private boolean supportsSchemasInTableDefinitions;

    /** The supports schemas in index definitions. */
    private boolean supportsSchemasInIndexDefinitions;

    /** The supports schemas in privilege definitions. */
    private boolean supportsSchemasInPrivilegeDefinitions;

    /** The supports catalogs in data manipulation. */
    private boolean supportsCatalogsInDataManipulation;

    /** The supports catalogs in procedure calls. */
    private boolean supportsCatalogsInProcedureCalls;

    /** The supports catalogs in table definitions. */
    private boolean supportsCatalogsInTableDefinitions;

    /** The supports catalogs in index definitions. */
    private boolean supportsCatalogsInIndexDefinitions;

    /** The supports catalogs in privilege definitions. */
    private boolean supportsCatalogsInPrivilegeDefinitions;

    /** The supports positioned delete. */
    private boolean supportsPositionedDelete;

    /** The supports positioned update. */
    private boolean supportsPositionedUpdate;

    /** The supports select for update. */
    private boolean supportsSelectForUpdate;

    /** The supports stored procedures. */
    private boolean supportsStoredProcedures;

    /** The supports subqueries in comparisons. */
    private boolean supportsSubqueriesInComparisons;

    /** The supports subqueries in exists. */
    private boolean supportsSubqueriesInExists;

    /** The supports subqueries in ins. */
    private boolean supportsSubqueriesInIns;

    /** The supports subqueries in quantifieds. */
    private boolean supportsSubqueriesInQuantifieds;

    /** The supports correlated subqueries. */
    private boolean supportsCorrelatedSubqueries;

    /** The supports union. */
    private boolean supportsUnion;

    /** The supports union all. */
    private boolean supportsUnionAll;

    /** The supports open cursors across commit. */
    private boolean supportsOpenCursorsAcrossCommit;

    /** The supports open cursors across rollback. */
    private boolean supportsOpenCursorsAcrossRollback;

    /** The supports open statements across commit. */
    private boolean supportsOpenStatementsAcrossCommit;

    /** The supports open statements across rollback. */
    private boolean supportsOpenStatementsAcrossRollback;

    /** The max binary literal length. */
    private int maxBinaryLiteralLength;

    /** The max char literal length. */
    private int maxCharLiteralLength;

    /** The max column name length. */
    private int maxColumnNameLength;

    /** The max columns in group by. */
    private int maxColumnsInGroupBy;

    /** The max columns in index. */
    private int maxColumnsInIndex;

    /** The max columns in order by. */
    private int maxColumnsInOrderBy;

    /** The max columns in select. */
    private int maxColumnsInSelect;

    /** The max columns in table. */
    private int maxColumnsInTable;

    /** The max connections. */
    private int maxConnections;

    /** The max cursor name length. */
    private int maxCursorNameLength;

    /** The max index length. */
    private int maxIndexLength;

    /** The max schema name length. */
    private int maxSchemaNameLength;

    /** The max procedure name length. */
    private int maxProcedureNameLength;

    /** The max catalog name length. */
    private int maxCatalogNameLength;

    /** The max row size. */
    private int maxRowSize;

    /** The max row size include blobs. */
    private boolean maxRowSizeIncludeBlobs;

    /** The max statement length. */
    private int maxStatementLength;

    /** The max statements. */
    private int maxStatements;

    /** The max table name length. */
    private int maxTableNameLength;

    /** The max tables in select. */
    private int maxTablesInSelect;

    /** The max user name length. */
    private int maxUserNameLength;

    /** The default transaction isolation. */
    private int defaultTransactionIsolation;

    /** The supports transactions. */
    private boolean supportsTransactions;

    /** The supports data definition and data manipulation transactions. */
    private boolean supportsDataDefinitionAndDataManipulationTransactions;

    /** The supports data manipulation transactions only. */
    private boolean supportsDataManipulationTransactionsOnly;

    /** The data definition causes transaction commit. */
    private boolean dataDefinitionCausesTransactionCommit;

    /** The data definition ignored in transactions. */
    private boolean dataDefinitionIgnoredInTransactions;

    /** The supports batch updates. */
    private boolean supportsBatchUpdates;

    /** The supports savepoints. */
    private boolean supportsSavepoints;

    /** The supports named parameters. */
    private boolean supportsNamedParameters;

    /** The supports multiple open results. */
    private boolean supportsMultipleOpenResults;

    /** The supports get generated keys. */
    private boolean supportsGetGeneratedKeys;

    /** The result set holdability. */
    private int resultSetHoldability;

    /** The database major version. */
    private int databaseMajorVersion;

    /** The database minor version. */
    private int databaseMinorVersion;

    /** The JDBC major version. */
    private int jdbcMajorVersion;

    /** The JDBC minor version. */
    private int jdbcMinorVersion;

    /** The SQL state type. */
    private int sqlStateType;

    /** The locators update copy. */
    private boolean locatorsUpdateCopy;

    /** The supports statement pooling. */
    private boolean supportsStatementPooling;

    /** The supports stored functions using call syntax. */
    private boolean supportsStoredFunctionsUsingCallSyntax;

    /** The auto commit failure closes all result sets. */
    private boolean autoCommitFailureClosesAllResultSets;

    /** The generated key always returned. */
    private boolean generatedKeyAlwaysReturned;

    /** The max logical lob size. */
    private long maxLogicalLobSize;

    /** The supports ref cursors. */
    private boolean supportsRefCursors;

    /** The schemas. */
    private List<SchemaMetadata> schemas;

    /** The kind. */
    private String kind = "database";

    /**
     * Instantiates a new database metadata.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public DatabaseMetadata(Connection connection) throws SQLException {
        this(connection, null, null, null);
    }

    /**
     * Instantiates a new database metadata.
     *
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemaNameFilter the schema name filter
     * @param nameFilter the name filter
     * @throws SQLException the SQL exception
     */
    public DatabaseMetadata(Connection connection, String catalogName, Filter<String> schemaNameFilter, Filter<String> nameFilter)
            throws SQLException {
        super();
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try {
            this.allProceduresAreCallable = databaseMetaData.allProceduresAreCallable();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.allProceduresAreCallable()");
            }
        }
        try {
            this.allTablesAreSelectable = databaseMetaData.allTablesAreSelectable();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.allTablesAreSelectable()");
            }
        }
        try {
            this.url = databaseMetaData.getURL();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getURL()");
            }
        }
        try {
            this.userName = databaseMetaData.getUserName();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getUserName()");
            }
        }
        try {
            this.isReadOnly = databaseMetaData.isReadOnly();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.isReadOnly()");
            }
        }
        try {
            this.nullsAreSortedHigh = databaseMetaData.nullsAreSortedHigh();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.nullsAreSortedHigh()");
            }
        }
        try {
            this.nullsAreSortedLow = databaseMetaData.nullsAreSortedLow();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.nullsAreSortedLow()");
            }
        }
        try {
            this.nullsAreSortedAtStart = databaseMetaData.nullsAreSortedAtStart();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.nullsAreSortedAtStart()");
            }
        }
        try {
            this.nullsAreSortedAtEnd = databaseMetaData.nullsAreSortedAtEnd();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.nullsAreSortedAtEnd()");
            }
        }
        try {
            this.databaseProductName = databaseMetaData.getDatabaseProductName();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getDatabaseProductName()");
            }
        }
        try {
            this.databaseProductVersion = databaseMetaData.getDatabaseProductVersion();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getDatabaseProductVersion()");
            }
        }
        try {
            this.driverName = databaseMetaData.getDriverName();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getDriverName()");
            }
        }
        try {
            this.driverVersion = databaseMetaData.getDriverVersion();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getDriverVersion()");
            }
        }
        try {
            this.driverMajorVersion = databaseMetaData.getDriverMajorVersion();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getDriverMajorVersion()");
            }
        }
        try {
            this.driverMinorVersion = databaseMetaData.getDriverMinorVersion();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getDriverMinorVersion()");
            }
        }
        try {
            this.usesLocalFiles = databaseMetaData.usesLocalFiles();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.usesLocalFiles()");
            }
        }
        try {
            this.usesLocalFilePerTable = databaseMetaData.usesLocalFilePerTable();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.usesLocalFilePerTable()");
            }
        }
        try {
            this.supportsMixedCaseIdentifiers = databaseMetaData.supportsMixedCaseIdentifiers();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsMixedCaseIdentifiers()");
            }
        }
        try {
            this.storesUpperCaseIdentifiers = databaseMetaData.storesUpperCaseIdentifiers();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.storesUpperCaseIdentifiers()");
            }
        }
        try {
            this.storesLowerCaseIdentifiers = databaseMetaData.storesLowerCaseIdentifiers();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.storesLowerCaseIdentifiers()");
            }
        }
        try {
            this.storesMixedCaseIdentifiers = databaseMetaData.storesMixedCaseIdentifiers();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.storesMixedCaseIdentifiers()");
            }
        }
        try {
            this.supportsMixedCaseQuotedIdentifiers = databaseMetaData.supportsMixedCaseQuotedIdentifiers();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsMixedCaseQuotedIdentifiers()");
            }
        }
        try {
            this.storesUpperCaseQuotedIdentifiers = databaseMetaData.storesUpperCaseQuotedIdentifiers();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.storesUpperCaseQuotedIdentifiers()");
            }
        }
        try {
            this.storesLowerCaseQuotedIdentifiers = databaseMetaData.storesLowerCaseQuotedIdentifiers();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.storesLowerCaseQuotedIdentifiers()");
            }
        }
        try {
            this.storesMixedCaseQuotedIdentifiers = databaseMetaData.storesMixedCaseQuotedIdentifiers();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.storesMixedCaseQuotedIdentifiers()");
            }
        }
        try {
            this.identifierQuoteString = databaseMetaData.getIdentifierQuoteString();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getIdentifierQuoteString()");
            }
        }
        try {
            this.sqlKeywords = databaseMetaData.getSQLKeywords();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getSQLKeywords()");
            }
        }
        try {
            this.numericFunctions = databaseMetaData.getNumericFunctions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getNumericFunctions()");
            }
        }
        try {
            this.stringFunctions = databaseMetaData.getStringFunctions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getStringFunctions()");
            }
        }
        try {
            this.systemFunctions = databaseMetaData.getSystemFunctions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getSystemFunctions()");
            }
        }
        try {
            this.timeDateFunctions = databaseMetaData.getTimeDateFunctions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getTimeDateFunctions()");
            }
        }
        try {
            this.searchStringEscape = databaseMetaData.getSearchStringEscape();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getSearchStringEscape()");
            }
        }
        try {
            this.extraNameCharacters = databaseMetaData.getExtraNameCharacters();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getExtraNameCharacters()");
            }
        }
        try {
            this.supportsAlterTableWithAddColumn = databaseMetaData.supportsAlterTableWithAddColumn();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsAlterTableWithAddColumn()");
            }
        }
        try {
            this.supportsAlterTableWithDropColumn = databaseMetaData.supportsAlterTableWithDropColumn();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsAlterTableWithDropColumn()");
            }
        }
        try {
            this.supportsColumnAliasing = databaseMetaData.supportsColumnAliasing();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsColumnAliasing()");
            }
        }
        try {
            this.nullPlusNonNullIsNull = databaseMetaData.nullPlusNonNullIsNull();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.nullPlusNonNullIsNull()");
            }
        }
        try {
            this.supportsConvert = databaseMetaData.supportsConvert();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsConvert()");
            }
        }
        try {
            this.supportsTableCorrelationNames = databaseMetaData.supportsTableCorrelationNames();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsTableCorrelationNames()");
            }
        }
        try {
            this.supportsDifferentTableCorrelationNames = databaseMetaData.supportsDifferentTableCorrelationNames();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsDifferentTableCorrelationNames()");
            }
        }
        try {
            this.supportsExpressionsInOrderBy = databaseMetaData.supportsExpressionsInOrderBy();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsExpressionsInOrderBy()");
            }
        }
        try {
            this.supportsOrderByUnrelated = databaseMetaData.supportsOrderByUnrelated();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsOrderByUnrelated()");
            }
        }
        try {
            this.supportsGroupBy = databaseMetaData.supportsGroupBy();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsGroupBy()");
            }
        }
        try {
            this.supportsGroupByUnrelated = databaseMetaData.supportsGroupByUnrelated();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsGroupByUnrelated()");
            }
        }
        try {
            this.supportsGroupByBeyondSelect = databaseMetaData.supportsGroupByBeyondSelect();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsGroupByBeyondSelect()");
            }
        }
        try {
            this.supportsLikeEscapeClause = databaseMetaData.supportsLikeEscapeClause();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsLikeEscapeClause()");
            }
        }
        try {
            this.supportsMultipleResultSets = databaseMetaData.supportsMultipleResultSets();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsMultipleResultSets()");
            }
        }
        try {
            this.supportsMultipleTransactions = databaseMetaData.supportsMultipleTransactions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsMultipleTransactions()");
            }
        }
        try {
            this.supportsNonNullableColumns = databaseMetaData.supportsNonNullableColumns();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsNonNullableColumns()");
            }
        }
        try {
            this.supportsMinimumSQLGrammar = databaseMetaData.supportsMinimumSQLGrammar();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsMinimumSQLGrammar()");
            }
        }
        try {
            this.supportsCoreSQLGrammar = databaseMetaData.supportsCoreSQLGrammar();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsCoreSQLGrammar()");
            }
        }
        try {
            this.supportsExtendedSQLGrammar = databaseMetaData.supportsExtendedSQLGrammar();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsExtendedSQLGrammar()");
            }
        }
        try {
            this.supportsANSI92EntryLevelSQL = databaseMetaData.supportsANSI92EntryLevelSQL();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsANSI92EntryLevelSQL()");
            }
        }
        try {
            this.supportsANSI92IntermediateSQL = databaseMetaData.supportsANSI92IntermediateSQL();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsANSI92IntermediateSQL()");
            }
        }
        try {
            this.supportsANSI92FullSQL = databaseMetaData.supportsANSI92FullSQL();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsANSI92FullSQL()");
            }
        }
        try {
            this.supportsIntegrityEnhancementFacility = databaseMetaData.supportsIntegrityEnhancementFacility();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsIntegrityEnhancementFacility()");
            }
        }
        try {
            this.supportsOuterJoins = databaseMetaData.supportsOuterJoins();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsOuterJoins()");
            }
        }
        try {
            this.supportsFullOuterJoins = databaseMetaData.supportsFullOuterJoins();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsFullOuterJoins()");
            }
        }
        try {
            this.supportsLimitedOuterJoins = databaseMetaData.supportsLimitedOuterJoins();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsLimitedOuterJoins()");
            }
        }
        try {
            this.schemaTerm = databaseMetaData.getSchemaTerm();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getSchemaTerm()");
            }
        }
        try {
            this.procedureTerm = databaseMetaData.getProcedureTerm();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getProcedureTerm()");
            }
        }
        try {
            this.catalogTerm = databaseMetaData.getCatalogTerm();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getCatalogTerm()");
            }
        }
        try {
            this.isCatalogAtStart = databaseMetaData.isCatalogAtStart();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.isCatalogAtStart()");
            }
        }
        try {
            this.catalogSeparator = databaseMetaData.getCatalogSeparator();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getCatalogSeparator()");
            }
        }
        try {
            this.supportsSchemasInDataManipulation = databaseMetaData.supportsSchemasInDataManipulation();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSchemasInDataManipulation()");
            }
        }
        try {
            this.supportsSchemasInProcedureCalls = databaseMetaData.supportsSchemasInProcedureCalls();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSchemasInProcedureCalls()");
            }
        }
        try {
            this.supportsSchemasInTableDefinitions = databaseMetaData.supportsSchemasInTableDefinitions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSchemasInTableDefinitions()");
            }
        }
        try {
            this.supportsSchemasInIndexDefinitions = databaseMetaData.supportsSchemasInIndexDefinitions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSchemasInIndexDefinitions()");
            }
        }
        try {
            this.supportsSchemasInPrivilegeDefinitions = databaseMetaData.supportsSchemasInPrivilegeDefinitions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSchemasInPrivilegeDefinitions()");
            }
        }
        try {
            this.supportsCatalogsInDataManipulation = databaseMetaData.supportsCatalogsInDataManipulation();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsCatalogsInDataManipulation()");
            }
        }
        try {
            this.supportsCatalogsInProcedureCalls = databaseMetaData.supportsCatalogsInProcedureCalls();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsCatalogsInProcedureCalls()");
            }
        }
        try {
            this.supportsCatalogsInTableDefinitions = databaseMetaData.supportsCatalogsInTableDefinitions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsCatalogsInTableDefinitions()");
            }
        }
        try {
            this.supportsCatalogsInIndexDefinitions = databaseMetaData.supportsCatalogsInIndexDefinitions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsCatalogsInIndexDefinitions()");
            }
        }
        try {
            this.supportsCatalogsInPrivilegeDefinitions = databaseMetaData.supportsCatalogsInPrivilegeDefinitions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsCatalogsInPrivilegeDefinitions()");
            }
        }
        try {
            this.supportsPositionedDelete = databaseMetaData.supportsPositionedDelete();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsPositionedDelete()");
            }
        }
        try {
            this.supportsPositionedUpdate = databaseMetaData.supportsPositionedUpdate();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsPositionedUpdate()");
            }
        }
        try {
            this.supportsSelectForUpdate = databaseMetaData.supportsSelectForUpdate();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSelectForUpdate()");
            }
        }
        try {
            this.supportsStoredProcedures = databaseMetaData.supportsStoredProcedures();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsStoredProcedures()");
            }
        }
        try {
            this.supportsSubqueriesInComparisons = databaseMetaData.supportsSubqueriesInComparisons();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSubqueriesInComparisons()");
            }
        }
        try {
            this.supportsSubqueriesInExists = databaseMetaData.supportsSubqueriesInExists();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSubqueriesInExists()");
            }
        }
        try {
            this.supportsSubqueriesInIns = databaseMetaData.supportsSubqueriesInIns();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSubqueriesInIns()");
            }
        }
        try {
            this.supportsSubqueriesInQuantifieds = databaseMetaData.supportsSubqueriesInQuantifieds();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSubqueriesInQuantifieds()");
            }
        }
        try {
            this.supportsCorrelatedSubqueries = databaseMetaData.supportsCorrelatedSubqueries();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsCorrelatedSubqueries()");
            }
        }
        try {
            this.supportsUnion = databaseMetaData.supportsUnion();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsUnion()");
            }
        }
        try {
            this.supportsUnionAll = databaseMetaData.supportsUnionAll();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsUnionAll()");
            }
        }
        try {
            this.supportsOpenCursorsAcrossCommit = databaseMetaData.supportsOpenCursorsAcrossCommit();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsOpenCursorsAcrossCommit()");
            }
        }
        try {
            this.supportsOpenCursorsAcrossRollback = databaseMetaData.supportsOpenCursorsAcrossRollback();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsOpenCursorsAcrossRollback()");
            }
        }
        try {
            this.supportsOpenStatementsAcrossCommit = databaseMetaData.supportsOpenStatementsAcrossCommit();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsOpenStatementsAcrossCommit()");
            }
        }
        try {
            this.supportsOpenStatementsAcrossRollback = databaseMetaData.supportsOpenStatementsAcrossRollback();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsOpenStatementsAcrossRollback()");
            }
        }
        try {
            this.maxBinaryLiteralLength = databaseMetaData.getMaxBinaryLiteralLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxBinaryLiteralLength()");
            }
        }
        try {
            this.maxCharLiteralLength = databaseMetaData.getMaxCharLiteralLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxCharLiteralLength()");
            }
        }
        try {
            this.maxColumnNameLength = databaseMetaData.getMaxColumnNameLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxColumnNameLength()");
            }
        }
        try {
            this.maxColumnsInGroupBy = databaseMetaData.getMaxColumnsInGroupBy();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxColumnsInGroupBy()");
            }
        }
        try {
            this.maxColumnsInIndex = databaseMetaData.getMaxColumnsInIndex();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxColumnsInIndex()");
            }
        }
        try {
            this.maxColumnsInOrderBy = databaseMetaData.getMaxColumnsInOrderBy();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxColumnsInOrderBy()");
            }
        }
        try {
            this.maxColumnsInSelect = databaseMetaData.getMaxColumnsInSelect();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxColumnsInSelect()");
            }
        }
        try {
            this.maxColumnsInTable = databaseMetaData.getMaxColumnsInTable();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxColumnsInTable()");
            }
        }
        try {
            this.maxConnections = databaseMetaData.getMaxConnections();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxConnections()");
            }
        }
        try {
            this.maxCursorNameLength = databaseMetaData.getMaxCursorNameLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxCursorNameLength()");
            }
        }
        try {
            this.maxIndexLength = databaseMetaData.getMaxIndexLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxIndexLength()");
            }
        }
        try {
            this.maxSchemaNameLength = databaseMetaData.getMaxSchemaNameLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxSchemaNameLength()");
            }
        }
        try {
            this.maxProcedureNameLength = databaseMetaData.getMaxProcedureNameLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxProcedureNameLength()");
            }
        }
        try {
            this.maxCatalogNameLength = databaseMetaData.getMaxCatalogNameLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxCatalogNameLength()");
            }
        }
        try {
            this.maxRowSize = databaseMetaData.getMaxRowSize();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxRowSize()");
            }
        }
        try {
            this.maxRowSizeIncludeBlobs = databaseMetaData.doesMaxRowSizeIncludeBlobs();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.doesMaxRowSizeIncludeBlobs()");
            }
        }
        try {
            this.maxStatementLength = databaseMetaData.getMaxStatementLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxStatementLength()");
            }
        }
        try {
            this.maxStatements = databaseMetaData.getMaxStatements();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxStatements()");
            }
        }
        try {
            this.maxTableNameLength = databaseMetaData.getMaxTableNameLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxTableNameLength()");
            }
        }
        try {
            this.maxTablesInSelect = databaseMetaData.getMaxTablesInSelect();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxTablesInSelect()");
            }
        }
        try {
            this.maxUserNameLength = databaseMetaData.getMaxUserNameLength();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxUserNameLength()");
            }
        }
        try {
            this.defaultTransactionIsolation = databaseMetaData.getDefaultTransactionIsolation();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getDefaultTransactionIsolation()");
            }
        }
        try {
            this.supportsTransactions = databaseMetaData.supportsTransactions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsTransactions()");
            }
        }
        try {
            this.supportsDataDefinitionAndDataManipulationTransactions =
                    databaseMetaData.supportsDataDefinitionAndDataManipulationTransactions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsDataDefinitionAndDataManipulationTransactions()");
            }
        }
        try {
            this.supportsDataManipulationTransactionsOnly = databaseMetaData.supportsDataManipulationTransactionsOnly();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsDataManipulationTransactionsOnly()");
            }
        }
        try {
            this.dataDefinitionCausesTransactionCommit = databaseMetaData.dataDefinitionCausesTransactionCommit();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.dataDefinitionCausesTransactionCommit()");
            }
        }
        try {
            this.dataDefinitionIgnoredInTransactions = databaseMetaData.dataDefinitionIgnoredInTransactions();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.dataDefinitionIgnoredInTransactions()");
            }
        }
        try {
            this.supportsBatchUpdates = databaseMetaData.supportsBatchUpdates();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsBatchUpdates()");
            }
        }
        try {
            this.supportsSavepoints = databaseMetaData.supportsSavepoints();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsSavepoints()");
            }
        }
        try {
            this.supportsNamedParameters = databaseMetaData.supportsNamedParameters();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsNamedParameters()");
            }
        }
        try {
            this.supportsMultipleOpenResults = databaseMetaData.supportsMultipleOpenResults();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsMultipleOpenResults()");
            }
        }
        try {
            this.supportsGetGeneratedKeys = databaseMetaData.supportsGetGeneratedKeys();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsGetGeneratedKeys()");
            }
        }
        try {
            this.resultSetHoldability = databaseMetaData.getResultSetHoldability();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getResultSetHoldability()");
            }
        }
        try {
            this.databaseMajorVersion = databaseMetaData.getDatabaseMajorVersion();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getDatabaseMajorVersion()");
            }
        }
        try {
            this.databaseMinorVersion = databaseMetaData.getDatabaseMinorVersion();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getDatabaseMinorVersion()");
            }
        }
        try {
            this.jdbcMajorVersion = databaseMetaData.getJDBCMajorVersion();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getJDBCMajorVersion()");
            }
        }
        try {
            this.jdbcMinorVersion = databaseMetaData.getJDBCMinorVersion();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getJDBCMinorVersion()");
            }
        }
        try {
            this.sqlStateType = databaseMetaData.getSQLStateType();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getSQLStateType()");
            }
        }
        try {
            this.locatorsUpdateCopy = databaseMetaData.locatorsUpdateCopy();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.locatorsUpdateCopy()");
            }
        }
        try {
            this.supportsStatementPooling = databaseMetaData.supportsStatementPooling();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsStatementPooling()");
            }
        }
        try {
            this.supportsStoredFunctionsUsingCallSyntax = databaseMetaData.supportsStoredFunctionsUsingCallSyntax();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsStoredFunctionsUsingCallSyntax()");
            }
        }
        try {
            this.autoCommitFailureClosesAllResultSets = databaseMetaData.autoCommitFailureClosesAllResultSets();
        } catch (Exception e1) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.autoCommitFailureClosesAllResultSets()");
            }
        }
        try {
            this.generatedKeyAlwaysReturned = databaseMetaData.generatedKeyAlwaysReturned();
        } catch (Throwable e) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.generatedKeyAlwaysReturned()");
            }
        }
        try {
            this.maxLogicalLobSize = databaseMetaData.getMaxLogicalLobSize();
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.getMaxLogicalLobSize()");
            }
        }
        try {
            this.supportsRefCursors = databaseMetaData.supportsRefCursors();
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(NOT_SUPPORTED + "DatabaseMetaData.supportsRefCursors()");
            }
        }

        this.schemas = DatabaseMetadataHelper.listSchemas(connection, catalogName, schemaNameFilter, nameFilter);
    }

    /**
     * Checks if is all procedures are callable.
     *
     * @return true, if is all procedures are callable
     */
    public boolean isAllProceduresAreCallable() {
        return allProceduresAreCallable;
    }

    /**
     * Sets the all procedures are callable.
     *
     * @param allProceduresAreCallable the new all procedures are callable
     */
    public void setAllProceduresAreCallable(boolean allProceduresAreCallable) {
        this.allProceduresAreCallable = allProceduresAreCallable;
    }

    /**
     * Checks if is all tables are selectable.
     *
     * @return true, if is all tables are selectable
     */
    public boolean isAllTablesAreSelectable() {
        return allTablesAreSelectable;
    }

    /**
     * Sets the all tables are selectable.
     *
     * @param allTablesAreSelectable the new all tables are selectable
     */
    public void setAllTablesAreSelectable(boolean allTablesAreSelectable) {
        this.allTablesAreSelectable = allTablesAreSelectable;
    }

    /**
     * Gets the URL.
     *
     * @return the URL
     */
    public String getURL() {
        return url;
    }

    /**
     * Sets the URL.
     *
     * @param url the new URL
     */
    public void setURL(String url) {
        this.url = url;
    }

    /**
     * Gets the user name.
     *
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the gets the user name.
     *
     * @param userName the new gets the user name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Checks if is read only.
     *
     * @return true, if is read only
     */
    public boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * Sets the read only.
     *
     * @param isReadOnly the new read only
     */
    public void setReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    /**
     * Checks if is nulls are sorted high.
     *
     * @return true, if is nulls are sorted high
     */
    public boolean isNullsAreSortedHigh() {
        return nullsAreSortedHigh;
    }

    /**
     * Sets the nulls are sorted high.
     *
     * @param nullsAreSortedHigh the new nulls are sorted high
     */
    public void setNullsAreSortedHigh(boolean nullsAreSortedHigh) {
        this.nullsAreSortedHigh = nullsAreSortedHigh;
    }

    /**
     * Checks if is nulls are sorted low.
     *
     * @return true, if is nulls are sorted low
     */
    public boolean isNullsAreSortedLow() {
        return nullsAreSortedLow;
    }

    /**
     * Sets the nulls are sorted low.
     *
     * @param nullsAreSortedLow the new nulls are sorted low
     */
    public void setNullsAreSortedLow(boolean nullsAreSortedLow) {
        this.nullsAreSortedLow = nullsAreSortedLow;
    }

    /**
     * Checks if is nulls are sorted at start.
     *
     * @return true, if is nulls are sorted at start
     */
    public boolean isNullsAreSortedAtStart() {
        return nullsAreSortedAtStart;
    }

    /**
     * Sets the nulls are sorted at start.
     *
     * @param nullsAreSortedAtStart the new nulls are sorted at start
     */
    public void setNullsAreSortedAtStart(boolean nullsAreSortedAtStart) {
        this.nullsAreSortedAtStart = nullsAreSortedAtStart;
    }

    /**
     * Checks if is nulls are sorted at end.
     *
     * @return true, if is nulls are sorted at end
     */
    public boolean isNullsAreSortedAtEnd() {
        return nullsAreSortedAtEnd;
    }

    /**
     * Sets the nulls are sorted at end.
     *
     * @param nullsAreSortedAtEnd the new nulls are sorted at end
     */
    public void setNullsAreSortedAtEnd(boolean nullsAreSortedAtEnd) {
        this.nullsAreSortedAtEnd = nullsAreSortedAtEnd;
    }

    /**
     * Gets the database product name.
     *
     * @return the database product name
     */
    public String getDatabaseProductName() {
        return databaseProductName;
    }

    /**
     * Sets the database product name.
     *
     * @param databaseProductName the new database product name
     */
    public void setDatabaseProductName(String databaseProductName) {
        this.databaseProductName = databaseProductName;
    }

    /**
     * Gets the database product version.
     *
     * @return the database product version
     */
    public String getDatabaseProductVersion() {
        return databaseProductVersion;
    }

    /**
     * Sets the database product version.
     *
     * @param databaseProductVersion the new database product version
     */
    public void setDatabaseProductVersion(String databaseProductVersion) {
        this.databaseProductVersion = databaseProductVersion;
    }

    /**
     * Gets the driver name.
     *
     * @return the driver name
     */
    public String getDriverName() {
        return driverName;
    }

    /**
     * Sets the driver name.
     *
     * @param driverName the new driver name
     */
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    /**
     * Gets the driver version.
     *
     * @return the driver version
     */
    public String getDriverVersion() {
        return driverVersion;
    }

    /**
     * Sets the driver version.
     *
     * @param driverVersion the new driver version
     */
    public void setDriverVersion(String driverVersion) {
        this.driverVersion = driverVersion;
    }

    /**
     * Gets the driver major version.
     *
     * @return the driver major version
     */
    public int getDriverMajorVersion() {
        return driverMajorVersion;
    }

    /**
     * Sets the driver major version.
     *
     * @param driverMajorVersion the new driver major version
     */
    public void setDriverMajorVersion(int driverMajorVersion) {
        this.driverMajorVersion = driverMajorVersion;
    }

    /**
     * Gets the driver minor version.
     *
     * @return the driver minor version
     */
    public int getDriverMinorVersion() {
        return driverMinorVersion;
    }

    /**
     * Sets the driver minor version.
     *
     * @param driverMinorVersion the new driver minor version
     */
    public void setDriverMinorVersion(int driverMinorVersion) {
        this.driverMinorVersion = driverMinorVersion;
    }

    /**
     * Checks if is uses local files.
     *
     * @return true, if is uses local files
     */
    public boolean isUsesLocalFiles() {
        return usesLocalFiles;
    }

    /**
     * Sets the uses local files.
     *
     * @param usesLocalFiles the new uses local files
     */
    public void setUsesLocalFiles(boolean usesLocalFiles) {
        this.usesLocalFiles = usesLocalFiles;
    }

    /**
     * Checks if is uses local file per table.
     *
     * @return true, if is uses local file per table
     */
    public boolean isUsesLocalFilePerTable() {
        return usesLocalFilePerTable;
    }

    /**
     * Sets the uses local file per table.
     *
     * @param usesLocalFilePerTable the new uses local file per table
     */
    public void setUsesLocalFilePerTable(boolean usesLocalFilePerTable) {
        this.usesLocalFilePerTable = usesLocalFilePerTable;
    }

    /**
     * Checks if is supports mixed case identifiers.
     *
     * @return true, if is supports mixed case identifiers
     */
    public boolean isSupportsMixedCaseIdentifiers() {
        return supportsMixedCaseIdentifiers;
    }

    /**
     * Sets the supports mixed case identifiers.
     *
     * @param supportsMixedCaseIdentifiers the new supports mixed case identifiers
     */
    public void setSupportsMixedCaseIdentifiers(boolean supportsMixedCaseIdentifiers) {
        this.supportsMixedCaseIdentifiers = supportsMixedCaseIdentifiers;
    }

    /**
     * Checks if is stores upper case identifiers.
     *
     * @return true, if is stores upper case identifiers
     */
    public boolean isStoresUpperCaseIdentifiers() {
        return storesUpperCaseIdentifiers;
    }

    /**
     * Sets the stores upper case identifiers.
     *
     * @param storesUpperCaseIdentifiers the new stores upper case identifiers
     */
    public void setStoresUpperCaseIdentifiers(boolean storesUpperCaseIdentifiers) {
        this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
    }

    /**
     * Checks if is stores lower case identifiers.
     *
     * @return true, if is stores lower case identifiers
     */
    public boolean isStoresLowerCaseIdentifiers() {
        return storesLowerCaseIdentifiers;
    }

    /**
     * Sets the stores lower case identifiers.
     *
     * @param storesLowerCaseIdentifiers the new stores lower case identifiers
     */
    public void setStoresLowerCaseIdentifiers(boolean storesLowerCaseIdentifiers) {
        this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;
    }

    /**
     * Checks if is stores mixed case identifiers.
     *
     * @return true, if is stores mixed case identifiers
     */
    public boolean isStoresMixedCaseIdentifiers() {
        return storesMixedCaseIdentifiers;
    }

    /**
     * Sets the stores mixed case identifiers.
     *
     * @param storesMixedCaseIdentifiers the new stores mixed case identifiers
     */
    public void setStoresMixedCaseIdentifiers(boolean storesMixedCaseIdentifiers) {
        this.storesMixedCaseIdentifiers = storesMixedCaseIdentifiers;
    }

    /**
     * Checks if is supports mixed case quoted identifiers.
     *
     * @return true, if is supports mixed case quoted identifiers
     */
    public boolean isSupportsMixedCaseQuotedIdentifiers() {
        return supportsMixedCaseQuotedIdentifiers;
    }

    /**
     * Sets the supports mixed case quoted identifiers.
     *
     * @param supportsMixedCaseQuotedIdentifiers the new supports mixed case quoted identifiers
     */
    public void setSupportsMixedCaseQuotedIdentifiers(boolean supportsMixedCaseQuotedIdentifiers) {
        this.supportsMixedCaseQuotedIdentifiers = supportsMixedCaseQuotedIdentifiers;
    }

    /**
     * Checks if is stores upper case quoted identifiers.
     *
     * @return true, if is stores upper case quoted identifiers
     */
    public boolean isStoresUpperCaseQuotedIdentifiers() {
        return storesUpperCaseQuotedIdentifiers;
    }

    /**
     * Sets the stores upper case quoted identifiers.
     *
     * @param storesUpperCaseQuotedIdentifiers the new stores upper case quoted identifiers
     */
    public void setStoresUpperCaseQuotedIdentifiers(boolean storesUpperCaseQuotedIdentifiers) {
        this.storesUpperCaseQuotedIdentifiers = storesUpperCaseQuotedIdentifiers;
    }

    /**
     * Checks if is stores lower case quoted identifiers.
     *
     * @return true, if is stores lower case quoted identifiers
     */
    public boolean isStoresLowerCaseQuotedIdentifiers() {
        return storesLowerCaseQuotedIdentifiers;
    }

    /**
     * Sets the stores lower case quoted identifiers.
     *
     * @param storesLowerCaseQuotedIdentifiers the new stores lower case quoted identifiers
     */
    public void setStoresLowerCaseQuotedIdentifiers(boolean storesLowerCaseQuotedIdentifiers) {
        this.storesLowerCaseQuotedIdentifiers = storesLowerCaseQuotedIdentifiers;
    }

    /**
     * Checks if is stores mixed case quoted identifiers.
     *
     * @return true, if is stores mixed case quoted identifiers
     */
    public boolean isStoresMixedCaseQuotedIdentifiers() {
        return storesMixedCaseQuotedIdentifiers;
    }

    /**
     * Sets the stores mixed case quoted identifiers.
     *
     * @param storesMixedCaseQuotedIdentifiers the new stores mixed case quoted identifiers
     */
    public void setStoresMixedCaseQuotedIdentifiers(boolean storesMixedCaseQuotedIdentifiers) {
        this.storesMixedCaseQuotedIdentifiers = storesMixedCaseQuotedIdentifiers;
    }

    /**
     * Gets the identifier quote string.
     *
     * @return the identifier quote string
     */
    public String getIdentifierQuoteString() {
        return identifierQuoteString;
    }

    /**
     * Sets the identifier quote string.
     *
     * @param identifierQuoteString the new identifier quote string
     */
    public void setIdentifierQuoteString(String identifierQuoteString) {
        this.identifierQuoteString = identifierQuoteString;
    }

    /**
     * Gets the sql keywords.
     *
     * @return the sql keywords
     */
    public String getSqlKeywords() {
        return sqlKeywords;
    }

    /**
     * Sets the sql keywords.
     *
     * @param sqlKeywords the new sql keywords
     */
    public void setSqlKeywords(String sqlKeywords) {
        this.sqlKeywords = sqlKeywords;
    }

    /**
     * Gets the numeric functions.
     *
     * @return the numeric functions
     */
    public String getNumericFunctions() {
        return numericFunctions;
    }

    /**
     * Sets the numeric functions.
     *
     * @param numericFunctions the new numeric functions
     */
    public void setNumericFunctions(String numericFunctions) {
        this.numericFunctions = numericFunctions;
    }

    /**
     * Gets the string functions.
     *
     * @return the string functions
     */
    public String getStringFunctions() {
        return stringFunctions;
    }

    /**
     * Sets the string functions.
     *
     * @param stringFunctions the new string functions
     */
    public void setStringFunctions(String stringFunctions) {
        this.stringFunctions = stringFunctions;
    }

    /**
     * Gets the system functions.
     *
     * @return the system functions
     */
    public String getSystemFunctions() {
        return systemFunctions;
    }

    /**
     * Sets the system functions.
     *
     * @param systemFunctions the new system functions
     */
    public void setSystemFunctions(String systemFunctions) {
        this.systemFunctions = systemFunctions;
    }

    /**
     * Gets the time date functions.
     *
     * @return the time date functions
     */
    public String getTimeDateFunctions() {
        return timeDateFunctions;
    }

    /**
     * Sets the time date functions.
     *
     * @param timeDateFunctions the new time date functions
     */
    public void setTimeDateFunctions(String timeDateFunctions) {
        this.timeDateFunctions = timeDateFunctions;
    }

    /**
     * Gets the search string escape.
     *
     * @return the search string escape
     */
    public String getSearchStringEscape() {
        return searchStringEscape;
    }

    /**
     * Sets the search string escape.
     *
     * @param searchStringEscape the new search string escape
     */
    public void setSearchStringEscape(String searchStringEscape) {
        this.searchStringEscape = searchStringEscape;
    }

    /**
     * Gets the extra name characters.
     *
     * @return the extra name characters
     */
    public String getExtraNameCharacters() {
        return extraNameCharacters;
    }

    /**
     * Sets the extra name characters.
     *
     * @param extraNameCharacters the new extra name characters
     */
    public void setExtraNameCharacters(String extraNameCharacters) {
        this.extraNameCharacters = extraNameCharacters;
    }

    /**
     * Checks if is supports alter table with add column.
     *
     * @return true, if is supports alter table with add column
     */
    public boolean isSupportsAlterTableWithAddColumn() {
        return supportsAlterTableWithAddColumn;
    }

    /**
     * Sets the supports alter table with add column.
     *
     * @param supportsAlterTableWithAddColumn the new supports alter table with add column
     */
    public void setSupportsAlterTableWithAddColumn(boolean supportsAlterTableWithAddColumn) {
        this.supportsAlterTableWithAddColumn = supportsAlterTableWithAddColumn;
    }

    /**
     * Checks if is supports alter table with drop column.
     *
     * @return true, if is supports alter table with drop column
     */
    public boolean isSupportsAlterTableWithDropColumn() {
        return supportsAlterTableWithDropColumn;
    }

    /**
     * Sets the supports alter table with drop column.
     *
     * @param supportsAlterTableWithDropColumn the new supports alter table with drop column
     */
    public void setSupportsAlterTableWithDropColumn(boolean supportsAlterTableWithDropColumn) {
        this.supportsAlterTableWithDropColumn = supportsAlterTableWithDropColumn;
    }

    /**
     * Checks if is supports column aliasing.
     *
     * @return true, if is supports column aliasing
     */
    public boolean isSupportsColumnAliasing() {
        return supportsColumnAliasing;
    }

    /**
     * Sets the supports column aliasing.
     *
     * @param supportsColumnAliasing the new supports column aliasing
     */
    public void setSupportsColumnAliasing(boolean supportsColumnAliasing) {
        this.supportsColumnAliasing = supportsColumnAliasing;
    }

    /**
     * Checks if is null plus non null is null.
     *
     * @return true, if is null plus non null is null
     */
    public boolean isNullPlusNonNullIsNull() {
        return nullPlusNonNullIsNull;
    }

    /**
     * Sets the null plus non null is null.
     *
     * @param nullPlusNonNullIsNull the new null plus non null is null
     */
    public void setNullPlusNonNullIsNull(boolean nullPlusNonNullIsNull) {
        this.nullPlusNonNullIsNull = nullPlusNonNullIsNull;
    }

    /**
     * Checks if is supports convert.
     *
     * @return true, if is supports convert
     */
    public boolean isSupportsConvert() {
        return supportsConvert;
    }

    /**
     * Sets the supports convert.
     *
     * @param supportsConvert the new supports convert
     */
    public void setSupportsConvert(boolean supportsConvert) {
        this.supportsConvert = supportsConvert;
    }

    /**
     * Checks if is supports table correlation names.
     *
     * @return true, if is supports table correlation names
     */
    public boolean isSupportsTableCorrelationNames() {
        return supportsTableCorrelationNames;
    }

    /**
     * Sets the supports table correlation names.
     *
     * @param supportsTableCorrelationNames the new supports table correlation names
     */
    public void setSupportsTableCorrelationNames(boolean supportsTableCorrelationNames) {
        this.supportsTableCorrelationNames = supportsTableCorrelationNames;
    }

    /**
     * Checks if is supports different table correlation names.
     *
     * @return true, if is supports different table correlation names
     */
    public boolean isSupportsDifferentTableCorrelationNames() {
        return supportsDifferentTableCorrelationNames;
    }

    /**
     * Sets the supports different table correlation names.
     *
     * @param supportsDifferentTableCorrelationNames the new supports different table correlation names
     */
    public void setSupportsDifferentTableCorrelationNames(boolean supportsDifferentTableCorrelationNames) {
        this.supportsDifferentTableCorrelationNames = supportsDifferentTableCorrelationNames;
    }

    /**
     * Checks if is supports expressions in order by.
     *
     * @return true, if is supports expressions in order by
     */
    public boolean isSupportsExpressionsInOrderBy() {
        return supportsExpressionsInOrderBy;
    }

    /**
     * Sets the supports expressions in order by.
     *
     * @param supportsExpressionsInOrderBy the new supports expressions in order by
     */
    public void setSupportsExpressionsInOrderBy(boolean supportsExpressionsInOrderBy) {
        this.supportsExpressionsInOrderBy = supportsExpressionsInOrderBy;
    }

    /**
     * Checks if is supports order by unrelated.
     *
     * @return true, if is supports order by unrelated
     */
    public boolean isSupportsOrderByUnrelated() {
        return supportsOrderByUnrelated;
    }

    /**
     * Sets the supports order by unrelated.
     *
     * @param supportsOrderByUnrelated the new supports order by unrelated
     */
    public void setSupportsOrderByUnrelated(boolean supportsOrderByUnrelated) {
        this.supportsOrderByUnrelated = supportsOrderByUnrelated;
    }

    /**
     * Checks if is supports group by.
     *
     * @return true, if is supports group by
     */
    public boolean isSupportsGroupBy() {
        return supportsGroupBy;
    }

    /**
     * Sets the supports group by.
     *
     * @param supportsGroupBy the new supports group by
     */
    public void setSupportsGroupBy(boolean supportsGroupBy) {
        this.supportsGroupBy = supportsGroupBy;
    }

    /**
     * Checks if is supports group by unrelated.
     *
     * @return true, if is supports group by unrelated
     */
    public boolean isSupportsGroupByUnrelated() {
        return supportsGroupByUnrelated;
    }

    /**
     * Sets the supports group by unrelated.
     *
     * @param supportsGroupByUnrelated the new supports group by unrelated
     */
    public void setSupportsGroupByUnrelated(boolean supportsGroupByUnrelated) {
        this.supportsGroupByUnrelated = supportsGroupByUnrelated;
    }

    /**
     * Checks if is supports group by beyond select.
     *
     * @return true, if is supports group by beyond select
     */
    public boolean isSupportsGroupByBeyondSelect() {
        return supportsGroupByBeyondSelect;
    }

    /**
     * Sets the supports group by beyond select.
     *
     * @param supportsGroupByBeyondSelect the new supports group by beyond select
     */
    public void setSupportsGroupByBeyondSelect(boolean supportsGroupByBeyondSelect) {
        this.supportsGroupByBeyondSelect = supportsGroupByBeyondSelect;
    }

    /**
     * Checks if is supports like escape clause.
     *
     * @return true, if is supports like escape clause
     */
    public boolean isSupportsLikeEscapeClause() {
        return supportsLikeEscapeClause;
    }

    /**
     * Sets the supports like escape clause.
     *
     * @param supportsLikeEscapeClause the new supports like escape clause
     */
    public void setSupportsLikeEscapeClause(boolean supportsLikeEscapeClause) {
        this.supportsLikeEscapeClause = supportsLikeEscapeClause;
    }

    /**
     * Checks if is supports multiple result sets.
     *
     * @return true, if is supports multiple result sets
     */
    public boolean isSupportsMultipleResultSets() {
        return supportsMultipleResultSets;
    }

    /**
     * Sets the supports multiple result sets.
     *
     * @param supportsMultipleResultSets the new supports multiple result sets
     */
    public void setSupportsMultipleResultSets(boolean supportsMultipleResultSets) {
        this.supportsMultipleResultSets = supportsMultipleResultSets;
    }

    /**
     * Checks if is supports multiple transactions.
     *
     * @return true, if is supports multiple transactions
     */
    public boolean isSupportsMultipleTransactions() {
        return supportsMultipleTransactions;
    }

    /**
     * Sets the supports multiple transactions.
     *
     * @param supportsMultipleTransactions the new supports multiple transactions
     */
    public void setSupportsMultipleTransactions(boolean supportsMultipleTransactions) {
        this.supportsMultipleTransactions = supportsMultipleTransactions;
    }

    /**
     * Checks if is supports non nullable columns.
     *
     * @return true, if is supports non nullable columns
     */
    public boolean isSupportsNonNullableColumns() {
        return supportsNonNullableColumns;
    }

    /**
     * Sets the supports non nullable columns.
     *
     * @param supportsNonNullableColumns the new supports non nullable columns
     */
    public void setSupportsNonNullableColumns(boolean supportsNonNullableColumns) {
        this.supportsNonNullableColumns = supportsNonNullableColumns;
    }

    /**
     * Checks if is supports minimum SQL grammar.
     *
     * @return true, if is supports minimum SQL grammar
     */
    public boolean isSupportsMinimumSQLGrammar() {
        return supportsMinimumSQLGrammar;
    }

    /**
     * Sets the supports minimum SQL grammar.
     *
     * @param supportsMinimumSQLGrammar the new supports minimum SQL grammar
     */
    public void setSupportsMinimumSQLGrammar(boolean supportsMinimumSQLGrammar) {
        this.supportsMinimumSQLGrammar = supportsMinimumSQLGrammar;
    }

    /**
     * Checks if is supports core SQL grammar.
     *
     * @return true, if is supports core SQL grammar
     */
    public boolean isSupportsCoreSQLGrammar() {
        return supportsCoreSQLGrammar;
    }

    /**
     * Sets the supports core SQL grammar.
     *
     * @param supportsCoreSQLGrammar the new supports core SQL grammar
     */
    public void setSupportsCoreSQLGrammar(boolean supportsCoreSQLGrammar) {
        this.supportsCoreSQLGrammar = supportsCoreSQLGrammar;
    }

    /**
     * Checks if is supports extended SQL grammar.
     *
     * @return true, if is supports extended SQL grammar
     */
    public boolean isSupportsExtendedSQLGrammar() {
        return supportsExtendedSQLGrammar;
    }

    /**
     * Sets the supports extended SQL grammar.
     *
     * @param supportsExtendedSQLGrammar the new supports extended SQL grammar
     */
    public void setSupportsExtendedSQLGrammar(boolean supportsExtendedSQLGrammar) {
        this.supportsExtendedSQLGrammar = supportsExtendedSQLGrammar;
    }

    /**
     * Checks if is supports ANSI 92 entry level SQL.
     *
     * @return true, if is supports ANSI 92 entry level SQL
     */
    public boolean isSupportsANSI92EntryLevelSQL() {
        return supportsANSI92EntryLevelSQL;
    }

    /**
     * Sets the supports ANSI 92 entry level SQL.
     *
     * @param supportsANSI92EntryLevelSQL the new supports ANSI 92 entry level SQL
     */
    public void setSupportsANSI92EntryLevelSQL(boolean supportsANSI92EntryLevelSQL) {
        this.supportsANSI92EntryLevelSQL = supportsANSI92EntryLevelSQL;
    }

    /**
     * Checks if is supports ANSI 92 intermediate SQL.
     *
     * @return true, if is supports ANSI 92 intermediate SQL
     */
    public boolean isSupportsANSI92IntermediateSQL() {
        return supportsANSI92IntermediateSQL;
    }

    /**
     * Sets the supports ANSI 92 intermediate SQL.
     *
     * @param supportsANSI92IntermediateSQL the new supports ANSI 92 intermediate SQL
     */
    public void setSupportsANSI92IntermediateSQL(boolean supportsANSI92IntermediateSQL) {
        this.supportsANSI92IntermediateSQL = supportsANSI92IntermediateSQL;
    }

    /**
     * Checks if is supports ANSI 92 full SQL.
     *
     * @return true, if is supports ANSI 92 full SQL
     */
    public boolean isSupportsANSI92FullSQL() {
        return supportsANSI92FullSQL;
    }

    /**
     * Sets the supports ANSI 92 full SQL.
     *
     * @param supportsANSI92FullSQL the new supports ANSI 92 full SQL
     */
    public void setSupportsANSI92FullSQL(boolean supportsANSI92FullSQL) {
        this.supportsANSI92FullSQL = supportsANSI92FullSQL;
    }

    /**
     * Checks if is supports integrity enhancement facility.
     *
     * @return true, if is supports integrity enhancement facility
     */
    public boolean isSupportsIntegrityEnhancementFacility() {
        return supportsIntegrityEnhancementFacility;
    }

    /**
     * Sets the supports integrity enhancement facility.
     *
     * @param supportsIntegrityEnhancementFacility the new supports integrity enhancement facility
     */
    public void setSupportsIntegrityEnhancementFacility(boolean supportsIntegrityEnhancementFacility) {
        this.supportsIntegrityEnhancementFacility = supportsIntegrityEnhancementFacility;
    }

    /**
     * Checks if is supports outer joins.
     *
     * @return true, if is supports outer joins
     */
    public boolean isSupportsOuterJoins() {
        return supportsOuterJoins;
    }

    /**
     * Sets the supports outer joins.
     *
     * @param supportsOuterJoins the new supports outer joins
     */
    public void setSupportsOuterJoins(boolean supportsOuterJoins) {
        this.supportsOuterJoins = supportsOuterJoins;
    }

    /**
     * Checks if is supports full outer joins.
     *
     * @return true, if is supports full outer joins
     */
    public boolean isSupportsFullOuterJoins() {
        return supportsFullOuterJoins;
    }

    /**
     * Sets the supports full outer joins.
     *
     * @param supportsFullOuterJoins the new supports full outer joins
     */
    public void setSupportsFullOuterJoins(boolean supportsFullOuterJoins) {
        this.supportsFullOuterJoins = supportsFullOuterJoins;
    }

    /**
     * Checks if is supports limited outer joins.
     *
     * @return true, if is supports limited outer joins
     */
    public boolean isSupportsLimitedOuterJoins() {
        return supportsLimitedOuterJoins;
    }

    /**
     * Sets the supports limited outer joins.
     *
     * @param supportsLimitedOuterJoins the new supports limited outer joins
     */
    public void setSupportsLimitedOuterJoins(boolean supportsLimitedOuterJoins) {
        this.supportsLimitedOuterJoins = supportsLimitedOuterJoins;
    }

    /**
     * Gets the schema term.
     *
     * @return the schema term
     */
    public String getSchemaTerm() {
        return schemaTerm;
    }

    /**
     * Sets the schema term.
     *
     * @param schemaTerm the new schema term
     */
    public void setSchemaTerm(String schemaTerm) {
        this.schemaTerm = schemaTerm;
    }

    /**
     * Gets the procedure term.
     *
     * @return the procedure term
     */
    public String getProcedureTerm() {
        return procedureTerm;
    }

    /**
     * Sets the procedure term.
     *
     * @param procedureTerm the new procedure term
     */
    public void setProcedureTerm(String procedureTerm) {
        this.procedureTerm = procedureTerm;
    }

    /**
     * Gets the catalog term.
     *
     * @return the catalog term
     */
    public String getCatalogTerm() {
        return catalogTerm;
    }

    /**
     * Sets the catalog term.
     *
     * @param catalogTerm the new catalog term
     */
    public void setCatalogTerm(String catalogTerm) {
        this.catalogTerm = catalogTerm;
    }

    /**
     * Checks if is catalog at start.
     *
     * @return true, if is catalog at start
     */
    public boolean isCatalogAtStart() {
        return isCatalogAtStart;
    }

    /**
     * Sets the catalog at start.
     *
     * @param isCatalogAtStart the new catalog at start
     */
    public void setCatalogAtStart(boolean isCatalogAtStart) {
        this.isCatalogAtStart = isCatalogAtStart;
    }

    /**
     * Gets the the catalog separator.
     *
     * @return the catalog separator
     */
    public String getCatalogSeparator() {
        return catalogSeparator;
    }

    /**
     * Sets the catalog separator.
     *
     * @param catalogSeparator the new catalog separator
     */
    public void setCatalogSeparator(String catalogSeparator) {
        this.catalogSeparator = catalogSeparator;
    }

    /**
     * Checks if is supports schemas in data manipulation.
     *
     * @return true, if is supports schemas in data manipulation
     */
    public boolean isSupportsSchemasInDataManipulation() {
        return supportsSchemasInDataManipulation;
    }

    /**
     * Sets the supports schemas in data manipulation.
     *
     * @param supportsSchemasInDataManipulation the new supports schemas in data manipulation
     */
    public void setSupportsSchemasInDataManipulation(boolean supportsSchemasInDataManipulation) {
        this.supportsSchemasInDataManipulation = supportsSchemasInDataManipulation;
    }

    /**
     * Checks if is supports schemas in procedure calls.
     *
     * @return true, if is supports schemas in procedure calls
     */
    public boolean isSupportsSchemasInProcedureCalls() {
        return supportsSchemasInProcedureCalls;
    }

    /**
     * Sets the supports schemas in procedure calls.
     *
     * @param supportsSchemasInProcedureCalls the new supports schemas in procedure calls
     */
    public void setSupportsSchemasInProcedureCalls(boolean supportsSchemasInProcedureCalls) {
        this.supportsSchemasInProcedureCalls = supportsSchemasInProcedureCalls;
    }

    /**
     * Checks if is supports schemas in table definitions.
     *
     * @return true, if is supports schemas in table definitions
     */
    public boolean isSupportsSchemasInTableDefinitions() {
        return supportsSchemasInTableDefinitions;
    }

    /**
     * Sets the supports schemas in table definitions.
     *
     * @param supportsSchemasInTableDefinitions the new supports schemas in table definitions
     */
    public void setSupportsSchemasInTableDefinitions(boolean supportsSchemasInTableDefinitions) {
        this.supportsSchemasInTableDefinitions = supportsSchemasInTableDefinitions;
    }

    /**
     * Checks if is supports schemas in index definitions.
     *
     * @return true, if is supports schemas in index definitions
     */
    public boolean isSupportsSchemasInIndexDefinitions() {
        return supportsSchemasInIndexDefinitions;
    }

    /**
     * Sets the supports schemas in index definitions.
     *
     * @param supportsSchemasInIndexDefinitions the new supports schemas in index definitions
     */
    public void setSupportsSchemasInIndexDefinitions(boolean supportsSchemasInIndexDefinitions) {
        this.supportsSchemasInIndexDefinitions = supportsSchemasInIndexDefinitions;
    }

    /**
     * Checks if is supports schemas in privilege definitions.
     *
     * @return true, if is supports schemas in privilege definitions
     */
    public boolean isSupportsSchemasInPrivilegeDefinitions() {
        return supportsSchemasInPrivilegeDefinitions;
    }

    /**
     * Sets the supports schemas in privilege definitions.
     *
     * @param supportsSchemasInPrivilegeDefinitions the new supports schemas in privilege definitions
     */
    public void setSupportsSchemasInPrivilegeDefinitions(boolean supportsSchemasInPrivilegeDefinitions) {
        this.supportsSchemasInPrivilegeDefinitions = supportsSchemasInPrivilegeDefinitions;
    }

    /**
     * Checks if is supports catalogs in data manipulation.
     *
     * @return true, if is supports catalogs in data manipulation
     */
    public boolean isSupportsCatalogsInDataManipulation() {
        return supportsCatalogsInDataManipulation;
    }

    /**
     * Sets the supports catalogs in data manipulation.
     *
     * @param supportsCatalogsInDataManipulation the new supports catalogs in data manipulation
     */
    public void setSupportsCatalogsInDataManipulation(boolean supportsCatalogsInDataManipulation) {
        this.supportsCatalogsInDataManipulation = supportsCatalogsInDataManipulation;
    }

    /**
     * Checks if is supports catalogs in procedure calls.
     *
     * @return true, if is supports catalogs in procedure calls
     */
    public boolean isSupportsCatalogsInProcedureCalls() {
        return supportsCatalogsInProcedureCalls;
    }

    /**
     * Sets the supports catalogs in procedure calls.
     *
     * @param supportsCatalogsInProcedureCalls the new supports catalogs in procedure calls
     */
    public void setSupportsCatalogsInProcedureCalls(boolean supportsCatalogsInProcedureCalls) {
        this.supportsCatalogsInProcedureCalls = supportsCatalogsInProcedureCalls;
    }

    /**
     * Checks if is supports catalogs in table definitions.
     *
     * @return true, if is supports catalogs in table definitions
     */
    public boolean isSupportsCatalogsInTableDefinitions() {
        return supportsCatalogsInTableDefinitions;
    }

    /**
     * Sets the supports catalogs in table definitions.
     *
     * @param supportsCatalogsInTableDefinitions the new supports catalogs in table definitions
     */
    public void setSupportsCatalogsInTableDefinitions(boolean supportsCatalogsInTableDefinitions) {
        this.supportsCatalogsInTableDefinitions = supportsCatalogsInTableDefinitions;
    }

    /**
     * Checks if is supports catalogs in index definitions.
     *
     * @return true, if is supports catalogs in index definitions
     */
    public boolean isSupportsCatalogsInIndexDefinitions() {
        return supportsCatalogsInIndexDefinitions;
    }

    /**
     * Sets the supports catalogs in index definitions.
     *
     * @param supportsCatalogsInIndexDefinitions the new supports catalogs in index definitions
     */
    public void setSupportsCatalogsInIndexDefinitions(boolean supportsCatalogsInIndexDefinitions) {
        this.supportsCatalogsInIndexDefinitions = supportsCatalogsInIndexDefinitions;
    }

    /**
     * Checks if is supports catalogs in privilege definitions.
     *
     * @return true, if is supports catalogs in privilege definitions
     */
    public boolean isSupportsCatalogsInPrivilegeDefinitions() {
        return supportsCatalogsInPrivilegeDefinitions;
    }

    /**
     * Sets the supports catalogs in privilege definitions.
     *
     * @param supportsCatalogsInPrivilegeDefinitions the new supports catalogs in privilege definitions
     */
    public void setSupportsCatalogsInPrivilegeDefinitions(boolean supportsCatalogsInPrivilegeDefinitions) {
        this.supportsCatalogsInPrivilegeDefinitions = supportsCatalogsInPrivilegeDefinitions;
    }

    /**
     * Checks if is supports positioned delete.
     *
     * @return true, if is supports positioned delete
     */
    public boolean isSupportsPositionedDelete() {
        return supportsPositionedDelete;
    }

    /**
     * Sets the supports positioned delete.
     *
     * @param supportsPositionedDelete the new supports positioned delete
     */
    public void setSupportsPositionedDelete(boolean supportsPositionedDelete) {
        this.supportsPositionedDelete = supportsPositionedDelete;
    }

    /**
     * Checks if is supports positioned update.
     *
     * @return true, if is supports positioned update
     */
    public boolean isSupportsPositionedUpdate() {
        return supportsPositionedUpdate;
    }

    /**
     * Sets the supports positioned update.
     *
     * @param supportsPositionedUpdate the new supports positioned update
     */
    public void setSupportsPositionedUpdate(boolean supportsPositionedUpdate) {
        this.supportsPositionedUpdate = supportsPositionedUpdate;
    }

    /**
     * Checks if is supports select for update.
     *
     * @return true, if is supports select for update
     */
    public boolean isSupportsSelectForUpdate() {
        return supportsSelectForUpdate;
    }

    /**
     * Sets the supports select for update.
     *
     * @param supportsSelectForUpdate the new supports select for update
     */
    public void setSupportsSelectForUpdate(boolean supportsSelectForUpdate) {
        this.supportsSelectForUpdate = supportsSelectForUpdate;
    }

    /**
     * Checks if is supports stored procedures.
     *
     * @return true, if is supports stored procedures
     */
    public boolean isSupportsStoredProcedures() {
        return supportsStoredProcedures;
    }

    /**
     * Sets the supports stored procedures.
     *
     * @param supportsStoredProcedures the new supports stored procedures
     */
    public void setSupportsStoredProcedures(boolean supportsStoredProcedures) {
        this.supportsStoredProcedures = supportsStoredProcedures;
    }

    /**
     * Checks if is supports subqueries in comparisons.
     *
     * @return true, if is supports subqueries in comparisons
     */
    public boolean isSupportsSubqueriesInComparisons() {
        return supportsSubqueriesInComparisons;
    }

    /**
     * Sets the supports subqueries in comparisons.
     *
     * @param supportsSubqueriesInComparisons the new supports subqueries in comparisons
     */
    public void setSupportsSubqueriesInComparisons(boolean supportsSubqueriesInComparisons) {
        this.supportsSubqueriesInComparisons = supportsSubqueriesInComparisons;
    }

    /**
     * Checks if is supports subqueries in exists.
     *
     * @return true, if is supports subqueries in exists
     */
    public boolean isSupportsSubqueriesInExists() {
        return supportsSubqueriesInExists;
    }

    /**
     * Sets the supports subqueries in exists.
     *
     * @param supportsSubqueriesInExists the new supports subqueries in exists
     */
    public void setSupportsSubqueriesInExists(boolean supportsSubqueriesInExists) {
        this.supportsSubqueriesInExists = supportsSubqueriesInExists;
    }

    /**
     * Checks if is supports subqueries in ins.
     *
     * @return true, if is supports subqueries in ins
     */
    public boolean isSupportsSubqueriesInIns() {
        return supportsSubqueriesInIns;
    }

    /**
     * Sets the supports subqueries in ins.
     *
     * @param supportsSubqueriesInIns the new supports subqueries in ins
     */
    public void setSupportsSubqueriesInIns(boolean supportsSubqueriesInIns) {
        this.supportsSubqueriesInIns = supportsSubqueriesInIns;
    }

    /**
     * Checks if is supports subqueries in quantifieds.
     *
     * @return true, if is supports subqueries in quantifieds
     */
    public boolean isSupportsSubqueriesInQuantifieds() {
        return supportsSubqueriesInQuantifieds;
    }

    /**
     * Sets the supports subqueries in quantifieds.
     *
     * @param supportsSubqueriesInQuantifieds the new supports subqueries in quantifieds
     */
    public void setSupportsSubqueriesInQuantifieds(boolean supportsSubqueriesInQuantifieds) {
        this.supportsSubqueriesInQuantifieds = supportsSubqueriesInQuantifieds;
    }

    /**
     * Checks if is supports correlated subqueries.
     *
     * @return true, if is supports correlated subqueries
     */
    public boolean isSupportsCorrelatedSubqueries() {
        return supportsCorrelatedSubqueries;
    }

    /**
     * Sets the supports correlated subqueries.
     *
     * @param supportsCorrelatedSubqueries the new supports correlated subqueries
     */
    public void setSupportsCorrelatedSubqueries(boolean supportsCorrelatedSubqueries) {
        this.supportsCorrelatedSubqueries = supportsCorrelatedSubqueries;
    }

    /**
     * Checks if is supports union.
     *
     * @return true, if is supports union
     */
    public boolean isSupportsUnion() {
        return supportsUnion;
    }

    /**
     * Sets the supports union.
     *
     * @param supportsUnion the new supports union
     */
    public void setSupportsUnion(boolean supportsUnion) {
        this.supportsUnion = supportsUnion;
    }

    /**
     * Checks if is supports union all.
     *
     * @return true, if is supports union all
     */
    public boolean isSupportsUnionAll() {
        return supportsUnionAll;
    }

    /**
     * Sets the supports union all.
     *
     * @param supportsUnionAll the new supports union all
     */
    public void setSupportsUnionAll(boolean supportsUnionAll) {
        this.supportsUnionAll = supportsUnionAll;
    }

    /**
     * Checks if is supports open cursors across commit.
     *
     * @return true, if is supports open cursors across commit
     */
    public boolean isSupportsOpenCursorsAcrossCommit() {
        return supportsOpenCursorsAcrossCommit;
    }

    /**
     * Sets the supports open cursors across commit.
     *
     * @param supportsOpenCursorsAcrossCommit the new supports open cursors across commit
     */
    public void setSupportsOpenCursorsAcrossCommit(boolean supportsOpenCursorsAcrossCommit) {
        this.supportsOpenCursorsAcrossCommit = supportsOpenCursorsAcrossCommit;
    }

    /**
     * Checks if is supports open cursors across rollback.
     *
     * @return true, if is supports open cursors across rollback
     */
    public boolean isSupportsOpenCursorsAcrossRollback() {
        return supportsOpenCursorsAcrossRollback;
    }

    /**
     * Sets the supports open cursors across rollback.
     *
     * @param supportsOpenCursorsAcrossRollback the new supports open cursors across rollback
     */
    public void setSupportsOpenCursorsAcrossRollback(boolean supportsOpenCursorsAcrossRollback) {
        this.supportsOpenCursorsAcrossRollback = supportsOpenCursorsAcrossRollback;
    }

    /**
     * Checks if is supports open statements across commit.
     *
     * @return true, if is supports open statements across commit
     */
    public boolean isSupportsOpenStatementsAcrossCommit() {
        return supportsOpenStatementsAcrossCommit;
    }

    /**
     * Sets the supports open statements across commit.
     *
     * @param supportsOpenStatementsAcrossCommit the new supports open statements across commit
     */
    public void setSupportsOpenStatementsAcrossCommit(boolean supportsOpenStatementsAcrossCommit) {
        this.supportsOpenStatementsAcrossCommit = supportsOpenStatementsAcrossCommit;
    }

    /**
     * Checks if is supports open statements across rollback.
     *
     * @return true, if is supports open statements across rollback
     */
    public boolean isSupportsOpenStatementsAcrossRollback() {
        return supportsOpenStatementsAcrossRollback;
    }

    /**
     * Sets the supports open statements across rollback.
     *
     * @param supportsOpenStatementsAcrossRollback the new supports open statements across rollback
     */
    public void setSupportsOpenStatementsAcrossRollback(boolean supportsOpenStatementsAcrossRollback) {
        this.supportsOpenStatementsAcrossRollback = supportsOpenStatementsAcrossRollback;
    }

    /**
     * Gets the max binary literal length.
     *
     * @return the max binary literal length
     */
    public int getMaxBinaryLiteralLength() {
        return maxBinaryLiteralLength;
    }

    /**
     * Sets the max binary literal length.
     *
     * @param maxBinaryLiteralLength the new max binary literal length
     */
    public void setMaxBinaryLiteralLength(int maxBinaryLiteralLength) {
        this.maxBinaryLiteralLength = maxBinaryLiteralLength;
    }

    /**
     * Gets the max char literal length.
     *
     * @return the max char literal length
     */
    public int getMaxCharLiteralLength() {
        return maxCharLiteralLength;
    }

    /**
     * Sets the max char literal length.
     *
     * @param maxCharLiteralLength the new max char literal length
     */
    public void setMaxCharLiteralLength(int maxCharLiteralLength) {
        this.maxCharLiteralLength = maxCharLiteralLength;
    }

    /**
     * Gets the max column name length.
     *
     * @return the max column name length
     */
    public int getMaxColumnNameLength() {
        return maxColumnNameLength;
    }

    /**
     * Sets the max column name length.
     *
     * @param maxColumnNameLength the new max column name length
     */
    public void setMaxColumnNameLength(int maxColumnNameLength) {
        this.maxColumnNameLength = maxColumnNameLength;
    }

    /**
     * Gets the max columns in group by.
     *
     * @return the max columns in group by
     */
    public int getMaxColumnsInGroupBy() {
        return maxColumnsInGroupBy;
    }

    /**
     * Sets the max columns in group by.
     *
     * @param maxColumnsInGroupBy the new max columns in group by
     */
    public void setMaxColumnsInGroupBy(int maxColumnsInGroupBy) {
        this.maxColumnsInGroupBy = maxColumnsInGroupBy;
    }

    /**
     * Gets the max columns in index.
     *
     * @return the max columns in index
     */
    public int getMaxColumnsInIndex() {
        return maxColumnsInIndex;
    }

    /**
     * Sets the max columns in index.
     *
     * @param maxColumnsInIndex the new max columns in index
     */
    public void setMaxColumnsInIndex(int maxColumnsInIndex) {
        this.maxColumnsInIndex = maxColumnsInIndex;
    }

    /**
     * Gets the max columns in order by.
     *
     * @return the max columns in order by
     */
    public int getMaxColumnsInOrderBy() {
        return maxColumnsInOrderBy;
    }

    /**
     * Sets the max columns in order by.
     *
     * @param maxColumnsInOrderBy the new max columns in order by
     */
    public void setMaxColumnsInOrderBy(int maxColumnsInOrderBy) {
        this.maxColumnsInOrderBy = maxColumnsInOrderBy;
    }

    /**
     * Gets the max columns in select.
     *
     * @return the max columns in select
     */
    public int getMaxColumnsInSelect() {
        return maxColumnsInSelect;
    }

    /**
     * Sets the max columns in select.
     *
     * @param maxColumnsInSelect the new max columns in select
     */
    public void setMaxColumnsInSelect(int maxColumnsInSelect) {
        this.maxColumnsInSelect = maxColumnsInSelect;
    }

    /**
     * Gets the max columns in table.
     *
     * @return the max columns in table
     */
    public int getMaxColumnsInTable() {
        return maxColumnsInTable;
    }

    /**
     * Sets the max columns in table.
     *
     * @param maxColumnsInTable the new max columns in table
     */
    public void setMaxColumnsInTable(int maxColumnsInTable) {
        this.maxColumnsInTable = maxColumnsInTable;
    }

    /**
     * Gets the max connections.
     *
     * @return the max connections
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Sets the max connections.
     *
     * @param maxConnections the new max connections
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Gets the max cursor name length.
     *
     * @return the max cursor name length
     */
    public int getMaxCursorNameLength() {
        return maxCursorNameLength;
    }

    /**
     * Sets the max cursor name length.
     *
     * @param maxCursorNameLength the new max cursor name length
     */
    public void setMaxCursorNameLength(int maxCursorNameLength) {
        this.maxCursorNameLength = maxCursorNameLength;
    }

    /**
     * Gets the max index length.
     *
     * @return the max index length
     */
    public int getMaxIndexLength() {
        return maxIndexLength;
    }

    /**
     * Sets the max index length.
     *
     * @param maxIndexLength the new max index length
     */
    public void setMaxIndexLength(int maxIndexLength) {
        this.maxIndexLength = maxIndexLength;
    }

    /**
     * Gets the max schema name length.
     *
     * @return the max schema name length
     */
    public int getMaxSchemaNameLength() {
        return maxSchemaNameLength;
    }

    /**
     * Sets the max schema name length.
     *
     * @param maxSchemaNameLength the new max schema name length
     */
    public void setMaxSchemaNameLength(int maxSchemaNameLength) {
        this.maxSchemaNameLength = maxSchemaNameLength;
    }

    /**
     * Gets the max procedure name length.
     *
     * @return the max procedure name length
     */
    public int getMaxProcedureNameLength() {
        return maxProcedureNameLength;
    }

    /**
     * Sets the max procedure name length.
     *
     * @param maxProcedureNameLength the new max procedure name length
     */
    public void setMaxProcedureNameLength(int maxProcedureNameLength) {
        this.maxProcedureNameLength = maxProcedureNameLength;
    }

    /**
     * Gets the max catalog name length.
     *
     * @return the max catalog name length
     */
    public int getMaxCatalogNameLength() {
        return maxCatalogNameLength;
    }

    /**
     * Sets the max catalog name length.
     *
     * @param maxCatalogNameLength the new max catalog name length
     */
    public void setMaxCatalogNameLength(int maxCatalogNameLength) {
        this.maxCatalogNameLength = maxCatalogNameLength;
    }

    /**
     * Gets the max row size.
     *
     * @return the max row size
     */
    public int getMaxRowSize() {
        return maxRowSize;
    }

    /**
     * Sets the max row size.
     *
     * @param maxRowSize the new max row size
     */
    public void setMaxRowSize(int maxRowSize) {
        this.maxRowSize = maxRowSize;
    }

    /**
     * Checks if is max row size include blobs.
     *
     * @return true, if is max row size include blobs
     */
    public boolean isMaxRowSizeIncludeBlobs() {
        return maxRowSizeIncludeBlobs;
    }

    /**
     * Sets the max row size include blobs.
     *
     * @param maxRowSizeIncludeBlobs the new max row size include blobs
     */
    public void setMaxRowSizeIncludeBlobs(boolean maxRowSizeIncludeBlobs) {
        this.maxRowSizeIncludeBlobs = maxRowSizeIncludeBlobs;
    }

    /**
     * Gets the max statement length.
     *
     * @return the max statement length
     */
    public int getMaxStatementLength() {
        return maxStatementLength;
    }

    /**
     * Sets the max statement length.
     *
     * @param maxStatementLength the new max statement length
     */
    public void setMaxStatementLength(int maxStatementLength) {
        this.maxStatementLength = maxStatementLength;
    }

    /**
     * Gets the max statements.
     *
     * @return the max statements
     */
    public int getMaxStatements() {
        return maxStatements;
    }

    /**
     * Sets the max statements.
     *
     * @param maxStatements the new max statements
     */
    public void setMaxStatements(int maxStatements) {
        this.maxStatements = maxStatements;
    }

    /**
     * Gets the max table name length.
     *
     * @return the max table name length
     */
    public int getMaxTableNameLength() {
        return maxTableNameLength;
    }

    /**
     * Sets the max table name length.
     *
     * @param maxTableNameLength the new max table name length
     */
    public void setMaxTableNameLength(int maxTableNameLength) {
        this.maxTableNameLength = maxTableNameLength;
    }

    /**
     * Gets the max tables in select.
     *
     * @return the max tables in select
     */
    public int getMaxTablesInSelect() {
        return maxTablesInSelect;
    }

    /**
     * Sets the max tables in select.
     *
     * @param maxTablesInSelect the new max tables in select
     */
    public void setMaxTablesInSelect(int maxTablesInSelect) {
        this.maxTablesInSelect = maxTablesInSelect;
    }

    /**
     * Gets the max user name length.
     *
     * @return the max user name length
     */
    public int getMaxUserNameLength() {
        return maxUserNameLength;
    }

    /**
     * Sets the max user name length.
     *
     * @param maxUserNameLength the new max user name length
     */
    public void setMaxUserNameLength(int maxUserNameLength) {
        this.maxUserNameLength = maxUserNameLength;
    }

    /**
     * Gets the default transaction isolation.
     *
     * @return the default transaction isolation
     */
    public int getDefaultTransactionIsolation() {
        return defaultTransactionIsolation;
    }

    /**
     * Sets the default transaction isolation.
     *
     * @param defaultTransactionIsolation the new default transaction isolation
     */
    public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
        this.defaultTransactionIsolation = defaultTransactionIsolation;
    }

    /**
     * Checks if is supports transactions.
     *
     * @return true, if is supports transactions
     */
    public boolean isSupportsTransactions() {
        return supportsTransactions;
    }

    /**
     * Sets the supports transactions.
     *
     * @param supportsTransactions the new supports transactions
     */
    public void setSupportsTransactions(boolean supportsTransactions) {
        this.supportsTransactions = supportsTransactions;
    }

    /**
     * Checks if is supports data definition and data manipulation transactions.
     *
     * @return true, if is supports data definition and data manipulation transactions
     */
    public boolean isSupportsDataDefinitionAndDataManipulationTransactions() {
        return supportsDataDefinitionAndDataManipulationTransactions;
    }

    /**
     * Sets the supports data definition and data manipulation transactions.
     *
     * @param supportsDataDefinitionAndDataManipulationTransactions the new supports data definition and
     *        data manipulation transactions
     */
    public void setSupportsDataDefinitionAndDataManipulationTransactions(boolean supportsDataDefinitionAndDataManipulationTransactions) {
        this.supportsDataDefinitionAndDataManipulationTransactions = supportsDataDefinitionAndDataManipulationTransactions;
    }

    /**
     * Checks if is supports data manipulation transactions only.
     *
     * @return true, if is supports data manipulation transactions only
     */
    public boolean isSupportsDataManipulationTransactionsOnly() {
        return supportsDataManipulationTransactionsOnly;
    }

    /**
     * Sets the supports data manipulation transactions only.
     *
     * @param supportsDataManipulationTransactionsOnly the new supports data manipulation transactions
     *        only
     */
    public void setSupportsDataManipulationTransactionsOnly(boolean supportsDataManipulationTransactionsOnly) {
        this.supportsDataManipulationTransactionsOnly = supportsDataManipulationTransactionsOnly;
    }

    /**
     * Checks if is data definition causes transaction commit.
     *
     * @return true, if is data definition causes transaction commit
     */
    public boolean isDataDefinitionCausesTransactionCommit() {
        return dataDefinitionCausesTransactionCommit;
    }

    /**
     * Sets the data definition causes transaction commit.
     *
     * @param dataDefinitionCausesTransactionCommit the new data definition causes transaction commit
     */
    public void setDataDefinitionCausesTransactionCommit(boolean dataDefinitionCausesTransactionCommit) {
        this.dataDefinitionCausesTransactionCommit = dataDefinitionCausesTransactionCommit;
    }

    /**
     * Checks if is data definition ignored in transactions.
     *
     * @return true, if is data definition ignored in transactions
     */
    public boolean isDataDefinitionIgnoredInTransactions() {
        return dataDefinitionIgnoredInTransactions;
    }

    /**
     * Sets the data definition ignored in transactions.
     *
     * @param dataDefinitionIgnoredInTransactions the new data definition ignored in transactions
     */
    public void setDataDefinitionIgnoredInTransactions(boolean dataDefinitionIgnoredInTransactions) {
        this.dataDefinitionIgnoredInTransactions = dataDefinitionIgnoredInTransactions;
    }

    /**
     * Checks if is supports batch updates.
     *
     * @return true, if is supports batch updates
     */
    public boolean isSupportsBatchUpdates() {
        return supportsBatchUpdates;
    }

    /**
     * Sets the supports batch updates.
     *
     * @param supportsBatchUpdates the new supports batch updates
     */
    public void setSupportsBatchUpdates(boolean supportsBatchUpdates) {
        this.supportsBatchUpdates = supportsBatchUpdates;
    }

    /**
     * Checks if is supports savepoints.
     *
     * @return true, if is supports savepoints
     */
    public boolean isSupportsSavepoints() {
        return supportsSavepoints;
    }

    /**
     * Sets the supports savepoints.
     *
     * @param supportsSavepoints the new supports savepoints
     */
    public void setSupportsSavepoints(boolean supportsSavepoints) {
        this.supportsSavepoints = supportsSavepoints;
    }

    /**
     * Checks if is supports named parameters.
     *
     * @return true, if is supports named parameters
     */
    public boolean isSupportsNamedParameters() {
        return supportsNamedParameters;
    }

    /**
     * Sets the supports named parameters.
     *
     * @param supportsNamedParameters the new supports named parameters
     */
    public void setSupportsNamedParameters(boolean supportsNamedParameters) {
        this.supportsNamedParameters = supportsNamedParameters;
    }

    /**
     * Checks if is supports multiple open results.
     *
     * @return true, if is supports multiple open results
     */
    public boolean isSupportsMultipleOpenResults() {
        return supportsMultipleOpenResults;
    }

    /**
     * Sets the supports multiple open results.
     *
     * @param supportsMultipleOpenResults the new supports multiple open results
     */
    public void setSupportsMultipleOpenResults(boolean supportsMultipleOpenResults) {
        this.supportsMultipleOpenResults = supportsMultipleOpenResults;
    }

    /**
     * Checks if is supports get generated keys.
     *
     * @return true, if is supports get generated keys
     */
    public boolean isSupportsGetGeneratedKeys() {
        return supportsGetGeneratedKeys;
    }

    /**
     * Sets the supports get generated keys.
     *
     * @param supportsGetGeneratedKeys the new supports get generated keys
     */
    public void setSupportsGetGeneratedKeys(boolean supportsGetGeneratedKeys) {
        this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
    }

    /**
     * Gets the result set holdability.
     *
     * @return the result set holdability
     */
    public int getResultSetHoldability() {
        return resultSetHoldability;
    }

    /**
     * Sets the result set holdability.
     *
     * @param resultSetHoldability the new result set holdability
     */
    public void setGetResultSetHoldability(int resultSetHoldability) {
        this.resultSetHoldability = resultSetHoldability;
    }

    /**
     * Gets the database major version.
     *
     * @return the database major version
     */
    public int getDatabaseMajorVersion() {
        return databaseMajorVersion;
    }

    /**
     * Sets the database major version.
     *
     * @param databaseMajorVersion the new database major version
     */
    public void setGetDatabaseMajorVersion(int databaseMajorVersion) {
        this.databaseMajorVersion = databaseMajorVersion;
    }

    /**
     * Gets the database minor version.
     *
     * @return the database minor version
     */
    public int getDatabaseMinorVersion() {
        return databaseMinorVersion;
    }

    /**
     * Sets the database minor version.
     *
     * @param databaseMinorVersion the new database minor version
     */
    public void setGetDatabaseMinorVersion(int databaseMinorVersion) {
        this.databaseMinorVersion = databaseMinorVersion;
    }

    /**
     * Gets the JDBC major version.
     *
     * @return the JDBC major version
     */
    public int getJDBCMajorVersion() {
        return jdbcMajorVersion;
    }

    /**
     * Sets the JDBC major version.
     *
     * @param jdbcMajorVersion the new JDBC major version
     */
    public void setJDBCMajorVersion(int jdbcMajorVersion) {
        this.jdbcMajorVersion = jdbcMajorVersion;
    }

    /**
     * Gets the JDBC minor version.
     *
     * @return the JDBC minor version
     */
    public int getJDBCMinorVersion() {
        return jdbcMinorVersion;
    }

    /**
     * Sets the JDBC minor version.
     *
     * @param jdbcMinorVersion the new JDBC minor version
     */
    public void setJDBCMinorVersion(int jdbcMinorVersion) {
        this.jdbcMinorVersion = jdbcMinorVersion;
    }

    /**
     * Gets the SQL state type.
     *
     * @return the SQL state type
     */
    public int getSQLStateType() {
        return sqlStateType;
    }

    /**
     * Sets the SQL state type.
     *
     * @param sqlStateType the new SQL state type
     */
    public void setSQLStateType(int sqlStateType) {
        this.sqlStateType = sqlStateType;
    }

    /**
     * Checks if is locators update copy.
     *
     * @return true, if is locators update copy
     */
    public boolean isLocatorsUpdateCopy() {
        return locatorsUpdateCopy;
    }

    /**
     * Sets the locators update copy.
     *
     * @param locatorsUpdateCopy the new locators update copy
     */
    public void setLocatorsUpdateCopy(boolean locatorsUpdateCopy) {
        this.locatorsUpdateCopy = locatorsUpdateCopy;
    }

    /**
     * Checks if is supports statement pooling.
     *
     * @return true, if is supports statement pooling
     */
    public boolean isSupportsStatementPooling() {
        return supportsStatementPooling;
    }

    /**
     * Sets the supports statement pooling.
     *
     * @param supportsStatementPooling the new supports statement pooling
     */
    public void setSupportsStatementPooling(boolean supportsStatementPooling) {
        this.supportsStatementPooling = supportsStatementPooling;
    }

    /**
     * Checks if is supports stored functions using call syntax.
     *
     * @return true, if is supports stored functions using call syntax
     */
    public boolean isSupportsStoredFunctionsUsingCallSyntax() {
        return supportsStoredFunctionsUsingCallSyntax;
    }

    /**
     * Sets the supports stored functions using call syntax.
     *
     * @param supportsStoredFunctionsUsingCallSyntax the new supports stored functions using call syntax
     */
    public void setSupportsStoredFunctionsUsingCallSyntax(boolean supportsStoredFunctionsUsingCallSyntax) {
        this.supportsStoredFunctionsUsingCallSyntax = supportsStoredFunctionsUsingCallSyntax;
    }

    /**
     * Checks if is auto commit failure closes all result sets.
     *
     * @return true, if is auto commit failure closes all result sets
     */
    public boolean isAutoCommitFailureClosesAllResultSets() {
        return autoCommitFailureClosesAllResultSets;
    }

    /**
     * Sets the auto commit failure closes all result sets.
     *
     * @param autoCommitFailureClosesAllResultSets the new auto commit failure closes all result sets
     */
    public void setAutoCommitFailureClosesAllResultSets(boolean autoCommitFailureClosesAllResultSets) {
        this.autoCommitFailureClosesAllResultSets = autoCommitFailureClosesAllResultSets;
    }

    /**
     * Checks if is generated key always returned.
     *
     * @return true, if is generated key always returned
     */
    public boolean isGeneratedKeyAlwaysReturned() {
        return generatedKeyAlwaysReturned;
    }

    /**
     * Sets the generated key always returned.
     *
     * @param generatedKeyAlwaysReturned the new generated key always returned
     */
    public void setGeneratedKeyAlwaysReturned(boolean generatedKeyAlwaysReturned) {
        this.generatedKeyAlwaysReturned = generatedKeyAlwaysReturned;
    }

    /**
     * Gets the max logical lob size.
     *
     * @return the max logical lob size
     */
    public long getMaxLogicalLobSize() {
        return maxLogicalLobSize;
    }

    /**
     * Sets the max logical lob size.
     *
     * @param maxLogicalLobSize the new max logical lob size
     */
    public void setMaxLogicalLobSize(long maxLogicalLobSize) {
        this.maxLogicalLobSize = maxLogicalLobSize;
    }

    /**
     * Checks if is supports ref cursors.
     *
     * @return true, if is supports ref cursors
     */
    public boolean isSupportsRefCursors() {
        return supportsRefCursors;
    }

    /**
     * Sets the supports ref cursors.
     *
     * @param supportsRefCursors the new supports ref cursors
     */
    public void setSupportsRefCursors(boolean supportsRefCursors) {
        this.supportsRefCursors = supportsRefCursors;
    }

    /**
     * Gets the schemas.
     *
     * @return the schemas
     */
    public List<SchemaMetadata> getSchemas() {
        return schemas;
    }

    // public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
    // return databaseMetaData.supportsTransactionIsolationLevel(level);
    // }
    //
    // public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
    // throws SQLException {
    // return databaseMetaData.getProcedures(catalog, schemaPattern, procedureNamePattern);
    // }
    //
    // public ResultSet getProcedureColumns(String catalog, String schemaPattern, String
    // procedureNamePattern,
    // String columnNamePattern) throws SQLException {
    // return databaseMetaData.getProcedureColumns(catalog, schemaPattern, procedureNamePattern,
    // columnNamePattern);
    // }
    //
    // public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern,
    // String[] types)
    // throws SQLException {
    // return databaseMetaData.getTables(catalog, schemaPattern, tableNamePattern, types);
    // }
    //
    // public ResultSet getSchemas() throws SQLException {
    // return databaseMetaData.getSchemas();
    // }
    //
    // public ResultSet getCatalogs() throws SQLException {
    // return databaseMetaData.getCatalogs();
    // }
    //
    // public ResultSet getTableTypes() throws SQLException {
    // return databaseMetaData.getTableTypes();
    // }

    // public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String
    // columnNamePattern)
    // throws SQLException {
    // return databaseMetaData.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
    // }
    //
    // public ResultSet getColumnPrivileges(String catalog, String schema, String table, String
    // columnNamePattern)
    // throws SQLException {
    // return databaseMetaData.getColumnPrivileges(catalog, schema, table, columnNamePattern);
    // }
    //
    // public ResultSet getTablePrivileges(String catalog, String schemaPattern, String
    // tableNamePattern)
    // throws SQLException {
    // return databaseMetaData.getTablePrivileges(catalog, schemaPattern, tableNamePattern);
    // }
    //
    // public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope,
    // boolean nullable)
    // throws SQLException {
    // return databaseMetaData.getBestRowIdentifier(catalog, schema, table, scope, nullable);
    // }
    //
    // public ResultSet getVersionColumns(String catalog, String schema, String table) throws
    // SQLException {
    // return databaseMetaData.getVersionColumns(catalog, schema, table);
    // }
    //
    // public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException
    // {
    // return databaseMetaData.getPrimaryKeys(catalog, schema, table);
    // }
    //
    // public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException
    // {
    // return databaseMetaData.getImportedKeys(catalog, schema, table);
    // }
    //
    // public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException
    // {
    // return databaseMetaData.getExportedKeys(catalog, schema, table);
    // }
    //
    // public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable,
    // String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
    // return databaseMetaData.getCrossReference(parentCatalog, parentSchema, parentTable,
    // foreignCatalog,
    // foreignSchema, foreignTable);
    // }
    //
    // public ResultSet getTypeInfo() throws SQLException {
    // return databaseMetaData.getTypeInfo();
    // }
    //
    // public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique,
    // boolean approximate)
    // throws SQLException {
    // return databaseMetaData.getIndexInfo(catalog, schema, table, unique, approximate);
    // }
    //
    // public boolean supportsResultSetType(int type) throws SQLException {
    // return databaseMetaData.supportsResultSetType(type);
    // }
    //
    // public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
    // return databaseMetaData.supportsResultSetConcurrency(type, concurrency);
    // }
    //
    // public boolean ownUpdatesAreVisible(int type) throws SQLException {
    // return databaseMetaData.ownUpdatesAreVisible(type);
    // }
    //
    // public boolean ownDeletesAreVisible(int type) throws SQLException {
    // return databaseMetaData.ownDeletesAreVisible(type);
    // }
    //
    // public boolean ownInsertsAreVisible(int type) throws SQLException {
    // return databaseMetaData.ownInsertsAreVisible(type);
    // }
    //
    // public boolean othersUpdatesAreVisible(int type) throws SQLException {
    // return databaseMetaData.othersUpdatesAreVisible(type);
    // }
    //
    // public boolean othersDeletesAreVisible(int type) throws SQLException {
    // return databaseMetaData.othersDeletesAreVisible(type);
    // }
    //
    // public boolean othersInsertsAreVisible(int type) throws SQLException {
    // return databaseMetaData.othersInsertsAreVisible(type);
    // }
    //
    // public boolean updatesAreDetected(int type) throws SQLException {
    // return databaseMetaData.updatesAreDetected(type);
    // }
    //
    // public boolean deletesAreDetected(int type) throws SQLException {
    // return databaseMetaData.deletesAreDetected(type);
    // }
    //
    // public boolean insertsAreDetected(int type) throws SQLException {
    // return databaseMetaData.insertsAreDetected(type);
    // }
    //
    // public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[]
    // types)
    // throws SQLException {
    // return databaseMetaData.getUDTs(catalog, schemaPattern, typeNamePattern, types);
    // }
    //
    // public Connection getConnection() throws SQLException {
    // return databaseMetaData.getConnection();
    // }
    //
    // public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern)
    // throws SQLException
    // {
    // return databaseMetaData.getSuperTypes(catalog, schemaPattern, typeNamePattern);
    // }
    //
    // public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern)
    // throws
    // SQLException {
    // return databaseMetaData.getSuperTables(catalog, schemaPattern, tableNamePattern);
    // }
    //
    // public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern,
    // String attributeNamePattern) throws SQLException {
    // return databaseMetaData.getAttributes(catalog, schemaPattern, typeNamePattern,
    // attributeNamePattern);
    // }
    //
    // public boolean supportsResultSetHoldability(int holdability) throws SQLException {
    // return databaseMetaData.supportsResultSetHoldability(holdability);
    // }
    //
    // public RowIdLifetime getRowIdLifetime() throws SQLException {
    // return databaseMetaData.getRowIdLifetime();
    // }
    //
    // public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
    // return databaseMetaData.getSchemas(catalog, schemaPattern);
    // }
    //
    // public ResultSet getClientInfoProperties() throws SQLException {
    // return databaseMetaData.getClientInfoProperties();
    // }
    //
    // public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
    // throws SQLException {
    // return databaseMetaData.getFunctions(catalog, schemaPattern, functionNamePattern);
    // }
    //
    // public ResultSet getFunctionColumns(String catalog, String schemaPattern, String
    // functionNamePattern,
    // String columnNamePattern) throws SQLException {
    // return databaseMetaData.getFunctionColumns(catalog, schemaPattern, functionNamePattern,
    // columnNamePattern);
    // }
    //
    // public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern,
    // String columnNamePattern) throws SQLException {
    // return databaseMetaData.getPseudoColumns(catalog, schemaPattern, tableNamePattern,
    // columnNamePattern);
    // }

    /**
     * Gets the kind.
     *
     * @return the kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the kind.
     *
     * @param kind the new kind
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

}
