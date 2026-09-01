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

import jakarta.validation.constraints.NotBlank;

/**
 * The body of a tenant registration.
 */
public class RegisterTenantParameter {

    /** The human readable name of the tenant. */
    @NotBlank(message = "A tenant name is required")
    private String name;

    /**
     * The tenant subdomain. Optional - it defaults to the tenant id.
     *
     * <p>
     * The platform requires every tenant to carry a unique subdomain, because that is how the
     * {@code SUBDOMAIN} resolution strategy finds it. Deployments that resolve tenants from token
     * groups instead serve every tenant from one host and never look at it, which is why callers may
     * leave it out; the parameter exists so the API stays usable for a {@code SUBDOMAIN} deployment.
     */
    private String subdomain;

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the subdomain.
     *
     * @return the subdomain
     */
    public String getSubdomain() {
        return subdomain;
    }

    /**
     * Sets the subdomain.
     *
     * @param subdomain the new subdomain
     */
    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }
}
