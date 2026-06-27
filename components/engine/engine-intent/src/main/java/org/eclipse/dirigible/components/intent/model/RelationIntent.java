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
 * Relationship between two {@link EntityIntent}s. {@link #kind} is one of {@code oneToMany},
 * {@code manyToOne}, {@code oneToOne}, {@code manyToMany}.
 */
public class RelationIntent {

    private String name;
    private String kind;
    private String to;
    private boolean required;
    private boolean composition;
    private String description;
    /**
     * Optional cross-model reference: the {@code uses[].model} alias of the intent model that owns the
     * {@link #to} target entity. Absent (the default) means the target is an entity in this same model.
     * Present means the relation is a cross-model association - the generator emits a
     * {@code PROJECTION} to the owner model plus an integer FK + dropdown, and never a local table for
     * the target.
     */
    private String model;

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

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isComposition() {
        return composition;
    }

    public void setComposition(boolean composition) {
        this.composition = composition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    /** Whether this relation targets an entity owned by another intent model. */
    public boolean isCrossModel() {
        return model != null && !model.isBlank();
    }
}
