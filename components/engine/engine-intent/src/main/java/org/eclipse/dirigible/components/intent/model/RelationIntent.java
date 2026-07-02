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
    /**
     * Document role: marks this to-one relation as the document's <b>status</b> (widget {@code
     * DOCUMENT_STATUS}). In the document (header-items) layout it renders as a read-only coloured pill
     * in the form's title bar instead of an editable dropdown - typically a workflow-managed status.
     */
    private boolean documentStatus;
    /**
     * Optional initial value for this to-one relation's FK: the integer id of a seed row of the target
     * (e.g. a status nomenclature). Emitted as the FK column's {@code dataDefaultValue}, so a new row
     * gets this FK at the <b>database level</b> on insert when the column is left unset. Use this for
     * an initial document status (e.g. {@code Status} defaults to the DRAFT seed) instead of a process
     * step - a default is race-free, whereas a process step that sets it on start races the trigger's
     * {@code ProcessId} write-back. Named {@code init} for readability (it is the relation's analogue
     * of a field's {@code defaultValue}).
     */
    private String init;
    /**
     * Optional form-control width as a 12-column grid span (3/4/6/12: 3 = quarter, 4 = third, 6 = half,
     * 12 = full). Emitted as the FK property's {@code widgetSize}; the Harmonia form maps it to
     * {@code grid-column: span N}. Absent (the default) leaves it unset (the form falls back to half
     * width). Use a small span (e.g. 4) to pack several short dropdowns onto one row.
     */
    private Integer size;

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

    public boolean isDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(boolean documentStatus) {
        this.documentStatus = documentStatus;
    }

    public String getInit() {
        return init;
    }

    public void setInit(String init) {
        this.init = init;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
