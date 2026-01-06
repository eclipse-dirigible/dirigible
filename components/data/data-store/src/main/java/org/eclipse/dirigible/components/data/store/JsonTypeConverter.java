/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata;
import org.eclipse.dirigible.components.data.store.model.EntityMetadata;
import org.eclipse.dirigible.components.data.store.parser.EntityParser;

/**
 * Utility class to recursively traverse a nested Map (deserialized from JSON) and safely convert
 * numeric types (Double/Float) into Long objects if they represent whole numbers or are identified
 * as ID fields.
 */
public class JsonTypeConverter {

    /**
     * Recursively traverses the map and normalizes types that commonly come from JSON/GSON so they are
     * friendlier for Hibernate persistence. This includes: - converting floating-point values that are
     * whole numbers into Long - converting numeric id fields (ending with "id") into Long - preserving
     * SQL Date/Time/Timestamp and byte[] values
     *
     * @param data The map object deserialized from JSON.
     * @return The mutated map with normalized number types.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeNumericTypes(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        for (Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                normalizeNumericTypes(nestedMap);
            } else if (value instanceof List) {
                // Normalize elements inside lists (maps, numbers, strings -> typed values)
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object elem = list.get(i);
                    if (elem instanceof Map) {
                        normalizeNumericTypes((Map<String, Object>) elem);
                    } else if (elem instanceof Number) {
                        list.set(i, normalizeNumber((Number) elem, key.toLowerCase(Locale.ROOT)
                                                                      .endsWith("id")));
                    } else if (elem instanceof String) {
                        Object parsed = tryParseTemporalOrBinary((String) elem, key);
                        if (parsed != null) {
                            list.set(i, parsed);
                        }
                    }
                }
            } else if (value instanceof byte[]) {
                // leave binary blobs as-is
            } else if (value instanceof Date || value instanceof Time || value instanceof Timestamp) {
                // preserve SQL temporal types as-is
            } else if (value instanceof String) {
                // Try to parse date/time/timestamp strings and base64-encoded binaries
                Object parsed = tryParseTemporalOrBinary((String) value, key);
                if (parsed != null) {
                    entry.setValue(parsed);
                }
            } else if (value instanceof Number) {
                boolean isIdKey = key.toLowerCase(Locale.ROOT)
                                     .endsWith("id");
                Object normalizedValue = normalizeNumber((Number) value, isIdKey);
                if (normalizedValue != value) {
                    entry.setValue(normalizedValue);
                }
            }
        }
        return data;
    }

    /**
     * Convert JDBC LOB types present in a result map into script-friendly primitives: - Blob -> byte[]
     * - Clob -> String Recurses into nested maps and lists.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeDbLobsToPrimitives(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        for (Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                normalizeDbLobsToPrimitives((Map<String, Object>) value);
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object elem = list.get(i);
                    if (elem instanceof Map) {
                        normalizeDbLobsToPrimitives((Map<String, Object>) elem);
                    } else if (elem instanceof java.sql.Blob) {
                        try {
                            java.sql.Blob blob = (java.sql.Blob) elem;
                            byte[] bytes = blob.getBytes(1, (int) blob.length());
                            list.set(i, bytes);
                        } catch (Exception e) {
                            // leave original on error
                        }
                    } else if (elem instanceof java.sql.Clob) {
                        try {
                            java.sql.Clob clob = (java.sql.Clob) elem;
                            String s = clob.getSubString(1, (int) clob.length());
                            list.set(i, s);
                        } catch (Exception e) {
                            // leave original
                        }
                    }
                }
            } else if (value instanceof java.sql.Blob) {
                try {
                    java.sql.Blob blob = (java.sql.Blob) value;
                    byte[] bytes = blob.getBytes(1, (int) blob.length());
                    entry.setValue(bytes);
                } catch (Exception e) {
                    // leave original on error
                }
            } else if (value instanceof java.sql.Clob) {
                try {
                    java.sql.Clob clob = (java.sql.Clob) value;
                    String s = clob.getSubString(1, (int) clob.length());
                    entry.setValue(s);
                } catch (Exception e) {
                    // leave original
                }
            }
        }

        return data;
    }

    /**
     * Normalize map values using entity metadata if available. Falls back to general heuristics when
     * metadata is missing.
     *
     * @param data the map to normalize
     * @param entityName the entity name (used to look up EntityMetadata)
     * @return the normalized map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeForEntity(Map<String, Object> data, String entityName) {
        if (data == null) {
            return null;
        }

        // First apply general numeric heuristics
        normalizeNumericTypes(data);

        EntityMetadata metadata = EntityParser.ENTITIES.get(entityName);
        if (metadata == null) {
            return data; // no entity metadata available
        }

        for (EntityFieldMetadata field : metadata.getFields()) {
            String prop = field.getPropertyName();
            if (prop == null || !data.containsKey(prop)) {
                continue;
            }
            Object value = data.get(prop);
            if (value == null) {
                continue;
            }

            // If collection, recurse inside
            if (field.isCollection() && value instanceof List) {
                List<Object> list = (List<Object>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object elem = list.get(i);
                    if (elem instanceof Map) {
                        normalizeForEntity((Map<String, Object>) elem, field.getCollectionDetails() != null ? field.getCollectionDetails()
                                                                                                                   .getEntityName()
                                : null);
                    }
                }
                continue;
            }

            // Use column details database type if provided
            String dbType = null;
            if (field.getColumnDetails() != null && field.getColumnDetails()
                                                         .getDatabaseType() != null) {
                dbType = field.getColumnDetails()
                              .getDatabaseType()
                              .toLowerCase(Locale.ROOT);
            }

            try {
                if (dbType != null) {
                    if (dbType.contains("timestamp") || dbType.contains("datetime")) {
                        if (value instanceof String) {
                            Object parsed = tryParseTemporalOrBinary((String) value, prop);
                            if (parsed instanceof Timestamp) {
                                data.put(prop, parsed);
                            } else if (parsed instanceof Date) {
                                // promote date -> timestamp at start of day
                                data.put(prop, new Timestamp(((Date) parsed).getTime()));
                            }
                        }
                        continue;
                    }

                    if (dbType.contains("date") && !dbType.contains("time")) {
                        if (value instanceof String) {
                            try {
                                LocalDate ld = LocalDate.parse((String) value);
                                data.put(prop, Date.valueOf(ld));
                            } catch (DateTimeParseException e) {
                                // fallback to heuristic
                            }
                        }
                        continue;
                    }

                    if (dbType.contains("time") && !dbType.contains("date")) {
                        if (value instanceof String) {
                            try {
                                LocalTime lt = LocalTime.parse((String) value);
                                data.put(prop, Time.valueOf(lt));
                            } catch (DateTimeParseException e) {
                                // fallback
                            }
                        }
                        continue;
                    }

                    if (dbType.contains("uuid")) {
                        if (value instanceof String) {
                            try {
                                UUID uuid = UUID.fromString((String) value);
                                data.put(prop, uuid);
                            } catch (IllegalArgumentException e) {
                                // keep as string
                            }
                        }
                        continue;
                    }

                    if (dbType.contains("decimal") || dbType.contains("numeric")) {
                        if (value instanceof Number) {
                            data.put(prop, new BigDecimal(value.toString()));
                        } else if (value instanceof String) {
                            try {
                                data.put(prop, new BigDecimal((String) value));
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                        }
                        continue;
                    }

                    if (dbType.contains("double")) {
                        if (value instanceof Number) {
                            data.put(prop, Double.valueOf(value.toString()));
                        } else if (value instanceof String) {
                            try {
                                data.put(prop, Double.valueOf((String) value));
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                        }
                        continue;
                    }

                    if (dbType.contains("float") || dbType.contains("real")) {
                        if (value instanceof Number) {
                            data.put(prop, Float.valueOf(value.toString()));
                        } else if (value instanceof String) {
                            try {
                                data.put(prop, Float.valueOf((String) value));
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                        }
                        continue;
                    }


                    if (dbType.contains("blob")) {
                        // For BLOB columns keep raw bytes (byte[]) to match array expectations
                        if (value instanceof String) {
                            Object parsed = tryParseTemporalOrBinary((String) value, prop);
                            if (parsed instanceof byte[]) {
                                data.put(prop, new SerialBlob((byte[]) parsed));
                            }
                        } else if (value instanceof List) {
                            // Convert list of numbers to byte[]
                            List<?> list = (List<?>) value;
                            byte[] arr = new byte[list.size()];
                            for (int i = 0; i < list.size(); i++) {
                                Object elem = list.get(i);
                                if (elem instanceof Number) {
                                    arr[i] = ((Number) elem).byteValue();
                                } else if (elem instanceof String) {
                                    try {
                                        arr[i] = (byte) Integer.parseInt((String) elem);
                                    } catch (NumberFormatException e) {
                                        arr[i] = 0;
                                    }
                                } else {
                                    arr[i] = 0;
                                }
                            }
                            data.put(prop, new SerialBlob(arr));
                        } else if (value instanceof byte[]) {
                            data.put(prop, new SerialBlob((byte[]) value));
                        }
                        continue;
                    }

                    if (dbType.contains("bytea") || dbType.contains("binary")) {
                        // Keep raw bytes for these DB types
                        if (value instanceof String) {
                            Object parsed = tryParseTemporalOrBinary((String) value, prop);
                            if (parsed instanceof byte[]) {
                                data.put(prop, parsed);
                            }
                        } else if (value instanceof List) {
                            List<?> list = (List<?>) value;
                            byte[] arr = new byte[list.size()];
                            for (int i = 0; i < list.size(); i++) {
                                Object elem = list.get(i);
                                if (elem instanceof Number) {
                                    arr[i] = ((Number) elem).byteValue();
                                } else if (elem instanceof String) {
                                    try {
                                        arr[i] = (byte) Integer.parseInt((String) elem);
                                    } catch (NumberFormatException e) {
                                        arr[i] = 0;
                                    }
                                } else {
                                    arr[i] = 0;
                                }
                            }
                            data.put(prop, arr);
                        }
                        continue;
                    }

                    if (dbType.contains("clob")) {
                        if (value instanceof String) {
                            try {
                                Clob clob = new SerialClob(((String) value).toCharArray());
                                data.put(prop, clob);
                            } catch (Exception e) {
                                // best-effort: keep as original String on failure
                            }
                        }
                        continue;
                    }

                    // Integer types
                    if (dbType.contains("bigint")) {
                        if (value instanceof Number) {
                            data.put(prop, Long.valueOf(((Number) value).longValue()));
                        }
                        continue;
                    }

                    if (dbType.contains("long")) {
                        if (value instanceof Number) {
                            data.put(prop, Long.valueOf(((Number) value).longValue()));
                        }
                        continue;
                    }

                    if (dbType.contains("int") || dbType.contains("integer") || dbType.contains("serial")) {
                        if (value instanceof Number) {
                            data.put(prop, Integer.valueOf(((Number) value).intValue()));
                        }
                        continue;
                    }

                    if (dbType.contains("smallint") || dbType.contains("short")) {
                        if (value instanceof Number) {
                            data.put(prop, Short.valueOf(((Number) value).shortValue()));
                        }
                        continue;
                    }

                    if (dbType.contains("tinyint")) {
                        if (value instanceof Number) {
                            data.put(prop, Byte.valueOf(((Number) value).byteValue()));
                        }
                        continue;
                    }
                }
            } catch (Exception e) {
                // best-effort conversion, ignore exceptions to preserve original value
            }
        }

        return data;
    }

    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/\\s]+={0,2}$");

    private static Object tryParseTemporalOrBinary(String s, String key) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        String lowerKey = key == null ? "" : key.toLowerCase(Locale.ROOT);

        // Try to parse as ISO instant/offset/local datetime -> Timestamp
        try {
            Instant inst = Instant.parse(s);
            return Timestamp.from(inst);
        } catch (DateTimeParseException e) {
            // ignore
        }

        try {
            OffsetDateTime odt = OffsetDateTime.parse(s);
            return Timestamp.from(odt.toInstant());
        } catch (DateTimeParseException e) {
            // ignore
        }

        try {
            LocalDateTime ldt = LocalDateTime.parse(s);
            return Timestamp.valueOf(ldt);
        } catch (DateTimeParseException e) {
            // ignore
        }

        // Date only
        try {
            LocalDate ld = LocalDate.parse(s);
            return Date.valueOf(ld);
        } catch (DateTimeParseException e) {
            // ignore
        }

        // Time only
        try {
            LocalTime lt = LocalTime.parse(s);
            return Time.valueOf(lt);
        } catch (DateTimeParseException e) {
            // ignore
        }

        // Heuristic base64 detection for binary fields
        if ((lowerKey.endsWith("bytes")
                || lowerKey.endsWith("blob") || lowerKey.contains("binary") || lowerKey.endsWith("data") || lowerKey.endsWith("base64"))
                && BASE64_PATTERN.matcher(s)
                                 .matches()) {
            try {
                byte[] decoded = Base64.getDecoder()
                                       .decode(s);
                if (decoded.length > 0) {
                    return decoded;
                }
            } catch (IllegalArgumentException e) {
                // not base64
            }
        }

        // Heuristic detection for CLOB/text fields
        if (lowerKey.contains("clob")) {
            try {
                // SerialClob accepts a char[]; safe best-effort creation
                return new SerialClob(s.toCharArray());
            } catch (Exception e) {
                // ignore and fall through to keep original String
            }
        }

        return null;
    }

    /**
     * Safely converts an object value into a Long if it represents a whole number, or if the key is an
     * ID key.
     *
     * @param value The raw object value (Double, Float, etc.).
     * @return The original object or the newly converted Long object.
     */
    private static Object normalizeNumber(Number number, boolean isIdKey) {
        if (number == null) {
            return null;
        }

        // If the number is already a Long and not an id conversion request, keep it
        if (number instanceof Long && !isIdKey) {
            return number;
        }

        // Small integer types: optionally convert to Long if this is an id field
        if (number instanceof Integer || number instanceof Short || number instanceof Byte) {
            if (isIdKey) {
                return Long.valueOf(number.longValue());
            }
            return number;
        }

        // Floating point numbers: convert to Long if they are whole numbers
        if (number instanceof Float || number instanceof Double) {
            double d = number.doubleValue();
            long l = (long) d;
            if (d == l) {
                return Long.valueOf(l);
            }
            return number;
        }

        // BigDecimal: if it represents a whole number convert to Long; if this is an id field
        // try to convert as well (exact if possible)
        if (number instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) number;
            try {
                BigDecimal stripped = bd.stripTrailingZeros();
                if (stripped.scale() <= 0) {
                    return Long.valueOf(bd.longValue());
                }
            } catch (ArithmeticException e) {
                // fall through
            }
            if (isIdKey) {
                try {
                    return Long.valueOf(((BigDecimal) number).longValueExact());
                } catch (ArithmeticException e) {
                    // cannot convert exactly; keep original BigDecimal
                }
            }
            return number;
        }

        // Fallback: for id keys prefer Long conversion
        if (isIdKey) {
            return Long.valueOf(number.longValue());
        }

        return number;
    }
}
