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
import java.util.List;

/**
 * Domain entity declaration in an {@code .intent}. Generates one EDM entity, one DSM table, one TS
 * or Java entity decorator, and a default repository / controller pair downstream.
 */
public class EntityIntent {

    private String name;
    private String description;
    /**
     * Optional entity classification. {@code setting} marks the entity as nomenclature / configuration
     * data: the EDM generator emits it with {@code type="SETTING"} so the template engine routes it
     * under the dashboard's Settings menu instead of generating a top-level perspective. Absent (the
     * default) means a regular managed entity.
     */
    private String kind;
    private List<FieldIntent> fields = new ArrayList<>();
    private List<RelationIntent> relations = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * Whether this entity is declared as a setting / nomenclature (case-insensitive
     * {@code kind: setting}).
     */
    public boolean isSetting() {
        return "setting".equalsIgnoreCase(kind);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<FieldIntent> getFields() {
        return fields;
    }

    public void setFields(List<FieldIntent> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }

    public List<RelationIntent> getRelations() {
        return relations;
    }

    public void setRelations(List<RelationIntent> relations) {
        this.relations = relations == null ? new ArrayList<>() : relations;
    }
}
