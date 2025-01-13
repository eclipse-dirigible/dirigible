/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.builder;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import org.apache.olingo.odata2.api.edm.EdmType;
import org.apache.olingo.odata2.core.edm.EdmBoolean;
import org.apache.olingo.odata2.core.edm.EdmByte;
import org.apache.olingo.odata2.core.edm.EdmDateTime;
import org.apache.olingo.odata2.core.edm.EdmDateTimeOffset;
import org.apache.olingo.odata2.core.edm.EdmDecimal;
import org.apache.olingo.odata2.core.edm.EdmDouble;
import org.apache.olingo.odata2.core.edm.EdmGuid;
import org.apache.olingo.odata2.core.edm.EdmInt16;
import org.apache.olingo.odata2.core.edm.EdmInt32;
import org.apache.olingo.odata2.core.edm.EdmInt64;
import org.apache.olingo.odata2.core.edm.EdmNull;
import org.apache.olingo.odata2.core.edm.EdmSByte;
import org.apache.olingo.odata2.core.edm.EdmSingle;
import org.apache.olingo.odata2.core.edm.EdmString;
import org.apache.olingo.odata2.core.edm.EdmTime;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * The Class SQLUtilsTest.
 */
@RunWith(MockitoJUnitRunner.class)
public class SQLUtilsTest {

    /** The stmt. */
    @Mock
    private PreparedStatement stmt;

    /**
     * Test set string parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetStringParameter() throws SQLException {
        String value = "TestValue";
        executeSetParameter(value, new EdmString());
        Mockito.verify(stmt)
               .setString(1, value);
    }

    /**
     * Test set byte parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetByteParameter() throws SQLException {
        short value = 13;
        executeSetParameter(value, new EdmByte());
        Mockito.verify(stmt)
               .setShort(1, value);
    }

    /**
     * Test set int 16 parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetInt16Parameter() throws SQLException {
        short value = 16;
        executeSetParameter(value, new EdmInt16());
        Mockito.verify(stmt)
               .setShort(1, value);
    }

    /**
     * Test set int 32 parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetInt32Parameter() throws SQLException {
        int value = 36589;
        executeSetParameter(value, new EdmInt32());
        Mockito.verify(stmt)
               .setInt(1, value);
    }

    /**
     * Test set int 64 parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetInt64Parameter() throws SQLException {
        long value = 4684684L;
        executeSetParameter(value, new EdmInt64());
        Mockito.verify(stmt)
               .setLong(1, value);
    }

    /**
     * Test set double parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetDoubleParameter() throws SQLException {
        double value = 458.32;
        executeSetParameter(value, new EdmDouble());
        Mockito.verify(stmt)
               .setDouble(1, value);
    }

    /**
     * Test set boolean parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetBooleanParameter() throws SQLException {
        boolean value = false;
        executeSetParameter(value, new EdmBoolean());
        Mockito.verify(stmt)
               .setBoolean(1, value);
    }

    /**
     * Test set decimal parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetDecimalParameter() throws SQLException {
        BigDecimal value = BigDecimal.valueOf(1569863.3);
        executeSetParameter(value, new EdmDecimal());
        Mockito.verify(stmt)
               .setBigDecimal(1, value);
    }

    /**
     * Test set null parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetNullParameter() throws SQLException {
        Object value = null;
        executeSetParameter(value, new EdmNull());
        Mockito.verify(stmt)
               .setObject(1, value);
    }

    /**
     * Test set guid parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetGuidParameter() throws SQLException {
        Object value = "2312312301231-123123j";
        executeSetParameter(value, new EdmGuid());
        Mockito.verify(stmt)
               .setObject(1, value);
    }

    /**
     * Test set S byte parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetSByteParameter() throws SQLException {
        byte value = 12;
        executeSetParameter(value, new EdmSByte());
        Mockito.verify(stmt)
               .setByte(1, value);
    }

    /**
     * Test set single parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetSingleParameter() throws SQLException {
        float value = 11;
        executeSetParameter(value, new EdmSingle());
        Mockito.verify(stmt)
               .setFloat(1, value);
    }

    /**
     * Test set time parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetTimeParameter() throws SQLException {
        Calendar value = new GregorianCalendar(2022, Calendar.FEBRUARY, 24);
        executeSetParameter(value, new EdmTime());
        Time expectedTime = new Time(value.getTime()
                                          .getTime());
        Mockito.verify(stmt)
               .setTime(1, expectedTime);
    }

    /**
     * Test set date time parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetDateTimeParameter() throws SQLException {
        Calendar value = new GregorianCalendar(2022, Calendar.MARCH, 3);
        executeSetParameter(value, new EdmDateTime());
        Timestamp expectedTimestamp = new Timestamp(value.getTime()
                                                         .getTime());
        Mockito.verify(stmt)
               .setTimestamp(1, expectedTimestamp);
    }

    /**
     * Test set date time offset parameter.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSetDateTimeOffsetParameter() throws SQLException {
        Calendar value = new GregorianCalendar(2022, Calendar.MARCH, 3);
        executeSetParameter(value, new EdmDateTimeOffset());
        Date expectedDate = new Date(value.getTime()
                                          .getTime());
        Mockito.verify(stmt)
               .setDate(1, expectedDate);
    }

    /**
     * Execute set parameter.
     *
     * @param value the value
     * @param edmType the edm type
     * @throws SQLException the SQL exception
     */
    private void executeSetParameter(Object value, EdmType edmType) throws SQLException {
        SQLStatementParam param = new SQLStatementParam(value, edmType, null);
        List<SQLStatementParam> params = new ArrayList<>();
        params.add(param);
        SQLUtils.setParamsOnStatement(stmt, params);
    }

}
