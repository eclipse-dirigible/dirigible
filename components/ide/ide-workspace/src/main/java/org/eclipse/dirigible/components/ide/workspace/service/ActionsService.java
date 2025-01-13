/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.process.Piper;
import org.eclipse.dirigible.commons.process.ProcessUtils;
import org.eclipse.dirigible.components.base.helpers.logging.LoggingOutputStream;
import org.eclipse.dirigible.components.command.CommandDescriptor;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.project.ProjectAction;
import org.eclipse.dirigible.components.project.ProjectMetadata;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;
import org.eclipse.dirigible.repository.local.LocalWorkspaceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Class ActionsService.
 */
@Service
public class ActionsService {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ActionsService.class);

    /**
     * The workspace service.
     */
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * Execute action.
     *
     * @param workspace the workspace
     * @param project the project
     * @param action the action
     * @return the int
     */
    public int executeAction(String workspace, String project, String action) {
        Project projectObject = workspaceService.getProject(workspace, project);
        File fileObject = projectObject.getFile("project.json");
        if (!fileObject.exists()) {
            // no project.json file, hence no action to be executed
            return -1;
        }
        try {
            ProjectMetadata projectJson = GsonHelper.fromJson(new String(fileObject.getContent()), ProjectMetadata.class);
            List<ProjectAction> actions = projectJson.getActions();
            if (actions == null) {
                logger.error("Actions section not found in the project descriptor file: " + project);
            } else {
                ProjectAction projectAction = actions.stream()
                                                     .filter(a -> a.getName()
                                                                   .equals(action))
                                                     .findFirst()
                                                     .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                             "Action not found: " + action));

                String workingDirectory =
                        LocalWorkspaceMapper.getMappedName((FileSystemRepository) projectObject.getRepository(), projectObject.getPath());
                int result = 0;
                for (CommandDescriptor next : getCommandsForOS(projectAction)) {
                    result += executeCommandLine(workingDirectory, next.getCommand());
                    if (result > 0) {
                        break;
                    }
                }
                return result;
            }
        } catch (Exception e) {
            String errorMessage = "Malformed project file: " + project + " (" + e.getMessage() + ")";
            logger.error(errorMessage, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }
        return -1;
    }

    /**
     * Gets the commands for OS.
     *
     * @param projectAction the project action
     * @return the commands for OS
     */
    public List<CommandDescriptor> getCommandsForOS(ProjectAction projectAction) {
        List<CommandDescriptor> commands = projectAction.getCommands();
        return commands.stream()
                       .filter(CommandDescriptor::isCompatibleWithCurrentOS)
                       .collect(Collectors.toList());
    }

    /**
     * Execute command line.
     *
     * @param workingDirectory the working directory
     * @param commandLine the command line
     * @return the int
     * @throws Exception the exception
     */
    public int executeCommandLine(String workingDirectory, String commandLine) throws Exception {
        logger.info("Executing [{}], working dir [{}]", commandLine, workingDirectory);
        int result = 0;
        String[] args;
        try {
            args = ProcessUtils.translateCommandline(commandLine);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to translate [%s]", commandLine);
            logger.error(errorMessage, e);
            throw new Exception(errorMessage, e);
        }

        try {
            ProcessBuilder processBuilder = ProcessUtils.createProcess(args);

            processBuilder.directory(new java.io.File(workingDirectory));

            processBuilder.redirectErrorStream(true);

            Process process = ProcessUtils.startProcess(args, processBuilder);
            Piper pipe = new Piper(process.getInputStream(), new LoggingOutputStream(logger, LoggingOutputStream.LogLevel.INFO));
            new Thread(pipe).start();
            try {
                int i = 0;
                boolean deadYet = false;
                do {
                    Thread.sleep(ProcessUtils.DEFAULT_WAIT_TIME);
                    try {
                        result = process.exitValue();
                        deadYet = true;
                    } catch (IllegalThreadStateException e) {
                        if (++i >= ProcessUtils.DEFAULT_LOOP_COUNT) {
                            process.destroy();
                            throw new RuntimeException(
                                    "Exceeds timeout - " + ((ProcessUtils.DEFAULT_WAIT_TIME / 1000) * ProcessUtils.DEFAULT_LOOP_COUNT), e);
                        }
                    }
                } while (!deadYet);

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new IOException(e);
            }
        } catch (Exception e) {
            String errorMessage = String.format("Failed to execute [%s], working dir [%s]", commandLine, workingDirectory);
            logger.error(errorMessage, e);
            throw new Exception(errorMessage, e);
        }
        return result;
    }

    /**
     * List actions.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the string
     */
    public String listActions(String workspace, String project) {
        return GsonHelper.toJson(listRegisteredActions(workspace, project));
    }

    /**
     * List actions.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the list of actions
     */
    public List<ProjectAction> listRegisteredActions(String workspace, String project) {
        Project projectObject = workspaceService.getProject(workspace, project);
        File fileObject = projectObject.getFile("project.json");
        if (!fileObject.exists()) {
            return new ArrayList<ProjectAction>();
        }
        try {
            ProjectMetadata projectJson = GsonHelper.fromJson(new String(fileObject.getContent()), ProjectMetadata.class);
            List<ProjectAction> actions = projectJson.getActions();
            if (actions == null) {
                logger.error("Actions section not found in the project descriptor file: " + project);
                return new ArrayList<ProjectAction>();
            }
            return actions;
        } catch (Exception e) {
            String error = "Malformed project file: " + project + " (" + e.getMessage() + ")";
            logger.error(error);
            logger.trace(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

}
