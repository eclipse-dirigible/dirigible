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
 * Declares a dependency on another intent model whose entities this model references cross-model (a
 * {@link RelationIntent} with a matching {@link RelationIntent#getModel() model} alias). Each entry
 * resolves to the referenced project + model that owns the target entity; the EDM generator turns
 * it into a {@code PROJECTION} entity (the same read-only cross-project reference the EDM editor
 * emits) so the consuming entity stores an integer FK and renders a dropdown sourced from the
 * owner's REST service, without generating the owner's table / DAO / controller locally.
 */
public class UsesIntent {

    /** The referenced model's name - its {@code .model} base name, also used as the gen folder. */
    private String model;

    /** The workspace project that owns the referenced model; defaults to {@link #model} when blank. */
    private String project;

    /**
     * Whether the model may be ABSENT from the application (dirigible #7979). A Generate that finds
     * neither the owner's {@code .model} in the workspace nor its published copy generates this model
     * without it: its relations to the owner, and the checks, list/order/subtitle and form entries and
     * the reports naming them, are left out and reported. With the owner present nothing changes.
     */
    private boolean optional;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /** The owning project, falling back to the model alias when {@code project} is not declared. */
    public String resolveProject() {
        return (project == null || project.isBlank()) ? model : project;
    }
}
