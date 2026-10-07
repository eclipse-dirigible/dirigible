/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli.generate;

/** The intent could not be generated: it was refused, or one of its code generations failed. */
class GenerationFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    GenerationFailedException(String message) {
        super(message);
    }

    GenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
