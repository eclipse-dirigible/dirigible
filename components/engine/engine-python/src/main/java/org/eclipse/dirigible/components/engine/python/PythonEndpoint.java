/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.python;

import static org.eclipse.dirigible.graalium.core.graal.ValueTransformer.transformValue;
import java.io.File;
import java.nio.file.Path;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.python.graalium.GraalPyCodeRunner;
import org.eclipse.dirigible.graalium.core.modules.DirigibleSourceProvider;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Class PythonEndpoint.
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "py", BaseEndpoint.PREFIX_ENDPOINT_PUBLIC + "py"})
public class PythonEndpoint extends BaseEndpoint {

    /** The Constant PYTHON. */
    private static final String PYTHON = ".py/";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PythonEndpoint.class.getCanonicalName());

    /** The Constant HTTP_PATH_MATCHER. */
    private static final String HTTP_PATH_MATCHER = "/{projectName}/{*projectFilePath}";

    /** The repository. */
    private final IRepository repository;

    /**
     * Instantiates a new python endpoint.
     *
     * @param repository the repository
     */
    @Autowired
    public PythonEndpoint(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets the.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @GetMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> get(@PathVariable("projectName") String projectName, @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executePython(projectName, projectFilePath, params, null);
    }

    /**
     * Post.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @PostMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> post(@PathVariable("projectName") String projectName, @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executePython(projectName, projectFilePath, params, null);
    }

    /**
     * Post file.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @param file the file
     * @return the response entity
     */
    @PostMapping(value = HTTP_PATH_MATCHER, consumes = "multipart/form-data")
    public ResponseEntity<?> postFile(@PathVariable("projectName") String projectName,
            @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params,
            @Validated @RequestParam("file") MultipartFile[] file) {
        return executePython(projectName, projectFilePath, params, file);
    }

    /**
     * Put.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @PutMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> put(@PathVariable("projectName") String projectName, @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executePython(projectName, projectFilePath, params, null);
    }

    /**
     * Put file.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @param file the file
     * @return the response entity
     */
    @PutMapping(value = HTTP_PATH_MATCHER, consumes = "multipart/form-data")
    public ResponseEntity<?> putFile(@PathVariable("projectName") String projectName,
            @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params,
            @Validated @RequestParam("file") MultipartFile file) {
        return executePython(projectName, projectFilePath, params, new MultipartFile[] {file});
    }

    /**
     * Patch.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @PatchMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> patch(@PathVariable("projectName") String projectName, @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executePython(projectName, projectFilePath, params, null);
    }

    /**
     * Delete.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @DeleteMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> delete(@PathVariable("projectName") String projectName,
            @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executePython(projectName, projectFilePath, params, null);
    }

    /**
     * Extract project file path.
     *
     * @param projectFilePath the project file path
     * @return the string
     */
    protected String extractProjectFilePath(String projectFilePath) {
        if (projectFilePath.indexOf(PYTHON) > 0) {
            projectFilePath = projectFilePath.substring(0, projectFilePath.indexOf(PYTHON) + PYTHON.length() + 1);
        }
        return projectFilePath;
    }

    /**
     * Extract path param.
     *
     * @param projectFilePath the project file path
     * @return the string
     */
    protected String extractPathParam(String projectFilePath) {
        String projectFilePathParam = "";
        if (projectFilePath.indexOf(PYTHON) > 0) {
            projectFilePathParam = projectFilePath.substring(projectFilePath.indexOf(PYTHON) + PYTHON.length() + 1);
        }
        return projectFilePathParam;
    }

    /**
     * Execute python.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @param files the files
     * @return the response entity
     */
    private ResponseEntity<?> executePython(String projectName, String projectFilePath, MultiValueMap<String, String> params,
            MultipartFile[] files) {
        String projectFilePathParam = extractPathParam(projectFilePath);
        projectFilePath = extractProjectFilePath(projectFilePath);
        return executePython(projectName, projectFilePath, projectFilePathParam, params, files);
    }

    /**
     * Execute python.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param projectFilePathParam the project file path param
     * @param params the params
     * @param files the files
     * @return the response entity
     */
    protected ResponseEntity<?> executePython(String projectName, String projectFilePath, String projectFilePathParam,
            MultiValueMap<String, String> params, MultipartFile[] files) {
        try {
            if (isNotValid(projectName) || isNotValid(projectFilePath)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            Object result = handleRequest(projectName, normalizePath(projectFilePath), normalizePath(projectFilePathParam),
                    params.get("debug") != null);
            return ResponseEntity.ok(result);
        } catch (RepositoryNotFoundException e) {
            String message = e.getMessage() + ". Try to publish the service before execution.";
            throw new RepositoryNotFoundException(message, e);
        }
    }

    /**
     * Handle request.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param projectFilePathParam the project file path param
     * @param debug the debug
     * @return the object
     */
    private Object handleRequest(String projectName, String projectFilePath, String projectFilePathParam, boolean debug) {
        Path absoluteSourcePath = getAbsolutePathIfValidProjectFile(projectName, projectFilePath);
        Path workingDir = getDirigibleWorkingDirectory();
        Path projectDir = workingDir.resolve(projectName);
        Path pythonMods = getDirigiblePythonModulesDirectory();

        try (var runner = new GraalPyCodeRunner(workingDir, projectDir, pythonMods, debug)) {
            Source source = runner.prepareSource(absoluteSourcePath);
            Value value = runner.run(source);
            return transformValue(value);
        }
    }

    /**
     * Gets the absolute path if valid project file.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @return the absolute path if valid project file
     */
    private static Path getAbsolutePathIfValidProjectFile(String projectName, String projectFilePath) {
        var sourceProvider = new DirigibleSourceProvider();
        String sourceFilePath = Path.of(projectName, projectFilePath)
                                    .toString();
        String maybePythonCode = sourceProvider.getSource(sourceFilePath);
        if (maybePythonCode == null) {
            throw new RuntimeException("Python source code for project name '" + projectName + "' and file name '" + projectFilePath
                    + "' could not be found, consider publishing it.");
        }

        return sourceProvider.getAbsoluteSourcePath(projectName, projectFilePath);
    }

    /**
     * Checks if is not valid.
     *
     * @param inputPath the input path
     * @return true, if is not valid
     */
    private boolean isNotValid(String inputPath) {
        String registryPath = getDirigibleWorkingDirectory().toString();
        String normalizedInputPath = Path.of(inputPath)
                                         .normalize()
                                         .toString();
        File file = new File(registryPath, normalizedInputPath);
        try {
            return !file.toPath()
                        .normalize()
                        .startsWith(registryPath);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Gets the dirigible working directory.
     *
     * @return the dirigible working directory
     */
    private Path getDirigibleWorkingDirectory() {
        String publicRegistryPath = repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        return Path.of(publicRegistryPath);
    }

    /**
     * Gets the dirigible python modules directory.
     *
     * @return the dirigible python modules directory
     */
    private Path getDirigiblePythonModulesDirectory() {
        String publicRegistryPath = repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        return Path.of(publicRegistryPath)
                   .resolve("python-modules");
    }

    /**
     * Normalize path.
     *
     * @param path the path
     * @return the string
     */
    private String normalizePath(String path) {
        if (path != null) {
            if (path.startsWith(IRepository.SEPARATOR)) {
                return path.substring(1);
            }
        }
        return path;
    }
}
