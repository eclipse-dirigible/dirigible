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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when a write raised one or more {@link Warning}s its caller has not confirmed (issue
 * #7466). The Java controller runtime maps it to HTTP {@code 428 Precondition Required} with the
 * warnings in the body; the caller repeats the same request naming the warning codes it accepts in
 * the {@code X-Confirm-Warnings} header, and the write then goes through.
 *
 * <p>
 * This is the soft counterpart of {@link ValidationException}: the write is legitimate and stays
 * possible, the person making it only has to be told first.
 */
public class ConfirmationRequiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<Warning> warnings;

    /**
     * Creates the exception.
     *
     * @param warnings the warnings awaiting confirmation, in declaration order
     */
    public ConfirmationRequiredException(List<Warning> warnings) {
        super(warnings.stream()
                      .map(Warning::message)
                      .collect(Collectors.joining("; ")));
        this.warnings = List.copyOf(warnings);
    }

    /**
     * The warnings awaiting confirmation.
     *
     * @return the warnings, in declaration order
     */
    public List<Warning> getWarnings() {
        return warnings;
    }

}
