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
import java.util.Map;

/**
 * An effective-dated register lookup: on a record event, fill a to-one relation from the row of a
 * register whose validity period covers a date carried by the record.
 *
 * <p>
 * The register says "X applied to Y between A and B" - a vehicle assignment, a price list, a
 * contract, an org assignment. The incoming record carries the match key(s) and the date; the row
 * whose period covers that date names the value. Nothing else in the DSL expresses this:
 * {@code dependsOn} is a UI-time copy with equality matching only, a decision condition is a single
 * comparison, and {@code setField} writes constants.
 *
 * <pre>
 * resolves:
 *   - name: identifyDriver
 *     event: { onCreate: Fine }                    # onCreate / onUpdate, optional `when` guard
 *     set: Driver                                  # the to-one of the record to fill
 *     from: VehicleAssignment                      # the register
 *     match: { Vehicle: Vehicle }                  # register property &lt;- record property
 *     between: { start: ValidFrom, end: ValidTo, value: ViolationAt }
 *     outcome: Resolution                          # optional string field stamped with the outcome
 *     found:     { setStatus: IDENTIFIED }
 *     notFound:  { setStatus: UNRESOLVED }
 *     ambiguous: { setStatus: UNRESOLVED }
 * </pre>
 *
 * <p>
 * All three outcomes are first-class. Two register rows covering the same date is
 * {@code ambiguous}, never "pick one": an automation that silently chooses between two candidates
 * is worse than none, so the record is left unresolved and flagged for a human. The attempt is
 * observable through the {@link #outcome} field (queryable, and readable by a process decision) and
 * through the handler's log line, which names the keys and the date that were checked.
 *
 * <p>
 * A {@code match} value and {@code between.value} may be a <b>to-one path off the record</b>
 * ({@code SalesInvoice.Customer.PriceList}), not only a column of the record itself: the key a line
 * is priced by belongs to the document header, and copying it down onto every line is a UI-time
 * copy that never happens on a REST create, a {@code generates:} create-from or a schedule fan-out
 * - exactly the paths whose rows would then stay unresolved. Each hop is loaded through the
 * generated repositories, a cross-model owner included, and the terminal segment may itself be a
 * to-one, whose foreign key is what the register column is matched against.
 *
 * <p>
 * {@code copy} copies scalars of the found row onto fields of the record - the price the price-list
 * row names, the rate a contract names. Only the single covering row is copied from, and only into
 * a field the record does not already carry a value in: the same never-overwrite rule the resolved
 * relation has, so a manual correction stands.
 */
public class ResolveIntent {

    /** Stable name of this lookup (also the generated handler class stem). */
    private String name;

    /**
     * The record event that triggers the lookup: a map carrying exactly one of {@code onCreate} /
     * {@code onUpdate} naming the record entity, plus an optional {@code when} guard. (The key is
     * {@code event}, not {@code on} - YAML 1.1 resolves a bare {@code on} to the boolean {@code true}.)
     */
    private Map<String, Object> event = new LinkedHashMap<>();

    /** The to-one relation of the record entity the lookup fills. */
    private String set;

    /** The register entity queried for the covering row. */
    private String from;

    /**
     * Equality keys: a register property to the record property it must equal. The right-hand side may
     * be a to-one <b>path</b> off the record ({@code SalesInvoice.Customer.PriceList}), whose terminal
     * segment is a field or a to-one whose foreign key is compared.
     */
    private Map<String, String> match = new LinkedHashMap<>();

    /**
     * Optional static register filter: {@code <register property>: <literal>} pairs ANDed into the
     * lookup's query, for the narrowing {@code match} structurally cannot express. Every {@code match}
     * pair binds a register column to a column of the RECORD, so "and only rows that are still valid"
     * has no form there - and a register accumulates cancelled and superseded rows, each of which then
     * covers its old period forever and turns a lookup with exactly one right answer into a permanent
     * {@code ambiguous}. Mirrors the relation-level {@code where}, except that MULTIPLE pairs are
     * allowed: that one is capped at a single pair because it lands in two EDM attributes, whereas
     * these become chained {@code Criteria.eq} calls where a second condition costs nothing.
     *
     * <p>
     * A pair naming the register's {@code function: EntityStatus} relation may use the symbolic seed
     * name ({@code Status: ACTIVE}); it resolves against the REGISTER's nomenclature, not the record's.
     */
    private Map<String, Object> where = new LinkedHashMap<>();

    /**
     * The validity period: {@code start} / {@code end} name the register's period bounds and
     * {@code value} the record's date the period must cover. An absent bound on a register row is
     * open-ended (still valid); {@code end} is inclusive, and a date-only bound covers its whole day.
     * {@code value} may be a to-one path off the record, like a {@code match} value.
     */
    private Map<String, String> between = new LinkedHashMap<>();

    /**
     * Optional scalar copies from the single covering row: {@code <register field>: <record field>}.
     * Written only on {@code found}, and only into a record field that is still empty - the
     * never-overwrite rule the resolved relation has. The resolved relation itself is {@link #set};
     * this is for the values that hang off the found row rather than point at it.
     */
    private Map<String, String> copy = new LinkedHashMap<>();

    /**
     * Optional string field of the record stamped with {@code found} / {@code notFound} /
     * {@code ambiguous} - the observable trace of the attempt.
     */
    private String outcome;

    /** Optional outcome block for exactly one covering row: {@code { setStatus: <status> }}. */
    private Map<String, Object> found = new LinkedHashMap<>();

    /** Optional outcome block for no covering row. */
    private Map<String, Object> notFound = new LinkedHashMap<>();

    /** Optional outcome block for more than one covering row. */
    private Map<String, Object> ambiguous = new LinkedHashMap<>();

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
        this.event = event == null ? new LinkedHashMap<>() : event;
    }

    public String getSet() {
        return set;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Map<String, String> getMatch() {
        return match;
    }

    public void setMatch(Map<String, String> match) {
        this.match = match == null ? new LinkedHashMap<>() : match;
    }

    public Map<String, Object> getWhere() {
        return where;
    }

    public void setWhere(Map<String, Object> where) {
        this.where = where == null ? new LinkedHashMap<>() : where;
    }

    public Map<String, String> getCopy() {
        return copy;
    }

    public void setCopy(Map<String, String> copy) {
        this.copy = copy == null ? new LinkedHashMap<>() : copy;
    }

    public Map<String, String> getBetween() {
        return between;
    }

    public void setBetween(Map<String, String> between) {
        this.between = between == null ? new LinkedHashMap<>() : between;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Map<String, Object> getFound() {
        return found;
    }

    public void setFound(Map<String, Object> found) {
        this.found = found == null ? new LinkedHashMap<>() : found;
    }

    public Map<String, Object> getNotFound() {
        return notFound;
    }

    public void setNotFound(Map<String, Object> notFound) {
        this.notFound = notFound == null ? new LinkedHashMap<>() : notFound;
    }

    public Map<String, Object> getAmbiguous() {
        return ambiguous;
    }

    public void setAmbiguous(Map<String, Object> ambiguous) {
        this.ambiguous = ambiguous == null ? new LinkedHashMap<>() : ambiguous;
    }
}
