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

/**
 * The Procedure Column Metadata transport object.
 */
public class ParameterColumnMetadata {

    /** The name. */
    private String name;

    /** The kind. */
    private int kind;

    /** The type. */
    private String type;

    /** The precision. */
    private int precision;

    /** The length. */
    private int length;

    /** The scale. */
    private int scale;

    /** The radix. */
    private int radix;

    /** The nullable. */
    private boolean nullable;

    /** The remarks. */
    private String remarks;

    /**
     * Procedure Column Metadata.
     *
     * @param name name
     * @param kind kind
     * @param type type
     * @param precision precision
     * @param length length
     * @param scale scale
     * @param radix radix
     * @param nullable nullable
     * @param remarks remarks
     */
    public ParameterColumnMetadata(String name, int kind, String type, int precision, int length, int scale, int radix, boolean nullable,
            String remarks) {
        super();
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.precision = precision;
        this.length = length;
        this.scale = scale;
        this.radix = radix;
        this.nullable = nullable;
        this.remarks = remarks;
    }

    /**
     * Get the metadata name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the metadata name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the metadata kind.
     *
     * @return the kind
     */
    public int getKind() {
        return kind;
    }

    /**
     * Set the metadata kind.
     *
     * @param kind the kind to set
     */
    public void setKind(int kind) {
        this.kind = kind;
    }

    /**
     * Get the metadata type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the metadata type.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the metadata precision.
     *
     * @return the precision
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * Set the metadata precision.
     *
     * @param precision the precision to set
     */
    public void setPrecision(int precision) {
        this.precision = precision;
    }

    /**
     * Get the metadata length.
     *
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * Set the metadata length.
     *
     * @param length the length to set
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Get the metadata scale.
     *
     * @return the scale
     */
    public int getScale() {
        return scale;
    }

    /**
     * Set the metadata scale.
     *
     * @param scale the scale to set
     */
    public void setScale(int scale) {
        this.scale = scale;
    }

    /**
     * Get the metadata radix.
     *
     * @return the radix
     */
    public int getRadix() {
        return radix;
    }

    /**
     * Set the metadata radix.
     *
     * @param radix the radix to set
     */
    public void setRadix(int radix) {
        this.radix = radix;
    }

    /**
     * Get the metadata nullable.
     *
     * @return the nullable
     */
    public boolean getNullable() {
        return nullable;
    }

    /**
     * Set the metadata nullable.
     *
     * @param nullable the nullable to set
     */
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    /**
     * Get the metadata remarks.
     *
     * @return the remarks
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * Set the metadata remarks.
     *
     * @param remarks the remarks to set
     */
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

}
