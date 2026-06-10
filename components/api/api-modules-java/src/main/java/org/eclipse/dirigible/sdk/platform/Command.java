/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.platform;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.eclipse.dirigible.commons.process.execution.ProcessExecutionOptions;
import org.eclipse.dirigible.components.api.platform.CommandFacade;

/**
 * Spawns OS-level processes from the JVM, captures their merged stdout/stderr, and returns it as a
 * String. The first overload runs with the inherited environment; the second and third let you add
 * or remove specific variables; the third also accepts a {@link ProcessExecutionOptions} record for
 * advanced settings (working directory, timeout, capture-error-stream toggle).
 * <p>
 * Use with caution — the command string is passed to a shell on POSIX and to {@code cmd /c} on
 * Windows, so any user-supplied input must be sanitized or the call should use an
 * {@code ProcessExecutionOptions} variant that takes an argv array (see the commons-process package
 * for the full API).
 */
public final class Command {

    private Command() {}

    public static String exec(String command) throws ExecutionException, InterruptedException {
        return CommandFacade.execute(command, Collections.emptyMap(), Collections.emptyList());
    }

    public static String exec(String command, Map<String, String> envAdditions, List<String> envRemovals)
            throws ExecutionException, InterruptedException {
        return CommandFacade.execute(command, envAdditions, envRemovals);
    }

    public static String exec(String command, Map<String, String> envAdditions, List<String> envRemovals, ProcessExecutionOptions options)
            throws ExecutionException, InterruptedException {
        return CommandFacade.execute(command, envAdditions, envRemovals, options);
    }
}
