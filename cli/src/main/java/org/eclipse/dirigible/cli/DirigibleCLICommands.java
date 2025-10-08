/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli;

import org.eclipse.dirigible.cli.server.DirigibleServer;
import org.eclipse.dirigible.cli.server.DirigibleServerConfig;
import org.eclipse.dirigible.cli.util.SleepUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@ShellComponent
public class DirigibleCLICommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleCLICommands.class);

    private final DirigibleServer dirigibleServer;

    DirigibleCLICommands(DirigibleServer dirigibleServer) {
        this.dirigibleServer = dirigibleServer;
    }

    @ShellMethod
    public String greet(@ShellOption String name) {
        return "Hello, " + name + "!";
    }

    @ShellMethod("Run Eclipse Dirigible project")
    public String start(
            @ShellOption(value = {"dirigibleJarPath"}, defaultValue = ShellOption.NULL,
                    help = "Path to Eclipse Dirigible jar") String dirigibleJarPathOption, //
            @ShellOption(value = {"projectPath"}, defaultValue = ShellOption.NULL,
                    help = "Path to Eclipse Dirigible project. If not specified, user working directory will be used.") String projectPathOption) {

        Path dirigibleJarPath = getDirigibleJarPath(dirigibleJarPathOption);
        Path projectPath = getProjectPath(projectPathOption);

        DirigibleServerConfig serverConfig = new DirigibleServerConfig(dirigibleJarPath, projectPath);
        int exitCode = dirigibleServer.start(serverConfig);

        SleepUtil.sleepMillis(TimeUnit.SECONDS, 3);// give time to the server to stop (prevent mixed logs)
        return "Server exited with code: " + exitCode;
    }

    private Path getDirigibleJarPath(String dirigibleJarPathOption) {
        if (isOptionProvided(dirigibleJarPathOption)) {
            LOGGER.info("Provided dirigible jar path option with value: {}", dirigibleJarPathOption);
        } else {
            LOGGER.info("Missing dirigible jar path option. Need to download the jar or we can make this option required???");
            throw new IllegalStateException("Flow not implemented");
        }
        Path dirigibleJarPath = Path.of(dirigibleJarPathOption);

        if (!Files.exists(dirigibleJarPath)) {
            throw new IllegalArgumentException(
                    "Invalid value [" + dirigibleJarPathOption + "] for dirigible jar path. File does not exist.");
        }
        return dirigibleJarPath;
    }

    private boolean isOptionProvided(Object optionValue) {
        return null != optionValue;
    }

    private Path getProjectPath(String projectPathOption) {
        if (isOptionProvided(projectPathOption)) {
            LOGGER.info("Provided project path with value: {}", projectPathOption);
            return Path.of(projectPathOption);
        }

        String userDir = System.getProperty("user.dir");
        LOGGER.info("Missing project path option. Will consider the user path [{}] as project directory", userDir);
        return Path.of(userDir);
    }

}
