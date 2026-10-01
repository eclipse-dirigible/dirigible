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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import jakarta.persistence.UniqueConstraint;

/**
 * One person in one tenant of this application, as the external provisioning system last reported
 * them: their status, the revision of that report, who invited them and who last changed the
 * membership, and why the latest change was not applied. Their roles are {@link TenantUserRole}
 * rows.
 *
 * <p>
 * The table is a <b>replica</b>. Only the provisioning system writes it, through the users snapshot
 * endpoint, and a snapshot is applied only when its revision is higher than the stored one. The
 * single exception is {@link #getLastSignInAt()}, which is this application's own observation of a
 * sign-in and which no snapshot touches.
 *
 * <p>
 * The email is the identity key, stored lower-cased, and unique together with the tenant. The row
 * lives in the SystemDB with an explicit tenant id, like the tenant registry itself, because the
 * provisioning system writes it without entering the tenant. Every read and write is scoped by the
 * tenant id. It deliberately does not extend {@code Auditable}: who invited and who changed a user
 * are people, and they come from the snapshot.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANT_USERS", uniqueConstraints = {
        @UniqueConstraint(name = "UK_DIRIGIBLE_TENANT_USERS_TENANT_EMAIL", columnNames = {"TENUSER_TENANT_ID", "TENUSER_EMAIL"})})
public class TenantUser {

    /** The id - the Owner's commands address a user by it, never by email. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TENUSER_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The owning tenant. */
    @Column(name = "TENUSER_TENANT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String tenantId;

    /** The email, lower-cased and trimmed. */
    @Column(name = "TENUSER_EMAIL", columnDefinition = "VARCHAR", nullable = false, length = 320)
    private String email;

    /** The status; {@link TenantUserStatus#REMOVED} is a hidden tombstone. */
    @Enumerated(EnumType.STRING)
    @Column(name = "TENUSER_STATUS", columnDefinition = "VARCHAR", nullable = false, length = 50)
    private TenantUserStatus status;

    /** The provisioning system's revision of this user - the ordering and the stale-edit token. */
    @Column(name = "TENUSER_REVISION", columnDefinition = "BIGINT", nullable = false)
    private long revision;

    /** Who asked for the invitation that began this membership. */
    @Column(name = "TENUSER_INVITED_BY", columnDefinition = "VARCHAR", length = 320)
    private String invitedBy;

    /** When this membership began. */
    @Column(name = "TENUSER_INVITED_AT", columnDefinition = "TIMESTAMP")
    private Instant invitedAt;

    /** Who last changed the membership; on a removed user, who removed them. */
    @Column(name = "TENUSER_LAST_CHANGED_BY", columnDefinition = "VARCHAR", length = 320)
    private String lastChangedBy;

    /** When the membership was last changed. */
    @Column(name = "TENUSER_LAST_CHANGED_AT", columnDefinition = "TIMESTAMP")
    private Instant lastChangedAt;

    /**
     * Why the latest change was not applied: a reason code of the change message, or {@code FAILED}.
     * Kept as a string - the platform does not interpret it, the screen translates it.
     */
    @Column(name = "TENUSER_LAST_ERROR_CODE", columnDefinition = "VARCHAR", length = 64)
    private String lastErrorCode;

    /** The text that goes with {@link #lastErrorCode}. */
    @Column(name = "TENUSER_LAST_ERROR_MESSAGE", columnDefinition = "VARCHAR", length = 2000)
    private String lastErrorMessage;

    /** When the person last entered the tenant - the one column this application writes. */
    @Column(name = "TENUSER_LAST_SIGN_IN_AT", columnDefinition = "TIMESTAMP")
    private Instant lastSignInAt;

    /** The roles held, being added or being removed. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("role ASC")
    private List<TenantUserRole> roles = new ArrayList<>();

    /**
     * Instantiates a new tenant user. Required by JPA.
     */
    TenantUser() {}

    /**
     * Instantiates a new tenant user.
     *
     * @param tenantId the owning tenant
     * @param email the email, already normalized
     */
    TenantUser(String tenantId, String email) {
        this.tenantId = tenantId;
        this.email = email;
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

    public TenantUserStatus getStatus() {
        return status;
    }

    void setStatus(TenantUserStatus status) {
        this.status = status;
    }

    public long getRevision() {
        return revision;
    }

    void setRevision(long revision) {
        this.revision = revision;
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

    public String getLastChangedBy() {
        return lastChangedBy;
    }

    void setLastChangedBy(String lastChangedBy) {
        this.lastChangedBy = lastChangedBy;
    }

    public Instant getLastChangedAt() {
        return lastChangedAt;
    }

    void setLastChangedAt(Instant lastChangedAt) {
        this.lastChangedAt = lastChangedAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public Instant getLastSignInAt() {
        return lastSignInAt;
    }

    void setLastSignInAt(Instant lastSignInAt) {
        this.lastSignInAt = lastSignInAt;
    }

    /**
     * The roles held, being added or being removed - a read-only view: the replica changes them only
     * through the methods below, as it applies a snapshot.
     *
     * @return the roles, by name
     */
    public List<TenantUserRole> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    /**
     * The row of a role, in any state.
     *
     * @param role the role
     * @return the row, if the user has one
     */
    Optional<TenantUserRole> roleNamed(String role) {
        return roles.stream()
                    .filter(row -> row.getRole()
                                      .equals(role))
                    .findFirst();
    }

    /**
     * Adds a role row.
     *
     * @param role the role
     * @param state where it stands
     * @return the row
     */
    TenantUserRole addRole(String role, TenantUserRoleState state) {
        TenantUserRole row = new TenantUserRole(this, role, state);
        roles.add(row);
        return row;
    }

    /**
     * Drops every role row whose role is not one of the given ones.
     *
     * @param kept the roles to keep
     */
    void retainRoles(Set<String> kept) {
        roles.removeIf(row -> !kept.contains(row.getRole()));
    }

    /**
     * Drops every role row.
     */
    void clearRoles() {
        roles.clear();
    }
}
