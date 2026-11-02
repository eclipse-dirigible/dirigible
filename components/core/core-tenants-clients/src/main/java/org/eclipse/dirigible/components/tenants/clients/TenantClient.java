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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One integration client of a tenant: the machine client of the tenant's external system, as the
 * external provisioning system registered it. The client id is unique across tenants - a client
 * belongs to exactly one. A revoked client keeps its row, so a late registry cannot bring it back.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANT_CLIENTS",
        uniqueConstraints = {@UniqueConstraint(name = "UK_DIRIGIBLE_TENANT_CLIENTS_CLIENT_ID", columnNames = {"TENCLIENT_CLIENT_ID"})})
public class TenantClient {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TENCLIENT_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The tenant the client belongs to. */
    @Column(name = "TENCLIENT_TENANT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String tenantId;

    /** The client id the identity provider issued. */
    @Column(name = "TENCLIENT_CLIENT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String clientId;

    /** The human-readable name. */
    @Column(name = "TENCLIENT_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String name;

    /** The revision of the registry that last wrote the row. */
    @Column(name = "TENCLIENT_REVISION", columnDefinition = "BIGINT", nullable = false)
    private long revision;

    /** Who asked for the client. */
    @Column(name = "TENCLIENT_CREATED_BY", columnDefinition = "VARCHAR", length = 320)
    private String createdBy;

    /** When the client was created. */
    @Column(name = "TENCLIENT_CREATED_AT", columnDefinition = "TIMESTAMP")
    private Instant createdAt;

    /** Who revoked the client. */
    @Column(name = "TENCLIENT_REVOKED_BY", columnDefinition = "VARCHAR", length = 320)
    private String revokedBy;

    /** When the client was revoked, null while it is active. */
    @Column(name = "TENCLIENT_REVOKED_AT", columnDefinition = "TIMESTAMP")
    private Instant revokedAt;

    /** The roles the client holds in its tenant. */
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("role ASC")
    private List<TenantClientRole> roles = new ArrayList<>();

    /**
     * For JPA.
     */
    TenantClient() {}

    /**
     * Instantiates a new client.
     *
     * @param tenantId the tenant id
     * @param clientId the client id
     */
    TenantClient(String tenantId, String clientId) {
        this.tenantId = tenantId;
        this.clientId = clientId;
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public long getRevision() {
        return revision;
    }

    void setRevision(long revision) {
        this.revision = revision;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    /**
     * Whether the client was revoked.
     *
     * @return true once revoked - for good
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Revokes the client, unless it already is: the first revocation is the one kept.
     *
     * @param by who revoked it, may be null
     * @param at when
     */
    void revoke(String by, Instant at) {
        if (revokedAt == null) {
            revokedAt = at;
            revokedBy = by;
        }
    }

    public List<TenantClientRole> getRoles() {
        return roles;
    }

    /**
     * The role names.
     *
     * @return the names, in order
     */
    public Set<String> roleNames() {
        Set<String> names = new LinkedHashSet<>();
        roles.forEach(role -> names.add(role.getRole()));
        return names;
    }

    /**
     * Replaces the roles with the given ones, keeping the rows of the roles that stay.
     *
     * @param names the role names
     */
    void replaceRoles(Collection<String> names) {
        Set<String> wanted = new LinkedHashSet<>(names);
        roles.removeIf(role -> !wanted.contains(role.getRole()));
        Set<String> kept = roleNames();
        wanted.stream()
              .filter(name -> !kept.contains(name))
              .forEach(name -> roles.add(new TenantClientRole(this, name)));
    }
}
