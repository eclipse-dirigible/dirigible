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

    /** Equality keys: a register property to the record property it must equal. */
    private Map<String, String> match = new LinkedHashMap<>();

    /**
     * The validity period: {@code start} / {@code end} name the register's period bounds and
     * {@code value} the record's date the period must cover. An absent bound on a register row is
     * open-ended (still valid); {@code end} is inclusive, and a date-only bound covers its whole day.
     */
    private Map<String, String> between = new LinkedHashMap<>();

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
