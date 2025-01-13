/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.logs.dto;

/**
 * The Class LogInfo.
 */
public class LogInfo {

    /** The name. */
    private String name;

    /** The severity. */
    private String severity;

    /**
     * Instantiates a new log info.
     *
     * @param name the name
     * @param severity the severity
     */
    public LogInfo(String name, String severity) {
        super();
        this.name = name;
        this.severity = severity;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the severity.
     *
     * @return the severity
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the severity.
     *
     * @param severity the severity to set
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }



}
