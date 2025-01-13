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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The Enum DirigibleConfig.
 */
public enum DirigibleConfig {

    SNOWFLAKE_DATA_SOURCE_LIFESPAN_SECONDS("DIRIGIBLE_SNOWFLAKE_DATA_SOURCE_LIFESPAN_SECONDS", "540"), // 9 minutes

    LEAKED_CONNECTIONS_MAX_IN_USE_SECONDS("DIRIGIBLE_LEAKED_CONNECTIONS_MAX_IN_USE_SECONDS", "180"), // 3 min by default

    LEAKED_CONNECTIONS_CHECK_INTERVAL_SECONDS("DIRIGIBLE_LEAKED_CONNECTIONS_CHECK_INTERVAL_SECONDS", "30"),

    TENANTS_PROVISIONING_FREQUENCY_SECONDS("DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS", "900"), // 15 minutes

    /** The cms internal root folder. */
    CMS_INTERNAL_ROOT_FOLDER("DIRIGIBLE_CMS_INTERNAL_ROOT_FOLDER", "target/dirigible/cms"),

    /** The default data source name. */
    DEFAULT_DATA_SOURCE_NAME("DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT", "DefaultDB"),

    /** The system data source name. */
    SYSTEM_DATA_SOURCE_NAME("DIRIGIBLE_DATABASE_DATASOURCE_NAME_SYSTEM", "SystemDB"),

    /** The synchronizer frequency. */
    SYNCHRONIZER_FREQUENCY("DIRIGIBLE_SYNCHRONIZER_FREQUENCY", "10"),

    /** The trial enabled. */
    TRIAL_ENABLED("DIRIGIBLE_TRIAL_ENABLED", Boolean.FALSE.toString()),

    /** The repository local root folder. */
    REPOSITORY_LOCAL_ROOT_FOLDER("DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER", "target"),

    /** The multi tenant mode enabled. */
    MULTI_TENANT_MODE_ENABLED("DIRIGIBLE_MULTI_TENANT_MODE", Boolean.FALSE.toString()),

    /** The tenant subdomain regex. */
    TENANT_SUBDOMAIN_REGEX("DIRIGIBLE_TENANT_SUBDOMAIN_REGEX", "^([^\\.]+)\\..+$"),

    SNOWFLAKE_ADMIN_USERNAME("DIRIGIBLE_SNOWFLAKE_ADMIN_USERNAME", null),

    /** The basic admin username. */
    BASIC_ADMIN_USERNAME("DIRIGIBLE_BASIC_USERNAME", toBase64("admin")),

    /** The basic admin pass. */
    BASIC_ADMIN_PASS("DIRIGIBLE_BASIC_PASSWORD", toBase64("admin"));

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleConfig.class);

    /** The key. */
    private final String key;

    /** The default value. */
    private final String defaultValue;

    /**
     * Instantiates a new dirigible config.
     *
     * @param key the key
     * @param defaultValue the default value
     */
    DirigibleConfig(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Gets the from base 64 value.
     *
     * @return the from base 64 value
     */
    public String getFromBase64Value() {
        String val = getStringValue();
        return fromBase64(val);
    }

    /**
     * Gets the string value.
     *
     * @return the string value
     */
    public String getStringValue() {
        return Configuration.get(key, defaultValue);
    }

    /**
     * From base 64.
     *
     * @param string the string
     * @return the string
     */
    private static String fromBase64(String string) {
        return new String(Base64.getDecoder()
                                .decode(string),
                StandardCharsets.UTF_8);
    }

    /**
     * To base 64.
     *
     * @param string the string
     * @return the string
     */
    private static String toBase64(String string) {
        return Base64.getEncoder()
                     .encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gets the key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the boolean value.
     *
     * @return the boolean value
     */
    public boolean getBooleanValue() {
        String configValue = getStringValue();
        return Boolean.valueOf(configValue);
    }

    /**
     * Gets the int value.
     *
     * @return the int value
     */
    public int getIntValue() {
        String stringValue = getStringValue();
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ex) {
            LOGGER.warn("Configuration with key [{}] has invalid non integer value: {}. Returning the defalt value [{}]", key, stringValue,
                    defaultValue, ex);
        }
        return Integer.parseInt(defaultValue);
    }

}
