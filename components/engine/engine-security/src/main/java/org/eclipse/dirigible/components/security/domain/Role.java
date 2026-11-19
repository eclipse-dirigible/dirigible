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

import org.eclipse.dirigible.components.base.artefact.Artefact;

import com.google.gson.annotations.Expose;

import jakarta.persistence.*;

/**
 * The Class SecurityRole.
 */

@Entity
@Table(name = "DIRIGIBLE_SECURITY_ROLES")
public class Role extends Artefact {

    /**
     * The Constant ARTEFACT_TYPE.
     */
    public static final String ARTEFACT_TYPE = "role";

    /**
     * The id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLE_ID", nullable = false)
    private Long id;

    /**
     * Whether a tenant's Owner may give this role to a person of the tenant (#8728) - the
     * {@code "assignable"} attribute of a {@code .roles} entry. A role without it stays reachable only
     * to integration clients and to groups made at the identity provider.
     */
    @Column(name = "ROLE_ASSIGNABLE")
    @Expose
    private Boolean assignable;

    /**
     * How the role reads to a tenant's Owner choosing it for a person - the {@code "label"} attribute
     * of a {@code .roles} entry; the name stands in when there is none.
     */
    @Column(name = "ROLE_LABEL", columnDefinition = "VARCHAR", length = 255)
    @Expose
    private String label;

    /**
     * Instantiates a new role.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     */
    public Role(String location, String name, String description) {
        super(location, name, ARTEFACT_TYPE, description, null);
    }

    /**
     * Instantiates a new role.
     */
    public Role() {
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
     * Whether a tenant's Owner may give this role to a person of the tenant.
     *
     * @return true only when the {@code .roles} entry says so
     */
    public boolean isAssignable() {
        return Boolean.TRUE.equals(assignable);
    }

    /**
     * Sets whether a tenant's Owner may give this role to a person of the tenant.
     *
     * @param assignable the flag; null reads as false
     */
    public void setAssignable(Boolean assignable) {
        this.assignable = assignable;
    }

    /**
     * Gets the label a tenant's Owner reads.
     *
     * @return the label, null when the entry declares none
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label a tenant's Owner reads.
     *
     * @param label the label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "SecurityRole {" + "id=" + id + ", location='" + location + '\'' + ", name='" + name + '\'' + ", type='" + type + '\''
                + ", description='" + description + '\'' + ", assignable=" + assignable + ", label='" + label + '\'' + ", key='" + key
                + '\'' + ", dependencies='" + dependencies + '\'' + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy="
                + updatedBy + ", updatedAt=" + updatedAt + '}';
    }
}
