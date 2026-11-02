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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.clients.IntegrationClient;
import org.eclipse.dirigible.components.base.tenant.clients.IntegrationClientRegistry;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * The integration client registry: written by the external provisioning system, one tenant's
 * complete list at a time, and read by the security layer on every request of a registered client.
 *
 * <p>
 * The rules of an apply, all in one transaction:
 * <ol>
 * <li>a registry whose revision is not higher than the stored one changes nothing - a late push
 * cannot undo a newer one;</li>
 * <li>every client it names is inserted or updated: its name, its roles exactly as sent, its
 * revocation;</li>
 * <li>a revoked client stays revoked whatever a later registry says - a client id is never
 * reused;</li>
 * <li>a stored client the registry does not name is revoked;</li>
 * <li>a client id registered to another tenant refuses the whole registry with a 409 - a client
 * belongs to one tenant.</li>
 * </ol>
 */
@Service
class TenantClientRegistryService implements IntegrationClientRegistry {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantClientRegistryService.class);

    /** A tenant id - the tenant registry's own rule. */
    private static final Pattern TENANT_ID = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$");

    /** The registry. */
    private final TenantClientRepository clients;

    /** The tenants. */
    private final TenantService tenants;

    /** One new transaction per registry, whatever the caller runs in. */
    private final TransactionTemplate writes;

    /** A read-only transaction for the lookups, which read the lazy roles. */
    private final TransactionTemplate reads;

    /**
     * Instantiates the service.
     *
     * @param clients the registry
     * @param tenants the tenants
     * @param transactionManager the SystemDB transaction manager
     */
    TenantClientRegistryService(TenantClientRepository clients, TenantService tenants,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
        this.clients = clients;
        this.tenants = tenants;
        this.writes = new TransactionTemplate(transactionManager);
        this.writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.reads = new TransactionTemplate(transactionManager);
        this.reads.setReadOnly(true);
    }

    /**
     * The registered client of an id, unless it was revoked.
     *
     * @param clientId the client id the access token names
     * @return the client, empty when the id is unknown or revoked
     */
    @Override
    public Optional<IntegrationClient> findActive(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            return Optional.empty();
        }
        return reads.execute(status -> clients.findByClientId(clientId)
                                              .filter(client -> !client.isRevoked())
                                              .map(client -> new IntegrationClient(client.getClientId(), client.getTenantId(),
                                                      client.getName(), client.roleNames())));
    }

    /**
     * Applies a tenant's complete registry.
     *
     * @param tenantId the tenant id
     * @param body the registry
     * @return what the apply did
     */
    TenantClientSync.Result apply(String tenantId, TenantClientSync body) {
        requireTenant(tenantId);
        List<String> problems = TenantClientSyncValidation.problems(body);
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", problems));
        }
        TenantClientSync.Result result = writes.execute(status -> applyLocked(tenantId, body));
        LOGGER.info("Integration clients of tenant [{}]: revision [{}] applied [{}], active [{}], revoked [{}], stored revision [{}]",
                LogSanitizer.sanitize(tenantId), body.revision(), result.applied(), result.active(), result.revoked(), result.revision());
        return result;
    }

    /**
     * The registry of a tenant, revoked clients included.
     *
     * @param tenantId the tenant id
     * @return the registry
     */
    TenantClientSync list(String tenantId) {
        requireTenant(tenantId);
        return reads.execute(status -> {
            List<TenantClient> stored = clients.findByTenantIdOrderByClientId(tenantId);
            return new TenantClientSync(maxRevision(stored), stored.stream()
                                                                   .map(TenantClientSync.Client::of)
                                                                   .toList());
        });
    }

    private TenantClientSync.Result applyLocked(String tenantId, TenantClientSync body) {
        List<TenantClient> stored = clients.findForUpdateByTenantIdOrderByClientId(tenantId);
        Long storedRevision = maxRevision(stored);
        if (storedRevision != null && body.revision() <= storedRevision) {
            return result(false, storedRevision, stored);
        }
        long revision = body.revision();
        Map<String, TenantClient> byClientId = new LinkedHashMap<>();
        stored.forEach(client -> byClientId.put(client.getClientId(), client));
        Set<String> named = new HashSet<>();
        for (TenantClientSync.Client sent : body.clients()) {
            named.add(sent.clientId());
            TenantClient client = byClientId.get(sent.clientId());
            if (client == null) {
                if (clients.findByClientId(sent.clientId())
                           .isPresent()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Client [" + sent.clientId() + "] is registered to another tenant - a client belongs to one tenant");
                }
                client = new TenantClient(tenantId, sent.clientId());
                byClientId.put(sent.clientId(), client);
            } else if (client.isRevoked() && !sent.isRevoked()) {
                LOGGER.warn("Client [{}] of tenant [{}] was revoked and stays revoked, although revision [{}] lists it as active.",
                        LogSanitizer.sanitize(sent.clientId()), LogSanitizer.sanitize(tenantId), revision);
            }
            client.setName(sent.name()
                               .trim());
            client.setCreatedBy(sent.createdBy());
            client.setCreatedAt(TenantClientSyncValidation.instant(sent.createdAt()));
            if (sent.isRevoked()) {
                client.revoke(sent.revokedBy(), TenantClientSyncValidation.instant(sent.revokedAt()));
            }
            client.replaceRoles(sent.roles() == null ? List.of() : sent.roles());
            client.setRevision(revision);
        }
        Instant now = Instant.now();
        for (TenantClient client : byClientId.values()) {
            if (!named.contains(client.getClientId())) {
                if (!client.isRevoked()) {
                    LOGGER.info("Client [{}] of tenant [{}] is not in revision [{}] of the registry. Revoking it.",
                            LogSanitizer.sanitize(client.getClientId()), LogSanitizer.sanitize(tenantId), revision);
                }
                client.revoke(null, now);
                client.setRevision(revision);
            }
        }
        List<TenantClient> all = new ArrayList<>(byClientId.values());
        clients.saveAll(all);
        return result(true, revision, all);
    }

    private static TenantClientSync.Result result(boolean applied, Long revision, List<TenantClient> all) {
        int revoked = (int) all.stream()
                               .filter(TenantClient::isRevoked)
                               .count();
        return new TenantClientSync.Result(applied, revision, all.size() - revoked, revoked);
    }

    private static Long maxRevision(List<TenantClient> stored) {
        return stored.isEmpty() ? null
                : stored.stream()
                        .mapToLong(TenantClient::getRevision)
                        .max()
                        .getAsLong();
    }

    private void requireTenant(String tenantId) {
        if (tenantId == null || !TENANT_ID.matcher(tenantId)
                                          .matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tenant id [" + tenantId + "] is not valid - letters, digits and hyphens, not starting or ending with a hyphen");
        }
        Optional<Tenant> tenant = tenants.findById(tenantId);
        if (tenant.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no tenant with id [" + tenantId + "]");
        }
        if (TenantStatus.DEPROVISIONING == tenant.get()
                                                 .getStatus()) {
            // its registry is being deleted; a registry written now would leave orphan rows behind
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant [" + tenantId + "] is being deprovisioned");
        }
    }
}
