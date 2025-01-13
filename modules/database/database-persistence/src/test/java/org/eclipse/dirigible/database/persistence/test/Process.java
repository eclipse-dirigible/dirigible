/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.test;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Class Process.
 */
@Table(name = "PROCESSES")
public class Process {

    /**
     * The Enum ProcessType.
     */
    enum ProcessType {

        /** The started. */
        STARTED,
        /** The stopped. */
        STOPPED,
        /** The failed. */
        FAILED,
        /** The inprogress. */
        INPROGRESS
    }

    /** The id. */
    @Id
    @GeneratedValue
    @Column(name = "PROCESS_ID", columnDefinition = "BIGINT", nullable = false)
    private long id;

    /** The name. */
    @Column(name = "PROCESS_NAME", columnDefinition = "VARCHAR", nullable = false, length = 512)
    private String name;

    /** The type as string. */
    @Column(name = "PROCESS_TYPE_AS_STRING")
    @Enumerated(EnumType.STRING)
    private ProcessType typeAsString;

    /** The type as int. */
    @Column(name = "PROCESS_TYPE_AS_INT")
    @Enumerated(EnumType.ORDINAL)
    private ProcessType typeAsInt;

    /**
     * Gets the id.
     *
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId(long id) {
        this.id = id;
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
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the type as string.
     *
     * @return the type as string
     */
    public ProcessType getTypeAsString() {
        return typeAsString;
    }

    /**
     * Sets the type as string.
     *
     * @param typeAsString the new type as string
     */
    public void setTypeAsString(ProcessType typeAsString) {
        this.typeAsString = typeAsString;
    }

    /**
     * Gets the type as int.
     *
     * @return the type as int
     */
    public ProcessType getTypeAsInt() {
        return typeAsInt;
    }

    /**
     * Sets the type as int.
     *
     * @param typeAsInt the new type as int
     */
    public void setTypeAsInt(ProcessType typeAsInt) {
        this.typeAsInt = typeAsInt;
    }

}
