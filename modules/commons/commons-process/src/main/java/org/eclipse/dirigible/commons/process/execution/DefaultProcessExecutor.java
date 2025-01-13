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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.eclipse.dirigible.commons.process.execution.output.OutputsPair;
import org.eclipse.dirigible.commons.process.execution.output.ProcessResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * The Class DefaultProcessExecutor.
 */
public final class DefaultProcessExecutor extends ProcessExecutor<OutputsPair> {

    /**
     * Execute process.
     *
     * @param commandLine the command line
     * @param executor the executor
     * @param environmentVariables the environment variables
     * @return the future
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Future<ProcessResult<OutputsPair>> executeProcess(CommandLine commandLine, DefaultExecutor executor,
            Map<String, String> environmentVariables) throws IOException {
        ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(stdOut, stdErr);
        executor.setStreamHandler(pumpStreamHandler);

        ProcessExecutionFuture processExecutionFuture = execute(executor, commandLine, environmentVariables);

        return processExecutionFuture.thenApply(exitCode -> {
            String output = stdOut.toString(StandardCharsets.UTF_8);
            String errorOutput = stdErr.toString(StandardCharsets.UTF_8);
            OutputsPair outputsPair = new OutputsPair(output, errorOutput);
            return new ProcessResult<>(exitCode, outputsPair);
        });
    }
}
