/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.tenant;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.dirigible.commons.config.DirigibleConfig;

import java.util.UUID;

public class DirigibleTestTenant {

    private static final String LOCALHOST = "localhost";

    private static final String DEFAULT_TENANT_ID = "default-tenant";
    private static final String DEFAULT_TENANT_NAME = "default-tenant";
    private static final String DEFAULT_TENANT_SUBDOMAIN = "default";

    private final String name;
    private final boolean defaultTenant;
    private final String id;
    private final String subdomain;
    private final String username;
    private final String password;

    public DirigibleTestTenant(String name) {
        this(false, //
                name, //
                UUID.randomUUID()
                    .toString(), //
                RandomStringUtils.randomAlphabetic(10)
                                 .toLowerCase(), //
                UUID.randomUUID()
                    .toString(), //
                UUID.randomUUID()
                    .toString());
    }

    public DirigibleTestTenant(boolean defaultTenant, String name, String id, String subdomain, String username, String password) {
        this.defaultTenant = defaultTenant;
        this.name = name;
        this.id = id;
        this.subdomain = subdomain;
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public String getHost() {
        return isDefaultTenant() ? LOCALHOST : (subdomain + "." + LOCALHOST);
    }

    public boolean isDefaultTenant() {
        return defaultTenant;
    }

    @Override
    public String toString() {
        return "DirigibleTestTenant{" + "name='" + name + '\'' + ", defaultTenant=" + defaultTenant + ", id='" + id + '\'' + ", subdomain='"
                + subdomain + '\'' + ", username='" + username + '\'' + ", password='" + password + '\'' + '}';
    }

    public static DirigibleTestTenant createDefaultTenant() {
        return new DirigibleTestTenant(true, //
                DEFAULT_TENANT_NAME, //
                DEFAULT_TENANT_ID, //
                DEFAULT_TENANT_SUBDOMAIN, //
                DirigibleConfig.BASIC_ADMIN_USERNAME.getFromBase64Value(), //
                DirigibleConfig.BASIC_ADMIN_PASS.getFromBase64Value());
    }
}
