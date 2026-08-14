/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.ai;

/**
 * Raised when an AI assistant's upstream call fails (transport error or non-success status).
 */
public class AssistantUpstreamException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates the exception.
     *
     * @param message the message
     */
    public AssistantUpstreamException(String message) {
        super(message);
    }

    /**
     * Instantiates the exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public AssistantUpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
