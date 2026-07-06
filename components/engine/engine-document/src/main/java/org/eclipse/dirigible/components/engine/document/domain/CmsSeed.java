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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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

    /** The raw file content (kept so the seed phase does not re-read the repository). */
    @Lob
    @Basic
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
