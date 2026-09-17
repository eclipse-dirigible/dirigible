/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when the deployment opted into the tenant provisioning API with
 * {@link DirigibleConfig#TENANT_PROVISIONING_API_ENABLED}.
 *
 * <p>
 * Every bean of this component carries it, so a deployment that did not opt in has no endpoint, no
 * service and no security rule from here at all - the API is absent rather than merely refusing.
 * That is the isolation this feature rests on: it accepts real database credentials.
 *
 * <p>
 * The decision is read from Dirigible's own configuration rather than through
 * {@code @ConditionalOnProperty}: Dirigible resolves a key across its own layers (runtime, tenant,
 * environment, deployment, module) and a value placed there programmatically - which is how the
 * integration tests switch the API on - never reaches the Spring {@code Environment}.
 */
public class TenantProvisioningApiEnabledCondition implements Condition {

    /**
     * Matches.
     *
     * @param context the context
     * @param metadata the metadata
     * @return true, if the tenant provisioning API is enabled
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getBooleanValue();
    }
}
