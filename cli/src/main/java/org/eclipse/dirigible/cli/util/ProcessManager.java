/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

@Component
public class ProcessManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessManager.class);

    public int startSynchronously(String... commandArgs) throws ProcessException {
        return startSynchronously(Optional.empty(), commandArgs);
    }

    public int startSynchronously(Optional<Path> processPath, String... commandArgs) {
        ProcessBuilder builder = new ProcessBuilder(commandArgs);

        if (processPath.isPresent()) {
            builder.directory(processPath.get()
                                         .toFile());
        }

        builder.inheritIO();

        try {
            Process process = builder.start();
            return process.waitFor();
        } catch (IOException ex) {
            String errorMessage = "Failure in process: " + Arrays.toString(commandArgs);
            throw new ProcessException(errorMessage, ex);
        } catch (InterruptedException ex) {
            LOGGER.debug("Process interrupted (terminated)", ex);
            return 0;
        }
    }

    public int startSynchronously(Path processPath, String... commandArgs) {
        return startSynchronously(Optional.of(processPath), commandArgs);
    }
}
