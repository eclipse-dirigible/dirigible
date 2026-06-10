/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import org.eclipse.dirigible.components.api.db.DataStoreFacade;

/**
 * Untyped Hibernate-backed CRUD over named entities. Each call addresses an entity by its logical
 * name (matching {@link Entity#name() @Entity.name} on a registered client class) and exchanges
 * data as JSON — convenient for scripted callers and for endpoints that proxy arbitrary entity
 * names.
 * <p>
 * For typed CRUD over an {@link Entity @Entity}-annotated client class with compile-time field
 * checks, prefer {@code org.eclipse.dirigible.components.data.store.java.JavaEntityStore} (resolve
 * it through {@code BeanProvider.getBean(JavaEntityStore.class)} inside a controller method). The
 * two stores sit on the same Hibernate session — changes from one are immediately visible to the
 * other.
 */
public final class Store {

    private Store() {}

    public static Object save(String entityName, String json) {
        return DataStoreFacade.save(entityName, json);
    }

    public static void upsert(String entityName, String json) {
        DataStoreFacade.upsert(entityName, json);
    }

    public static void update(String entityName, String json) {
        DataStoreFacade.update(entityName, json);
    }

    public static String list(String entityName, String optionsJson) {
        return DataStoreFacade.list(entityName, optionsJson);
    }

    public static long count(String entityName, String optionsJson) {
        return DataStoreFacade.count(entityName, optionsJson);
    }

    public static String find(String entityName, String exampleJson, int limit, int offset) {
        return DataStoreFacade.find(entityName, exampleJson, limit, offset);
    }

    public static String get(String entityName, java.io.Serializable id) {
        return DataStoreFacade.get(entityName, id);
    }

    public static void deleteEntry(String entityName, java.io.Serializable id) {
        DataStoreFacade.deleteEntry(entityName, id);
    }

    public static String getEntityName(String name) {
        return DataStoreFacade.getEntityName(name);
    }

    public static String getTableName(String name) {
        return DataStoreFacade.getTableName(name);
    }

    public static String getIdName(String name) {
        return DataStoreFacade.getIdName(name);
    }

    public static String getIdColumn(String name) {
        return DataStoreFacade.getIdColumn(name);
    }
}
