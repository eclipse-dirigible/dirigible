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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

/**
 * {@code generate}: regenerates a project from its {@code *.intent} without a running platform
 * (#7642), and with {@code --check} reports how its committed files drift from the regeneration -
 * the pull-request gate a module repository runs against the platform version it pins.
 *
 * <p>
 * Exit codes: 0 when the project is regenerated, or with {@code --check} when nothing drifts; 1
 * when {@code --check} finds drift; 2 when the intent cannot be generated at all.
 */
@Component
public class GenerateCommands {

    /** {@code --check} found committed files that differ from the regeneration. */
    static final int EXIT_DRIFT = 1;

    /** The intent was refused, or one of its code generations failed. */
    static final int EXIT_GENERATION_FAILED = 2;

    private final CommandExitCode exitCode;
    private final String platformVersion;

    GenerateCommands(CommandExitCode exitCode, ObjectProvider<BuildProperties> buildProperties) {
        this.exitCode = exitCode;
        BuildProperties build = buildProperties.getIfAvailable();
        this.platformVersion = build == null ? "unknown" : build.getVersion();
    }

    /**
     * Regenerates a project, or checks it for drift.
     *
     * @param projectOption the project folder, the working directory when absent
     * @param outOption where to write the regenerated project, the project itself when absent
     * @param check whether to compare instead of write
     * @return the report
     * @throws IOException when the project cannot be read or the output written
     */
    @Command(name = "generate",
            description = "Regenerate a project from its *.intent without a running platform; with --check, report drift from the committed files.")
    public String generate(
            @Option(longName = "project",
                    description = "The project folder. If not specified, the working directory is used.") String projectOption,
            @Option(longName = "out",
                    description = "Where to write the regenerated project. If not specified, the project itself.") String outOption,
            @Option(longName = "check", defaultValue = "false",
                    description = "Write nothing; print the unified diff of every committed file the regeneration changes or drops, and exit 1 if there is one.") boolean check)
            throws IOException {
        Path project = Path.of(isBlank(projectOption) ? System.getProperty("user.dir") : projectOption)
                           .toAbsolutePath()
                           .normalize();
        if (!Files.isDirectory(project)) {
            exitCode.set(EXIT_GENERATION_FAILED);
            return "No project folder at [" + project + "]";
        }
        StringBuilder report = new StringBuilder("Eclipse Dirigible ").append(platformVersion)
                                                                      .append(" - generate ")
                                                                      .append(project)
                                                                      .append('\n');
        try (IntentRegeneration regeneration = new IntentRegeneration()) {
            Path regenerated = regeneration.regenerate(project);
            GenerationDrift drift = GenerationDrift.between(project, regenerated);
            if (check) {
                reportDrift(report, drift);
            } else {
                Path out = isBlank(outOption) ? project
                        : Path.of(outOption)
                              .toAbsolutePath()
                              .normalize();
                Written written = write(drift, regenerated, project, out);
                report.append("Regenerated into [")
                      .append(out)
                      .append("]: ")
                      .append(written.files())
                      .append(" file(s) written, ")
                      .append(written.removed())
                      .append(" removed\n");
            }
        } catch (GenerationFailedException e) {
            exitCode.set(EXIT_GENERATION_FAILED);
            report.append(e.getMessage())
                  .append('\n');
        }
        return report.toString();
    }

    private void reportDrift(StringBuilder report, GenerationDrift drift) {
        if (drift.isClean()) {
            report.append("No drift: every committed file is what the intent generates (")
                  .append(drift.uncommitted()
                               .size())
                  .append(" generated file(s) are not committed)\n");
            return;
        }
        exitCode.set(EXIT_DRIFT);
        report.append("Drift: ")
              .append(drift.changed()
                           .size())
              .append(" committed file(s) differ from the regeneration, ")
              .append(drift.removed()
                           .size())
              .append(" are no longer generated\n")
              .append(drift.diff());
    }

    /** What a write did: the files it wrote and the files it removed. */
    private record Written(int files, int removed) {
    }

    /**
     * Writes the regeneration. Into the project itself the regeneration is applied - what it changes or
     * adds is written and what it no longer produces is deleted, as the IDE's Generate does; anywhere
     * else the regenerated project is copied whole.
     */
    private static Written write(GenerationDrift drift, Path regenerated, Path project, Path out) throws IOException {
        if (out.equals(project)) {
            for (String file : drift.changed()) {
                copy(regenerated.resolve(file), project.resolve(file));
            }
            for (String file : drift.uncommitted()) {
                copy(regenerated.resolve(file), project.resolve(file));
            }
            for (String file : drift.removed()) {
                Files.delete(project.resolve(file));
            }
            return new Written(drift.changed()
                                    .size()
                    + drift.uncommitted()
                           .size(),
                    drift.removed()
                         .size());
        }
        Set<String> files = GenerationDrift.filesOf(regenerated);
        for (String file : files) {
            copy(regenerated.resolve(file), out.resolve(file));
        }
        return new Written(files.size(), 0);
    }

    private static void copy(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
