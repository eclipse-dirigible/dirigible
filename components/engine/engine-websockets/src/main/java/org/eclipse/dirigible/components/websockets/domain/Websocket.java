/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.domain;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import com.google.gson.annotations.Expose;

import jakarta.persistence.*;

/**
 * The Class Websocket.
 */
@Entity
@Table(name = "DIRIGIBLE_WEBSOCKETS")
public class Websocket extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "websocket";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WEBSOCKET_ID", nullable = false)
    private Long id;

    /** The endpoint. */
    @Column(name = "WEBSOCKET_ENDPOINT_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255, unique = true)
    @Expose
    private String endpoint;

    /** The handler. */
    @Column(name = "WEBSOCKET_HANDLER", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String handler;

    /** The engine. */
    @Column(name = "WEBSOCKET_ENGINE", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String engine;

    /**
     * Instantiates a new websocket.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param endpoint the endpoint
     * @param handler the handler
     * @param engine the engine
     */
    public Websocket(String location, String name, String description, String endpoint, String handler, String engine) {
        super(location, name, ARTEFACT_TYPE, description, null);
        this.endpoint = endpoint;
        this.handler = handler;
        this.engine = engine;
    }

    /**
     * Instantiates a new websocket.
     */
    public Websocket() {
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
     * Gets the endpoint.
     *
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Sets the endpoint.
     *
     * @param endpoint the new endpoint
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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
     * @param handler the new handler
     */
    public void setHandler(String handler) {
        this.handler = handler;
    }

    /**
     * Gets the engine.
     *
     * @return the engine
     */
    public String getEngine() {
        return engine;
    }

    /**
     * Sets the engine.
     *
     * @param engine the new engine
     */
    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Websocket {" + "id=" + id + ", endpoint='" + endpoint + '\'' + ", handler='" + handler + '\'' + ", engine='" + engine + '\''
                + ", location='" + location + '\'' + ", name='" + name + '\'' + ", type='" + type + '\'' + ", description='" + description
                + '\'' + ", key='" + key + '\'' + ", dependencies='" + dependencies + '\'' + ", createdBy=" + createdBy + ", createdAt="
                + createdAt + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + '}';
    }
}
