/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.csvim.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.google.gson.annotations.Expose;

import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * The Csvim Entity.
 */
@Entity
@Table(name = "DIRIGIBLE_CSVIM")
public class Csvim extends Artefact {

    /**
     * The Constant ARTEFACT_TYPE.
     */
    public static final String ARTEFACT_TYPE = "csvim";

    /**
     * The id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CSVIM_ID", nullable = false)
    private Long id;

    /**
     * The version.
     */
    @Column(name = "CSVIM_VERSION", columnDefinition = "VARCHAR")
    @Expose
    private String version;

    /**
     * The datasource.
     */
    @Column(name = "CSVIM_DATASOURCE", columnDefinition = "VARCHAR")
    @Expose
    private String datasource;

    /**
     * The csv file definitions.
     */
    @OneToMany(mappedBy = "csvim", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Nullable
    @Expose
    private List<CsvFile> files = new ArrayList<CsvFile>();

    /**
     * Instantiates a new csvim.
     *
     * @param id the id
     * @param version the version
     * @param files the files
     */
    public Csvim(Long id, String version, List<CsvFile> files) {
        this.id = id;
        this.version = version;
        this.files = files;
    }

    /**
     * Instantiates a new csvim.
     */
    public Csvim() {

    }

    /**
     * Gets the id.
     *
     * @return get the id of csvim
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id of the csvim
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the version.
     *
     * @return get the version of csvim
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version.
     *
     * @param version the version of the csvim
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets the datasource.
     *
     * @return the datasource
     */
    public String getDatasource() {
        return datasource;
    }

    /**
     * Sets the datasource.
     *
     * @param datasource the new datasource
     */
    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    /**
     * Gets the files.
     *
     * @return get list of csvFiles
     */
    @Nullable
    public List<CsvFile> getFiles() {
        return files;
    }

    /**
     * Sets the files.
     *
     * @param csvFile set list of csvFiles
     */
    public void setFiles(@Nullable List<CsvFile> csvFile) {
        this.files = csvFile;
    }

    public Optional<CsvFile> getFileByKey(String key) {
        if (files != null) {
            return files.stream()
                        .filter(f -> Objects.equals(key, f.getKey()))
                        .findFirst();

        }
        return Optional.empty();
    }
}
