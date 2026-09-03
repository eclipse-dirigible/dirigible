/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.dirigible.sdk.db.ValidationException;
import org.junit.jupiter.api.Test;

/**
 * A gate's rejection is recognised through whatever Flowable wrapped it in, so the completing user
 * is told why the transition was refused (issue #7014).
 */
class ClientValidationFailureTest {

    /** A client subclass of the SDK exception - the shape hand-written domain logic may raise. */
    private static final class DocumentRejected extends ValidationException {

        private static final long serialVersionUID = 1L;

        DocumentRejected(String message) {
            super(message);
        }
    }

    @Test
    void theAuthoredMessageIsReadThroughFlowablesWrapping() {
        Throwable failure = new IllegalStateException("command failed",
                new RuntimeException("delegate failed", new ValidationException("There must be line items present")));

        assertEquals("There must be line items present", ClientValidationFailure.messageOf(failure));
    }

    @Test
    void aSubclassOfTheSdkExceptionCountsToo() {
        assertEquals("Unbalanced", ClientValidationFailure.messageOf(new RuntimeException(new DocumentRejected("Unbalanced"))));
    }

    @Test
    void anOrdinaryFailureIsNotAValidation() {
        assertNull(ClientValidationFailure.messageOf(new IllegalStateException("no client class loader")));
    }

    @Test
    void aBlankRejectionStaysAServerFault() {
        assertNull(ClientValidationFailure.messageOf(new ValidationException("  ")));
    }

    @Test
    void aCyclicCauseChainTerminates() {
        RuntimeException outer = new RuntimeException("outer");
        RuntimeException inner = new RuntimeException("inner", outer);
        outer.initCause(inner);

        assertNull(ClientValidationFailure.messageOf(outer));
    }
}
