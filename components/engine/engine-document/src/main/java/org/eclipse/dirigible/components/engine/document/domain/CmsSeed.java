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
 * A CMS seed artefact — the persisted projection of a file placed under a project's {@code doc/}
 * folder. Every such file is copied into the tenant-scoped CMS at the path it mirrors under
 * {@code doc/} (create-if-absent), so a project can ship starter content (print templates, images,
 * documents) that a business user then customises through the Documents perspective.
 */
@Entity
@Table(name = "DIRIGIBLE_CMS_SEEDS")
public class CmsSeed extends Artefact {

    /** The artefact type. */
    public static final String ARTEFACT_TYPE = "cms-seed";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CMS_SEED_ID", nullable = false)
    private Long id;

    /** The CMS path the file seeds to (its location relative to the {@code doc/} folder). */
    @Column(name = "CMS_SEED_PATH", length = 1020)
    private String cmsPath;

    /**
     * The raw file content (kept so the seed phase does not re-read the repository).
     * <p>
     * Mapped as inline binary, deliberately NOT as a {@code @Lob}. It renders {@code bytea} on
     * PostgreSQL and leaves H2 ({@code blob}) and SQL Server ({@code varbinary(max)}) with the column
     * they already have, so no deployed schema but PostgreSQL's has anything to migrate. On PostgreSQL
     * Hibernate maps a {@code @Lob byte[]} to an {@code oid} large object, and pgjdbc's large-object
     * API refuses to run in auto-commit mode - which is exactly how the synchronization thread reads
     * and writes this row, so every seed save failed there with "Large Objects may not be used in
     * auto-commit mode". Seed content is a single file's bytes; it belongs in the row, not in a
     * side-table of large objects.
     */
    @JdbcTypeCode(SqlTypes.LONG32VARBINARY)
    @Column(name = "CMS_SEED_CONTENT")
    private byte[] content;

    /**
     * Instantiates a new CMS seed.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     */
    public CmsSeed(String location, String name, String description) {
        super(location, name, ARTEFACT_TYPE, description, null);
    }

    /**
     * Instantiates a new CMS seed.
     */
    public CmsSeed() {
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
     * Gets the CMS path the file seeds to.
     *
     * @return the CMS path
     */
    public String getCmsPath() {
        return cmsPath;
    }

    /**
     * Sets the CMS path the file seeds to.
     *
     * @param cmsPath the CMS path to set
     */
    public void setCmsPath(String cmsPath) {
        this.cmsPath = cmsPath;
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
        return "CmsSeed{" + "id=" + id + ", location='" + location + '\'' + ", name='" + name + '\'' + ", cmsPath='" + cmsPath + '\''
                + ", type='" + type + '\'' + ", key='" + key + '\'' + ", createdBy=" + createdBy + ", createdAt=" + createdAt + '}';
    }
}
