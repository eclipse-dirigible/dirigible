/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The Enum DirigibleConfig.
 */
public enum DirigibleConfig {

    MS_SHAREPOINT_TENANT_ID("DIRIGIBLE_MS_SHAREPOINT_TENANT_ID", null), //
    MS_SHAREPOINT_SITE_HOSTNAME("DIRIGIBLE_MS_SHAREPOINT_SITE_HOSTNAME", null), //
    MS_SHAREPOINT_SITE_PATH("DIRIGIBLE_MS_SHAREPOINT_SITE_PATH", null), //
    MS_SHAREPOINT_CLIENT_ID("DIRIGIBLE_MS_SHAREPOINT_CLIENT_ID", null), //
    MS_SHAREPOINT_CLIENT_SECRET("DIRIGIBLE_MS_SHAREPOINT_CLIENT_SECRET", null), //
    MS_SHAREPOINT_TOKEN("DIRIGIBLE_MS_SHAREPOINT_TOKEN", null), //

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

    /** Bridge the platform readiness onto Spring's ApplicationAvailability (#6448). */
    READINESS_AVAILABILITY_BRIDGE_ENABLED("DIRIGIBLE_READINESS_AVAILABILITY_BRIDGE_ENABLED", Boolean.FALSE.toString()), //

    /** Seconds an API client is asked to wait when the boot gate refuses a request (#6448). */
    READINESS_GATE_RETRY_AFTER_SECONDS("DIRIGIBLE_READINESS_GATE_RETRY_AFTER_SECONDS", "5"), //

    HOME_URL("DIRIGIBLE_HOME_URL", "services/web/home/"), //

    /**
     * The application's externally-reachable base URL, used to build absolute links (e.g. in
     * notification emails).
     */
    APP_BASE_URL("DIRIGIBLE_APP_BASE_URL", ""), //

    APPLICATION_LANGUAGES("DIRIGIBLE_APPLICATION_LANGUAGES", "en"), //

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

    /** Serve and store Office documents with the legacy Microsoft mime types. */
    DOCUMENTS_CONTENT_TYPE_MS_ENABLED("DIRIGIBLE_DOCUMENTS_EXT_CONTENT_TYPE_MS_ENABLED", "false"),

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

    /**
     * How the current tenant is resolved for a request: {@code SUBDOMAIN} (the default - matched from
     * the host header against {@link #TENANT_SUBDOMAIN_REGEX}) or {@code TOKEN_GROUPS} (the tenant the
     * user selected, validated against the identity provider groups named
     * {@code <tenantId>.<appId>.<role>}), which lets one host serve every tenant of the application.
     */
    TENANT_RESOLUTION_STRATEGY("DIRIGIBLE_TENANT_RESOLUTION_STRATEGY", "SUBDOMAIN"),

    /**
     * The id of the application this deployment runs, as it appears in the identity provider group
     * names {@code <tenantId>.<appId>.<role>}. Groups of other applications are ignored. Mandatory when
     * {@link #TENANT_RESOLUTION_STRATEGY} is {@code TOKEN_GROUPS}, and it must not contain a dot.
     */
    APP_ID("DIRIGIBLE_APP_ID", null),

    /**
     * The token claim carrying the user groups. AWS Cognito puts them in {@code cognito:groups} (the
     * default), a Keycloak realm typically in {@code groups}.
     */
    TENANT_GROUPS_CLAIM("DIRIGIBLE_TENANT_GROUPS_CLAIM", "cognito:groups"),

    SNOWFLAKE_ADMIN_USERNAME("DIRIGIBLE_SNOWFLAKE_ADMIN_USERNAME", null),

    /** The basic admin username. */
    BASIC_ADMIN_USERNAME("DIRIGIBLE_BASIC_USERNAME", toBase64("admin")),

    /** The basic admin pass. */
    BASIC_ADMIN_PASS("DIRIGIBLE_BASIC_PASSWORD", toBase64("admin")),

    /**
     * Optional application-owned login page for the OAuth2 login profiles (cognito, keycloak). When
     * set, unauthenticated browser requests are redirected to this page (typically under the already
     * public {@code /public/web/}) instead of the identity provider, so the application hosts its own
     * sign-in UX. Blank keeps the standard redirect to the provider.
     */
    SECURITY_LOGIN_PAGE("DIRIGIBLE_SECURITY_LOGIN_PAGE", null),

    /** Whether the Java LSP (JDT.LS) integration is enabled. */
    JAVA_LSP_ENABLED("DIRIGIBLE_JAVA_LSP_ENABLED", Boolean.TRUE.toString()),

    /** Directory where the JDT Language Server is installed (or will be extracted to). */
    JAVA_LSP_INSTALL_DIR("DIRIGIBLE_JAVA_LSP_INSTALL_DIR", null),

    /** Max heap (-Xmx) for the JDT.LS process; 512m OOMs when indexing the full platform classpath. */
    JAVA_LSP_MAX_HEAP("DIRIGIBLE_JAVA_LSP_MAX_HEAP", "2g"),

    /** Default JDWP port the Java debug adapter attaches to. */
    JAVA_DEBUG_JDWP_PORT("DIRIGIBLE_JAVA_DEBUG_JDWP_PORT", "8000"),

    /** Whether dynamic dependency resolution of project.json maven declarations is enabled. */
    DEPENDENCIES_DYNAMIC_ENABLED("DIRIGIBLE_DEPENDENCIES_DYNAMIC", Boolean.TRUE.toString()),

    /**
     * Directory the resolved dependency JARs are linked into; blank means [user
     * home]/.dirigible/resolved-modules.
     */
    DEPENDENCIES_DIR("DIRIGIBLE_DEPENDENCIES_DIR", null),

    /**
     * Whether dependency resolution is frozen: the activated set comes from the lockfile only -
     * checksum-verified, no re-mediation, no new coordinates, network never consulted. The mode
     * immutable production images should run.
     */
    DEPENDENCIES_FROZEN("DIRIGIBLE_DEPENDENCIES_FROZEN", Boolean.FALSE.toString()),

    /**
     * Path of the dependency lockfile; blank means project-lock.json inside the resolved-modules
     * directory.
     */
    DEPENDENCIES_LOCKFILE("DIRIGIBLE_DEPENDENCIES_LOCKFILE", null),

    /**
     * Maven local repository; blank means [user home]/.m2/repository when it exists, else [user
     * home]/.dirigible/m2.
     */
    MAVEN_LOCAL_REPO("DIRIGIBLE_MAVEN_LOCAL_REPO", null),

    /**
     * Remote Maven repositories as comma-separated id=url pairs. An entry with id central overrides the
     * default Maven Central URL; other entries are added to it. Credentials go in the
     * DIRIGIBLE_MAVEN_[ID]_USERNAME / DIRIGIBLE_MAVEN_[ID]_PASSWORD pair for the entry's uppercased id.
     */
    MAVEN_REPOSITORIES("DIRIGIBLE_MAVEN_REPOSITORIES", null),

    /** Whether Maven dependency resolution runs offline (local repository only). */
    MAVEN_OFFLINE("DIRIGIBLE_MAVEN_OFFLINE", Boolean.FALSE.toString()),

    /** Milliseconds the platform waits for a started native-app process to start accepting TCP. */
    NATIVE_APP_READY_TIMEOUT_MS("DIRIGIBLE_NATIVE_APP_READY_TIMEOUT_MS", "30000"),

    /** Interval (seconds) between ticks of the SystemJob that keeps ALWAYS-mode native apps alive. */
    NATIVE_APP_MONITOR_INTERVAL_SECONDS("DIRIGIBLE_NATIVE_APP_MONITOR_INTERVAL_SECONDS", "30"),

    /** TTL in seconds for the native-app proxy lookup cache. */
    NATIVE_APP_REGISTRY_TTL_SECONDS("DIRIGIBLE_NATIVE_APP_REGISTRY_TTL_SECONDS", "60"),

    /** Interval (seconds) between ticks of the relay that drains the entity event outbox. */
    EVENT_OUTBOX_RELAY_INTERVAL_SECONDS("DIRIGIBLE_EVENT_OUTBOX_RELAY_INTERVAL_SECONDS", "30"),

    /**
     * Seconds an outbox entry is left alone after it was written (or last attempted) before the relay
     * picks it up. Keeps the relay from racing the in-process dispatch that follows every commit.
     */
    EVENT_OUTBOX_RELAY_GRACE_SECONDS("DIRIGIBLE_EVENT_OUTBOX_RELAY_GRACE_SECONDS", "60"),

    /** Anthropic API key powering the Intent Editor's AI assistant; blank disables the assistant. */
    INTENT_AI_API_KEY("DIRIGIBLE_INTENT_AI_API_KEY", null),

    /** Claude model the Intent Editor's AI assistant talks to. */
    INTENT_AI_MODEL("DIRIGIBLE_INTENT_AI_MODEL", "claude-opus-4-8"),

    /** Base URL of the Anthropic-compatible API the Intent assistant calls. */
    INTENT_AI_BASE_URL("DIRIGIBLE_INTENT_AI_BASE_URL", "https://api.anthropic.com"),

    /**
     * Maximum tokens the Intent assistant may generate in a single proposal. The tool contract re-emits
     * the COMPLETE {@code app.intent} on every turn and every repair round, so the ceiling has to hold
     * a whole application plus its explanation, not one edit - a few hundred lines of this YAML is
     * thousands of tokens before the JSON string escaping, and the reasoning pass draws on the same
     * budget.
     */
    INTENT_AI_MAX_TOKENS("DIRIGIBLE_INTENT_AI_MAX_TOKENS", "32768"),

    /** Anthropic API version header sent by the Intent assistant. */
    INTENT_AI_VERSION("DIRIGIBLE_INTENT_AI_VERSION", "2023-06-01"),

    /**
     * URL of an external ActiveMQ broker the messaging engine connects to, e.g.
     * {@code tcp://activemq:61616}, {@code ssl://b-....mq.eu-central-1.amazonaws.com:61617} or a
     * {@code failover:(...)} list. Left unset (or blank) the platform starts and uses its own embedded
     * {@code vm://localhost} broker, which is the only mode the messaging monitoring perspective can
     * introspect.
     */
    MESSAGING_BROKER_URL("DIRIGIBLE_MESSAGING_BROKER_URL", null),

    /** Username for the external messaging broker; unset connects anonymously. */
    MESSAGING_BROKER_USERNAME("DIRIGIBLE_MESSAGING_BROKER_USERNAME", null),

    /** Password for the external messaging broker; unset connects anonymously. */
    MESSAGING_BROKER_PASSWORD("DIRIGIBLE_MESSAGING_BROKER_PASSWORD", null),

    /**
     * Whether the EMBEDDED messaging broker persists its messages in the default (system) database.
     * Applies to the embedded broker only - an external broker owns its own persistence, so the value
     * is ignored when {@link #MESSAGING_BROKER_URL} is set.
     */
    MESSAGING_USE_DEFAULT_DATABASE("DIRIGIBLE_MESSAGING_USE_DEFAULT_DATABASE", Boolean.TRUE.toString()),

    /**
     * Seconds an armed act-as (delegated entry) state survives before it expires on its own. The window
     * is absolute - it starts at arming and is never renewed by activity - so a state left armed and
     * forgotten stops hiding the real identity's world on its own.
     */
    ACT_AS_TTL_SECONDS("DIRIGIBLE_ACT_AS_TTL_SECONDS", "1800");

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

    public String getMandatoryStringValue() throws InvalidConfigException {
        String stringValue = getStringValue();
        if (StringUtils.isBlank(stringValue)) {
            throw new InvalidConfigException("Configuration with key [" + key + "] is empty", key);
        }
        return stringValue;
    }
}
