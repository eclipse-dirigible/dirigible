package org.eclipse.dirigible.commons.api.helpers;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilsTest {

    @Test
    void testParseDate() {
        Date date = DateTimeUtils.parseDate("1/14/2025");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        assertThat(date).isNotNull();
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2025);
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY); // 0-based!
        assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(14);
    }
}
