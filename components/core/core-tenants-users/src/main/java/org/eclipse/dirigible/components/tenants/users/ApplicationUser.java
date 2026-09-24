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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * One person in one tenant of this application: the account status, the latest request made for
 * them, who invited them and who last changed the row, and when they last entered the tenant. The
 * roles granted to them are {@link ApplicationUserRole} rows.
 *
 * <p>
 * The email is the only identity key, stored lower-cased, and unique together with the tenant. The
 * row lives in the SystemDB with an explicit tenant id, like the tenant registry itself, because
 * the provisioning system records outcomes here without entering the tenant. Every read and write
 * is scoped by the tenant id.
 *
 * <p>
 * It deliberately does not extend {@code Auditable}: the platform's auditor stamps a fixed
 * technical name, while who invited and who changed a user are people, and they are recorded here
 * explicitly.
 */
@Entity
@Table(name = "DIRIGIBLE_APPLICATION_USERS")
public class ApplicationUser {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPUSER_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The owning tenant. */
    @Column(name = "APPUSER_TENANT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String tenantId;

    /** The email, lower-cased and trimmed. */
    @Column(name = "APPUSER_EMAIL", columnDefinition = "VARCHAR", nullable = false, length = 320)
    private String email;

    /** The account status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "APPUSER_STATUS", columnDefinition = "VARCHAR", nullable = false, length = 50)
    private ApplicationUserStatus status;

    /** The failure text of the latest failed request. */
    @Column(name = "APPUSER_ERROR_MESSAGE", columnDefinition = "VARCHAR", length = 2000)
    private String errorMessage;

    /** The message id of the latest request. */
    @Column(name = "APPUSER_REQUEST_ID", columnDefinition = "VARCHAR", length = 64)
    private String requestId;

    /** The role the latest request asked for. */
    @Column(name = "APPUSER_REQUESTED_ROLE", columnDefinition = "VARCHAR", length = 100)
    private String requestedRole;

    /** The state of the latest request. */
    @Enumerated(EnumType.STRING)
    @Column(name = "APPUSER_REQUEST_STATE", columnDefinition = "VARCHAR", length = 50)
    private ApplicationUserRequestState requestState;

    /** When the latest request was published. */
    @Column(name = "APPUSER_REQUESTED_AT", columnDefinition = "TIMESTAMP")
    private Instant requestedAt;

    /** Who made the first request. Never overwritten. */
    @Column(name = "APPUSER_INVITED_BY", columnDefinition = "VARCHAR", length = 320)
    private String invitedBy;

    /** When the first request was made. Never overwritten. */
    @Column(name = "APPUSER_INVITED_AT", columnDefinition = "TIMESTAMP")
    private Instant invitedAt;

    /** Who last changed the row - a person, never a machine client. */
    @Column(name = "APPUSER_UPDATED_BY", columnDefinition = "VARCHAR", length = 320)
    private String updatedBy;

    /** When the person last entered the tenant. */
    @Column(name = "APPUSER_LAST_SIGN_IN_AT", columnDefinition = "TIMESTAMP")
    private Instant lastSignInAt;

    /** When the row was created. */
    @Column(name = "APPUSER_CREATED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Instant createdAt;

    /** When the row was last changed. */
    @Column(name = "APPUSER_UPDATED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Instant updatedAt;

    /** The granted roles. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("role ASC")
    private List<ApplicationUserRole> roles = new ArrayList<>();

    /**
     * Instantiates a new application user. Required by JPA.
     */
    ApplicationUser() {}

    /**
     * Instantiates a new application user.
     *
     * @param tenantId the owning tenant
     * @param email the email, already normalized
     * @param status the initial status
     * @param now the creation time
     */
    ApplicationUser(String tenantId, String email, ApplicationUserStatus status, Instant now) {
        this.tenantId = tenantId;
        this.email = email;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * The role row granting the given role, if any.
     *
     * @param role the role
     * @return the role row
     */
    Optional<ApplicationUserRole> roleNamed(String role) {
        return roles.stream()
                    .filter(granted -> granted.getRole()
                                              .equals(role))
                    .findFirst();
    }

    /**
     * Grants a role.
     *
     * @param role the role
     * @param grantedBy who granted it
     * @param grantedAt when
     * @param grantingRequestId the request that granted it, may be null
     */
    void grant(String role, String grantedBy, Instant grantedAt, String grantingRequestId) {
        roles.add(new ApplicationUserRole(this, role, grantedBy, grantedAt, grantingRequestId));
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public ApplicationUserStatus getStatus() {
        return status;
    }

    void setStatus(ApplicationUserStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRequestId() {
        return requestId;
    }

    void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    void setRequestedRole(String requestedRole) {
        this.requestedRole = requestedRole;
    }

    public ApplicationUserRequestState getRequestState() {
        return requestState;
    }

    void setRequestState(ApplicationUserRequestState requestState) {
        this.requestState = requestState;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    void setInvitedAt(Instant invitedAt) {
        this.invitedAt = invitedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getLastSignInAt() {
        return lastSignInAt;
    }

    void setLastSignInAt(Instant lastSignInAt) {
        this.lastSignInAt = lastSignInAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ApplicationUserRole> getRoles() {
        return roles;
    }
}
