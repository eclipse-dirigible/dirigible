/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Whether this deployment offers the tenant data export.
 */
public class TenantExportEnabledCondition implements Condition {

    /**
     * Matches.
     *
     * @param context the context
     * @param metadata the metadata
     * @return true when {@code DIRIGIBLE_TENANT_EXPORT_ENABLED} is on
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return DirigibleConfig.TENANT_EXPORT_ENABLED.getBooleanValue();
    }
}
