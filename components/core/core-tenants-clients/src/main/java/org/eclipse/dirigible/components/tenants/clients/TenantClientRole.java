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

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One role an integration client holds in its tenant.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANT_CLIENT_ROLES", uniqueConstraints = {@UniqueConstraint(name = "UK_DIRIGIBLE_TENANT_CLIENT_ROLES_CLIENT_ROLE",
        columnNames = {"TENCLIENTROLE_CLIENT", "TENCLIENTROLE_ROLE"})})
public class TenantClientRole {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TENCLIENTROLE_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The client. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TENCLIENTROLE_CLIENT", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TenantClient client;

    /** The role name. */
    @Column(name = "TENCLIENTROLE_ROLE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String role;

    /**
     * For JPA.
     */
    TenantClientRole() {}

    /**
     * Instantiates a new role of a client.
     *
     * @param client the client
     * @param role the role name
     */
    TenantClientRole(TenantClient client, String role) {
        this.client = client;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public TenantClient getClient() {
        return client;
    }

    public String getRole() {
        return role;
    }
}
