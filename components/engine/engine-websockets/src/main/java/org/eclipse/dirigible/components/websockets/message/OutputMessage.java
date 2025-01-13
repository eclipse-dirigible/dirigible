/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.message;

/**
 * The Class OutputMessage.
 */
public class OutputMessage {

    /** The from. */
    private String from;

    /** The text. */
    private String text;

    /** The time. */
    private String time;

    /**
     * Instantiates a new output message.
     *
     * @param from the from
     * @param text the text
     * @param time the time
     */
    public OutputMessage(final String from, final String text, final String time) {

        this.from = from;
        this.text = text;
        this.time = time;
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
     * Gets the time.
     *
     * @return the time
     */
    public String getTime() {
        return time;
    }

    /**
     * Gets the from.
     *
     * @return the from
     */
    public String getFrom() {
        return from;
    }
}
