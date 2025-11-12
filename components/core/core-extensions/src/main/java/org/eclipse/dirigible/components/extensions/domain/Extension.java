/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.domain;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import com.google.gson.annotations.Expose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Class Extension.
 */
@Entity
@Table(name = "DIRIGIBLE_EXTENSIONS")
public class Extension extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "extension";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXTENSION_ID", nullable = false)
    private Long id;

    /** The extension point. */
    @Column(name = "EXTENSION_EXTENSIONPOINT_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255, unique = false)
    @Expose
    private String extensionPoint;

    /** The module. */
    @Column(name = "EXTENSION_MODULE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String module;

    /** The role. */
    @Column(name = "EXTENSION_ROLE", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String role;


    /**
     * Instantiates a new extension.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param extensionPoint the extension point
     * @param module the module
     * @param role the role
     */
    public Extension(String location, String name, String description, String extensionPoint, String module, String role) {
        super(location, name, ARTEFACT_TYPE, description, null);
        this.extensionPoint = extensionPoint;
        this.module = module;
        this.role = role;
    }

    /**
     * Instantiates a new extension.
     */
    public Extension() {
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
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the extension point.
     *
     * @return the extensionPoint
     */
    public String getExtensionPoint() {
        return extensionPoint;
    }

    /**
     * Sets the extension point.
     *
     * @param extensionPoint the extensionPoint to set
     */
    public void setExtensionPoint(String extensionPoint) {
        this.extensionPoint = extensionPoint;
    }

    /**
     * Gets the module.
     *
     * @return the module
     */
    public String getModule() {
        return module;
    }

    /**
     * Sets the module.
     *
     * @param module the module to set
     */
    public void setModule(String module) {
        this.module = module;
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
     * @param role the role to set
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
        return "Extension [id=" + id + ", extensionPoint=" + extensionPoint + ", module=" + module + ", role=" + role + ", location="
                + location + ", name=" + name + ", description=" + description + ", type=" + type + ", key=" + key + ", dependencies="
                + dependencies + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy + ", updatedAt="
                + updatedAt + "]";
    }

}
