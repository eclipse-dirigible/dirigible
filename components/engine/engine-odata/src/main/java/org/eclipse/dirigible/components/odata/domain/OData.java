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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.odata.api.ODataAssociation;
import org.eclipse.dirigible.components.odata.api.ODataEntity;
import com.google.gson.annotations.Expose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * The OData Model.
 */
@Entity
@Table(name = "DIRIGIBLE_ODATA")
public class OData extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "odata";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ODATA_ID", nullable = false)
    private Long id;

    /** The namespace. */
    @Column(name = "ODATA_NAMESPACE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String namespace;

    /** The raw content. */
    @Column(name = "HDB_CONTENT", columnDefinition = "TEXT")
    private String content;

    /** The entities. */
    @Transient
    @Expose
    private List<ODataEntity> entities = new ArrayList<ODataEntity>();

    /** The associations. */
    @Transient
    @Expose
    private List<ODataAssociation> associations = new ArrayList<ODataAssociation>();

    /**
     * Instantiates a new o data.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @param namespace the namespace
     * @param entities the entities
     * @param associations the associations
     */
    public OData(String location, String name, String description, Set<String> dependencies, String namespace, List<ODataEntity> entities,
            List<ODataAssociation> associations) {
        super(location, name, ARTEFACT_TYPE, description, dependencies);
        this.namespace = namespace;
        this.entities = entities;
        this.associations = associations;
    }

    /**
     * Instantiates a new o data.
     */
    public OData() {
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
     * Gets the namesapce.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace.
     *
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
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
     * To json.
     *
     * @return the string
     */
    public String toJson() {
        return GsonHelper.toJson(this);
    }

    /**
     * Gets the entities.
     *
     * @return the entities
     */
    public List<ODataEntity> getEntities() {
        return entities;
    }

    /**
     * Sets the entities.
     *
     * @param entities the new entities
     */
    public void setEntities(List<ODataEntity> entities) {
        this.entities = entities;
    }

    /**
     * Gets the associations.
     *
     * @return the associations
     */
    public List<ODataAssociation> getAssociations() {
        return associations;
    }

    /**
     * Sets the associations.
     *
     * @param associations the new associations
     */
    public void setAssociations(List<ODataAssociation> associations) {
        this.associations = associations;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "OData [id=" + id + ", namespace=" + namespace + ", entities=" + entities + ", associations=" + associations + ", location="
                + location + ", name=" + name + ", type=" + type + ", description=" + description + ", key=" + key + ", dependencies="
                + dependencies + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy + ", updatedAt="
                + updatedAt + "]";
    }

}
