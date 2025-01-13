/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.graal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class Logging {

    private Logging() {}

    private static final Logger SYS_OUT_LOGGER = LoggerFactory.getLogger("app.out");
    private static final Logger SYS_ERR_LOGGER = LoggerFactory.getLogger("app.err");

    static {
        SYS_ERR_LOGGER.atLevel(Level.ERROR);
    }

    public static OutputStream outputStream() {
        return new PrintStream(new GraalJSLogging(SYS_OUT_LOGGER, false), true);
    }

    public static OutputStream errorStream() {
        return new PrintStream(new GraalJSLogging(SYS_ERR_LOGGER, true), true);
    }

    /**
     * The Class GraalJSLogging.
     */
    private static class GraalJSLogging extends OutputStream {

        /**
         * The Constant LINE_SEPERATOR.
         */
        protected static final String LINE_SEPERATOR = System.getProperty("line.separator");

        /**
         * The log.
         */
        protected Logger logger;

        /**
         * The is error.
         */
        protected boolean isError;

        /**
         * Used to maintain the contract of {@link #close()}.
         */
        protected boolean hasBeenClosed = false;

        /**
         * The internal buffer where data is stored.
         */
        protected byte[] buf;

        /**
         * The number of valid bytes in the buffer. This value is always in the range <code>0</code> through
         * <code>buf.length</code>; elements <code>buf[0]</code> through <code>buf[count-1]</code> contain
         * valid byte data.
         */
        protected int count;

        /**
         * Remembers the size of the buffer for speed.
         */
        private int bufLength;

        /**
         * The default number of bytes in the buffer. =2048
         */
        public static final int DEFAULT_BUFFER_LENGTH = 2048;

        /**
         * Creates the GraalJS to flush to the given category.
         *
         * @param logger the Logger to write to
         * @param isError the if true write to error, else info
         * @throws IllegalArgumentException in case of error
         */
        public GraalJSLogging(Logger logger, boolean isError) throws IllegalArgumentException {
            if (logger == null) {
                throw new IllegalArgumentException("log == null");
            }

            this.isError = isError;
            this.logger = logger;
            bufLength = DEFAULT_BUFFER_LENGTH;
            buf = new byte[DEFAULT_BUFFER_LENGTH];
            count = 0;
        }

        /**
         * Closes this output stream and releases any system resources associated with this stream. The
         * general contract of <code>close</code> is that it closes the output stream. A closed stream
         * cannot perform output operations and cannot be reopened.
         */
        @Override
        public void close() {
            flush();
            hasBeenClosed = true;
        }

        /**
         * Writes the specified byte to this output stream. The general contract for <code>write</code> is
         * that one byte is written to the output stream. The byte to be written is the eight low-order bits
         * of the argument <code>b</code>. The 24 high-order bits of <code>b</code> are ignored.
         *
         * @param b the <code>byte</code> to write
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @Override
        public void write(final int b) throws IOException {
            if (hasBeenClosed) {
                throw new IOException("The stream has been closed.");
            }

            if (b == 0) {
                return;
            }

            if (count == bufLength) {
                final int newBufLength = bufLength + DEFAULT_BUFFER_LENGTH;
                final byte[] newBuf = new byte[newBufLength];

                System.arraycopy(buf, 0, newBuf, 0, bufLength);

                buf = newBuf;
                bufLength = newBufLength;
            }

            buf[count] = (byte) b;
            count++;
        }

        /**
         * Flushes this output stream and forces any buffered output bytes to be written out. The general
         * contract of <code>flush</code> is that calling it is an indication that, if any bytes previously
         * written have been buffered by the implementation of the output stream, such bytes should
         * immediately be written to their intended destination.
         */
        @Override
        public void flush() {

            if (count == 0) {
                return;
            }

            if (count == LINE_SEPERATOR.length()) {
                if (((char) buf[0]) == LINE_SEPERATOR.charAt(0)
                        && ((count == 1) || ((count == 2) && ((char) buf[1]) == LINE_SEPERATOR.charAt(1)))) {
                    reset();
                    return;
                }
            }

            final byte[] theBytes = new byte[count];

            System.arraycopy(buf, 0, theBytes, 0, count);

            if (isError) {
                if (logger.isErrorEnabled()) {
                    logger.error(new String(theBytes));
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info(new String(theBytes));
                }
            }

            reset();
        }

        /**
         * Reset.
         */
        private void reset() {
            count = 0;
        }
    }
}
