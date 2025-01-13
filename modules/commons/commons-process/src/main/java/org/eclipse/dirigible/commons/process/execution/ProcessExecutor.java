/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.process.execution;

import org.apache.commons.exec.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.dirigible.commons.process.Commandline;
import org.eclipse.dirigible.commons.process.execution.output.OutputsPair;
import org.eclipse.dirigible.commons.process.execution.output.ProcessResult;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * The Class ProcessExecutor.
 *
 * @param <TOut> the generic type
 */
public abstract class ProcessExecutor<TOut> {

    /**
     * Creates the with errors redirect.
     *
     * @return the process executor
     */
    public static ProcessExecutor<String> createWithErrorsRedirect() {
        return new ErrorsRedirectProcessExecutor();
    }

    /**
     * Creates the.
     *
     * @return the process executor
     */
    public static ProcessExecutor<OutputsPair> create() {
        return new DefaultProcessExecutor();
    }

    /**
     * Execute process.
     *
     * @param path the path
     * @param environmentVariables the environment variables
     * @return the future
     */
    public Future<ProcessResult<TOut>> executeProcess(String path, Map<String, String> environmentVariables,
            ProcessExecutionOptions options) {
        try {
            Pair<String, String[]> executableAndArgs = toExecutableAndArgs(path);
            CommandLine commandLine = new CommandLine(executableAndArgs.getLeft());
            commandLine.addArguments(executableAndArgs.getRight(), false);
            DefaultExecutor executor = new DefaultExecutor();

            String maybeWorkingDirectory = options.getWorkingDirectory();
            if (!StringUtils.isEmpty(maybeWorkingDirectory)) {
                File workingDirectory = new File(maybeWorkingDirectory);
                executor.setWorkingDirectory(workingDirectory);
            }

            return executeProcess(commandLine, executor, environmentVariables);
        } catch (Throwable t) {
            throw new ProcessExecutionException(t);
        }
    }

    /**
     * To executable and args.
     *
     * @param path the path
     * @return the pair
     */
    private static Pair<String, String[]> toExecutableAndArgs(String path) {
        String[] parts = Commandline.translateCommandline(path);
        String executable = parts[0];
        String[] arguments = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[] {};
        return Pair.of(executable, arguments);
    }

    /**
     * Execute process.
     *
     * @param commandLine the command line
     * @param executor the executor
     * @param environmentVariables the environment variables
     * @return the future
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public abstract Future<ProcessResult<TOut>> executeProcess(CommandLine commandLine, DefaultExecutor executor,
            Map<String, String> environmentVariables) throws IOException;

    /**
     * Execute.
     *
     * @param executor the executor
     * @param commandLine the command line
     * @param environmentVariables the environment variables
     * @return the process execution future
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected ProcessExecutionFuture execute(DefaultExecutor executor, CommandLine commandLine, Map<String, String> environmentVariables)
            throws IOException {
        ProcessExecutionFuture processExecutionFuture = new ProcessExecutionFuture();
        executor.execute(commandLine, environmentVariables, processExecutionFuture);
        return processExecutionFuture;
    }
}
