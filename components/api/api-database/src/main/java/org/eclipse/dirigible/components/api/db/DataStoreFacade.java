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
import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.store.DataStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
     * Query.
     *
     * @param name the name
     * @param limit
     * @param offset
     * @return the string
     */
    public static String query(String name, int limit, int offset) {
        List list = DataStoreFacade.get()
                                   .getDataStore()
                                   .query(name, limit, offset);
        return JsonHelper.toJson(list);
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
        return JsonHelper.toJson(list);
    }

    /**
     * Query native.
     *
     * @param name the name
     * @return the string
     */
    public static String queryNative(String name) {
        List list = DataStoreFacade.get()
                                   .getDataStore()
                                   .queryNative(name);
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

}
