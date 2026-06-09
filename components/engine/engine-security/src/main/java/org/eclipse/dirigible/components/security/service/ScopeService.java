/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.security.domain.Scope;
import org.eclipse.dirigible.components.security.repository.ScopeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class ScopeService.
 *
 * <p>
 * Owns the {@code *.scopes} artefacts that map an OAuth2 scope name to one or more Dirigible role
 * names. The scope-to-roles view is consulted on every M2M (Bearer) request, so it is cached in
 * memory and invalidated whenever the underlying artefacts change through synchronization.
 */
@Service
@Transactional
public class ScopeService extends BaseArtefactService<Scope, Long> {

    /** Cached scope-name to role-names mappings; {@code null} means the cache has to be rebuilt. */
    private volatile Map<String, Set<String>> scopeRolesCache;

    /**
     * Instantiates a new scope service.
     *
     * @param repository the repository
     */
    public ScopeService(ScopeRepository repository) {
        super(repository);
    }

    /**
     * Returns the scope-name to role-names mappings declared by the synchronized {@code *.scopes}
     * artefacts.
     *
     * <p>
     * The result is cached and rebuilt lazily after a change. A scope absent from this map is resolved
     * 1:1 (the scope name is the role name) by the caller.
     *
     * @return an immutable-by-convention snapshot of the scope-to-roles mappings (never {@code null})
     */
    @Transactional(readOnly = true)
    public Map<String, Set<String>> getScopeRolesMappings() {
        Map<String, Set<String>> cache = scopeRolesCache;
        if (cache == null) {
            cache = buildScopeRolesMappings();
            scopeRolesCache = cache;
        }
        return cache;
    }

    /**
     * Builds the scope-to-roles mappings from the persisted scopes.
     *
     * @return the freshly built mappings
     */
    private Map<String, Set<String>> buildScopeRolesMappings() {
        Map<String, Set<String>> mappings = new HashMap<>();
        for (Scope scope : getAll()) {
            if (scope.getScope() == null || scope.getRoles() == null || scope.getRoles()
                                                                             .isEmpty()) {
                continue;
            }
            mappings.computeIfAbsent(scope.getScope(), key -> new HashSet<>())
                    .addAll(scope.getRoles());
        }
        return mappings;
    }

    /**
     * Invalidates the cached scope-to-roles mappings.
     */
    public void clearCache() {
        scopeRolesCache = null;
    }

    /**
     * Save.
     *
     * @param scope the scope
     * @return the scope
     */
    @Override
    public Scope save(Scope scope) {
        Scope saved = super.save(scope);
        clearCache();
        return saved;
    }

    /**
     * Delete.
     *
     * @param scope the scope
     */
    @Override
    public void delete(Scope scope) {
        super.delete(scope);
        clearCache();
    }
}
