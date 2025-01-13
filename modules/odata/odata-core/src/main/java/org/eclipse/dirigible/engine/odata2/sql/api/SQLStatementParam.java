/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.api;

import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.core.edm.EdmNull;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding;

/**
 * The Class SQLStatementParam.
 */
public class SQLStatementParam {

    /** The value. */
    private final Object value;

    /** The edm type. */
    private final EdmType edmType;

    /** The column info. */
    private final EdmTableBinding.ColumnInfo columnInfo;

    /**
     * The Enum TemporalType.
     */
    public enum TemporalType {

        /** The date. */
        DATE,
        /** The time. */
        TIME,
        /** The timestamp. */
        TIMESTAMP
    }

    /**
     * Instantiates a new SQL statement param.
     *
     * @param value the value
     * @param edmProperty the edm property
     * @param columnInfo the column info
     * @throws EdmException the edm exception
     */
    public SQLStatementParam(Object value, EdmProperty edmProperty, EdmTableBinding.ColumnInfo columnInfo) throws EdmException {
        this.value = value;
        this.edmType = edmProperty.getType();
        this.columnInfo = columnInfo;
    }

    /**
     * Instantiates a new SQL statement param.
     *
     * @param value the value
     * @param edmType the edm type
     * @param columnInfo the column info
     */
    public SQLStatementParam(Object value, EdmType edmType, EdmTableBinding.ColumnInfo columnInfo) {
        this.value = value;
        this.edmType = edmType;
        this.columnInfo = columnInfo;
    }

    /**
     * Checks if is temporal type.
     *
     * @return true, if is temporal type
     */
    public boolean isTemporalType() {
        return getTemporalType() != null;
    }

    /**
     * Gets the temporal type.
     *
     * @return the temporal type
     */
    public TemporalType getTemporalType() {
        EdmType edmType = getEdmType();
        if (EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                                      .equals(edmType)) {
            return TemporalType.TIMESTAMP;
        } else if (EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()
                                         .equals(edmType)) {
            return TemporalType.TIME;
        } else if (EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                                                   .equals(edmType)) {
            return TemporalType.DATE;
        } else {
            return null;
        }
    }

    /**
     * Gets the edm simple kind.
     *
     * @return the edm simple kind
     */
    public EdmSimpleTypeKind getEdmSimpleKind() {
        if (EdmSimpleTypeKind.String.getEdmSimpleTypeInstance()
                                    .equals(edmType)) {
            return EdmSimpleTypeKind.String;
        }
        if (EdmSimpleTypeKind.Int16.getEdmSimpleTypeInstance()
                                   .equals(edmType)) {
            return EdmSimpleTypeKind.Int16;
        }
        if (EdmSimpleTypeKind.Int32.getEdmSimpleTypeInstance()
                                   .equals(edmType)) {
            return EdmSimpleTypeKind.Int32;
        }
        if (EdmSimpleTypeKind.Int64.getEdmSimpleTypeInstance()
                                   .equals(edmType)) {
            return EdmSimpleTypeKind.Int64;
        }
        if (EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance()
                                    .equals(edmType)) {
            return EdmSimpleTypeKind.Double;
        }
        if (EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance()
                                     .equals(edmType)) {
            return EdmSimpleTypeKind.Decimal;
        }
        if (EdmSimpleTypeKind.Boolean.getEdmSimpleTypeInstance()
                                     .equals(edmType)) {
            return EdmSimpleTypeKind.Boolean;
        }
        if (EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                                      .equals(edmType)) {
            return EdmSimpleTypeKind.DateTime;
        }
        if (EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                                            .equals(edmType)) {
            return EdmSimpleTypeKind.DateTimeOffset;
        }
        if (EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()
                                  .equals(edmType)) {
            return EdmSimpleTypeKind.Time;
        }
        if (EdmSimpleTypeKind.Guid.getEdmSimpleTypeInstance()
                                  .equals(edmType)) {
            return EdmSimpleTypeKind.Guid;
        }
        if (EdmSimpleTypeKind.Binary.getEdmSimpleTypeInstance()
                                    .equals(edmType)) {
            return EdmSimpleTypeKind.Binary;
        }
        if (EdmSimpleTypeKind.SByte.getEdmSimpleTypeInstance()
                                   .equals(edmType)) {
            return EdmSimpleTypeKind.SByte;
        }
        if (EdmSimpleTypeKind.Single.getEdmSimpleTypeInstance()
                                    .equals(edmType)) {
            return EdmSimpleTypeKind.Single;
        }
        if (edmType instanceof EdmNull) {
            return EdmSimpleTypeKind.Null;
        }
        if (EdmSimpleTypeKind.Byte.getEdmSimpleTypeInstance()
                                  .equals(edmType)) {
            return EdmSimpleTypeKind.Byte;
        }
        if (EdmSimpleTypeKind.Int32.getEdmSimpleTypeInstance()
                                   .isCompatible((EdmSimpleType) edmType)) {
            return EdmSimpleTypeKind.Int32;
        }

        throw new IllegalStateException("Unexpected EdmType - " + edmType);
    }

    /**
     * Gets the edm type.
     *
     * @return the edm type
     */
    public EdmType getEdmType() {
        return edmType;
    }

    /**
     * Gets the sql type.
     *
     * @return the sql type
     */
    public String getSqlType() {
        return columnInfo == null ? null : columnInfo.getJdbcType();
    }

    /**
     * Gets the sql column name.
     *
     * @return the sql column name
     */
    public String getSqlColumnName() {
        return columnInfo == null ? null : columnInfo.getColumnName();
    }

    /**
     * Gets the value.
     *
     * @param <T> the generic type
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }
}
