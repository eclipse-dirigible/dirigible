/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.clients;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The body of {@code PUT tenants/{tenantId}/clients}: the tenant's complete client registry as the
 * external provisioning system holds it - every client it ever registered for the tenant, revoked
 * ones included or left out - and the revision of that registry, one sequence per tenant. The same
 * shape is answered by {@code GET}.
 *
 * @param revision the revision of the registry
 * @param clients the clients
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TenantClientSync(Long revision, List<Client> clients) {

    /**
     * One client.
     *
     * @param clientId the client id the identity provider issued
     * @param name the human-readable name
     * @param roles the roles the client holds in the tenant
     * @param createdBy who asked for the client
     * @param createdAt when, ISO-8601
     * @param revokedBy who revoked it
     * @param revokedAt when, ISO-8601 - null for an active client
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Client(String clientId, String name, List<String> roles, String createdBy, String createdAt, String revokedBy,
            String revokedAt) {

        /**
         * Whether the client is sent as revoked.
         *
         * @return true when it carries a revocation
         */
        boolean isRevoked() {
            return revokedAt != null;
        }

        /**
         * The view of a stored client.
         *
         * @param client the client
         * @return the view
         */
        static Client of(TenantClient client) {
            return new Client(client.getClientId(), client.getName(), List.copyOf(client.roleNames()), client.getCreatedBy(),
                    text(client.getCreatedAt()), client.getRevokedBy(), text(client.getRevokedAt()));
        }

        private static String text(Instant instant) {
            return instant == null ? null : instant.toString();
        }
    }

    /**
     * What an apply did.
     *
     * @param applied false when the registry was not newer than the stored one, and nothing changed
     * @param revision the revision stored now
     * @param active how many clients of the tenant are active
     * @param revoked how many are revoked
     */
    record Result(boolean applied, Long revision, int active, int revoked) {
    }
}
