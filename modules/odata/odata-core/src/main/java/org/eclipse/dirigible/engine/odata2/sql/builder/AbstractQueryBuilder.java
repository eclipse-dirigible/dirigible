/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.builder;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.*;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementBuilder;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.clause.SQLJoinClause;
import org.eclipse.dirigible.engine.odata2.sql.clause.SQLWhereClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.olingo.odata2.api.commons.HttpStatusCodes.INTERNAL_SERVER_ERROR;
import static org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding.ColumnInfo;
import static org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils.fqn;

/**
 * The Class AbstractQueryBuilder.
 */
public abstract class AbstractQueryBuilder implements SQLStatementBuilder {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(SQLSelectBuilder.class);

    /** The table aliases for entities in query. */
    private final Map<String, EdmStructuralType> tableAliasesForEntitiesInQuery;

    /** The table aliases for many to many mapping tables in query. */
    private final Map<String, String> tableAliasesForManyToManyMappingTablesInQuery;

    /** The structural types in join. */
    private final Set<String> structuralTypesInJoin;

    /** The table binding. */
    private final EdmTableBindingProvider tableBinding;

    /** The join expressions. */
    private final List<SQLJoinClause> joinExpressions = new ArrayList<>();

    /** The sql statement params. */
    private final List<SQLStatementParam> sqlStatementParams;

    /** The where clause. */
    private final SQLWhereClause whereClause;

    /**
     * Instantiates a new abstract query builder.
     *
     * @param tableBinding the table binding
     */
    public AbstractQueryBuilder(final EdmTableBindingProvider tableBinding) {
        this.tableBinding = tableBinding;
        this.whereClause = new SQLWhereClause();
        this.tableAliasesForEntitiesInQuery = new TreeMap<>();
        this.tableAliasesForManyToManyMappingTablesInQuery = new TreeMap<>();
        this.structuralTypesInJoin = new HashSet<>();
        this.sqlStatementParams = new ArrayList<>();
    }

    /**
     * Gets the statement params.
     *
     * @return the statement params
     */
    public List<SQLStatementParam> getStatementParams() {
        return sqlStatementParams;
    }

    /**
     * Adds the statement param.
     *
     * @param param the param
     */
    public void addStatementParam(SQLStatementParam param) {
        sqlStatementParams.add(param);
    }

    /**
     * Gets the where clause.
     *
     * @return the where clause
     */
    public SQLWhereClause getWhereClause() {
        return whereClause;
    }

    /**
     * Adds the statement param.
     *
     * @param entity the entity
     * @param property the property
     * @param value the value
     * @throws EdmException the edm exception
     */
    public void addStatementParam(EdmNavigationProperty entity, EdmProperty property, Object value) throws EdmException {
        EdmType edmType = entity.getType();
        if (edmType instanceof EdmEntityType) {
            addStatementParam((EdmEntityType) edmType, property, value);
        } else {
            throw new OData2Exception("Not implemented", HttpStatusCodes.NOT_IMPLEMENTED);
        }
    }

    /**
     * Adds the statement param.
     *
     * @param entity the entity
     * @param property the property
     * @param value the value
     * @throws EdmException the edm exception
     */
    public void addStatementParam(EdmStructuralType entity, EdmProperty property, Object value) throws EdmException {
        if (property.isSimple()) {
            EdmTableBinding.ColumnInfo info = getSQLTableColumnInfo(entity, property);
            addStatementParam(new SQLStatementParam(value, property.getType(), info));
        } else {
            throw new IllegalArgumentException("Not Implemented");
        }
    }

    /**
     * Gets the table binding.
     *
     * @return the table binding
     */
    public EdmTableBindingProvider getTableBinding() {
        return tableBinding;
    }

    public String getSQLTableName(EdmStructuralType target, SQLContext context) {
        EdmTableBinding mapping = tableBinding.getEdmTableBinding(target);

        ISqlDialect dialect = SqlDialectFactory.getDialect(context.getDatabaseSystem());
        char escapeSymbol = dialect.getEscapeSymbol();

        Optional<String> schemaName = mapping.getSchemaName();

        return schemaName.isEmpty() ? getTableName(mapping, escapeSymbol)
                : (escape(schemaName.get(), escapeSymbol) + "." + getTableName(mapping, escapeSymbol));
    }

    private String getTableName(EdmTableBinding mapping, char escapeSymbol) {
        String tableName = mapping.getTableName();
        return escape(tableName, escapeSymbol);
    }

    private String escape(String str, char escapeSymbol) {
        return escapeSymbol + str + escapeSymbol;
    }

    /**
     * Gets the SQL table name.
     *
     * @param target the target
     * @return the SQL table name
     */
    public String getSQLTableName(final EdmStructuralType target) { // TODO use context
        EdmTableBinding mapping = tableBinding.getEdmTableBinding(target);

        Optional<String> schemaName = mapping.getSchemaName();

        return schemaName.isEmpty() ? getTableName(mapping) : (escape(schemaName.get()) + "." + getTableName(mapping));
    }

    private String getTableName(EdmTableBinding mapping) {
        String tableName = mapping.getTableName();
        return escape(tableName);
    }

    private String escape(String str) {
        return "\"" + str + "\"";
    }

    /**
     * Gets the SQL mapping table name.
     *
     * @param from the from
     * @param to the to
     * @return the SQL mapping table name
     * @throws EdmException the edm exception
     */
    public List<String> getSQLMappingTableName(final EdmStructuralType from, final EdmStructuralType to) throws EdmException {
        return tableBinding.getEdmTableBinding(from)
                           .getMappingTableName(to);
    }

    /**
     * Gets the SQL mapping table join column.
     *
     * @param from the from
     * @param to the to
     * @return the SQL mapping table join column
     * @throws EdmException the edm exception
     */
    public List<String> getSQLMappingTableJoinColumn(final EdmStructuralType from, final EdmStructuralType to) throws EdmException {
        return tableBinding.getEdmTableBinding(from)
                           .getMappingTableJoinColumn(to);
    }

    /**
     * Gets the SQL join table name.
     *
     * @param from the from
     * @param to the to
     * @return the SQL join table name
     * @throws EdmException the edm exception
     */
    public List<String> getSQLJoinTableName(final EdmStructuralType from, final EdmStructuralType to) throws EdmException {
        if (tableBinding.getEdmTableBinding(from)
                        .hasJoinColumnTo(to))
            return tableBinding.getEdmTableBinding(from)
                               .getJoinColumnTo(to);
        throw new IllegalArgumentException("No join column definition found from type " + from + " to type " + to);
    }

    /**
     * Checks for SQL mapping table present.
     *
     * @param from the from
     * @param to the to
     * @return true, if successful
     * @throws EdmException the edm exception
     */
    public boolean hasSQLMappingTablePresent(final EdmStructuralType from, final EdmStructuralType to) throws EdmException {
        EdmTableBinding mapping = tableBinding.getEdmTableBinding(from);
        return mapping.hasMappingTable(to);
    }

    /**
     * Checks for key generated present.
     *
     * @param target the target
     * @return true, if successful
     */
    public boolean hasKeyGeneratedPresent(final EdmStructuralType target) {
        EdmTableBinding mapping = tableBinding.getEdmTableBinding(target);
        return mapping.isPropertyMapped("keyGenerated");
    }

    /**
     * Checks for aggregation type present.
     *
     * @param target the target
     * @return true, if successful
     */
    public boolean hasAggregationTypePresent(final EdmStructuralType target) {
        EdmTableBinding mapping = tableBinding.getEdmTableBinding(target);
        return mapping.isPropertyMapped("aggregationType");
    }

    /**
     * Checks if is aggregation type explicit.
     *
     * @param target the target
     * @return true, if is aggregation type explicit
     */
    public boolean isAggregationTypeExplicit(final EdmStructuralType target) {
        EdmTableBinding mapping = tableBinding.getEdmTableBinding(target);
        return mapping.isAggregationTypeExplicit();
    }

    /**
     * Checks if is column contained in aggregation prop.
     *
     * @param target the target
     * @param columnName the column name
     * @return true, if is column contained in aggregation prop
     */
    public boolean isColumnContainedInAggregationProp(final EdmStructuralType target, String columnName) {
        EdmTableBinding mapping = tableBinding.getEdmTableBinding(target);
        return mapping.isColumnContainedInAggregationProp(columnName);
    }

    /**
     * Gets the column aggregation type.
     *
     * @param target the target
     * @param columnName the column name
     * @return the column aggregation type
     */
    public String getColumnAggregationType(final EdmStructuralType target, String columnName) {
        EdmTableBinding mapping = tableBinding.getEdmTableBinding(target);
        return mapping.getColumnAggregationType(columnName);
    }

    /**
     * Gets the SQL table primary key.
     *
     * @param type the type
     * @return the SQL table primary key
     * @throws EdmException the edm exception
     */
    public String getSQLTablePrimaryKey(final EdmStructuralType type) throws EdmException {
        return tableBinding.getEdmTableBinding(type)
                           .getPrimaryKey();
    }

    /**
     * Gets the SQL table parameters.
     *
     * @param type the type
     * @return the SQL table parameters
     * @throws EdmException the edm exception
     */
    public List<String> getSQLTableParameters(final EdmStructuralType type) throws EdmException {
        return tableBinding.getEdmTableBinding(type)
                           .getParameters();
    }

    /**
     * Gets the SQL table data structure type.
     *
     * @param type the type
     * @return the SQL table data structure type
     * @throws EdmException the edm exception
     */
    public EdmTableBinding.DataStructureType getSQLTableDataStructureType(final EdmStructuralType type) throws EdmException {
        return tableBinding.getEdmTableBinding(type)
                           .getDataStructureType();
    }

    /**
     * Gets the SQL table column.
     *
     * @param targetEnitityType the target enitity type
     * @param p the p
     * @return the SQL table column
     */
    public String getSQLTableColumn(final EdmStructuralType targetEnitityType, final EdmProperty p) {
        if (p.isSimple()) {
            return "\"" + getSQLTableAlias(targetEnitityType) + "\".\"" + tableBinding.getEdmTableBinding(targetEnitityType)
                                                                                      .getColumnName(p)
                    + "\"";
        } else {
            throw new IllegalArgumentException("Unable to get the table column name of complex property " + p);
        }
    }

    /**
     * Gets the SQL table column no alias.
     *
     * @param targetEnitityType the target enitity type
     * @param p the p
     * @return the SQL table column no alias
     */
    public String getSQLTableColumnNoAlias(final EdmStructuralType targetEnitityType, final EdmProperty p) {
        if (p.isSimple()) {
            return "\"" + tableBinding.getEdmTableBinding(targetEnitityType)
                                      .getColumnName(p)
                    + "\"";
        } else {
            throw new IllegalArgumentException("Unable to get the table column name of complex property " + p);
        }
    }

    /**
     * Fix database names case.
     *
     * @param column the column
     * @return the string
     */
    private String fixDatabaseNamesCase(String column) {
        return "\"" + column + "\"";
    }

    /**
     * Gets the SQL join column no alias.
     *
     * @param targetEnitityType the target enitity type
     * @param p the p
     * @return the SQL join column no alias
     * @throws EdmException the edm exception
     */
    public List<String> getSQLJoinColumnNoAlias(final EdmStructuralType targetEnitityType, final EdmNavigationProperty p)
            throws EdmException {
        List<String> joinColums = tableBinding.getEdmTableBinding(targetEnitityType)
                                              .getJoinColumnTo((EdmStructuralType) p.getType());
        return joinColums.stream()
                         .map(this::fixDatabaseNamesCase)
                         .collect(Collectors.toList());
    }

    /**
     * Gets the pure SQL column name.
     *
     * @param type the type
     * @param p the p
     * @return the pure SQL column name
     * @throws EdmException the edm exception
     */
    public String getPureSQLColumnName(final EdmStructuralType type, final EdmProperty p) throws EdmException {
        return tableBinding.getEdmTableBinding(type)
                           .getColumnName(p);
    }

    /**
     * Gets the SQL table column info.
     *
     * @param targetEnitityType the target enitity type
     * @param p the p
     * @return the SQL table column info
     * @throws EdmException the edm exception
     */
    public ColumnInfo getSQLTableColumnInfo(final EdmStructuralType targetEnitityType, final EdmProperty p) throws EdmException {
        if (p.isSimple()) {
            ColumnInfo info = tableBinding.getEdmTableBinding((targetEnitityType))
                                          .getColumnInfo(p);
            return new ColumnInfo("\"" + getSQLTableAlias(targetEnitityType) + "\".\"" + info.getColumnName() + "\"", info.getJdbcType());

        } else {
            throw new IllegalArgumentException("Unable to get the table column name of complex property " + p);
        }
    }

    /**
     * Gets the SQL table column alias.
     *
     * @param targetEnitityType the target enitity type
     * @param property the property
     * @return the SQL table column alias
     */
    public String getSQLTableColumnAlias(final EdmStructuralType targetEnitityType, final EdmProperty property) {
        if (property.isSimple())
            return tableBinding.getEdmTableBinding(targetEnitityType)
                               .getColumnName(property)
                    + "_" + getSQLTableAlias(targetEnitityType);
        else
            throw new IllegalArgumentException("Unable to get the table column name of complex property " + property);
    }

    /**
     * Checks if is transient type.
     *
     * @param targetEnitityType the target enitity type
     * @param property the property
     * @return true, if is transient type
     */
    public boolean isTransientType(final EdmStructuralType targetEnitityType, final EdmProperty property) {
        return !tableBinding.getEdmTableBinding(targetEnitityType)
                            .isPropertyMapped(property);
    }

    /**
     * Gets the SQL table alias.
     *
     * @param type the type
     * @return the SQL table alias
     */
    public String getSQLTableAlias(final EdmType type) {
        if (type instanceof EdmStructuralType)
            return getTableAliasForType((EdmStructuralType) type);
        else
            throw new IllegalArgumentException("Mapping of types other than EdmEntityType and EdmComplexType is not supported!");
    }

    /**
     * Gets the SQL table alias for many to many mapping table.
     *
     * @param manyToManyMappingTable the many to many mapping table
     * @return the SQL table alias for many to many mapping table
     */
    public String getSQLTableAliasForManyToManyMappingTable(final String manyToManyMappingTable) {
        return getTableAliasForManyToManyMappingTable(manyToManyMappingTable);
    }

    /**
     * Gets the table alias for type.
     *
     * @param st the st
     * @return the table alias for type
     */
    private String getTableAliasForType(final EdmStructuralType st) {
        Collection<String> keys = tableAliasesForEntitiesInQuery.keySet();
        try {
            for (String key : keys) {
                EdmStructuralType type = tableAliasesForEntitiesInQuery.get(key);
                if (fqn(type).equals(fqn(st)))
                    return key;
            }
            return grantTableAliasForStructuralTypeInQuery(st);
        } catch (Exception e) {
            throw new OData2Exception("No mapping has been defined for type " + fqn(st), INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gets the table alias for many to many mapping table.
     *
     * @param manyToManyMappingTable the many to many mapping table
     * @return the table alias for many to many mapping table
     */
    private String getTableAliasForManyToManyMappingTable(final String manyToManyMappingTable) {
        Collection<String> keys = tableAliasesForManyToManyMappingTablesInQuery.keySet();
        for (String key : keys) {
            String target = tableAliasesForManyToManyMappingTablesInQuery.get(key);
            if (target.equals(manyToManyMappingTable))
                return key;
        }
        return grantTableAliasForManyToManyMappingTableInQuery(manyToManyMappingTable);
    }

    /**
     * Get table aliases.
     *
     * @return the aliases
     */
    public Iterator<String> getTablesAliasesForEntitiesInQuery() {
        return tableAliasesForEntitiesInQuery.keySet()
                                             .iterator();
    }

    /**
     * Gets the entity in query for alias.
     *
     * @param tableAlias the table alias
     * @return the entity in query for alias
     */
    public EdmStructuralType getEntityInQueryForAlias(final String tableAlias) {
        return tableAliasesForEntitiesInQuery.get(tableAlias);
    }

    /**
     * Grant table alias for structural type in query.
     *
     * @param entity the entity
     * @return the string
     */
    public String grantTableAliasForStructuralTypeInQuery(final EdmStructuralType entity) {
        try {
            Collection<EdmStructuralType> targets = tableAliasesForEntitiesInQuery.values();
            for (EdmStructuralType type : targets) {
                if (fqn(type).equals(fqn(entity)))
                    // Alias is already contained in the map
                    return getTableAliasForType(type);
            }
            String alias = "T" + tableAliasesForEntitiesInQuery.size();
            LOG.debug("Grant Alias '" + alias + "' for " + entity.getName());
            // Add alias to map
            tableAliasesForEntitiesInQuery.put("T" + tableAliasesForEntitiesInQuery.size(), entity);
            return alias;
        } catch (EdmException e) {
            throw new OData2Exception(INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Grant table alias for many to many mapping table in query.
     *
     * @param manyToManyMappingTable the many to many mapping table
     * @return the string
     */
    private String grantTableAliasForManyToManyMappingTableInQuery(final String manyToManyMappingTable) {
        Collection<String> targets = tableAliasesForManyToManyMappingTablesInQuery.values();
        for (String target : targets) {
            if (target.equals(manyToManyMappingTable))
                // Alias is already contained in the map
                return getTableAliasForManyToManyMappingTable(target);
        }
        String alias = "MT" + tableAliasesForManyToManyMappingTablesInQuery.size();
        LOG.debug("Grant Alias '" + alias + "' for " + manyToManyMappingTable);
        // Add alias to map
        tableAliasesForManyToManyMappingTablesInQuery.put("MT" + tableAliasesForManyToManyMappingTablesInQuery.size(),
                manyToManyMappingTable);
        return alias;
    }

}
