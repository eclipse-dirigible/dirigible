/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.access;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The single authority on who may read or write a CMS path.
 * <p>
 * Grants are tenant data, kept in {@link CmsAccessStore} and cached per tenant. The cache is
 * dropped on every write, so a grant or a revoke takes effect on the next request - the mechanism
 * this replaces needed up to a minute, a synchronization round and two caches to propagate the same
 * decision.
 * <p>
 * The cache is per node. A clustered deployment would need an external invalidation signal, exactly
 * as the per-tenant configuration cache does today; that is a known limitation rather than an
 * oversight.
 */
@Component
public class CmsAccessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmsAccessService.class);

    /** Cache key for the single-tenant case and for reads outside a tenant scope. */
    private static final String DEFAULT_TENANT = "__default";

    private final CmsAccessStore store;
    private final TenantContext tenantContext;
    private final Map<String, List<CmsAccessGrant>> cache = new ConcurrentHashMap<>();

    CmsAccessService(CmsAccessStore store, TenantContext tenantContext) {
        this.store = store;
        this.tenantContext = tenantContext;
    }

    /**
     * Whether a caller holding the given roles may act on a path.
     *
     * @param path the CMS path
     * @param method {@code READ} or {@code WRITE}
     * @param holdsRole tells whether the caller holds a role
     * @return true when allowed
     */
    public boolean isAllowed(String path, String method, Predicate<String> holdsRole) {
        List<CmsAccessGrant> grants = grants();
        if (grants.isEmpty()) {
            return true;
        }
        if (CmsAccessGrant.METHOD_WRITE.equalsIgnoreCase(method)
                && !CmsAccessResolution.isAllowed(grants, path, CmsAccessGrant.METHOD_READ, holdsRole)) {
            return false; // writing implies reading
        }
        return CmsAccessResolution.isAllowed(grants, path, method, holdsRole);
    }

    /**
     * The grants that decide a path, for the management surface: those declared ON the path and those
     * inherited from an ancestor.
     *
     * @param path the CMS path
     * @return the effective grants
     */
    public EffectiveGrants effectiveGrants(String path) {
        String normalized = CmsAccessResolution.normalize(path);
        List<CmsAccessGrant> own = grants().stream()
                                           .filter(grant -> CmsAccessResolution.normalize(grant.path())
                                                                               .equals(normalized))
                                           .toList();
        List<CmsAccessGrant> inherited = grants().stream()
                                                 .filter(grant -> !CmsAccessResolution.normalize(grant.path())
                                                                                      .equals(normalized))
                                                 .filter(grant -> CmsAccessResolution.ancestry(normalized)
                                                                                     .contains(CmsAccessResolution.normalize(grant.path())))
                                                 .toList();
        return new EffectiveGrants(normalized, own, inherited);
    }

    /**
     * Grants a role read or write access to a path.
     *
     * @param path the CMS path
     * @param method {@code READ} or {@code WRITE}
     * @param role the role
     * @param user who is granting
     * @throws SQLException when the write fails
     */
    public void grant(String path, String method, String role, String user) throws SQLException {
        store.add(new CmsAccessGrant(CmsAccessResolution.normalize(path), method.toUpperCase(), role), user);
        invalidate();
        LOGGER.info("Granted [{}] on [{}] to role [{}] by [{}]", method, path, role, user);
    }

    /**
     * Revokes a grant.
     *
     * @param path the CMS path
     * @param method {@code READ} or {@code WRITE}
     * @param role the role
     * @param user who is revoking
     * @throws SQLException when the write fails
     */
    public void revoke(String path, String method, String role, String user) throws SQLException {
        store.remove(new CmsAccessGrant(CmsAccessResolution.normalize(path), method.toUpperCase(), role));
        invalidate();
        LOGGER.info("Revoked [{}] on [{}] from role [{}] by [{}]", method, path, role, user);
    }

    /** Drops the current tenant's cached grants. */
    public void invalidate() {
        cache.remove(tenantKey());
    }

    /**
     * The current tenant's grants, loaded on first use.
     * <p>
     * A read failure yields NO grants rather than an exception: the CMS is open by default, and a store
     * that cannot be read must not take the Documents surface down with it. It is logged at error level
     * so the cause is visible.
     *
     * @return the grants
     */
    List<CmsAccessGrant> grants() {
        return cache.computeIfAbsent(tenantKey(), key -> {
            try {
                return List.copyOf(store.readAll());
            } catch (SQLException e) {
                LOGGER.error("Failed to read the CMS access grants for tenant [{}]", key, e);
                return List.of();
            }
        });
    }

    private String tenantKey() {
        try {
            return tenantContext.isInitialized() ? tenantContext.getCurrentTenant()
                                                                .getId()
                    : DEFAULT_TENANT;
        } catch (RuntimeException e) {
            LOGGER.debug("No tenant in scope; using the default cache key", e);
            return DEFAULT_TENANT;
        }
    }

    /**
     * What decides a path, split into what is declared on it and what it inherits.
     *
     * @param path the normalized path
     * @param own the grants declared on the path itself
     * @param inherited the grants inherited from an ancestor
     */
    public record EffectiveGrants(String path, List<CmsAccessGrant> own, List<CmsAccessGrant> inherited) {

        /**
         * Everything that bears on the path, declared and inherited together.
         *
         * @return the union of the two lists
         */
        public List<CmsAccessGrant> all() {
            List<CmsAccessGrant> all = new java.util.ArrayList<>(own);
            all.addAll(inherited);
            return all;
        }
    }
}
