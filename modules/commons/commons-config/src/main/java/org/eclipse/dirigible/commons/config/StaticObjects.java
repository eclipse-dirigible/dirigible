/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class StaticObjects.
 */
public final class StaticObjects {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(StaticObjects.class);

    /** The Constant DATASOURCE. */
    public static final String DATASOURCE = "DATASOURCE"; // DataSource

    /** The Constant SYSTEM_DATASOURCE. */
    public static final String SYSTEM_DATASOURCE = "SYSTEM_DATASOURCE"; // System DataSource

    /** The Constant DATABASE. */
    public static final String DATABASE = "DATABASE"; // IDatabase

    /** The Constant REPOSITORY. */
    public static final String REPOSITORY = "REPOSITORY"; // IRepository

    /** The Constant MASTER_REPOSITORY. */
    public static final String MASTER_REPOSITORY = "MASTER_REPOSITORY"; // IMasterRepository

    /** The Constant DATABASE_REPOSITORY. */
    public static final String DATABASE_REPOSITORY = "DATABASE_REPOSITORY"; // DatabaseRepository

    /** The Constant LOCAL_REPOSITORY. */
    public static final String LOCAL_REPOSITORY = "LOCAL_REPOSITORY"; // DatabaseRepository

    /** The Constant CMS_PROVIDER. */
    public static final String CMS_PROVIDER = "CMS_PROVIDER"; // ICmsProvider

    /** The Constant CMS_DATABASE_REPOSITORY. */
    public static final String CMS_DATABASE_REPOSITORY = "CMS_DATABASE_REPOSITORY"; // CmsDatabaseRepository

    /** The Constant JAVASCRIPT_ENGINE. */
    public static final String JAVASCRIPT_ENGINE = "JAVASCRIPT_ENGINE"; // IJavascriptEngineExecutor

    /** The Constant WEBSOCKET_HANDLER. */
    public static final String WEBSOCKET_HANDLER = "WEBSOCKET_HANDLER"; // WebsocketHandler

    /** The Constant BPM_PROVIDER. */
    public static final String BPM_PROVIDER = "BPM_PROVIDER"; // IBpmProvider



    /** The Constant OBJECTS. */
    private final static Map<String, Object> OBJECTS = Collections.synchronizedMap(new HashMap<String, Object>());

    /**
     * Gets the.
     *
     * @param key the key
     * @return the object
     */
    public static final Object get(String key) {
        if (logger.isTraceEnabled()) {
            logger.trace("Getting static object by key: " + key);
        }
        Object object = OBJECTS.get(key);
        if (object == null) {
            String message = "Static object by key: " + key + " is null";
            if (logger.isErrorEnabled()) {
                logger.error(message);
            }
            new Exception(message).printStackTrace();
        }
        return object;
    }

    /**
     * Sets the.
     *
     * @param key the key
     * @param object the object
     */
    public static final void set(String key, Object object) {
        if (logger.isInfoEnabled()) {
            logger.info("Setting static object by key: " + key);
        }
        OBJECTS.put(key, object);
    }

    /**
     * Exists.
     *
     * @param key the key
     * @return true, if successful
     */
    public static final boolean exists(String key) {
        if (logger.isTraceEnabled()) {
            logger.trace("Exists static object by key: " + key);
        }
        Object object = OBJECTS.get(key);
        if (object == null) {
            return false;
        }
        return true;
    }

}
