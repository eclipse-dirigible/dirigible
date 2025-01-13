/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.processor;

import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmStructuralType;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLProcessor;
import org.eclipse.dirigible.engine.odata2.sql.builder.EdmUtils;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder;
import org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * The Class ResultSetReader.
 */
public class ResultSetReader {

    /** The callback. */
    private final SQLProcessor callback;

    /**
     * Instantiates a new result set reader.
     *
     * @param callback the callback
     */
    public ResultSetReader(SQLProcessor callback) {
        this.callback = callback;
    }

    /**
     * Gets the entity data from result set.
     *
     * @param selectEntityQuery the select entity query
     * @param entityType the entity type
     * @param properties the properties
     * @param resultSet the result set
     * @return the entity data from result set
     * @throws SQLException the SQL exception
     * @throws ODataException the o data exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected Map<String, Object> getEntityDataFromResultSet(SQLSelectBuilder selectEntityQuery, final EdmStructuralType entityType,
            Collection<EdmProperty> properties, final ResultSet resultSet) throws SQLException, ODataException, IOException {
        Map<String, Object> result = new HashMap<>();
        for (EdmProperty property : properties) {
            result.put(property.getName(), readProperty(entityType, property, selectEntityQuery, resultSet));
        }
        return result;
    }

    /**
     * Gets the result set entity.
     *
     * @param selectEntityQuery the select entity query
     * @param entityType the entity type
     * @param properties the properties
     * @param resultSet the result set
     * @param hasGeneratedId the has generated id
     * @return the result set entity
     * @throws SQLException the SQL exception
     * @throws ODataException the o data exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected ResultSetEntity getResultSetEntity(SQLSelectBuilder selectEntityQuery, final EdmEntityType entityType,
            Collection<EdmProperty> properties, final ResultSet resultSet, boolean hasGeneratedId)
            throws SQLException, ODataException, IOException {
        Map<String, Object> data = new HashMap<>();
        for (EdmProperty property : properties) {
            data.put(property.getName(), readProperty(entityType, property, selectEntityQuery, resultSet));
        }

        if (hasGeneratedId) {
            return new ResultSetEntity(entityType, data, String.valueOf(resultSet.getInt("row_num")));
        }
        return new ResultSetEntity(entityType, data);
    }

    /**
     * Read property.
     *
     * @param entityType the entity type
     * @param property the property
     * @param selectEntityQuery the select entity query
     * @param resultSet the result set
     * @return the object
     * @throws SQLException the SQL exception
     * @throws ODataException the o data exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected Object readProperty(EdmStructuralType entityType, EdmProperty property, SQLSelectBuilder selectEntityQuery,
            ResultSet resultSet) throws SQLException, ODataException, IOException {
        Object propertyDbValue;
        if (property.isSimple()) {
            if (!selectEntityQuery.isTransientType(entityType, property)) {
                final String columnName = selectEntityQuery.getSQLTableColumnAlias(entityType, property);
                if ("Binary".equals(property.getType()
                                            .getName())) {
                    propertyDbValue = resultSet.getBytes(columnName);
                } else {
                    propertyDbValue = resultSet.getObject(columnName);
                }
                return callback.onCustomizePropertyValue(entityType, property, entityType, propertyDbValue);
            } else {
                return null;
            }
        } else {
            EdmStructuralType complexProperty = (EdmStructuralType) property.getType();
            Map<String, Object> complexPropertyData = new HashMap<>();
            for (String pn : complexProperty.getPropertyNames()) {
                EdmProperty prop = (EdmProperty) complexProperty.getProperty(pn);
                final String columnName = selectEntityQuery.getSQLTableColumnAlias(complexProperty, prop);
                propertyDbValue = resultSet.getObject(columnName);
                complexPropertyData.put(pn, callback.onCustomizePropertyValue(entityType, property, entityType, propertyDbValue));
            }
            return complexPropertyData;
        }
    }

    /**
     * Accumulate expanded entities.
     *
     * @param query the query
     * @param resultSet the result set
     * @param accumulator the accumulator
     * @param expandEntities the expand entities
     * @throws SQLException the SQL exception
     * @throws ODataException the o data exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void accumulateExpandedEntities(SQLSelectBuilder query, ResultSet resultSet, ExpandAccumulator accumulator,
            List<ArrayList<NavigationPropertySegment>> expandEntities) throws SQLException, ODataException, IOException {

        for (List<NavigationPropertySegment> expandContents : expandEntities) {
            List<ResultSetEntity> parents = new ArrayList<>();
            /*
             * The inner loop is for nested expands e.g. Owner/Address, where the parent has a higher index. If
             * the resultset contains an Address, the Owner would be empty. If the result set contains an owner,
             * it is added ot the accumulation
             */
            for (NavigationPropertySegment expandContent : expandContents) {
                EdmEntityType expandType = expandContent.getTargetEntitySet()
                                                        .getEntityType();
                Map<String, Object> expandData =
                        getEntityDataFromResultSet(query, expandType, EdmUtils.getProperties(expandType), resultSet);
                if (OData2Utils.isEmpty(expandType, expandData)) {
                    break;
                } else {
                    ResultSetEntity entity = new ResultSetEntity(expandType, expandData);
                    accumulator.addExpandEntity(entity, parents);
                    parents.add(entity);
                }
            }
        }
    }

    /**
     * The Class ExpandAccumulator.
     */
    public static class ExpandAccumulator {

        /** The entity. */
        private final ResultSetEntity entity;

        /** The expand data. */
        private final LinkedHashMap<String, List<ExpandAccumulator>> expandData;

        /**
         * Instantiates a new expand accumulator.
         *
         * @param type the type
         * @throws EdmException the edm exception
         */
        public ExpandAccumulator(EdmEntityType type) throws EdmException {
            this(new ResultSetEntity(type, new HashMap<>()));
        }

        /**
         * Instantiates a new expand accumulator.
         *
         * @param entity the entity
         */
        public ExpandAccumulator(ResultSetEntity entity) {
            this.entity = entity;
            this.expandData = new LinkedHashMap<>();
        }

        /**
         * Checks if is accumulator for.
         *
         * @param entity the entity
         * @return true, if is accumulator for
         */
        public boolean isAccumulatorFor(ResultSetEntity entity) {
            return this.entity.equals(entity);
        }

        /**
         * Gets the result set entity.
         *
         * @return the result set entity
         */
        public ResultSetEntity getResultSetEntity() {
            return entity;
        }

        /**
         * Adds the expand entity.
         *
         * @param entity the entity
         * @param parents the parents
         * @return true, if successful
         */
        public boolean addExpandEntity(ResultSetEntity entity, List<ResultSetEntity> parents) {
            if (parents.isEmpty()) {
                String fqn = OData2Utils.fqn(entity.entityType);
                expandData.computeIfAbsent(fqn, k -> new ArrayList<>());
                List<ExpandAccumulator> accumulators = expandData.get(fqn);
                ExpandAccumulator last = lastAccumulator(accumulators);
                if (last == null || !last.isAccumulatorFor(entity)) {
                    for (ExpandAccumulator accumulator : accumulators) {
                        if (accumulator.isAccumulatorFor(entity)) {
                            return false; // do not add more than once the same entity
                        }
                    }
                    accumulators.add(new ExpandAccumulator(entity));
                }
                return true;
            } else { // recursion on the parents
                ResultSetEntity firstParent = parents.get(0);
                ExpandAccumulator firstParentAccumulator = accumulatorFor(firstParent);
                ArrayList<ResultSetEntity> nextParents = new ArrayList<>(parents);
                nextParents.remove(0);// firstParent
                return firstParentAccumulator.addExpandEntity(entity, nextParents);
            }

        }

        /**
         * Last accumulator.
         *
         * @param acc the acc
         * @return the expand accumulator
         */
        ExpandAccumulator lastAccumulator(List<ExpandAccumulator> acc) {
            return (acc == null || acc.size() == 0) ? null : acc.get(acc.size() - 1);
        }

        /**
         * Accumulator for.
         *
         * @param firstParent the first parent
         * @return the expand accumulator
         */
        ExpandAccumulator accumulatorFor(ResultSetEntity firstParent) {
            String fqn = OData2Utils.fqn(firstParent.entityType);
            if (expandData.get(fqn) != null) {
                List<ExpandAccumulator> accumulators = expandData.get(fqn);
                for (ExpandAccumulator acc : accumulators) {
                    if (acc.isAccumulatorFor(firstParent)) {
                        return acc;
                    }
                }
            }
            throw new IllegalStateException("Unsupported expand case");
        }

        /**
         * Render for expand.
         *
         * @return the map
         */
        public Map<String, Object> renderForExpand() {
            return renderForExpand(this);
        }

        /**
         * Render for expand.
         *
         * @param input the input
         * @return the map
         */
        public Map<String, Object> renderForExpand(ExpandAccumulator input) {
            Map<String, Object> result = new HashMap<>(input.getResultSetEntity().data);
            for (String key : input.expandData.keySet()) {
                List<ExpandAccumulator> accumulators = input.expandData.get(key);
                List<Map<String, Object>> expandData = new ArrayList<>();
                for (ExpandAccumulator acc : accumulators) {
                    expandData.add(renderForExpand(acc));
                }
                result.put(key, expandData);
            }
            return result;
        }
    }


    /**
     * The Class ResultSetEntity.
     */
    static class ResultSetEntity {

        /** The data. */
        final Map<String, Object> data;

        /** The keys. */
        final Map<String, Object> keys;

        /** The entity type. */
        final EdmEntityType entityType;

        /**
         * Instantiates a new result set entity.
         *
         * @param type the type
         * @param data the data
         * @throws EdmException the edm exception
         */
        public ResultSetEntity(EdmEntityType type, Map<String, Object> data) throws EdmException {
            this.entityType = type;
            this.data = data;
            this.keys = new HashMap<>();
            Collection<EdmProperty> keyProperties = type.getKeyProperties();
            for (EdmProperty p : keyProperties) {
                String name = p.getName();
                keys.put(name, data.get(name));
            }
        }

        /**
         * Instantiates a new result set entity.
         *
         * @param type the type
         * @param data the data
         * @param key the key
         * @throws EdmException the edm exception
         */
        public ResultSetEntity(EdmEntityType type, Map<String, Object> data, String key) throws EdmException {
            this.entityType = type;
            this.data = data;
            this.keys = new HashMap<>();
            keys.put("Id", key);
        }

        /**
         * Checks if is empty.
         *
         * @return true, if is empty
         * @throws ODataException the o data exception
         */
        public boolean isEmpty() throws ODataException {
            return OData2Utils.isEmpty(entityType, data);
        }

        /**
         * Equals.
         *
         * @param o the o
         * @return true, if successful
         */
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ResultSetEntity that = (ResultSetEntity) o;
            return keys.equals(that.keys);
        }

        /**
         * Hash code.
         *
         * @return the int
         */
        @Override
        public int hashCode() {
            return Objects.hash(keys);
        }

        @Override
        public String toString() {
            return "ResultSetEntity{" + "data=" + data + ", keys=" + keys + ", entityType=" + entityType + '}';
        }
    }

}
