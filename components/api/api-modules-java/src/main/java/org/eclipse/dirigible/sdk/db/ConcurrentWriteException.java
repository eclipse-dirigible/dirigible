/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

/**
 * Thrown when a write could not be committed because another writer changed or removed the same
 * rows while it was in progress - a row it had read was gone, or no longer what it read, by the
 * time it was written. Nothing of the write was kept: its transaction was rolled back whole. The
 * Java controller runtime maps it to HTTP {@code 409 Conflict} carrying the message, because the
 * request itself is sound and repeating it against the current state is what the caller should do.
 */
public class ConcurrentWriteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a concurrent-write exception.
     *
     * @param message the user-facing reason the write was refused
     * @param cause the persistence failure that detected the concurrent change
     */
    public ConcurrentWriteException(String message, Throwable cause) {
        super(message, cause);
    }

}
