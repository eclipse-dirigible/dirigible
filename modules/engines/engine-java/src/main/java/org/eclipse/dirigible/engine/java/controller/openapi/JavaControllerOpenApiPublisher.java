/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.controller.openapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.dirigible.components.openapi.domain.OpenAPI;
import org.eclipse.dirigible.components.openapi.service.OpenAPIService;
import org.eclipse.dirigible.engine.java.annotations.Documentation;
import org.eclipse.dirigible.engine.java.controller.ControllerEntry;
import org.eclipse.dirigible.engine.java.controller.ParamBinding;
import org.eclipse.dirigible.engine.java.controller.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reflects a {@link ControllerEntry} into a minimal OpenAPI 3.0 fragment and persists it through
 * {@link OpenAPIService} so the global aggregator at {@code /services/openapi} picks it up
 * automatically.
 *
 * <p>
 * The schema content is deliberately conservative: bodies and return types are described as
 * {@code object} / {@code array} / scalar with no property-level introspection. We can enrich the
 * generator in a follow-up without changing the publish/retract contract.
 */
@Component
public class JavaControllerOpenApiPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaControllerOpenApiPublisher.class);

    private static final String LOCATION_PREFIX = "java-controller://";

    private static final String ENDPOINT_PREFIX = "/services/java/";

    private final OpenAPIService openAPIService;

    private final ObjectMapper objectMapper;

    @Autowired
    public JavaControllerOpenApiPublisher(OpenAPIService openAPIService, ObjectMapper objectMapper) {
        this.openAPIService = openAPIService;
        this.objectMapper = objectMapper;
    }

    /** Serialise and save an OpenAPI fragment for the controller. Best-effort — errors are logged. */
    public void publish(ControllerEntry entry) {
        try {
            String json = objectMapper.writeValueAsString(buildSpec(entry));
            String location = locationOf(entry.project(), entry.fqn());
            OpenAPI existing = openAPIService.findByLocation(location)
                                             .stream()
                                             .findFirst()
                                             .orElse(null);
            OpenAPI artefact = existing != null ? existing : new OpenAPI(location, entry.fqn(), "Java controller " + entry.fqn());
            artefact.setLocation(location);
            artefact.setName(entry.fqn());
            artefact.setType(OpenAPI.ARTEFACT_TYPE);
            artefact.setDescription("Java controller " + entry.fqn());
            artefact.setContent(json);
            artefact.updateKey();
            openAPIService.save(artefact);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to build OpenAPI fragment for controller [{}]: {}", entry.fqn(), e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to persist OpenAPI fragment for controller [{}]: {}", entry.fqn(), e.getMessage(), e);
        }
    }

    /** Delete the previously-published fragment for a controller, if any. */
    public void retract(String project, String fqn) {
        try {
            String location = locationOf(project, fqn);
            for (OpenAPI a : openAPIService.findByLocation(location)) {
                openAPIService.delete(a);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to retract OpenAPI fragment for controller [{}::{}]: {}", project, fqn, e.getMessage(), e);
        }
    }

    private Map<String, Object> buildSpec(ControllerEntry entry) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.0");
        spec.put("info", buildInfo(entry));
        spec.put("paths", buildPaths(entry));
        return spec;
    }

    private Map<String, Object> buildInfo(ControllerEntry entry) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", simpleName(entry.fqn()));
        info.put("version", "1.0");
        Documentation doc = entry.instance()
                                 .getClass()
                                 .getAnnotation(Documentation.class);
        if (doc != null && !doc.value()
                               .isBlank()) {
            info.put("description", doc.value());
        }
        return info;
    }

    private Map<String, Object> buildPaths(ControllerEntry entry) {
        Map<String, Object> paths = new LinkedHashMap<>();
        String basePath = ENDPOINT_PREFIX + entry.project() + "/" + entry.basePath();
        for (Route route : entry.routes()) {
            String suffix = normaliseSuffix(route.pathTemplate());
            String fullPath = basePath + suffix;
            @SuppressWarnings("unchecked")
            Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(fullPath, k -> new LinkedHashMap<>());
            pathItem.put(route.httpMethod()
                              .name()
                              .toLowerCase(),
                    buildOperation(entry, route));
        }
        return paths;
    }

    private Map<String, Object> buildOperation(ControllerEntry entry, Route route) {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("operationId", entry.fqn() + "::" + route.method()
                                                        .getName()
                + "::" + route.httpMethod()
                              .name());
        Documentation doc = route.method()
                                 .getAnnotation(Documentation.class);
        if (doc != null && !doc.value()
                               .isBlank()) {
            op.put("summary", doc.value());
        }
        op.put("tags", List.of(simpleName(entry.fqn())));

        List<Map<String, Object>> parameters = new ArrayList<>();
        for (ParamBinding b : route.paramBindings()) {
            if (b.kind() == ParamBinding.Kind.PATH) {
                parameters.add(parameterSpec(b.name(), "path", true, b.targetType()));
            } else if (b.kind() == ParamBinding.Kind.QUERY) {
                parameters.add(parameterSpec(b.name(), "query", false, b.targetType()));
            }
        }
        if (!parameters.isEmpty()) {
            op.put("parameters", parameters);
        }

        for (ParamBinding b : route.paramBindings()) {
            if (b.kind() == ParamBinding.Kind.BODY) {
                op.put("requestBody", requestBodySpec(b.targetType()));
                break;
            }
        }

        op.put("responses", responsesSpec(route.method()
                                               .getReturnType(),
                route.method()
                     .getGenericReturnType()));

        if (route.roles() != null && !route.roles()
                                           .isEmpty()) {
            List<Map<String, Object>> security = new ArrayList<>();
            Map<String, Object> requirement = new LinkedHashMap<>();
            requirement.put("dirigibleRoles", List.copyOf(route.roles()));
            security.add(requirement);
            op.put("security", security);
        }

        return op;
    }

    private static Map<String, Object> parameterSpec(String name, String in, boolean required, Class<?> type) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", name);
        p.put("in", in);
        p.put("required", required);
        p.put("schema", schemaFor(type, null));
        return p;
    }

    private static Map<String, Object> requestBodySpec(Class<?> type) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("required", true);
        Map<String, Object> jsonMedia = new LinkedHashMap<>();
        jsonMedia.put("schema", schemaFor(type, null));
        body.put("content", Map.of("application/json", jsonMedia));
        return body;
    }

    private static Map<String, Object> responsesSpec(Class<?> returnType, java.lang.reflect.Type genericReturn) {
        Map<String, Object> responses = new LinkedHashMap<>();
        if (returnType == void.class) {
            responses.put("200", Map.of("description", "OK"));
            return responses;
        }
        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("description", "OK");
        Map<String, Object> jsonMedia = new LinkedHashMap<>();
        jsonMedia.put("schema", schemaFor(returnType, genericReturn));
        ok.put("content", Map.of("application/json", jsonMedia));
        responses.put("200", ok);
        return responses;
    }

    private static Map<String, Object> schemaFor(Class<?> type, java.lang.reflect.Type generic) {
        Map<String, Object> schema = new LinkedHashMap<>();
        if (type == String.class || type == CharSequence.class) {
            schema.put("type", "string");
            return schema;
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class || type == short.class
                || type == Short.class || type == byte.class || type == Byte.class) {
            schema.put("type", "integer");
            return schema;
        }
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            schema.put("type", "number");
            return schema;
        }
        if (type == boolean.class || type == Boolean.class) {
            schema.put("type", "boolean");
            return schema;
        }
        if (type == UUID.class) {
            schema.put("type", "string");
            schema.put("format", "uuid");
            return schema;
        }
        if (Collection.class.isAssignableFrom(type)) {
            schema.put("type", "array");
            schema.put("items", Map.of("type", "object"));
            return schema;
        }
        schema.put("type", "object");
        return schema;
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        String tail = dot >= 0 ? fqn.substring(dot + 1) : fqn;
        int dollar = tail.lastIndexOf('$');
        return dollar >= 0 ? tail.substring(dollar + 1) : tail;
    }

    private static String normaliseSuffix(String template) {
        if (template == null || template.isBlank()) {
            return "";
        }
        return template.startsWith("/") ? template : "/" + template;
    }

    private static String locationOf(String project, String fqn) {
        return LOCATION_PREFIX + project + "::" + fqn;
    }
}
