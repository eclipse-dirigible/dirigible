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

import org.eclipse.dirigible.engine.java.annotations.Column;
import org.eclipse.dirigible.engine.java.annotations.CreatedAt;
import org.eclipse.dirigible.engine.java.annotations.Entity;
import org.eclipse.dirigible.engine.java.annotations.GeneratedValue;
import org.eclipse.dirigible.engine.java.annotations.GenerationType;
import org.eclipse.dirigible.engine.java.annotations.Id;
import org.eclipse.dirigible.engine.java.annotations.Table;
import org.eclipse.dirigible.engine.java.annotations.Transient;
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

    @Test
    void skips_transient_fields() {
        JavaEntityToHbmMapper.Result r = JavaEntityToHbmMapper.map("p::WithTransient", WithTransient.class);
        // The transient field's name should NOT appear as a <property> in the HBM XML.
        assertTrue(r.descriptor()
                     .serialize()
                     .indexOf("name=\"computed\"") < 0, "transient property must not be mapped");
    }

    @Test
    void rejects_class_without_id() {
        assertThrows(IllegalArgumentException.class, () -> JavaEntityToHbmMapper.map("p::NoId", NoId.class));
    }

    @Test
    void rejects_class_without_entity_annotation() {
        assertThrows(IllegalArgumentException.class, () -> JavaEntityToHbmMapper.map("p::Plain", Plain.class));
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
    public static class NoId {
        @Column
        public String name;
    }

    public static class Plain {
        @Id
        public Long id;
    }

}
