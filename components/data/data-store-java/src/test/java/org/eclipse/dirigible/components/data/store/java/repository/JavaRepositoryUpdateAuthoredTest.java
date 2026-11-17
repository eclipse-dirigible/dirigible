/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.sdk.db.TargetAgreementRule;
import org.eclipse.dirigible.sdk.extensions.Extensions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * An authored targeted write carries the record's update-audit values in its one statement, and a
 * plain targeted write - the path the platform's derived and bookkeeping writes take - carries none
 * (issue #8690).
 */
class JavaRepositoryUpdateAuthoredTest {

    private static final String TOPIC = "vacations-VacationRequest-VacationRequest-transitioned";

    private static final Timestamp NOW = Timestamp.valueOf("2026-11-17 12:00:00");

    private JavaEntityStore store;

    private MockedStatic<BeanProvider> beans;

    private MockedStatic<Extensions> extensions;

    private RequestRepository repository;

    @BeforeEach
    void setUp() {
        store = mock(JavaEntityStore.class);
        beans = mockStatic(BeanProvider.class);
        beans.when(BeanProvider::isInitialzed)
             .thenReturn(true);
        extensions = mockStatic(Extensions.class);
        extensions.when(() -> Extensions.find(TargetAgreementRule.class))
                  .thenReturn(List.of());
        repository = new RequestRepository(store);
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("UpdatedAt", NOW);
        audit.put("UpdatedBy", "approver");
        when(store.updateAuditValues(Request.class)).thenReturn(audit);
        when(store.updateProperties(any(), any(), any(), any())).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        extensions.close();
        beans.close();
    }

    @Test
    void anAuthoredWriteCarriesTheAuditValuesInItsStatement() {
        assertEquals(1, repository.updateAuthored(42, Map.of("Status", 3), TOPIC));

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("Status", 3);
        expected.put("UpdatedAt", NOW);
        expected.put("UpdatedBy", "approver");
        verify(store).updateProperties(Request.class, 42, expected, TOPIC);
    }

    @Test
    void anAuthoredWriteWithoutATopicPublishesNothing() {
        repository.updateAuthored(42, Map.of("Status", 3));

        verify(store).updateProperties(Request.class, 42, Map.of("Status", 3, "UpdatedAt", NOW, "UpdatedBy", "approver"), null);
    }

    @Test
    void aValueTheCallerWritesIsNotOverwrittenByTheStamp() {
        repository.updateAuthored(42, Map.of("Status", 3, "UpdatedBy", "integration"), TOPIC);

        verify(store).updateProperties(Request.class, 42, Map.of("Status", 3, "UpdatedAt", NOW, "UpdatedBy", "integration"), TOPIC);
    }

    @Test
    void aPlainTargetedWriteStampsNothing() {
        repository.updateProperties(42, Map.of("Total", 10), TOPIC);

        verify(store).updateProperties(Request.class, 42, Map.of("Total", 10), TOPIC);
        verify(store, never()).updateAuditValues(any());
    }

    @Test
    void anEmptyAuthoredWriteWritesNothing() {
        JavaEntityStore untouched = mock(JavaEntityStore.class);

        assertEquals(0, new RequestRepository(untouched).updateAuthored(42, Map.of(), TOPIC));

        verifyNoInteractions(untouched);
    }

    static class Request {
    }

    static class RequestRepository extends JavaRepository<Request> {

        private final JavaEntityStore store;

        RequestRepository(JavaEntityStore store) {
            super(Request.class);
            this.store = store;
        }

        @Override
        protected JavaEntityStore store() {
            return store;
        }
    }
}
