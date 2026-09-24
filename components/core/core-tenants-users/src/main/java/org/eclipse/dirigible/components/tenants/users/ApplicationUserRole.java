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

import java.time.Instant;

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
 * One role granted to an {@link ApplicationUser} - an identity-provider group membership
 * {@code <tenantId>.<appId>.<role>} as this application knows it - with who granted it, when, and
 * by which request. The first grant keeps its who and when: a later callback for the same role
 * changes nothing here.
 */
@Entity
@Table(name = "DIRIGIBLE_APPLICATION_USER_ROLES",
        uniqueConstraints = {@UniqueConstraint(name = "UK_DIRIGIBLE_APPLICATION_USER_ROLES_USER_ROLE",
                columnNames = {"APPUSERROLE_USER_ID", "APPUSERROLE_ROLE"})})
public class ApplicationUserRole {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPUSERROLE_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The user the role is granted to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPUSERROLE_USER_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ApplicationUser user;

    /** The role, with the casing of the group's third segment. */
    @Column(name = "APPUSERROLE_ROLE", columnDefinition = "VARCHAR", nullable = false, length = 100)
    private String role;

    /** Who granted it. */
    @Column(name = "APPUSERROLE_GRANTED_BY", columnDefinition = "VARCHAR", length = 320)
    private String grantedBy;

    /** When it was granted. */
    @Column(name = "APPUSERROLE_GRANTED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Instant grantedAt;

    /** The request that granted it. */
    @Column(name = "APPUSERROLE_REQUEST_ID", columnDefinition = "VARCHAR", length = 64)
    private String requestId;

    /**
     * Instantiates a new role row. Required by JPA.
     */
    ApplicationUserRole() {}

    /**
     * Instantiates a new role row.
     *
     * @param user the user
     * @param role the role
     * @param grantedBy who granted it
     * @param grantedAt when
     * @param requestId the granting request, may be null
     */
    ApplicationUserRole(ApplicationUser user, String role, String grantedBy, Instant grantedAt, String requestId) {
        this.user = user;
        this.role = role;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
        this.requestId = requestId;
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public String getRequestId() {
        return requestId;
    }
}
