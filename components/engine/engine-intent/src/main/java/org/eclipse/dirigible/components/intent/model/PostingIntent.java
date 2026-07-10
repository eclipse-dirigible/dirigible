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
import java.util.Map;

/**
 * Declarative posting: when a (usually cross-model) source document reaches a status, create ONE
 * local document with computed multi-line content - the accounting "source document → balanced
 * journal entry" shape, generalized. The generated artifact is a client-Java {@code MessageHandler}
 * on the source's {@code -transitioned} topic that re-loads the source by id (the event payload is
 * as-of the transition; later-step data such as a stamped number is not in it), guards on the
 * status, enforces at-most-once via the {@link #backReference}, resolves the {@link #rule} row, and
 * writes the target document + items through their repositories.
 *
 * <pre>
 * postings:
 *   - name: salesInvoicePosting
 *     event: { onTransition: SalesInvoice, model: kf-mod-sales-invoices, when: "Status == 3" }
 *     creates: JournalEntry
 *     backReference: SalesInvoice
 *     map: { EntryDate: date, Customer: Customer, Reason: "Sales invoice {number}" }
 *     rule: { entity: PostingRule, match: { documentType: "Sales Invoice" } }
 *     items:
 *       - { Account: rule(receivableAccount), debit: "Net + Vat" }
 *       - { Account: rule(revenueAccount),    credit: "Net" }
 *       - { Account: rule(vatAccount),        credit: "Vat", when: "Vat != 0" }
 * </pre>
 */
public class PostingIntent {

    private String name;
    /**
     * The trigger: {@code onTransition} names the source entity, {@code model} its owning intent model
     * alias (absent = same model), {@code when} the status guard as {@code <Property> == <seed id>} -
     * evaluated against the RE-LOADED source, not the raw payload.
     */
    private Map<String, Object> event;
    /** The local document entity this posting creates (must own a composition items child). */
    private String creates;
    /**
     * The {@link #creates} entity's to-one relation back to the source document - written on create and
     * used as the at-most-once guard (an event redelivery must not double-post).
     */
    private String backReference;
    /**
     * Header assignments: target property → a source property name (copy), a literal, or a literal
     * template with {@code {sourceProperty}} interpolation.
     */
    private Map<String, String> map;
    /**
     * The account-determination row: {@code entity} names a local (setting) entity, {@code match} a
     * single {@code column: literal} selector. Items reference its columns via {@code rule(<column>)};
     * a missing row or a null referenced column skips the posting (→ the unposted worklist), never
     * throws.
     */
    private Map<String, Object> rule;
    /**
     * The generated item rows: each map's keys are fields/relations of the items entity, values are
     * {@code rule(<column>)} references or arithmetic expressions over the SOURCE's fields (the SDK
     * {@code Calc} evaluator); an optional {@code when} key guards the row ({@code <Field> != 0} /
     * {@code == 0} on a source field).
     */
    private List<Map<String, String>> items;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getEvent() {
        return event;
    }

    public void setEvent(Map<String, Object> event) {
        this.event = event;
    }

    public String getCreates() {
        return creates;
    }

    public void setCreates(String creates) {
        this.creates = creates;
    }

    public String getBackReference() {
        return backReference;
    }

    public void setBackReference(String backReference) {
        this.backReference = backReference;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public Map<String, Object> getRule() {
        return rule;
    }

    public void setRule(Map<String, Object> rule) {
        this.rule = rule;
    }

    public List<Map<String, String>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, String>> items) {
        this.items = items;
    }
}
