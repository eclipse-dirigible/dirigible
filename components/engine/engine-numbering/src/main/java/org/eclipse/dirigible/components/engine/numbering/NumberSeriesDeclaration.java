/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.numbering;

import org.eclipse.dirigible.components.base.artefact.Artefact;

import com.google.gson.annotations.Expose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One series declared by a module's {@code .numbers} artefact. A number series is a TENANT-LEVEL
 * business object - a module never owns one; its {@code .numbers} file declares a REQUIREMENT ("I
 * need series X; if this tenant has none, provision it with this prefix and width"), exactly as a
 * {@code .roles} file declares a role. The artefact's {@code name} is the series identity; the
 * declared prefix and size are only the provisioning DEFAULTS - the tenant's live shape and counter
 * live in {@code DIRIGIBLE_DOCUMENT_NUMBERS} and are never written back here.
 */
@Entity
@Table(name = "DIRIGIBLE_NUMBER_SERIES_DECLARATIONS")
public class NumberSeriesDeclaration extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "numbers";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DECLARATION_ID", nullable = false)
    private Long id;

    /** The declared default prefix (may be empty - a prefix-less continuous number). */
    @Column(name = "DECLARATION_PREFIX", columnDefinition = "VARCHAR", nullable = false, length = 64)
    @Expose
    private String prefix = "";

    /** The declared default total rendered width. */
    @Column(name = "DECLARATION_SIZE", columnDefinition = "INTEGER", nullable = false)
    @Expose
    private int size;

    /**
     * The PARTITION SOURCE of a partitioned series (intent {@code per:}): the physical table whose rows
     * are the partition values, so the management surface can label a partition row by the entity's
     * display name and list a (virtual) row for every value BEFORE its first allocation - an operator
     * seeds a company's starting number before its first document. Authored physical coordinates,
     * exactly like a {@code .csvim}'s table/columns. Empty for an unpartitioned series.
     */
    @Column(name = "DECLARATION_PARTITION_TABLE", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String partitionTable;

    /** The partition-source column holding the partition VALUE (the {@code per} relation's key). */
    @Column(name = "DECLARATION_PARTITION_KEY", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String partitionKey;

    /** The partition-source column holding the human label the settings page shows. */
    @Column(name = "DECLARATION_PARTITION_LABEL", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String partitionLabel;

    /**
     * Instantiates a new number series declaration.
     *
     * @param location the location
     * @param name the series identity
     * @param prefix the declared default prefix
     * @param size the declared default width
     */
    public NumberSeriesDeclaration(String location, String name, String prefix, int size) {
        super(location, name, ARTEFACT_TYPE, null, null);
        this.prefix = prefix;
        this.size = size;
    }

    /**
     * Instantiates a new number series declaration.
     */
    public NumberSeriesDeclaration() {
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
     * Gets the declared default prefix.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the declared default prefix.
     *
     * @param prefix the new prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the declared default width.
     *
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the declared default width.
     *
     * @param size the new size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Gets the partition-source table.
     *
     * @return the table, or {@code null} for an unpartitioned series
     */
    public String getPartitionTable() {
        return partitionTable;
    }

    /**
     * Sets the partition-source table.
     *
     * @param partitionTable the table
     */
    public void setPartitionTable(String partitionTable) {
        this.partitionTable = partitionTable;
    }

    /**
     * Gets the partition-source key column.
     *
     * @return the column, or {@code null}
     */
    public String getPartitionKey() {
        return partitionKey;
    }

    /**
     * Sets the partition-source key column.
     *
     * @param partitionKey the column
     */
    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    /**
     * Gets the partition-source label column.
     *
     * @return the column, or {@code null}
     */
    public String getPartitionLabel() {
        return partitionLabel;
    }

    /**
     * Sets the partition-source label column.
     *
     * @param partitionLabel the column
     */
    public void setPartitionLabel(String partitionLabel) {
        this.partitionLabel = partitionLabel;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "NumberSeriesDeclaration {id=" + id + ", location='" + location + '\'' + ", name='" + name + '\'' + ", prefix='" + prefix
                + '\'' + ", size=" + size + ", key='" + key + '\'' + '}';
    }
}
