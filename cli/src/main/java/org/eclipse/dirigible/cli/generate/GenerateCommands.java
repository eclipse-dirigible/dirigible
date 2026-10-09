/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli.generate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.cli.util.ProcessManager;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

/**
 * {@code generate}: regenerates a project from its {@code *.intent} without a running platform, or
 * checks it for drift (#7642). The generator is the platform's own, so it runs from the platform
 * jar - the CLI stays a launcher and does not carry the platform (#7793).
 */
@Component
public class GenerateCommands {

    /** The platform jar's headless generate entry point. */
    static final String GENERATE_MAIN = "org.eclipse.dirigible.generate.HeadlessGenerate";

    /** The platform jar is missing, or the generate process could not be started. */
    static final int EXIT_GENERATION_FAILED = 2;

    private final CommandExitCode exitCode;
    private final ProcessManager processManager;

    GenerateCommands(CommandExitCode exitCode, ProcessManager processManager) {
        this.exitCode = exitCode;
        this.processManager = processManager;
    }

    /**
     * Regenerates a project, or checks it for drift. The exit code is the platform's: 0, 1 on drift, 2
     * when the intent cannot be generated.
     *
     * @param dirigibleJarPathOption the platform jar
     * @param projectOption the project folder, the working directory when absent
     * @param outOption where to write the regenerated project, the project itself when absent
     * @param check whether to compare instead of write
     * @return what went wrong before the platform ran, empty otherwise - the platform prints its own
     *         report
     */
    @Command(name = "generate",
            description = "Regenerate a project from its *.intent without a running platform; with --check, report drift from the committed files.")
    public String generate(@Option(longName = "dirigibleJarPath",
            description = "Path to the Eclipse Dirigible fat/uber jar. This value is automatically resolved when the CLI is installed via npm.") String dirigibleJarPathOption,
            @Option(longName = "project",
                    description = "The project folder. If not specified, the working directory is used.") String projectOption,
            @Option(longName = "out",
                    description = "Where to write the regenerated project. If not specified, the project itself.") String outOption,
            @Option(longName = "check", defaultValue = "false",
                    description = "Write nothing; print the unified diff of every committed file the regeneration changes or drops, and exit 1 if there is one.") boolean check) {
        if (isBlank(dirigibleJarPathOption) || !Files.isRegularFile(Path.of(dirigibleJarPathOption))) {
            exitCode.set(EXIT_GENERATION_FAILED);
            return "No Eclipse Dirigible jar at [" + dirigibleJarPathOption + "] - pass --dirigibleJarPath";
        }
        List<String> command = new ArrayList<>(List.of("java", "-Dloader.main=" + GENERATE_MAIN, "-jar", Path.of(dirigibleJarPathOption)
                                                                                                             .toAbsolutePath()
                                                                                                             .toString()));
        if (!isBlank(projectOption)) {
            command.add("--project");
            command.add(absolute(projectOption));
        }
        if (!isBlank(outOption)) {
            command.add("--out");
            command.add(absolute(outOption));
        }
        if (check) {
            command.add("--check");
        }
        exitCode.set(processManager.startSynchronously(command.toArray(String[]::new)));
        return "";
    }

    private static String absolute(String path) {
        return Path.of(path)
                   .toAbsolutePath()
                   .normalize()
                   .toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
