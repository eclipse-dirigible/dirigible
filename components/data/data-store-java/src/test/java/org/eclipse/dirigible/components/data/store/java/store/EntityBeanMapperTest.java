/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.eclipse.dirigible.components.data.store.java.hbm.JavaEntityToHbmMapper;
import org.eclipse.dirigible.components.data.store.java.manager.RegisteredEntity;
import org.eclipse.dirigible.engine.java.annotations.Column;
import org.eclipse.dirigible.engine.java.annotations.Entity;
import org.eclipse.dirigible.engine.java.annotations.Id;
import org.eclipse.dirigible.engine.java.annotations.Transient;
import org.junit.jupiter.api.Test;

class EntityBeanMapperTest {

    @Test
    void roundtrip_preserves_persistent_fields() {
        RegisteredEntity meta = JavaEntityToHbmMapper.map("p::Person", Person.class)
                                                      .registered();
        Person original = new Person();
        original.id = 42L;
        original.name = "Ada";
        original.age = 36;
        original.scratch = "ignored";

        Map<String, Object> data = EntityBeanMapper.toMap(original, meta);
        assertEquals(42L, data.get("id"));
        assertEquals("Ada", data.get("name"));
        assertEquals(36, data.get("age"));
        assertNull(data.get("scratch"), "@Transient is skipped");

        Person back = EntityBeanMapper.fromMap(Person.class, data, meta);
        assertEquals(42L, back.id);
        assertEquals("Ada", back.name);
        assertEquals(36, back.age);
        assertNull(back.scratch);
    }

    @Test
    void coerces_jdbc_numeric_widths() {
        RegisteredEntity meta = JavaEntityToHbmMapper.map("p::Person", Person.class)
                                                      .registered();
        // Hibernate may return java.math.BigInteger or java.lang.Integer for a long column on
        // different drivers; both should land back as Long.
        Map<String, Object> data = Map.of("id", java.math.BigInteger.valueOf(7), "name", "Linus", "age", 50L);
        Person back = EntityBeanMapper.fromMap(Person.class, data, meta);
        assertEquals(7L, back.id);
        assertEquals(50, back.age);
    }

    @Entity
    public static class Person {
        @Id
        public Long id;
        @Column
        public String name;
        @Column
        public int age;
        @Transient
        public String scratch;
    }

}
