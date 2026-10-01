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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One role of a {@link TenantUser} - an identity-provider group membership
 * {@code <tenantId>.<appId>.<role>} as the provisioning system last reported it.
 *
 * <ul>
 * <li>{@link TenantUserRoleState#GRANTED}: the role is held, with who granted it and when.</li>
 * <li>{@link TenantUserRoleState#ADDING}: a request adds the role, and it is not applied yet - so
 * there is no grantor yet.</li>
 * <li>{@link TenantUserRoleState#REMOVING}: the role is held, and a request removes it.</li>
 * </ul>
 *
 * A role with no row is not held. A change is in progress while the user is
 * {@link TenantUserStatus#PENDING} or any of their roles is {@code ADDING} or {@code REMOVING}.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANT_USER_ROLES", uniqueConstraints = {
        @UniqueConstraint(name = "UK_DIRIGIBLE_TENANT_USER_ROLES_USER_ROLE", columnNames = {"TENUSERROLE_USER_ID", "TENUSERROLE_ROLE"})})
public class TenantUserRole {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TENUSERROLE_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The user the role belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TENUSERROLE_USER_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TenantUser user;

    /** The role, with the casing of the group's third segment. */
    @Column(name = "TENUSERROLE_ROLE", columnDefinition = "VARCHAR", nullable = false, length = 100)
    private String role;

    /** Where the role stands. */
    @Enumerated(EnumType.STRING)
    @Column(name = "TENUSERROLE_STATE", columnDefinition = "VARCHAR", nullable = false, length = 50)
    private TenantUserRoleState state;

    /** Who granted the role; null while it is being added. */
    @Column(name = "TENUSERROLE_GRANTED_BY", columnDefinition = "VARCHAR", length = 320)
    private String grantedBy;

    /** When the role was granted; null while it is being added. */
    @Column(name = "TENUSERROLE_GRANTED_AT", columnDefinition = "TIMESTAMP")
    private Instant grantedAt;

    /**
     * Instantiates a new role row. Required by JPA.
     */
    TenantUserRole() {}

    /**
     * Instantiates a new role row.
     *
     * @param user the user
     * @param role the role
     * @param state where it stands
     */
    TenantUserRole(TenantUser user, String role, TenantUserRoleState state) {
        this.user = user;
        this.role = role;
        this.state = state;
    }

    public Long getId() {
        return id;
    }

    public TenantUser getUser() {
        return user;
    }

    public String getRole() {
        return role;
    }

    public TenantUserRoleState getState() {
        return state;
    }

    void setState(TenantUserRoleState state) {
        this.state = state;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }
}
