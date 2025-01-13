/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.BytesHelper;
import org.springframework.stereotype.Component;

/**
 * Facade for working with I/O streams.
 */
@Component
public class StreamsFacade {

    /**
     * Read the next byte from the provided {@link InputStream}.
     *
     * @param input the input stream to read from
     * @return the next byte of data in the range 0-255 or -1 if end of stream is reached
     * @throws IOException in case of failure in underlying layer
     */
    public static final int read(InputStream input) throws IOException {
        return input.read();
    }

    /**
     * Read the content of the provided {@link InputStream} as byte array.
     *
     * @param input the input stream to read from
     * @return the input stream content
     * @throws IOException in case of failure in underlying layer
     */
    public static final byte[] readBytes(InputStream input) throws IOException {
        return IOUtils.toByteArray(input);
    }

    /**
     * Read the content of the {@link InputStream} as UTF-8 text.
     *
     * @param input the input stream to read from
     * @return the content of the stream as string
     * @throws IOException in case of failure in underlying layer
     */
    public static final String readText(InputStream input) throws IOException {
        return IOUtils.toString(input, StandardCharsets.UTF_8);
    }

    /**
     * Close the provided {@link InputStream}.
     *
     * @param input the input stream to close
     * @throws IOException in case of failure in underlying layer
     */
    public static final void close(InputStream input) throws IOException {
        input.close();
    }

    /**
     * Write byte to the provided {@link OutputStream}. The 24 higher bits are ignored.
     *
     * @param output the output stream to write to
     * @param value the byte to write
     * @throws IOException in case of failure in underlying layer
     */
    public static final void write(OutputStream output, int value) throws IOException {
        output.write(value);
    }

    /**
     * Write data to the provided {@link OutputStream}.
     *
     * @param output the output stream to write to
     * @param input the data to write
     * @throws IOException in case of failure in underlying layer
     */
    public static final void writeBytes(OutputStream output, String input) throws IOException {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        output.write(bytes);
    }

    /**
     * Write text to the provided {@link OutputStream} using UTF-8 encoding.
     *
     * @param output the output stream to write to
     * @param value the text to write
     * @throws IOException in case of failure in underlying layer
     */
    public static final void writeText(OutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Close the provided {@link OutputStream}.
     *
     * @param output the output stream that is to be closed
     * @throws IOException in case of failure in underlying layer
     */
    public static final void close(OutputStream output) throws IOException {
        output.close();
    }

    /**
     * Write the {@link InputStream} content into the {@link OutputStream}.
     *
     * @param input the input stream to read from
     * @param output the output stream to write to
     * @throws IOException in case of failure in underlying layer
     */
    public static final void copy(InputStream input, OutputStream output) throws IOException {
        IOUtils.copy(input, output);
    }

    /**
     * Write the {@link InputStream} content into the {@link OutputStream}.
     *
     * @param input the input stream to read from
     * @param output the output stream to write to
     * @throws IOException in case of failure in underlying layer
     */
    public static final void copyLarge(InputStream input, OutputStream output) throws IOException {
        IOUtils.copyLarge(input, output);
    }

    /**
     * Get the byte array as {@link ByteArrayInputStream}.
     *
     * @param input the byte array
     * @return the created byte array input stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final ByteArrayInputStream createByteArrayInputStream(byte[] input) throws IOException {
        return new ByteArrayInputStream(input);
    }

    /**
     * Get the input data as {@link ByteArrayInputStream}.
     *
     * @param input the input data
     * @return the created byte array input stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final ByteArrayInputStream createByteArrayInputStream(String input) throws IOException {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        return new ByteArrayInputStream(bytes);
    }

    /**
     * Create empty {@link ByteArrayInputStream}.
     *
     * @return the created byte array input stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final ByteArrayInputStream createByteArrayInputStream() throws IOException {
        return new ByteArrayInputStream(new byte[] {});
    }

    /**
     * Create empty {@link ByteArrayOutputStream}.
     *
     * @return the created byte array output stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final ByteArrayOutputStream createByteArrayOutputStream() throws IOException {
        return new ByteArrayOutputStream();
    }

    /**
     * Get the {@link ByteArrayOutputStream} content as byte array.
     *
     * @param output the byte array output stream
     * @return the content of the byte array output stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final byte[] getBytes(ByteArrayOutputStream output) throws IOException {
        byte[] bytes = output.toByteArray();
        return bytes;
    }

    /**
     * Get the {@link ByteArrayOutputStream} content as UTF-8 string.
     *
     * @param output the byte array output stream
     * @return the content of the byte array output stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final String getText(ByteArrayOutputStream output) throws IOException {
        byte[] bytes = output.toByteArray();
        String text = new String(bytes, StandardCharsets.UTF_8);
        return text;
    }

    /**
     * Gets the resource as byte array input stream.
     *
     * @param path the path
     * @return the resource as byte array input stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final ByteArrayInputStream getResourceAsByteArrayInputStream(String path) throws IOException {
        InputStream in = null;
        try {
            in = StreamsFacade.class.getResourceAsStream("/META-INF/dirigible" + path);
            if (in != null) {
                return new ByteArrayInputStream(IOUtils.toByteArray(in));
            }
            return new ByteArrayInputStream("".getBytes());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
