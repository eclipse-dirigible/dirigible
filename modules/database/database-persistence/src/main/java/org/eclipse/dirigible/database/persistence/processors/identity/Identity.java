/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.processors.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Identity transport object.
 */
@Table(name = "DIRIGIBLE_IDENTITY")
public class Identity {

    /** The table. */
    @Id
    @Column(name = "IDENTITY_TABLE", columnDefinition = "VARCHAR", nullable = false, length = 512)
    private String table;

    /** The value. */
    @Column(name = "IDENTITY_VALUE", columnDefinition = "BIGINT", nullable = false)
    private long value;

    /**
     * Gets the table.
     *
     * @return the table
     */
    public String getTable() {
        return table;
    }

    /**
     * Sets the table.
     *
     * @param table the new table
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public long getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the new value
     */
    public void setValue(long value) {
        this.value = value;
    }

}
