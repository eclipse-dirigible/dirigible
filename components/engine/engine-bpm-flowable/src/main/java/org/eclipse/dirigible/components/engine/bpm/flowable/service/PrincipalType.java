/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.service;

public enum PrincipalType {

    /** The assignee. */
    ASSIGNEE("assignee"),
    /** The candidate groups. */
    CANDIDATE_GROUPS("groups");

    /** The type. */
    private final String type;

    /**
     * Instantiates a new type.
     *
     * @param type the type
     */
    PrincipalType(String type) {
        this.type = type;
    }

    /**
     * From string.
     *
     * @param type the type
     * @return the type
     */
    public static PrincipalType fromString(String type) {
        for (PrincipalType enumValue : values()) {
            if (enumValue.type.equals(type)) {
                return enumValue;
            }
        }
        throw new IllegalArgumentException("Unknown enum type: " + type);
    }
}
