/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.command.service;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.process.Piper;
import org.eclipse.dirigible.commons.process.ProcessUtils;
import org.eclipse.dirigible.components.api.http.HttpRequestFacade;
import org.eclipse.dirigible.components.api.http.HttpResponseFacade;
import org.eclipse.dirigible.components.command.Command;
import org.eclipse.dirigible.components.registry.accessor.RegistryAccessor;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * The Class WebService.
 */
@Service
@RequestScope
public class CommandService {
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CommandService.class);

    private final RegistryAccessor registryAccessor;

    CommandService(RegistryAccessor registryAccessor) {
        this.registryAccessor = registryAccessor;
    }

    /**
     * Exist resource.
     *
     * @param path the requested resource location
     * @return if the {@link IResource}
     */
    public boolean existResource(String path) {
        return registryAccessor.existResource(path);
    }

    /**
     * Execute service.
     *
     * @param module the module
     * @param params the params
     * @return the result
     * @throws Exception the exception
     */
    public String executeCommand(String module, Map<String, String> params) throws Exception {

        logger.trace("entering: executeCommand()"); //$NON-NLS-1$
        logger.trace("module = {}", module); //$NON-NLS-1$

        if (module == null) {
            throw new IllegalArgumentException("Command module name cannot be null");
        }

        if (HttpRequestFacade.isValid()) {
            HttpRequestFacade.setAttribute(HttpRequestFacade.ATTRIBUTE_REST_RESOURCE_PATH, getResource(module).getPath());
        }

        String result;

        String commandSource = new String(getResourceContent(module), StandardCharsets.UTF_8);
        String root = getRepositoryRoot();
        String workingDirectory = root + getResource(module).getParent()
                                                            .getPath();

        Command commandDefinition;
        try {
            commandDefinition = GsonHelper.fromJson(commandSource, Command.class);
        } catch (Exception e2) {
            logger.error(e2.getMessage(), e2);
            throw new Exception(e2);
        }

        commandDefinition.validate();

        String commandLine = commandDefinition.getTargetCommand()
                                              .getCommand();

        result = executeCommandLine(workingDirectory, commandLine, commandDefinition.getSet(), commandDefinition.getUnset(), params);

        try {
            HttpResponseFacade.setContentType(commandDefinition.getContentType());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        logger.trace("exiting: executeCommand()");
        return result;
    }

    /**
     * Gets the resource.
     *
     * @param path the requested resource location
     * @return the {@link IResource} instance
     */
    public IResource getResource(String path) {
        return registryAccessor.getResource(path);
    }

    /**
     * Gets the resource content.
     *
     * @param path the requested resource location
     * @return the {@link IResource} content as a byte array
     */
    public byte[] getResourceContent(String path) {
        return registryAccessor.getRegistryContent(path);
    }

    /**
     * Gets the repository root.
     *
     * @return the repository root
     */
    public String getRepositoryRoot() {
        return registryAccessor.getRepository()
                               .getParameter("REPOSITORY_ROOT_FOLDER");
    }

    /**
     * Execute command line.
     *
     * @param workingDirectory the working directory
     * @param commandLine the command line
     * @param forAdding the for adding
     * @param forRemoving the for removing
     * @param params the params
     * @return the string
     * @throws Exception the exception
     */
    public String executeCommandLine(String workingDirectory, String commandLine, Map<String, String> forAdding, List<String> forRemoving,
            Map<String, String> params) throws Exception {

        String[] args = translateCommandLine(commandLine);

        if (shouldLogCommand()) {
            logger.debug("executing command={}", commandLine);
        }

        ByteArrayOutputStream out;
        ProcessBuilder processBuilder = ProcessUtils.createProcess(args);

        ProcessUtils.addEnvironmentVariables(processBuilder, forAdding);
        ProcessUtils.addEnvironmentVariables(processBuilder, params);
        ProcessUtils.removeEnvironmentVariables(processBuilder, forRemoving);

        processBuilder.directory(new File(workingDirectory));

        processBuilder.redirectErrorStream(true);

        out = new ByteArrayOutputStream();
        Process process = ProcessUtils.startProcess(args, processBuilder);
        Piper pipe = new Piper(process.getInputStream(), out);
        new Thread(pipe).start();
        try {
            int i = 0;
            boolean deadYet = false;
            do {
                Thread.sleep(ProcessUtils.DEFAULT_WAIT_TIME);
                try {
                    process.exitValue();
                    deadYet = true;
                } catch (IllegalThreadStateException e) {
                    if (++i >= ProcessUtils.DEFAULT_LOOP_COUNT) {
                        process.destroy();
                        String message = "Exceeds timeout - " + ((ProcessUtils.DEFAULT_WAIT_TIME / 1000) * ProcessUtils.DEFAULT_LOOP_COUNT);
                        throw new RuntimeException(message, e);
                    }
                }
            } while (!deadYet);

        } catch (Exception e) {
            throw new IOException(e);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String[] translateCommandLine(String commandLine) throws Exception {
        try {
            return ProcessUtils.translateCommandline(commandLine);
        } catch (Exception e) {
            throw new Exception("Failed to translate commandLine [" + commandLine + "]", e);
        }
    }

    /**
     * Should log command.
     *
     * @return true, if successful
     */
    private boolean shouldLogCommand() {
        return DirigibleConfig.EXEC_COMMAND_LOGGING_ENABLED.getBooleanValue();
    }

}
