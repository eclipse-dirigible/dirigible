/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The final-attempt arithmetic behind the intent {@code onError} conversion - the exact mirror of
 * Flowable's {@code JobRetryCmd}: with a cycle {@code R<n>/<every>} the job's FIRST failure carries
 * no exception message yet (retries still hold the engine default, so only the cycle's own budget
 * counts), and a later failure is final when the remaining retries are down to one. Getting this
 * wrong either converts too early (retries the intent asked for never run) or too late (the job
 * dead-letters and the onError route is never taken).
 */
class IntentStepResilienceTest {

    /** No declared retry: the non-retried failure routes immediately, engine defaults or not. */
    @Test
    void noCycleMeansTheFirstFailureIsFinal() {
        assertTrue(IntentStepResilience.isFinalAttempt(null, null, 3));
        assertTrue(IntentStepResilience.isFinalAttempt(" ", null, 3));
    }

    /** No visible job (a synchronous execution): nothing would ever re-run the attempt. */
    @Test
    void aMissingJobIsFinal() {
        assertTrue(IntentStepResilience.isFinalAttempt("R4/PT30S", null, null));
    }

    /**
     * The first failure of a cycle job (no exception message recorded yet): the job's retries still
     * hold the engine default, so only the cycle's budget decides - R4 has three more attempts coming.
     */
    @Test
    void theFirstFailureOfAMultiAttemptCycleIsNotFinal() {
        assertFalse(IntentStepResilience.isFinalAttempt("R4/PT30S", null, 3));
    }

    @Test
    void aSingleAttemptCycleIsFinalOnTheFirstFailure() {
        assertTrue(IntentStepResilience.isFinalAttempt("R1/PT30S", null, 3));
    }

    /** R4: attempts two and three still have retries left; attempt four (retries == 1) is final. */
    @Test
    void aLaterFailureIsFinalExactlyWhenRetriesAreDownToOne() {
        assertFalse(IntentStepResilience.isFinalAttempt("R4/PT30S", "boom", 3));
        assertFalse(IntentStepResilience.isFinalAttempt("R4/PT30S", "boom", 2));
        assertTrue(IntentStepResilience.isFinalAttempt("R4/PT30S", "boom", 1));
    }

    /** A cycle shape this helper does not recognize must never claim finality on the first failure. */
    @Test
    void anUnrecognizedCycleNeverClaimsTheFirstFailure() {
        assertFalse(IntentStepResilience.isFinalAttempt("PT30S", null, 3));
        assertFalse(IntentStepResilience.isFinalAttempt("R/PT30S", null, 3));
        // ...while a later failure still follows the retries the engine actually maintains.
        assertTrue(IntentStepResilience.isFinalAttempt("PT30S", "boom", 1));
    }
}
