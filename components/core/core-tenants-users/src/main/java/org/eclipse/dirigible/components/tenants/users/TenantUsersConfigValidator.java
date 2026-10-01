/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Refuses to start tenant users management on a configuration where it could not work. It validates
 * in its constructor on purpose: a half-usable setup must abort the context refresh rather than let
 * an owner publish changes nobody receives, or wait for a list nobody can write.
 */
@Component
@Conditional(TenantUsersEnabledCondition.class)
class TenantUsersConfigValidator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantUsersConfigValidator.class);

    /** The key v1 read the queue from - set alone, it means a configuration that was never migrated. */
    static final String OLD_QUEUE_KEY = "DIRIGIBLE_TENANT_USERS_REQUEST_QUEUE";

    /** The marker of a destination shared across deployments. */
    private static final String GLOBAL = "global:";

    /**
     * Validates the configuration.
     */
    TenantUsersConfigValidator() {
        if (TenantResolutionStrategy.fromConfiguration() != TenantResolutionStrategy.TOKEN_GROUPS) {
            throw invalid(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey(),
                    "tenant roles such as the owner role exist only under " + TenantResolutionStrategy.TOKEN_GROUPS);
        }
        String queue = TenantUsersSettings.changeQueue();
        if (isBlank(queue)) {
            String old = Configuration.get(OLD_QUEUE_KEY);
            if (!isBlank(old)) {
                throw invalid(OLD_QUEUE_KEY, "the change requests changed shape, so the key was renamed - set ["
                        + DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey() + "] instead, to a queue whose consumer reads them");
            }
            throw invalid(DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey(),
                    "it names the queue the owners' change requests are published to");
        }
        String trimmed = queue.trim();
        if (!trimmed.startsWith(GLOBAL) || trimmed.length() == GLOBAL.length()) {
            throw invalid(DirigibleConfig.TENANT_USERS_CHANGE_QUEUE.getKey(), "it must be a [" + GLOBAL
                    + "] destination - any other queue is tenant-prefixed, and no provisioning system consumes it");
        }
        if (!DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getBooleanValue()) {
            throw invalid(DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getKey(),
                    "the provisioning system writes the users through the tenant provisioning API - without it the list stays empty");
        }
        String broker = DirigibleConfig.MESSAGING_BROKER_URL.getStringValue();
        if (isBlank(broker)) {
            LOGGER.warn(
                    "Tenant users management publishes change requests to [{}] on the EMBEDDED broker - no external provisioning "
                            + "system can receive them. Set [{}] to use an external broker.",
                    LogSanitizer.sanitize(queue), DirigibleConfig.MESSAGING_BROKER_URL.getKey());
        }
        if (DirigibleConfig.TRIAL_ENABLED.getBooleanValue()) {
            LOGGER.warn("Trial mode grants no tenant role, so no user can manage tenant users while [{}] is on.",
                    DirigibleConfig.TRIAL_ENABLED.getKey());
        }
        LOGGER.info("Tenant users management is enabled: change queue [{}].", LogSanitizer.sanitize(queue));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * An invalid configuration.
     *
     * @param key the key
     * @param reason why
     * @return the exception
     */
    private static InvalidConfigException invalid(String key, String reason) {
        String message =
                "Invalid configuration [" + key + "] while [" + DirigibleConfig.TENANT_USERS_ENABLED.getKey() + "] is on: " + reason;
        LOGGER.error(LogSanitizer.sanitize(message));
        return new InvalidConfigException(message, key);
    }
}
