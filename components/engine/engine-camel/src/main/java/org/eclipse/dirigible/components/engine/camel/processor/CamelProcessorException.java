/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.processor;

/**
 * The Class CamelProcessorException.
 */
public class CamelProcessorException extends RuntimeException {

    /**
     * Instantiates a new camel processor exception.
     *
     * @param message the message
     */
    public CamelProcessorException(String message) {
        super(message);
    }

    /**
     * Instantiates a new camel processor exception.
     *
     * @param cause the cause
     */
    public CamelProcessorException(Throwable cause) {
        super(cause);
    }

    /**
     * Instantiates a new camel processor exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public CamelProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
