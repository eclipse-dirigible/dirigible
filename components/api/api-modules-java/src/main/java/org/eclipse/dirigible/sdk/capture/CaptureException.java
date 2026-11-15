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

/**
 * A document could not be read: the provider is missing, failed, refused, or the file is unusable.
 */
public class CaptureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Why the reading failed. */
    public enum Kind {
        /** No provider or API key is configured. */
        NOT_CONFIGURED,
        /** The provider could not be reached or answered with an error or an unusable answer. */
        UPSTREAM,
        /** The provider declined to read the document. */
        REFUSED,
        /** The file itself cannot be read: empty, too large or of an unsupported type. */
        UNREADABLE
    }

    private final Kind kind;

    /**
     * Create the exception.
     *
     * @param kind why it failed
     * @param message the explanation
     */
    public CaptureException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    /**
     * Create the exception.
     *
     * @param kind why it failed
     * @param message the explanation
     * @param cause the underlying failure
     */
    public CaptureException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    /**
     * Why the reading failed.
     *
     * @return the kind
     */
    public Kind getKind() {
        return kind;
    }
}
