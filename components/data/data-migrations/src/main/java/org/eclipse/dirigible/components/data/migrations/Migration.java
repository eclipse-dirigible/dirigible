/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.migrations;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import com.google.gson.annotations.Expose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The artefact of one {@code .migration} file. It records what the file declares; whether the
 * migration has been applied is a fact of each target database, kept in that database's
 * {@code DIRIGIBLE_MIGRATIONS} ledger, never here.
 */
@Entity
@Table(name = "DIRIGIBLE_MIGRATION_FILES")
public class Migration extends Artefact {

    /** The artefact type. */
    public static final String ARTEFACT_TYPE = "migration";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MIGRATION_ID", nullable = false)
    private Long id;

    @Column(name = "MIGRATION_PROJECT", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String project;

    @Column(name = "MIGRATION_VERSION", columnDefinition = "VARCHAR", nullable = false, length = 64)
    @Expose
    private String version;

    @Column(name = "MIGRATION_SCOPE", columnDefinition = "VARCHAR", nullable = false, length = 16)
    @Expose
    private String scope;

    @Column(name = "MIGRATION_IDEMPOTENT", columnDefinition = "BOOLEAN", nullable = false)
    @Expose
    private boolean idempotent;

    @Column(name = "MIGRATION_CHECKSUM", columnDefinition = "VARCHAR", nullable = false, length = 64)
    @Expose
    private String checksum;

    Migration(String location, String name, MigrationScript script) {
        super(location, name, ARTEFACT_TYPE, null, null);
        this.project = script.project();
        this.version = script.version();
        this.scope = script.scope()
                           .name();
        this.idempotent = script.idempotent();
        this.checksum = script.checksum();
    }

    /** For JPA. */
    public Migration() {
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
     * @param id the id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the project the migration belongs to.
     *
     * @return the project
     */
    public String getProject() {
        return project;
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
     * Gets where the migration applies: {@code EACH} tenant schema or the {@code SYSTEM} database.
     *
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * Whether an edit of the applied migration re-applies it instead of failing it.
     *
     * @return true when the migration declares itself idempotent
     */
    public boolean isIdempotent() {
        return idempotent;
    }

    /**
     * Gets the checksum of the content the file had when it was parsed.
     *
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    @Override
    public String toString() {
        return "Migration {id=" + id + ", location='" + location + '\'' + ", project='" + project + '\'' + ", version='" + version + '\''
                + ", scope='" + scope + '\'' + ", lifecycle=" + lifecycle + '}';
    }
}
