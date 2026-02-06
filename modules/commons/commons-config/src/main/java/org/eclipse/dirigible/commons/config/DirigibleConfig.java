/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Enum DirigibleConfig.
 */
public enum DirigibleConfig {

    REGISTRY_EXTERNAL_FOLDER("DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER", null), //

    // an example for DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER=/a/b/mydir
    // if set to true - /a/b/mydir will be replicated to <repo_dir>/mydir
    // if set to false - /a/b/mydir will be replicated to <repo_dir>
    REGISTRY_EXTERNAL_FOLDER_AS_SUBFOLDER("DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER_AS_SUBFOLDER", Boolean.FALSE.toString()), //
    // folders separated by comma, example value: target,bin,node_modules
    REGISTRY_EXTERNAL_IGNORED_FOLDERS("DIRIGIBLE_REGISTRY_EXTERNAL_IGNORED_FOLDERS", null), //
    // folders separated by comma, example value: target,bin,node_modules
    REGISTRY_LOCAL_IGNORED_FOLDERS("DIRIGIBLE_REGISTRY_LOCAL_IGNORED_FOLDERS", null), //

    CSV_DATA_BATCH_SIZE("DIRIGIBLE_CSV_DATA_BATCH_SIZE", "1000"), //

    FLOWABLE_DATABASE_DRIVER("DIRIGIBLE_FLOWABLE_DATABASE_DRIVER", null), //
    FLOWABLE_DATABASE_URL("DIRIGIBLE_FLOWABLE_DATABASE_URL", null), //
    FLOWABLE_DATABASE_USER("DIRIGIBLE_FLOWABLE_DATABASE_USER", null), //
    FLOWABLE_DATABASE_PASSWORD("DIRIGIBLE_FLOWABLE_DATABASE_PASSWORD", null), //
    FLOWABLE_DATABASE_DATASOURCE_NAME("DIRIGIBLE_FLOWABLE_DATABASE_DATASOURCE_NAME", null), //
    FLOWABLE_DATABASE_SCHEMA_UPDATE("DIRIGIBLE_FLOWABLE_DATABASE_SCHEMA_UPDATE", Boolean.TRUE.toString()), //

    FLOWABLE_MAIL_SERVER_HOST("DIRIGIBLE_FLOWABLE_MAIL_SERVER_HOST", null), //
    FLOWABLE_MAIL_SERVER_PORT("DIRIGIBLE_FLOWABLE_MAIL_SERVER_PORT", "587"), //
    FLOWABLE_MAIL_SERVER_USERNAME("DIRIGIBLE_FLOWABLE_MAIL_SERVER_USERNAME", null), //
    FLOWABLE_MAIL_SERVER_PASSWORD("DIRIGIBLE_FLOWABLE_MAIL_SERVER_PASSWORD", null), //
    FLOWABLE_MAIL_SERVER_USE_TLS("DIRIGIBLE_FLOWABLE_MAIL_SERVER_USE_TLS", Boolean.TRUE.toString()), //
    FLOWABLE_MAIL_SERVER_USE_SSL("DIRIGIBLE_FLOWABLE_MAIL_SERVER_USE_SSL", Boolean.FALSE.toString()), //
    FLOWABLE_MAIL_SERVER_DEFAULT_FROM("DIRIGIBLE_FLOWABLE_MAIL_SERVER_DEFAULT_FROM", null), //

    EXEC_COMMAND_LOGGING_ENABLED("DIRIGIBLE_EXEC_COMMAND_LOGGING_ENABLED", Boolean.FALSE.toString()), //

    SYNCHRONIZER_CROSS_RETRY_COUNT("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_COUNT", "10"), //

    SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS", "10000"), //

    HOME_URL("DIRIGIBLE_HOME_URL", "services/web/shell-ide/"), //

    MAIL_USERNAME("DIRIGIBLE_MAIL_USERNAME", null), //

    MAIL_PASSWORD("DIRIGIBLE_MAIL_PASSWORD", null), //

    MAIL_TRANSPORT_PROTOCOL("DIRIGIBLE_MAIL_TRANSPORT_PROTOCOL", "smtps"), //

    MAIL_SMTP_HOST("DIRIGIBLE_MAIL_SMTP_HOST", null), //

    MAIL_SMTP_PORT("DIRIGIBLE_MAIL_SMTP_PORT", null), MAIL_SMTP_AUTH("DIRIGIBLE_MAIL_SMTP_AUTH", null), //

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

    /** The multi tenant mode cognito single user pool enabled. */
    MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED("DIRIGIBLE_MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL", Boolean.FALSE.toString()),

    /** The multi tenant mode keycloak single realm enabled. */
    MULTI_TENANT_MODE_KEYCLOAK_SINGLE_REALM_ENABLED("DIRIGIBLE_MULTI_TENANT_MODE_KEYCLOAK_SINGLE_REALM", Boolean.FALSE.toString()),

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
     * Gets the boolean value.
     *
     * @return the boolean value
     */
    public boolean getBooleanValue() {
        String configValue = getStringValue();
        return Boolean.valueOf(configValue);
    }

    public void setBooleanValue(boolean value) {
        setStringValue(Boolean.toString(value));
    }

    public void setStringValue(String value) {
        Configuration.set(getKey(), value);
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

    public void setIntValue(int value) {
        setStringValue(Integer.toString(value));
    }
}
