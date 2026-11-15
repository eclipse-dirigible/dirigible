/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

/** One thing a person must look at before trusting a captured record. */
public final class CaptureFlag {

    /** Why the field needs attention. */
    public enum Reason {
        /** The document does not carry the value. */
        MISSING,
        /** The model was not sure of the value. */
        LOW_CONFIDENCE,
        /** The value could not be converted to the field's type. */
        UNREADABLE,
        /** A lookup found no record. */
        NO_MATCH,
        /** A lookup found several records. */
        AMBIGUOUS_MATCH,
        /** The value read differs from the one in the QR code. */
        QR_MISMATCH,
        /** The value contradicts another value of the document. */
        MISMATCH,
        /** A line item needs attention. */
        LINE
    }

    private final String field;
    private final Reason reason;
    private final String value;
    private final String message;

    private CaptureFlag(String field, Reason reason, String value, String message) {
        this.field = field;
        this.reason = reason;
        this.value = value;
        this.message = message;
    }

    /**
     * Create a flag.
     *
     * @param field the field concerned
     * @param reason why it needs attention
     * @param value the value read, may be null
     * @param message a short explanation, may be null
     * @return the flag
     */
    public static CaptureFlag of(String field, Reason reason, String value, String message) {
        return new CaptureFlag(field, reason, value, message);
    }

    /**
     * The field concerned.
     *
     * @return the field name
     */
    public String getField() {
        return field;
    }

    /**
     * Why the field needs attention.
     *
     * @return the reason
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * The value read.
     *
     * @return the value, or null
     */
    public String getValue() {
        return value;
    }

    /**
     * A short explanation.
     *
     * @return the message, or null
     */
    public String getMessage() {
        return message;
    }
}
