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

import java.util.List;
import java.util.Optional;

/**
 * Places a configuration key in its {@link ConfigGroup} by prefix.
 * <p>
 * The rules are evaluated in order and the first match wins, so a more specific prefix is listed
 * before the general one it would otherwise fall under: {@code FLOWABLE_MAIL_} lands in mail before
 * {@code FLOWABLE_} takes it to BPM, and {@code JAVASCRIPT_GRAALVM_DEBUGGER_PORT} in the developer
 * tools before {@code JAVASCRIPT_} takes it to the runtimes. Prefixes are matched after a leading
 * {@code DIRIGIBLE_}, which is optional. A group set explicitly on the key's
 * {@link DirigibleConfig} entry wins over these rules, and so does its subgroup.
 */
public final class ConfigGroups {

    private static final String KEY_PREFIX = "DIRIGIBLE_";

    /** The subgroup of the mail group holding the process (Flowable) mail server. */
    public static final String SUBGROUP_PROCESS = "process";

    private record Rule(ConfigGroup group, String subgroup, List<String> prefixes) {

        Rule(ConfigGroup group, String... prefixes) {
            this(group, null, List.of(prefixes));
        }

        boolean matches(String name) {
            return prefixes.stream()
                           .anyMatch(name::startsWith);
        }
    }

    private static final List<Rule> RULES = List.of( //
            new Rule(ConfigGroup.AI, "INTENT_AI_"), //
            new Rule(ConfigGroup.DEVTOOLS, "JAVA_LSP_", "JAVA_DEBUG_", "TERMINAL_", "GRAALIUM_", "TSC_WATCH", "EXEC_COMMAND_",
                    "JAVASCRIPT_GRAALVM_DEBUGGER_PORT"), //
            new Rule(ConfigGroup.RUNTIMES, "JAVASCRIPT_", "JAVA_RECONCILE", "DEPENDENCIES_", "MAVEN_", "NATIVE_APP", "PROJECT_TYPESCRIPT",
                    "GENERATE_", "CORE_MODULE_SIGNATURE"), //
            new Rule(ConfigGroup.BRANDING, "BRANDING_", "THEME_"), //
            new Rule(ConfigGroup.LOCALE, "APPLICATION_LANGUAGES", "APPLICATION_COUNTRY"), //
            new Rule(ConfigGroup.TENANCY, "MULTI_TENANT", "TENANT_", "TENANTS_", "APP_ID"), //
            new Rule(ConfigGroup.WEB, "CORS_", "SECURITY_CROSS", "SECURITY_FRAME", "SECURITY_HSTS", "SECURITY_REFERRER", "SECURITY_CONTENT",
                    "SECURITY_PERMISSIONS", "SESSION_COOKIE", "PRODUCTIVE_IFRAME"), //
            new Rule(ConfigGroup.AUTH, "BASIC_", "OAUTH", "KEYCLOAK_", "COGNITO_", "GITHUB_", "ANONYMOUS_", "SECURITY_LOGIN", "ACT_AS",
                    "JWT"), //
            new Rule(ConfigGroup.MAIL, SUBGROUP_PROCESS, List.of("FLOWABLE_MAIL_")), //
            new Rule(ConfigGroup.MAIL, "MAIL_"), //
            new Rule(ConfigGroup.BPM, "FLOWABLE_", "BPM_"), //
            new Rule(ConfigGroup.JOBS, "SCHEDULER_", "JOB_", "TRACING_"), //
            new Rule(ConfigGroup.MESSAGING, "MESSAGING_", "RABBITMQ_", "REDIS_", "EVENT_OUTBOX"), //
            new Rule(ConfigGroup.DOCUMENTS, "CMS_", "DOCUMENTS_", "S3_", "MS_SHAREPOINT", "PRINT_", "AWS_"), //
            new Rule(ConfigGroup.DATABASE, "DATABASE_", "LEAKED_", "SNOWFLAKE_", "MONGODB_", "PERSISTENCE_"), //
            new Rule(ConfigGroup.REPOSITORY, "REPOSITORY_", "MASTER_", "REGISTRY_", "GIT_", "INDEXING_"), //
            new Rule(ConfigGroup.SYNC, "SYNCHRONIZER_", "PUBLISH_", "CSV_", "READINESS_"), //
            new Rule(ConfigGroup.INTEGRATIONS, "DESTINATION", "CONNECTIVITY_", "FTP_", "SFTP_", "SPARK_", "ETCD_"), //
            new Rule(ConfigGroup.INSTANCE, "PRODUCT_", "INSTANCE_", "HOME_URL", "APP_BASE_URL", "SERVER_", "HOST", "TRIAL_", "OPERATIONS_",
                    "SPRING_ADMIN"));

    private ConfigGroups() {}

    /**
     * The group a configuration key belongs to.
     *
     * @param key the configuration key, with or without the {@code DIRIGIBLE_} prefix
     * @return the group, {@link ConfigGroup#OTHER} when no rule matches
     */
    public static ConfigGroup resolve(String key) {
        Optional<ConfigGroup> explicit = explicitGroup(key);
        if (explicit.isPresent()) {
            return explicit.get();
        }
        Rule rule = rule(key);
        return rule == null ? ConfigGroup.OTHER : rule.group();
    }

    /**
     * The subgroup of a configuration key within its group, such as {@link #SUBGROUP_PROCESS} for the
     * process mail server keys.
     *
     * @param key the configuration key
     * @return the subgroup id, or {@code null} when the key sits in its group directly
     */
    public static String subgroup(String key) {
        Optional<DirigibleConfig> entry = catalogued(key);
        if (entry.isPresent() && entry.get()
                                      .getSubgroup() != null) {
            return entry.get()
                        .getSubgroup();
        }
        if (explicitGroup(key).isPresent()) {
            return null;
        }
        Rule rule = rule(key);
        return rule == null ? null : rule.subgroup();
    }

    private static Optional<ConfigGroup> explicitGroup(String key) {
        return catalogued(key).map(DirigibleConfig::getGroup)
                              .flatMap(ConfigGroup::fromId);
    }

    private static Optional<DirigibleConfig> catalogued(String key) {
        return key == null ? Optional.empty() : DirigibleConfig.fromKey(key);
    }

    private static Rule rule(String key) {
        if (key == null) {
            return null;
        }
        String name = key.startsWith(KEY_PREFIX) ? key.substring(KEY_PREFIX.length()) : key;
        for (Rule rule : RULES) {
            if (rule.matches(name)) {
                return rule;
            }
        }
        return null;
    }

}
