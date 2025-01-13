/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.http.client;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class HttpClientResponse.
 */
public class HttpClientResponse {

    /** The status code. */
    private int statusCode;

    /** The status message. */
    private String statusMessage;

    /** The data. */
    private byte[] data;

    /** The text. */
    private String text;

    /** The protocol. */
    private String protocol;

    /** The binary. */
    private boolean binary;

    /** The headers. */
    private List<HttpClientHeader> headers = new ArrayList<HttpClientHeader>();

    /**
     * Gets the status code.
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the status code.
     *
     * @param statusCode the new status code
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the status message.
     *
     * @return the status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Sets the status message.
     *
     * @param statusMessage the new status message
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * Gets the data.
     *
     * @return the data
     */
    public byte[] getData() {
        if (data == null) {
            return null;
        }
        return data.clone();
    }

    /**
     * Sets the data.
     *
     * @param data the new data
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Gets the text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text.
     *
     * @param text the new text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the protocol.
     *
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol.
     *
     * @param protocol the new protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets the headers.
     *
     * @return the headers
     */
    public List<HttpClientHeader> getHeaders() {
        return headers;
    }

    /**
     * Sets the headers.
     *
     * @param headers the new headers
     */
    public void setHeaders(List<HttpClientHeader> headers) {
        this.headers = headers;
    }

    /**
     * Checks if is binary.
     *
     * @return true, if is binary
     */
    public boolean isBinary() {
        return binary;
    }

    /**
     * Sets the binary.
     *
     * @param binary the new binary
     */
    public void setBinary(boolean binary) {
        this.binary = binary;
    }

}
