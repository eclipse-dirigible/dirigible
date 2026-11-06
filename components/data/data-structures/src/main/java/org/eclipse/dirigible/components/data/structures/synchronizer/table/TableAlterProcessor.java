/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.table;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintUnique;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;

import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.database.DatabaseNameNormalizer;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.SqlException;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.table.AlterTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Table Alter Processor.
 */
public class TableAlterProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TableAlterProcessor.class);

    /** The Constant INCOMPATIBLE_CHANGE_OF_TABLE. */
    private static final String INCOMPATIBLE_CHANGE_OF_TABLE = "Incompatible change of table [%s] by adding a column [%s] which is [%s]";

    /**
     * The tables, by {@link #tableIdentity}, whose declared unique keys this process saw holding - the
     * ones {@link #retryUniqueConstraints} has nothing to attempt on (#8317).
     */
    private static final Set<String> UNIQUE_KEYS_HOLD = ConcurrentHashMap.newKeySet();

    /**
     * PostgreSQL's {@code duplicate_table}: a relation - table or index - of that name already exists.
     */
    private static final String DUPLICATE_RELATION_SQL_STATE = "42P07";
    // $NON-NLS-1$

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, Table tableModel) throws SQLException {
        String tableName = "\"" + tableModel.getName() + "\"";

        logger.info("Processing Alter Table: " + tableName);

        Map<String, String> columnDefinitions = new HashMap<>();
        // the live columns' names as the database spells them, and the ones it holds NOT NULL - both
        // keyed by the canonical (upper-case) name every comparison below uses
        Map<String, String> liveNames = new HashMap<>();
        Set<String> notNullColumns = new HashSet<>();
        DatabaseMetaData dmd = connection.getMetaData();
        String schema = connection.getSchema();
        ResultSet rsColumns = dmd.getColumns(null, schema, DatabaseNameNormalizer.normalizeTableName(tableName), null);
        while (rsColumns.next()) {
            int columnType = rsColumns.getInt(5);
            String liveName = rsColumns.getString(4);
            String columnName = liveName.toUpperCase();
            try {
                String typeName = DataTypeUtils.getDatabaseTypeName(columnType);
                String canonicalName = DatabaseNameNormalizer.normalizeColumnName(columnName);
                columnDefinitions.put(canonicalName, typeName);
                liveNames.put(canonicalName, liveName);
                if (rsColumns.getInt(11) == DatabaseMetaData.columnNoNulls) {
                    notNullColumns.add(canonicalName);
                }
            } catch (SqlException ex) {
                String errorMessage = "Missing type for column [" + columnName + "] and type [" + columnType + "]";
                throw new SqlException(errorMessage, ex);
            }
        }

        renameColumns(connection, tableModel, columnDefinitions, liveNames, notNullColumns);

        List<String> modelColumnNames = new ArrayList<>();

        // ADD iteration
        for (TableColumn columnModel : tableModel.getColumns()) {
            String name = DatabaseNameNormalizer.normalizeColumnName(columnModel.getName());

            DataType type = DataType.valueOfByName(columnModel.getType());
            String length = columnModel.getLength();
            boolean isNullable = columnModel.isNullable();
            boolean isPrimaryKey = columnModel.isPrimaryKey();
            boolean isUnique = columnModel.isUnique();
            String defaultValue = columnModel.getDefaultValue();
            String scale = columnModel.getScale();
            String args = "";
            if (length != null) {
                if (type.equals(DataType.VARCHAR) || type.equals(DataType.CHAR) || type.equals(DataType.NVARCHAR)
                        || type.equals(DataType.CHARACTER_VARYING) || type.equals(DataType.CHARACTER)) {
                    args = ISqlKeywords.OPEN + length + ISqlKeywords.CLOSE;
                }
                if (scale != null) {
                    if (type.equals(DataType.DECIMAL)) {
                        args = ISqlKeywords.OPEN + length + "," + scale + ISqlKeywords.CLOSE;
                    }
                }
            }
            String defaultClause = ColumnDefaultClause.of(type, defaultValue);
            args += defaultClause;

            modelColumnNames.add(name.toUpperCase());

            String nameOriginalCanonical = name.toUpperCase();
            if (!columnDefinitions.containsKey(nameOriginalCanonical)) {
                // a rename this database cannot do in place: the new column is added and the values are
                // copied over, while the old column stays (undeclared, so kept) - the NOT NULL the model
                // declares cannot be added to a column that starts out empty
                String copyFrom = renamedLiveColumn(columnModel, liveNames);
                if (copyFrom != null && !isNullable) {
                    logger.warn("Column [{}] of table [{}] is renamed from [{}] by adding it - it is created nullable,"
                            + " this database cannot rename a column in place", name, tableName, copyFrom);
                    isNullable = true;
                }
                // a NOT NULL column with a declared default is added as such, the existing rows taking
                // the default (#8339); without one, the rows already in the table would hold NULL in it,
                // which every database refuses - so it is added nullable, once, rather than failing the
                // table on every pass
                if (copyFrom == null && !isNullable && !isPrimaryKey && !declaresValue(defaultClause, defaultValue)
                        && holdsRows(connection, tableName)) {
                    logger.warn("Column [{}] of table [{}] is declared NOT NULL without a default and the table already holds rows"
                            + " - it is added nullable; fill it and declare a default to have it enforced", name, tableName);
                    isNullable = true;
                }

                AlterTableBuilder alterTableBuilder = SqlFactory.getNative(connection)
                                                                .alter()
                                                                .table(tableName);

                alterTableBuilder.add()
                                 .column("\"" + name + "\"", type, isPrimaryKey, isNullable, isUnique, args);

                if (isPrimaryKey) {
                    logger.error("Column Definitions: {}", columnDefinitions);
                    throw new SQLException(String.format(INCOMPATIBLE_CHANGE_OF_TABLE, tableName, name, "PRIMARY KEY"));
                }

                executeAlterBuilder(connection, alterTableBuilder);
                if (copyFrom != null) {
                    copyColumn(connection, tableName, copyFrom, name);
                }

            } else {
                String typeFromMetadata = columnDefinitions.get(nameOriginalCanonical);
                String typeFromDefinition = type.toString();
                String unifiedTypeFromMetadata = DataTypeUtils.getUnifiedDatabaseType(typeFromMetadata);
                String unifiedTypeFromDefinition = DataTypeUtils.getUnifiedDatabaseType(typeFromDefinition);
                if (!unifiedTypeFromMetadata.equals(unifiedTypeFromDefinition)) {
                    if (!DataTypeUtils.isCharacterType(unifiedTypeFromMetadata)
                            || !DataTypeUtils.isCharacterType(unifiedTypeFromDefinition)) {
                        logger.error("Column Definitions: {}", columnDefinitions);
                        throw new SQLException(String.format(INCOMPATIBLE_CHANGE_OF_TABLE, tableName, name,
                                "of type " + typeFromMetadata + " to be changed to " + type));
                    }
                    // Both are character types, so the existing column can hold the same kind of value and
                    // is left as it is - the alternative, failing the whole table, drowns the real
                    // incompatible changes in noise on every re-publish over an existing database.
                    logger.warn(
                            "Column [{}] of table [{}] is [{}] in the database while it is defined as [{}]. The column is kept as it is.",
                            name, tableName, typeFromMetadata, typeFromDefinition);
                }
            }
        }

        reconcileUndeclaredColumns(connection, tableName, tableModel, modelColumnNames, liveNames, notNullColumns);
        reconcileUniqueConstraints(connection, tableName, tableModel);
    }

    /**
     * Whether a column's DEFAULT clause gives the existing rows a value - a declared {@code NULL}
     * default does not.
     *
     * @param defaultClause the rendered clause, empty when the column declares no default
     * @param defaultValue the default as the model carries it
     * @return whether the rows already in the table would get a value
     */
    private static boolean declaresValue(String defaultClause, String defaultValue) {
        return !defaultClause.isEmpty() && !"NULL".equalsIgnoreCase(defaultValue.trim());
    }

    /**
     * Whether the table holds at least one row.
     *
     * @param connection the connection
     * @param quotedTableName the table name as the builders want it (quoted)
     * @return whether the table holds rows
     * @throws SQLException when the count fails
     */
    private static boolean holdsRows(Connection connection, String quotedTableName) throws SQLException {
        return SqlFactory.deriveDialect(connection)
                         .count(connection, DatabaseNameNormalizer.normalizeTableName(quotedTableName)) > 0;
    }

    /**
     * Renames in place every live column the model declares a column {@code renamedFrom} (#7635), so
     * the values move with the name instead of the old column being left behind and the new one added
     * empty. A rename runs only while the old name is live and the new one is not, so a second run
     * issues nothing. On a database without an in-place rename the column is left to the ADD pass,
     * which copies the values over (see {@link #renamedLiveColumn}). The live-column maps are updated
     * to what the database holds afterwards.
     *
     * @param connection the connection
     * @param tableModel the model
     * @param columnDefinitions the live column types, by canonical name
     * @param liveNames the live column names as the database spells them, by canonical name
     * @param notNullColumns the canonical names of the live NOT NULL columns
     * @throws SQLException when the database refuses the rename - the data must not be split across two
     *         columns, so the table fails rather than falling through to the ADD
     */
    private static void renameColumns(Connection connection, Table tableModel, Map<String, String> columnDefinitions,
            Map<String, String> liveNames, Set<String> notNullColumns) throws SQLException {
        ISqlDialect dialect = SqlFactory.deriveDialect(connection);
        String table = DatabaseNameNormalizer.normalizeTableName(tableModel.getName());
        for (TableColumn columnModel : tableModel.getColumns()) {
            String from = renamedLiveColumn(columnModel, liveNames);
            if (from == null) {
                continue;
            }
            String to = DatabaseNameNormalizer.normalizeColumnName(columnModel.getName());
            String sql = dialect.renameColumn(table, from, to);
            if (sql == null) {
                continue;
            }
            logger.info("Renaming column [{}] of table [{}] to [{}], declared by [{}]", from, table, to, tableModel.getLocation());
            executeStatement(connection, sql);
            String fromKey = canonical(from);
            String toKey = canonical(to);
            columnDefinitions.put(toKey, columnDefinitions.remove(fromKey));
            liveNames.remove(fromKey);
            liveNames.put(toKey, to);
            if (notNullColumns.remove(fromKey)) {
                notNullColumns.add(toKey);
            }
        }
    }

    /**
     * The live column a model column is renamed from - only while the rename is still to be done: the
     * old name is live and the new one is not.
     *
     * @param columnModel the model column
     * @param liveNames the live column names as the database spells them, by canonical name
     * @return the live name of the old column, or null when there is nothing to rename
     */
    private static String renamedLiveColumn(TableColumn columnModel, Map<String, String> liveNames) {
        String renamedFrom = columnModel.getRenamedFrom();
        if (renamedFrom == null || renamedFrom.isBlank()) {
            return null;
        }
        String toKey = canonical(DatabaseNameNormalizer.normalizeColumnName(columnModel.getName()));
        String fromKey = canonical(DatabaseNameNormalizer.normalizeColumnName(renamedFrom.trim()));
        if (fromKey.equals(toKey) || liveNames.containsKey(toKey)) {
            return null;
        }
        return liveNames.get(fromKey);
    }

    /**
     * Copies the values of a renamed column into the column that replaces it, on a database that cannot
     * rename in place.
     *
     * @param connection the connection
     * @param quotedTableName the table name as the builders want it (quoted)
     * @param from the live name of the old column
     * @param to the new column
     * @throws SQLException when the copy fails
     */
    private static void copyColumn(Connection connection, String quotedTableName, String from, String to) throws SQLException {
        String escape = String.valueOf(SqlFactory.deriveDialect(connection)
                                                 .getEscapeSymbol());
        String sql = SqlFactory.getNative(connection)
                               .update()
                               .table(quotedTableName)
                               .set("\"" + to + "\"", escape + from + escape)
                               .build();
        logger.info("Copying the values of column [{}] of table [{}] into its successor [{}]", from, quotedTableName, to);
        executeStatement(connection, sql);
    }

    /**
     * Settles every live column the model does not declare (#7635). A column is dropped only when the
     * table lists it as {@code dropped}, or when the operator opted into
     * {@link DirigibleConfig#DATABASE_DROP_UNDECLARED_COLUMNS}; any other is KEPT with its data - a
     * renamed or removed field must never cost the rows their values, in any tenant - logged at WARN
     * with the artefact that owns the table, and counted in the {@code artefacts} health component. A
     * kept column the database holds NOT NULL is relaxed to nullable, because the application no longer
     * writes it and every insert would be refused otherwise.
     *
     * @param connection the connection
     * @param quotedTableName the table name as the builders want it (quoted)
     * @param tableModel the model
     * @param modelColumnNames the canonical names of the declared columns
     * @param liveNames the live column names as the database spells them, by canonical name
     * @param notNullColumns the canonical names of the live NOT NULL columns
     * @throws SQLException when a declared drop fails
     */
    private static void reconcileUndeclaredColumns(Connection connection, String quotedTableName, Table tableModel,
            List<String> modelColumnNames, Map<String, String> liveNames, Set<String> notNullColumns) throws SQLException {
        String table = DatabaseNameNormalizer.normalizeTableName(quotedTableName);
        Set<String> declaredDrops = new HashSet<>();
        if (tableModel.getDropped() != null) {
            for (String dropped : tableModel.getDropped()) {
                String droppedKey = canonical(DatabaseNameNormalizer.normalizeColumnName(dropped.trim()));
                if (modelColumnNames.contains(droppedKey)) {
                    logger.warn("Column [{}] of table [{}] is both declared and listed as dropped by [{}] - it is kept", dropped, table,
                            tableModel.getLocation());
                } else {
                    declaredDrops.add(droppedKey);
                }
            }
        }
        boolean dropUndeclared = DirigibleConfig.DATABASE_DROP_UNDECLARED_COLUMNS.getBooleanValue();
        Set<String> orphans = new TreeSet<>();
        for (Map.Entry<String, String> live : liveNames.entrySet()) {
            String key = live.getKey();
            String column = live.getValue();
            if (modelColumnNames.contains(key)) {
                continue;
            }
            if (declaredDrops.contains(key) || dropUndeclared) {
                logger.warn("Dropping column [{}] of table [{}] with its data - {}", column, table,
                        declaredDrops.contains(key) ? "listed as dropped by [" + tableModel.getLocation() + "]"
                                : DirigibleConfig.DATABASE_DROP_UNDECLARED_COLUMNS.getKey() + " is on");
                AlterTableBuilder alterTableBuilder = SqlFactory.getNative(connection)
                                                                .alter()
                                                                .table(quotedTableName);
                alterTableBuilder.drop()
                                 .column("\"" + column + "\"", DataType.BOOLEAN);
                executeAlterBuilder(connection, alterTableBuilder);
                continue;
            }
            orphans.add(column);
            logger.warn("Column [{}] of table [{}] is not declared by [{}] and is kept with its data - list it as dropped to remove it",
                    column, table, tableModel.getLocation());
            if (notNullColumns.contains(key)) {
                relaxNotNull(connection, table, column);
            }
        }
        PlatformReadiness.getInstance()
                         .recordOrphanColumns(connection.getCatalog() + "." + connection.getSchema() + "." + table, orphans);
    }

    /**
     * Lets a kept undeclared column hold NULL, fail-soft: a database that cannot express it, or refuses
     * it (the column is part of the primary key), leaves the column as it is with the consequence
     * logged, without failing the table.
     *
     * @param connection the connection
     * @param table the (unquoted) table
     * @param column the live column name
     */
    private static void relaxNotNull(Connection connection, String table, String column) {
        String sql = SqlFactory.deriveDialect(connection)
                               .dropNotNull(table, column);
        if (sql == null) {
            logger.warn("Column [{}] of table [{}] is NOT NULL and this database cannot relax it here - an insert that does not set it"
                    + " is refused; declare the column again or list it as dropped", column, table);
            return;
        }
        logger.info("Column [{}] of table [{}] is no longer written, so it is made nullable", column, table);
        try {
            executeStatement(connection, sql);
        } catch (SQLException e) {
            logger.error("Column [{}] of table [{}] could not be made nullable - an insert that does not set it is refused", column, table,
                    e);
        }
    }

    private static String canonical(String columnName) {
        return columnName.toUpperCase();
    }

    /**
     * Executes one statement of the alter path.
     *
     * @param connection the connection
     * @param sql the statement
     * @throws SQLException when the database refuses it
     */
    private static void executeStatement(Connection connection, String sql) throws SQLException {
        logger.info(sql);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    /**
     * Brings the table's UNIQUE constraints in line with the model - the half of schema evolution the
     * column pass above never covered (#7019). A key the model declares (a {@code unique} column or a
     * composite {@code uniqueIndexes} entry) that the database lacks is ADDED; a UNIQUE the database
     * enforces that the model no longer declares is DROPPED - a constraint holds no data, so unlike an
     * undeclared column (#7635) it costs nothing to remove. Keys are compared as column SETS, so a
     * differently named but equal key is left alone and a second run issues nothing. PRIMARY KEY and
     * FOREIGN KEY constraints are never touched.
     *
     * <p>
     * Fails soft: a dialect without a catalog of unique constraints skips the step (no change from
     * before), and a statement the database refuses - duplicate rows already present, a foreign key
     * that depends on the unique - is logged with the table and key, without failing the column
     * reconciliation or the publish.
     *
     * <p>
     * A key that could not be added is not forgotten (#8317): the table is left out of the ones whose
     * keys are known to hold, so {@link #retryUniqueConstraints} attempts it again on a later pass.
     *
     * @param connection the connection
     * @param quotedTableName the table name as the alter builder wants it (quoted)
     * @param tableModel the model
     */
    static void reconcileUniqueConstraints(Connection connection, String quotedTableName, Table tableModel) {
        reconcileUniqueConstraints(connection, quotedTableName, tableModel, true);
    }

    /**
     * Attempts again the declared UNIQUE keys of an existing table that this process has not yet seen
     * holding (#8317) - one the alter could not add, the duplicate rows that blocked it since resolved
     * by a migration or by hand, or any table not checked since the process started. Only the missing
     * keys are added; nothing is dropped, that stays the alter's. A table whose keys hold is remembered
     * and costs nothing on later calls; one that is still refused is logged and attempted again on the
     * next call, without failing anything - its artefact stays as it is.
     *
     * @param connection the connection
     * @param tableModel the model
     */
    public static void retryUniqueConstraints(Connection connection, Table tableModel) {
        String quotedTableName = "\"" + tableModel.getName() + "\"";
        try {
            if (UNIQUE_KEYS_HOLD.contains(tableIdentity(connection, quotedTableName)) || !SqlFactory.getNative(connection)
                                                                                                    .existsTable(connection,
                                                                                                            tableModel.getName())) {
                return;
            }
        } catch (SQLException e) {
            logger.warn("Unique constraints of table [{}] are not attempted again on this pass - the database could not be read",
                    tableModel.getName(), e);
            return;
        }
        reconcileUniqueConstraints(connection, quotedTableName, tableModel, false);
    }

    /**
     * The identity of a table across the tenants and databases this process serves.
     *
     * @param connection the connection
     * @param quotedTableName the table name as the alter builder wants it (quoted)
     * @return the catalog, schema and table
     * @throws SQLException when the connection cannot name its catalog or schema
     */
    private static String tableIdentity(Connection connection, String quotedTableName) throws SQLException {
        return connection.getCatalog() + "." + connection.getSchema() + "." + DatabaseNameNormalizer.normalizeTableName(quotedTableName);
    }

    private static void reconcileUniqueConstraints(Connection connection, String quotedTableName, Table tableModel,
            boolean dropUndeclared) {
        String tableName = DatabaseNameNormalizer.normalizeTableName(quotedTableName);
        Map<String, List<String>> existing;
        try {
            existing = SqlFactory.getNative(connection)
                                 .uniqueConstraints(connection, tableName);
        } catch (SQLException e) {
            logger.warn("Unique constraints of table [{}] are not reconciled - the catalog read failed: {}", tableName, e.getMessage());
            return;
        }
        if (existing == null) {
            logger.info("Unique constraints of table [{}] are not reconciled - this database exposes no catalog of them", tableName);
            rememberWhetherDeclaredKeysHold(connection, quotedTableName, true);
            return;
        }
        existing.replaceAll((name, columns) -> columns.stream()
                                                      .map(String::toUpperCase)
                                                      .toList());
        Map<Set<String>, DesiredUnique> desired = new LinkedHashMap<>();
        for (TableColumn column : tableModel.getColumns()) {
            if (column.isUnique() && !column.isPrimaryKey()) {
                String columnName = DatabaseNameNormalizer.normalizeColumnName(column.getName())
                                                          .toUpperCase();
                desired.putIfAbsent(Set.of(columnName), new DesiredUnique(tableName + "_" + columnName + "_UNIQUE", List.of(columnName)));
            }
        }
        if (tableModel.getConstraints() != null && tableModel.getConstraints()
                                                             .getUniqueIndexes() != null) {
            for (TableConstraintUnique unique : tableModel.getConstraints()
                                                          .getUniqueIndexes()) {
                if (unique.getColumns() == null || unique.getColumns().length == 0) {
                    continue;
                }
                List<String> columns = new ArrayList<>(unique.getColumns().length);
                for (String column : unique.getColumns()) {
                    columns.add(DatabaseNameNormalizer.normalizeColumnName(column)
                                                      .toUpperCase());
                }
                String name = unique.getName() != null && !unique.getName()
                                                                 .isBlank() ? unique.getName()
                                                                         : tableName + "_" + String.join("_", columns) + "_UNIQUE";
                desired.putIfAbsent(new HashSet<>(columns), new DesiredUnique(name, columns));
            }
        }
        Set<Set<String>> present = new HashSet<>();
        for (Map.Entry<String, List<String>> constraint : existing.entrySet()) {
            Set<String> columns = new HashSet<>(constraint.getValue());
            if (desired.containsKey(columns)) {
                present.add(columns);
                continue;
            }
            if (!dropUndeclared) {
                continue;
            }
            logger.warn("Dropping unique constraint [{}] on table [{}] over {} - the model no longer declares it", constraint.getKey(),
                    tableName, constraint.getValue());
            AlterTableBuilder drop = SqlFactory.getNative(connection)
                                               .alter()
                                               .table(quotedTableName);
            drop.drop()
                .unique(constraint.getKey(), constraint.getValue()
                                                       .toArray(new String[0]));
            executeConstraintChange(connection, drop, tableName, constraint.getKey());
        }
        boolean declaredKeysHold = true;
        Set<Set<String>> indexed = desired.size() > present.size() ? uniqueIndexes(connection, tableName) : Set.of();
        for (Map.Entry<Set<String>, DesiredUnique> key : desired.entrySet()) {
            if (present.contains(key.getKey()) || indexed.contains(key.getKey())) {
                continue;
            }
            DesiredUnique unique = key.getValue();
            logger.info("Adding unique constraint [{}] on table [{}] over {}", unique.name(), tableName, unique.columns());
            AlterTableBuilder add = SqlFactory.getNative(connection)
                                              .alter()
                                              .table(quotedTableName);
            add.add()
               .unique(unique.name(), unique.columns()
                                            .toArray(new String[0]));
            declaredKeysHold &= executeConstraintChange(connection, add, tableName, unique.name());
        }
        rememberWhetherDeclaredKeysHold(connection, quotedTableName, declaredKeysHold);
    }

    /**
     * Records whether every declared key of the table holds, so that {@link #retryUniqueConstraints}
     * skips it or attempts it again.
     *
     * @param connection the connection
     * @param quotedTableName the table name as the alter builder wants it (quoted)
     * @param declaredKeysHold whether every declared key holds
     */
    private static void rememberWhetherDeclaredKeysHold(Connection connection, String quotedTableName, boolean declaredKeysHold) {
        try {
            String identity = tableIdentity(connection, quotedTableName);
            if (declaredKeysHold) {
                UNIQUE_KEYS_HOLD.add(identity);
            } else {
                UNIQUE_KEYS_HOLD.remove(identity);
            }
        } catch (SQLException e) {
            logger.warn("Table [{}] is not recorded as reconciled - the connection cannot name its catalog, so its unique constraints"
                    + " are checked again on the next pass", quotedTableName, e);
        }
    }

    /**
     * The column sets of the table's unique indexes. A table created with a composite key holds it as a
     * UNIQUE INDEX, which the catalog of constraints does not list - the key is enforced all the same
     * and must not be added a second time as a constraint (#8317).
     *
     * @param connection the connection
     * @param tableName the (unquoted) table
     * @return the column sets, upper-cased; empty when the database cannot list them
     */
    private static Set<Set<String>> uniqueIndexes(Connection connection, String tableName) {
        Map<String, Set<String>> indexes = new HashMap<>();
        try (ResultSet rs = connection.getMetaData()
                                      .getIndexInfo(null, connection.getSchema(), tableName, true, false)) {
            while (rs.next()) {
                String index = rs.getString("INDEX_NAME");
                String column = rs.getString("COLUMN_NAME");
                if (index != null && column != null) {
                    indexes.computeIfAbsent(index, name -> new HashSet<>())
                           .add(column.toUpperCase());
                }
            }
        } catch (SQLException e) {
            logger.warn("Unique indexes of table [{}] could not be read - a declared key they already enforce may be added again",
                    tableName, e);
            return Set.of();
        }
        return new HashSet<>(indexes.values());
    }

    /** A key the model wants: its constraint name and its columns in the declared order. */
    private record DesiredUnique(String name, List<String> columns) {
    }

    /**
     * One ADD/DROP CONSTRAINT statement, fail-soft (see {@link #reconcileUniqueConstraints}).
     *
     * @param connection the connection
     * @param builder the built statement
     * @param tableName the table, for the log
     * @param constraint the constraint, for the log
     * @return whether the database took the change
     */
    private static boolean executeConstraintChange(Connection connection, AlterTableBuilder builder, String tableName, String constraint) {
        String sql = builder.build();
        if (logger.isInfoEnabled()) {
            logger.info(sql);
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (isNameTaken(e)) {
                logger.warn("Unique constraint [{}] on table [{}] is not added - the name is already taken by another index or table in"
                        + " the schema, over other columns than the declared ones. Rename the key or drop that relation; it is attempted"
                        + " again on the next pass. Statement: [{}], database: {}", constraint, tableName, sql, e.getMessage());
                return false;
            }
            logger.error("Unique constraint [{}] on table [{}] could not be reconciled - the table's other changes stand, and it is"
                    + " attempted again on the next pass. Statement: [{}]", constraint, tableName, sql, e);
            return false;
        }
    }

    /**
     * Whether the database refused the statement because the constraint's name is taken by another
     * relation (#8498). On PostgreSQL a constraint is backed by an index of the same name, and index
     * names are unique across the schema, so an index the key was born as - or any other relation of
     * that name - refuses the ADD with SQLState {@code 42P07}. A key whose index already covers the
     * declared columns never gets here: {@link #uniqueIndexes} counts it as present.
     *
     * @param e the refusal
     * @return whether it is a name clash rather than a refusal of the key itself
     */
    static boolean isNameTaken(SQLException e) {
        for (SQLException cause = e; cause != null; cause = cause.getNextException()) {
            if (DUPLICATE_RELATION_SQL_STATE.equals(cause.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Execute alter builder.
     *
     * @param connection the connection
     * @param alterTableBuilder the alter table builder
     * @throws SQLException the SQL exception
     */
    private static void executeAlterBuilder(Connection connection, AlterTableBuilder alterTableBuilder) throws SQLException {
        final String sql = alterTableBuilder.build();
        if (logger.isInfoEnabled()) {
            logger.info(sql);
        }
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            statement.executeUpdate();
        } catch (SQLException e) {
            if (logger.isErrorEnabled()) {
                logger.error(sql);
            }
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new SQLException(e.getMessage(), e);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

}
