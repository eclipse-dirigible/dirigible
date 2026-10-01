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

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The value rules every tenant-users path shares: what an email and a tenant id look like, and how
 * an email is normalized before it is stored or compared.
 */
final class TenantUserRules {

    /** An email address: one {@code @}, a dotted domain, no whitespace. */
    static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$");

    /**
     * A tenant id: letters, digits and hyphens, not starting or ending with a hyphen. Copied, because
     * the tenant provisioning API's own rule is package-private.
     */
    static final Pattern TENANT_ID = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$");

    /** The longest email, and the longest name of the person behind a "by" field. */
    static final int EMAIL_MAX = 320;

    private TenantUserRules() {}

    /**
     * Normalizes an email: trimmed and lower-cased.
     *
     * @param email the email
     * @return the normalized email
     */
    static String normalize(String email) {
        return Objects.requireNonNull(email, "email")
                      .trim()
                      .toLowerCase(Locale.ROOT);
    }

    /**
     * Whether a normalized email is one.
     *
     * @param email the email
     * @return whether it is an email address
     */
    static boolean isEmail(String email) {
        return email != null && !email.isEmpty() && email.length() <= EMAIL_MAX && EMAIL.matcher(email)
                                                                                        .matches();
    }

    /**
     * Whether a tenant id is one.
     *
     * @param tenantId the tenant id
     * @return whether it is valid
     */
    static boolean isTenantId(String tenantId) {
        return tenantId != null && TENANT_ID.matcher(tenantId)
                                            .matches();
    }
}
