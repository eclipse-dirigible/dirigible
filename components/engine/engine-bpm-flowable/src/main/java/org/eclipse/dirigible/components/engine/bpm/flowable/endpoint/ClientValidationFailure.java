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

import java.util.HashSet;
import java.util.Set;

/**
 * Recognises a client-side business validation - the {@code ValidationException} a generated
 * repository's {@code checks:} gate, a capacity guard or hand-written domain logic raises - in the
 * failure a BPM operation came back with, so the endpoint can answer the person who acted with the
 * authored message instead of a 500.
 *
 * <p>
 * Matched by class NAME rather than {@code instanceof}: {@code
 * org.eclipse.dirigible.sdk.db.ValidationException} lives in {@code api-modules-java}, which this
 * module cannot depend on - the cycle engine-bpm-flowable -&gt; engine-java -&gt; api-modules-java
 * -&gt; api-bpm -&gt; engine-bpm-flowable is the same reason the Java call delegate resolves client
 * classes reflectively. The cause chain is walked because Flowable wraps a delegate's failure on
 * its way out of the command.
 */
final class ClientValidationFailure {

    /** The client SDK's business-validation exception, matched by name. */
    private static final String VALIDATION_EXCEPTION = "org.eclipse.dirigible.sdk.db.ValidationException";

    private ClientValidationFailure() {}

    /**
     * The user-facing message of a client-side validation in the failure's cause chain.
     *
     * @param failure the exception the operation failed with, may be {@code null}
     * @return the validation message, or {@code null} when the failure is not a client validation (or
     *         carries no message - a blank rejection tells the user nothing, so it stays a server fault
     *         with its stack trace)
     */
    static String messageOf(Throwable failure) {
        Set<Throwable> seen = new HashSet<>();
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            String message = cause.getMessage();
            if (isValidation(cause.getClass()) && message != null && !message.isBlank()) {
                return message;
            }
        }
        return null;
    }

    /** Whether the type - or any of its supertypes - is the client SDK's validation exception. */
    private static boolean isValidation(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (VALIDATION_EXCEPTION.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }
}
