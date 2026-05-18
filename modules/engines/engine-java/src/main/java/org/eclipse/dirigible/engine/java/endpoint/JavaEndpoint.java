/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.endpoint;

import java.util.Optional;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.eclipse.dirigible.engine.java.runtime.JavaClassRegistry;
import org.eclipse.dirigible.engine.java.runtime.LoadedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Dispatches incoming HTTP requests to user-supplied {@link JavaHandler} implementations.
 *
 * <p>
 * URL shape: {@code /services/java/<project>/<package-path>/<ClassName>} (secured) and the
 * unauthenticated counterpart {@code /public/java/...}. The path after {@code <project>/} is
 * converted to a fully-qualified class name by replacing {@code /} with {@code .} — exactly the
 * inverse of the convention {@code JavaSynchronizer} uses to derive the FQN from the source file's
 * location.
 *
 * <p>
 * A fresh instance of the handler is constructed on each request via
 * {@link Class#getDeclaredConstructor()}. The thread-context classloader is switched to the
 * handler's own loader so utilities that rely on TCCL (Jackson, JPA, logging frameworks) can
 * resolve user types correctly, then restored after the call returns.
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "java", BaseEndpoint.PREFIX_ENDPOINT_PUBLIC + "java"})
public class JavaEndpoint extends BaseEndpoint {

    /** Matches {@code /<project>/<one-or-more-path-segments>} — last segment is the class name. */
    private static final String PATH_MATCHER = "/{project}/{*classPath}";

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaEndpoint.class);

    private final JavaClassRegistry registry;

    @Autowired
    public JavaEndpoint(JavaClassRegistry registry) {
        this.registry = registry;
    }

    @GetMapping(PATH_MATCHER)
    public void get(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(project, classPath, request, response);
    }

    @PostMapping(PATH_MATCHER)
    public void post(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(project, classPath, request, response);
    }

    @PutMapping(PATH_MATCHER)
    public void put(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(project, classPath, request, response);
    }

    @PatchMapping(PATH_MATCHER)
    public void patch(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(project, classPath, request, response);
    }

    @DeleteMapping(PATH_MATCHER)
    public void delete(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(project, classPath, request, response);
    }

    private void dispatch(String project, String classPath, HttpServletRequest request, HttpServletResponse response) {
        String classFqn = toFqn(classPath);
        Optional<LoadedHandler> maybeHandler = registry.find(project, classFqn);
        if (maybeHandler.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Java handler registered for [" + project + "/" + classFqn + "]");
        }
        LoadedHandler loaded = maybeHandler.get();

        ClassLoader previousTccl = Thread.currentThread()
                                         .getContextClassLoader();
        try {
            Thread.currentThread()
                  .setContextClassLoader(loaded.getLoader());
            JavaHandler handler = loaded.newInstance();
            handler.handle(request, response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Java handler [{}/{}] failed: {}", project, classFqn, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Java handler [" + classFqn + "] failed: " + e.getMessage(),
                    e);
        } finally {
            Thread.currentThread()
                  .setContextClassLoader(previousTccl);
        }
    }

    private static String toFqn(String classPath) {
        String trimmed = classPath.startsWith("/") ? classPath.substring(1) : classPath;
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class path is empty");
        }
        return trimmed.replace('/', '.');
    }

}
