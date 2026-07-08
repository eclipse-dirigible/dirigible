/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Coverage for the calculated-field evaluator, focused on the date functions: date-typed
 * identifiers read as epoch days; daysBetween is the exclusive difference, businessDaysBetween the
 * inclusive Mon-Fri count, monthsBetween the whole-month difference. The numeric semantics
 * (null-as-0, division-by-zero-as-0, half-up rounding) are asserted alongside since the JS mirror
 * must agree.
 */
class CalcTest {

    /** A calculation subject with the public-field shape the generated entities use. */
    public static class Subject {

        public BigDecimal Hours;
        public BigDecimal Rate;
        public LocalDate FromDate;
        public LocalDate ToDate;
        public String StartText;
    }

    private static Subject subject() {
        Subject s = new Subject();
        s.Hours = new BigDecimal("8");
        s.Rate = new BigDecimal("12.5");
        // 2026-07-20 is a Monday, 2026-07-24 a Friday
        s.FromDate = LocalDate.of(2026, 7, 20);
        s.ToDate = LocalDate.of(2026, 7, 24);
        s.StartText = "2026-07-20";
        return s;
    }

    @Test
    void numericArithmeticAndNullSemantics() {
        assertEquals(new BigDecimal("100.00"), Calc.eval("Hours * Rate", subject(), 2));
        assertEquals(new BigDecimal("0.00"), Calc.eval("Missing * 5", subject(), 2), "missing identifier reads as 0");
        assertEquals(new BigDecimal("0.00"), Calc.eval("Hours / 0", subject(), 2), "division by zero yields 0");
    }

    @Test
    void daysBetweenIsTheExclusiveDifference() {
        assertEquals(new BigDecimal("4"), Calc.eval("daysBetween(FromDate, ToDate)", subject(), 0));
        assertEquals(new BigDecimal("5"), Calc.eval("daysBetween(FromDate, ToDate) + 1", subject(), 0), "inclusive span");
    }

    @Test
    void businessDaysBetweenCountsMonToFriInclusive() {
        Subject s = subject();
        assertEquals(new BigDecimal("5"), Calc.eval("businessDaysBetween(FromDate, ToDate)", s, 0), "Mon..Fri = 5 working days");
        s.ToDate = LocalDate.of(2026, 7, 26); // through Sunday - still 5
        assertEquals(new BigDecimal("5"), Calc.eval("businessDaysBetween(FromDate, ToDate)", s, 0));
        s.ToDate = LocalDate.of(2026, 7, 28); // through the next Tuesday - 7
        assertEquals(new BigDecimal("7"), Calc.eval("businessDaysBetween(FromDate, ToDate)", s, 0));
        s.ToDate = LocalDate.of(2026, 7, 19); // end before start - 0
        assertEquals(new BigDecimal("0"), Calc.eval("businessDaysBetween(FromDate, ToDate)", s, 0));
        s.ToDate = s.FromDate; // a single Monday - 1
        assertEquals(new BigDecimal("1"), Calc.eval("businessDaysBetween(FromDate, ToDate)", s, 0));
    }

    @Test
    void monthsBetweenIsTheWholeMonthDifference() {
        Subject s = subject();
        s.FromDate = LocalDate.of(2026, 1, 15);
        s.ToDate = LocalDate.of(2027, 1, 15);
        assertEquals(new BigDecimal("12"), Calc.eval("monthsBetween(FromDate, ToDate)", s, 0));
    }

    @Test
    void isoStringDatesReadAsEpochDaysToo() {
        // The HTML date input binds a plain yyyy-MM-dd string; it must behave like the LocalDate field.
        assertEquals(new BigDecimal("5"), Calc.eval("businessDaysBetween(StartText, ToDate)", subject(), 0));
    }
}
