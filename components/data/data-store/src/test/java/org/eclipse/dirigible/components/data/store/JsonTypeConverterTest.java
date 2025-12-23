/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata;
import org.eclipse.dirigible.components.data.store.model.EntityMetadata;
import org.eclipse.dirigible.components.data.store.parser.EntityParser;
import org.junit.jupiter.api.Test;

public class JsonTypeConverterTest {

    @Test
    public void testNormalizeNumericAndTemporalTypes() {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", Double.valueOf(123.0));
        m.put("id", Integer.valueOf(7));
        m.put("age", Double.valueOf(30.0));
        m.put("height", Double.valueOf(180.5));
        m.put("big", new BigDecimal("500.00"));
        m.put("bdNonInt", new BigDecimal("12.34"));
        m.put("bytes", new byte[] {1, 2, 3});
        Date date = new Date(System.currentTimeMillis());
        Time time = new Time(System.currentTimeMillis());
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        m.put("date", date);
        m.put("time", time);
        m.put("timestamp", ts);

        JsonTypeConverter.normalizeNumericTypes(m);

        assertTrue(m.get("userId") instanceof Long);
        assertEquals(123L, ((Long) m.get("userId")).longValue());

        assertTrue(m.get("id") instanceof Long);
        assertEquals(7L, ((Long) m.get("id")).longValue());

        // whole double becomes Long
        assertTrue(m.get("age") instanceof Long);
        assertEquals(30L, ((Long) m.get("age")).longValue());

        // fractional double remains Double
        assertTrue(m.get("height") instanceof Double);
        assertEquals(180.5, ((Double) m.get("height")).doubleValue(), 0.00001);

        assertTrue(m.get("big") instanceof Long);
        assertEquals(500L, ((Long) m.get("big")).longValue());

        assertTrue(m.get("bdNonInt") instanceof BigDecimal);

        assertTrue(m.get("bytes") instanceof byte[]);
        assertArrayEquals(new byte[] {1, 2, 3}, (byte[]) m.get("bytes"));

        assertTrue(m.get("date") instanceof Date);
        assertTrue(m.get("time") instanceof Time);
        assertTrue(m.get("timestamp") instanceof Timestamp);
    }

    @Test
    public void testNestedMapNormalization() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("profileId", Double.valueOf(999.0));

        Map<String, Object> root = new HashMap<>();
        root.put("user", nested);

        JsonTypeConverter.normalizeNumericTypes(root);

        Map<?, ?> resultNested = (Map<?, ?>) root.get("user");
        assertTrue(resultNested.get("profileId") instanceof Long);
        assertEquals(999L, ((Long) resultNested.get("profileId")).longValue());
    }

    @Test
    public void testParseStringsToTemporalAndBinary() {
        Map<String, Object> m = new HashMap<>();
        // ISO instant -> Timestamp
        m.put("createdAt", "2025-12-23T12:34:56Z");
        // Date only -> java.sql.Date
        m.put("birthDate", "2025-12-23");
        // Time only -> java.sql.Time
        m.put("wakeTime", "12:34:56");
        // base64 payload on a key that hints binary
        byte[] payload = new byte[] {1, 2, 3, 4};
        String b64 = java.util.Base64.getEncoder()
                                     .encodeToString(payload);
        m.put("fileBytes", b64);

        JsonTypeConverter.normalizeNumericTypes(m);

        assertTrue(m.get("createdAt") instanceof Timestamp);
        assertTrue(m.get("birthDate") instanceof Date);
        assertTrue(m.get("wakeTime") instanceof Time);
        assertTrue(m.get("fileBytes") instanceof byte[]);
        assertArrayEquals(payload, (byte[]) m.get("fileBytes"));
    }

    @Test
    public void testNormalizeForEntityUsingMetadata() {
        EntityMetadata meta = new EntityMetadata();

        EntityFieldMetadata tsField = new EntityFieldMetadata();
        tsField.setPropertyName("ValidFrom");
        EntityFieldMetadata.ColumnDetails tsCol = new EntityFieldMetadata.ColumnDetails();
        tsCol.setDatabaseType("timestamp");
        tsField.setColumnDetails(tsCol);

        EntityFieldMetadata decField = new EntityFieldMetadata();
        decField.setPropertyName("Amount");
        EntityFieldMetadata.ColumnDetails decCol = new EntityFieldMetadata.ColumnDetails();
        decCol.setDatabaseType("decimal");
        decField.setColumnDetails(decCol);

        EntityFieldMetadata uuidField = new EntityFieldMetadata();
        uuidField.setPropertyName("UUID");
        EntityFieldMetadata.ColumnDetails uuidCol = new EntityFieldMetadata.ColumnDetails();
        uuidCol.setDatabaseType("uuid");
        uuidField.setColumnDetails(uuidCol);

        EntityFieldMetadata blobField = new EntityFieldMetadata();
        blobField.setPropertyName("SomeBlob");
        EntityFieldMetadata.ColumnDetails blobCol = new EntityFieldMetadata.ColumnDetails();
        blobCol.setDatabaseType("bytea");
        blobField.setColumnDetails(blobCol);

        meta.getFields()
            .add(tsField);
        meta.getFields()
            .add(decField);
        meta.getFields()
            .add(uuidField);
        meta.getFields()
            .add(blobField);

        // register in parser map
        EntityParser.ENTITIES.put("TestEntity", meta);

        Map<String, Object> m = new HashMap<>();
        m.put("ValidFrom", "2025-12-23T12:34:56Z");
        m.put("Amount", Double.valueOf(0.1));
        m.put("UUID", "550e8400-e29b-41d4-a716-446655440000");
        byte[] payload = new byte[] {9, 8, 7};
        m.put("SomeBlob", java.util.Base64.getEncoder()
                                          .encodeToString(payload));

        JsonTypeConverter.normalizeForEntity(m, "TestEntity");

        assertTrue(m.get("ValidFrom") instanceof Timestamp);
        assertTrue(m.get("Amount") instanceof BigDecimal);
        assertTrue(m.get("UUID") instanceof java.util.UUID);
        assertTrue(m.get("SomeBlob") instanceof byte[]);
        assertArrayEquals(payload, (byte[]) m.get("SomeBlob"));
    }
}
