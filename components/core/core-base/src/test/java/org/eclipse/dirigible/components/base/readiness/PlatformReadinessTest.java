/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.readiness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.base.readiness.PlatformReadiness.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The readiness lifecycle (#6448): INITIALIZING until the first depleted pass; later passes re-arm
 * to SYNCHRONIZING and settle READY / READY_DEGRADED; the boot latch is one-way.
 */
class PlatformReadinessTest {

    private final PlatformReadiness readiness = PlatformReadiness.getInstance();

    @BeforeEach
    void reset() {
        readiness.reset();
    }

    @Test
    void bootStaysInitializingUntilTheFirstPassDepletes() {
        assertEquals(State.INITIALIZING, readiness.getState());
        readiness.passStarted();
        assertEquals(State.INITIALIZING, readiness.getState(), "a pre-boot pass must not flip the state");
        assertFalse(readiness.isBootCompleted());

        readiness.passCompleted(0);
        assertEquals(State.READY, readiness.getState());
        assertTrue(readiness.isBootCompleted());
    }

    @Test
    void aLaterPassReArmsButNeverRevokesTheBootLatch() {
        readiness.passCompleted(0);

        readiness.passStarted();
        assertEquals(State.SYNCHRONIZING, readiness.getState(), "a post-boot pass re-arms the state");
        assertTrue(readiness.isBootCompleted(), "the boot latch is one-way - a publish never takes the app offline");

        readiness.passCompleted(3);
        assertEquals(State.READY_DEGRADED, readiness.getState(), "terminally failed artefacts degrade, never block");
        assertEquals(3, readiness.getFailedArtefacts());

        readiness.passStarted();
        readiness.passCompleted(0);
        assertEquals(State.READY, readiness.getState(), "a clean pass clears the degradation");
        assertEquals(0, readiness.getFailedArtefacts());
    }
}
