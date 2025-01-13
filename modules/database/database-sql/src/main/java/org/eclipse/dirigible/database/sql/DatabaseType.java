/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql;

/**
 * The Enum DatabaseType.
 */
public enum DatabaseType {

    /** The rdbms. */
    RDBMS("RDBMS"),

    /** The nosql. */
    NOSQL("NOSQL");

    /** The name. */
    private String name;

    /**
     * Instantiates a new database type.
     *
     * @param name the name
     */
    DatabaseType(String name) {
        this.name = name;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * To string.
     *
     * @return the string
     */
    public String toString() {
        return name;
    }

    /**
     * Value of by name.
     *
     * @param name the name
     * @return the database type
     */
    public static final DatabaseType valueOfByName(String name) {
        for (DatabaseType type : DatabaseType.class.getEnumConstants()) {
            if (type.toString()
                    .equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("DatabaseType not found: " + name);
    }

}
