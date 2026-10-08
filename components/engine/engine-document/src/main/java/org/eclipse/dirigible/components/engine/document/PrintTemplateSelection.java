/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.springframework.stereotype.Component;

/**
 * Which print template a document type prints with, per language - a tenant configuration, never
 * state in the CMS. The key is {@code DIRIGIBLE_PRINT_TEMPLATE_<ENTITY>_<LANG>} (e.g.
 * {@code DIRIGIBLE_PRINT_TEMPLATE_SALESINVOICE_EN}) and the value a template reference
 * ({@code acme-blue}, or {@code standard@1.28.0} to pin a shipped version), so the selection is per
 * tenant, cached by the configuration store, and editable raw in Settings → Configurations as well
 * as from the Print templates page.
 */
@Component
class PrintTemplateSelection {

    /** The key family the tenant configuration policy admits by prefix. */
    static final String KEY_PREFIX = "DIRIGIBLE_PRINT_TEMPLATE_";

    private final TenantConfigurationService tenantConfigurationService;

    PrintTemplateSelection(TenantConfigurationService tenantConfigurationService) {
        this.tenantConfigurationService = tenantConfigurationService;
    }

    /**
     * The configuration key of a document type and language: both upper-cased, anything but a letter or
     * digit turned into an underscore.
     *
     * @param entity the document type
     * @param language the language code
     * @return the key
     */
    static String key(String entity, String language) {
        return KEY_PREFIX + keySegment(entity) + "_" + keySegment(language);
    }

    /**
     * The selected template reference of the current tenant, read like every other tenant override:
     * from the thread-scoped configuration, which {@code TenantConfigurationInitFilter} fills for a
     * request and the listener dispatch fills for a message (a render outside both loads it first - see
     * {@link PrintFacade}). A deployment-wide value from the environment applies when the tenant has
     * none.
     *
     * @param entity the document type
     * @param language the language code
     * @return the selected reference, empty when none is set
     */
    Optional<String> get(String entity, String language) {
        return nonBlank(Configuration.get(key(entity, language)));
    }

    /**
     * The selection the current tenant has stored, read from its configuration store - for the
     * synchronizer, whose thread carries no tenant configuration.
     *
     * @param entity the document type
     * @param language the language code
     * @return the stored reference, empty when none is stored
     * @throws SQLException if the tenant configuration cannot be read
     */
    Optional<String> getStored(String entity, String language) throws SQLException {
        String key = key(entity, language);
        return tenantConfigurationService.listForCurrentTenant()
                                         .stream()
                                         .filter(entry -> key.equals(entry.key()))
                                         .findFirst()
                                         .flatMap(entry -> nonBlank(entry.value()));
    }

    /**
     * Selects a template for the current tenant.
     *
     * @param entity the document type
     * @param language the language code
     * @param reference the template reference
     * @throws SQLException if the tenant configuration cannot be written
     */
    void select(String entity, String language, String reference) throws SQLException {
        tenantConfigurationService.set(key(entity, language), reference);
    }

    private static Optional<String> nonBlank(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    private static String keySegment(String value) {
        return value.toUpperCase(Locale.ROOT)
                    .replaceAll("[^A-Z0-9]", "_");
    }
}
