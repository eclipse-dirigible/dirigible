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

import com.google.gson.annotations.Expose;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.eclipse.dirigible.components.base.artefact.Artefact;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The Csvim Entity.
 */
@Entity
@Table(name = "DIRIGIBLE_CSVIM")
public class Csvim extends Artefact {

    public static final String ARTEFACT_TYPE = "csvim";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CSVIM_ID", nullable = false)
    private Long id;

    @Column(name = "CSVIM_VERSION", columnDefinition = "VARCHAR")
    @Expose
    private String version;

    @Column(name = "CSVIM_DATASOURCE", columnDefinition = "VARCHAR")
    @Expose
    private String datasource;

    @OneToMany(mappedBy = "csvim", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Nullable
    @Expose
    private List<CsvFile> files = new ArrayList<>();

    public Csvim(Long id, String version, List<CsvFile> files) {
        this.id = id;
        this.version = version;
        this.files = files;
    }

    public Csvim() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    @Nullable
    public List<CsvFile> getFiles() {
        return files;
    }

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
