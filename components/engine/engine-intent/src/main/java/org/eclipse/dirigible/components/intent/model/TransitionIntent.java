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

import java.util.List;

/**
 * Declarative on-demand status transition: a per-record button that moves a document into a
 * designated {@code EntityStatus} - guarded server-side - after its create-time process has ended.
 * The classic consumers are void/cancel an issued document, close a case, reopen a ticket.
 *
 * <pre>
 * transitions:
 *   - name: VoidInvoice
 *     forEntity: Invoice          # must declare a function: EntityStatus relation
 *     from: [3, 4]                # allowed source status seed ids
 *     setStatus: 8                # the target status seed id
 *     when: "Paid == 0"           # optional extra guard over an own numeric field
 *     label: Void
 *     icon: ban
 * </pre>
 *
 * Two halves are generated (the {@code generates} pattern): a client button contributed to the
 * app's {@code <project>-custom-action} extension point, and a server {@code @Controller}
 * ({@code <ClassName>Transition}) that re-loads the record, validates the guards, flips ONLY the
 * status column through the targeted {@code updateProperty} primitive (a workflow-style system
 * write - no {@code -updated} re-fire), and publishes the {@code -transitioned} topic so posting
 * glue / integrations observe the transition. A guard failure returns 409 and leaves the record
 * untouched.
 */
public class TransitionIntent {

    private String name;
    /**
     * The entity whose generated view shows the button; must declare a {@code function: EntityStatus}
     * relation.
     */
    private String forEntity;
    /** Allowed SOURCE status seed ids - the transition is rejected (409) from any other status. */
    private List<Integer> from;
    /** The TARGET status seed id written when the guards pass. */
    private Integer setStatus;
    /**
     * Optional extra guard: {@code <Field> == <number>} or {@code <Field> != <number>} over an own
     * field of the entity, evaluated server-side with the SDK {@code Calc} evaluator (a {@code null}
     * field reads as 0 - the calculated-field semantics).
     */
    private String when;
    /** Button label (defaults to a humanized name). */
    private String label;
    /** Optional Lucide icon. */
    private String icon;
    /** Optional ordering among a view's actions. */
    private Integer order;

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

    public List<Integer> getFrom() {
        return from;
    }

    public void setFrom(List<Integer> from) {
        this.from = from;
    }

    public Integer getSetStatus() {
        return setStatus;
    }

    public void setSetStatus(Integer setStatus) {
        this.setStatus = setStatus;
    }

    public String getWhen() {
        return when;
    }

    public void setWhen(String when) {
        this.when = when;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
