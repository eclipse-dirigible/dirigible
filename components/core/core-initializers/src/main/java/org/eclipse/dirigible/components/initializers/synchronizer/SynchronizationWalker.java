/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import org.eclipse.dirigible.repository.api.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The Class SynchronizationWalker.
 */
@Component
@Scope("singleton")
public class SynchronizationWalker {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SynchronizationWalker.class);


    /** The synchronization walker callback. */
    private SynchronizationWalkerCallback synchronizationWalkerCallback;

    /**
     * Instantiates a new synchronization walker.
     *
     * @param synchronizationWalkerCallback the synchronization walker callback
     */
    public SynchronizationWalker(SynchronizationWalkerCallback synchronizationWalkerCallback) {
        this.synchronizationWalkerCallback = synchronizationWalkerCallback;
    }

    /**
     * Walk.
     *
     * @param root the root
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void walk(String root) throws IOException {
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(Paths.get(root), opts, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                synchronizationWalkerCallback.visitFile(file, attrs, file.toString()
                                                                         .substring(root.length())
                                                                         .replace(File.separator, IRepository.SEPARATOR));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                logger.error("Failed to access file: " + file.toString());
                return FileVisitResult.CONTINUE;
            }

        });
    }

}
