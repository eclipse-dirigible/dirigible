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
 * Form / screen declaration. {@link #forEntity} ties the form to an {@link EntityIntent} so the
 * form generator can pick fields by name and emit a working CRUD view by default.
 */
public class FormIntent {

    private String name;
    private String forEntity;
    private String description;
    private List<String> fields = new ArrayList<>();
    private List<String> editable = new ArrayList<>();
    private List<String> actions = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getForEntity() {
        return forEntity;
    }

    public void setForEntity(String forEntity) {
        this.forEntity = forEntity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }

    /**
     * The subset of {@link #fields} the reviewer may edit when this form backs a BPM user task. A user
     * task form is read-only by default (it shows the entity for a decision); a field listed here
     * renders as a bound, editable control whose value is written back to the entity by the process's
     * Writer service task on completion. Empty (the default) = fully read-only.
     */
    public List<String> getEditable() {
        return editable;
    }

    public void setEditable(List<String> editable) {
        this.editable = editable == null ? new ArrayList<>() : editable;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions == null ? new ArrayList<>() : actions;
    }
}
