/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.domain;

import java.util.List;

/**
 * The Column Metadata transport object.
 */
public class NoSQLColumnMetadata {

    /** The name. */
    private String name;

    /** The type. */
    private String type;

    /** The size. */
    private int size;

    /** The nullable. */
    private boolean nullable;

    /** The key. */
    private boolean key;

    /** The kind. */
    private String kind = "column";

    /** The scale. */
    private int scale;

    /** The columns. */
    private List<NoSQLColumnMetadata> columns;

    /**
     * Instantiates a new column metadata.
     *
     * @param name the name
     * @param type the type
     * @param size the size
     * @param nullable the nullable
     * @param key the key
     * @param scale the scale
     */
    public NoSQLColumnMetadata(String name, String type, int size, boolean nullable, boolean key, int scale) {
        super();
        this.name = name;
        this.type = type;
        this.size = size;
        this.nullable = nullable;
        this.key = key;
        this.scale = scale;
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
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the new type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the size.
     *
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the size.
     *
     * @param size the new size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Checks if is nullable.
     *
     * @return true, if is nullable
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Sets the nullable.
     *
     * @param nullable the new nullable
     */
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    /**
     * Checks if is key.
     *
     * @return true, if is key
     */
    public boolean isKey() {
        return key;
    }

    /**
     * Sets the key.
     *
     * @param key the new key
     */
    public void setKey(boolean key) {
        this.key = key;
    }

    /**
     * Gets the kind.
     *
     * @return the kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the kind.
     *
     * @param kind the new kind
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * Gets the scale.
     *
     * @return the scale
     */
    public int getScale() {
        return scale;
    }

    /**
     * Sets the scale.
     *
     * @param scale the scale to set
     */
    public void setScale(int scale) {
        this.scale = scale;
    }

    /**
     * Gets the columns.
     *
     * @return the columns
     */
    public List<NoSQLColumnMetadata> getColumns() {
        return columns;
    }

    /**
     * Sets the columns.
     *
     * @param columns the new columns
     */
    public void setColumns(List<NoSQLColumnMetadata> columns) {
        this.columns = columns;
    }



}
