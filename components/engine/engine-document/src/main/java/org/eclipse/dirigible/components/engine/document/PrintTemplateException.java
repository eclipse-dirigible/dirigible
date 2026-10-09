/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

/**
 * A print template operation the catalogue refuses, with the reason the caller maps to its answer.
 */
class PrintTemplateException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Why an operation was refused. */
    enum Reason {
        /** The template, or the folder it would live in, does not exist. */
        NOT_FOUND,
        /** The operation contradicts the template's kind or state (a shipped version, the active one). */
        CONFLICT,
        /** A name, a language or a template's content is not acceptable. */
        INVALID,
        /** The caller's CMS access grants do not allow the operation on the template's path. */
        FORBIDDEN
    }

    private final Reason reason;

    PrintTemplateException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    PrintTemplateException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    Reason getReason() {
        return reason;
    }
}
