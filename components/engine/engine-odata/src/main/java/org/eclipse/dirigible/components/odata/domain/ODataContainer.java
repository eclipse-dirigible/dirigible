/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.domain;

import java.util.Set;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import com.google.gson.annotations.Expose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The OData Schema Entity.
 */
@Entity
@Table(name = "DIRIGIBLE_ODATA_CONTAINER")
public class ODataContainer extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "odatacontainer";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ODATAC_ID", nullable = false)
    private Long id;

    /** The content. */
    @Column(name = "ODATAC_CONTENT", columnDefinition = "TEXT", nullable = true)
    @Expose
    private String content;

    /**
     * Instantiates a new o data container.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @param content the content
     */
    public ODataContainer(String location, String name, String description, Set<String> dependencies, String content) {
        super(location, name, ARTEFACT_TYPE, description, dependencies);
        this.content = content;
    }

    /**
     * Instantiates a new o data container.
     */
    public ODataContainer() {
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
     * Gets the content.
     *
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content.
     *
     * @param content the new content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "ODataContainer [id=" + id + ", content=" + content + ", location=" + location + ", name=" + name + ", type=" + type
                + ", description=" + description + ", key=" + key + ", dependencies=" + dependencies + ", createdBy=" + createdBy
                + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + "]";
    }

}
