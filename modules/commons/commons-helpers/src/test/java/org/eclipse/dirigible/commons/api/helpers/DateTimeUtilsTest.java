/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.api.helpers;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilsTest {

    @Test
    void testOptionallyParseDateTime() {
        Optional<Timestamp> timestamp = DateTimeUtils.optionallyParseDateTime("2025/10/20 23:11:22.000000");
        assertThat(timestamp).isNotEmpty();

        Optional<Timestamp> timestamp2 = DateTimeUtils.optionallyParseDateTime("invalid timestamp");
        assertThat(timestamp2).isEmpty();
    }

    @Test
    void testOptionallyParseDate() {
        Optional<Date> date = DateTimeUtils.optionallyParseDate("20250114");
        assertThat(date).isNotEmpty();

        Optional<Date> date2 = DateTimeUtils.optionallyParseDate("invalid date");
        assertThat(date2).isEmpty();

        Optional<Date> date3 = DateTimeUtils.optionallyParseDate("2025-07-02T00:00:00.000Z");
        assertThat(date3).isNotEmpty();
    }

    @Test
    void testOptionallyParseTime() {
        Optional<Time> time = DateTimeUtils.optionallyParseTime("23:11:22.000000");
        assertThat(time).isNotEmpty();

        Optional<Time> time2 = DateTimeUtils.optionallyParseTime("invalid time");
        assertThat(time2).isEmpty();
    }

    @Test
    void testParseDate() {
        testParseDate("1/14/2025");
        testParseDate("14.01.2025");
        testParseDate("20250114");
    }

    private void testParseDate(String dateString) {
        Date date = DateTimeUtils.parseDate(dateString);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        assertThat(date).isNotNull();
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2025);
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY);
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(14);
    }
}
