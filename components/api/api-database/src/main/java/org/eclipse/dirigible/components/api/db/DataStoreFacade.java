/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
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
     */
    public static void save(String name, String json) {
        DataStoreFacade.get()
                       .getDataStore()
                       .save(name, json);
    }

    /**
     * List.
     *
     * @param name the name
     * @return the string
     */
    public static String list(String name) {
        List list = DataStoreFacade.get()
                                   .getDataStore()
                                   .list(name);
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
