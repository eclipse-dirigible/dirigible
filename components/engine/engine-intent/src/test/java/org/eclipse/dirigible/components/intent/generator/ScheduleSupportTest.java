/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.components.intent.model.ScheduleConditionIntent;
import org.eclipse.dirigible.components.intent.model.ScheduleIntent;
import org.junit.jupiter.api.Test;

class ScheduleSupportTest {

    private static ScheduleConditionIntent cond(String field, String op, Object value) {
        ScheduleConditionIntent c = new ScheduleConditionIntent();
        c.setField(field);
        c.setOp(op);
        c.setValue(value);
        return c;
    }

    private static ScheduleIntent schedule(List<ScheduleConditionIntent> where) {
        ScheduleIntent s = new ScheduleIntent();
        s.setName("overdue");
        s.setWhere(where);
        return s;
    }

    @Test
    void emptyWhereIsAnUnfilteredCriteria() {
        assertEquals("Criteria.create()", ScheduleSupport.criteriaExpression(schedule(List.of())));
    }

    @Test
    void buildsTypedCriteriaWithPascalFieldsDateTokensAndLiterals() {
        ScheduleIntent s = schedule(List.of(cond("dueOn", "lt", "CURRENT_DATE"), cond("status", "eq", "ACTIVE")));
        assertEquals("Criteria.create().lt(\"DueOn\", java.time.LocalDate.now()).eq(\"Status\", \"ACTIVE\")",
                ScheduleSupport.criteriaExpression(s));
    }

    @Test
    void numbersAndTimestampTokensRenderWithoutQuotes() {
        ScheduleIntent s = schedule(List.of(cond("quantity", "gt", 1), cond("changedAt", "ge", "CURRENT_TIMESTAMP")));
        assertEquals("Criteria.create().gt(\"Quantity\", 1).ge(\"ChangedAt\", java.time.LocalDateTime.now())",
                ScheduleSupport.criteriaExpression(s));
    }

    @Test
    void aRelativeMomentOffsetsTheTokenAgainstTheRunsClock() {
        ScheduleIntent s = schedule(List.of(cond("updatedAt", "lt", "CURRENT_TIMESTAMP-PT30M"), cond("sentOn", "lt", "CURRENT_DATE-P7D")));
        assertEquals(
                "Criteria.create().lt(\"UpdatedAt\", java.time.LocalDateTime.now().minus(java.time.Duration.parse(\"PT30M\")))"
                        + ".lt(\"SentOn\", java.time.LocalDate.now().minus(java.time.Period.parse(\"P7D\")))",
                ScheduleSupport.criteriaExpression(s));
    }

    @Test
    void theForwardFormIsAdmittedSymmetrically() {
        ScheduleIntent s = schedule(List.of(cond("dueOn", "le", "CURRENT_DATE+P7D")));
        assertEquals("Criteria.create().le(\"DueOn\", java.time.LocalDate.now().plus(java.time.Period.parse(\"P7D\")))",
                ScheduleSupport.criteriaExpression(s));
    }

    @Test
    void aCalendarAmountOnATimestampStaysACalendarAmount() {
        // P1M is a month, not 30 days - a Period on a LocalDateTime keeps that meaning, which a
        // Duration could not express at all.
        ScheduleIntent s = schedule(List.of(cond("changedAt", "lt", "CURRENT_TIMESTAMP-P1M")));
        assertEquals("Criteria.create().lt(\"ChangedAt\", java.time.LocalDateTime.now().minus(java.time.Period.parse(\"P1M\")))",
                ScheduleSupport.criteriaExpression(s));
    }

    @Test
    void momentReadsTheTokenAndItsOffset() {
        assertNull(ScheduleSupport.moment("Provisioning"), "an ordinary literal is not a moment");
        assertNull(ScheduleSupport.moment(7), "a number is not a moment");

        ScheduleSupport.Moment bare = ScheduleSupport.moment("CURRENT_DATE");
        assertEquals(ScheduleSupport.Moment.Shape.DATE, bare.shape());
        assertNull(bare.duration());
        assertTrue(bare.offsetValid(), "a bare token has nothing to be invalid about");

        ScheduleSupport.Moment now = ScheduleSupport.moment("NOW-PT1H");
        assertEquals(ScheduleSupport.Moment.Shape.TIMESTAMP, now.shape());
        assertEquals("PT1H", now.duration());
        assertTrue(now.offsetValid());
    }

    @Test
    void anOffsetTheShapeCannotCarryIsRejected() {
        assertFalse(ScheduleSupport.moment("CURRENT_DATE-PT30M")
                                   .offsetValid(),
                "a date has no time component, so a time offset on it is an authoring error");
        assertFalse(ScheduleSupport.moment("CURRENT_TIMESTAMP-30M")
                                   .offsetValid(),
                "the offset is an ISO-8601 duration, not a bare amount");
        assertFalse(ScheduleSupport.moment("CURRENT_TIMESTAMP-P7D-P1D")
                                   .offsetValid(),
                "exactly one offset on one token - a moment vocabulary, not an expression language");
        assertFalse(ScheduleSupport.moment("CURRENT_TIMESTAMP-P1MT2H")
                                   .offsetValid(),
                "months and hours cannot be one amount - Period takes no time, Duration takes no months");
    }

    @Test
    void anUnparseableOffsetLeavesTheValueAQuotedLiteral() {
        // The parser reports it; the generator must still emit something that compiles rather than a
        // half-built expression.
        ScheduleIntent s = schedule(List.of(cond("dueOn", "lt", "CURRENT_DATE-PT30M")));
        assertEquals("Criteria.create().lt(\"DueOn\", \"CURRENT_DATE-PT30M\")", ScheduleSupport.criteriaExpression(s));
    }

    @Test
    void operatorSupport() {
        assertTrue(ScheduleSupport.isSupportedOperator("between") == false && ScheduleSupport.isSupportedOperator("lt"));
        assertFalse(ScheduleSupport.isSupportedOperator("xx"));
    }
}
