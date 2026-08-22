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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The per-process at-most-once bookkeeping a trigger-target record carries.
 *
 * <p>
 * The bug these guard against: one {@code ProcessId} column made "has this process run for this
 * record" indistinguishable from "has ANY process run for this record", so a transition-triggered
 * flow was silently skipped for a record an earlier create-triggered flow had already stamped.
 */
class ProcessStampsTest {

    @Test
    void anUnstampedRecordHasStartedNothing() {
        assertFalse(ProcessStamps.has(null, "Dunning"));
        assertFalse(ProcessStamps.has("", "Dunning"));
        assertNull(ProcessStamps.idFor(null, "Dunning"));
    }

    @Test
    void aStampedProcessIsRecordedWithItsInstance() {
        String stamps = ProcessStamps.with(null, "Identify", "42");

        assertEquals("Identify=42", stamps);
        assertTrue(ProcessStamps.has(stamps, "Identify"));
        assertEquals("42", ProcessStamps.idFor(stamps, "Identify"));
    }

    /** The reported case: a record another process already stamped is NOT started-for-this-process. */
    @Test
    void anotherProcessStampDoesNotCountAsThisProcess() {
        String stamps = ProcessStamps.with(null, "Identify", "42");

        assertFalse(ProcessStamps.has(stamps, "Dunning"), "the identification flow's stamp must not block the dunning flow");
        assertNull(ProcessStamps.idFor(stamps, "Dunning"));
    }

    @Test
    void aSecondProcessKeepsTheFirstOnesStamp() {
        String stamps = ProcessStamps.with(ProcessStamps.with(null, "Identify", "42"), "Dunning", "99");

        assertEquals("Identify=42,Dunning=99", stamps);
        assertEquals("42", ProcessStamps.idFor(stamps, "Identify"), "a wait or abort of the first flow must still find ITS instance");
        assertEquals("99", ProcessStamps.idFor(stamps, "Dunning"));
    }

    @Test
    void restartingAProcessPointsAtTheCurrentInstance() {
        String stamps = ProcessStamps.with(ProcessStamps.with(null, "Identify", "42"), "Identify", "77");

        assertEquals("Identify=77", stamps, "one entry per process, not an append-only log");
        assertEquals("77", ProcessStamps.idFor(stamps, "Identify"));
    }

    /**
     * Parsed by splitting, never by substring search: a process whose name prefixes another's would
     * otherwise read the wrong instance - or block a flow that never ran.
     */
    @Test
    void aPrefixingProcessNameIsNotConfusedWithItsNeighbour() {
        String stamps = ProcessStamps.with(null, "DunningReminder", "99");

        assertFalse(ProcessStamps.has(stamps, "Dunning"));
        assertNull(ProcessStamps.idFor(stamps, "Dunning"));
        assertEquals("99", ProcessStamps.idFor(stamps, "DunningReminder"));
    }

    /** A stamp that cannot be parsed must not take down the listener reading it. */
    @Test
    void malformedStampsAreIgnoredRatherThanFatal() {
        assertFalse(ProcessStamps.has("garbage", "Dunning"));
        assertFalse(ProcessStamps.has(",,=,=99,", "Dunning"));
        assertNull(ProcessStamps.idFor("Dunning=", "Dunning"), "an entry with no instance id is not a start");
        assertEquals("Identify=42", ProcessStamps.with("garbage,=99", "Identify", "42"), "unparseable entries are dropped, not carried");
    }

    @Test
    void aBlankProcessNameChangesNothing() {
        assertEquals("Identify=42", ProcessStamps.with("Identify=42", "", "99"));
        assertFalse(ProcessStamps.has("Identify=42", ""));
    }
}
