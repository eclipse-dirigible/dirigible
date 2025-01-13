/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.api;

import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.EdmType;
import org.apache.olingo.odata2.core.edm.EdmBinary;
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
import org.junit.Assert;
import org.junit.Test;

/**
 * The Class SQLStatementParamTest.
 */
public class SQLStatementParamTest {

    /**
     * Test edm type int 32 to simple kind.
     */
    @Test
    public void testEdmTypeInt32ToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmInt32(), EdmSimpleTypeKind.Int32);
    }

    /**
     * Test edm type int 16 to simple kind.
     */
    @Test
    public void testEdmTypeInt16ToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmInt16(), EdmSimpleTypeKind.Int16);
    }

    /**
     * Test edm type int 64 to simple kind.
     */
    @Test
    public void testEdmTypeInt64ToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmInt64(), EdmSimpleTypeKind.Int64);
    }

    /**
     * Test edm type string to simple kind.
     */
    @Test
    public void testEdmTypeStringToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmString(), EdmSimpleTypeKind.String);
    }

    /**
     * Test edm type double to simple kind.
     */
    @Test
    public void testEdmTypeDoubleToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmDouble(), EdmSimpleTypeKind.Double);
    }

    /**
     * Test edm type decimal to simple kind.
     */
    @Test
    public void testEdmTypeDecimalToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmDecimal(), EdmSimpleTypeKind.Decimal);
    }

    /**
     * Test edm type guid to simple kind.
     */
    @Test
    public void testEdmTypeGuidToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmGuid(), EdmSimpleTypeKind.Guid);
    }

    /**
     * Test edm type single to simple kind.
     */
    @Test
    public void testEdmTypeSingleToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmSingle(), EdmSimpleTypeKind.Single);
    }

    /**
     * Test edm type S byte to simple kind.
     */
    @Test
    public void testEdmTypeSByteToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmSByte(), EdmSimpleTypeKind.SByte);
    }

    /**
     * Test edm type byte to simple kind.
     */
    @Test
    public void testEdmTypeByteToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmByte(), EdmSimpleTypeKind.Byte);
    }

    /**
     * Test edm type date time to simple kind.
     */
    @Test
    public void testEdmTypeDateTimeToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmDateTime(), EdmSimpleTypeKind.DateTime);
    }

    /**
     * Test edm type date time offset to simple kind.
     */
    @Test
    public void testEdmTypeDateTimeOffsetToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmDateTimeOffset(), EdmSimpleTypeKind.DateTimeOffset);
    }

    /**
     * Test edm type time to simple kind.
     */
    @Test
    public void testEdmTypeTimeToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmTime(), EdmSimpleTypeKind.Time);
    }

    /**
     * Test edm type null to simple kind.
     */
    @Test
    public void testEdmTypeNullToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmNull(), EdmSimpleTypeKind.Null);
    }

    /**
     * Test edm type binary to simple kind.
     */
    @Test
    public void testEdmTypeBinaryToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmBinary(), EdmSimpleTypeKind.Binary);
    }

    /**
     * Test edm type boolean to simple kind.
     */
    @Test
    public void testEdmTypeBooleanToSimpleKind() {
        testEdmTypeToSimpleKind(new EdmBoolean(), EdmSimpleTypeKind.Boolean);
    }

    /**
     * Test edm type to simple kind.
     *
     * @param edmType the edm type
     * @param expectedKind the expected kind
     */
    private void testEdmTypeToSimpleKind(EdmType edmType, EdmSimpleTypeKind expectedKind) {
        SQLStatementParam sqlStatementParam = new SQLStatementParam(null, edmType, null);
        Assert.assertEquals("Unexpected EdmSimpleKind.", expectedKind, sqlStatementParam.getEdmSimpleKind());
    }

}
