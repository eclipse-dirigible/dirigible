/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.javascript.endpoint;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.graalium.core.JavascriptSourceProvider;
import org.eclipse.dirigible.graalium.core.modules.DirigibleSourceProvider;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The Class JavascriptEndpoint.
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "js", BaseEndpoint.PREFIX_ENDPOINT_PUBLIC + "js"})
public class JavascriptEndpoint extends BaseEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(JavascriptEndpoint.class.getCanonicalName());

    /** The Constant HTTP_PATH_MATCHER. */
    private static final String HTTP_PATH_MATCHER = "/{projectName}/{*projectFilePath}";
    /** The Constant CJS. */
    private static final String CJS = ".cjs/";
    /** The Constant MJS. */
    private static final String MJS = ".mjs/";
    /** The Constant JS. */
    private static final String JS = ".js/";
    /** The javascript service. */
    private final JavascriptService javascriptService;
    /** The repository. */
    private final IRepository repository;
    /** The source provider. */
    private final JavascriptSourceProvider sourceProvider = new DirigibleSourceProvider();

    /**
     * Instantiates a new javascript endpoint.
     *
     * @param javascriptService the javascript service
     * @param repository the repository
     */
    @Autowired
    public JavascriptEndpoint(JavascriptService javascriptService, IRepository repository) {
        this.javascriptService = javascriptService;
        this.repository = repository;
    }

    /**
     * The Dts.
     */
    record Dts(String content, String moduleName, String filePath) {

        /**
         * From dts path.
         *
         * @param dtsDirRoot the dts dir root
         * @param dtsPath the dts path
         * @return the dts
         */
        static Dts fromDtsPath(Path dtsDirRoot, Path dtsPath) {
            String content = readAllText(dtsPath);
            Path relativePath = dtsDirRoot.relativize(dtsPath);
            String filePath = "file:///node_modules/sdk/" + relativePath;
            String moduleName = ("sdk/" + relativePath).replace("index.d.ts", "")
                                                       .replace(".d.ts", "");
            return new Dts(content, moduleName, filePath);
        }

        /**
         * Read all text.
         *
         * @param path the path
         * @return the string
         */
        private static String readAllText(Path path) {
            try {
                byte[] bytes = Files.readAllBytes(path);
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets the dts.
     *
     * @return the dts
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @GetMapping("/all-dts")
    public List<Dts> getDTS() throws IOException {
        Path dtsRoot = sourceProvider.getAbsoluteProjectPath("modules")
                                     .resolve("dist")
                                     .resolve("dts");

        if (dtsRoot.toFile()
                   .exists()) {
            try (var dtsTree = Files.walk(dtsRoot)) {
                List<Dts> allDtsFilesContent = dtsTree.filter(Files::isRegularFile)
                                                      .map(dts -> Dts.fromDtsPath(dtsRoot, dts))
                                                      .toList();
                return allDtsFilesContent;
            }
        } else {
            logger.warn("DTS files not avalilable or still not loaded");
            return new ArrayList<Dts>();
        }
    }

    /**
     * Gets the.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response
     */
    @WithSpan
    @GetMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> get(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executeJavaScript(projectName, projectFilePath, params, null);
    }

    /**
     * Execute java script.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @param files the files
     * @return the response
     */
    private ResponseEntity<?> executeJavaScript(String projectName, String projectFilePath, MultiValueMap<String, String> params,
            MultipartFile[] files) {
        String projectFilePathParam = extractPathParam(projectFilePath);
        projectFilePath = extractProjectFilePath(projectFilePath);
        return executeJavaScript(projectName, projectFilePath, projectFilePathParam, params, files);
    }

    /**
     * Extract project file path.
     *
     * @param projectFilePath the project file path
     * @return the string
     */
    protected String extractProjectFilePath(String projectFilePath) {
        if (projectFilePath.indexOf(JS) > 0) {
            projectFilePath = projectFilePath.substring(0, projectFilePath.indexOf(JS) + 3);
        } else if (projectFilePath.indexOf(MJS) > 0) {
            projectFilePath = projectFilePath.substring(0, projectFilePath.indexOf(MJS) + 4);
        } else if (projectFilePath.indexOf(CJS) > 0) {
            projectFilePath = projectFilePath.substring(0, projectFilePath.indexOf(CJS) + 4);
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
        if (projectFilePath.indexOf(JS) > 0) {
            projectFilePathParam = projectFilePath.substring(projectFilePath.indexOf(JS) + 3);
        } else if (projectFilePath.indexOf(MJS) > 0) {
            projectFilePathParam = projectFilePath.substring(projectFilePath.indexOf(MJS) + 4);
        } else if (projectFilePath.indexOf(CJS) > 0) {
            projectFilePathParam = projectFilePath.substring(projectFilePath.indexOf(CJS) + 4);
        }
        return projectFilePathParam;
    }

    /**
     * Execute java script.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param projectFilePathParam the project file path param
     * @param params the params
     * @param files the files
     * @return the response
     */
    protected ResponseEntity<?> executeJavaScript(String projectName, String projectFilePath, String projectFilePathParam,
            MultiValueMap<String, String> params, MultipartFile[] files) {
        try {
            if (!isValid(projectName) || !isValid(projectFilePath)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            Map<Object, Object> context = new HashMap<Object, Object>();
            if (params != null) {
                context.put("params", params);
            }
            List<MultipartFile> filesList = new ArrayList<MultipartFile>();
            if (files != null) {
                Collections.addAll(filesList, files);
                context.put("files", filesList);
            }

            Object result =
                    getJavascriptService().handleRequest(projectName, normalizePath(projectFilePath), normalizePath(projectFilePathParam),
                            context, ((MultiValueMap<String, String>) context.get("params")).get("debug") != null, false);
            return ResponseEntity.ok(result instanceof Value ? null : result);
        } catch (RepositoryNotFoundException e) {
            String message = e.getMessage() + ". Try to publish the service before execution.";
            throw new RepositoryNotFoundException(message, e);
        }
    }

    /**
     * Gets the javascript handler.
     *
     * @return the javascript handler
     */
    protected JavascriptService getJavascriptService() {
        return javascriptService;
    }

    /**
     * Checks if is valid.
     *
     * @param inputPath the input path
     * @return true, if is valid
     */
    public boolean isValid(String inputPath) {
        String registryPath = getDirigibleWorkingDirectory().toString();
        String normalizedInputPath = java.nio.file.Path.of(inputPath)
                                                       .normalize()
                                                       .toString();
        File file = new File(registryPath, normalizedInputPath);
        try {
            return file.getCanonicalPath()
                       .startsWith(registryPath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Gets the dirigible working directory.
     *
     * @return the dirigible working directory
     */
    protected java.nio.file.Path getDirigibleWorkingDirectory() {
        String publicRegistryPath = repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        return java.nio.file.Path.of(publicRegistryPath);
    }

    /**
     * Normalize path.
     *
     * @param path the path
     * @return the string
     */
    protected String normalizePath(String path) {
        if (path != null) {
            if (path.startsWith(IRepository.SEPARATOR)) {
                return path.substring(1);
            }
        }
        return path;
    }

    /**
     * Post.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response
     */
    @WithSpan
    @PostMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> post(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executeJavaScript(projectName, projectFilePath, params, null);
    }

    /**
     * Post.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @param file the file
     * @return the response
     */
    @WithSpan
    @PostMapping(value = HTTP_PATH_MATCHER, consumes = "multipart/form-data")
    public ResponseEntity<?> postFile(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params,
            @Validated @RequestParam("file") MultipartFile[] file) {
        return executeJavaScript(projectName, projectFilePath, params, file);
    }

    /**
     * Put.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response
     */
    @WithSpan
    @PutMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> put(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executeJavaScript(projectName, projectFilePath, params, null);
    }

    /**
     * Put.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @param file the file
     * @return the response
     */
    @WithSpan
    @PutMapping(value = HTTP_PATH_MATCHER, consumes = "multipart/form-data")
    public ResponseEntity<?> putFile(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params,
            @Validated @RequestParam("file") MultipartFile file) {
        return executeJavaScript(projectName, projectFilePath, params, new MultipartFile[] {file});
    }

    /**
     * Patch.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response
     */
    @PatchMapping(HTTP_PATH_MATCHER)
    @WithSpan("java_script_endpoint_patch")
    public ResponseEntity<?> patch(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executeJavaScript(projectName, projectFilePath, params, null);
    }

    /**
     * Delete.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response
     */
    @WithSpan
    @DeleteMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> delete(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return executeJavaScript(projectName, projectFilePath, params, null);
    }
}
