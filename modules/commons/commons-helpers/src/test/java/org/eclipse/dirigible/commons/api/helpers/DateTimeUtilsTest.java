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
