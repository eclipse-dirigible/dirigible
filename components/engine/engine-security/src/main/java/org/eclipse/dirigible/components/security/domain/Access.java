/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import com.google.gson.annotations.Expose;

/**
 * The Class SecurityAccess.
 */

@Entity
@Table(name = "DIRIGIBLE_SECURITY_ACCESS")
public class Access extends Artefact {

    /**
     * The Constant ARTEFACT_TYPE.
     */
    public static final String ARTEFACT_TYPE = "access";

    /**
     * The id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ACCESS_ID", nullable = false)
    private Long id;

    /**
     * The scope.
     */
    @Column(name = "ACCESS_SCOPE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String scope;

    /**
     * The path.
     */
    @Column(name = "ACCESS_PATH", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String path;

    /**
     * The method.
     */
    @Column(name = "ACCESS_METHOD", columnDefinition = "VARCHAR", nullable = false, length = 20)
    @Expose
    private String method;

    /**
     * The role.
     */
    @Column(name = "ACCESS_ROLE", columnDefinition = "VARCHAR", nullable = false, length = 64)
    @Expose
    private String role;

    /**
     * Instantiates a new access.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param scope the scope
     * @param path the path
     * @param method the method
     * @param role the role
     */
    public Access(String location, String name, String description, String scope, String path, String method, String role) {
        super(location, name, ARTEFACT_TYPE, description, null);
        this.scope = scope;
        this.path = path;
        this.method = method;
        this.role = role;
    }

    /**
     * Instantiates a new access.
     */
    public Access() {
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
     * Gets the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path.
     *
     * @param path the new path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the method.
     *
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the method.
     *
     * @param method the new method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Gets the role.
     *
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role.
     *
     * @param role the new role
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "SecurityAccess{" + "id=" + id + ", scope='" + scope + '\'' + ", path='" + path + '\'' + ", method='" + method + '\''
                + ", role='" + role + '\'' + ", location='" + location + '\'' + ", name='" + name + '\'' + ", type='" + type + '\''
                + ", description='" + description + '\'' + ", key='" + key + '\'' + ", dependencies='" + dependencies + '\''
                + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + '}';
    }
}
