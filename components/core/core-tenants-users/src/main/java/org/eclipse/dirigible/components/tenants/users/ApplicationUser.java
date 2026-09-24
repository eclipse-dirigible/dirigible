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
 * One person in one tenant of this application: the account status, who invited them and who last
 * changed the row, and when they last entered the tenant. Their roles - requested, granted or
 * failed, each with its own request and audit - are {@link ApplicationUserRole} rows.
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

    /** The roles, in every state. */
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
     * The row of the given role, in any state.
     *
     * @param role the role
     * @return the role row
     */
    Optional<ApplicationUserRole> roleNamed(String role) {
        return roles.stream()
                    .filter(row -> row.getRole()
                                      .equals(role))
                    .findFirst();
    }

    /**
     * Adds a role row.
     *
     * @param role the role
     * @param state its initial state
     * @return the row
     */
    ApplicationUserRole addRole(String role, ApplicationUserRoleState state) {
        ApplicationUserRole row = new ApplicationUserRole(this, role, state);
        roles.add(row);
        return row;
    }

    /**
     * Removes a role row.
     *
     * @param role the role
     */
    void removeRole(String role) {
        roles.removeIf(row -> row.getRole()
                                 .equals(role));
    }

    /**
     * Whether any role is in the given state.
     *
     * @param state the state
     * @return whether one is
     */
    boolean hasRoleIn(ApplicationUserRoleState state) {
        return roles.stream()
                    .anyMatch(row -> row.is(state));
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
