/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.generate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code generate} regenerates a project from its intent without a running platform, and
 * {@code --check} tells a project whose committed files are its regeneration from one whose are not
 * (#7642), run from the platform jar's classes (#7793).
 */
class HeadlessGenerateTest {

    private static final String SAMPLE = "regeneration-sample";
    private static final String INTENT = "app.intent";

    @TempDir
    Path workspace;

    /** A check is only as good as the output is stable: two regenerations write the same bytes. */
    @Test
    void twoRegenerationsAreByteIdentical() throws IOException {
        Path project = sample();

        Map<String, byte[]> first = regenerate(project);
        Map<String, byte[]> second = regenerate(project);

        assertThat(first).as("the regeneration writes the generated code")
                         .containsKey("gen/regen/api/ticket/TicketController.java");
        assertThat(second.keySet()).isEqualTo(first.keySet());
        first.forEach((file, content) -> assertThat(second.get(file)).as(file)
                                                                     .isEqualTo(content));
    }

    @Test
    void aRegeneratedProjectHasNoDriftAndAnEditedIntentHas() throws IOException {
        Path project = sample();

        Run written = run("--project", project.toString());
        assertThat(written.exitCode()).as(written.output())
                                      .isZero();
        assertThat(project.resolve("gen/regen/data/ticket/TicketEntity.java")).exists();

        Run clean = run("--project", project.toString(), "--check");
        assertThat(clean.exitCode()).as(clean.output())
                                    .isZero();
        assertThat(clean.output()).contains("No drift");

        Path intent = project.resolve(INTENT);
        Files.writeString(intent, Files.readString(intent, StandardCharsets.UTF_8)
                                       .replace("required: true, length: 200", "required: true, length: 250"),
                StandardCharsets.UTF_8);
        Run drift = run("--project", project.toString(), "--check");
        assertThat(drift.exitCode()).as(drift.output())
                                    .isEqualTo(HeadlessGenerate.EXIT_DRIFT);
        assertThat(drift.output()).contains("--- a/gen/regen/data/ticket/TicketEntity.java",
                "-    @Column(name = \"TICKET_SUBJECT\", length = 200", "+    @Column(name = \"TICKET_SUBJECT\", length = 250");
        assertThat(Files.readString(project.resolve("gen/regen/data/ticket/TicketEntity.java"),
                StandardCharsets.UTF_8)).as("a check writes nothing")
                                        .contains("length = 200");
    }

    @Test
    void aFolderWithoutAnIntentIsRefused() throws IOException {
        Run report = run("--project", Files.createDirectories(workspace.resolve("empty"))
                                           .toString(),
                "--check");

        assertThat(report.exitCode()).isEqualTo(HeadlessGenerate.EXIT_GENERATION_FAILED);
        assertThat(report.output()).contains("No *.intent file");
    }

    /** What a run printed and the exit code it ended with. */
    private record Run(int exitCode, String output) {
    }

    private static Run run(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int exitCode = HeadlessGenerate.run(args, new PrintStream(output, true, StandardCharsets.UTF_8));
        return new Run(exitCode, output.toString(StandardCharsets.UTF_8));
    }

    private static Map<String, byte[]> regenerate(Path project) throws IOException {
        try (IntentRegeneration regeneration = new IntentRegeneration()) {
            Path regenerated = regeneration.regenerate(project);
            Map<String, byte[]> files = new LinkedHashMap<>();
            for (String file : GenerationDrift.filesOf(regenerated)) {
                files.put(file, Files.readAllBytes(regenerated.resolve(file)));
            }
            return files;
        }
    }

    private Path sample() throws IOException {
        Path project = Files.createDirectories(workspace.resolve(SAMPLE));
        try (InputStream in = HeadlessGenerateTest.class.getResourceAsStream("/" + SAMPLE + "/" + INTENT)) {
            Files.write(project.resolve(INTENT), in.readAllBytes());
        }
        return project;
    }
}
