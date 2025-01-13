/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.process;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.input.BoundedInputStream;

/**
 * The Class Piper.
 */
public class Piper implements java.lang.Runnable {

    /** The Constant BROKEN_PIPE. */
    private static final String BROKEN_PIPE = "Broken pipe";

    /** The Constant MAX_COMMAND_OUTPUT_LENGTH. */
    private static final long MAX_COMMAND_OUTPUT_LENGTH = 2097152;

    /** The input. */
    private InputStream input;

    /** The output. */
    private OutputStream output;

    /**
     * Instantiates a new piper.
     *
     * @param input the input
     * @param output the output
     */
    public Piper(InputStream input, OutputStream output) {
        this.input = new BoundedInputStream(input, MAX_COMMAND_OUTPUT_LENGTH);
        this.output = output;
    }

    /**
     * Run.
     */
    @Override
    public void run() {
        try {
            byte[] b = new byte[512];
            int read = 1;
            int sum = 0;
            while (read > -1) {
                read = input.read(b, 0, b.length);
                sum += read;
                if (read > -1) {
                    // Write bytes to output
                    output.write(b, 0, read);
                }
            }
            if (sum >= (MAX_COMMAND_OUTPUT_LENGTH - 1)) {
                output.write("\n...".getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            // Something happened while reading or writing streams; pipe is broken
            throw new RuntimeException(BROKEN_PIPE, e);
        } finally {
            try {
                input.close();
            } catch (Exception e) {
                // Do nothing
            }
            try {
                output.close();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

}
