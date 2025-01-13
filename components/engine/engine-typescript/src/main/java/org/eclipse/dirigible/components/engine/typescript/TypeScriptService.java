/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.typescript;

import org.apache.commons.io.FileUtils;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The Class TypeScriptService.
 */
@Component
public class TypeScriptService {

    /** The Constant TS_EXT. */
    private static final String TS_EXT = ".ts";

    /** The Constant DTS_EXT. */
    private static final String DTS_EXT = ".d.ts";

    /** The repository. */
    private final IRepository repository;

    /**
     * Instantiates a new type script service.
     *
     * @param repository the repository
     */
    @Autowired
    public TypeScriptService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Checks if is type script file.
     *
     * @param path the path
     * @return true, if is type script file
     */
    public boolean isTypeScriptFile(String path) {
        return path.endsWith(TS_EXT);
    }

    /**
     * Should compile type script.
     *
     * @param projectName the project name
     * @param entryPath the entry path
     * @return true, if successful
     */
    public boolean shouldCompileTypeScript(String projectName, String entryPath) {
        if (entryPath != null && !entryPath.equals("")) {
            return isTSButNotDTS(entryPath);
        }

        var projectDir = getProjectDirFile(projectName);
        return shouldCompileTypeScript(projectDir);
    }

    /**
     * Should compile type script.
     *
     * @param dir the dir
     * @return true, if successful
     */
    public boolean shouldCompileTypeScript(File dir) {
        if (shouldIgnoreProject(dir.getName())) {
            return false;
        }

        return dir.exists() && !getTypeScriptFilesInDir(dir).isEmpty();
    }

    /**
     * Should ignore project.
     *
     * @param projectName the project name
     * @return true, if successful
     */
    private static boolean shouldIgnoreProject(String projectName) {
        return "dev-tools".equals(projectName) || "modules".equals(projectName);
    }

    /**
     * Checks if is TS but not DTS.
     *
     * @param entryPath the entry path
     * @return true, if is TS but not DTS
     */
    private static boolean isTSButNotDTS(String entryPath) {
        return entryPath.endsWith(TS_EXT) && !entryPath.endsWith(DTS_EXT);
    }

    /**
     * Compile type script.
     *
     * @param projectName the project name
     * @param entryPath the entry path
     */
    public void compileTypeScript(String projectName, String entryPath) {
        var projectDir = getProjectDirFile(projectName);
        File outDir;
        Collection<File> tsFiles;

        if (entryPath != null && !entryPath.equals("")) {
            var tsFilePathString = new RepositoryPath(IRepositoryStructure.PATH_REGISTRY_PUBLIC, projectName, entryPath).toString();
            var tsFilePath = new File(repository.getInternalResourcePath(tsFilePathString)).toPath();
            outDir = tsFilePath.getParent()
                               .toFile();
            tsFiles = Collections.singletonList(tsFilePath.toFile());
        } else {
            tsFiles = getTypeScriptFilesInDir(projectDir);
            outDir = projectDir;
        }

        esbuild(projectDir, outDir, tsFiles);
    }

    /**
     * Esbuild.
     *
     * @param projectDir the project dir
     * @param outDir the out dir
     * @param filesToCompile the files to compile
     */
    private static void esbuild(File projectDir, File outDir, Collection<File> filesToCompile) {
        var esbuildCommand = new ArrayList<String>();
        esbuildCommand.add("esbuild");
        esbuildCommand.addAll(filesToCompile.stream()
                                            .map(Object::toString)
                                            .toList());
        esbuildCommand.add("--outdir=" + outDir);
        esbuildCommand.add("--out-extension:.js=.mjs");
        esbuildCommand.add("--sourcemap=inline");

        var processBuilder = new ProcessBuilder(esbuildCommand).directory(projectDir)
                                                               .redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            int statusCode = process.waitFor();
            if (statusCode != 0) {
                throw new RuntimeException("esbuild error: finished with: " + statusCode);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException("Could not run esbuild", e);
        }
    }

    /**
     * Gets the project dir file.
     *
     * @param projectName the project name
     * @return the project dir file
     */
    private File getProjectDirFile(String projectName) {
        var registryRelativeProjectPath = new RepositoryPath(IRepositoryStructure.PATH_REGISTRY_PUBLIC, projectName).toString();
        return new File(repository.getInternalResourcePath(registryRelativeProjectPath));
    }

    /**
     * Gets the type script files in dir.
     *
     * @param projectDir the project dir
     * @return the type script files in dir
     */
    private Collection<File> getTypeScriptFilesInDir(File projectDir) {
        return FileUtils.listFiles(projectDir, new String[] {"ts"}, true)
                        .stream()
                        .filter(x -> isTSButNotDTS(x.toString()))
                        .collect(Collectors.toList());
    }
}
