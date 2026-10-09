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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.dirigible.cli.util.ProcessManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code generate} runs the platform jar's headless generate and ends with its exit code - the CLI
 * carries no generator of its own (#7793).
 */
class GenerateCommandsTest {

    @TempDir
    Path folder;

    /** Records the command instead of starting it. */
    private static final class RecordingProcessManager extends ProcessManager {

        private final int exitCode;
        private List<String> command;

        RecordingProcessManager(int exitCode) {
            this.exitCode = exitCode;
        }

        @Override
        public int startSynchronously(String... commandArgs) {
            command = List.of(commandArgs);
            return exitCode;
        }
    }

    @Test
    void aCheckRunsThePlatformJarAndEndsWithItsExitCode() throws IOException {
        Path jar = Files.createFile(folder.resolve("dirigible-application-executable.jar"));
        Path project = Files.createDirectories(folder.resolve("project"));
        RecordingProcessManager processManager = new RecordingProcessManager(1);
        CommandExitCode exitCode = new CommandExitCode();

        new GenerateCommands(exitCode, processManager).generate(jar.toString(), project.toString(), null, true);

        assertThat(processManager.command).containsExactly("java", "-Dloader.main=" + GenerateCommands.GENERATE_MAIN, "-jar",
                jar.toAbsolutePath()
                   .toString(),
                "--project", project.toAbsolutePath()
                                    .toString(),
                "--check");
        assertThat(exitCode.getExitCode()).as("the drift exit code of the platform")
                                          .isEqualTo(1);
    }

    @Test
    void anOutputFolderIsPassedOnAndAWriteIsNotACheck() throws IOException {
        Path jar = Files.createFile(folder.resolve("dirigible-application-executable.jar"));
        Path out = folder.resolve("out");
        RecordingProcessManager processManager = new RecordingProcessManager(0);
        CommandExitCode exitCode = new CommandExitCode();

        new GenerateCommands(exitCode, processManager).generate(jar.toString(), null, out.toString(), false);

        assertThat(processManager.command).containsSubsequence("--out", out.toAbsolutePath()
                                                                           .toString())
                                          .doesNotContain("--check", "--project");
        assertThat(exitCode.getExitCode()).isZero();
    }

    @Test
    void withoutThePlatformJarNothingRuns() {
        RecordingProcessManager processManager = new RecordingProcessManager(0);
        CommandExitCode exitCode = new CommandExitCode();

        String report = new GenerateCommands(exitCode, processManager).generate(folder.resolve("missing.jar")
                                                                                      .toString(),
                null, null, true);

        assertThat(processManager.command).isNull();
        assertThat(exitCode.getExitCode()).isEqualTo(GenerateCommands.EXIT_GENERATION_FAILED);
        assertThat(report).contains("--dirigibleJarPath");
    }
}
