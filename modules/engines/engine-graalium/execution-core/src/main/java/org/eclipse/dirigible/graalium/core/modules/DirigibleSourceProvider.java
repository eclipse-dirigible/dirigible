/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.modules;

import org.eclipse.dirigible.commons.config.StaticObjects;
import org.eclipse.dirigible.graalium.core.JavascriptSourceProvider;
import org.eclipse.dirigible.graalium.core.javascript.CalledFromJS;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The Class DirigibleSourceProvider.
 */
@Component
@CalledFromJS
public class DirigibleSourceProvider implements JavascriptSourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleSourceProvider.class);

    /**
     * Gets the absolute source path.
     *
     * @param projectName the project name
     * @param projectFileName the project file name
     * @return the absolute source path
     */
    @Override
    public Path getAbsoluteSourcePath(String projectName, String projectFileName) {

        String projectFilePath = Path.of(projectName, projectFileName)
                                     .toString();
        String internalRepositoryRelativeSourcePath = getInternalRepositoryRelativeSourcePath(projectFilePath);
        String absoluteSourcePathString = getRepository().getInternalResourcePath(internalRepositoryRelativeSourcePath);
        return Path.of(absoluteSourcePathString);
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    static IRepository getRepository() {
        return (IRepository) StaticObjects.get(StaticObjects.REPOSITORY);
    }

    /**
     * Gets the internal repository relative source path.
     *
     * @param projectFilePath the project file path
     * @return the internal repository relative source path
     */
    protected String getInternalRepositoryRelativeSourcePath(String projectFilePath) {
        return Path.of(IRepositoryStructure.PATH_REGISTRY_PUBLIC, projectFilePath)
                   .toString();
    }

    /**
     * Gets the absolute project path.
     *
     * @param projectName the project name
     * @return the absolute project path
     */
    @Override
    public Path getAbsoluteProjectPath(String projectName) {
        String projectFilePath = Path.of(projectName)
                                     .toString();
        String internalRepositoryRelativeSourcePath = getInternalRepositoryRelativeSourcePath(projectFilePath);
        String absoluteSourcePathString = getRepository().getInternalResourcePath(internalRepositoryRelativeSourcePath);
        return Path.of(absoluteSourcePathString);
    }

    /**
     * Gets the source.
     *
     * @param sourceFilePath the project file path
     * @return the source
     */
    @Override
    public String getSource(String sourceFilePath) {
        sourceFilePath = withDefaultFileExtensionIfNecessary(sourceFilePath);

        String internalRepositoryRelativeSourcePath = getInternalRepositoryRelativeSourcePath(sourceFilePath);

        byte[] maybeContentFromRepository = tryGetFromRepository(internalRepositoryRelativeSourcePath);
        if (maybeContentFromRepository != null) {
            return new String(maybeContentFromRepository, StandardCharsets.UTF_8);
        }

        byte[] maybeContentFromClassLoader = tryGetFromClassLoader(internalRepositoryRelativeSourcePath, sourceFilePath);
        if (maybeContentFromClassLoader != null) {
            return new String(maybeContentFromClassLoader, StandardCharsets.UTF_8);
        }

        return null;
    }

    /**
     * With default file extension if necessary.
     *
     * @param filePath the file path
     * @return the string
     */
    protected static String withDefaultFileExtensionIfNecessary(String filePath) {
        if (filePath.endsWith(".js") || filePath.endsWith(".json") || filePath.endsWith(".mjs") || filePath.endsWith(".xsjs")
                || filePath.endsWith(".ts") || filePath.endsWith(".py")) {
            return filePath;
        }

        return filePath + ".js";
    }

    /**
     * Try get from repository.
     *
     * @param repositoryFilePathString the repository file path string
     * @return the byte[]
     */
    protected byte[] tryGetFromRepository(String repositoryFilePathString) {
        IResource resource = getRepository().getResource(repositoryFilePathString);
        if (!resource.exists()) {
            return null;
        }
        return resource.getContent();
    }

    /**
     * Try get from class loader.
     *
     * @param repositoryAwareFilePathString the repository aware file path string
     * @param filePathString the file path string
     * @return the byte[]
     */
    protected byte[] tryGetFromClassLoader(String repositoryAwareFilePathString, String filePathString) {
        try {
            var lookupPath = createLookupPath(filePathString);
            try (InputStream bundled = this.getClass()
                                           .getResourceAsStream(lookupPath)) {
                byte[] content = null;
                if (bundled != null) {
                    content = bundled.readAllBytes();
                    getRepository().createResource(repositoryAwareFilePathString, content);
                }
                return content;
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Creates the lookup path.
     *
     * @param filePathString the file path string
     * @return the string
     */
    protected String createLookupPath(String filePathString) {
        if (filePathString.startsWith(File.separator + "webjars")) {
            return File.separator + "META-INF" + File.separator + "resources" + filePathString;
        } else if (filePathString.startsWith("webjars")) {
            return File.separator + "META-INF" + File.separator + "resources" + File.separator + filePathString;
        }

        return File.separator + "META-INF" + File.separator + "dirigible" + File.separator + filePathString;
    }

    /**
     * Unpacked to file system.
     *
     * @param pathToUnpack the path to unpack
     * @param pathToLookup the path to lookup
     * @return the path
     */
    public Path unpackedToFileSystem(Path pathToUnpack, Path pathToLookup) {
        String path = File.separator + "META-INF" + File.separator + "dirigible" + File.separator + pathToLookup.toString();
        try (InputStream bundled = this.getClass()
                                       .getResourceAsStream(path)) {
            Files.createDirectories(pathToUnpack.getParent());
            if (!Files.exists(pathToUnpack)) {
                LOGGER.debug("File [{}] does NOT exist. Will be created", pathToUnpack);
                Files.createFile(pathToUnpack);
            } else {
                LOGGER.debug("File [{}] exists and will NOT be created", pathToUnpack);
            }
            if (null == bundled) {
                throw new IllegalStateException(
                        "Failed to load resource from path [" + path + "] and cannot be unpacked to [" + pathToUnpack + "]");
            }
            Files.copy(bundled, pathToUnpack, StandardCopyOption.REPLACE_EXISTING);
            return pathToUnpack;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to unpack [" + pathToLookup + "] to [" + pathToUnpack + "]", ex);
        }
    }
}
