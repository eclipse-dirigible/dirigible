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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.openapi.domain.OpenAPI;
import org.eclipse.dirigible.components.openapi.service.OpenAPIService;
import org.eclipse.dirigible.engine.java.annotations.http.Body;
import org.eclipse.dirigible.engine.java.annotations.http.Controller;
import org.eclipse.dirigible.engine.java.annotations.http.Get;
import org.eclipse.dirigible.engine.java.annotations.http.PathParam;
import org.eclipse.dirigible.engine.java.annotations.http.Post;
import org.eclipse.dirigible.engine.java.annotations.http.QueryParam;
import org.eclipse.dirigible.engine.java.annotations.http.Roles;
import org.eclipse.dirigible.engine.java.controller.ControllerClassConsumer;
import org.eclipse.dirigible.engine.java.controller.ControllerEntry;
import org.eclipse.dirigible.engine.java.controller.ControllerRouter;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JavaControllerOpenApiPublisherTest {

    private final ControllerRouter router = new ControllerRouter();

    private final ControllerClassConsumer consumer = new ControllerClassConsumer(router, Optional.empty());

    @Test
    void publish_persists_a_valid_openapi_fragment() throws Exception {
        OpenAPIService service = mock(OpenAPIService.class);
        when(service.findByLocation(any())).thenReturn(List.of());
        JavaControllerOpenApiPublisher publisher = new JavaControllerOpenApiPublisher(service, new ObjectMapper());

        ControllerEntry entry = consumer.build(loaded(Sample.class));
        publisher.publish(entry);

        ArgumentCaptor<OpenAPI> captor = ArgumentCaptor.forClass(OpenAPI.class);
        verify(service, atLeastOnce()).save(captor.capture());

        OpenAPI saved = captor.getValue();
        // Location anchored to the controller's .java source path so the framework's orphan
        // cleanup (which deletes OpenAPI artefacts whose location doesn't resolve to a registry
        // resource) doesn't wipe it between rebuilds.
        assertEquals("/p/" + Sample.class.getName()
                                         .replace('.', '/')
                + ".java", saved.getLocation());
        assertEquals(Sample.class.getName(), saved.getName());

        JsonNode root = new ObjectMapper().readTree(saved.getContent());
        assertEquals("3.0.0", root.get("openapi")
                                  .asText());
        assertEquals("Sample", root.get("info")
                                   .get("title")
                                   .asText());

        JsonNode paths = root.get("paths");
        String basePath = "/services/java/p/" + Sample.class.getName()
                                                            .replace('.', '/');

        assertTrue(paths.has(basePath + "/list"));
        assertTrue(paths.has(basePath + "/{id}"));
        assertTrue(paths.has(basePath));

        JsonNode createOp = paths.get(basePath)
                                 .get("post");
        assertTrue(createOp.has("requestBody"));
        // Roles annotation surfaced as a security requirement.
        assertTrue(createOp.has("security"));

        JsonNode byIdOp = paths.get(basePath + "/{id}")
                               .get("get");
        JsonNode parameters = byIdOp.get("parameters");
        assertEquals(1, parameters.size());
        assertEquals("id", parameters.get(0)
                                     .get("name")
                                     .asText());
        assertEquals("path", parameters.get(0)
                                       .get("in")
                                       .asText());
    }

    @Test
    void retract_deletes_existing_artefact() {
        OpenAPIService service = mock(OpenAPIService.class);
        String location = "/p/demo/Sample.java";
        OpenAPI existing = new OpenAPI(location, "demo.Sample", "x");
        when(service.findByLocation(location)).thenReturn(List.of(existing));
        JavaControllerOpenApiPublisher publisher = new JavaControllerOpenApiPublisher(service, new ObjectMapper());

        publisher.retract("p", "demo.Sample");

        verify(service).delete(existing);
    }

    @Controller
    @Roles({"DEVELOPER"})
    static class Sample {

        @Get("/list")
        public List<String> list(@QueryParam("limit") Integer limit) {
            return List.of();
        }

        @Get("/{id}")
        public String byId(@PathParam("id") Long id) {
            return id.toString();
        }

        @Post
        public String create(@Body Payload p) {
            return p.name;
        }

        static class Payload {
            public String name;
        }
    }

    private static LoadedClass loaded(Class<?> type) {
        return new LoadedClass("p", type.getName(), type, type.getClassLoader());
    }
}
