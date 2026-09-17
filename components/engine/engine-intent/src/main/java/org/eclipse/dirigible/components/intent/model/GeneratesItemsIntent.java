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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The composition-child half of a {@link GeneratesIntent}: for each source item row (the child of
 * {@link GeneratesIntent#getFrom()}) a target item row is created and re-pointed at the newly
 * created target master. {@link #from} / {@link #to} are the child entity names; {@link #map} /
 * {@link #defaults} follow the same semantics as on the parent (source copy vs
 * {@code now}/literal). The foreign key back to the master is set automatically - it must not be
 * listed in {@link #map}.
 *
 * <p>
 * {@link #where} is the SOURCE-ROW RULE (issue #7091): without it every row of the source document
 * became a line, so a document could only be generated whole - an unapproved timesheet was billed
 * at the same footing as an approved one, and an empty one (whose mapped value the target refuses)
 * stopped the whole create-from until someone deleted the row by hand. {@link #refuse} declares
 * which of the two readings the document means: dropping an unqualified row silently and billing it
 * silently are both wrong, for different months.
 */
public class GeneratesItemsIntent {

    /** The source item entity (the composition child of the source master). */
    private String from;

    /** The target item entity (the composition child of the target master). */
    private String to;

    /** Target item property -> source item property. */
    private Map<String, String> map = new LinkedHashMap<>();

    /** Target item property -> {@code now} or a literal value. */
    private Map<String, String> defaults = new LinkedHashMap<>();

    /**
     * Optional source-row rule (issue #7091): only the rows of {@link #from} that satisfy every
     * condition become target lines. The conditions are the same field/op/value triples a
     * {@code schedules[].where} carries and are pushed into the very {@code Criteria} that already
     * selects the source's rows by their master foreign key, so the unqualified rows are never loaded.
     *
     * <p>
     * A condition naming the source item's {@code function: EntityStatus} relation may use the seeded
     * status NAME - an id is positional, and a status inserted mid-nomenclature would otherwise
     * silently retarget the rule.
     *
     * <p>
     * With a rule declared, a source whose rows ALL fail it refuses the create-from rather than
     * committing a header with no lines: a document of no lines is not the document that was asked for,
     * and the empty invoice is the harder failure to notice of the two.
     */
    private List<ScheduleConditionIntent> where;

    /**
     * Optional refusal message (issue #7091): with it, a source row that does not satisfy
     * {@link #where} stops the whole create-from - a {@code ValidationException} carrying this text and
     * the keys of the offending rows - instead of being left out of the document.
     *
     * <p>
     * Which of the two an unqualified row deserves is a property of the document, not of the platform:
     * a rejected timesheet quietly dropped from an invoice and a rejected timesheet quietly billed are
     * both wrong, so skipping is the default and this declares the other reading. Requires
     * {@link #where} - there is nothing for a row to be unqualified against without it.
     */
    private String refuse;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map == null ? new LinkedHashMap<>() : map;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults == null ? new LinkedHashMap<>() : defaults;
    }

    public List<ScheduleConditionIntent> getWhere() {
        return where;
    }

    public void setWhere(List<ScheduleConditionIntent> where) {
        this.where = where;
    }

    /** Whether a source-row rule is declared (see {@link #where}). */
    public boolean hasWhere() {
        return where != null && !where.isEmpty();
    }

    public String getRefuse() {
        return refuse;
    }

    public void setRefuse(String refuse) {
        this.refuse = refuse;
    }

    /** Whether an unqualified source row refuses the whole create-from (see {@link #refuse}). */
    public boolean hasRefuse() {
        return refuse != null && !refuse.isBlank();
    }
}
