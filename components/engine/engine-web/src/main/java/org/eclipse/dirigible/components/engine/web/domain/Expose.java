/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.domain;

import java.util.Arrays;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.converters.ArrayOfStringsToCsvConverter;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Class Expose.
 */
@Entity
@Table(name = "DIRIGIBLE_WEB_EXPOSE")
public class Expose extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "expose";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXPOSE_ID", nullable = false)
    private Long id;

    /** The project. */
    @Column(name = "EXPOSE_PROJECT", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @com.google.gson.annotations.Expose
    private String project;

    /** The exposes. */
    @Column(name = "EXPOSE_EXPOSES", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    @Nullable
    @Convert(converter = ArrayOfStringsToCsvConverter.class)
    @com.google.gson.annotations.Expose
    private String[] exposes;

    /**
     * Instantiates a new web.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param exposes the exposes
     */
    public Expose(String location, String name, String description, String[] exposes) {
        super(location, name, ARTEFACT_TYPE, description, null);
        this.exposes = exposes;
    }

    /**
     * Instantiates a new web.
     */
    public Expose() {
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
     * Gets the project.
     *
     * @return the project
     */
    public String getProject() {
        return project;
    }

    /**
     * Sets the project.
     *
     * @param project the project to set
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Gets the exposes list.
     *
     * @return the exposes list
     */
    public String[] getExposes() {
        return exposes;
    }

    /**
     * Sets the exposes list.
     *
     * @param exposes the exposes list
     */
    public void setExposes(String[] exposes) {
        this.exposes = exposes;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Web [id=" + id + ", project=" + project + ", exposes=" + Arrays.toString(exposes) + ", location=" + location + ", name="
                + name + ", type=" + type + ", description=" + description + ", key=" + key + ", dependencies=" + dependencies
                + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + "]";
    }

}
