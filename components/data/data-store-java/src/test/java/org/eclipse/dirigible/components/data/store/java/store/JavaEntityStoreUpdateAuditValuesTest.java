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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.components.base.context.OriginatingUser;
import org.eclipse.dirigible.components.data.store.java.hbm.JavaEntityToHbmMapper;
import org.eclipse.dirigible.components.data.store.java.manager.JavaEntityManager;
import org.eclipse.dirigible.components.data.store.java.outbox.EventOutbox;
import org.eclipse.dirigible.sdk.db.CreatedAt;
import org.eclipse.dirigible.sdk.db.CreatedBy;
import org.eclipse.dirigible.sdk.db.Entity;
import org.eclipse.dirigible.sdk.db.GeneratedValue;
import org.eclipse.dirigible.sdk.db.GenerationType;
import org.eclipse.dirigible.sdk.db.Id;
import org.eclipse.dirigible.sdk.db.UpdatedAt;
import org.eclipse.dirigible.sdk.db.UpdatedBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The values an authored targeted write stamps (issue #8690): the update-audit properties only,
 * typed as the entity declares them, with the user acting on the thread.
 */
class JavaEntityStoreUpdateAuditValuesTest {

    private JavaEntityManager entityManager;

    private JavaEntityStore store;

    @BeforeEach
    void setUp() {
        entityManager = mock(JavaEntityManager.class);
        store = new JavaEntityStore(entityManager, mock(EventOutbox.class));
        register(Audited.class);
        register(Plain.class);
    }

    @Test
    void stampsTheUpdateAuditPropertiesWithTheActingUser() {
        Instant before = Instant.now();
        Map<String, Object> values;
        try (OriginatingUser.Binding approver = OriginatingUser.bind("approver")) {
            values = store.updateAuditValues(Audited.class);
        }

        assertEquals(2, values.size(), values.toString());
        assertEquals("approver", values.get("UpdatedBy"));
        Timestamp stamped = assertInstanceOf(Timestamp.class, values.get("UpdatedAt"));
        assertFalse(stamped.toInstant()
                           .isBefore(before));
    }

    @Test
    void leavesTheCreateAuditPropertiesAlone() {
        Map<String, Object> values = store.updateAuditValues(Audited.class);

        assertFalse(values.containsKey("CreatedAt"));
        assertFalse(values.containsKey("CreatedBy"));
        assertTrue(values.containsKey("UpdatedAt"));
    }

    @Test
    void anEntityWithoutAuditPropertiesStampsNothing() {
        assertTrue(store.updateAuditValues(Plain.class)
                        .isEmpty());
    }

    private void register(Class<?> type) {
        when(entityManager.findForClass(type)).thenReturn(Optional.of(JavaEntityToHbmMapper.map("p::" + type.getSimpleName(), type)
                                                                                           .registered()));
    }

    @Entity
    static class Audited {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Integer Id;

        public Integer Status;

        @CreatedAt
        public Timestamp CreatedAt;

        @CreatedBy
        public String CreatedBy;

        @UpdatedAt
        public Timestamp UpdatedAt;

        @UpdatedBy
        public String UpdatedBy;
    }

    @Entity
    static class Plain {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Integer Id;

        public Integer Status;
    }
}
