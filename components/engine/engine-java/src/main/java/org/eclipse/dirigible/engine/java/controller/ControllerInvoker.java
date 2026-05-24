/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Invokes a matched controller route: enforces {@code @Roles}, binds parameters from the HTTP
 * request, calls the underlying method via reflection, and writes the return value back as the
 * response body.
 */
@Component
public class ControllerInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerInvoker.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public ControllerInvoker(ObjectProvider<ObjectMapper> objectMapperProvider) {
        // Prefer the Spring-managed primary ObjectMapper (so users get the platform's Jackson
        // configuration); fall back to a fresh one if no bean is registered — e.g. in minimal test
        // contexts where JacksonAutoConfiguration didn't fire.
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
    }

    /** Test-friendly constructor — bypasses Spring's ObjectProvider. */
    ControllerInvoker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Resolve roles + bind params + invoke + serialise the result. */
    public void invoke(RouteMatch match, HttpServletRequest request, HttpServletResponse response) {
        Route route = match.route();
        checkRoles(route.roles(), request);

        Object[] args;
        try {
            args = bindParameters(route, match.pathParameters(), request, response);
        } catch (BindingException e) {
            // Spring Boot 4 strips ResponseStatusException.getReason() from the JSON response body, so
            // without this log line the caller sees a bare 400 with no clue what failed (e.g. a
            // mistyped JSON field that Jackson couldn't coerce into the target Java type).
            LOGGER.warn("Bad request binding for [{}#{}]: {}", match.entry()
                                                                    .fqn(),
                    route.method()
                         .getName(),
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

        Method method = route.method();
        method.setAccessible(true);

        Object returnValue;
        try {
            returnValue = method.invoke(match.entry()
                                             .instance(),
                    args);
        } catch (IllegalAccessException e) {
            LOGGER.error("Cannot invoke controller method [{}#{}]: {}", match.entry()
                                                                             .fqn(),
                    method.getName(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ResponseStatusException rse) {
                throw rse;
            }
            LOGGER.error("Controller [{}#{}] threw: {}", match.entry()
                                                              .fqn(),
                    method.getName(), cause.getMessage(), cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, cause.getMessage(), cause);
        }

        writeResponse(response, method, returnValue);
    }

    /**
     * Mirrors {@code org.eclipse.dirigible.components.api.security.UserFacade#isInRole} so that
     * controller authorisation behaves the same as the TypeScript {@code @Roles} decorator without
     * pulling the {@code api-security} module (which transitively brings in {@code engine-javascript})
     * into this engine.
     */
    private void checkRoles(List<String> roles, HttpServletRequest request) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        if (Configuration.isAnonymousModeEnabled() || Configuration.isAnonymousUserEnabled()) {
            return;
        }
        if (request.isUserInRole(Roles.RoleNames.DEVELOPER) || request.isUserInRole(Roles.RoleNames.ADMINISTRATOR)) {
            return;
        }
        for (String role : roles) {
            if (request.isUserInRole(role)) {
                return;
            }
        }
        String current = request.getRemoteUser();
        if (current == null) {
            current = "anonymous";
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Current user [" + current + "] is not in role(s) " + roles + " required for this endpoint");
    }

    private Object[] bindParameters(Route route, Map<String, String> pathParameters, HttpServletRequest request,
            HttpServletResponse response) throws BindingException {
        List<ParamBinding> bindings = route.paramBindings();
        Object[] args = new Object[bindings.size()];
        for (int i = 0; i < bindings.size(); i++) {
            ParamBinding b = bindings.get(i);
            args[i] = bind(b, pathParameters, request, response);
        }
        return args;
    }

    private Object bind(ParamBinding binding, Map<String, String> pathParameters, HttpServletRequest request, HttpServletResponse response)
            throws BindingException {
        switch (binding.kind()) {
            case BODY:
                try {
                    return objectMapper.readValue(request.getInputStream(), objectMapper.constructType(binding.genericType()));
                } catch (IOException e) {
                    throw new BindingException("Failed to parse request body as " + binding.targetType()
                                                                                           .getSimpleName()
                            + ": " + e.getMessage(), e);
                }
            case PATH: {
                String raw = pathParameters.get(binding.name());
                if (raw == null) {
                    throw new BindingException("Missing path parameter [" + binding.name() + "]");
                }
                try {
                    return TypeCoercer.coerce(raw, binding.targetType());
                } catch (IllegalArgumentException e) {
                    throw new BindingException(
                            "Invalid value for path parameter [" + binding.name() + "]: " + raw + " (" + e.getMessage() + ")", e);
                }
            }
            case QUERY: {
                String raw = request.getParameter(binding.name());
                try {
                    return TypeCoercer.coerce(raw, binding.targetType());
                } catch (IllegalArgumentException e) {
                    throw new BindingException(
                            "Invalid value for query parameter [" + binding.name() + "]: " + raw + " (" + e.getMessage() + ")", e);
                }
            }
            case CTX_REQUEST:
                return request;
            case CTX_RESPONSE:
                return response;
            case CTX_PARAMS:
                Map<String, String> merged = new HashMap<>(pathParameters);
                request.getParameterMap()
                       .forEach((k, v) -> {
                           if (v != null && v.length > 0) {
                               merged.put(k, v[0]);
                           }
                       });
                return merged;
            default:
                throw new BindingException("Unknown parameter binding kind: " + binding.kind());
        }
    }

    private void writeResponse(HttpServletResponse response, Method method, Object returnValue) {
        if (method.getReturnType() == void.class) {
            // The method handled the response itself (or chose to write nothing). If nothing has
            // been committed yet, leave the default 200 OK + empty body in place.
            return;
        }
        if (response.isCommitted()) {
            return;
        }
        if (returnValue == null) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }
        try {
            if (returnValue instanceof CharSequence cs) {
                response.setContentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8");
                byte[] bytes = cs.toString()
                                 .getBytes(StandardCharsets.UTF_8);
                response.setContentLength(bytes.length);
                response.getOutputStream()
                        .write(bytes);
            } else {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                objectMapper.writeValue(response.getOutputStream(), returnValue);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialise response: " + e.getMessage(), e);
        }
    }

    /** Internal binding failure — surfaces as HTTP 400. */
    private static final class BindingException extends Exception {
        private static final long serialVersionUID = 1L;

        BindingException(String message) {
            super(message);
        }

        BindingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
