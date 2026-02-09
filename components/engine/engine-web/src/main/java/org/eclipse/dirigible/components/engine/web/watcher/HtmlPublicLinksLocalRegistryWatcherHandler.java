/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.watcher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.registry.watcher.LocalRegistryWatcherHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The Class HtmlPublicLinksLocalRegistryWatcherHandler.
 *
 */
@Component
@Scope("singleton")
public class HtmlPublicLinksLocalRegistryWatcherHandler implements LocalRegistryWatcherHandler {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HtmlPublicLinksLocalRegistryWatcherHandler.class);

    /** The injector. */
    private final HtmlPlatformLinksInjector injector;

    /**
     * Instantiates a new html public links local registry watcher handler.
     */
    public HtmlPublicLinksLocalRegistryWatcherHandler() {
        List<PlatformAsset> assets = PlatformAssetsJsonLoader.loadAssetsFromJson();
        this.injector = new HtmlPlatformLinksInjector(assets);
    }

    /**
     * Directory registered.
     *
     * @param path the path
     */
    @Override
    public void directoryRegistered(Path path) {}

    /**
     * Directory created.
     *
     * @param path the path
     */
    @Override
    public void directoryCreated(Path path) {}

    /**
     * File registered.
     *
     * @param path the path
     */
    @Override
    public void fileRegistered(Path path) {
        findAndReplace(path);
    }

    /**
     * File created.
     *
     * @param path the path
     */
    @Override
    public void fileCreated(Path path) {
        findAndReplace(path);
    }

    /**
     * File modified.
     *
     * @param path the path
     */
    @Override
    public void fileModified(Path path) {
        findAndReplace(path);
    }

    /**
     * File deleted.
     *
     * @param path the path
     */
    @Override
    public void fileDeleted(Path path) {}

    /**
     * Find and replace.
     *
     * @param path the path
     */
    private void findAndReplace(Path path) {
        try {
            if (path.toString()
                    .endsWith(".html")) {
                try (FileInputStream fis = new FileInputStream(path.toFile())) {
                    String originalHtml = IOUtils.toString(fis, StandardCharsets.UTF_8);
                    if (!originalHtml.contains("platform-links")) {
                        return;
                    }
                    String processedHtml = injector.processHtml(originalHtml);
                    FileUtils.write(path.toFile(), processedHtml, StandardCharsets.UTF_8);
                }
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

}
