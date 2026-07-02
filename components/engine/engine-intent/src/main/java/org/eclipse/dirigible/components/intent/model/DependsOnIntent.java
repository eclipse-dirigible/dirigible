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
 * Depends-On declaration for a field or a to-one relation: the widget reacts to a sibling to-one
 * relation (the trigger) of the same entity. When the trigger's selection changes, the generated
 * form loads the trigger's target record and either <b>auto-populates</b> this field's value from
 * one of its properties ({@link #valueFrom}) or <b>filters</b> this dropdown's option list
 * ({@link #filterBy}) - the classic Country&rarr;City cascade. Emitted as the EDM
 * {@code widgetDependsOn*} property attributes.
 */
public class DependsOnIntent {

    /**
     * Name of the sibling <b>to-one relation</b> of the same entity that triggers this dependency (the
     * master control, e.g. {@code Country} for a dependent {@code City}). Mandatory.
     */
    private String relation;

    /**
     * Property of the <b>trigger's target entity</b> whose value is used - a field or to-one relation
     * name in intent notation (e.g. {@code price} on {@code Product}, or the {@code uom} FK). Optional
     * on a relation (defaults to the trigger target's primary key - the cascade case); mandatory on a
     * field (there is nothing sensible to default a scalar to).
     */
    private String valueFrom;

    /**
     * Property of <b>this relation's target entity</b> the dropdown options are filtered by, compared
     * for equality against the resolved {@link #valueFrom} value (e.g. {@code country} on
     * {@code City}). Only meaningful on a relation; defaults to the target's primary key (the
     * narrow-to-referenced case, auto-selecting the single match). Rejected on a field - a scalar has
     * no option list.
     */
    private String filterBy;

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getValueFrom() {
        return valueFrom;
    }

    public void setValueFrom(String valueFrom) {
        this.valueFrom = valueFrom;
    }

    public String getFilterBy() {
        return filterBy;
    }

    public void setFilterBy(String filterBy) {
        this.filterBy = filterBy;
    }
}
