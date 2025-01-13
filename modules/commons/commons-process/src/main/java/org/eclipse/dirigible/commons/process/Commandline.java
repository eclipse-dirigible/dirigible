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

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * The Class Commandline.
 */
public class Commandline {

    /**
     * =====================================================================================.
     *
     * @param toProcess the to process
     * @return the string[]
     */
    /** The following function has been taken from Apache Ant code base */
    /** Class: org/apache/tools/ant/types/Commandline.java */
    /** ===================================================================================== */

    /**
     * Crack a command line.
     *
     * @param toProcess the command line to process
     * @return the command line broken into strings. An empty or null toProcess parameter results in a
     *         zero sized array
     */
    public static String[] translateCommandline(final String toProcess) {
        if ((toProcess == null) || (toProcess.length() == 0)) {
            // no command? no string
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        final ArrayList<String> list = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            final String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("\'".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                default:
                    if ("\'".equals(nextTok)) {
                        state = inQuote;
                    } else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    } else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || (current.length() != 0)) {
                            list.add(current.toString());
                            current = new StringBuilder();
                        }
                    } else {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }

        if (lastTokenHasBeenQuoted || (current.length() != 0)) {
            list.add(current.toString());
        }

        if ((state == inQuote) || (state == inDoubleQuote)) {
            throw new IllegalArgumentException("Unbalanced quotes in " + toProcess);
        }

        final String[] args = new String[list.size()];
        return list.toArray(args);
    }

}
