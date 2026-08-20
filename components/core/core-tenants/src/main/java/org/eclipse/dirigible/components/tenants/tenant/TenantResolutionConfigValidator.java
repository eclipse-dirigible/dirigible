/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.tenant;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Refuses to start on a tenant resolution configuration that cannot work.
 *
 * <p>
 * The validation runs in the constructor, so an inconsistent configuration aborts the context
 * refresh before the instance accepts any traffic. Serving requests with a half-usable resolution
 * setup is worse than not starting: users would silently land in the wrong tenant, or in no tenant
 * at all.
 *
 * <p>
 * The class is public because its integration test lives in another module.
 */
@Component
public class TenantResolutionConfigValidator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantResolutionConfigValidator.class);

    /**
     * Validates the configuration.
     *
     * @throws InvalidConfigException if the configured combination cannot resolve tenants
     */
    public TenantResolutionConfigValidator() {
        TenantResolutionStrategy strategy = TenantResolutionStrategy.fromConfiguration();
        if (strategy != TenantResolutionStrategy.TOKEN_GROUPS) {
            LOGGER.debug("Tenant resolution strategy is [{}].", strategy);
            return;
        }
        validateTokenGroupsStrategy();
        LOGGER.info("Tenant resolution strategy is [{}] for application [{}], groups claim [{}].", strategy,
                DirigibleConfig.APP_ID.getStringValue(), DirigibleConfig.TENANT_GROUPS_CLAIM.getStringValue());
    }

    private void validateTokenGroupsStrategy() {
        String appId = DirigibleConfig.APP_ID.getStringValue();
        if (appId == null || appId.isBlank()) {
            throw invalidConfig(DirigibleConfig.APP_ID, "it is the application id in the group names <tenantId>.<appId>.<role>");
        }
        if (appId.contains(".")) {
            throw invalidConfig(DirigibleConfig.APP_ID,
                    "it must not contain a dot - the group name <tenantId>.<appId>.<role> would not be parseable, so no group could ever grant a tenant role. Configured value: ["
                            + appId + "]");
        }
        if (!DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getBooleanValue()) {
            throw invalidConfig(DirigibleConfig.MULTI_TENANT_MODE_ENABLED,
                    "tenants cannot be resolved from token groups while the platform runs in single tenant mode");
        }
        if (DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED.getBooleanValue()) {
            throw invalidConfig(DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED,
                    "it is the legacy tenant model based on the custom:tenant claim, which the token groups strategy replaces. The two are mutually exclusive");
        }
        String groupsClaim = DirigibleConfig.TENANT_GROUPS_CLAIM.getStringValue();
        if (groupsClaim == null || groupsClaim.isBlank()) {
            throw invalidConfig(DirigibleConfig.TENANT_GROUPS_CLAIM, "it is the token claim the tenant groups are read from");
        }
    }

    private InvalidConfigException invalidConfig(DirigibleConfig config, String reason) {
        String message = "Invalid configuration [" + config.getKey() + "] while [" + DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey()
                + "] is [" + TenantResolutionStrategy.TOKEN_GROUPS + "]: " + reason;
        LOGGER.error(message);
        return new InvalidConfigException(message, config.getKey());
    }
}
