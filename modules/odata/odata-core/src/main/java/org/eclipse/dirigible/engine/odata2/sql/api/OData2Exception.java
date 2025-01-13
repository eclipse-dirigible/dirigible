/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.api;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;

/**
 * Base runtime exception for the OData2 implementation.
 */
public class OData2Exception extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -3168076877969983097L;

    /** The http status. */
    private final HttpStatusCodes httpStatus;

    /**
     * Instantiates a new o data 2 exception.
     *
     * @param httpStatus the http status
     */
    public OData2Exception(final HttpStatusCodes httpStatus) {
        this.httpStatus = httpStatus;
    }

    /**
     * Instantiates a new o data 2 exception.
     *
     * @param text the text
     * @param httpStatus the http status
     */
    public OData2Exception(String text, final HttpStatusCodes httpStatus) {
        super(text);
        this.httpStatus = httpStatus;
    }

    /**
     * Instantiates a new o data 2 exception.
     *
     * @param text the text
     * @param httpStatus the http status
     * @param cause the cause
     */
    public OData2Exception(String text, final HttpStatusCodes httpStatus, final Throwable cause) {
        super(text, cause);
        this.httpStatus = httpStatus;
    }

    /**
     * Instantiates a new o data 2 exception.
     *
     * @param httpStatus the http status
     * @param cause the cause
     */
    public OData2Exception(final HttpStatusCodes httpStatus, final Throwable cause) {
        super(cause);
        this.httpStatus = httpStatus;
    }

    /**
     * Gets the http status.
     *
     * @return the http status
     */
    public HttpStatusCodes getHttpStatus() {
        return httpStatus;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return super.toString() + ": [" + httpStatus + " " + httpStatus.getStatusCode() + "]";
    }

}
