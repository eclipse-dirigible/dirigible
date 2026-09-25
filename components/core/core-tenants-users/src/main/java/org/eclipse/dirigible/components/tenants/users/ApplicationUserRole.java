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
 * One role of an {@link ApplicationUser} - an identity-provider group membership
 * {@code <tenantId>.<appId>.<role>} as this application knows it - through its whole life: who
 * asked for it and when, by which request, whether it was granted (by whom, when) or why it failed.
 *
 * <p>
 * One row per user and role. A request creates it {@link ApplicationUserRoleState#REQUESTED}; the
 * provisioning system's callback makes it {@link ApplicationUserRoleState#GRANTED} or
 * {@link ApplicationUserRoleState#FAILED}. The first grant keeps its who and when: a later callback
 * for a granted role changes nothing here.
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

    /** The user the role belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "APPUSERROLE_USER_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ApplicationUser user;

    /** The role, with the casing of the group's third segment. */
    @Column(name = "APPUSERROLE_ROLE", columnDefinition = "VARCHAR", nullable = false, length = 100)
    private String role;

    /** Where the role stands. */
    @Enumerated(EnumType.STRING)
    @Column(name = "APPUSERROLE_STATE", columnDefinition = "VARCHAR", nullable = false, length = 50)
    private ApplicationUserRoleState state;

    /** The message id of the latest request for the role - the granting one once it is granted. */
    @Column(name = "APPUSERROLE_REQUEST_ID", columnDefinition = "VARCHAR", length = 64)
    private String requestId;

    /** Who asked for the role, when this application asked. */
    @Column(name = "APPUSERROLE_REQUESTED_BY", columnDefinition = "VARCHAR", length = 320)
    private String requestedBy;

    /** When the latest request was published - refreshed when it is sent again. */
    @Column(name = "APPUSERROLE_REQUESTED_AT", columnDefinition = "TIMESTAMP")
    private Instant requestedAt;

    /** Who granted it. */
    @Column(name = "APPUSERROLE_GRANTED_BY", columnDefinition = "VARCHAR", length = 320)
    private String grantedBy;

    /** When it was granted. */
    @Column(name = "APPUSERROLE_GRANTED_AT", columnDefinition = "TIMESTAMP")
    private Instant grantedAt;

    /** The failure text of the latest failed request. */
    @Column(name = "APPUSERROLE_ERROR_MESSAGE", columnDefinition = "VARCHAR", length = 2000)
    private String errorMessage;

    /**
     * Instantiates a new role row. Required by JPA.
     */
    ApplicationUserRole() {}

    /**
     * Instantiates a new role row.
     *
     * @param user the user
     * @param role the role
     * @param state the initial state
     */
    ApplicationUserRole(ApplicationUser user, String role, ApplicationUserRoleState state) {
        this.user = user;
        this.role = role;
        this.state = state;
    }

    /**
     * Whether the role is in the given state.
     *
     * @param expected the state
     * @return whether it is
     */
    boolean is(ApplicationUserRoleState expected) {
        return state == expected;
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public ApplicationUserRoleState getState() {
        return state;
    }

    void setState(ApplicationUserRoleState state) {
        this.state = state;
    }

    public String getRequestId() {
        return requestId;
    }

    void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
