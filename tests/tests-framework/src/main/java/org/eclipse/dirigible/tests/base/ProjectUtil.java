/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Component
public class ProjectUtil {

    public static final String JAR_PATH = ".jar!";
    public static final String FILE_PREFIX = "file:/";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectUtil.class);
    private final IRepository repository;

    ProjectUtil(IRepository repository) {
        this.repository = repository;
    }

    public void copyResourceProjectToDefaultUserWorkspace(String resourcesFolder) {
        copyResourceProjectToDefaultUserWorkspace(resourcesFolder, Collections.emptyMap());
    }

    public void copyResourceProjectToDefaultUserWorkspace(String resourcesFolder, Map<String, String> placeholders) {
        copyResourceProjectToUserWorkspace(DirigibleTestTenant.createDefaultTenant()
                                                              .getUsername(),
                resourcesFolder, placeholders);
    }

    public void copyResourceProjectToUserWorkspace(String user, String resourcesFolder, Map<String, String> placeholders) {
        String targetProjectName = extractFolderName(resourcesFolder);

        copyResourceFolderContentToUserWorkspaceProject(user, resourcesFolder, targetProjectName, placeholders);
    }

    private String extractFolderName(String resourcesFolder) {
        boolean rootDir = resourcesFolder.lastIndexOf("/") == -1;
        return rootDir ? resourcesFolder : (resourcesFolder.substring(resourcesFolder.lastIndexOf("/") + 1));
    }

    public void copyResourceFolderContentToUserWorkspaceProject(String user, String resourcesFolder, String targetProjectName,
            Map<String, String> placeholders) {
        String destinationDirPath = createUserWorkspaceFolderPath(user, targetProjectName);

        copyResourceFolder(resourcesFolder, destinationDirPath, placeholders);
    }

    public void copyResourceFolder(String resourcesFolder, String destinationDirPath, Map<String, String> placeholders) {
        String sourceDirPath = getResourcePath(resourcesFolder);
        File sourceDir = new File(sourceDirPath);
        File destinationDir = new File(destinationDirPath);

        LOGGER.debug("Copy folder [{}] to folder [{}]", sourceDir, destinationDir);

        try {
            if (sourceDirPath.contains(JAR_PATH)) {
                copyFromJar(sourceDirPath, destinationDir);
            } else if (sourceDir.exists()) {
                FileUtils.copyDirectory(sourceDir, destinationDir);
            } else {
                throw new IllegalStateException("Source is neither a valid directory nor a valid JAR resource: " + sourceDirPath);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to copy [" + sourceDir + "] to " + destinationDir, ex);
        }

        Collection<File> files = FileUtils.listFiles(destinationDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : files) {
            replacePlaceholderInFile(file, placeholders);
        }
    }

    private String getResourcePath(String path) {
        URL resourceURL = ProjectUtil.class.getClassLoader()
                                           .getResource(path);
        if (resourceURL == null) {
            throw new IllegalStateException("Missing resource with path " + path);
        }
        return resourceURL.getPath();
    }

    private void copyFromJar(String resourcePath, File destinationDir) throws IOException {
        int beginIndex = resourcePath.indexOf(FILE_PREFIX) + FILE_PREFIX.length() - 1;
        int endIndex = resourcePath.indexOf(JAR_PATH) + JAR_PATH.length() - 1;
        String jarPath = resourcePath.substring(beginIndex, endIndex);
        String resourcePathInJar = resourcePath.substring(resourcePath.lastIndexOf(JAR_PATH) + JAR_PATH.length() + 1);

        try (JarFile jarFile = new JarFile(jarPath)) {
            File destDir = new File(destinationDir, resourcePathInJar);
            if (destDir.exists() && !destDir.isDirectory()) {
                throw new IOException("The destination path is not a directory: " + destDir);
            }

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName()
                         .startsWith(resourcePathInJar)) {
                    String entryPath = entry.getName();
                    String relativeEntryPath = entryPath.substring(resourcePathInJar.length());

                    File entryDestination = new File(destinationDir, relativeEntryPath);

                    if (entry.isDirectory()) {
                        if (!entryDestination.exists()) {
                            entryDestination.mkdirs();
                        }
                    } else {
                        File parentDir = entryDestination.getParentFile();
                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs(); // Ensure parent directories exist
                        }

                        try (InputStream is = jarFile.getInputStream(entry); OutputStream os = new FileOutputStream(entryDestination)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }
            }
        }
    }

    private void replacePlaceholderInFile(File file, Map<String, String> placeholders) {
        try {
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String placeholder = entry.getKey();
                String replacement = entry.getValue();

                LOGGER.debug("Replacing [{}] with [{}] in file [{}]", placeholder, replacement, file);
                content = StringUtils.replace(content, placeholder, replacement);
            }

            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to replace placeholders [" + placeholders + "] in file " + file, ex);
        }
    }

    private String createUserWorkspaceFolderPath(String user, String projectName) {
        return repository.getRepositoryPath() + File.separator + IRepositoryStructure.KEYWORD_USERS + File.separator + user + File.separator
                + IRepositoryStructure.KEYWORD_WORKSPACE + File.separator + projectName;
    }

}
