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
    void operatorSupport() {
        assertTrue(ScheduleSupport.isSupportedOperator("between") == false && ScheduleSupport.isSupportedOperator("lt"));
        assertFalse(ScheduleSupport.isSupportedOperator("xx"));
    }
}
