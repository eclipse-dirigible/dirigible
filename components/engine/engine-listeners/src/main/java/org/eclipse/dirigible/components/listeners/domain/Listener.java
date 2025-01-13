/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.domain;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import com.google.gson.annotations.Expose;

import jakarta.persistence.*;

/**
 * The Class Listener.
 */
@Entity
@Table(name = "DIRIGIBLE_LISTENERS")
public class Listener extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "listener";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LISTENER_ID", nullable = false)
    private Long id;

    /** The handler. */
    @Column(name = "LISTENER_HANDLER", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String handler;

    /** The handler. */
    @Convert(converter = ListenerKindConverter.class)
    @Column(name = "LISTENER_KIND", columnDefinition = "CHAR", nullable = false, length = 1)
    @Expose
    private ListenerKind kind;

    /**
     * Instantiates a new listener.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param handler the handler
     * @param kind the kind
     */
    public Listener(String location, String name, String description, String handler, ListenerKind kind) {
        super(location, name, ARTEFACT_TYPE, description, null);
        this.handler = handler;
        this.kind = kind;
    }

    /**
     * Instantiates a new listener.
     */
    public Listener() {}

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
     * Gets the kind.
     *
     * @return the kind
     */
    public ListenerKind getKind() {
        return kind;
    }

    /**
     * Sets the kind.
     *
     * @param kind the new kind
     */
    public void setKind(ListenerKind kind) {
        this.kind = kind;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Listener{" + "id=" + id + ", handler='" + handler + '\'' + ", kind='" + kind + '\'' + ", location='" + location + '\''
                + ", name='" + name + '\'' + ", type='" + type + '\'' + ", description='" + description + '\'' + ", key='" + key + '\''
                + ", dependencies='" + dependencies + '\'' + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy="
                + updatedBy + ", updatedAt=" + updatedAt + '}';
    }
}
