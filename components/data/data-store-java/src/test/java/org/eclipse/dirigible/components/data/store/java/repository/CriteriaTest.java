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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class CriteriaTest {

    @Test
    void emptyCriteriaRendersBareFrom() {
        Criteria criteria = Criteria.create();
        assertEquals("from BookEntity", criteria.append("from BookEntity"));
        assertTrue(criteria.parameters()
                           .isEmpty());
    }

    @Test
    void combinesConditionsWithAndAndBindsValues() {
        Criteria criteria = Criteria.create()
                                    .lt("dueOn", "2026-01-01")
                                    .eq("status", "ACTIVE")
                                    .orderByDesc("dueOn");

        assertEquals("from LoanEntity where dueOn < :p0 and status = :p1 order by dueOn desc", criteria.append("from LoanEntity"));
        assertEquals("2026-01-01", criteria.parameters()
                                           .get("p0"));
        assertEquals("ACTIVE", criteria.parameters()
                                       .get("p1"));
    }

    @Test
    void rendersBetweenInAndNullChecks() {
        Criteria between = Criteria.create()
                                   .between("price", 10, 20);
        assertEquals("from BookEntity where price between :p0 and :p1", between.append("from BookEntity"));
        assertEquals(20, between.parameters()
                                .get("p1"));

        Criteria in = Criteria.create()
                              .in("status", List.of("ACTIVE", "OVERDUE"));
        assertEquals("from LoanEntity where status in (:p0)", in.append("from LoanEntity"));
        assertEquals(List.of("ACTIVE", "OVERDUE"), in.parameters()
                                                     .get("p0"));

        Criteria nulls = Criteria.create()
                                 .isNull("returnedOn")
                                 .isNotNull("member");
        assertEquals("from LoanEntity where returnedOn is null and member is not null", nulls.append("from LoanEntity"));
        assertTrue(nulls.parameters()
                        .isEmpty());
    }

    @Test
    void aNullValueRendersANullSafePredicateRatherThanANeverTrueComparison() {
        // Issue #7134: `property = :p` with a null bind is never true in SQL's three-valued logic, not
        // even for a null column, so a lookup built from values that may be null matched NOTHING and
        // said nothing about it - an idempotency guard keyed on a nullable property created a duplicate
        // on every re-run while reporting that its target "already existed [0]" times.
        Criteria criteria = Criteria.create()
                                    .eq("customer", null)
                                    .eq("status", "ACTIVE")
                                    .ne("returnedOn", null);

        assertEquals("from LoanEntity where customer is null and status = :p0 and returnedOn is not null",
                criteria.append("from LoanEntity"));
        // Only the non-null value binds: a null is part of the predicate, never a parameter.
        assertEquals(1, criteria.parameters()
                                .size());
        assertEquals("ACTIVE", criteria.parameters()
                                       .get("p0"));
    }

    @Test
    void rejectsNonIdentifierPropertyNamesToPreventInjection() {
        assertThrows(IllegalArgumentException.class, () -> Criteria.create()
                                                                   .eq("status; drop table", "x"));
        assertThrows(IllegalArgumentException.class, () -> Criteria.create()
                                                                   .orderByAsc("1=1"));
    }
}
