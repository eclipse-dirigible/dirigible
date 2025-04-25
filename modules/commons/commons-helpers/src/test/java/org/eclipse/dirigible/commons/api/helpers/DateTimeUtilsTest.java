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
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilsTest {

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
