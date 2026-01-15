/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.javascript.service;

import org.eclipse.dirigible.components.base.http.access.UserRequestVerifier;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.graalium.core.JavascriptSourceProvider;
import org.eclipse.dirigible.graalium.core.modules.DirigibleSourceProvider;
import org.eclipse.dirigible.repository.api.IRepository;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.eclipse.dirigible.graalium.core.graal.ValueTransformer.transformValue;

/**
 * The Class JavascriptHandler.
 */
public class JavascriptHandler {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(JavascriptHandler.class);

    /** The source provider. */
    private final JavascriptSourceProvider sourceProvider;

    /** The repository. */
    private final IRepository repository;

    /** The Constant systemRunner. */
    private static final ThreadLocal<DirigibleJavascriptCodeRunner> systemRunner = new ThreadLocal<>();

    /**
     * Instantiates a new javascript handler.
     *
     * @param repository the repository
     */
    public JavascriptHandler(IRepository repository) {
        this(repository, new DirigibleSourceProvider());
    }

    /**
     * Instantiates a new javascript handler.
     *
     * @param repository the repository
     * @param sourceProvider the source provider
     */
    public JavascriptHandler(IRepository repository, JavascriptSourceProvider sourceProvider) {
        this.repository = repository;
        this.sourceProvider = sourceProvider;
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public IRepository getRepository() {
        return repository;
    }

    public DirigibleJavascriptCodeRunner getSystemRunner() {
        DirigibleJavascriptCodeRunner localSystemRunner = systemRunner.get();
        if (localSystemRunner == null) {
            systemRunner.set(new DirigibleJavascriptCodeRunner(null, false));
        }
        return systemRunner.get();
    }

    /**
     * Handle callback.
     *
     * @param filePath the file path
     * @param parameters the parameters
     * @return the object
     */
    public Object handleCallback(String filePath, Map<Object, Object> parameters) {
        if (filePath == null) {
            throw new RuntimeException("Path to the file to be executed cannot be null");
        }
        Path path = Path.of(filePath);
        if (path.getNameCount() > 1) {
            return handleRequest(path.getRoot()
                                     .toString(),
                    path.subpath(1, path.getNameCount() - 1)
                        .toString(),
                    null, parameters, false, false);
        }
        throw new RuntimeException("Path to the file to be executed must contain a parent folder");
    }

    /**
     * Handle request.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param projectFilePathParam the project file path param
     * @param parameters the parameters
     * @param debug the debug
     * @return the object
     */
    public Object handleRequest(String projectName, String projectFilePath, String projectFilePathParam, Map<Object, Object> parameters,
            boolean debug, boolean keep) {
        try {
            if (UserRequestVerifier.isValid()) {
                UserRequestVerifier.getRequest()
                                   .setAttribute("dirigible-rest-resource-path", projectFilePathParam);
            }

            String sourceFilePath = Path.of(projectName, projectFilePath)
                                        .toString();
            String maybeJSCode = sourceProvider.getSource(sourceFilePath);
            if (maybeJSCode == null) {
                throw new IOException("JavaScript source code for project name '" + projectName + "' and file name '" + projectFilePath
                        + "' could not be found, consider publishing it.");
            }

            Path absoluteSourcePath = sourceProvider.getAbsoluteSourcePath(projectName, projectFilePath);
            if (keep) {
                return runCode(sourceFilePath, absoluteSourcePath, getSystemRunner());
            } else {
                try (DirigibleJavascriptCodeRunner runner = new DirigibleJavascriptCodeRunner(parameters, debug)) {
                    return runCode(sourceFilePath, absoluteSourcePath, runner);
                }
            }
        } catch (Throwable ex) {
            if (ex.getMessage() == null) {
                logger.error("Null object has been found", ex);
                return ex.getMessage();
            }
            if (ex.getMessage()
                  .contains("consider publish")) {
                logger.error(ex.getMessage());
                return ex.getMessage();
            }
            if (ex.getMessage() != null) {
                logger.error(ex.getMessage(), ex);
            }
            String errorMessage = String.format(
                    "Error on processing JavaScript service from project: [%s], path: [%s], project file path param [%s] with parameters: [%s] - %s",
                    projectName, projectFilePath, projectFilePathParam != null ? projectFilePathParam : "none",
                    parameters != null ? parameters : "none", ex.getMessage());
            throw new RuntimeException(errorMessage, ex);
        }
    }

    public Object runCode(String sourceFilePath, Path absoluteSourcePath, DirigibleJavascriptCodeRunner runner) {
        Source source = runner.prepareSource(absoluteSourcePath);
        runner.getGraalJSInterceptor()
              .onBeforeRun(sourceFilePath, absoluteSourcePath, source, runner.getCodeRunner()
                                                                             .getGraalContext());
        Value value = runner.run(source);
        runner.getGraalJSInterceptor()
              .onAfterRun(sourceFilePath, absoluteSourcePath, source, runner.getCodeRunner()
                                                                            .getGraalContext(),
                      value);
        return transformValue(value);
    }

    /**
     * Handle request.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @param projectFilePathParam the project file path param
     * @param parameters the parameters
     * @param debug the debug
     * @return the object
     */
    public Object handleRequest(String projectName, String projectFilePath, String projectFilePathParam, Map<Object, Object> parameters,
            boolean debug) {
        return handleRequest(projectName, projectFilePath, projectFilePathParam, parameters, debug, false);
    }

}
