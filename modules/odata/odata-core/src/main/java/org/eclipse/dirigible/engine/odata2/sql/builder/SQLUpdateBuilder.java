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
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatement;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.clause.SQLWhereClause;
import org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.dirigible.engine.odata2.sql.builder.SQLUtils.*;
import static org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils.fqn;

/**
 * The Class SQLUpdateBuilder.
 */
public class SQLUpdateBuilder extends AbstractQueryBuilder {

    /** The uri key properties. */
    private final Map<String, Object> uriKeyProperties;

    /** The target. */
    private EdmEntityType target;

    /** The update entry. */
    private ODataEntry updateEntry;

    /** The table name. */
    private String tableName;

    /** The non key column names. */
    private final List<String> nonKeyColumnNames = new ArrayList<>();

    /**
     * Instantiates a new SQL update builder.
     *
     * @param tableMappingProvider the table mapping provider
     * @param uriKeys the uri keys
     * @throws ODataException the o data exception
     */
    public SQLUpdateBuilder(final EdmTableBindingProvider tableMappingProvider, Map<String, Object> uriKeys) throws ODataException {
        super(tableMappingProvider);
        this.uriKeyProperties = uriKeys;
    }

    /**
     * Update.
     *
     * @param target the target
     * @param updateEntry the update entry
     * @return the SQL update builder
     * @throws ODataException the o data exception
     */
    public SQLUpdateBuilder update(EdmEntityType target, ODataEntry updateEntry) throws ODataException {
        this.updateEntry = updateEntry;
        this.target = target;
        return this;
    }

    /**
     * Gets the update entry.
     *
     * @return the update entry
     */
    public ODataEntry getUpdateEntry() {
        return updateEntry;
    }

    /**
     * Builds the.
     *
     * @param context the context
     * @return the SQL statement
     */
    @Override
    public SQLStatement build(SQLContext context) {
        return new SQLStatement() {
            // TODO make immutable
            @Override
            public String sql() throws ODataException {
                initializeQuery();
                StringBuilder builder = new StringBuilder();
                builder.append("UPDATE ");
                builder.append(getTargetTableName());
                builder.append(" SET ");
                builder.append(buildColumnList())
                       .append(" WHERE ");

                SQLWhereClause where = new SQLWhereClause(buildWhereClauseForKeys());
                where.and(getWhereClause()); // the interceptor added where clause

                builder.append(where.getWhereClause());
                return SQLUtils.assertParametersCount(normalizeSQLExpression(builder.toString()), getStatementParams());
            }

            @Override
            public List<SQLStatementParam> getStatementParams() {
                return SQLUpdateBuilder.this.getStatementParams();
            }

            @Override
            public boolean isEmpty() {
                return nonKeyColumnNames.isEmpty();
            }
        };
    }

    /**
     * Gets the target.
     *
     * @return the target
     */
    public EdmStructuralType getTarget() {
        return target;
    }

    /**
     * Sets the table name.
     *
     * @param tableName the table name
     * @return the SQL update builder
     */
    public SQLUpdateBuilder setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    /**
     * Gets the target table name.
     *
     * @return the target table name
     */
    public String getTargetTableName() {
        return tableName != null ? tableName : buildInto();
    }

    /**
     * Initialize query.
     *
     * @throws ODataException the o data exception
     */
    public void initializeQuery() throws ODataException {

        grantTableAliasForStructuralTypeInQuery(target);
        Map<String, Object> entryValues = updateEntry.getProperties();

        for (EdmProperty property : EdmUtils.getProperties(target)) { // we iterate first the own properties of the type
            if (entryValues.containsKey(property.getName()) && !isKeyProperty(target, property)) {
                String columnName = getSQLTableColumnNoAlias(target, property);
                nonKeyColumnNames.add(columnName);
                this.addStatementParam(target, property, entryValues.get(property.getName()));

            }
        }

        for (EdmNavigationProperty inlineEntry : EdmUtils.getNavigationProperties(target)) {
            if (entryValues.containsKey(inlineEntry.getName())) {
                Collection<EdmProperty> inlineEntityKeys = EdmUtils.getKeyProperties(inlineEntry);
                if (!inlineEntityKeys.isEmpty()) {
                    nonKeyColumnNames.addAll(getSQLJoinColumnNoAlias(target, inlineEntry));
                    for (EdmProperty inlineEntityKey : inlineEntityKeys) {
                        Object value = OData2Utils.getInlineEntryKeyValue(entryValues, inlineEntry, inlineEntityKey);
                        this.addStatementParam(inlineEntry, inlineEntityKey, value);
                    }
                } else {
                    throw new ODataException("Deep update not implemented yet. Please split the update requests!");
                }
            }
        }
        // Add the key values in the end. The syntax is update table set (col=val) where key1=val1,key2=val2
        for (String key : target.getKeyPropertyNames()) { // the order matters
            Object value = uriKeyProperties.get(key);
            if (!isValidKeyValue(value)) {
                throw new OData2Exception("Invalid key value for property " + key, HttpStatusCodes.BAD_REQUEST);
            }
            EdmProperty property = (EdmProperty) target.getProperty(key);
            this.addStatementParam(target, property, uriKeyProperties.get(key));
        }
    }

    /**
     * Builds the into.
     *
     * @return the string
     */
    private String buildInto() {
        StringBuilder into = new StringBuilder();
        Iterator<String> it = getTablesAliasesForEntitiesInQuery();
        while (it.hasNext()) {
            String tableAlias = it.next();
            EdmStructuralType target = getEntityInQueryForAlias(tableAlias);
            if (isUpdateTarget(target)) {
                into.append(getSQLTableName(target));
                break;
            }
        }
        return into.toString();
    }

    /**
     * Checks if is update target.
     *
     * @param target the target
     * @return true, if is update target
     */
    protected boolean isUpdateTarget(final EdmStructuralType target) {
        // always select the entity target
        return fqn(getTarget()).equals(fqn(target));
    }

    /**
     * Builds the column list.
     *
     * @return the string
     */
    protected String buildColumnList() {
        return csv(nonKeyColumnNames.stream()
                                    .map(n -> n + "=?")
                                    .collect(Collectors.toList()));
    }

    /**
     * Builds the where clause for keys.
     *
     * @return the string
     * @throws EdmException the edm exception
     */
    protected String buildWhereClauseForKeys() throws EdmException {
        List<String> keyConditions = new ArrayList<>();
        for (String key : target.getKeyPropertyNames()) {
            EdmProperty keyProperty = (EdmProperty) target.getProperty(key);
            String keyColumnName = getSQLTableColumnNoAlias(target, keyProperty);
            keyConditions.add(String.format("%s=?", keyColumnName));
        }
        return join(keyConditions, " AND "); // this plays a role when we are dealing with composite IDs
    }
}
