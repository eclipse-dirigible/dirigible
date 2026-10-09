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

import static org.eclipse.dirigible.commons.config.ConfigMeta.meta;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The Enum DirigibleConfig.
 */
public enum DirigibleConfig {

    MS_SHAREPOINT_TENANT_ID("DIRIGIBLE_MS_SHAREPOINT_TENANT_ID", null), //
    MS_SHAREPOINT_SITE_HOSTNAME("DIRIGIBLE_MS_SHAREPOINT_SITE_HOSTNAME", null), //
    MS_SHAREPOINT_SITE_PATH("DIRIGIBLE_MS_SHAREPOINT_SITE_PATH", null), //
    MS_SHAREPOINT_CLIENT_ID("DIRIGIBLE_MS_SHAREPOINT_CLIENT_ID", null), //
    MS_SHAREPOINT_CLIENT_SECRET("DIRIGIBLE_MS_SHAREPOINT_CLIENT_SECRET", null, meta().secret()), //
    MS_SHAREPOINT_TOKEN("DIRIGIBLE_MS_SHAREPOINT_TOKEN", null, meta().secret()), //

    REGISTRY_EXTERNAL_FOLDER("DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER", null), //

    // an example for DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER=/a/b/mydir
    // if set to true - /a/b/mydir will be replicated to <repo_dir>/mydir
    // if set to false - /a/b/mydir will be replicated to <repo_dir>
    REGISTRY_EXTERNAL_FOLDER_AS_SUBFOLDER("DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER_AS_SUBFOLDER", Boolean.FALSE.toString()), //
    // folders separated by comma, example value: target,bin,node_modules
    REGISTRY_EXTERNAL_IGNORED_FOLDERS("DIRIGIBLE_REGISTRY_EXTERNAL_IGNORED_FOLDERS", null, meta().type(ConfigType.LIST)), //
    // folders separated by comma, example value: target,bin,node_modules
    REGISTRY_LOCAL_IGNORED_FOLDERS("DIRIGIBLE_REGISTRY_LOCAL_IGNORED_FOLDERS", null, meta().type(ConfigType.LIST)), //

    CSV_DATA_BATCH_SIZE("DIRIGIBLE_CSV_DATA_BATCH_SIZE", "1000"), //

    FLOWABLE_DATABASE_DRIVER("DIRIGIBLE_FLOWABLE_DATABASE_DRIVER", null), //
    FLOWABLE_DATABASE_URL("DIRIGIBLE_FLOWABLE_DATABASE_URL", null), //
    FLOWABLE_DATABASE_USER("DIRIGIBLE_FLOWABLE_DATABASE_USER", null), //
    FLOWABLE_DATABASE_PASSWORD("DIRIGIBLE_FLOWABLE_DATABASE_PASSWORD", null, meta().secret()), //
    FLOWABLE_DATABASE_DATASOURCE_NAME("DIRIGIBLE_FLOWABLE_DATABASE_DATASOURCE_NAME", null), //
    FLOWABLE_DATABASE_SCHEMA_UPDATE("DIRIGIBLE_FLOWABLE_DATABASE_SCHEMA_UPDATE", Boolean.TRUE.toString()), //
    /**
     * The Flowable history level: what the engine records about finished and deleted process instances.
     * {@code audit} (Flowable's own default) keeps every instance, task and variable, so an instance
     * that is gone from the runtime can still be accounted for; {@code none} is an explicit opt-out
     * that leaves no trace at all.
     */
    FLOWABLE_HISTORY_LEVEL("DIRIGIBLE_FLOWABLE_HISTORY_LEVEL", "audit", meta().type(ConfigType.ENUM)), //

    FLOWABLE_MAIL_SERVER_HOST("DIRIGIBLE_FLOWABLE_MAIL_SERVER_HOST", null, meta().subgroup("process")), //
    FLOWABLE_MAIL_SERVER_PORT("DIRIGIBLE_FLOWABLE_MAIL_SERVER_PORT", "587", meta().subgroup("process")), //
    FLOWABLE_MAIL_SERVER_USERNAME("DIRIGIBLE_FLOWABLE_MAIL_SERVER_USERNAME", null, meta().subgroup("process")), //
    FLOWABLE_MAIL_SERVER_PASSWORD("DIRIGIBLE_FLOWABLE_MAIL_SERVER_PASSWORD", null, meta().subgroup("process")
                                                                                         .secret()), //
    FLOWABLE_MAIL_SERVER_USE_TLS("DIRIGIBLE_FLOWABLE_MAIL_SERVER_USE_TLS", Boolean.TRUE.toString(), meta().subgroup("process")), //
    FLOWABLE_MAIL_SERVER_USE_SSL("DIRIGIBLE_FLOWABLE_MAIL_SERVER_USE_SSL", Boolean.FALSE.toString(), meta().subgroup("process")), //
    FLOWABLE_MAIL_SERVER_DEFAULT_FROM("DIRIGIBLE_FLOWABLE_MAIL_SERVER_DEFAULT_FROM", null, meta().subgroup("process")), //

    EXEC_COMMAND_LOGGING_ENABLED("DIRIGIBLE_EXEC_COMMAND_LOGGING_ENABLED", Boolean.FALSE.toString()), //

    SYNCHRONIZER_CROSS_RETRY_COUNT("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_COUNT", "10"), //

    SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS", "10000"), //

    /**
     * How often an idle instance gives its FAILED artefacts one more START attempt (#7248). A pass
     * otherwise runs only on a registry change, so a listener the broker refused at boot was never
     * retried until someone published something else.
     */
    SYNCHRONIZER_FAILED_RETRY_INTERVAL_SECONDS("DIRIGIBLE_SYNCHRONIZER_FAILED_RETRY_INTERVAL_SECONDS", "30"), //

    /** Bridge the platform readiness onto Spring's ApplicationAvailability (#6448). */
    READINESS_AVAILABILITY_BRIDGE_ENABLED("DIRIGIBLE_READINESS_AVAILABILITY_BRIDGE_ENABLED", Boolean.FALSE.toString()), //

    /**
     * With the availability bridge enabled, accept traffic only once the boot is also clean: no failed
     * artefact and every AOT-listed class registered (#7533).
     */
    READINESS_REQUIRE_CLEAN_BOOT("DIRIGIBLE_READINESS_REQUIRE_CLEAN_BOOT", Boolean.FALSE.toString()), //

    /** Seconds an API client is asked to wait when the boot gate refuses a request (#6448). */
    READINESS_GATE_RETRY_AFTER_SECONDS("DIRIGIBLE_READINESS_GATE_RETRY_AFTER_SECONDS", "5"), //

    HOME_URL("DIRIGIBLE_HOME_URL", "services/web/home/"), //

    /**
     * The application's externally-reachable base URL, used to build absolute links (e.g. in
     * notification emails). Per tenant, because a tenant may be served from its own host.
     */
    APP_BASE_URL("DIRIGIBLE_APP_BASE_URL", "", meta().type(ConfigType.URL)
                                                     .tenant(12)), //

    /**
     * The languages the Region &amp; Language picker offers, comma-separated (e.g. {@code en,bg,fr}),
     * the first one the default. Per tenant: each tenant decides which languages its users see; the
     * modules carry whatever translations they ship and fall back to the default language for anything
     * missing.
     */
    APPLICATION_LANGUAGES("DIRIGIBLE_APPLICATION_LANGUAGES", "en", meta().type(ConfigType.LIST)
                                                                         .tenant(8)), //

    /**
     * The tenant's country as an ISO 3166-1 alpha-2 code (e.g. {@code BG}), blank when the deployment
     * has none. It resolves the label variants a generated application declares per country - what a
     * national identifier is called is a property of the company, not of the language its users read
     * the UI in - and is tenant-overridable through the tenant configuration.
     */
    APPLICATION_COUNTRY("DIRIGIBLE_APPLICATION_COUNTRY", "", meta().tenant(9)), //

    MAIL_USERNAME("DIRIGIBLE_MAIL_USERNAME", null), //

    MAIL_PASSWORD("DIRIGIBLE_MAIL_PASSWORD", null, meta().secret()), //

    MAIL_TRANSPORT_PROTOCOL("DIRIGIBLE_MAIL_TRANSPORT_PROTOCOL", "smtps"), //

    MAIL_SMTP_HOST("DIRIGIBLE_MAIL_SMTP_HOST", null), //

    MAIL_SMTP_PORT("DIRIGIBLE_MAIL_SMTP_PORT", null), MAIL_SMTP_AUTH("DIRIGIBLE_MAIL_SMTP_AUTH", null), //

    SNOWFLAKE_DATA_SOURCE_LIFESPAN_SECONDS("DIRIGIBLE_SNOWFLAKE_DATA_SOURCE_LIFESPAN_SECONDS", "540"), // 9 minutes

    LEAKED_CONNECTIONS_MAX_IN_USE_SECONDS("DIRIGIBLE_LEAKED_CONNECTIONS_MAX_IN_USE_SECONDS", "180"), // 3 min by default

    LEAKED_CONNECTIONS_CHECK_INTERVAL_SECONDS("DIRIGIBLE_LEAKED_CONNECTIONS_CHECK_INTERVAL_SECONDS", "30"),

    TENANTS_PROVISIONING_FREQUENCY_SECONDS("DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS", "900"), // 15 minutes

    /** The cms internal root folder. */
    CMS_INTERNAL_ROOT_FOLDER("DIRIGIBLE_CMS_INTERNAL_ROOT_FOLDER", "target/dirigible/cms"),

    /**
     * Serve and store Office documents with the legacy Microsoft mime types. Per tenant, because it
     * depends on the client software its users open files with; read per call.
     */
    DOCUMENTS_CONTENT_TYPE_MS_ENABLED("DIRIGIBLE_DOCUMENTS_EXT_CONTENT_TYPE_MS_ENABLED", "false", meta().tenant(10)),

    /** The default data source name. */
    DEFAULT_DATA_SOURCE_NAME("DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT", "DefaultDB"),

    /** The system data source name. */
    SYSTEM_DATA_SOURCE_NAME("DIRIGIBLE_DATABASE_DATASOURCE_NAME_SYSTEM", "SystemDB"),

    /**
     * Drop a live column the published table definition no longer declares (#7635). Off by default: an
     * undeclared column is kept, with its data, and only a table's own {@code dropped} list removes
     * one. Meant for development instances, where a regenerated model is the only owner of the data.
     */
    DATABASE_DROP_UNDECLARED_COLUMNS("DIRIGIBLE_DATABASE_DROP_UNDECLARED_COLUMNS", Boolean.FALSE.toString()),

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
    TENANT_RESOLUTION_STRATEGY("DIRIGIBLE_TENANT_RESOLUTION_STRATEGY", "SUBDOMAIN", meta().type(ConfigType.ENUM)),

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

    /**
     * Whether this deployment exposes the tenant provisioning API under
     * {@code /services/tenant-provisioning/**}, through which an external provisioner registers a
     * tenant with a caller-supplied id, registers its data source from credentials it created itself,
     * and activates it. Off by default: the API hands out and accepts real database credentials, so a
     * deployment has to opt in, and when it does not, none of the beans behind it exist at all.
     */
    TENANT_PROVISIONING_API_ENABLED("DIRIGIBLE_TENANT_PROVISIONING_API_ENABLED", Boolean.FALSE.toString()),

    /**
     * Whether tenant owners may manage the users of their tenant from the application shell's Settings:
     * list them, see where each one stands, invite a person with one or more roles, change a member's
     * roles and remove a member. Each such action is one change request published to
     * {@link #TENANT_USERS_CHANGE_QUEUE} for an external provisioning system, which writes back what it
     * did through the tenant provisioning API. Off by default; it requires
     * {@link #TENANT_RESOLUTION_STRATEGY} {@code TOKEN_GROUPS} and
     * {@link #TENANT_PROVISIONING_API_ENABLED}.
     */
    TENANT_USERS_ENABLED("DIRIGIBLE_TENANT_USERS_ENABLED", Boolean.FALSE.toString()),

    /**
     * The queue the tenant owners' change requests are published to, e.g.
     * {@code global:acme.user-changes}. It must be a {@code global:} destination - one an external
     * provisioning system consumes. Required when {@link #TENANT_USERS_ENABLED} is on.
     */
    TENANT_USERS_CHANGE_QUEUE("DIRIGIBLE_TENANT_USERS_CHANGE_QUEUE", null),

    SNOWFLAKE_ADMIN_USERNAME("DIRIGIBLE_SNOWFLAKE_ADMIN_USERNAME", null),

    /** The basic admin username. */
    BASIC_ADMIN_USERNAME("DIRIGIBLE_BASIC_USERNAME", toBase64("admin")),

    /** The basic admin pass. */
    BASIC_ADMIN_PASS("DIRIGIBLE_BASIC_PASSWORD", toBase64("admin"), meta().secret()),

    /**
     * Optional application-owned login page for the OAuth2 login profiles (cognito, keycloak). When
     * set, unauthenticated browser requests are redirected to this page (typically under the already
     * public {@code /public/web/}) instead of the identity provider, so the application hosts its own
     * sign-in UX. Blank keeps the standard redirect to the provider.
     */
    SECURITY_LOGIN_PAGE("DIRIGIBLE_SECURITY_LOGIN_PAGE", null),

    /**
     * Comma-separated origins, or origin patterns such as {@code https://*.example.com},
     * {@code tauri://localhost} or {@code capacitor://localhost}, that may call the platform from
     * another origin. Unset, the OAuth2 login profiles (cognito, keycloak, github) answer no
     * cross-origin request at all, while the basic profile keeps granting every origin - without
     * credentials. The STOMP handshake accepts those that name a host - a wildcard origin never reaches
     * it - and a session opened from one of them is authenticated by its handshake cookie only with
     * {@link #CORS_ALLOW_CREDENTIALS}; otherwise its CONNECT frame carries a bearer token.
     */
    CORS_ALLOWED_ORIGINS("DIRIGIBLE_CORS_ALLOWED_ORIGINS", null, meta().type(ConfigType.LIST)),

    /**
     * Whether a cross-origin request from a configured origin may carry cookies and HTTP authentication
     * - and, the same decision, whether a STOMP session opened cross-origin from such an origin is
     * authenticated by the cookie or HTTP authentication of its handshake; without it the session's
     * CONNECT frame needs a bearer token. Refused together with a wildcard origin. A bearer-token
     * client does not need it.
     */
    CORS_ALLOW_CREDENTIALS("DIRIGIBLE_CORS_ALLOW_CREDENTIALS", Boolean.FALSE.toString()),

    /** Comma-separated HTTP methods granted to the configured origins. */
    CORS_ALLOWED_METHODS("DIRIGIBLE_CORS_ALLOWED_METHODS", "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS", meta().type(ConfigType.LIST)),

    /**
     * Comma-separated request headers granted to the configured origins. {@code X-Tenant-Id} is the
     * header a bearer request names its tenant with under the {@code TOKEN_GROUPS} tenant resolution
     * strategy.
     */
    CORS_ALLOWED_HEADERS("DIRIGIBLE_CORS_ALLOWED_HEADERS", "Authorization,Content-Type,Accept,X-Requested-With,X-Tenant-Id",
            meta().type(ConfigType.LIST)),

    /** Comma-separated response headers a script of a configured origin may read. */
    CORS_EXPOSED_HEADERS("DIRIGIBLE_CORS_EXPOSED_HEADERS", "Content-Disposition", meta().type(ConfigType.LIST)),

    /** Seconds a browser may cache the answer to a preflight request. */
    CORS_MAX_AGE("DIRIGIBLE_CORS_MAX_AGE", "3600"),

    /**
     * Whether a state-changing request (POST, PUT, PATCH, DELETE) that a browser sends from another
     * site with the user's ambient credentials - the session cookie, or HTTP authentication the browser
     * remembered - is refused with 403. That is a cross-site request forgery: a page the logged in user
     * visits posts a form to a generated endpoint. The browser's own {@code Sec-Fetch-Site} header
     * decides, {@code Origin} where it is missing; a bearer token, a request without either header (a
     * server-side client) and an origin configured in {@link #CORS_ALLOWED_ORIGINS} by host are let
     * through.
     */
    SECURITY_CROSS_SITE_PROTECTION("DIRIGIBLE_SECURITY_CROSS_SITE_PROTECTION", Boolean.TRUE.toString()),

    /**
     * The {@code X-Frame-Options} every chain answers with: {@code SAMEORIGIN} (the IDE and the
     * generated shells frame their own pages), {@code DENY}, or {@code DISABLED} for a deployment whose
     * pages are framed by another site.
     */
    SECURITY_FRAME_OPTIONS("DIRIGIBLE_SECURITY_FRAME_OPTIONS", "SAMEORIGIN", meta().type(ConfigType.ENUM)),

    /**
     * When {@code Strict-Transport-Security} is sent: {@code auto} on requests the platform sees as
     * secure (behind a TLS-terminating proxy only with {@code server.forward-headers-strategy}),
     * {@code always} on every response - for a deployment reached only over https whose proxy does not
     * forward the scheme - or {@code off}.
     */
    SECURITY_HSTS("DIRIGIBLE_SECURITY_HSTS", "auto", meta().type(ConfigType.ENUM)),

    /** The {@code Referrer-Policy} every chain answers with; blank sends none. */
    SECURITY_REFERRER_POLICY("DIRIGIBLE_SECURITY_REFERRER_POLICY", "strict-origin-when-cross-origin"),

    /**
     * A {@code Content-Security-Policy} every chain answers with; unset sends none. Sent as
     * {@code Content-Security-Policy-Report-Only} while
     * {@link #SECURITY_CONTENT_SECURITY_POLICY_REPORT_ONLY} holds, so a policy can be tried against the
     * IDE and the generated applications before it is enforced.
     */
    SECURITY_CONTENT_SECURITY_POLICY("DIRIGIBLE_SECURITY_CONTENT_SECURITY_POLICY", null),

    /**
     * Whether {@link #SECURITY_CONTENT_SECURITY_POLICY} only reports violations instead of blocking.
     */
    SECURITY_CONTENT_SECURITY_POLICY_REPORT_ONLY("DIRIGIBLE_SECURITY_CONTENT_SECURITY_POLICY_REPORT_ONLY", Boolean.TRUE.toString()),

    /** A {@code Permissions-Policy} every chain answers with; unset sends none. */
    SECURITY_PERMISSIONS_POLICY("DIRIGIBLE_SECURITY_PERMISSIONS_POLICY", null),

    /**
     * Comma-separated kinds of bearer tokens the OAuth2 login profiles (cognito, keycloak) accept:
     * {@code id} - an ID token identifies a user by the principal claim and grants the roles of the
     * user's groups - and {@code access} - an access token of a machine client is identified by
     * {@code sub} and grants the roles its scopes map to.
     */
    OAUTH2_JWT_TOKEN_KINDS("DIRIGIBLE_OAUTH2_JWT_TOKEN_KINDS", "id,access", meta().type(ConfigType.LIST)),

    /**
     * Claim of a bearer ID token the user name is read from. Blank means the user-name attribute of the
     * login profile ({@code email} on Cognito, {@code preferred_username} on Keycloak). An ID token
     * without the claim is refused.
     */
    OAUTH2_JWT_PRINCIPAL_CLAIM("DIRIGIBLE_OAUTH2_JWT_PRINCIPAL_CLAIM", null),

    /**
     * Comma-separated audiences a bearer token must be issued for. An ID token is always checked -
     * against the client id of the login profile when this is blank. An access token is checked only
     * when this is set: a Cognito access token names its client in {@code client_id}, a Keycloak one
     * carries the {@code aud} an audience mapper of the realm adds.
     */
    OAUTH2_JWT_AUDIENCES("DIRIGIBLE_OAUTH2_JWT_AUDIENCES", null, meta().type(ConfigType.LIST)),

    /** Issuer a bearer token must carry. Blank means the issuer of the login profile. */
    OAUTH2_JWT_ISSUER_URI("DIRIGIBLE_OAUTH2_JWT_ISSUER_URI", null),

    /**
     * JWKS endpoint the signatures of bearer tokens are verified against. Blank means the endpoint of
     * the login profile.
     */
    OAUTH2_JWT_JWK_SET_URI("DIRIGIBLE_OAUTH2_JWT_JWK_SET_URI", null),

    /**
     * Whether a bearer ID token identified by its {@code email} claim must also carry
     * {@code email_verified=true}. Cognito lets a user sign up with an address they do not own until it
     * is verified, so an unverified address must not become an identity here.
     */
    OAUTH2_JWT_REQUIRE_VERIFIED_EMAIL("DIRIGIBLE_OAUTH2_JWT_REQUIRE_VERIFIED_EMAIL", Boolean.TRUE.toString()),

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
     * Directory the resolved dependency JARs are linked into - the inventory of what the declarations
     * resolve to, not a launch-classpath entry (the swappable modules classloader serves them; on
     * loader.path the application classloader would shadow every later upgrade). Blank means [user
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
    MAVEN_REPOSITORIES("DIRIGIBLE_MAVEN_REPOSITORIES", null, meta().type(ConfigType.LIST)),

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

    /**
     * Interval (seconds) between ticks of the watchdog that re-attempts client-Java runtime state a
     * previous pass could not establish - a JMS subscription the broker refused, a job registration
     * that threw.
     */
    JAVA_RECONCILE_INTERVAL_SECONDS("DIRIGIBLE_JAVA_RECONCILE_INTERVAL_SECONDS", "30"),

    /** Anthropic API key powering the Intent Editor's AI assistant; blank disables the assistant. */
    INTENT_AI_API_KEY("DIRIGIBLE_INTENT_AI_API_KEY", null, meta().secret()),

    /** Claude model the Intent Editor's AI assistant talks to. */
    INTENT_AI_MODEL("DIRIGIBLE_INTENT_AI_MODEL", "claude-opus-5"),

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
    MESSAGING_BROKER_PASSWORD("DIRIGIBLE_MESSAGING_BROKER_PASSWORD", null, meta().secret()),

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
    ACT_AS_TTL_SECONDS("DIRIGIBLE_ACT_AS_TTL_SECONDS", "1800"),

    /**
     * Largest image, in bytes, a {@code .print} template may embed. An image is inlined into the
     * stylesheet as base64 (which is a third larger again) and the whole document is rendered in
     * memory, so a print that reaches for a 20 MB photograph must fail soft - print without it - rather
     * than take the render down. 2 MB is far above any logo or stamp.
     */
    PRINT_IMAGE_MAX_SIZE("DIRIGIBLE_PRINT_IMAGE_MAX_SIZE", "2097152"),

    // ---- Keys read by name elsewhere (folded in from Configuration.getConfigParams(), #7759). The
    // deprecated ones are read nowhere in this repository: listed so a deployment that still sets one
    // is warned at startup, to be removed one release later.

    ANONYMOUS_USER_NAME_PROPERTY_NAME("DIRIGIBLE_ANONYMOUS_USER_NAME_PROPERTY_NAME", null), //
    /** The branding keys: per tenant, so that each tenant's shell carries its own brand. */
    BRANDING_NAME("DIRIGIBLE_BRANDING_NAME", null, meta().tenant(0)), //
    BRANDING_SUBTITLE("DIRIGIBLE_BRANDING_SUBTITLE", null, meta().tenant(1)), //
    BRANDING_BRAND("DIRIGIBLE_BRANDING_BRAND", null, meta().tenant(2)), //
    BRANDING_BRAND_URL("DIRIGIBLE_BRANDING_BRAND_URL", null, meta().tenant(3)), //
    BRANDING_FAVICON("DIRIGIBLE_BRANDING_FAVICON", null, meta().tenant(4)), //
    BRANDING_THEME("DIRIGIBLE_BRANDING_THEME", null, meta().tenant(5)), //
    BRANDING_PREFIX("DIRIGIBLE_BRANDING_PREFIX", null, meta().tenant(6)), //
    BRANDING_ANALYTICS("DIRIGIBLE_BRANDING_ANALYTICS", null, meta().tenant(7)), //
    GIT_ROOT_FOLDER("DIRIGIBLE_GIT_ROOT_FOLDER", null), //
    REGISTRY_IMPORT_WORKSPACE("DIRIGIBLE_REGISTRY_IMPORT_WORKSPACE", null, meta().deprecated()), //
    REPOSITORY_PROVIDER("DIRIGIBLE_REPOSITORY_PROVIDER", null), //
    REPOSITORY_DATABASE_DATASOURCE_NAME("DIRIGIBLE_REPOSITORY_DATABASE_DATASOURCE_NAME", null, meta().deprecated()), //
    MASTER_REPOSITORY_PROVIDER("DIRIGIBLE_MASTER_REPOSITORY_PROVIDER", null), //
    MASTER_REPOSITORY_ZIP_LOCATION("DIRIGIBLE_MASTER_REPOSITORY_ZIP_LOCATION", null), //
    MASTER_REPOSITORY_JAR_PATH("DIRIGIBLE_MASTER_REPOSITORY_JAR_PATH", null), //
    REPOSITORY_SEARCH_ROOT_FOLDER("DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER", null), //
    REPOSITORY_SEARCH_ROOT_FOLDER_IS_ABSOLUTE("DIRIGIBLE_REPOSITORY_SEARCH_ROOT_FOLDER_IS_ABSOLUTE", null), //
    REPOSITORY_SEARCH_INDEX_LOCATION("DIRIGIBLE_REPOSITORY_SEARCH_INDEX_LOCATION", null), //
    REPOSITORY_VERSIONING_ENABLED("DIRIGIBLE_REPOSITORY_VERSIONING_ENABLED", null), //
    DATABASE_PROVIDER("DIRIGIBLE_DATABASE_PROVIDER", null), //
    DATABASE_DEFAULT_SET_AUTO_COMMIT("DIRIGIBLE_DATABASE_DEFAULT_SET_AUTO_COMMIT", null, meta().deprecated()), //
    DATABASE_DEFAULT_MAX_CONNECTIONS_COUNT("DIRIGIBLE_DATABASE_DEFAULT_MAX_CONNECTIONS_COUNT", null, meta().deprecated()), //
    DATABASE_DEFAULT_WAIT_TIMEOUT("DIRIGIBLE_DATABASE_DEFAULT_WAIT_TIMEOUT", null), //
    DATABASE_DEFAULT_WAIT_COUNT("DIRIGIBLE_DATABASE_DEFAULT_WAIT_COUNT", null, meta().deprecated()), //
    DATABASE_CUSTOM_DATASOURCES("DIRIGIBLE_DATABASE_CUSTOM_DATASOURCES", null), //
    DATABASE_DERBY_ROOT_FOLDER_DEFAULT("DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER_DEFAULT", null, meta().deprecated()), //
    DATABASE_H2_ROOT_FOLDER_DEFAULT("DIRIGIBLE_DATABASE_H2_ROOT_FOLDER_DEFAULT", null), //
    DATABASE_H2_DRIVER("DIRIGIBLE_DATABASE_H2_DRIVER", null), //
    DATABASE_H2_URL("DIRIGIBLE_DATABASE_H2_URL", null), //
    DATABASE_H2_USERNAME("DIRIGIBLE_DATABASE_H2_USERNAME", null), //
    DATABASE_H2_PASSWORD("DIRIGIBLE_DATABASE_H2_PASSWORD", null, meta().secret()), //
    DATABASE_TRANSFER_BATCH_SIZE("DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE", null), //
    PERSISTENCE_CREATE_TABLE_ON_USE("DIRIGIBLE_PERSISTENCE_CREATE_TABLE_ON_USE", null), //
    MONGODB_CLIENT_URI("DIRIGIBLE_MONGODB_CLIENT_URI", null), //
    MONGODB_DATABASE_DEFAULT("DIRIGIBLE_MONGODB_DATABASE_DEFAULT", null), //
    SCHEDULER_MEMORY_STORE("DIRIGIBLE_SCHEDULER_MEMORY_STORE", null), //
    SCHEDULER_DATASOURCE_TYPE("DIRIGIBLE_SCHEDULER_DATASOURCE_TYPE", null, meta().deprecated()), //
    SCHEDULER_DATASOURCE_NAME("DIRIGIBLE_SCHEDULER_DATASOURCE_NAME", null, meta().deprecated()), //
    SCHEDULER_DATABASE_DELEGATE("DIRIGIBLE_SCHEDULER_DATABASE_DELEGATE", null), //
    SCHEDULER_LOGS_RETANTION_PERIOD("DIRIGIBLE_SCHEDULER_LOGS_RETANTION_PERIOD", null, meta().type(ConfigType.DURATION)
                                                                                             .aliasOf(
                                                                                                     "DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD")), //
    SCHEDULER_EMAIL_SENDER("DIRIGIBLE_SCHEDULER_EMAIL_SENDER", null), //
    SCHEDULER_EMAIL_RECIPIENTS("DIRIGIBLE_SCHEDULER_EMAIL_RECIPIENTS", null, meta().type(ConfigType.LIST)), //
    SCHEDULER_EMAIL_SUBJECT_ERROR("DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ERROR", null), //
    SCHEDULER_EMAIL_SUBJECT_NORMAL("DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_NORMAL", null), //
    SCHEDULER_EMAIL_TEMPLATE_ERROR("DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ERROR", null), //
    SCHEDULER_EMAIL_TEMPLATE_NORMAL("DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_NORMAL", null), //
    SCHEDULER_EMAIL_URL_SCHEME("DIRIGIBLE_SCHEDULER_EMAIL_URL_SCHEME", null), //
    SCHEDULER_EMAIL_URL_HOST("DIRIGIBLE_SCHEDULER_EMAIL_URL_HOST", null), //
    SCHEDULER_EMAIL_URL_PORT("DIRIGIBLE_SCHEDULER_EMAIL_URL_PORT", null), //
    SYNCHRONIZER_IGNORE_DEPENDENCIES("DIRIGIBLE_SYNCHRONIZER_IGNORE_DEPENDENCIES", null, meta().deprecated()), //
    SYNCHRONIZER_EXCLUDE_PATHS("DIRIGIBLE_SYNCHRONIZER_EXCLUDE_PATHS", null, meta().deprecated()), //
    JOB_EXPRESSION_BPM("DIRIGIBLE_JOB_EXPRESSION_BPM", null, meta().deprecated()), //
    JOB_EXPRESSION_DATA_STRUCTURES("DIRIGIBLE_JOB_EXPRESSION_DATA_STRUCTURES", null, meta().deprecated()), //
    JOB_EXPRESSION_EXTENSIONS("DIRIGIBLE_JOB_EXPRESSION_EXTENSIONS", null, meta().deprecated()), //
    JOB_EXPRESSION_JOBS("DIRIGIBLE_JOB_EXPRESSION_JOBS", null, meta().deprecated()), //
    JOB_EXPRESSION_MESSAGING("DIRIGIBLE_JOB_EXPRESSION_MESSAGING", null, meta().deprecated()), //
    JOB_EXPRESSION_MIGRATIONS("DIRIGIBLE_JOB_EXPRESSION_MIGRATIONS", null, meta().deprecated()), //
    JOB_EXPRESSION_ODATA("DIRIGIBLE_JOB_EXPRESSION_ODATA", null, meta().deprecated()), //
    JOB_EXPRESSION_PUBLISHER("DIRIGIBLE_JOB_EXPRESSION_PUBLISHER", null, meta().deprecated()), //
    JOB_EXPRESSION_SECURITY("DIRIGIBLE_JOB_EXPRESSION_SECURITY", null, meta().deprecated()), //
    JOB_EXPRESSION_REGISTRY("DIRIGIBLE_JOB_EXPRESSION_REGISTRY", null, meta().deprecated()), //
    JOB_DEFAULT_TIMEOUT("DIRIGIBLE_JOB_DEFAULT_TIMEOUT", null), //
    CMS_PROVIDER("DIRIGIBLE_CMS_PROVIDER", null), //
    /**
     * Whether the per-path CMS access grants are enforced. Per tenant, because whether a tenant
     * restricts folders by role is its own decision; read per request.
     */
    CMS_ROLES_ENABLED("DIRIGIBLE_CMS_ROLES_ENABLED", Boolean.TRUE.toString(), meta().tenant(11)), //
    CMS_INTERNAL_ROOT_FOLDER_IS_ABSOLUTE("DIRIGIBLE_CMS_INTERNAL_ROOT_FOLDER_IS_ABSOLUTE", null, meta().deprecated()), //
    CMS_INTERNAL_VERSIONING_ENABLED("DIRIGIBLE_CMS_INTERNAL_VERSIONING_ENABLED", null), //
    CMS_MANAGED_CONFIGURATION_JNDI_NAME("DIRIGIBLE_CMS_MANAGED_CONFIGURATION_JNDI_NAME", null, meta().deprecated()), //
    CMS_MANAGED_CONFIGURATION_AUTH_METHOD("DIRIGIBLE_CMS_MANAGED_CONFIGURATION_AUTH_METHOD", null, meta().deprecated()), //
    CMS_MANAGED_CONFIGURATION_NAME("DIRIGIBLE_CMS_MANAGED_CONFIGURATION_NAME", null, meta().deprecated()), //
    CMS_MANAGED_CONFIGURATION_KEY("DIRIGIBLE_CMS_MANAGED_CONFIGURATION_KEY", null, meta().deprecated()), //
    CMS_MANAGED_CONFIGURATION_DESTINATION("DIRIGIBLE_CMS_MANAGED_CONFIGURATION_DESTINATION", null, meta().deprecated()), //
    CONNECTIVITY_CONFIGURATION_JNDI_NAME("DIRIGIBLE_CONNECTIVITY_CONFIGURATION_JNDI_NAME", null), //
    CMS_DATABASE_DATASOURCE_TYPE("DIRIGIBLE_CMS_DATABASE_DATASOURCE_TYPE", null, meta().deprecated()), //
    CMS_DATABASE_DATASOURCE_NAME("DIRIGIBLE_CMS_DATABASE_DATASOURCE_NAME", null, meta().deprecated()), //
    BPM_PROVIDER("DIRIGIBLE_BPM_PROVIDER", null, meta().deprecated()), //
    FLOWABLE_USE_SYSTEM_DATASOURCE("DIRIGIBLE_FLOWABLE_USE_SYSTEM_DATASOURCE", null, meta().deprecated()), //
    JAVASCRIPT_ENGINE_TYPE_DEFAULT("DIRIGIBLE_JAVASCRIPT_ENGINE_TYPE_DEFAULT", null, meta().deprecated()), //
    JAVASCRIPT_GRAALVM_DEBUGGER_PORT("DIRIGIBLE_JAVASCRIPT_GRAALVM_DEBUGGER_PORT", null), //
    JAVASCRIPT_GRAALVM_ALLOW_HOST_ACCESS("DIRIGIBLE_JAVASCRIPT_GRAALVM_ALLOW_HOST_ACCESS", null, meta().deprecated()), //
    JAVASCRIPT_GRAALVM_ALLOW_CREATE_THREAD("DIRIGIBLE_JAVASCRIPT_GRAALVM_ALLOW_CREATE_THREAD", null, meta().deprecated()), //
    JAVASCRIPT_GRAALVM_ALLOW_CREATE_PROCESS("DIRIGIBLE_JAVASCRIPT_GRAALVM_ALLOW_CREATE_PROCESS", null, meta().deprecated()), //
    JAVASCRIPT_GRAALVM_ALLOW_IO("DIRIGIBLE_JAVASCRIPT_GRAALVM_ALLOW_IO", null, meta().deprecated()), //
    JAVASCRIPT_GRAALVM_COMPATIBILITY_MODE_NASHORN("DIRIGIBLE_JAVASCRIPT_GRAALVM_COMPATIBILITY_MODE_NASHORN", null, meta().deprecated()), //
    JAVASCRIPT_GRAALVM_COMPATIBILITY_MODE_MOZILLA("DIRIGIBLE_JAVASCRIPT_GRAALVM_COMPATIBILITY_MODE_MOZILLA", null, meta().deprecated()), //
    OPERATIONS_LOGS_ROOT_FOLDER_DEFAULT("DIRIGIBLE_OPERATIONS_LOGS_ROOT_FOLDER_DEFAULT", null), //
    THEME_DEFAULT("DIRIGIBLE_THEME_DEFAULT", null), //
    GENERATE_PRETTY_NAMES("DIRIGIBLE_GENERATE_PRETTY_NAMES", null, meta().deprecated()), //
    OAUTH_CUSTOM_CLIENTS("DIRIGIBLE_OAUTH_CUSTOM_CLIENTS", null), //
    OAUTH_ENABLED("DIRIGIBLE_OAUTH_ENABLED", null, meta().deprecated()), //
    OAUTH_AUTHORIZE_UR("DIRIGIBLE_OAUTH_AUTHORIZE_UR", null, meta().aliasOf("DIRIGIBLE_OAUTH_AUTHORIZE_URL")), //
    OAUTH_TOKEN_URL("DIRIGIBLE_OAUTH_TOKEN_URL", null, meta().deprecated()), //
    OAUTH_CLIENT_ID("DIRIGIBLE_OAUTH_CLIENT_ID", null, meta().deprecated()), //
    OAUTH_CLIENT_SECRET("DIRIGIBLE_OAUTH_CLIENT_SECRET", null, meta().secret()
                                                                     .deprecated()), //
    OAUTH_VERIFICATION_KEY("DIRIGIBLE_OAUTH_VERIFICATION_KEY", null, meta().deprecated()), //
    OAUTH_APPLICATION_NAME("DIRIGIBLE_OAUTH_APPLICATION_NAME", null, meta().deprecated()), //
    OAUTH_APPLICATION_HOST("DIRIGIBLE_OAUTH_APPLICATION_HOST", null, meta().deprecated()), //
    OAUTH_ISSUER("DIRIGIBLE_OAUTH_ISSUER", null, meta().deprecated()), //
    OAUTH_AUTHORIZE_URL("DIRIGIBLE_OAUTH_AUTHORIZE_URL", null, meta().deprecated()), //
    OAUTH_TOKEN_REQUEST_METHOD("DIRIGIBLE_OAUTH_TOKEN_REQUEST_METHOD", null, meta().deprecated()), //
    OAUTH_VERIFICATION_KEY_EXPONENT("DIRIGIBLE_OAUTH_VERIFICATION_KEY_EXPONENT", null, meta().deprecated()), //
    OAUTH_CHECK_ISSUER_ENABLED("DIRIGIBLE_OAUTH_CHECK_ISSUER_ENABLED", null, meta().deprecated()), //
    OAUTH_CHECK_AUDIENCE_ENABLED("DIRIGIBLE_OAUTH_CHECK_AUDIENCE_ENABLED", null, meta().deprecated()), //
    PRODUCT_NAME("DIRIGIBLE_PRODUCT_NAME", null), //
    PRODUCT_VERSION("DIRIGIBLE_PRODUCT_VERSION", null), //
    PRODUCT_REPOSITORY("DIRIGIBLE_PRODUCT_REPOSITORY", null), //
    PRODUCT_COMMIT_ID("DIRIGIBLE_PRODUCT_COMMIT_ID", null), //
    PRODUCT_TYPE("DIRIGIBLE_PRODUCT_TYPE", null), //
    INSTANCE_NAME("DIRIGIBLE_INSTANCE_NAME", null), //
    SPARK_CLIENT_URI("DIRIGIBLE_SPARK_CLIENT_URI", null, meta().deprecated()), //
    TERMINAL_ENABLED("DIRIGIBLE_TERMINAL_ENABLED", Boolean.TRUE.toString()), //
    MAIL_CONFIG_PROVIDER("DIRIGIBLE_MAIL_CONFIG_PROVIDER", null), //
    MAIL_SMTPS_HOST("DIRIGIBLE_MAIL_SMTPS_HOST", null), //
    MAIL_SMTPS_PORT("DIRIGIBLE_MAIL_SMTPS_PORT", null), //
    MAIL_SMTPS_AUTH("DIRIGIBLE_MAIL_SMTPS_AUTH", null), //
    KEYCLOAK_AUTH_SERVER_URL("DIRIGIBLE_KEYCLOAK_AUTH_SERVER_URL", null), //
    KEYCLOAK_CLIENT_ID("DIRIGIBLE_KEYCLOAK_CLIENT_ID", null), //
    CSV_DATA_MAX_COMPARE_SIZE("DIRIGIBLE_CSV_DATA_MAX_COMPARE_SIZE", null, meta().deprecated()), //
    DESTINATION_CLIENT_ID("DIRIGIBLE_DESTINATION_CLIENT_ID", null, meta().deprecated()), //
    DESTINATION_CLIENT_SECRET("DIRIGIBLE_DESTINATION_CLIENT_SECRET", null, meta().secret()
                                                                                 .deprecated()), //
    DESTINATION_URL("DIRIGIBLE_DESTINATION_URL", null, meta().deprecated()), //
    DESTINATION_URI("DIRIGIBLE_DESTINATION_URI", null, meta().deprecated()), //
    BASIC_ENABLED("DIRIGIBLE_BASIC_ENABLED", null), //
    FTP_USERNAME("DIRIGIBLE_FTP_USERNAME", null), //
    FTP_PASSWORD("DIRIGIBLE_FTP_PASSWORD", null, meta().secret()), //
    FTP_PORT("DIRIGIBLE_FTP_PORT", null), //
    SFTP_USERNAME("DIRIGIBLE_SFTP_USERNAME", null), //
    SFTP_PASSWORD("DIRIGIBLE_SFTP_PASSWORD", null, meta().secret()), //
    SFTP_PORT("DIRIGIBLE_SFTP_PORT", null), //
    SERVER_MAXHTTPHEADERSIZE("SERVER_MAXHTTPHEADERSIZE", null, meta().group("instance")), //
    PUBLISH_DISABLED("DIRIGIBLE_PUBLISH_DISABLED", null), //
    AWS_DEFAULT_REGION("AWS_DEFAULT_REGION", null, meta().group("documents")), //
    AWS_ACCESS_KEY_ID("AWS_ACCESS_KEY_ID", null, meta().group("documents")), //
    AWS_SECRET_ACCESS_KEY("AWS_SECRET_ACCESS_KEY", null, meta().group("documents")
                                                               .secret()), //
    S3_PROVIDER("DIRIGIBLE_S3_PROVIDER", null), //
    S3_BUCKET("DIRIGIBLE_S3_BUCKET", null), //
    DATABASE_SYSTEM_DRIVER("DIRIGIBLE_DATABASE_SYSTEM_DRIVER", null), //
    DATABASE_SYSTEM_URL("DIRIGIBLE_DATABASE_SYSTEM_URL", null), //
    DATABASE_SYSTEM_USERNAME("DIRIGIBLE_DATABASE_SYSTEM_USERNAME", null), //
    DATABASE_SYSTEM_PASSWORD("DIRIGIBLE_DATABASE_SYSTEM_PASSWORD", null, meta().secret()), //
    DATABASE_SYSTEM_DIALECT("DIRIGIBLE_DATABASE_SYSTEM_DIALECT", null), //
    DATABASE_SYSTEM_DDL_AUTO("DIRIGIBLE_DATABASE_SYSTEM_DDL_AUTO", null), //
    SNOWFLAKE_DEFAULT_TABLE_TYPE("SNOWFLAKE_DEFAULT_TABLE_TYPE", null, meta().group("database")), //
    PROJECT_TYPESCRIPT("DIRIGIBLE_PROJECT_TYPESCRIPT", null), //
    MULTI_TENANT_MODE_SINGLE_USER_POOL("DIRIGIBLE_MULTI_TENANT_MODE_SINGLE_USER_POOL", null, meta().deprecated()), //
    TRACING_TASK_ENABLED("DIRIGIBLE_TRACING_TASK_ENABLED", null), //

    // ---- Keys read through a Spring placeholder or a local constant that the catalogue did not list
    // (#7759).

    COGNITO_CLIENT_ID("DIRIGIBLE_COGNITO_CLIENT_ID", null), //
    COGNITO_CLIENT_SECRET("DIRIGIBLE_COGNITO_CLIENT_SECRET", null, meta().secret()), //
    COGNITO_DOMAIN("DIRIGIBLE_COGNITO_DOMAIN", null), //
    COGNITO_GRANT_TYPE("DIRIGIBLE_COGNITO_GRANT_TYPE", "authorization_code"), //
    COGNITO_REGION_ID("DIRIGIBLE_COGNITO_REGION_ID", null), //
    COGNITO_SCOPE("DIRIGIBLE_COGNITO_SCOPE", "openid"), //
    COGNITO_USER_POOL_ID("DIRIGIBLE_COGNITO_USER_POOL_ID", null), //
    CSV_STRICT_MODE("DIRIGIBLE_CSV_STRICT_MODE", Boolean.FALSE.toString()), //
    DATABASE_DEFAULT_QUERY_LIMIT("DIRIGIBLE_DATABASE_DEFAULT_QUERY_LIMIT", "1000"), //
    DATABASE_METADATA_CACHE_TIME_LIMIT_IN_MINUTES("DIRIGIBLE_DATABASE_METADATA_CACHE_TIME_LIMIT_IN_MINUTES", "60"), //
    DESTINATIONS_INTERNAL_ROOT_FOLDER("DIRIGIBLE_DESTINATIONS_INTERNAL_ROOT_FOLDER", "target/dirigible"), //
    DESTINATIONS_INTERNAL_ROOT_FOLDER_IS_ABSOLUTE("DIRIGIBLE_DESTINATIONS_INTERNAL_ROOT_FOLDER_IS_ABSOLUTE", Boolean.FALSE.toString()), //
    DESTINATIONS_PROVIDER("DIRIGIBLE_DESTINATIONS_PROVIDER", "local"), //
    ETCD_CLIENT_ENDPOINT("DIRIGIBLE_ETCD_CLIENT_ENDPOINT", "http://localhost:2379"), //
    GITHUB_CLIENT_ID("DIRIGIBLE_GITHUB_CLIENT_ID", null), //
    GITHUB_CLIENT_SECRET("DIRIGIBLE_GITHUB_CLIENT_SECRET", null, meta().secret()), //
    GITHUB_SCOPE("DIRIGIBLE_GITHUB_SCOPE", "read:user,user:email", meta().type(ConfigType.LIST)), //
    GRAALIUM_DEBUG_PATH("DIRIGIBLE_GRAALIUM_DEBUG_PATH", "debug"), //
    GRAALIUM_DEBUG_PORT("DIRIGIBLE_GRAALIUM_DEBUG_PORT", "8081"), //
    GRAALIUM_DEBUG_SECURE("DIRIGIBLE_GRAALIUM_DEBUG_SECURE", Boolean.FALSE.toString()), //
    GRAALIUM_DEBUG_SUSPEND("DIRIGIBLE_GRAALIUM_DEBUG_SUSPEND", Boolean.TRUE.toString()), //
    GRAALIUM_ENABLE_DEBUG("DIRIGIBLE_GRAALIUM_ENABLE_DEBUG", Boolean.FALSE.toString()), //
    HOST("DIRIGIBLE_HOST", null), //
    INDEXING_MAX_RESULTS("DIRIGIBLE_INDEXING_MAX_RESULTS", "100"), //
    INDEXING_ROOT_FOLDER("DIRIGIBLE_INDEXING_ROOT_FOLDER", "target/dirigible/lucene"), //
    KEYCLOAK_CLIENT_SECRET("DIRIGIBLE_KEYCLOAK_CLIENT_SECRET", null, meta().secret()), //
    KEYCLOAK_GRANT_TYPE("DIRIGIBLE_KEYCLOAK_GRANT_TYPE", "authorization_code"), //
    PRODUCTIVE_IFRAME_ENABLED("DIRIGIBLE_PRODUCTIVE_IFRAME_ENABLED", Boolean.TRUE.toString()), //
    RABBITMQ_CLIENT_URI("DIRIGIBLE_RABBITMQ_CLIENT_URI", "127.0.0.1:5672"), //
    REDIS_CLIENT_URI("DIRIGIBLE_REDIS_CLIENT_URI", "localhost:6379"), //
    REPOSITORY_CACHE_ENABLED("DIRIGIBLE_REPOSITORY_CACHE_ENABLED", Boolean.FALSE.toString()), //
    REPOSITORY_CACHE_SIZE_LIMIT_IN_MEGABYTES("DIRIGIBLE_REPOSITORY_CACHE_SIZE_LIMIT_IN_MEGABYTES", "100"), //
    REPOSITORY_CACHE_TIME_LIMIT_IN_MINUTES("DIRIGIBLE_REPOSITORY_CACHE_TIME_LIMIT_IN_MINUTES", "10"), //
    SCHEDULER_EMAIL_SUBJECT_DISABLE("DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_DISABLE", "Job execution has been disabled: [%s]"), //
    SCHEDULER_EMAIL_SUBJECT_ENABLE("DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ENABLE", "Job execution has been enabled: [%s]"), //
    SCHEDULER_EMAIL_TEMPLATE_DISABLE("DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_DISABLE", "/job/templates/template-disable.txt"), //
    SCHEDULER_EMAIL_TEMPLATE_ENABLE("DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ENABLE", "/job/templates/template-enable.txt"), //
    SCHEDULER_LOGS_RETENTION_PERIOD("DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD", "168", meta().type(ConfigType.DURATION)), //
    SERVER_PORT("DIRIGIBLE_SERVER_PORT", "8080"), //
    SESSION_COOKIE_SAMESITE("DIRIGIBLE_SESSION_COOKIE_SAMESITE", "lax", meta().type(ConfigType.ENUM)), //
    SESSION_COOKIE_SECURE("DIRIGIBLE_SESSION_COOKIE_SECURE", Boolean.FALSE.toString()), //
    SPRING_ADMIN_CLIENT_PASSWORD("DIRIGIBLE_SPRING_ADMIN_CLIENT_PASSWORD", "admin", meta().secret()), //
    SPRING_ADMIN_CLIENT_USERNAME("DIRIGIBLE_SPRING_ADMIN_CLIENT_USERNAME", "admin"), //
    SPRING_ADMIN_SERVER_PASSWORD("DIRIGIBLE_SPRING_ADMIN_SERVER_PASSWORD", "admin", meta().secret()), //
    SPRING_ADMIN_SERVER_URL("DIRIGIBLE_SPRING_ADMIN_SERVER_URL", null), //
    SPRING_ADMIN_SERVER_USERNAME("DIRIGIBLE_SPRING_ADMIN_SERVER_USERNAME", "admin"), //
    TENANT_USERS_REQUEST_QUEUE("DIRIGIBLE_TENANT_USERS_REQUEST_QUEUE", null, meta().deprecatedBy("DIRIGIBLE_TENANT_USERS_CHANGE_QUEUE")), //
    TSC_WATCH_SERVICE_ENABLED("DIRIGIBLE_TSC_WATCH_SERVICE_ENABLED", Boolean.TRUE.toString()), //
    BRANDING_LOGO("DIRIGIBLE_BRANDING_LOGO", null);

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleConfig.class);

    /** The entries by key. */
    private static final Map<String, DirigibleConfig> BY_KEY = Arrays.stream(values())
                                                                     .collect(Collectors.toUnmodifiableMap(DirigibleConfig::getKey,
                                                                             Function.identity()));

    /** The key. */
    private final String key;

    /** The default value. */
    private final String defaultValue;

    /** The metadata. */
    private final ConfigMeta meta;

    /** The type - explicit, or inferred from the default value and the key. */
    private final ConfigType type;

    /**
     * Instantiates a new dirigible config.
     *
     * @param key the key
     * @param defaultValue the default value
     */
    DirigibleConfig(String key, String defaultValue) {
        this(key, defaultValue, meta());
    }

    /**
     * Instantiates a new dirigible config with its metadata.
     *
     * @param key the key
     * @param defaultValue the default value
     * @param meta the metadata
     */
    DirigibleConfig(String key, String defaultValue, ConfigMeta meta) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.meta = meta;
        this.type = meta.getType() != null ? meta.getType() : inferType(key, defaultValue);
    }

    /**
     * Infers the type of an entry that does not declare one: the key suffix of a time unit, a boolean
     * or whole-number default, then a URL suffix.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the type
     */
    private static ConfigType inferType(String key, String defaultValue) {
        if (key.endsWith("_SECONDS") || key.endsWith("_MILLIS") || key.endsWith("_MS") || key.endsWith("_MINUTES")) {
            return ConfigType.DURATION;
        }
        if (Boolean.TRUE.toString()
                        .equals(defaultValue)
                || Boolean.FALSE.toString()
                                .equals(defaultValue)
                || key.endsWith("_ENABLED")) {
            return ConfigType.BOOLEAN;
        }
        if (defaultValue != null && defaultValue.matches("-?\\d+")) {
            return ConfigType.INT;
        }
        if (key.endsWith("_URL") || key.endsWith("_URI")) {
            return ConfigType.URL;
        }
        return ConfigType.STRING;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * The group set explicitly on the entry. It wins over the group the key prefix resolves to.
     *
     * @return the group id, or null when the key prefix decides
     */
    public String getGroup() {
        return meta.getGroup();
    }

    /**
     * Gets the subgroup within the group.
     *
     * @return the subgroup id, or null
     */
    public String getSubgroup() {
        return meta.getSubgroup();
    }

    /**
     * Gets the type of the value.
     *
     * @return the type, never null
     */
    public ConfigType getType() {
        return type;
    }

    /**
     * Whether the value must never be shown in clear.
     *
     * @return true for passwords, secrets, tokens and API keys
     */
    public boolean isSensitive() {
        return meta.isSensitive();
    }

    /**
     * Whether a tenant may override the key through its tenant configuration.
     *
     * @return true if the key is tenant-overridable
     */
    public boolean isTenantOverridable() {
        return meta.getTenantOrder() >= 0;
    }

    /**
     * Gets the position of a tenant-overridable key in the tenant settings.
     *
     * @return the order, or -1 when the key is not tenant-overridable
     */
    public int getTenantOrder() {
        return meta.getTenantOrder();
    }

    /**
     * Whether a changed value applies only after a restart. True unless the value is known to be read
     * per call.
     *
     * @return true if a restart is required
     */
    public boolean isRestartRequired() {
        return meta.isRestartRequired();
    }

    /**
     * Whether the key is deprecated: read nowhere, renamed or misspelled. A deployment that sets one is
     * warned at startup.
     *
     * @return true if deprecated
     */
    public boolean isDeprecated() {
        return meta.isDeprecated();
    }

    /**
     * Gets the key that replaces this one.
     *
     * @return the replacement key, or null
     */
    public String getDeprecatedBy() {
        return meta.getDeprecatedBy();
    }

    /**
     * Whether the key is a misspelling of {@link #getDeprecatedBy()}: a value set under either spelling
     * is read under both.
     *
     * @return true for a misspelled alias
     */
    public boolean isAlias() {
        return meta.isAlias();
    }

    /**
     * Finds the entry of a key.
     *
     * @param key the key
     * @return the entry, or empty when the key is not catalogued
     */
    public static Optional<DirigibleConfig> fromKey(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    /**
     * Gets the keys a tenant may override, in the order the tenant settings show them.
     *
     * @return the tenant-overridable keys
     */
    public static List<String> tenantOverridableKeys() {
        return Arrays.stream(values())
                     .filter(DirigibleConfig::isTenantOverridable)
                     .sorted(Comparator.comparingInt(DirigibleConfig::getTenantOrder))
                     .map(DirigibleConfig::getKey)
                     .toList();
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

    /**
     * Gets a comma-separated value as a list: the entries trimmed, blank entries dropped, an unset
     * value an empty list.
     *
     * @return the list value, never {@code null}
     */
    public List<String> getListValue() {
        String stringValue = getStringValue();
        if (StringUtils.isBlank(stringValue)) {
            return List.of();
        }
        return Arrays.stream(stringValue.split(","))
                     .map(String::trim)
                     .filter(value -> !value.isEmpty())
                     .toList();
    }

    /**
     * Whether the value was configured explicitly, as opposed to the default value applying.
     *
     * @return true when the key is set in any configuration layer
     */
    public boolean isSet() {
        return Configuration.get(key) != null;
    }
}
