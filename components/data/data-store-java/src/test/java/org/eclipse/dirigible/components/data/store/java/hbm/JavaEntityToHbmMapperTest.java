/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.hbm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.sdk.db.Column;
import org.eclipse.dirigible.sdk.db.CreatedAt;
import org.eclipse.dirigible.sdk.db.Entity;
import org.eclipse.dirigible.sdk.db.GeneratedValue;
import org.eclipse.dirigible.sdk.db.GenerationType;
import org.eclipse.dirigible.sdk.db.Id;
import org.eclipse.dirigible.sdk.db.Lob;
import org.eclipse.dirigible.sdk.db.Table;
import org.eclipse.dirigible.sdk.db.Transient;
import org.junit.jupiter.api.Test;

class JavaEntityToHbmMapperTest {

    @Test
    void maps_basic_entity_with_default_names() {
        JavaEntityToHbmMapper.Result r = JavaEntityToHbmMapper.map("p::Basic", Basic.class);

        assertEquals("Basic", r.registered()
                               .entityName());
        assertEquals("BASIC", r.registered()
                               .tableName(),
                "default table name is uppercase entity name");

        String xml = r.descriptor()
                      .serialize();
        assertTrue(xml.contains("entity-name=\"Basic\""), xml);
        assertTrue(xml.contains("table=\"`BASIC`\""), xml);
        assertTrue(xml.contains("<id name=\"id\""), xml);
        assertTrue(xml.contains("<generator class=\"identity\""), xml);
        assertTrue(xml.contains("name=\"name\""), xml);
    }

    @Test
    void honours_entity_and_column_name_overrides() {
        JavaEntityToHbmMapper.Result r = JavaEntityToHbmMapper.map("p::Renamed", Renamed.class);

        assertEquals("CountryEntity", r.registered()
                                       .entityName());
        assertEquals("COUNTRIES", r.registered()
                                   .tableName());

        String xml = r.descriptor()
                      .serialize();
        assertTrue(xml.contains("entity-name=\"CountryEntity\""), xml);
        assertTrue(xml.contains("table=\"`COUNTRIES`\""), xml);
        assertTrue(xml.contains("column=\"`COUNTRY_NAME`\""), xml);
    }

    @Test
    void records_audit_flag_for_createdAt() {
        JavaEntityToHbmMapper.Result r = JavaEntityToHbmMapper.map("p::Audited", Audited.class);
        assertEquals("createdAt", r.registered()
                                   .audit()
                                   .createdAtProperty());
        assertTrue(r.registered()
                    .audit()
                    .any());
    }

    /**
     * java.time.* properties must be emitted with their FQN as the Hibernate type. The short names
     * (e.g. "Instant") aren't registered in Hibernate's basic-type registry and would trip
     * {@code Class.forName("Instant")} → {@code ClassLoadingException} when the SessionFactory is
     * built.
     */
    @Test
    void emits_fqn_for_java_time_types() {
        String xml = JavaEntityToHbmMapper.map("p::Audited", Audited.class)
                                          .descriptor()
                                          .serialize();
        assertTrue(xml.contains("type=\"java.time.Instant\""), xml);
        assertTrue(xml.indexOf("type=\"Instant\"") < 0, "must not emit bare short name 'Instant': " + xml);
    }

    @Test
    void skips_transient_fields() {
        JavaEntityToHbmMapper.Result r = JavaEntityToHbmMapper.map("p::WithTransient", WithTransient.class);
        // The transient field's name should NOT appear as a <property> in the HBM XML.
        assertTrue(r.descriptor()
                    .serialize()
                    .indexOf("name=\"computed\"") < 0,
                "transient property must not be mapped");
    }

    @Test
    void rejects_class_without_id() {
        assertThrows(IllegalArgumentException.class, () -> JavaEntityToHbmMapper.map("p::NoId", NoId.class));
    }

    @Test
    void rejects_class_without_entity_annotation() {
        assertThrows(IllegalArgumentException.class, () -> JavaEntityToHbmMapper.map("p::Plain", Plain.class));
    }

    /**
     * A @Lob property must be mapped with a length past every dialect's maximum VARCHAR - that is how
     * Hibernate is told to keep the column at the database's own large-text type. Mapped as the
     * annotation's default 255 instead, the schema update narrows an existing CLOB column to a
     * VARCHAR(255).
     */
    @Test
    void maps_a_lob_property_as_unbounded_text() {
        JavaEntityToHbmMapper.Result r = JavaEntityToHbmMapper.map("p::LargeText", LargeText.class);

        String xml = r.descriptor()
                      .serialize();
        assertTrue(xml.contains("column=\"`BODY`\"") && xml.contains("length=\"" + Integer.MAX_VALUE + "\""),
                "the @Lob column must carry the unbounded length: " + xml);
        // A plain String property keeps the annotation's own length.
        assertTrue(xml.contains("length=\"255\""), "a plain String property keeps its declared length: " + xml);
    }

    // ---- fixtures ---------------------------------------------------------------------------

    @Entity
    public static class Basic {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @Column
        public String name;
    }

    @Entity(name = "CountryEntity")
    @Table(name = "COUNTRIES")
    public static class Renamed {
        @Id
        public Long id;
        @Column(name = "COUNTRY_NAME")
        public String name;
    }

    @Entity
    public static class Audited {
        @Id
        public Long id;
        @CreatedAt
        public java.time.Instant createdAt;
    }

    @Entity
    public static class WithTransient {
        @Id
        public Long id;
        @Column
        public String name;
        @Transient
        public String computed;
    }

    @Entity
    public static class LargeText {
        @Id
        public Long id;
        @Lob
        @Column(name = "BODY")
        public String body;
        @Column(name = "TITLE")
        public String title;
    }

    @Entity
    public static class NoId {
        @Column
        public String name;
    }

    public static class Plain {
        @Id
        public Long id;
    }

}
