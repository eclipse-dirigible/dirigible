/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

/**
 * The Class CmsProviderInitializationException.
 */
public class CmsProviderInitializationException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -903595641536388250L;

    /**
     * Instantiates a new cms provider initialization exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public CmsProviderInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
