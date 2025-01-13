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

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.javascript.endpoint.JavascriptEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * The Class TypeScriptEndpoint.
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "ts", BaseEndpoint.PREFIX_ENDPOINT_PUBLIC + "ts"})
public class TypeScriptEndpoint extends BaseEndpoint {

    /** The Constant HTTP_PATH_MATCHER. */
    private static final String HTTP_PATH_MATCHER = "/{projectName}/{*projectFilePath}";

    /** The javascript endpoint. */
    private final JavascriptEndpoint javascriptEndpoint;

    /**
     * Instantiates a new type script endpoint.
     *
     * @param javascriptEndpoint the javascript endpoint
     */
    @Autowired
    public TypeScriptEndpoint(JavascriptEndpoint javascriptEndpoint) {
        this.javascriptEndpoint = javascriptEndpoint;
    }

    /**
     * Gets the.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @WithSpan
    @GetMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> get(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return javascriptEndpoint.get(projectName, replaceTSWithJSExtension(projectFilePath), params);
    }

    /**
     * Replace TS with MJS extension.
     *
     * @param projectFilePath the project file path
     * @return the string
     */
    private String replaceTSWithJSExtension(String projectFilePath) {
        int indexOfExtensionStart = projectFilePath.lastIndexOf(".ts");
        if (indexOfExtensionStart == -1) {
            throw new RuntimeException("Could not find .ts extension");
        }

        String projectFilePathWithoutExtension = projectFilePath.substring(0, indexOfExtensionStart);
        String maybePathParameters = projectFilePath.substring(indexOfExtensionStart)
                                                    .replace(".ts", ""); // for decorators and rs api
        return projectFilePathWithoutExtension + ".js" + maybePathParameters;
    }

    /**
     * Post.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @WithSpan
    @PostMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> post(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return javascriptEndpoint.post(projectName, replaceTSWithJSExtension(projectFilePath), params);
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
    @WithSpan
    @PostMapping(value = HTTP_PATH_MATCHER, consumes = "multipart/form-data")
    public ResponseEntity<?> postFile(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params,
            @Validated @RequestParam("file") MultipartFile[] file) {
        return javascriptEndpoint.postFile(projectName, replaceTSWithJSExtension(projectFilePath), params, file);
    }

    /**
     * Put.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @WithSpan
    @PutMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> put(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return javascriptEndpoint.put(projectName, replaceTSWithJSExtension(projectFilePath), params);
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
    @WithSpan
    @PutMapping(value = HTTP_PATH_MATCHER, consumes = "multipart/form-data")
    public ResponseEntity<?> putFile(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params,
            @Validated @RequestParam("file") MultipartFile file) {
        return javascriptEndpoint.putFile(projectName, replaceTSWithJSExtension(projectFilePath), params, file);
    }

    /**
     * Patch.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @WithSpan
    @PatchMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> patch(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("projectfile.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return javascriptEndpoint.patch(projectName, replaceTSWithJSExtension(projectFilePath), params);
    }

    /**
     * Delete.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param params the params
     * @return the response entity
     */
    @WithSpan
    @DeleteMapping(HTTP_PATH_MATCHER)
    public ResponseEntity<?> delete(@SpanAttribute("project.name") @PathVariable("projectName") String projectName,
            @SpanAttribute("project.file.path") @PathVariable("projectFilePath") String projectFilePath,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) {
        return javascriptEndpoint.delete(projectName, replaceTSWithJSExtension(projectFilePath), params);
    }
}
