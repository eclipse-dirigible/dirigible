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
import org.eclipse.dirigible.engine.java.controller.ControllerInvoker;
import org.eclipse.dirigible.engine.java.controller.ControllerRouter;
import org.eclipse.dirigible.engine.java.controller.RouteMatch;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.eclipse.dirigible.engine.java.runtime.JavaClassRegistry;
import org.eclipse.dirigible.engine.java.runtime.LoadedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
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
 * Dispatches incoming HTTP requests to user-supplied Java classes.
 *
 * <p>
 * URL shape: {@code /services/java/<project>/<package-path>/<ClassName>[/<route-suffix>]} (secured)
 * and the unauthenticated counterpart {@code /public/java/...}. Two dispatch styles are supported,
 * evaluated in order:
 * <ol>
 * <li>Controllers — classes annotated with
 * {@link org.eclipse.dirigible.sdk.http.Controller @Controller}. Method-level
 * {@code @Get / @Post / @Put / @Patch / @Delete} annotations supply the route suffix. Parameter
 * binding and role checks are handled by {@link ControllerInvoker}.</li>
 * <li>Handlers — classes implementing {@link JavaHandler}. The path after {@code <project>/} is
 * converted to a fully-qualified class name by replacing {@code /} with {@code .}, the registry is
 * consulted, and a fresh instance's {@code handle(req, resp)} is invoked.</li>
 * </ol>
 *
 * <p>
 * For handler dispatch, the thread-context classloader is switched to the handler's own loader so
 * utilities that rely on TCCL (Jackson, JPA, logging frameworks) can resolve user types correctly,
 * then restored after the call returns. Controllers reuse their long-lived instance and do not swap
 * TCCL on each request — instead the {@link ControllerInvoker} relies on Spring's
 * {@code ObjectMapper}.
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "java", BaseEndpoint.PREFIX_ENDPOINT_PUBLIC + "java"})
public class JavaEndpoint extends BaseEndpoint {

    /** Matches {@code /<project>/<one-or-more-path-segments>} — last segment is the class name. */
    private static final String PATH_MATCHER = "/{project}/{*classPath}";

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaEndpoint.class);

    private final JavaClassRegistry registry;

    private final ControllerRouter controllerRouter;

    private final ControllerInvoker controllerInvoker;

    @Autowired
    public JavaEndpoint(JavaClassRegistry registry, ControllerRouter controllerRouter, ControllerInvoker controllerInvoker) {
        this.registry = registry;
        this.controllerRouter = controllerRouter;
        this.controllerInvoker = controllerInvoker;
    }

    @GetMapping(PATH_MATCHER)
    public void get(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(HttpMethod.GET, project, classPath, request, response);
    }

    @PostMapping(PATH_MATCHER)
    public void post(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(HttpMethod.POST, project, classPath, request, response);
    }

    @PutMapping(PATH_MATCHER)
    public void put(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(HttpMethod.PUT, project, classPath, request, response);
    }

    @PatchMapping(PATH_MATCHER)
    public void patch(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(HttpMethod.PATCH, project, classPath, request, response);
    }

    @DeleteMapping(PATH_MATCHER)
    public void delete(@PathVariable("project") String project, @PathVariable("classPath") String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        dispatch(HttpMethod.DELETE, project, classPath, request, response);
    }

    private void dispatch(HttpMethod httpMethod, String project, String classPath, HttpServletRequest request,
            HttpServletResponse response) {
        Optional<RouteMatch> match = controllerRouter.match(httpMethod, project, classPath);
        if (match.isPresent()) {
            controllerInvoker.invoke(match.get(), request, response);
            return;
        }
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
            JavaHandler handler = loaded.instance();
            handler.handle(request, response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Java handler [{}/{}] failed: {}", sanitizeForLog(project), sanitizeForLog(classFqn),
                    sanitizeForLog(e.getMessage()), e);
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

    /**
     * Strip CR/LF (and stray control characters) from values that originate in user-controlled URL
     * segments before they reach the log. Prevents log-injection / log-forging where an attacker could
     * craft a request whose path embeds newlines to forge log entries.
     */
    private static String sanitizeForLog(String value) {
        if (value == null) {
            return "null";
        }
        return value.replaceAll("[\\r\\n\\t]", "_");
    }

}
