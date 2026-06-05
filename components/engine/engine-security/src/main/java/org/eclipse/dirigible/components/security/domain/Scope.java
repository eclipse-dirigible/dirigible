/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.domain;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import com.google.gson.annotations.Expose;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * The Class Scope.
 *
 * <p>
 * Maps an OAuth2 (M2M / client-credentials) scope name to one or more Dirigible role names. Loaded
 * from {@code *.scopes} artefacts so that a single scope can grant several roles; scopes without a
 * mapping fall back to a 1:1 scope-name-is-role-name resolution.
 */
@Entity
@Table(name = "DIRIGIBLE_SECURITY_SCOPES")
public class Scope extends Artefact {

    /**
     * The Constant ARTEFACT_TYPE.
     */
    public static final String ARTEFACT_TYPE = "scope";

    /**
     * The id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCOPE_ID", nullable = false)
    private Long id;

    /**
     * The bare scope name (the part after the last '/' of an OAuth2 scope value).
     */
    @Column(name = "SCOPE_SCOPE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String scope;

    /**
     * The role names granted by this scope.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "DIRIGIBLE_SECURITY_SCOPE_ROLES", joinColumns = @JoinColumn(name = "SCOPEROLE_SCOPE_ID"))
    @Column(name = "SCOPEROLE_ROLE", columnDefinition = "VARCHAR", length = 255)
    @Expose
    private Set<String> roles = new HashSet<>();

    /**
     * Instantiates a new scope.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param scope the scope
     * @param roles the roles
     */
    public Scope(String location, String name, String description, String scope, Set<String> roles) {
        super(location, name, ARTEFACT_TYPE, description, null);
        this.scope = scope;
        this.roles = roles;
    }

    /**
     * Instantiates a new scope.
     */
    public Scope() {
        super();
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the scope.
     *
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope.
     *
     * @param scope the new scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Gets the roles.
     *
     * @return the roles
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Sets the roles.
     *
     * @param roles the new roles
     */
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Scope{" + "id=" + id + ", scope='" + scope + '\'' + ", roles=" + roles + ", location='" + location + '\'' + ", name='"
                + name + '\'' + ", type='" + type + '\'' + ", description='" + description + '\'' + ", key='" + key + '\''
                + ", dependencies='" + dependencies + '\'' + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy="
                + updatedBy + ", updatedAt=" + updatedAt + '}';
    }
}
