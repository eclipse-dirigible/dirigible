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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.sdk.db.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The write-side reference check (dirigible #8100): a to-one naming a record that does not exist is
 * refused with a message naming the relation, the target being named by its generated entity class
 * so a cross-model reference needs no compile-time dependency on the owner model.
 */
class JavaRepositoryReferenceExistsTest {

    private static final String EMPLOYEE = "gen.staff.data.employee.EmployeeEntity";

    private JavaEntityStore store;

    private ClaimRepository repository;

    @BeforeEach
    void setUp() {
        store = mock(JavaEntityStore.class);
        repository = new ClaimRepository(store);
    }

    @Test
    void aReferenceToAnExistingRecordPasses() {
        when(store.isRegistered(EMPLOYEE)).thenReturn(true);
        when(store.existsById(EMPLOYEE, 41)).thenReturn(true);

        assertDoesNotThrow(() -> repository.requireReferenceExists("Employee", EMPLOYEE, 41));
    }

    @Test
    void aReferenceToAMissingRecordIsRefusedNamingTheRelation() {
        when(store.isRegistered(EMPLOYEE)).thenReturn(true);
        when(store.existsById(EMPLOYEE, 999999)).thenReturn(false);

        ValidationException refused =
                assertThrows(ValidationException.class, () -> repository.requireReferenceExists("Employee", EMPLOYEE, 999999));

        assertEquals("Employee 999999 does not exist", refused.getMessage());
    }

    /** An unset reference is the required check's business, not this one's - and costs no query. */
    @Test
    void anUnsetReferencePassesWithoutAQuery() {
        assertDoesNotThrow(() -> repository.requireReferenceExists("Employee", EMPLOYEE, null));

        verifyNoInteractions(store);
    }

    /** A target whose model is not deployed cannot be checked; the write is not refused for that. */
    @Test
    void aTargetThatIsNotRegisteredIsNotChecked() {
        when(store.isRegistered(EMPLOYEE)).thenReturn(false);

        assertDoesNotThrow(() -> repository.requireReferenceExists("Employee", EMPLOYEE, 41));

        verify(store, never()).existsById(EMPLOYEE, 41);
    }

    static class Claim {
    }

    static class ClaimRepository extends JavaRepository<Claim> {

        private final JavaEntityStore store;

        ClaimRepository(JavaEntityStore store) {
            super(Claim.class);
            this.store = store;
        }

        @Override
        protected JavaEntityStore store() {
            return store;
        }
    }
}
