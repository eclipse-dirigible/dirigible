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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A shipped print template - the persisted projection of a {@code .print} file a project places at
 * {@code doc/Templates/<Entity>/Print/<lang>/<name>.print}. Every tenant's CMS receives it as the
 * immutable version {@code Templates/<Entity>/Print/<lang>/<name>@<version>.print}, beside the
 * versions shipped before it.
 */
@Entity
@Table(name = "DIRIGIBLE_PRINT_TEMPLATE_SEEDS")
public class PrintTemplateSeed extends Artefact {

    /** The artefact type. */
    public static final String ARTEFACT_TYPE = "print-template";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRINT_TEMPLATE_SEED_ID", nullable = false)
    private Long id;

    /** The document type - the {@code <Entity>} folder. */
    @Column(name = "PRINT_TEMPLATE_ENTITY", length = 255)
    private String entityName;

    /** The language code - the {@code <lang>} folder. */
    @Column(name = "PRINT_TEMPLATE_LANGUAGE", length = 64)
    private String language;

    /** The template name - the shipped file name without the extension. */
    @Column(name = "PRINT_TEMPLATE_NAME", length = 255)
    private String templateName;

    /** The version the file ships as: the module's release version, else a content hash. */
    @Column(name = "PRINT_TEMPLATE_VERSION", length = 255)
    private String version;

    /**
     * The raw file content. Inline binary, never a {@code @Lob}, for the reason {@link CmsSeed}'s
     * content column documents: a {@code @Lob byte[]} is an {@code oid} large object on PostgreSQL,
     * which pgjdbc refuses on the auto-commit connection the synchronization thread saves on.
     */
    @JdbcTypeCode(SqlTypes.LONG32VARBINARY)
    @Column(name = "PRINT_TEMPLATE_CONTENT")
    private byte[] content;

    /**
     * Instantiates a new shipped print template.
     */
    public PrintTemplateSeed() {
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
     * Gets the document type.
     *
     * @return the document type
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Sets the document type.
     *
     * @param entityName the document type to set
     */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
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
     * Sets the language code.
     *
     * @param language the language code to set
     */
    public void setLanguage(String language) {
        this.language = language;
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
     * Sets the template name.
     *
     * @param templateName the template name to set
     */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
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
     * Sets the version.
     *
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets the raw file content.
     *
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Sets the raw file content.
     *
     * @param content the content to set
     */
    public void setContent(byte[] content) {
        this.content = content;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "PrintTemplateSeed{" + "id=" + id + ", location='" + location + '\'' + ", entityName='" + entityName + '\'' + ", language='"
                + language + '\'' + ", templateName='" + templateName + '\'' + ", version='" + version + '\'' + ", key='" + key + '\''
                + '}';
    }
}
