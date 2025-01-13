/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.endpoint;

/**
 * The Class TenantParameter.
 */
public class TenantParameter {

    /** The name. */
    private String name;

    /** The subdomain. */
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
     * @param name the name to set
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
     * @param subdomain the subdomain to set
     */
    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

}
