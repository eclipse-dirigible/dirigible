/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Optional;

import javax.sql.rowset.serial.SerialBlob;

import org.junit.jupiter.api.Test;

/**
 * #8143 - Database.query returned every BLOB one byte short: the writer passed {@code length - 1}
 * as Jackson's length argument, so the last byte was silently dropped (a 1-byte BLOB came back
 * empty).
 */
class ResultSetJsonWriterTest {

    @Test
    void blobIsWrittenWithEveryByte() throws Exception {
        byte[] stored = {10, 20, 30, 40, 50};

        String json = write("BL", new SerialBlob(stored));

        assertEquals("[{\"BL\":[10,20,30,40,50]}]", json);
    }

    @Test
    void oneByteBlobIsNotWrittenAsEmptyArray() throws Exception {
        String json = write("BL", new SerialBlob(new byte[] {7}));

        assertEquals("[{\"BL\":[7]}]", json);
    }

    @Test
    void emptyBlobIsWrittenAsEmptyArray() throws Exception {
        String json = write("BL", new SerialBlob(new byte[0]));

        assertEquals("[{\"BL\":[]}]", json);
    }

    /**
     * A VARBINARY / BINARY column comes back from the driver as byte[]; it used to be rendered through
     * Object.toString() as "[B@1a2b3c".
     */
    @Test
    void byteArrayIsWrittenAsArrayOfBytes() throws Exception {
        String json = write("VB", new byte[] {1, 2, 3, -1});

        assertEquals("[{\"VB\":[1,2,3,-1]}]", json);
    }

    @Test
    void stringifiedBlobIsRenderedAsBinaryMarker() throws Exception {
        ResultSetJsonWriter writer = new ResultSetJsonWriter();
        writer.setStringified(true);

        String json = write(writer, "BL", new SerialBlob(new byte[] {1, 2, 3}));

        assertEquals("[{\"BL\":\"[BINARY]\"}]", json);
    }

    private static String write(String column, Object value) throws Exception {
        ResultSetJsonWriter writer = new ResultSetJsonWriter();
        writer.setStringified(false);
        return write(writer, column, value);
    }

    private static String write(ResultSetJsonWriter writer, String column, Object value) throws Exception {
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnName(1)).thenReturn(column);
        when(metaData.getColumnLabel(1)).thenReturn(column);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.getObject(column)).thenReturn(value);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(resultSet, output, Optional.empty());
        return output.toString(StandardCharsets.UTF_8);
    }
}
