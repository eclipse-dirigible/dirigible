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
 * A task form's read-only lines table ({@code forms[].items}, issue #7907): the rows of a
 * composition child of the form's {@code forEntity} that belong to the record the task is about,
 * shown under the header fields - so the approver of an invoice sees what is billed, not only the
 * total.
 */
public class FormItemsIntent {

    /** The composition child whose rows are listed. */
    private String entity;

    /** The child's own fields, one column each, in order. */
    private List<String> fields = new ArrayList<>();

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }
}
