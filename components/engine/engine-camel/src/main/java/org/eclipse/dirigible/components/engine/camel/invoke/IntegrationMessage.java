/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.invoke;

import org.apache.camel.Message;

import java.util.Map;

/**
 * The Class IntegrationMessage.
 */
public class IntegrationMessage {

    /** The message. */
    private final Message message;

    /**
     * Instantiates a new integration message.
     *
     * @param message the message
     */
    public IntegrationMessage(Message message) {
        this.message = message;
    }

    /**
     * Gets the body as string.
     *
     * @return the body as string
     */
    public String getBodyAsString() {
        return message.getBody(String.class);
    }

    /**
     * Gets the body.
     *
     * @return the body
     */
    public Object getBody() {
        return message.getBody();
    }

    /**
     * Sets the body.
     *
     * @param body the new body
     */
    public void setBody(Object body) {
        message.setBody(body);
    }

    /**
     * Gets the headers.
     *
     * @return the headers
     */
    public Map<String, Object> getHeaders() {
        return message.getHeaders();
    }

    public Object getHeader(String headerName) {
        return message.getHeader(headerName);
    }

    public void setExchangeProperty(String propertyName, Object propertyValue) {
        message.getExchange()
               .setProperty(propertyName, propertyValue);
    }

    public Object getExchangeProperty(String propertyName) {
        return message.getExchange()
                      .getProperty(propertyName);
    }

    public Map<String, Object> getExchangeProperties() {
        return message.getExchange()
                      .getProperties();
    }

    /**
     * Sets the headers.
     *
     * @param headers the headers
     */
    public void setHeaders(Map<String, Object> headers) {
        message.setHeaders(headers);
    }

    /**
     * Sets the header.
     *
     * @param key the key
     * @param value the value
     */
    public void setHeader(String key, Object value) {
        message.setHeader(key, value);
    }

    /**
     * Gets the camel message.
     *
     * @return the camel message
     */
    public Message getCamelMessage() {
        return message;
    }
}
