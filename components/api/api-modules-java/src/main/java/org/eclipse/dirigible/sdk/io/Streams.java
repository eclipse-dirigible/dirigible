/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.dirigible.components.api.io.StreamsFacade;

/**
 * Stream-shaped I/O — read text / bytes from an {@link InputStream}, write to an
 * {@link OutputStream}, copy between them, build in-memory buffers via {@link ByteArrayInputStream}
 * / {@link ByteArrayOutputStream}. Useful when wiring together sources and sinks supplied by
 * different APIs (a multipart upload feeding a ZIP entry, the platform repository feeding an HTTP
 * response).
 * <p>
 * {@link #copyLarge(InputStream, OutputStream)} should be preferred over plain
 * {@link #copy(InputStream, OutputStream)} for files larger than a few MB — it uses a bigger
 * internal buffer and reports a {@code long} byte count via the underlying facade.
 */
public final class Streams {

    private Streams() {}

    public static int read(InputStream input) throws IOException {
        return StreamsFacade.read(input);
    }

    public static byte[] readBytes(InputStream input) throws IOException {
        return StreamsFacade.readBytes(input);
    }

    public static String readText(InputStream input) throws IOException {
        return StreamsFacade.readText(input);
    }

    public static void close(InputStream input) throws IOException {
        StreamsFacade.close(input);
    }

    public static void close(OutputStream output) throws IOException {
        StreamsFacade.close(output);
    }

    public static void write(OutputStream output, int value) throws IOException {
        StreamsFacade.write(output, value);
    }

    public static void writeBytes(OutputStream output, String input) throws IOException {
        StreamsFacade.writeBytes(output, input);
    }

    public static void writeText(OutputStream output, String value) throws IOException {
        StreamsFacade.writeText(output, value);
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        StreamsFacade.copy(input, output);
    }

    public static void copyLarge(InputStream input, OutputStream output) throws IOException {
        StreamsFacade.copyLarge(input, output);
    }

    public static ByteArrayInputStream createByteArrayInputStream(byte[] input) throws IOException {
        return StreamsFacade.createByteArrayInputStream(input);
    }

    public static ByteArrayInputStream createByteArrayInputStream(String input) throws IOException {
        return StreamsFacade.createByteArrayInputStream(input);
    }

    public static ByteArrayInputStream createByteArrayInputStream() throws IOException {
        return StreamsFacade.createByteArrayInputStream();
    }

    public static ByteArrayOutputStream createByteArrayOutputStream() throws IOException {
        return StreamsFacade.createByteArrayOutputStream();
    }

    public static byte[] getBytes(ByteArrayOutputStream output) throws IOException {
        return StreamsFacade.getBytes(output);
    }

    public static String getText(ByteArrayOutputStream output) throws IOException {
        return StreamsFacade.getText(output);
    }

    public static ByteArrayInputStream getResourceAsByteArrayInputStream(String path) throws IOException {
        return StreamsFacade.getResourceAsByteArrayInputStream(path);
    }
}
