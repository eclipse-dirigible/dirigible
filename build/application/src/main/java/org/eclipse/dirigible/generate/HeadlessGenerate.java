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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.Set;

/**
 * Regenerates a project from its {@code *.intent} without a running platform, or checks it for
 * drift (#7642). It lives in the platform jar and is launched from it, so the CLI that runs it
 * stays a launcher (#7793):
 *
 * <pre>
 * java -Dloader.main=org.eclipse.dirigible.generate.HeadlessGenerate -jar dirigible-application-executable.jar \
 *     [--project &lt;dir&gt;] [--out &lt;dir&gt;] [--check]
 * </pre>
 *
 * Exits 0 when generated (or, with {@code --check}, when there is no drift), {@link #EXIT_DRIFT} on
 * drift, {@link #EXIT_GENERATION_FAILED} when the intent cannot be generated.
 */
public final class HeadlessGenerate {

    /** {@code --check} found committed files that differ from the regeneration. */
    public static final int EXIT_DRIFT = 1;

    /** The intent was refused, one of its code generations failed, or the arguments are wrong. */
    public static final int EXIT_GENERATION_FAILED = 2;

    /** Quiet logging: the report is the output, the platform's own logback.xml would log to files. */
    private static final String LOGGING_CONFIGURATION = "generate-logback.xml";

    private HeadlessGenerate() {
        // entry point
    }

    /**
     * The entry point.
     *
     * @param args the command line
     */
    public static void main(String[] args) {
        // before the first logger is created
        System.setProperty("logback.configurationFile", LOGGING_CONFIGURATION);
        System.exit(run(args, System.out));
    }

    /**
     * Regenerates a project, or checks it for drift.
     *
     * @param args {@code [--project <dir>] [--out <dir>] [--check]}
     * @param out where the report goes
     * @return the exit code
     */
    static int run(String[] args, PrintStream out) {
        String projectOption = null;
        String outOption = null;
        boolean check = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--project" -> projectOption = i + 1 < args.length ? args[++i] : null;
                case "--out" -> outOption = i + 1 < args.length ? args[++i] : null;
                case "--check" -> check = true;
                default -> {
                    out.println("Unknown argument [" + args[i] + "] - expected [--project <dir>] [--out <dir>] [--check]");
                    return EXIT_GENERATION_FAILED;
                }
            }
        }
        Path project = Path.of(isBlank(projectOption) ? System.getProperty("user.dir") : projectOption)
                           .toAbsolutePath()
                           .normalize();
        if (!Files.isDirectory(project)) {
            out.println("No project folder at [" + project + "]");
            return EXIT_GENERATION_FAILED;
        }
        out.println("Eclipse Dirigible " + platformVersion() + " - generate " + project);
        try (IntentRegeneration regeneration = new IntentRegeneration()) {
            Path regenerated = regeneration.regenerate(project);
            GenerationDrift drift = GenerationDrift.between(project, regenerated);
            if (check) {
                return reportDrift(out, drift);
            }
            Path target = isBlank(outOption) ? project
                    : Path.of(outOption)
                          .toAbsolutePath()
                          .normalize();
            Written written = write(drift, regenerated, project, target);
            out.println("Regenerated into [" + target + "]: " + written.files() + " file(s) written, " + written.removed() + " removed");
            return 0;
        } catch (GenerationFailedException e) {
            out.println(e.getMessage());
            return EXIT_GENERATION_FAILED;
        } catch (IOException e) {
            out.println("Generating [" + project + "] failed: " + e.getMessage());
            return EXIT_GENERATION_FAILED;
        }
    }

    private static int reportDrift(PrintStream out, GenerationDrift drift) {
        if (drift.isClean()) {
            out.println("No drift: every committed file is what the intent generates (" + drift.uncommitted()
                                                                                               .size()
                    + " generated file(s) are not committed)");
            return 0;
        }
        out.println("Drift: " + drift.changed()
                                     .size()
                + " committed file(s) differ from the regeneration, " + drift.removed()
                                                                             .size()
                + " are no longer generated");
        out.print(drift.diff());
        return EXIT_DRIFT;
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

    /** The platform release, as {@code dirigible.properties} carries it. */
    private static String platformVersion() {
        try (InputStream in = HeadlessGenerate.class.getResourceAsStream("/dirigible.properties")) {
            if (in == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("DIRIGIBLE_PRODUCT_VERSION", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
