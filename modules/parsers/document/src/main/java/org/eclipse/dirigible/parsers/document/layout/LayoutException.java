/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.layout;

import org.eclipse.dirigible.parsers.document.SourcePosition;

/**
 * Signals an invalid layout value (e.g. {@code width="banana"}) in an otherwise well-formed
 * template, pointing at the offending attribute's source position.
 */
public class LayoutException extends RuntimeException {

    /** The serial version UID. */
    private static final long serialVersionUID = 1L;

    /** The source position of the offending value. */
    private final SourcePosition position;

    /**
     * Creates the exception.
     *
     * @param message the problem description
     * @param position the source position of the offending value, may be {@code null} when unknown
     */
    public LayoutException(String message, SourcePosition position) {
        super(position == null ? message : message + " at " + position);
        this.position = position;
    }

    /**
     * The source position of the offending value.
     *
     * @return the position, or {@code null} when unknown
     */
    public SourcePosition getPosition() {
        return position;
    }
}
