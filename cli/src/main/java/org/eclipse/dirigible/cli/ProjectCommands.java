/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli;

import org.eclipse.dirigible.cli.project.ProjectGenerator;
import org.eclipse.dirigible.cli.server.DirigibleServer;
import org.eclipse.dirigible.cli.server.DirigibleServerConfig;
import org.eclipse.dirigible.cli.util.SleepUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
class ProjectCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectCommands.class);

    private final DirigibleServer dirigibleServer;
    private final ProjectGenerator projectGenerator;

    ProjectCommands(DirigibleServer dirigibleServer, ProjectGenerator projectGenerator) {
        this.dirigibleServer = dirigibleServer;
        this.projectGenerator = projectGenerator;
    }

    @Command(name = "new", description = "Generate Eclipse Dirigible project.")
    String generateNewProject(
            @Option(longName = "name", shortName = 'n', defaultValue = "dirigible-project",
                    description = "The name of the project") String projectName,
            @Option(longName = "override", shortName = 'o', defaultValue = "false",
                    description = "Set to true to overwrite the existing project if it already exists.") boolean overrideProject) {
        Path projectPath = projectGenerator.generate(projectName, overrideProject);

        return "Successfully created project " + projectName + " in path " + projectPath.toString();
    }

    @Command(name = "start", description = "Run Eclipse Dirigible project")
    String startProject(@Option(longName = "dirigibleJarPath",
            description = "Path to the Eclipse Dirigible fat/uber jar. This value is automatically resolved when the CLI is installed via npm.") String dirigibleJarPathOption,
            @Option(longName = "projectPath",
                    description = "Path to Eclipse Dirigible project. If not specified, user working directory will be used.") String projectPathOption,
            @Option(longName = "watch", shortName = 'w', defaultValue = "false",
                    description = "Run in watch mode (live-reload).") boolean watchMode) {

        Path dirigibleJarPath = getDirigibleJarPath(dirigibleJarPathOption);
        Path projectPath = getProjectPath(projectPathOption);

        DirigibleServerConfig serverConfig = new DirigibleServerConfig(dirigibleJarPath, projectPath, watchMode);
        int exitCode = dirigibleServer.start(serverConfig);

        SleepUtil.sleepMillis(TimeUnit.SECONDS, 3);// give time to the server to stop (prevent mixed logs)
        return "Server exited with code: " + exitCode;
    }

    private Path getDirigibleJarPath(String dirigibleJarPathOption) {
        if (isOptionProvided(dirigibleJarPathOption)) {
            LOGGER.info("Provided dirigible jar path option with value: {}", dirigibleJarPathOption);
        } else {
            throw new IllegalStateException("Missing the dirigible jar path option.");
        }
        Path dirigibleJarPath = Path.of(dirigibleJarPathOption);

        if (!Files.exists(dirigibleJarPath)) {
            throw new IllegalArgumentException(
                    "Invalid value [" + dirigibleJarPathOption + "] for dirigible jar path. File does not exist.");
        }
        return dirigibleJarPath;
    }

    private boolean isOptionProvided(Object optionValue) {
        if (optionValue == null) {
            return false;
        }
        if (optionValue instanceof String s) {
            return !s.isEmpty();
        }
        return true;
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
