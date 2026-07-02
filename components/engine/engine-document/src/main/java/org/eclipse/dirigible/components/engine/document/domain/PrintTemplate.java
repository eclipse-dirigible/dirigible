/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document.domain;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A print template artefact — the persisted projection of a {@code .print} file. The file's base
 * name is the domain entity the template prints (e.g. {@code SalesInvoice.print} prints a
 * {@code SalesInvoice}).
 */
@Entity
@Table(name = "DIRIGIBLE_PRINT_TEMPLATES")
public class PrintTemplate extends Artefact {

    /** The artefact type. */
    public static final String ARTEFACT_TYPE = "print";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRINT_TEMPLATE_ID", nullable = false)
    private Long id;

    /** The template source. */
    @Column(name = "PRINT_CONTENT", columnDefinition = "CLOB")
    private String content;

    /**
     * Instantiates a new print template.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     */
    public PrintTemplate(String location, String name, String description) {
        super(location, name, ARTEFACT_TYPE, description, null);
    }

    /**
     * Instantiates a new print template.
     */
    public PrintTemplate() {
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
     * Gets the template source.
     *
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the template source.
     *
     * @param content the content to set
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
        return "PrintTemplate{" + "id=" + id + ", location='" + location + '\'' + ", name='" + name + '\'' + ", type='" + type + '\''
                + ", description='" + description + '\'' + ", key='" + key + '\'' + ", dependencies='" + dependencies + '\''
                + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + '}';
    }
}
