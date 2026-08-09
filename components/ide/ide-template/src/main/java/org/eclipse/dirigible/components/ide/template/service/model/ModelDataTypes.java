/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import java.util.Locale;

/**
 * Maps a model property's SQL data type onto the type names the generated artefacts need.
 */
final class ModelDataTypes {

    /**
     * The three type names a property's SQL type resolves to.
     *
     * @param java the coarse type name the older templates branch on
     * @param typescript the TypeScript type name, also used as the widget-class discriminator
     * @param javaClass the Java class to emit in generated Java sources
     */
    record DataType(String java, String typescript, String javaClass) {
    }

    /** The fallback for a type this mapping does not know. */
    private static final DataType UNKNOWN = new DataType("", "unknown", "Object");

    /**
     * Not instantiable.
     */
    private ModelDataTypes() {}

    /**
     * Resolves a SQL data type.
     *
     * @param dataType the SQL type name, case-insensitive
     * @return the mapped type names, never null
     */
    static DataType parse(String dataType) {
        if (dataType == null) {
            return UNKNOWN;
        }
        return switch (dataType.toUpperCase(Locale.ROOT)) {
            case "TINYINT", "INT1", "SMALLINT", "INT2", "SMALLSERIAL" -> new DataType("short", "number", "Short");
            case "MEDIUMINT", "INT3", "INT", "INT4", "INTEGER", "SERIAL" -> new DataType("int", "number", "Integer");
            case "BIGINT", "INT8", "BIGSERIAL" -> new DataType("long", "number", "Long");
            case "DECIMAL", "DEC", "NUMERIC", "FIXED" -> new DataType("double", "number", "java.math.BigDecimal");
            case "DOUBLE", "DOUBLE PRECISION", "REAL" -> new DataType("double", "number", "Double");
            case "FLOAT", "MONEY" -> new DataType("float", "number", "Float");
            case "CHAR", "ENUM", "INET4", "INET6", "TEXT", "TINYTEXT", "MEDIUMTEXT", "LONGTEXT", "VARCHAR", "LONG VARCHAR", "CHARACTER VARYING", "CHARACTER", "BPCHAR", "CLOB" -> new DataType(
                    "string", "string", "String");
            case "DATE" -> new DataType("date", "Date", "java.time.LocalDate");
            case "TIME", "TIME WITH TIME ZONE" -> new DataType("time", "string", "java.time.LocalTime");
            case "DATETIME", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE" -> new DataType("timestamp", "Date", "java.time.Instant");
            case "BOOLEAN", "BIT" -> new DataType("boolean", "boolean", "Boolean");
            case "BLOB" -> new DataType("blob", "string", "byte[]");
            case "NULL" -> new DataType("null", "null", "Object");
            default -> UNKNOWN;
        };
    }

    /**
     * Resolves the Java class to emit for a property, applying the overrides the audit annotations in
     * {@code org.eclipse.dirigible.sdk.db} demand: an audit timestamp is always an
     * {@code java.time.Instant} and an audit user is always a {@code String}, whatever the column's
     * declared type.
     *
     * @param baseJavaClass the class the SQL type maps to
     * @param auditType the property's audit role, may be null
     * @return the Java class to emit
     */
    static String resolveJavaClass(String baseJavaClass, String auditType) {
        if ("CREATED_AT".equals(auditType) || "UPDATED_AT".equals(auditType)) {
            return "java.time.Instant";
        }
        if ("CREATED_BY".equals(auditType) || "UPDATED_BY".equals(auditType)) {
            return "String";
        }
        return baseJavaClass == null || baseJavaClass.isEmpty() ? "Object" : baseJavaClass;
    }

}
