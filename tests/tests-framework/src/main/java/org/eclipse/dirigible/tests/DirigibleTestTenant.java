/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.UUID;

public class DirigibleTestTenant {
    private static final String LOCALHOST = "localhost";

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
}
