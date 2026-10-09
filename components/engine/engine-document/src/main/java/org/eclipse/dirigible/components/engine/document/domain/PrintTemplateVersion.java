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

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One print template version this instance has shipped - the ledger the print template catalogue
 * keeps beside the artefact rows, which only know the version shipped now. Rows are only ever
 * added: the id orders the versions by when they were first shipped, the content hash recognises a
 * tenant copy of an earlier one, and a version label, once recorded, always names the same bytes.
 */
@Entity
@Table(name = "DIRIGIBLE_PRINT_TEMPLATE_VERSIONS",
        uniqueConstraints = @UniqueConstraint(name = "UK_DIRIGIBLE_PRINT_TEMPLATE_VERSIONS_VERSION",
                columnNames = {"PRINT_TEMPLATE_ENTITY", "PRINT_TEMPLATE_LANGUAGE", "PRINT_TEMPLATE_NAME", "PRINT_TEMPLATE_VERSION"}))
public class PrintTemplateVersion {

    /** The id - also the shipping order. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRINT_TEMPLATE_VERSION_ID", nullable = false)
    private Long id;

    /** The document type. */
    @Column(name = "PRINT_TEMPLATE_ENTITY", length = 255, nullable = false)
    private String entityName;

    /** The language code. */
    @Column(name = "PRINT_TEMPLATE_LANGUAGE", length = 64, nullable = false)
    private String language;

    /** The template name. */
    @Column(name = "PRINT_TEMPLATE_NAME", length = 255, nullable = false)
    private String templateName;

    /** The version. */
    @Column(name = "PRINT_TEMPLATE_VERSION", length = 255, nullable = false)
    private String version;

    /** The SHA-256 of the shipped bytes, in hex. */
    @Column(name = "PRINT_TEMPLATE_CONTENT_HASH", length = 64, nullable = false)
    private String contentHash;

    /** When this instance first shipped the version. */
    @Column(name = "PRINT_TEMPLATE_SHIPPED_AT", columnDefinition = "TIMESTAMP")
    private Instant shippedAt;

    /** For JPA. */
    protected PrintTemplateVersion() {}

    /**
     * A version shipped now.
     *
     * @param entityName the document type
     * @param language the language code
     * @param templateName the template name
     * @param version the version
     * @param contentHash the SHA-256 of the shipped bytes, in hex
     */
    public PrintTemplateVersion(String entityName, String language, String templateName, String version, String contentHash) {
        this.entityName = entityName;
        this.language = language;
        this.templateName = templateName;
        this.version = version;
        this.contentHash = contentHash;
        this.shippedAt = Instant.now();
    }

    /**
     * Gets the id, which orders the versions by when they were first shipped.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the document type.
     *
     * @return the document type
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Gets the language code.
     *
     * @return the language code
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Gets the template name.
     *
     * @return the template name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Gets the version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the SHA-256 of the shipped bytes.
     *
     * @return the hash, in hex
     */
    public String getContentHash() {
        return contentHash;
    }

    /**
     * Gets when this instance first shipped the version.
     *
     * @return the instant
     */
    public Instant getShippedAt() {
        return shippedAt;
    }

    @Override
    public String toString() {
        return "PrintTemplateVersion{id=" + id + ", entityName='" + entityName + "', language='" + language + "', templateName='"
                + templateName + "', version='" + version + "'}";
    }
}
