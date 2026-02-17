/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.db;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.store.DataStore;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata;
import org.eclipse.dirigible.components.data.store.model.EntityMetadata;
import org.eclipse.dirigible.components.data.store.parser.EntityParser;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;

/**
 * The Class DataStoreFacade.
 */
@Component
public class DataStoreFacade implements InitializingBean {

    /** The data sore facade. */
    private static DataStoreFacade INSTANCE;

    /** The data store. */
    private final DataStore dataStore;

    /**
     * Instantiates a new data store facade.
     *
     * @param dataStore the data store
     */
    @Autowired
    public DataStoreFacade(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the data store facade
     */
    public static DataStoreFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the data store.
     *
     * @return the data store
     */
    public DataStore getDataStore() {
        return dataStore;
    }

    /**
     * Save.
     *
     * @param name the name
     * @param json the json
     * @return the identifier
     */
    public static Object save(String name, String json) {
        return DataStoreFacade.get()
                              .getDataStore()
                              .save(name, json);
    }

    /**
     * Save or update.
     *
     * @param name the name
     * @param json the json
     */
    public static void upsert(String name, String json) {
        DataStoreFacade.get()
                       .getDataStore()
                       .upsert(name, json);
    }

    /**
     * Update.
     *
     * @param name the name
     * @param json the json
     */
    public static void update(String name, String json) {
        DataStoreFacade.get()
                       .getDataStore()
                       .update(name, json);
    }

    /**
     * List.
     *
     * @param name the name
     * @return the string
     */
    private static String listAll(String name) {
        List list = DataStoreFacade.get()
                                   .getDataStore()
                                   .list(name);
        convertBlob(name, list);
        return JsonHelper.toJson(list);
    }

    /**
     * List.
     *
     * @param name the name
     * @param options the options
     * @return the string
     */
    public static String list(String name, String options) {
        if (options != null) {
            List list = DataStoreFacade.get()
                                       .getDataStore()
                                       .list(name, options);
            convertBlob(name, list);
            return JsonHelper.toJson(list);
        } else {
            return listAll(name);
        }
    }

    /**
     * Count.
     *
     * @param name the name
     * @param options the options
     * @return the count
     */
    public static long count(String name, String options) {
        if (options != null) {
            long count = DataStoreFacade.get()
                                        .getDataStore()
                                        .count(name, options);
            return count;
        } else {
            return DataStoreFacade.get()
                                  .getDataStore()
                                  .count(name);
        }
    }

    /**
     * Find.
     *
     * @param name the name
     * @param example the example
     * @param limit
     * @param offset
     * @return the string
     */
    public static String find(String name, String example, int limit, int offset) {
        List list = DataStoreFacade.get()
                                   .getDataStore()
                                   .findByExample(name, example, limit, offset);
        convertBlob(name, list);
        return JsonHelper.toJson(list);
    }

    /**
     * Query with indexed parameters.
     *
     * @param query the entity query
     * @param parameters the query parameters
     * @param limit the limit
     * @param offset the offset
     * @return the string
     * @throws SQLException
     */
    public static String query(String query, String parameters, int limit, int offset) throws SQLException {
        Optional<JsonElement> parametersElement = DatabaseFacade.parseOptionalJson(parameters);
        List list = DataStoreFacade.get()
                                   .getDataStore()
                                   .query(query, parametersElement, limit, offset);
        return JsonHelper.toJson(list);
    }

    /**
     * Query with named parameters.
     *
     * @param query the entity query
     * @param parameters the query parameters
     * @param limit the limit
     * @param offset the offset
     * @return the string
     * @throws SQLException
     */
    public static String queryNamed(String query, String parameters, int limit, int offset) throws SQLException {
        Optional<JsonElement> parametersElement = DatabaseFacade.parseOptionalJson(parameters);
        List list = DataStoreFacade.get()
                                   .getDataStore()
                                   .queryNamed(query, parametersElement, limit, offset);
        return JsonHelper.toJson(list);
    }

    /**
     * Query native with indexed parameters.
     *
     * @param query the entity name
     * @param parameters the native query parameters
     * @param limit the limit
     * @param offset the offset
     * @return the string
     * @throws SQLException
     */
    public static String queryNative(String query, String parameters, int limit, int offset) throws SQLException {
        Optional<JsonElement> parametersElement = DatabaseFacade.parseOptionalJson(parameters);
        List list = DataStoreFacade.get()
                                   .getDataStore()
                                   .queryNative(query, parametersElement, limit, offset);
        return JsonHelper.toJson(list);
    }

    /**
     * Query native with named parameters.
     *
     * @param query the entity name
     * @param parameters the native query parameters
     * @param limit the limit
     * @param offset the offset
     * @return the string
     * @throws SQLException
     */
    public static String queryNativeNamed(String query, String parameters, int limit, int offset) throws SQLException {
        Optional<JsonElement> parametersElement = DatabaseFacade.parseOptionalJson(parameters);
        List list = DataStoreFacade.get()
                                   .getDataStore()
                                   .queryNativeNamed(query, parametersElement, limit, offset);
        return JsonHelper.toJson(list);
    }

    /**
     * Gets the.
     *
     * @param name the name
     * @param id the id
     * @return the string
     */
    public static String get(String name, Serializable id) {
        Map object = DataStoreFacade.get()
                                    .getDataStore()
                                    .get(name, id);
        convertBlob(name, object);
        return JsonHelper.toJson(object);
    }

    /**
     * Delete.
     *
     * @param name the name
     * @param id the id
     */
    public static void deleteEntry(String name, Serializable id) {
        DataStoreFacade.get()
                       .getDataStore()
                       .delete(name, id);
    }

    public static String getEntityName(String name) {
        EntityMetadata metadata = EntityParser.ENTITIES.get(name);
        if (metadata == null) {
            throw new RuntimeException(
                    "Entity: [" + name + "] metadata does not exist. Consider to publishing it and wait a while to get registered.");
        }
        return metadata.getEntityName();
    }

    public static String getTableName(String name) {
        EntityMetadata metadata = EntityParser.ENTITIES.get(name);
        if (metadata == null) {
            throw new RuntimeException(
                    "Entity: [" + name + "] metadata does not exist. Consider to publishing it and wait a while to get registered.");
        }
        return metadata.getTableName();
    }

    public static String getIdName(String name) {
        EntityMetadata metadata = EntityParser.ENTITIES.get(name);
        if (metadata == null) {
            throw new RuntimeException(
                    "Entity: [" + name + "] metadata does not exist. Consider to publishing it and wait a while to get registered.");
        }
        return metadata.getFields()
                       .stream()
                       .filter(f -> f.isIdentifier())
                       .collect(Collectors.toList())
                       .getFirst()
                       .getPropertyName();
    }

    public static String getIdColumn(String name) {
        EntityMetadata metadata = EntityParser.ENTITIES.get(name);
        if (metadata == null) {
            throw new RuntimeException(
                    "Entity: [" + name + "] metadata does not exist. Consider to publishing it and wait a while to get registered.");
        }
        return metadata.getFields()
                       .stream()
                       .filter(f -> f.isIdentifier())
                       .collect(Collectors.toList())
                       .getFirst()
                       .getColumnDetails()
                       .getColumnName();
    }

    private static void convertBlob(String entityName, List<Map> data) {
        EntityMetadata metadata = EntityParser.ENTITIES.get(entityName);

        if (metadata == null) {
            return;
        }

        for (EntityFieldMetadata field : metadata.getFields()) {
            if (field.getColumnDetails()
                     .getDatabaseType()
                     .toLowerCase()
                     .contains("blob")) {
                for (Map next : data) {
                    String propertyName = field.getPropertyName();
                    Object value = next.get(propertyName);
                    if (value instanceof java.sql.Blob) {
                        try {
                            java.sql.Blob blob = (java.sql.Blob) value;
                            byte[] bytes = blob.getBytes(1, (int) blob.length());
                            next.put(propertyName, bytes);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    private static void convertBlob(String entityName, Map data) {
        EntityMetadata metadata = EntityParser.ENTITIES.get(entityName);

        if (metadata == null) {
            return;
        }

        for (EntityFieldMetadata field : metadata.getFields()) {
            if (field.getColumnDetails()
                     .getDatabaseType()
                     .toLowerCase()
                     .contains("blob")) {
                String propertyName = field.getPropertyName();
                Object value = data.get(propertyName);
                if (value instanceof java.sql.Blob) {
                    try {
                        java.sql.Blob blob = (java.sql.Blob) value;
                        byte[] bytes = blob.getBytes(1, (int) blob.length());
                        data.put(propertyName, bytes);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
