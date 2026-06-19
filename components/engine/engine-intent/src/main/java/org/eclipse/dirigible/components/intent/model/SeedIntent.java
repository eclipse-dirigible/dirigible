/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seed-data block: a list of rows that should be loaded into the named entity's table on
 * deployment. Each entry materializes into a {@code .csvim} declaration plus a matching {@code
 * .csv} file under {@code gen/}.
 *
 * <p>
 * Rows are authored as structured maps keyed by the intent's field names (e.g. {@code id},
 * {@code name}). The generator translates field names to the corresponding {@code dataName} columns
 * and writes the CSV in the entity's declared field order, so the result lines up with the table
 * the EDM generator produced.
 */
public class SeedIntent {

    private String name;
    private String entity;
    private String schema;
    private String description;
    private List<Map<String, Object>> rows = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    /**
     * Optional database schema for the {@code .csvim} declaration. Defaults to {@code PUBLIC} when
     * omitted - matches the existing CSVIM sample artefacts in the platform.
     */
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows == null ? new ArrayList<>() : rows;
    }

    /**
     * Convenience for callers that want to add rows programmatically rather than via direct list
     * mutation.
     */
    public void addRow(Map<String, Object> row) {
        this.rows.add(row == null ? new LinkedHashMap<>() : row);
    }
}
