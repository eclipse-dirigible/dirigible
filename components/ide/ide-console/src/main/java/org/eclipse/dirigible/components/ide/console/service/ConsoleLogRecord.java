/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.console.service;

/**
 * The Console Log Record transport object.
 */
public class ConsoleLogRecord {

    /** The level. */
    private String level;

    /** The message. */
    private String message;

    /** The timestamp. */
    private long timestamp;

    /**
     * Instantiates a new console log record.
     *
     * @param level the level
     * @param message the message
     * @param timestamp the timestamp
     */
    public ConsoleLogRecord(String level, String message, long timestamp) {
        super();
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    /**
     * Gets the level.
     *
     * @return the level
     */
    public String getLevel() {
        return level;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

}
