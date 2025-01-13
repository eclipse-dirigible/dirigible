/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.tenant;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class JobNameCreator.
 */
@Component
public class JobNameCreator {

    /** The Constant TENANT_JOB_NAME_REGEX. */
    private static final String TENANT_JOB_NAME_REGEX = ".+###(.+)";

    /** The Constant TENANT_JOB_NAME_PATTERN. */
    private static final Pattern TENANT_JOB_NAME_PATTERN = Pattern.compile(TENANT_JOB_NAME_REGEX);

    /** The tenant context. */
    private final TenantContext tenantContext;

    /**
     * Instantiates a new job name creator.
     *
     * @param tenantContext the tenant context
     */
    JobNameCreator(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * To tenant name.
     *
     * @param name the name
     * @return the string
     */
    public String toTenantName(String name) {
        Tenant currentTenant = tenantContext.getCurrentTenant();
        return currentTenant.isDefault() ? name : currentTenant.getId() + "###" + name;
    }

    /**
     * From tenant name.
     *
     * @param jobName the job name
     * @return the string
     */
    public String fromTenantName(String jobName) {
        Matcher matcher = TENANT_JOB_NAME_PATTERN.matcher(jobName);
        return matcher.matches() ? matcher.group(1) : jobName;
    }
}
