/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.component;

/**
 * Thrown when a client bean cannot be defined or wired — an unusable constructor, an unsatisfied or
 * ambiguous dependency, or a constructor injection cycle. The message names the offending bean and,
 * where relevant, the dependency chain, so the developer can fix the client code.
 */
public class BeanContainerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message the human-readable explanation
     */
    public BeanContainerException(String message) {
        super(message);
    }

    /**
     * @param message the human-readable explanation
     * @param cause the underlying failure
     */
    public BeanContainerException(String message, Throwable cause) {
        super(message, cause);
    }
}
