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

import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatement;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.dirigible.engine.odata2.sql.builder.SQLUtils.csvInBrackets;
import static org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils.fqn;

/**
 * The Class SQLInsertBuilder.
 */
public class SQLInsertBuilder extends AbstractQueryBuilder {

    /** The target. */
    private EdmEntityType target;

    /** The column names. */
    private final List<String> columnNames = new ArrayList<>();

    /** The entry. */
    private ODataEntry entry;

    /** The table name. */
    private String tableName;

    /**
     * Instantiates a new SQL insert builder.
     *
     * @param tableMappingProvider the table mapping provider
     */
    public SQLInsertBuilder(final EdmTableBindingProvider tableMappingProvider) {
        super(tableMappingProvider);
    }

    /**
     * Builds the.
     *
     * @param context the context
     * @return the SQL statement
     */
    @Override
    public SQLStatement build(final SQLContext context) {
        return new SQLStatement() {

            @Override
            public String sql() throws ODataException {
                // TODO make immutable
                initializeQuery();
                StringBuilder builder = new StringBuilder();
                builder.append("INSERT ");
                builder.append(" INTO ");
                builder.append(buildInto(context));
                builder.append(" VALUES ");
                builder.append(buildValues(context));
                return SQLUtils.normalizeSQLExpression(builder.toString());
            }

            @Override
            public List<SQLStatementParam> getStatementParams() {
                return SQLInsertBuilder.this.getStatementParams();
            }

            @Override
            public boolean isEmpty() {
                return columnNames.isEmpty();
            }
        };
    }

    /**
     * Into.
     *
     * @param target the target
     * @param entry the entry
     * @return the SQL insert builder
     */
    public SQLInsertBuilder into(final EdmEntityType target, ODataEntry entry) {
        this.target = target;
        this.entry = entry;
        return this;
    }

    /**
     * Gets the target.
     *
     * @return the target
     */
    public EdmEntityType getTarget() {
        return target;
    }

    /**
     * Gets the entry.
     *
     * @return the entry
     */
    public ODataEntry getEntry() {
        return entry;
    }

    /**
     * Sets the table name.
     *
     * @param tableName the table name
     * @return the SQL insert builder
     */
    public SQLInsertBuilder setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    /**
     * Gets the target table name.
     *
     * @return the target table name
     */
    public String getTargetTableName() {
        Iterator<String> it = getTablesAliasesForEntitiesInQuery();
        while (it.hasNext()) {
            String tableAlias = it.next();
            EdmStructuralType target = getEntityInQueryForAlias(tableAlias);
            if (isInsertTarget(target)) {
                return getSQLTableName(target);
            }
        }

        throw new RuntimeException("Unknown odata table name");
    }

    /**
     * Initialize query.
     *
     * @throws ODataException the o data exception
     */
    protected void initializeQuery() throws ODataException {
        grantTableAliasForStructuralTypeInQuery(target);
        Map<String, Object> entryValues = entry.getProperties();

        for (EdmProperty property : EdmUtils.getProperties(target)) { // we iterate first the own properties of the type
            if (entryValues.containsKey(property.getName())) {
                String columnName = getSQLTableColumnNoAlias(target, property);
                columnNames.add(columnName);
                this.addStatementParam(target, property, entryValues.get(property.getName()));

            }
        }

        for (EdmNavigationProperty inlineEntry : EdmUtils.getNavigationProperties(target)) {
            if (entryValues.containsKey(inlineEntry.getName())) {
                Collection<EdmProperty> inlineEntityKeys = EdmUtils.getKeyProperties(inlineEntry);
                if (!inlineEntityKeys.isEmpty()) {
                    columnNames.addAll(getSQLJoinColumnNoAlias(target, inlineEntry));
                    for (EdmProperty inlineEntityKey : inlineEntityKeys) {
                        Object value = OData2Utils.getInlineEntryKeyValue(entryValues, inlineEntry, inlineEntityKey);
                        this.addStatementParam(inlineEntry, inlineEntityKey, value);
                    }
                } else {
                    // TODO if the entry does not have ids, create the entry.
                    throw new ODataException("Deep insert not implemented yet. Please create a request if you need it");
                }
            }
        }
    }

    /**
     * Builds the into.
     *
     * @param context the context
     * @return the string
     */
    protected String buildInto(final SQLContext context) {
        StringBuilder into = new StringBuilder();
        into.append(tableName != null ? tableName : getTargetTableName())
            .append(" ")
            .append(buildColumnList(context));
        return into.toString();
    }

    /**
     * Checks if is insert target.
     *
     * @param target the target
     * @return true, if is insert target
     */
    protected boolean isInsertTarget(final EdmStructuralType target) {
        // always select the entity target
        return fqn(getTarget()).equals(fqn(target));
    }

    /**
     * Builds the column list.
     *
     * @param context the context
     * @return the string
     */
    private String buildColumnList(final SQLContext context) {
        return csvInBrackets(columnNames);
    }

    /**
     * Builds the values.
     *
     * @param context the context
     * @return the string
     */
    private String buildValues(final SQLContext context) {
        return csvInBrackets(columnNames.stream()
                                        .map(n -> "?")
                                        .collect(Collectors.toList()));
    }

}
