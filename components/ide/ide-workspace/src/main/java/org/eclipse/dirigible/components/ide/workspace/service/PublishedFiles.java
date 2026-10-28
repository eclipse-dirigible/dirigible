/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;

/**
 * The record of the files a publish copied into the registry, one per project, as paths relative to
 * the project root.
 * <p>
 * A publish copies the workspace project over its registry folder, so the registry alone cannot
 * tell a file deleted from the workspace apart from one that something else wrote there - the
 * JavaScript a TypeScript file is transpiled into, the output of a project's registry actions, a
 * file the application writes at runtime. This record can: a file it lists that the workspace no
 * longer has was put there by an earlier publish and is unpublished by the next one. It is kept
 * outside {@code /registry/public}, where no synchronizer, web endpoint or registry view reads it.
 */
final class PublishedFiles {

    /** The folder holding one record per published project. */
    static final String RECORDS_LOCATION = IRepositoryStructure.PATH_REGISTRY + IRepositoryStructure.SEPARATOR + ".published";

    private static final String LINE_SEPARATOR = "\n";

    private final IRepository repository;

    PublishedFiles(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Reads the files the earlier publishes of the project copied.
     *
     * @param project the project
     * @return the paths relative to the project root, empty when the project has no record
     */
    Set<String> read(String project) {
        IResource record = repository.getResource(recordLocation(project));
        if (!record.exists()) {
            return new TreeSet<>();
        }
        String content = new String(record.getContent(), StandardCharsets.UTF_8);
        return Arrays.stream(content.split(LINE_SEPARATOR))
                     .filter(line -> !line.isBlank())
                     .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Replaces the record of the project, or removes it when no file is left.
     *
     * @param project the project
     * @param files the paths relative to the project root
     */
    void write(String project, Set<String> files) {
        String location = recordLocation(project);
        IResource record = repository.getResource(location);
        if (files.isEmpty()) {
            if (record.exists()) {
                record.delete();
            }
            return;
        }
        byte[] content = String.join(LINE_SEPARATOR, new TreeSet<>(files))
                               .getBytes(StandardCharsets.UTF_8);
        if (record.exists()) {
            record.setContent(content);
        } else {
            repository.createResource(location, content);
        }
    }

    private static String recordLocation(String project) {
        return RECORDS_LOCATION + IRepositoryStructure.SEPARATOR + project;
    }

}
