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
 * an owner publish invitations nobody receives.
 */
@Component
@Conditional(TenantUsersEnabledCondition.class)
class TenantUsersConfigValidator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantUsersConfigValidator.class);

    /**
     * Validates the configuration.
     */
    TenantUsersConfigValidator() {
        if (TenantResolutionStrategy.fromConfiguration() != TenantResolutionStrategy.TOKEN_GROUPS) {
            throw invalid(DirigibleConfig.TENANT_RESOLUTION_STRATEGY,
                    "tenant roles such as the owner role exist only under " + TenantResolutionStrategy.TOKEN_GROUPS);
        }
        String queue = TenantUsersSettings.requestQueue();
        if (queue == null || queue.isBlank() || queue.trim()
                                                     .equals("global:")) {
            throw invalid(DirigibleConfig.TENANT_USERS_REQUEST_QUEUE, "it names the queue invitations are published to");
        }
        String broker = DirigibleConfig.MESSAGING_BROKER_URL.getStringValue();
        if (broker == null || broker.isBlank()) {
            LOGGER.warn(
                    "Tenant users management publishes invitations to [{}] on the EMBEDDED broker - no external provisioning "
                            + "system can receive them. Set [{}] to use an external broker.",
                    LogSanitizer.sanitize(queue), DirigibleConfig.MESSAGING_BROKER_URL.getKey());
        }
        if (DirigibleConfig.TRIAL_ENABLED.getBooleanValue()) {
            LOGGER.warn("Trial mode grants no tenant role, so no user can manage tenant users while [{}] is on.",
                    DirigibleConfig.TRIAL_ENABLED.getKey());
        }
        LOGGER.info("Tenant users management is enabled: request queue [{}].", LogSanitizer.sanitize(queue));
    }

    /**
     * An invalid configuration.
     *
     * @param config the key
     * @param reason why
     * @return the exception
     */
    private static InvalidConfigException invalid(DirigibleConfig config, String reason) {
        String message = "Invalid configuration [" + config.getKey() + "] while [" + DirigibleConfig.TENANT_USERS_ENABLED.getKey()
                + "] is on: " + reason;
        LOGGER.error(LogSanitizer.sanitize(message));
        return new InvalidConfigException(message, config.getKey());
    }
}
