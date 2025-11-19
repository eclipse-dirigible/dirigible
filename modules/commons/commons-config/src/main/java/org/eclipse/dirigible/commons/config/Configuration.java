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

import static java.text.MessageFormat.format;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration Facade class keeps all the configurations in the Dirigible instance It has the
 * default built in properties file - dirigible.properties After the initialization, all the default
 * properties are replaced with the ones coming as: 1. System's properties 2. Environment variables
 * This can be triggered programmatically with update() method It supports also loading of custom
 * properties files from the class loader with load() for the modules and also merge with a provided
 * properties object with add() methods
 */
public class Configuration {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
    /** The Constant MULTIVARIABLE_REGEX. */
    private static final String MULTIVARIABLE_REGEX = "\\$\\{([^}]+)\\}";
    /** The Constant MULTIVARIABLE_PATTERN. */
    private static final Pattern MULTIVARIABLE_PATTERN = Pattern.compile(MULTIVARIABLE_REGEX);
    /** The Constant RUNTIME_VARIABLES. */
    private static final Map<String, String> RUNTIME_VARIABLES = Collections.synchronizedMap(new HashMap<>());
    /** The Constant ENVIRONMENT_VARIABLES. */
    private static final Map<String, String> ENVIRONMENT_VARIABLES = Collections.synchronizedMap(new HashMap<>());
    /** The Constant DEPLOYMENT_VARIABLES. */
    private static final Map<String, String> DEPLOYMENT_VARIABLES = Collections.synchronizedMap(new HashMap<>());
    /** The Constant MODULE_VARIABLES. */
    private static final Map<String, String> MODULE_VARIABLES = Collections.synchronizedMap(new HashMap<>());
    /** The Constant CONFIG_FILE_PATH_DIRIGIBLE_COMMON_PROPERTIES. */
    private static final String CONFIG_FILE_PATH_DIRIGIBLE_COMMON_PROPERTIES = "/dirigible-commons.properties";
    private static final String CONFIG_FILE_PATH_DIRIGIBLE_PROPERTIES_OVERRIDES = "/dirigible.properties";
    /** The Constant ERROR_MESSAGE_CONFIGURATION_DOES_NOT_EXIST. */
    private static final String ERROR_MESSAGE_CONFIGURATION_DOES_NOT_EXIST = "Configuration file {0} does not exist";
    /** The Constant CONFIGURATION_PARAMETERS. */
    private static final String[] CONFIGURATION_PARAMETERS = getConfigParams();
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
    /** The loaded. */
    public static boolean LOADED = false;

    static {
        reloadConfigurations();
    }


    /**
     * The Enum ConfigType.
     */
    private enum ConfigType {

        /** The runtime. */
        RUNTIME,
        /** The environment. */
        ENVIRONMENT,
        /** The deployment. */
        DEPLOYMENT,
        /** The module. */
        MODULE
    }

    public static void reloadConfigurations() {
        RUNTIME_VARIABLES.clear();
        ENVIRONMENT_VARIABLES.clear();
        DEPLOYMENT_VARIABLES.clear();
        MODULE_VARIABLES.clear();

        loadDeploymentConfig(CONFIG_FILE_PATH_DIRIGIBLE_COMMON_PROPERTIES, CONFIG_FILE_PATH_DIRIGIBLE_PROPERTIES_OVERRIDES);
        loadEnvironmentConfig();
        LOADED = true;
    }

    /**
     * Load deployment config.
     *
     * @param path the path
     */
    private static void loadDeploymentConfig(String path, String overridePath) {
        load(path, ConfigType.DEPLOYMENT);
        overrideConfig(overridePath);
    }

    private static void overrideConfig(String overridePath) {
        try (InputStream inputStream = Configuration.class.getResourceAsStream(overridePath)) {
            if (null == inputStream) {
                LOGGER.info("Override file with path [{}] was not found.", overridePath);
            } else {
                LOGGER.info("Found override file with path [{}]. Will use it to override the configs.", overridePath);
                load(overridePath, ConfigType.DEPLOYMENT);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load file with path " + overridePath, e);
        }
    }

    /**
     * Load.
     *
     * @param path the path
     * @param type the type
     */
    private static void load(String path, ConfigType type) {
        try {
            Properties custom = new Properties();
            try (InputStream in = Configuration.class.getResourceAsStream(path)) {
                if (in != null) {
                    custom.load(in);
                    switch (type) {
                        case RUNTIME:
                            addConfigProperties(custom, RUNTIME_VARIABLES);
                            break;
                        case ENVIRONMENT:
                            addConfigProperties(custom, ENVIRONMENT_VARIABLES);
                            break;
                        case DEPLOYMENT:
                            addConfigProperties(custom, DEPLOYMENT_VARIABLES);
                            break;
                        case MODULE:
                            addConfigProperties(custom, MODULE_VARIABLES);
                            break;
                        default:
                            break;
                    }
                    logger.debug("Configuration loaded from: [{}]", path);
                } else if (!path.equals(CONFIG_FILE_PATH_DIRIGIBLE_COMMON_PROPERTIES)) {
                    throw new IOException(format(ERROR_MESSAGE_CONFIGURATION_DOES_NOT_EXIST, path));
                } else {
                    logger.debug("Configuration file [{}] does not exist", path);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load from file with path [{}]", path, e);
        }
    }

    /**
     * Adds the config properties.
     *
     * @param properties the properties
     * @param map the map
     */
    private static void addConfigProperties(Properties properties, Map<String, String> map) {
        for (String property : properties.stringPropertyNames()) {
            map.put(property, properties.getProperty(property));
        }
    }

    /**
     * Load environment config.
     */
    private static void loadEnvironmentConfig() {
        addConfigProperties(System.getenv(), ConfigType.ENVIRONMENT);
        addConfigProperties(System.getProperties(), ConfigType.ENVIRONMENT);
    }

    /**
     * Adds the config properties.
     *
     * @param properties the properties
     * @param type the type
     */
    private static void addConfigProperties(Map<String, String> properties, ConfigType type) {
        switch (type) {
            case RUNTIME:
                RUNTIME_VARIABLES.putAll(properties);
                break;
            case ENVIRONMENT:
                ENVIRONMENT_VARIABLES.putAll(properties);
                break;
            case DEPLOYMENT:
                DEPLOYMENT_VARIABLES.putAll(properties);
                break;
            case MODULE:
                MODULE_VARIABLES.putAll(properties);
                break;
            default:
                break;

        }
    }

    /**
     * Adds the config properties.
     *
     * @param properties the properties
     * @param type the type
     */
    private static void addConfigProperties(Properties properties, ConfigType type) {
        switch (type) {
            case RUNTIME:
                addConfigProperties(properties, RUNTIME_VARIABLES);
                break;
            case ENVIRONMENT:
                addConfigProperties(properties, ENVIRONMENT_VARIABLES);
                break;
            case DEPLOYMENT:
                addConfigProperties(properties, DEPLOYMENT_VARIABLES);
                break;
            case MODULE:
                addConfigProperties(properties, MODULE_VARIABLES);
                break;
            default:
                break;
        }
    }

    private static String[] getConfigParams() {
        List<String> staticParams = List.of( //
                "DIRIGIBLE_ANONYMOUS_USER_NAME_PROPERTY_NAME", //
                "DIRIGIBLE_BRANDING_NAME", //
                "DIRIGIBLE_BRANDING_BRAND", //
                "DIRIGIBLE_BRANDING_FAVICON", //
                "DIRIGIBLE_BRANDING_THEME", //
                "DIRIGIBLE_BRANDING_PREFIX", //
                "DIRIGIBLE_BRANDING_ANALYTICS", //
                "DIRIGIBLE_GIT_ROOT_FOLDER", //
                "DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER", //
                "DIRIGIBLE_REGISTRY_IMPORT_WORKSPACE", //
                "DIRIGIBLE_REPOSITORY_PROVIDER", //
                "DIRIGIBLE_REPOSITORY_DATABASE_DATASOURCE_NAME", //
                "DIRIGIBLE_MASTER_REPOSITORY_PROVIDER", //
                "DIRIGIBLE_MASTER_REPOSITORY_ZIP_LOCATION", //
                "DIRIGIBLE_MASTER_REPOSITORY_JAR_PATH", //
                "DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER", //
                "DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER_IS_ABSOLUTE", //
                "DIRIGIBLE_REPOSITORY_SEARCH_INDEX_LOCATION", //
                "DIRIGIBLE_REPOSITORY_VERSIONING_ENABLED", //
                "DIRIGIBLE_DATABASE_PROVIDER", //
                "DIRIGIBLE_DATABASE_DEFAULT_SET_AUTO_COMMIT", //
                "DIRIGIBLE_DATABASE_DEFAULT_MAX_CONNECTIONS_COUNT", //
                "DIRIGIBLE_DATABASE_DEFAULT_WAIT_TIMEOUT", //
                "DIRIGIBLE_DATABASE_DEFAULT_WAIT_COUNT", //
                "DIRIGIBLE_DATABASE_CUSTOM_DATASOURCES", //
                "DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT", //
                "DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER_DEFAULT", //
                "DIRIGIBLE_DATABASE_H2_ROOT_FOLDER_DEFAULT", //
                "DIRIGIBLE_DATABASE_H2_DRIVER", //
                "DIRIGIBLE_DATABASE_H2_URL", //
                "DIRIGIBLE_DATABASE_H2_USERNAME", //
                "DIRIGIBLE_DATABASE_H2_PASSWORD", //
                "DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE", //
                "DIRIGIBLE_PERSISTENCE_CREATE_TABLE_ON_USE", //
                "DIRIGIBLE_MONGODB_CLIENT_URI", //
                "DIRIGIBLE_MONGODB_DATABASE_DEFAULT", //
                "DIRIGIBLE_SCHEDULER_MEMORY_STORE", //
                "DIRIGIBLE_SCHEDULER_DATASOURCE_TYPE", //
                "DIRIGIBLE_SCHEDULER_DATASOURCE_NAME", //
                "DIRIGIBLE_SCHEDULER_DATABASE_DELEGATE", //
                "DIRIGIBLE_SCHEDULER_LOGS_RETANTION_PERIOD", //
                "DIRIGIBLE_SCHEDULER_EMAIL_SENDER", //
                "DIRIGIBLE_SCHEDULER_EMAIL_RECIPIENTS", //
                "DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ERROR", //
                "DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_NORMAL", //
                "DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ERROR", //
                "DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_NORMAL", //
                "DIRIGIBLE_SCHEDULER_EMAIL_URL_SCHEME", //
                "DIRIGIBLE_SCHEDULER_EMAIL_URL_HOST", //
                "DIRIGIBLE_SCHEDULER_EMAIL_URL_PORT", //
                "DIRIGIBLE_SYNCHRONIZER_IGNORE_DEPENDENCIES", //
                "DIRIGIBLE_SYNCHRONIZER_EXCLUDE_PATHS", //
                "DIRIGIBLE_HOME_URL", //
                "DIRIGIBLE_JOB_EXPRESSION_BPM", //
                "DIRIGIBLE_JOB_EXPRESSION_DATA_STRUCTURES", //
                "DIRIGIBLE_JOB_EXPRESSION_EXTENSIONS", //
                "DIRIGIBLE_JOB_EXPRESSION_JOBS", //
                "DIRIGIBLE_JOB_EXPRESSION_MESSAGING", //
                "DIRIGIBLE_JOB_EXPRESSION_MIGRATIONS", //
                "DIRIGIBLE_JOB_EXPRESSION_ODATA", //
                "DIRIGIBLE_JOB_EXPRESSION_PUBLISHER", //
                "DIRIGIBLE_JOB_EXPRESSION_SECURITY", //
                "DIRIGIBLE_JOB_EXPRESSION_REGISTRY", //
                "DIRIGIBLE_JOB_DEFAULT_TIMEOUT", //
                "DIRIGIBLE_CMS_PROVIDER", //
                "DIRIGIBLE_CMS_ROLES_ENABLED", //
                "DIRIGIBLE_CMS_INTERNAL_ROOT_FOLDER", //
                "DIRIGIBLE_CMS_INTERNAL_ROOT_FOLDER_IS_ABSOLUTE", //
                "DIRIGIBLE_CMS_INTERNAL_VERSIONING_ENABLED", //
                "DIRIGIBLE_CMS_MANAGED_CONFIGURATION_JNDI_NAME", //
                "DIRIGIBLE_CMS_MANAGED_CONFIGURATION_AUTH_METHOD", //
                "DIRIGIBLE_CMS_MANAGED_CONFIGURATION_NAME", //
                "DIRIGIBLE_CMS_MANAGED_CONFIGURATION_KEY", //
                "DIRIGIBLE_CMS_MANAGED_CONFIGURATION_DESTINATION", //
                "DIRIGIBLE_CONNECTIVITY_CONFIGURATION_JNDI_NAME", //
                "DIRIGIBLE_CMS_DATABASE_DATASOURCE_TYPE", //
                "DIRIGIBLE_CMS_DATABASE_DATASOURCE_NAME", //
                "DIRIGIBLE_BPM_PROVIDER", //
                "DIRIGIBLE_FLOWABLE_DATABASE_DRIVER", //
                "DIRIGIBLE_FLOWABLE_DATABASE_URL", //
                "DIRIGIBLE_FLOWABLE_DATABASE_USER", //
                "DIRIGIBLE_FLOWABLE_DATABASE_PASSWORD", //
                "DIRIGIBLE_FLOWABLE_DATABASE_DATASOURCE_NAME", //
                "DIRIGIBLE_FLOWABLE_DATABASE_SCHEMA_UPDATE", //
                "DIRIGIBLE_FLOWABLE_USE_SYSTEM_DATASOURCE", //
                "DIRIGIBLE_MESSAGING_USE_DEFAULT_DATABASE", //
                "DIRIGIBLE_KAFKA_BOOTSTRAP_SERVER", //
                "DIRIGIBLE_KAFKA_ACKS", //
                "DIRIGIBLE_KAFKA_KEY_SERIALIZER", //
                "DIRIGIBLE_KAFKA_VALUE_SERIALIZER", //
                "DIRIGIBLE_KAFKA_AUTOCOMMIT_ENABLED", //
                "DIRIGIBLE_KAFKA_AUTOCOMMIT_INTERVAL", //
                "DIRIGIBLE_JAVASCRIPT_ENGINE_TYPE_DEFAULT", //
                "DIRIGIBLE_JAVASCRIPT_GRAALVM_DEBUGGER_PORT", //
                "DIRIGIBLE_JAVASCRIPT_GRAALVM_ALLOW_HOST_ACCESS", //
                "DIRIGIBLE_JAVASCRIPT_GRAALVM_ALLOW_CREATE_THREAD", //
                "DIRIGIBLE_JAVASCRIPT_GRAALVM_ALLOW_CREATE_PROCESS", //
                "DIRIGIBLE_JAVASCRIPT_GRAALVM_ALLOW_IO", //
                "DIRIGIBLE_JAVASCRIPT_GRAALVM_COMPATIBILITY_MODE_NASHORN", //
                "DIRIGIBLE_JAVASCRIPT_GRAALVM_COMPATIBILITY_MODE_MOZILLA", //
                "DIRIGIBLE_OPERATIONS_LOGS_ROOT_FOLDER_DEFAULT", //
                "DIRIGIBLE_THEME_DEFAULT", //
                "DIRIGIBLE_GENERATE_PRETTY_NAMES", //
                "DIRIGIBLE_OAUTH_CUSTOM_CLIENTS", //
                "DIRIGIBLE_OAUTH_ENABLED", //
                "DIRIGIBLE_OAUTH_AUTHORIZE_UR", //
                "DIRIGIBLE_OAUTH_TOKEN_URL", //
                "DIRIGIBLE_OAUTH_CLIENT_ID", //
                "DIRIGIBLE_OAUTH_CLIENT_SECRET", //
                "DIRIGIBLE_OAUTH_VERIFICATION_KEY", //
                "DIRIGIBLE_OAUTH_APPLICATION_NAME", //
                "DIRIGIBLE_OAUTH_APPLICATION_HOST", //
                "DIRIGIBLE_OAUTH_ISSUER", //
                "DIRIGIBLE_OAUTH_AUTHORIZE_URL", //
                "DIRIGIBLE_OAUTH_TOKEN_REQUEST_METHOD", //
                "DIRIGIBLE_OAUTH_VERIFICATION_KEY", //
                "DIRIGIBLE_OAUTH_VERIFICATION_KEY_EXPONENT", //
                "DIRIGIBLE_OAUTH_CHECK_ISSUER_ENABLED", //
                "DIRIGIBLE_OAUTH_CHECK_AUDIENCE_ENABLED", //
                "DIRIGIBLE_OAUTH_APPLICATION_NAME", //
                "DIRIGIBLE_PRODUCT_NAME", //
                "DIRIGIBLE_PRODUCT_VERSION", //
                "DIRIGIBLE_PRODUCT_REPOSITORY", //
                "DIRIGIBLE_PRODUCT_COMMIT_ID", //
                "DIRIGIBLE_PRODUCT_TYPE", //
                "DIRIGIBLE_INSTANCE_NAME", //
                "DIRIGIBLE_SPARK_CLIENT_URI", //
                "DIRIGIBLE_TERMINAL_ENABLED", //
                "DIRIGIBLE_MAIL_CONFIG_PROVIDER", //
                "DIRIGIBLE_MAIL_SMTPS_HOST", //
                "DIRIGIBLE_MAIL_SMTPS_PORT", //
                "DIRIGIBLE_MAIL_SMTPS_AUTH", //
                "DIRIGIBLE_MAIL_SMTP_AUTH", //
                "DIRIGIBLE_KEYCLOAK_AUTH_SERVER_URL", //
                "DIRIGIBLE_KEYCLOAK_CLIENT_ID", //
                "DIRIGIBLE_CSV_DATA_MAX_COMPARE_SIZE", //
                "DIRIGIBLE_CSV_DATA_BATCH_SIZE", //
                "DIRIGIBLE_DESTINATION_CLIENT_ID", //
                "DIRIGIBLE_DESTINATION_CLIENT_SECRET", //
                "DIRIGIBLE_DESTINATION_URL", //
                "DIRIGIBLE_DESTINATION_URI", //
                "DIRIGIBLE_BASIC_ENABLED", //
                "DIRIGIBLE_FTP_USERNAME", //
                "DIRIGIBLE_FTP_PASSWORD", //
                "DIRIGIBLE_FTP_PORT", //
                "DIRIGIBLE_SFTP_USERNAME", //
                "DIRIGIBLE_SFTP_PASSWORD", //
                "DIRIGIBLE_SFTP_PORT", //
                "SERVER_MAXHTTPHEADERSIZE", //
                "DIRIGIBLE_PUBLISH_DISABLED", //
                "AWS_DEFAULT_REGION", //
                "AWS_ACCESS_KEY_ID", //
                "AWS_SECRET_ACCESS_KEY", //
                "DIRIGIBLE_S3_PROVIDER", //
                "DIRIGIBLE_S3_BUCKET", //
                "DIRIGIBLE_DATABASE_SYSTEM_DRIVER", //
                "DIRIGIBLE_DATABASE_SYSTEM_URL", //
                "DIRIGIBLE_DATABASE_SYSTEM_USERNAME", //
                "DIRIGIBLE_DATABASE_SYSTEM_PASSWORD", //
                "DIRIGIBLE_DATABASE_SYSTEM_DIALECT", //
                "DIRIGIBLE_DATABASE_SYSTEM_DDL_AUTO", //
                "SNOWFLAKE_DEFAULT_TABLE_TYPE", //
                "DIRIGIBLE_PROJECT_TYPESCRIPT", //
                "DIRIGIBLE_MULTI_TENANT_MODE", //
                "DIRIGIBLE_MULTI_TENANT_MODE_SINGLE_USER_POOL", //
                "DIRIGIBLE_TENANT_SUBDOMAIN_REGEX", //
                "DIRIGIBLE_TRACING_TASK_ENABLED");
        Set<String> configs = Arrays.stream(DirigibleConfig.values())
                                    .map(DirigibleConfig::getKey)
                                    .collect(Collectors.toSet());

        Set<String> allCfgParams = new HashSet<>(staticParams);
        allCfgParams.addAll(configs);
        return allCfgParams.toArray(new String[0]);
    }

    /**
     * Load module config.
     *
     * @param path the path
     */
    public static void loadModuleConfig(String path) {
        load(path, ConfigType.MODULE);
    }

    /**
     * Gets the as int.
     *
     * @param key the key
     * @return the as int
     */
    public static int getAsInt(String key) {
        return getAsInt(key, 0);
    }

    /**
     * Gets the as int.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the as int
     */
    public static int getAsInt(String key, int defaultValue) {
        String stringValue = get(key, defaultValue + "");
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException nfe) {
            logger.error("The configuration key: {} points to non integer value: {}", key, stringValue, nfe);
        }
        return defaultValue;
    }

    /**
     * Getter for the value of the property by its key.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the string
     */
    public static String get(String key, String defaultValue) {
        String value = null;
        if (RUNTIME_VARIABLES.containsKey(key)) {
            value = RUNTIME_VARIABLES.get(key);
        } else if (ENVIRONMENT_VARIABLES.containsKey(key)) {
            value = ENVIRONMENT_VARIABLES.get(key);
        } else if (DEPLOYMENT_VARIABLES.containsKey(key)) {
            value = DEPLOYMENT_VARIABLES.get(key);
        } else if (MODULE_VARIABLES.containsKey(key)) {
            value = MODULE_VARIABLES.get(key);
        }
        return (value != null) ? value : defaultValue;
    }

    /**
     * Setter for the property's key and value. Sets the new value, only if the key value is null.
     *
     * @param key the key
     * @param value the value
     */
    public static void setIfNull(String key, String value) {
        if (get(key) == null) {
            set(key, value);
        }
    }

    /**
     * Getter for the value of the property by its key.
     *
     * @param key the key
     * @return the string
     */
    public static String get(String key) {
        return get(key, null);
    }

    /**
     * Setter for the property's key and value.
     *
     * @param key the key
     * @param value the value
     */
    public static void set(String key, String value) {
        RUNTIME_VARIABLES.put(key, value);
    }

    /**
     * Remove property.
     *
     * @param key the key
     */
    public static void remove(String key) {
        RUNTIME_VARIABLES.remove(key);
    }

    /**
     * Getter for all the keys.
     *
     * @return the keys
     */
    public static String[] getKeys() {
        Set<String> keys = new HashSet<>();
        keys.addAll(RUNTIME_VARIABLES.keySet());
        keys.addAll(ENVIRONMENT_VARIABLES.keySet());
        keys.addAll(DEPLOYMENT_VARIABLES.keySet());
        keys.addAll(MODULE_VARIABLES.keySet());
        return keys.toArray(new String[] {});
    }

    /**
     * Update the properties values from the System's properties and from the Environment if any.
     */
    public static void update() {
        loadEnvironmentConfig();
    }

    /**
     * Checks if is anonymous mode enabled.
     *
     * @return true, if is anonymous mode enabled
     */
    public static boolean isAnonymousModeEnabled() {
        try {
            Class.forName("org.eclipse.dirigible.runtime.anonymous.AnonymousAccess");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return !isProtectedModeEnabled();
    }

    /**
     * Checks if is protected mode enabled.
     *
     * @return true, if is protected mode enabled
     */
    private static boolean isProtectedModeEnabled() {
        return isKeycloakModeEnabled() || isOAuthAuthenticationEnabled() || isJwtModeEnabled();
    }

    /**
     * Checks if is anonymous user enabled.
     *
     * @return true, if is anonymous user enabled
     */
    public static boolean isAnonymousUserEnabled() {
        try {
            Class.forName("org.eclipse.dirigible.anonymous.AnonymousUser");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return !isProtectedModeEnabled();
    }

    /**
     * Checks if the OAuth authentication is enabled.
     *
     * @return true, if the OAuth authentication is enabled
     */
    public static boolean isOAuthAuthenticationEnabled() {
        return isActiveSpringProfile("oauth");
    }

    /**
     * Checks if the Keycloak authentication is enabled.
     *
     * @return true, if the Keycloak authentication is enabled
     */
    public static boolean isKeycloakModeEnabled() {
        return isActiveSpringProfile("keycloak");
    }

    /**
     * Checks if is active spring profile.
     *
     * @param profile the profile
     * @return true, if is active spring profile
     */
    private static boolean isActiveSpringProfile(String profile) {
        return get("spring.profiles.active", "").contains(profile) || get("spring_profiles_active", "").contains(profile);
    }

    /**
     * Checks if is JWT mode enabled.
     *
     * @return true, if is JWT mode enabled
     */
    public static boolean isJwtModeEnabled() {
        boolean enabled = false;
        if (isOAuthAuthenticationEnabled()) {
            try {
                Class.forName("org.eclipse.dirigible.jwt.JwtAccess");
                enabled = true;
            } catch (ClassNotFoundException e) {
                // Do nothing
            }
        }
        return enabled;
    }

    /**
     * Checks if productive iframe is enabled.
     *
     * @return true, if productive iframe is enabled
     */
    public static boolean isProductiveIFrameEnabled() {
        return Boolean.parseBoolean(Configuration.get("DIRIGIBLE_PRODUCTIVE_IFRAME_ENABLED", Boolean.TRUE.toString()));
    }

    /**
     * Checks if Web IDE Terminal is enabled.
     *
     * @return true, if Web IDE Terminal is enabled
     */
    public static boolean isTerminalEnabled() {
        return Boolean.parseBoolean(Configuration.get("DIRIGIBLE_TERMINAL_ENABLED", Boolean.TRUE.toString()));
    }

    /**
     * Setter as a System's property.
     *
     * @param key the key
     * @param value the value
     */
    public static void setSystemProperty(String key, String value) {
        System.setProperty(key, value);
    }

    /**
     * Getter for the configurations set programmatically at runtime.
     *
     * @return the map of the runtime variables
     */
    public static Map<String, String> getRuntimeVariables() {
        return new HashMap<>(RUNTIME_VARIABLES);
    }

    /**
     * Getter for the configurations from the environment.
     *
     * @return the map of the variables from the environment
     */
    public static Map<String, String> getEnvironmentVariables() {
        return new HashMap<>(ENVIRONMENT_VARIABLES);
    }

    /**
     * Getter for the configurations from the dirigible.properties files
     *
     * @return the map of the variables from the dirigible.properties files
     */
    public static Map<String, String> getDeploymentVariables() {
        return new HashMap<>(DEPLOYMENT_VARIABLES);
    }

    /**
     * Getter for the configurations from the module's dirigible-*.properties files
     *
     * @return the map of the variables from the module's dirigible-*.properties files
     */
    public static Map<String, String> getModuleVariables() {
        return new HashMap<>(MODULE_VARIABLES);
    }

    /**
     * Checks if is OS windows.
     *
     * @return true, if is OS windows
     */
    public static boolean isOSWindows() {
        return (getOS().contains("win"));
    }

    /**
     * Gets the os.
     *
     * @return the os
     */
    public static String getOS() {
        return System.getProperty("os.name")
                     .toLowerCase();
    }

    /**
     * Checks if is OS mac.
     *
     * @return true, if is OS mac
     */
    public static boolean isOSMac() {
        return (getOS().contains("mac"));
    }

    /**
     * Checks if is osunix.
     *
     * @return true, if is osunix
     */
    public static boolean isOSUNIX() {
        return (getOS().contains("nix") || getOS().contains("nux") || getOS().indexOf("aix") > 0);
    }

    /**
     * Checks if is OS solaris.
     *
     * @return true, if is OS solaris
     */
    public static boolean isOSSolaris() {
        return (getOS().contains("sunos"));
    }

    /**
     * Gets the configuration parameters.
     *
     * @return the configuration parameters
     */
    public static String[] getConfigurationParameters() {
        return CONFIGURATION_PARAMETERS.clone();
    }

    /**
     * Configure a runtime object from configuration sources. The parameter patterns are: 1)
     * ${CONFIGURATION_PARAMETER} 2) ${CONFIGURATION_PARAMETER}.{DEFAULT_VALUE}
     *
     * @param o the object
     */
    public static void configureObject(Object o) {
        if (o == null) {
            return;
        }
        try {
            for (Field field : FieldUtils.getAllFields(o.getClass())) {
                Object v = FieldUtils.readField(field, o, true);
                if (v != null) {
                    if (v instanceof String s) {
                        if (s.startsWith("${") && s.endsWith("}")) {
                            if (s.indexOf("}.{") > 0) {
                                String k = s.substring(2, s.indexOf("}.{"));
                                String d = s.substring(s.indexOf("}.{") + 3, s.length() - 1);
                                FieldUtils.writeField(field, o, Configuration.get(k, d), true);
                            } else {
                                String k = s.substring(2, s.length() - 1);
                                FieldUtils.writeField(field, o, Configuration.get(k), true);
                            }
                        } else {
                            Matcher matcher = MULTIVARIABLE_PATTERN.matcher(s);
                            if (!matcher.find()) {
                                continue;
                            }

                            String finalValue = s;
                            do {
                                String placeholder = matcher.group(0);
                                String configName = matcher.group(1);
                                String replacement = Configuration.get(configName);
                                if (null == replacement) {
                                    logger.warn(
                                            "Missing configuration with name: [{}]. The value will not be replaced for field [{}] with value [{}]",
                                            configName, field.getName(), v);
                                    continue;
                                }
                                finalValue = finalValue.replaceAll(Pattern.quote(placeholder), replacement);
                            } while (matcher.find());
                            FieldUtils.writeField(field, o, finalValue, true);
                        }
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException |

                IllegalAccessException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
