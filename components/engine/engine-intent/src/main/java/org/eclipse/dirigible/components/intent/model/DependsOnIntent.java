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
     *
     * <p>
     * On a FIELD the value may also be the CONDITIONAL form (#6358): {@code valueFrom: { by: <path>,
     * cases: { <literal>: <property>, ... }, default: <property>? }} - the copied trigger-target
     * property is picked by a classifier value resolved from the {@code by} path: an own property
     * ({@code priceLevel}), a one-hop {@code <OwnRelation>.<property>} ({@code Customer.priceLevel}),
     * or - on a document item - a path starting at the composition parent relation, i.e. the open
     * document header ({@code SalesInvoice.Customer.priceLevel}). {@code cases} keys are literals
     * matched against the resolved classifier; {@code default} is the property used when no case
     * matches (no match and no default = no copy).
     */
    private Object valueFrom;

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

    /** The simple (string) form of {@code valueFrom}, or null when absent or conditional. */
    public String getValueFrom() {
        return valueFrom instanceof String string ? string : null;
    }

    /** The conditional form of {@code valueFrom}, or null when absent or a plain string. */
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> getValueFromConditional() {
        return valueFrom instanceof java.util.Map ? (java.util.Map<String, Object>) valueFrom : null;
    }

    /** True when {@code valueFrom} is declared in either form. */
    public boolean hasValueFrom() {
        return valueFrom != null;
    }

    public void setValueFrom(Object valueFrom) {
        this.valueFrom = valueFrom;
    }

    public String getFilterBy() {
        return filterBy;
    }

    public void setFilterBy(String filterBy) {
        this.filterBy = filterBy;
    }
}
