/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.model;

/**
 * The column element of the persistence model transport object.
 */
public class PersistenceTableColumnModel {

    /** The field. */
    private String field;

    /** The name. */
    private String name;

    /** The type. */
    private String type;

    /** The length. */
    private int length;

    /** The nullable. */
    private boolean nullable;

    /** The primary key. */
    private boolean primaryKey;

    /** The scale. */
    private int scale;

    /** The generated. */
    private String generated;

    /** The unique. */
    private boolean unique;

    /** The identity. */
    private boolean identity;

    /** The enumerated. */
    private String enumerated;

    /**
     * The constructor from the fields.
     *
     * @param field the field
     * @param name the name
     * @param type the type
     * @param length the length
     * @param nullable whether null values are allowed
     * @param primaryKey whether it is a primary key
     * @param scale the scale for floating point values
     * @param generated whether it is a generated value
     * @param unique whether it is unique
     * @param identity whether it is identity
     * @param enumerated whether it is enumerated
     */
    public PersistenceTableColumnModel(String field, String name, String type, int length, boolean nullable, boolean primaryKey, int scale,
            String generated, boolean unique, boolean identity, String enumerated) {
        this.field = field;
        this.name = name;
        this.type = type;
        this.length = length;
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.scale = scale;
        this.generated = generated;
        this.unique = unique;
        this.identity = identity;
        this.enumerated = enumerated;
    }

    /**
     * Empty constructor.
     */
    public PersistenceTableColumnModel() {}

    /**
     * The constructor from the fields.
     *
     * @param name the name
     * @param type the type
     * @param nullable the nullable
     * @param primaryKey the primary key
     * @param length the length
     * @param scale the scale
     */
    public PersistenceTableColumnModel(String name, String type, boolean nullable, boolean primaryKey, int length, int scale) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.length = length;
        this.scale = scale;
    }

    /**
     * Getter for the field.
     *
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * Setter for the field.
     *
     * @param field the field
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * Getter for the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for the name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Setter for the type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Getter for the length.
     *
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * Setter for the length.
     *
     * @param length the length
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Check for nullable.
     *
     * @return true if can be null
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Setter for the nullable.
     *
     * @param nullable whether null values are allowed
     */
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    /**
     * Check for primary key.
     *
     * @return true if primary key
     */
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * Setter for the primary key.
     *
     * @param primaryKey whether it is a primary key
     */
    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * Getter for the scale.
     *
     * @return the scale
     */
    public int getScale() {
        return scale;
    }

    /**
     * Setter for the scale.
     *
     * @param scale the scale
     */
    public void setScale(int scale) {
        this.scale = scale;
    }

    /**
     * Check whether it is generated value.
     *
     * @return true if generated
     */
    public String getGenerated() {
        return generated;
    }

    /**
     * Setter for the generated.
     *
     * @param generated whether it is generated
     */
    public void setGenerated(String generated) {
        this.generated = generated;
    }

    /**
     * Check whether it is unique value.
     *
     * @return true if unique
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Setter for the unique.
     *
     * @param unique whether it is unique
     */
    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    /**
     * Check whether it is identity value.
     *
     * @return true if identity
     */
    public boolean isIdentity() {
        return identity;
    }

    /**
     * Setter for the identity.
     *
     * @param identity whether it is identity
     */
    public void setIdentity(boolean identity) {
        this.identity = identity;
    }

    /**
     * Returns the name of the enumerated type in case the column is of type enumerated or null
     * otherwise.
     *
     * @return enumerated type name
     */
    public String getEnumerated() {
        return enumerated;
    }

    /**
     * Setter for the enumerated type name.
     *
     * @param enumerated the new enumerated
     */
    public void setEnumerated(String enumerated) {
        this.enumerated = enumerated;
    }

}
