/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant.groups;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a user's identity provider groups as tenant assignments for one application.
 *
 * <p>
 * A tenant-bearing group is named {@code <tenantId>.<appId>.<role>}, e.g.
 * {@code acme.library.Owner}. The tenant id and the application id must not contain a dot; the role
 * may (so {@code acme.library.Tenant.Owner} grants the role {@code Tenant.Owner}). Groups of other
 * applications are ignored - one identity provider serves the whole fleet - and groups that are not
 * tenant-bearing at all (plain staff groups such as {@code DEVELOPER}) become global roles.
 *
 * <p>
 * The logic is intentionally free of any Spring, servlet or OAuth2 types, so every identity
 * provider configuration can reuse it and it can be unit tested in isolation.
 */
public final class TenantGroupsParser {

    /** Tenant id and application id may not contain a dot; the role may. */
    private static final Pattern TENANT_GROUP_PATTERN = Pattern.compile("^(?<tenant>[^.]+)\\.(?<app>[^.]+)\\.(?<role>.+)$");

    private TenantGroupsParser() {}

    /**
     * Parses the groups of a user into the tenants and roles they grant in this application.
     *
     * @param groups the raw group names, e.g. from the configured groups claim; may be {@code null} or
     *        contain {@code null} entries
     * @param appId the id of this application, as it appears in the group names; must not be blank
     * @return the assignments, never {@code null}
     * @throws IllegalArgumentException if {@code appId} is blank - without it no group can be
     *         attributed to a tenant, which is a misconfiguration rather than an empty result
     */
    public static UserTenantAssignments parse(Collection<String> groups, String appId) {
        if (appId == null || appId.isBlank()) {
            throw new IllegalArgumentException("The application id is required to parse tenant groups");
        }
        if (groups == null || groups.isEmpty()) {
            return UserTenantAssignments.empty();
        }
        String applicationId = appId.trim();
        Map<String, Set<String>> tenantRoles = new LinkedHashMap<>();
        Set<String> globalRoles = new LinkedHashSet<>();

        for (String group : groups) {
            if (group == null || group.isBlank()) {
                continue;
            }
            String groupName = group.trim();
            Matcher matcher = TENANT_GROUP_PATTERN.matcher(groupName);
            if (!matcher.matches()) {
                globalRoles.add(groupName);
                continue;
            }
            if (!applicationId.equals(matcher.group("app"))) {
                continue;
            }
            tenantRoles.computeIfAbsent(matcher.group("tenant"), tenantId -> new LinkedHashSet<>())
                       .add(matcher.group("role"));
        }
        return new UserTenantAssignments(tenantRoles, globalRoles);
    }
}
