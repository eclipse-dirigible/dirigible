/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.platform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.process.execution.ProcessExecutionOptions;
import org.eclipse.dirigible.commons.process.execution.ProcessExecutor;
import org.eclipse.dirigible.commons.process.execution.output.OutputsPair;
import org.eclipse.dirigible.commons.process.execution.output.ProcessResult;
import org.springframework.stereotype.Component;

/**
 * The Class CommandFacade.
 */
@Component
public class CommandFacade {

    /**
     * Execute service module.
     *
     * @param command the command line code
     * @param add the add
     * @param remove the remove
     * @return the output of the command
     * @throws ExecutionException the execution exception
     * @throws InterruptedException the interrupted exception
     */
    public static String execute(String command, Map<String, String> add, List<String> remove)
            throws ExecutionException, InterruptedException {
        return execute(command, add, remove, new ProcessExecutionOptions());
    }

    /**
     * Execute service module.
     *
     * @param command the command line code
     * @param add the add
     * @param remove the remove
     * @param processExecutionOptionsJson options for the process execution
     * @return the output of the command
     * @throws ExecutionException the execution exception
     * @throws InterruptedException the interrupted exception
     */
    public static String execute(String command, Map<String, String> add, List<String> remove, String processExecutionOptionsJson)
            throws ExecutionException, InterruptedException {
        ProcessExecutionOptions options = GsonHelper.fromJson(processExecutionOptionsJson, ProcessExecutionOptions.class);
        return execute(command, add, remove, options);
    }

    /**
     * Execute service module.
     *
     * @param command the command line code
     * @param add the add
     * @param remove the remove
     * @param processExecutionOptions options for the process execution
     * @return the output of the command
     * @throws ExecutionException the execution exception
     * @throws InterruptedException the interrupted exception
     */
    public static String execute(String command, Map<String, String> add, List<String> remove,
            ProcessExecutionOptions processExecutionOptions) throws ExecutionException, InterruptedException {
        Map<String, String> environmentVariablesToUse = createEnvironmentVariables(add, remove);
        ProcessExecutor<OutputsPair> processExecutor = ProcessExecutor.create();
        Future<ProcessResult<OutputsPair>> outputFuture =
                processExecutor.executeProcess(command, environmentVariablesToUse, processExecutionOptions);
        ProcessResult<OutputsPair> output = outputFuture.get();
        OutputsPair outputsPair = output.getProcessOutputs();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("exitCode", output.getExitCode());
        resultMap.put("standardOutput", outputsPair.getStandardOutput());
        resultMap.put("errorOutput", outputsPair.getErrorOutput());
        return GsonHelper.toJson(resultMap);
    }

    /**
     * Creates the environment variables.
     *
     * @param add the add
     * @param remove the remove
     * @return the map
     */
    private static Map<String, String> createEnvironmentVariables(Map<String, String> add, List<String> remove) {
        if (add == null) {
            return new ProcessBuilder().environment();
        }

        Map<String, String> environmentVariables = new HashMap<>(add);
        if (remove != null) {
            remove.forEach(environmentVariables.keySet()::remove);
        }

        return environmentVariables;
    }

}
