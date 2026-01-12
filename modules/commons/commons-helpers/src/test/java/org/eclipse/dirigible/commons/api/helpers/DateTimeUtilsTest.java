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

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

    @Test
    void testOptionallyParseDateTime() {
        Optional<Timestamp> timestamp = DateTimeUtils.optionallyParseDateTime("2025/10/20 23:11:22.000000");
        assertThat(timestamp).isNotEmpty();

        Optional<Timestamp> timestamp2 = DateTimeUtils.optionallyParseDateTime("2025-10-20T23:11:22.000Z");
        assertThat(timestamp2).isNotEmpty();

        Optional<Timestamp> timestamp3 = DateTimeUtils.optionallyParseDateTime("invalid timestamp");
        assertThat(timestamp3).isEmpty();
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

        Optional<Time> time2 = DateTimeUtils.optionallyParseTime("2025-07-02T23:11:22.000Z");
        assertThat(time2).isNotEmpty();

        Optional<Time> time3 = DateTimeUtils.optionallyParseTime("invalid time");
        assertThat(time3).isEmpty();
    }

    @Test
    void testParseDate() {
        testParseDate("1/14/2025");
        testParseDate("14.01.2025");
        testParseDate("20250114");
        testParseDate("2025-01-14T00:00:00.000Z");
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
