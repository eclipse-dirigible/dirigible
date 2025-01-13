/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.binding;

import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.edm.provider.Mapping;

import java.util.*;

import static java.lang.String.format;

/**
 * The Class EdmTableBinding.
 */
public class EdmTableBinding extends Mapping {

    /**
     * The Enum DataStructureType.
     */
    public enum DataStructureType {

        /** The table. */
        TABLE("TABLE"),
        /** The view. */
        VIEW("VIEW"),
        /** The calc view. */
        CALC_VIEW("CALC VIEW");

        /** The value. */
        private final String value;

        /**
         * Instantiates a new data structure type.
         *
         * @param value the value
         */
        DataStructureType(String value) {
            this.value = value;
        }

        /**
         * Gets the type.
         *
         * @param value the value
         * @return the type
         */
        public static DataStructureType getType(String value) {
            for (DataStructureType type : DataStructureType.values()) {
                if (type.toString()
                        .equals(value)) {
                    return type;
                }
            }
            return DataStructureType.TABLE;
        }

        /**
         * To string.
         *
         * @return the string
         */
        public String toString() {
            return value;
        }
    }


    /** The Constant NO_PROPERTY_FOUND. */
    private static final String NO_PROPERTY_FOUND = "No sql binding configuration found in the mapping configuration for property %s."
            + " Did you map this property in the %s mapping?";

    /** The Constant PROPERTY_WRONG_CONFIGURATION. */
    private static final String PROPERTY_WRONG_CONFIGURATION =
            "Sql binding configuration in the mapping configuration for property %s is wrongly configured.";

    /** The Constant JOIN_COLUMN_UNSUPPORTED_CONFIGURATION. */
    private static final String JOIN_COLUMN_UNSUPPORTED_CONFIGURATION =
            PROPERTY_WRONG_CONFIGURATION + " The value %s is not of expected type List and String.";

    /** The binding data. */
    private final Map<String, Object> bindingData;

    /** The target fqn. */
    private final String targetFqn;

    /**
     * Instantiates a new edm table binding.
     *
     * @param bindingData the binding data
     */
    public EdmTableBinding(Map<String, Object> bindingData) {
        this.bindingData = bindingData;
        this.targetFqn = readEdmEntityFqn();
    }

    /**
     * Gets the edm fully qualifed name.
     *
     * @return the edm fully qualifed name
     */
    public String getEdmFullyQualifedName() {
        return targetFqn;
    }

    /**
     * Gets the table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return readMandatoryStringConfig("sqlTable");
    }

    public Optional<String> getSchemaName() {
        return readOptionalStringConfig("sqlSchema");
    }

    /**
     * Gets the mapping table name.
     *
     * @param target the target
     * @return the mapping table name
     * @throws EdmException the edm exception
     */
    public List<String> getMappingTableName(EdmStructuralType target) throws EdmException {
        return getRefProperties(target, "manyToManyMappingTable", "mappingTableName");
    }

    /**
     * Gets the mapping table join column.
     *
     * @param target the target
     * @return the mapping table join column
     * @throws EdmException the edm exception
     */
    public List<String> getMappingTableJoinColumn(EdmStructuralType target) throws EdmException {
        return getRefProperties(target, "manyToManyMappingTable", "mappingTableJoinColumn");
    }

    /**
     * Gets the join column to.
     *
     * @param target the target
     * @return the join column to
     * @throws EdmException the edm exception
     */
    public List<String> getJoinColumnTo(EdmStructuralType target) throws EdmException {
        return getRefProperties(target, "joinColumn", "");
    }

    /**
     * Gets the ref properties.
     *
     * @param target the target
     * @param property the property
     * @param secondaryProperty the secondary property
     * @return the ref properties
     * @throws EdmException the edm exception
     */
    @SuppressWarnings("unchecked")
    public List<String> getRefProperties(EdmStructuralType target, String property, String secondaryProperty) throws EdmException {
        String ref = "_ref_" + target.getName();
        Map<String, Object> refKeys = readMandatoryConfig(ref, Map.class);
        if (refKeys.containsKey(property)) {
            Object joinColumn = refKeys.get(property);
            if (joinColumn instanceof List) {
                return (List<String>) joinColumn;
            } else if (refKeys.get(property) instanceof String) {
                return Collections.singletonList(String.valueOf(refKeys.get(property)));
            } else if (refKeys.get(property) instanceof Map) {
                return Collections.singletonList(((Map<String, String>) refKeys.get(property)).get(secondaryProperty));
            } else {
                throw new IllegalArgumentException(format(format(JOIN_COLUMN_UNSUPPORTED_CONFIGURATION, ref, joinColumn)));
            }
        } else {
            throw new IllegalArgumentException(format(NO_PROPERTY_FOUND, ref + "->" + property, targetFqn));
        }
    }

    /**
     * Checks for mapping table.
     *
     * @param target the target
     * @return true, if successful
     * @throws EdmException the edm exception
     */
    public boolean hasMappingTable(EdmStructuralType target) throws EdmException {
        String ref = "_ref_" + target.getName();
        Map<String, Object> refKeys = readMandatoryConfig(ref, Map.class);
        return refKeys.containsKey("manyToManyMappingTable");
    }

    /**
     * Checks if is property mapped.
     *
     * @param p the p
     * @return true, if is property mapped
     */
    public boolean isPropertyMapped(EdmProperty p) {
        try {
            return this.isPropertyMapped(p.getName());
        } catch (EdmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Checks if is property mapped.
     *
     * @param propertyName the property name
     * @return true, if is property mapped
     */
    public boolean isPropertyMapped(String propertyName) {
        return bindingData.containsKey(propertyName);
    }

    /**
     * Checks if is aggregation type explicit.
     *
     * @return true, if is aggregation type explicit
     */
    public boolean isAggregationTypeExplicit() {
        String key = "aggregationType";
        if (isPropertyMapped(key)) {
            String aggregationType = readMandatoryStringConfig(key);
            return "explicit".equals(aggregationType);
        }

        return false;
    }

    /**
     * Checks if is column contained in aggregation prop.
     *
     * @param columnName the column name
     * @return true, if is column contained in aggregation prop
     */
    public boolean isColumnContainedInAggregationProp(String columnName) {
        String key = "aggregationProps";
        if (isPropertyMapped(key)) {
            Map<String, String> aggregationProps = readMandatoryConfig(key, Map.class);
            return aggregationProps.containsKey(columnName);
        }

        return false;
    }

    /**
     * Gets the column aggregation type.
     *
     * @param columnName the column name
     * @return the column aggregation type
     */
    public String getColumnAggregationType(String columnName) {
        Map<String, String> aggregationProps = readMandatoryConfig("aggregationProps", Map.class);
        return aggregationProps.get(columnName);
    }

    /**
     * Checks if is of type.
     *
     * @param <T> the generic type
     * @param key the key
     * @param clazz the clazz
     * @return true, if is of type
     */
    private <T> boolean isOfType(String key, Class<T> clazz) {
        if (bindingData.containsKey(key)) {
            Object property = bindingData.get(key);
            return clazz.isInstance(property);
        }

        throw new IllegalArgumentException(format(NO_PROPERTY_FOUND, key, targetFqn));
    }

    private String readMandatoryStringConfig(String key) {
        String value =
                readConfig(key, String.class).orElseThrow(() -> new IllegalArgumentException(format(NO_PROPERTY_FOUND, key, targetFqn)));
        return null == value ? null : value.trim();
    }

    /**
     * Read mandatory config.
     *
     * @param <T> the generic type
     * @param key the key
     * @param clazz the clazz
     * @return the t
     */
    private <T> T readMandatoryConfig(String key, Class<T> clazz) {
        return readConfig(key, clazz).orElseThrow(() -> new IllegalArgumentException(format(NO_PROPERTY_FOUND, key, targetFqn)));
    }

    private Optional<String> readOptionalStringConfig(String key) {
        if (bindingData.containsKey(key)) {
            Object property = bindingData.get(key);
            if (property instanceof String value) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    private <T> Optional<T> readConfig(String key, Class<T> clazz) {
        if (bindingData.containsKey(key)) {
            Object property = bindingData.get(key);
            if (clazz.isInstance(property)) {
                return Optional.of(clazz.cast(property));
            }
        }
        return Optional.empty();
    }

    /**
     * Checks for join column to.
     *
     * @param target the target
     * @return true, if successful
     * @throws EdmException the edm exception
     */
    public boolean hasJoinColumnTo(EdmStructuralType target) throws EdmException {
        if (target instanceof EdmEntityType || target instanceof EdmComplexType) {
            String jc = "_ref_" + target.getName();
            return bindingData.containsKey(jc);
        }
        return false;
    }

    /**
     * Gets the join column to.
     *
     * @param target the target
     * @return the join column to
     * @throws EdmException the edm exception
     */
    public List<String> getJoinColumnTo(EdmEntitySet target) throws EdmException {
        return getJoinColumnTo(target.getEntityType());
    }

    /**
     * Gets the column name.
     *
     * @param propertyName the property name
     * @return the column name
     */
    public String getColumnName(String propertyName) {
        if (bindingData.containsKey(propertyName)) {
            if (isOfType(propertyName, String.class)) {
                return String.valueOf(bindingData.get(propertyName));
            } else if (isOfType(propertyName, Map.class)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> value = (Map<String, Object>) bindingData.get(propertyName);
                return String.valueOf(value.get("name"));
            } else {
                throw new IllegalArgumentException(format(PROPERTY_WRONG_CONFIGURATION, propertyName));
            }

        } else {
            throw new IllegalArgumentException(format(NO_PROPERTY_FOUND, propertyName, targetFqn));
        }
    }

    /**
     * Gets the column name.
     *
     * @param property the property
     * @return the column name
     */
    public String getColumnName(EdmProperty property) {
        try {
            return getColumnName(property.getName());
        } catch (EdmException e) {
            throw new IllegalArgumentException(format(NO_PROPERTY_FOUND, property, targetFqn));
        }
    }

    /**
     * Read edm entity fqn.
     *
     * @return the string
     */
    protected String readEdmEntityFqn() {
        return readMandatoryStringConfig("edmTypeFqn");
    }

    /**
     * Gets the column info.
     *
     * @param propertyName the property name
     * @return the column info
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ColumnInfo getColumnInfo(String propertyName) {
        if (bindingData.containsKey(propertyName)) {
            if (isOfType(propertyName, String.class)) {
                return new ColumnInfo(String.valueOf(bindingData.get(propertyName)));
            } else if (isOfType(propertyName, Map.class)) {
                Map<String, Object> value = (Map) bindingData.get(propertyName);
                String name = String.valueOf(value.get("name"));
                String sqlType = String.valueOf(value.get("sqlType"));
                return new ColumnInfo(name, sqlType); // TODO read this from the database metadata and perform a
                // conversion there
            } else {
                throw new IllegalArgumentException(format(PROPERTY_WRONG_CONFIGURATION, propertyName));
            }
        } else {
            throw new IllegalArgumentException(format(NO_PROPERTY_FOUND, propertyName, targetFqn));
        }

    }

    /**
     * Gets the column info.
     *
     * @param property the property
     * @return the column info
     */
    public ColumnInfo getColumnInfo(EdmProperty property) {
        try {
            return getColumnInfo(property.getName());
        } catch (EdmException e) {
            throw new IllegalArgumentException(format(NO_PROPERTY_FOUND, property, targetFqn));
        }
    }

    /**
     * Gets the parameters.
     *
     * @return the parameters
     */
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        String key = "_parameters_";
        if (bindingData.containsKey(key)) {
            parameters = (List<String>) bindingData.get(key);
        }
        return parameters;
    }

    /**
     * Gets the data structure type.
     *
     * @return the data structure type
     */
    public DataStructureType getDataStructureType() {
        DataStructureType dataStructureType = DataStructureType.TABLE;
        String key = "dataStructureType";
        if (bindingData.containsKey(key)) {
            dataStructureType = DataStructureType.getType((String) bindingData.get(key));
        }
        return dataStructureType;
    }

    /**
     * Gets the primary key.
     *
     * @return the primary key
     */
    public String getPrimaryKey() {
        return readMandatoryStringConfig("_pk_");
    }

    /**
     * The Class ColumnInfo.
     */
    public static class ColumnInfo {

        /** The column name. */
        private final String columnName;

        /** The jdbc type. */
        private final String jdbcType;

        /**
         * Instantiates a new column info.
         *
         * @param columnName the column name
         * @param jdbcType the jdbc type
         */
        public ColumnInfo(final String columnName, final String jdbcType) {
            this.columnName = columnName;
            this.jdbcType = jdbcType;
        }

        /**
         * Instantiates a new column info.
         *
         * @param columnName the column name
         */
        public ColumnInfo(final String columnName) {
            this(columnName, null);
        }

        /**
         * Gets the column name.
         *
         * @return the column name
         */
        public String getColumnName() {
            return columnName;
        }

        /**
         * Gets the jdbc type.
         *
         * @return the jdbc type
         */
        public String getJdbcType() {
            return jdbcType;
        }
    }
}
