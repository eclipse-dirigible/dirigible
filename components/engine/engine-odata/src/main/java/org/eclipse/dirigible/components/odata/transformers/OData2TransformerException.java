/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.transformers;

/**
 * The {@link OData2TransformerException} is thrown in situations when there is an issue when
 * transforming odata file.
 */
public class OData2TransformerException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new repository exception.
     */
    public OData2TransformerException() {
        super();
    }

    /**
     * Instantiates a new repository exception.
     *
     * @param message the message
     */
    public OData2TransformerException(String message) {
        super(message);
    }

    /**
     * Instantiates a new repository exception.
     *
     * @param ex the ex
     */
    public OData2TransformerException(Throwable ex) {
        super(ex);
    }

    /**
     * Instantiates a new repository exception.
     *
     * @param message the message
     * @param ex the ex
     */
    public OData2TransformerException(String message, Throwable ex) {
        super(message, ex);
    }

}
