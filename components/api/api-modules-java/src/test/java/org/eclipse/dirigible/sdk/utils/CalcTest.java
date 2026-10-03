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
import java.util.HashMap;
import java.util.Map;
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

    /**
     * Money is evaluated in decimal, not double (#7578): a {@code decimal(18, 2)} column holds 18
     * significant digits, a double about 15, so the largest value the column holds came back as
     * {@code 1.0E16} - longer than the column, a 500 on the insert - and below the ceiling the low
     * cents were silently wrong.
     */
    @Test
    void moneyAtTheColumnsCeilingKeepsEveryDigit() {
        Map<String, Object> payment = new HashMap<>();
        payment.put("Amount", new BigDecimal("9999999999999999.99"));
        payment.put("Allocated", BigDecimal.ZERO);
        assertEquals(new BigDecimal("9999999999999999.99"), Calc.eval("Amount - Allocated", payment, 2),
                "the largest decimal(18, 2) value minus 0 is itself, not 1.0E16");

        payment.put("Amount", new BigDecimal("1234567890123456.78"));
        payment.put("Allocated", new BigDecimal("0.01"));
        assertEquals(new BigDecimal("1234567890123456.77"), Calc.eval("Amount - Allocated", payment, 2), "the low cents are exact");

        Map<String, Object> line = new HashMap<>();
        line.put("Quantity", new BigDecimal("3"));
        line.put("Price", new BigDecimal("3333333333333333.33"));
        assertEquals(new BigDecimal("9999999999999999.99"), Calc.eval("Quantity * Price", line, 2), "a product is exact too");
    }

    /**
     * Decimal semantics end to end: a tie rounds half-up on the decimal value, not its binary
     * approximation.
     */
    @Test
    void roundingAndDivisionAreDecimal() {
        Map<String, Object> values = new HashMap<>();
        values.put("Amount", new BigDecimal("1.005"));
        assertEquals(new BigDecimal("1.01"), Calc.eval("round(Amount, 2)", values, 2), "1.005 is a tie in decimal; in double it was 1.00");
        assertEquals(new BigDecimal("1.01"), Calc.eval("Amount", values, 2), "the final rounding to the column's scale too");

        values.put("Amount", new BigDecimal("100"));
        assertEquals(new BigDecimal("33.33"), Calc.eval("Amount / 3", values, 2));
        assertEquals(new BigDecimal("100.00"), Calc.eval("Amount / 3 * 3", values, 2), "a quotient carries enough digits to come back");
        assertEquals(new BigDecimal("0.30"), Calc.eval("0.1 + 0.2", values, 2));
        assertEquals(new BigDecimal("-2"), Calc.eval("floor(-1.5)", values, 0));
        assertEquals(new BigDecimal("-1"), Calc.eval("ceil(-1.5)", values, 0));
        assertEquals(new BigDecimal("-2"), Calc.eval("round(-1.5)", values, 0), "half-up rounds a tie away from zero");
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

    @Test
    void aMapEntityReadsIdentifiersFromItsEntries() {
        // The generated mapping code evaluates a formula over a record that has no compiled type.
        Map<String, Object> record = new HashMap<>();
        record.put("Hours", new BigDecimal("8"));
        record.put("Rate", new BigDecimal("12.5"));
        assertEquals(new BigDecimal("100.00"), Calc.eval("Hours * Rate", record, 2));
    }

    @Test
    void aMissingMapKeyReadsAsZeroLikeAMissingField() {
        assertEquals(new BigDecimal("0.00"), Calc.eval("Hours * Rate", new HashMap<String, Object>(), 2));
    }

    @Test
    void aMapEntitySupportsTheDateFunctionsToo() {
        Map<String, Object> record = new HashMap<>();
        record.put("FromDate", LocalDate.of(2026, 7, 20)); // Monday
        record.put("ToDate", LocalDate.of(2026, 7, 24)); // Friday
        assertEquals(new BigDecimal("5"), Calc.eval("businessDaysBetween(FromDate, ToDate)", record, 0));
    }
}
