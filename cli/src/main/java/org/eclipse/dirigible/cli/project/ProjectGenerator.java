/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli.project;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class ProjectGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectGenerator.class);

    private static final String PACKAGE_JSON_TEMPLATE = """
            {
              "name": "%s",
              "version": "1.0.0",
              "scripts": {
                "start": "dirigible start",
                "start:dev": "dirigible start --watch"
              },
              "devDependencies": {
                "@dirigiblelabs/dirigible-cli": "latest"
              }
            }
            """;

    private static final String HELLO_TS_CONTENT = """
            import { response } from "sdk/http";

            response.println("Hello World!");
            """;

    public Path generate(String projectName, boolean overrideProject) {
        // the folder where the command is executed
        String userDir = System.getProperty("user.dir");
        Path projectPath = Path.of(userDir, projectName);

        if (Files.exists(projectPath)) {
            if (overrideProject) {
                LOGGER.info("Will override project with path [{}]", projectPath);
                deleteExistingProject(projectPath);
            } else {
                throw new IllegalArgumentException(
                        "Project with name [" + projectName + "] already exists in [" + projectPath + "]. Use --override to replace it.");
            }
        }

        createDir(projectPath);

        String packageJsonContent = String.format(PACKAGE_JSON_TEMPLATE, projectName);
        createProjectFile(projectPath, "package.json", packageJsonContent);

        createProjectFile(projectPath, "hello.ts", HELLO_TS_CONTENT);

        return projectPath;
    }

    private void createDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create directory " + path, ex);
        }
    }

    private void createProjectFile(Path projectPath, String fileName, String content) {
        Path filePath = projectPath.resolve(fileName);
        try {
            Files.writeString(filePath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create file " + filePath, ex);
        }
    }

    private static void deleteExistingProject(Path projectPath) {
        try {
            FileUtils.deleteDirectory(projectPath.toFile());
        } catch (IOException ex) {
            LOGGER.warn("Failed to delete the existing project dir [{}]", projectPath, ex);
        }
    }
}
