/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.mongodb.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map.Entry;

import org.bson.Document;
import org.eclipse.dirigible.mongodb.jdbc.util.JsonArrayMongoIteratorResultSet;
import org.eclipse.dirigible.mongodb.jdbc.util.SingleColumnStaticResultSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoIterable;

/**
 * The Class MongoDBDatabaseMetadata.
 */
public class MongoDBDatabaseMetadata implements DatabaseMetaData {

    /** The db product name. */
    private String dbProductName;

    /** The db product version. */
    private String dbProductVersion;

    /** The url. */
    private String url;

    /** The is read only. */
    private boolean isReadOnly;

    /** The driver version. */
    private String driverVersion;

    /** The driver major version. */
    private int driverMajorVersion;

    /** The driver minor version. */
    private int driverMinorVersion;

    /** The driver name. */
    private String driverName;

    /** The connection. */
    private MongoDBConnection connection;

    /** The mapper. */
    private static ObjectMapper MAPPER = new ObjectMapper();

    /** The Constant COLUMN_NAME. */
    public static final String COLUMN_NAME = "COLUMN_NAME"; //$NON-NLS-1$

    /** The Constant TYPE_NAME. */
    public static final String TYPE_NAME = "TYPE_NAME"; //$NON-NLS-1$

    /** The Constant COLUMN_SIZE. */
    public static final String COLUMN_SIZE = "COLUMN_SIZE"; //$NON-NLS-1$

    /** The Constant IS_NULLABLE. */
    public static final String IS_NULLABLE = "IS_NULLABLE"; //$NON-NLS-1$

    /** The Constant DECIMAL_DIGITS. */
    public static final String DECIMAL_DIGITS = "DECIMAL_DIGITS"; //$NON-NLS-1$

    /** The Constant TABLE_NAME. */
    public static final String TABLE_NAME = "TABLE_NAME"; //$NON-NLS-1$

    /** The Constant TABLE_TYPE. */
    public static final String TABLE_TYPE = "TABLE_TYPE"; //$NON-NLS-1$

    /** The Constant REMARKS. */
    public static final String REMARKS = "REMARKS"; //$NON-NLS-1$

    /** The Constant NESTED. */
    public static final String NESTED = "NESTED"; //$NON-NLS-1$

    /** The Constant PK. */
    public static final String PK = "PK"; //$NON-NLS-1$

    /**
     * Instantiates a new mongo DB database metadata.
     *
     * @param connection the connection
     */
    public MongoDBDatabaseMetadata(MongoDBConnection connection) {
        this.connection = connection;
    }

    /**
     * Unwrap.
     *
     * @param <T> the generic type
     * @param iface the iface
     * @return the t
     * @throws SQLException the SQL exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) {
            return (T) this;
        }
        throw new SQLException("No wrapper for " + iface);
    }

    /**
     * Checks if is wrapper for.
     *
     * @param iface the iface
     * @return true, if is wrapper for
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface != null && iface.isAssignableFrom(getClass());
    }

    /**
     * All procedures are callable.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean allProceduresAreCallable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * All tables are selectable.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the url.
     *
     * @return the url
     * @throws SQLException the SQL exception
     */
    @Override
    public String getURL() throws SQLException {
        return this.url;
    }

    /**
     * Sets the url.
     *
     * @param url the new url
     * @throws SQLException the SQL exception
     */
    void setURL(String url) throws SQLException {
        this.url = url;
    }

    /**
     * Gets the user name.
     *
     * @return the user name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getUserName() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Checks if is read only.
     *
     * @return true, if is read only
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isReadOnly() throws SQLException {
        return this.isReadOnly;
    }

    /**
     * Sets the checks if is read only.
     *
     * @param isReadOnly the new checks if is read only
     * @throws SQLException the SQL exception
     */
    void setIsReadOnly(boolean isReadOnly) throws SQLException {
        this.isReadOnly = isReadOnly;
    }

    /**
     * Nulls are sorted high.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean nullsAreSortedHigh() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Nulls are sorted low.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean nullsAreSortedLow() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Nulls are sorted at start.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean nullsAreSortedAtStart() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Nulls are sorted at end.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the database product name.
     *
     * @return the database product name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getDatabaseProductName() throws SQLException {
        return this.dbProductName;
    }

    /**
     * Sets the database product name.
     *
     * @param name the new database product name
     * @throws SQLException the SQL exception
     */
    void setDatabaseProductName(String name) throws SQLException {
        this.dbProductName = name;
    }

    /**
     * Gets the database product version.
     *
     * @return the database product version
     * @throws SQLException the SQL exception
     */
    @Override
    public String getDatabaseProductVersion() throws SQLException {
        return this.dbProductVersion;
    }

    /**
     * Sets the database product version.
     *
     * @param version the new database product version
     * @throws SQLException the SQL exception
     */
    void setDatabaseProductVersion(String version) throws SQLException {
        this.dbProductVersion = version;
    }

    /**
     * Gets the driver name.
     *
     * @return the driver name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getDriverName() throws SQLException {
        return this.driverName;
    }

    /**
     * Sets the driver name.
     *
     * @param driverName the new driver name
     * @throws SQLException the SQL exception
     */
    void setDriverName(String driverName) throws SQLException {
        this.driverName = driverName;
    }

    /**
     * Gets the driver version.
     *
     * @return the driver version
     * @throws SQLException the SQL exception
     */
    @Override
    public String getDriverVersion() throws SQLException {
        return this.driverVersion;
    }

    /**
     * Sets the driver version.
     *
     * @param driverVersion the new driver version
     * @throws SQLException the SQL exception
     */
    void setDriverVersion(String driverVersion) throws SQLException {
        this.driverVersion = driverVersion;
    }

    /**
     * Gets the driver major version.
     *
     * @return the driver major version
     */
    @Override
    public int getDriverMajorVersion() {
        return this.driverMajorVersion;
    }

    /**
     * Sets the driver major version.
     *
     * @param majorVersion the new driver major version
     */
    void setDriverMajorVersion(int majorVersion) {
        this.driverMajorVersion = majorVersion;
    }

    /**
     * Gets the driver minor version.
     *
     * @return the driver minor version
     */
    @Override
    public int getDriverMinorVersion() {
        return this.driverMinorVersion;
    }

    /**
     * Sets the driver minor version.
     *
     * @param minorVersion the new driver minor version
     */
    void setDriverMinorVersion(int minorVersion) {
        this.driverMinorVersion = minorVersion;
    }

    /**
     * Uses local files.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean usesLocalFiles() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Uses local file per table.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean usesLocalFilePerTable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports mixed case identifiers.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Stores upper case identifiers.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Stores lower case identifiers.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Stores mixed case identifiers.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports mixed case quoted identifiers.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Stores upper case quoted identifiers.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Stores lower case quoted identifiers.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Stores mixed case quoted identifiers.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the identifier quote string.
     *
     * @return the identifier quote string
     * @throws SQLException the SQL exception
     */
    @Override
    public String getIdentifierQuoteString() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the SQL keywords.
     *
     * @return the SQL keywords
     * @throws SQLException the SQL exception
     */
    @Override
    public String getSQLKeywords() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the numeric functions.
     *
     * @return the numeric functions
     * @throws SQLException the SQL exception
     */
    @Override
    public String getNumericFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the string functions.
     *
     * @return the string functions
     * @throws SQLException the SQL exception
     */
    @Override
    public String getStringFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the system functions.
     *
     * @return the system functions
     * @throws SQLException the SQL exception
     */
    @Override
    public String getSystemFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the time date functions.
     *
     * @return the time date functions
     * @throws SQLException the SQL exception
     */
    @Override
    public String getTimeDateFunctions() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the search string escape.
     *
     * @return the search string escape
     * @throws SQLException the SQL exception
     */
    @Override
    public String getSearchStringEscape() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the extra name characters.
     *
     * @return the extra name characters
     * @throws SQLException the SQL exception
     */
    @Override
    public String getExtraNameCharacters() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Supports alter table with add column.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports alter table with drop column.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports column aliasing.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsColumnAliasing() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Null plus non null is null.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports convert.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsConvert() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports convert.
     *
     * @param fromType the from type
     * @param toType the to type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsConvert(int fromType, int toType) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports table correlation names.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsTableCorrelationNames() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports different table correlation names.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports expressions in order by.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports order by unrelated.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsOrderByUnrelated() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports group by.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsGroupBy() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports group by unrelated.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsGroupByUnrelated() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports group by beyond select.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports like escape clause.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsLikeEscapeClause() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports multiple result sets.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsMultipleResultSets() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports multiple transactions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsMultipleTransactions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports non nullable columns.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsNonNullableColumns() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports minimum SQL grammar.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports core SQL grammar.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports extended SQL grammar.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports ANSI 92 entry level SQL.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports ANSI 92 intermediate SQL.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports ANSI 92 full SQL.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsANSI92FullSQL() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports integrity enhancement facility.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports outer joins.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsOuterJoins() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports full outer joins.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsFullOuterJoins() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports limited outer joins.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the schema term.
     *
     * @return the schema term
     * @throws SQLException the SQL exception
     */
    @Override
    public String getSchemaTerm() throws SQLException {
        return "collection";
    }

    /**
     * Gets the procedure term.
     *
     * @return the procedure term
     * @throws SQLException the SQL exception
     */
    @Override
    public String getProcedureTerm() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the catalog term.
     *
     * @return the catalog term
     * @throws SQLException the SQL exception
     */
    @Override
    public String getCatalogTerm() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Checks if is catalog at start.
     *
     * @return true, if is catalog at start
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isCatalogAtStart() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the catalog separator.
     *
     * @return the catalog separator
     * @throws SQLException the SQL exception
     */
    @Override
    public String getCatalogSeparator() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Supports schemas in data manipulation.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports schemas in procedure calls.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports schemas in table definitions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports schemas in index definitions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports schemas in privilege definitions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports catalogs in data manipulation.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports catalogs in procedure calls.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports catalogs in table definitions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports catalogs in index definitions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports catalogs in privilege definitions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports positioned delete.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsPositionedDelete() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports positioned update.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsPositionedUpdate() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports select for update.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSelectForUpdate() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports stored procedures.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsStoredProcedures() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports subqueries in comparisons.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports subqueries in exists.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSubqueriesInExists() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports subqueries in ins.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSubqueriesInIns() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports subqueries in quantifieds.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports correlated subqueries.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports union.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsUnion() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports union all.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsUnionAll() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports open cursors across commit.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports open cursors across rollback.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports open statements across commit.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports open statements across rollback.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the max binary literal length.
     *
     * @return the max binary literal length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxBinaryLiteralLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max char literal length.
     *
     * @return the max char literal length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxCharLiteralLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max column name length.
     *
     * @return the max column name length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxColumnNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max columns in group by.
     *
     * @return the max columns in group by
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxColumnsInGroupBy() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max columns in index.
     *
     * @return the max columns in index
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxColumnsInIndex() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max columns in order by.
     *
     * @return the max columns in order by
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxColumnsInOrderBy() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max columns in select.
     *
     * @return the max columns in select
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxColumnsInSelect() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max columns in table.
     *
     * @return the max columns in table
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxColumnsInTable() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max connections.
     *
     * @return the max connections
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxConnections() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max cursor name length.
     *
     * @return the max cursor name length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxCursorNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max index length.
     *
     * @return the max index length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxIndexLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max schema name length.
     *
     * @return the max schema name length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxSchemaNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max procedure name length.
     *
     * @return the max procedure name length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxProcedureNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max catalog name length.
     *
     * @return the max catalog name length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxCatalogNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max row size.
     *
     * @return the max row size
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxRowSize() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Does max row size include blobs.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the max statement length.
     *
     * @return the max statement length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxStatementLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max statements.
     *
     * @return the max statements
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxStatements() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max table name length.
     *
     * @return the max table name length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxTableNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max tables in select.
     *
     * @return the max tables in select
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxTablesInSelect() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the max user name length.
     *
     * @return the max user name length
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxUserNameLength() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the default transaction isolation.
     *
     * @return the default transaction isolation
     * @throws SQLException the SQL exception
     */
    @Override
    public int getDefaultTransactionIsolation() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Supports transactions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsTransactions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports transaction isolation level.
     *
     * @param level the level
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports data definition and data manipulation transactions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports data manipulation transactions only.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Data definition causes transaction commit.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Data definition ignored in transactions.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the procedures.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param procedureNamePattern the procedure name pattern
     * @return the procedures
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the procedure columns.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param procedureNamePattern the procedure name pattern
     * @param columnNamePattern the column name pattern
     * @return the procedure columns
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the tables.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param tableNamePattern the table name pattern
     * @param types the types
     * @return the tables
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
        // we ignore catalog (not valid in mongodb) and schema pattern (not implemented)
        MongoIterable<String> collections = connection.getMongoDatabase()
                                                      .listCollectionNames();
        ArrayNode array = MAPPER.createArrayNode();
        for (String name : collections) {
            ObjectNode obj = MAPPER.createObjectNode();
            obj.put(TABLE_NAME, name);
            obj.put(TABLE_TYPE, "COLLECTION");
            obj.put(REMARKS, "");
            array.add(obj);
        }
        ResultSet tables = new JsonArrayMongoIteratorResultSet(array);
        return tables;
    }

    /**
     * Gets the schemas.
     *
     * @return the schemas
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getSchemas() throws SQLException {
        return this.getSchemas(null, null);
    }

    /**
     * Gets the catalogs.
     *
     * @return the catalogs
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getCatalogs() throws SQLException {
        return null;
    }

    /**
     * Gets the table types.
     *
     * @return the table types
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getTableTypes() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the columns.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param tableNamePattern the table name pattern
     * @param columnNamePattern the column name pattern
     * @return the columns
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
            throws SQLException {
        Document first = connection.getMongoDatabase()
                                   .getCollection(tableNamePattern)
                                   .find()
                                   .first();
        if (first == null) {
            first = new Document();
            first.append("error", "The first element in the collection is null");
        }
        ArrayNode array = populateColumns(first);
        ResultSet columns = new JsonArrayMongoIteratorResultSet(array);
        return columns;
    }

    /**
     * Populate columns.
     *
     * @param document the document
     * @return the array node
     */
    public ArrayNode populateColumns(Document document) {
        ArrayNode array = MAPPER.createArrayNode();
        for (Entry<String, Object> entry : document.entrySet()) {
            ObjectNode obj = MAPPER.createObjectNode();
            obj.put(COLUMN_NAME, entry.getKey()
                                      .toString());
            obj.put(TYPE_NAME, entry.getValue() != null ? entry.getValue()
                                                               .getClass()
                                                               .getSimpleName()
                    : "?");
            if (entry.getValue() != null && entry.getValue() instanceof Document) {
                Document nestedDocument = (Document) entry.getValue();
                ArrayNode nestedArray = populateColumns(nestedDocument);
                obj.put(NESTED, nestedArray);
            }
            obj.put(COLUMN_SIZE, "");
            obj.put(IS_NULLABLE, true);
            obj.put(DECIMAL_DIGITS, 0);
            obj.put(PK, "_id".equals(entry.getKey()
                                          .toString()));
            array.add(obj);
        }
        return array;
    }

    /**
     * Gets the column privileges.
     *
     * @param catalog the catalog
     * @param schema the schema
     * @param table the table
     * @param columnNamePattern the column name pattern
     * @return the column privileges
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the table privileges.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param tableNamePattern the table name pattern
     * @return the table privileges
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the best row identifier.
     *
     * @param catalog the catalog
     * @param schema the schema
     * @param table the table
     * @param scope the scope
     * @param nullable the nullable
     * @return the best row identifier
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the version columns.
     *
     * @param catalog the catalog
     * @param schema the schema
     * @param table the table
     * @return the version columns
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the primary keys.
     *
     * @param catalog the catalog
     * @param schema the schema
     * @param table the table
     * @return the primary keys
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        ResultSet primaryKeys = new SingleColumnStaticResultSet(Arrays.asList(new String[] {"_id"})
                                                                      .iterator());
        return primaryKeys;
    }

    /**
     * Gets the imported keys.
     *
     * @param catalog the catalog
     * @param schema the schema
     * @param table the table
     * @return the imported keys
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the exported keys.
     *
     * @param catalog the catalog
     * @param schema the schema
     * @param table the table
     * @return the exported keys
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the cross reference.
     *
     * @param parentCatalog the parent catalog
     * @param parentSchema the parent schema
     * @param parentTable the parent table
     * @param foreignCatalog the foreign catalog
     * @param foreignSchema the foreign schema
     * @param foreignTable the foreign table
     * @return the cross reference
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog,
            String foreignSchema, String foreignTable) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the type info.
     *
     * @return the type info
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getTypeInfo() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the index info.
     *
     * @param catalog the catalog
     * @param schema the schema
     * @param table the table
     * @param unique the unique
     * @param approximate the approximate
     * @return the index info
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        ResultSet indexInfo = new SingleColumnStaticResultSet(Arrays.asList(new String[] {})
                                                                    .iterator());
        return indexInfo;
    }

    /**
     * Supports result set type.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsResultSetType(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports result set concurrency.
     *
     * @param type the type
     * @param concurrency the concurrency
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Own updates are visible.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Own deletes are visible.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean ownDeletesAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Own inserts are visible.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean ownInsertsAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Others updates are visible.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Others deletes are visible.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean othersDeletesAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Others inserts are visible.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean othersInsertsAreVisible(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Updates are detected.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean updatesAreDetected(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Deletes are detected.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean deletesAreDetected(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Inserts are detected.
     *
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean insertsAreDetected(int type) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports batch updates.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the UD ts.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param typeNamePattern the type name pattern
     * @param types the types
     * @return the UD ts
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the connection.
     *
     * @return the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public Connection getConnection() throws SQLException {
        return this.connection;
    }

    /**
     * Supports savepoints.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsSavepoints() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports named parameters.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsNamedParameters() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports multiple open results.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports get generated keys.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the super types.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param typeNamePattern the type name pattern
     * @return the super types
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the super tables.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param tableNamePattern the table name pattern
     * @return the super tables
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the attributes.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param typeNamePattern the type name pattern
     * @param attributeNamePattern the attribute name pattern
     * @return the attributes
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Supports result set holdability.
     *
     * @param holdability the holdability
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the result set holdability.
     *
     * @return the result set holdability
     * @throws SQLException the SQL exception
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the database major version.
     *
     * @return the database major version
     * @throws SQLException the SQL exception
     */
    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the database minor version.
     *
     * @return the database minor version
     * @throws SQLException the SQL exception
     */
    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the JDBC major version.
     *
     * @return the JDBC major version
     * @throws SQLException the SQL exception
     */
    @Override
    public int getJDBCMajorVersion() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the JDBC minor version.
     *
     * @return the JDBC minor version
     * @throws SQLException the SQL exception
     */
    @Override
    public int getJDBCMinorVersion() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the SQL state type.
     *
     * @return the SQL state type
     * @throws SQLException the SQL exception
     */
    @Override
    public int getSQLStateType() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Locators update copy.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean locatorsUpdateCopy() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Supports statement pooling.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsStatementPooling() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the row id lifetime.
     *
     * @return the row id lifetime
     * @throws SQLException the SQL exception
     */
    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the schemas.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @return the schemas
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        ResultSet schemas = new SingleColumnStaticResultSet(Arrays.asList(new String[] {connection.mongoDatabase.getName()})
                                                                  .iterator());
        return schemas;
    }

    /**
     * Supports stored functions using call syntax.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Auto commit failure closes all result sets.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the client info properties.
     *
     * @return the client info properties
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the functions.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param functionNamePattern the function name pattern
     * @return the functions
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the function columns.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param functionNamePattern the function name pattern
     * @param columnNamePattern the column name pattern
     * @return the function columns
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the pseudo columns.
     *
     * @param catalog the catalog
     * @param schemaPattern the schema pattern
     * @param tableNamePattern the table name pattern
     * @param columnNamePattern the column name pattern
     * @return the pseudo columns
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Generated key always returned.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

}
