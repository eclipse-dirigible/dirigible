/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.domain;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import com.google.gson.annotations.Expose;

/**
 * The OData Handler Model.
 */
@Entity
@Table(name = "DIRIGIBLE_ODATA_HANDLER")
public class ODataHandler extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "odatahandler";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ODATAH_ID", nullable = false)
    private Long id;

    /** The namespace. */
    @Column(name = "ODATAH_NAMESPACE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String namespace;

    /** The method. */
    @Column(name = "ODATAH_METHOD", columnDefinition = "VARCHAR", nullable = false, length = 20)
    @Expose
    private String method;

    /** The type. */
    @Column(name = "ODATAH_KIND", columnDefinition = "VARCHAR", nullable = false, length = 20)
    @Expose
    private String kind;

    /** The handler. */
    @Column(name = "ODATAH_HANDLER", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String handler;

    /**
     * Instantiates a new o data handler.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @param namespace the namespace
     * @param method the method
     * @param kind the kind
     * @param handler the handler
     */
    public ODataHandler(String location, String name, String description, Set<String> dependencies, String namespace, String method,
            String kind, String handler) {
        super(location, name, ARTEFACT_TYPE, description, dependencies);
        this.namespace = namespace;
        this.method = method;
        this.kind = kind;
        this.handler = handler;
    }

    /**
     * Instantiates a new o data handler.
     */
    public ODataHandler() {
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
     * Gets the namespace.
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
     * @param method the method to set
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Gets the kind.
     *
     * @return the kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the kind.
     *
     * @param kind the kind to set
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * Gets the handler.
     *
     * @return the handler
     */
    public String getHandler() {
        return handler;
    }

    /**
     * Sets the handler.
     *
     * @param handler the handler to set
     */
    public void setHandler(String handler) {
        this.handler = handler;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "ODataHandler [id=" + id + ", namespace=" + namespace + ", method=" + method + ", kind=" + kind + ", handler=" + handler
                + ", location=" + location + ", name=" + name + ", type=" + type + ", description=" + description + ", key=" + key
                + ", dependencies=" + dependencies + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy
                + ", updatedAt=" + updatedAt + "]";
    }

}
