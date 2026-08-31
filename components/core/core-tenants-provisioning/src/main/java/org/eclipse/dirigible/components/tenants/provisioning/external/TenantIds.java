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

import java.util.regex.Pattern;

/**
 * What a caller-supplied tenant id may look like.
 *
 * <p>
 * The id is supplied by the caller rather than generated, because one tenant is the same customer
 * in every application of a landscape, and it then travels into places that constrain its alphabet:
 * it is the default subdomain, it is the prefix of the tenant's data source name, and - under the
 * token groups resolution strategy - it is the first segment of the identity provider group name
 * {@code <tenantId>.<appId>.<role>}. A dot would make that group name unparseable, so a tenant
 * granted by such a group could never be entered.
 *
 * <p>
 * A DNS label covers all three: letters, digits and inner hyphens, at most 63 characters. Rejecting
 * at registration is the only place where the answer is still cheap - afterwards the id is spread
 * across the identity provider, the database and the data source registry.
 */
final class TenantIds {

    /** A DNS label: no dots, no leading or trailing hyphen, 1 to 63 characters. */
    private static final Pattern VALID = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$");

    /**
     * Checks if the given tenant id is valid.
     *
     * @param tenantId the tenant id, may be null
     * @return true, if the id may be used as a tenant id
     */
    static boolean isValid(String tenantId) {
        return tenantId != null && VALID.matcher(tenantId)
                                        .matches();
    }

    /**
     * The reason a caller gets when the id is refused.
     *
     * @param tenantId the refused tenant id
     * @return the message
     */
    static String invalidMessage(String tenantId) {
        return "Invalid tenant id [" + tenantId
                + "]. A tenant id must be a DNS label - letters, digits and inner hyphens, at most 63 characters - and must not contain a dot,"
                + " since it is also the tenant's subdomain, the prefix of its data source name and the first segment of the identity provider"
                + " group names <tenantId>.<appId>.<role>.";
    }

    private TenantIds() {}
}
