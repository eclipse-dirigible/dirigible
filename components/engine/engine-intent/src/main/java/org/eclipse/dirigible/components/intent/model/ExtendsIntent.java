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

/**
 * Declares that the enclosing entity is an EXTENSION of another entity - it contributes its fields
 * to a base entity owned by another model, rather than defining a table of its own. Authored as
 * {@code extends: { model: <base-model>, entity: <base-entity> }} (the {@code model} must be listed
 * in {@code uses:}; omit it to extend an entity in the same model). At generation the EDM generator
 * marks the entity {@code type=EXTENSION} with
 * {@code extensionReferencedModel}/{@code extensionReferencedEntity}, and the model-to-code layer
 * folds the fields into the base table (see {@code generateUtils.mergeExtensionEntities}).
 */
public class ExtendsIntent {

    /**
     * The base model (a {@code uses:} alias) that owns the extended entity; null/blank = same model.
     */
    private String model;

    /** The base entity whose table gains this extension's fields. Required. */
    private String entity;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }
}
