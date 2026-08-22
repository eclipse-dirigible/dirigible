/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.bpm;

/**
 * Which processes a record has already started, and with which instance - the value of a
 * trigger-target entity's {@code ProcessIds} column, read and written by the generated glue.
 *
 * <p>
 * A record carries one {@code ProcessId} (the most recent instance, which is what the UI correlates
 * its tasks on) but can legitimately be the subject of SEVERAL processes: "on create, run the
 * identification flow; when identified, run the dunning flow" is the composition the
 * {@code onTransition} trigger axis invites. One column cannot answer "has THIS process already run
 * for this record", and answering it with "any process has" is what silently skipped the follow-up
 * flow. So the per-process answer lives here, alongside the instance each process was started with
 * - which is also what a wait or an abort needs in order to correlate against its OWN instance
 * rather than whichever process stamped last.
 *
 * <p>
 * The format is deliberately dull: {@code Process=instanceId} pairs joined by commas, parsed by
 * splitting rather than by substring search, so one process name can never be mistaken for another
 * that it prefixes ({@code Dunning} vs {@code DunningReminder}). Process names are Java identifiers
 * (they become generated class names), so they cannot contain the delimiters; every method here
 * tolerates malformed and null input rather than throwing, because a stamp that cannot be parsed
 * must not be able to take down the listener that reads it.
 */
public final class ProcessStamps {

    /** Between entries. */
    private static final char ENTRY_SEPARATOR = ',';

    /** Between a process name and its instance id. */
    private static final char PAIR_SEPARATOR = '=';

    private ProcessStamps() {}

    /**
     * Whether the given process has already been started for the record carrying these stamps - the
     * at-most-once guard of a process trigger, scoped to ONE process.
     *
     * @param stamps the record's {@code ProcessIds} value (may be null or blank)
     * @param process the process definition name
     * @return true when this process is recorded as started
     */
    public static boolean has(String stamps, String process) {
        return idFor(stamps, process) != null;
    }

    /**
     * The instance the given process was started with for this record, or {@code null} when it has not
     * been started (or the entry carries no id).
     *
     * @param stamps the record's {@code ProcessIds} value (may be null or blank)
     * @param process the process definition name
     * @return the process-instance id, or null
     */
    public static String idFor(String stamps, String process) {
        if (stamps == null || process == null || process.isBlank()) {
            return null;
        }
        for (String entry : stamps.split(String.valueOf(ENTRY_SEPARATOR))) {
            int split = entry.indexOf(PAIR_SEPARATOR);
            if (split <= 0) {
                continue; // no name, or no id: not an entry we can attribute to a process
            }
            if (process.equals(entry.substring(0, split)
                                    .trim())) {
                String id = entry.substring(split + 1)
                                 .trim();
                return id.isEmpty() ? null : id;
            }
        }
        return null;
    }

    /**
     * These stamps with the given process recorded as started with the given instance - replacing any
     * earlier entry for that process (a restarted process points at its current instance) and keeping
     * every other entry in place, so two processes stamping the same record do not erase each other.
     *
     * @param stamps the record's current {@code ProcessIds} value (may be null or blank)
     * @param process the process definition name
     * @param processInstanceId the instance just started
     * @return the new {@code ProcessIds} value
     */
    public static String with(String stamps, String process, String processInstanceId) {
        if (process == null || process.isBlank()) {
            return stamps;
        }
        StringBuilder result = new StringBuilder();
        if (stamps != null) {
            for (String entry : stamps.split(String.valueOf(ENTRY_SEPARATOR))) {
                int split = entry.indexOf(PAIR_SEPARATOR);
                String name = split <= 0 ? null
                        : entry.substring(0, split)
                               .trim();
                if (name == null || name.isEmpty() || name.equals(process)) {
                    continue; // dropped: unparseable, or the entry this call replaces
                }
                append(result, entry.trim());
            }
        }
        append(result, process + PAIR_SEPARATOR + (processInstanceId == null ? "" : processInstanceId));
        return result.toString();
    }

    private static void append(StringBuilder result, String entry) {
        if (result.length() > 0) {
            result.append(ENTRY_SEPARATOR);
        }
        result.append(entry);
    }
}
