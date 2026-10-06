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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

/**
 * How a project's committed files differ from what the platform regenerates from its intent.
 *
 * <p>
 * Drift is measured on what the project COMMITS: a committed file the regeneration writes
 * differently ({@link #changed()}), or one it no longer produces ({@link #removed()}). A file the
 * regeneration produces that the project does not carry ({@link #uncommitted()}) is reported but is
 * not drift - a project decides which generated files it keeps under version control (a module
 * repository keeps all of {@code gen/}, a test fixture only the Java its tests compile against).
 *
 * @param changed the committed files the regeneration writes differently, project-relative
 * @param removed the committed files the regeneration does not produce
 * @param uncommitted the regenerated files the project does not carry
 * @param diff the unified diff of the changed and removed files, from committed to regenerated
 */
record GenerationDrift(List<String> changed, List<String> removed, List<String> uncommitted, String diff) {

    /**
     * Compares a committed project with its regeneration.
     *
     * @param committed the project folder as committed
     * @param regenerated the regenerated project folder
     * @return the drift
     * @throws IOException when a file cannot be read
     */
    static GenerationDrift between(Path committed, Path regenerated) throws IOException {
        Set<String> committedFiles = filesOf(committed);
        Set<String> regeneratedFiles = filesOf(regenerated);
        List<String> changed = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        StringBuilder diff = new StringBuilder();
        for (String file : committedFiles) {
            byte[] before = Files.readAllBytes(committed.resolve(file));
            if (!regeneratedFiles.contains(file)) {
                removed.add(file);
                diff.append(unifiedDiff(file, before, new byte[0]));
                continue;
            }
            byte[] after = Files.readAllBytes(regenerated.resolve(file));
            if (!Arrays.equals(before, after)) {
                changed.add(file);
                diff.append(unifiedDiff(file, before, after));
            }
        }
        List<String> uncommitted = regeneratedFiles.stream()
                                                   .filter(file -> !committedFiles.contains(file))
                                                   .toList();
        return new GenerationDrift(changed, removed, uncommitted, diff.toString());
    }

    /** @return whether every committed file is what the regeneration writes */
    boolean isClean() {
        return changed.isEmpty() && removed.isEmpty();
    }

    /** The project-relative files of a folder, with {@code /} separators, in a stable order. */
    static Set<String> filesOf(Path folder) throws IOException {
        Set<String> files = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.filter(Files::isRegularFile)
                .map(file -> folder.relativize(file)
                                   .toString()
                                   .replace('\\', '/'))
                .filter(file -> !file.equals(".git") && !file.startsWith(".git/"))
                .forEach(files::add);
        }
        return files;
    }

    private static String unifiedDiff(String file, byte[] before, byte[] after) throws IOException {
        RawText a = new RawText(before);
        RawText b = new RawText(after);
        String header = "--- a/" + file + "\n+++ b/" + file + "\n";
        if (RawText.isBinary(before) || RawText.isBinary(after)) {
            return header + "Binary files differ\n";
        }
        EditList edits = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)
                                      .diff(RawTextComparator.DEFAULT, a, b);
        ByteArrayOutputStream hunks = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(hunks)) {
            formatter.format(edits, a, b);
        }
        return header + hunks.toString(StandardCharsets.UTF_8);
    }
}
