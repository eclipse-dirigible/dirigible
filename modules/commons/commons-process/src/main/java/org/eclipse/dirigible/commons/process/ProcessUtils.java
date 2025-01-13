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

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The Class ProcessUtils.
 */
public class ProcessUtils {

    /** The default wait time. */
    public static int DEFAULT_WAIT_TIME = 1000;

    /** The default loop count. */
    public static int DEFAULT_LOOP_COUNT = 600;

    /**
     * Creates the process.
     *
     * @param args the args
     * @return the process builder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static ProcessBuilder createProcess(String[] args) throws IOException {
        return new ProcessBuilder(args);
    }

    /**
     * Adds the environment variables.
     *
     * @param processBuilder the process builder
     * @param forAdding the for adding
     */
    public static void addEnvironmentVariables(ProcessBuilder processBuilder, Map<String, String> forAdding) {
        if ((processBuilder != null) && (forAdding != null)) {
            Map<String, String> env = processBuilder.environment();
            env.putAll(forAdding);
        }
    }

    /**
     * Removes the environment variables.
     *
     * @param processBuilder the process builder
     * @param forRemoving the for removing
     */
    public static void removeEnvironmentVariables(ProcessBuilder processBuilder, List<String> forRemoving) {
        if ((processBuilder != null) && (forRemoving != null)) {
            Map<String, String> env = processBuilder.environment();
            for (String remove : forRemoving) {
                env.remove(remove);
            }
        }
    }

    /**
     * Start process.
     *
     * @param args the args
     * @param processBuilder the process builder
     * @return the process
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Process startProcess(String[] args, ProcessBuilder processBuilder) throws IOException {
        return processBuilder.start();
    }

    /**
     * Translate commandline.
     *
     * @param toProcess the to process
     * @return the string[]
     */
    public static String[] translateCommandline(final String toProcess) {
        return Commandline.translateCommandline(toProcess);
    }

}
