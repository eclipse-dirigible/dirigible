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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.sdk.http.Body;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.http.PathParam;
import org.eclipse.dirigible.sdk.http.Post;
import org.eclipse.dirigible.sdk.http.QueryParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class ControllerInvokerBindingTest {

    private ControllerRouter router;

    private ControllerClassConsumer consumer;

    private ControllerInvoker invoker;

    @BeforeEach
    void setUp() {
        router = new ControllerRouter();
        consumer = new ControllerClassConsumer(router, Optional.empty());
        invoker = new ControllerInvoker(new ObjectMapper());
    }

    @Test
    void path_param_long_is_coerced() {
        ControllerEntry entry = consumer.build(loaded(Demo.class));
        Route route = entry.routes()
                           .stream()
                           .filter(r -> r.method()
                                         .getName()
                                         .equals("byId"))
                           .findFirst()
                           .orElseThrow();

        FakeResponse resp = new FakeResponse();
        invoker.invoke(new RouteMatch(entry, route, Map.of("id", "42")), mockRequest(null), resp);

        assertEquals(200, resp.status());
        assertEquals("got-42", resp.body());
    }

    @Test
    void query_param_can_be_missing_for_boxed_type() {
        ControllerEntry entry = consumer.build(loaded(Demo.class));
        Route route = entry.routes()
                           .stream()
                           .filter(r -> r.method()
                                         .getName()
                                         .equals("list"))
                           .findFirst()
                           .orElseThrow();

        FakeResponse resp = new FakeResponse();
        invoker.invoke(new RouteMatch(entry, route, Map.of()), mockRequest(null), resp);
        // Demo.list returns "limit=null" when limit is null.
        assertEquals("limit=null", resp.body());
    }

    @Test
    void body_param_is_deserialized() throws Exception {
        ControllerEntry entry = consumer.build(loaded(Demo.class));
        Route route = entry.routes()
                           .stream()
                           .filter(r -> r.method()
                                         .getName()
                                         .equals("create"))
                           .findFirst()
                           .orElseThrow();

        FakeResponse resp = new FakeResponse();
        invoker.invoke(new RouteMatch(entry, route, Map.of()), mockRequest("{\"name\":\"X\"}"), resp);
        assertEquals("name=X", resp.body());
    }

    @Test
    void invalid_path_param_yields_400() {
        ControllerEntry entry = consumer.build(loaded(Demo.class));
        Route route = entry.routes()
                           .stream()
                           .filter(r -> r.method()
                                         .getName()
                                         .equals("byId"))
                           .findFirst()
                           .orElseThrow();

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> invoker.invoke(new RouteMatch(entry, route, Map.of("id", "abc")), mockRequest(null), new FakeResponse()));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void invalid_body_yields_400() {
        ControllerEntry entry = consumer.build(loaded(Demo.class));
        Route route = entry.routes()
                           .stream()
                           .filter(r -> r.method()
                                         .getName()
                                         .equals("create"))
                           .findFirst()
                           .orElseThrow();

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> invoker.invoke(new RouteMatch(entry, route, Map.of()), mockRequest("not json"), new FakeResponse()));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void method_throwing_yields_500() {
        ControllerEntry entry = consumer.build(loaded(Demo.class));
        Route route = entry.routes()
                           .stream()
                           .filter(r -> r.method()
                                         .getName()
                                         .equals("boom"))
                           .findFirst()
                           .orElseThrow();

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> invoker.invoke(new RouteMatch(entry, route, Map.of()), mockRequest(null), new FakeResponse()));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
    }

    // --- fixtures --------------------------------------------------------------------------------

    @Controller
    static class Demo {

        @Get("/items/{id}")
        public String byId(@PathParam("id") long id) {
            return "got-" + id;
        }

        @Get("/list")
        public String list(@QueryParam("limit") Integer limit) {
            return "limit=" + limit;
        }

        @Post
        public String create(@Body Payload payload) {
            return "name=" + payload.name;
        }

        @Get("/boom")
        public String boom() {
            throw new RuntimeException("kaboom");
        }
    }

    static class Payload {
        public String name;
    }

    // --- HTTP test plumbing ----------------------------------------------------------------------

    private static org.eclipse.dirigible.engine.java.spi.LoadedClass loaded(Class<?> type) {
        return new org.eclipse.dirigible.engine.java.spi.LoadedClass("p", type.getName(), type, type.getClassLoader());
    }

    // Mockito hands the in-memory stream to the controller under test; ByteArrayInputStream.close()
    // is a no-op so leaving it open is harmless, and try-with-resources here would close it before
    // the test consumes it. Suppressing CodeQL's java/input-resource-leak.
    @SuppressWarnings("resource")
    private static HttpServletRequest mockRequest(String body) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        if (body != null) {
            try {
                when(req.getInputStream()).thenReturn(new InMemoryServletInputStream(body));
            } catch (IOException ignored) {
                // mocks don't throw
            }
        }
        return req;
    }

    static class InMemoryServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream in;

        InMemoryServletInputStream(String content) {
            this.in = new ByteArrayInputStream(content.getBytes());
        }

        @Override
        public int read() {
            return in.read();
        }

        @Override
        public boolean isFinished() {
            return in.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {}
    }

    static class FakeResponse implements HttpServletResponse {

        private int status = 200;

        private String contentType;

        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        private final ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public void write(int b) {
                out.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {}
        };

        public int status() {
            return status;
        }

        public String body() {
            return out.toString();
        }

        // --- the rest is boilerplate; we don't exercise it in tests ---------------------------

        @Override
        public void setStatus(int sc) {
            this.status = sc;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public void setContentType(String type) {
            this.contentType = type;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return sos;
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(out);
        }

        @Override
        public boolean isCommitted() {
            return false;
        }

        // Stubs — unused.
        @Override
        public void addCookie(jakarta.servlet.http.Cookie cookie) {}

        @Override
        public boolean containsHeader(String name) {
            return false;
        }

        @Override
        public String encodeURL(String url) {
            return url;
        }

        @Override
        public String encodeRedirectURL(String url) {
            return url;
        }

        @Override
        public void sendError(int sc, String msg) {}

        @Override
        public void sendError(int sc) {}

        @Override
        public void sendRedirect(String location) {}

        // Added in Jakarta Servlet 6.1 (Spring Boot 4) — unused in tests.
        @Override
        public void sendRedirect(String location, int sc, boolean clearBuffer) {}

        @Override
        public void setDateHeader(String name, long date) {}

        @Override
        public void addDateHeader(String name, long date) {}

        @Override
        public void setHeader(String name, String value) {}

        @Override
        public void addHeader(String name, String value) {}

        @Override
        public void setIntHeader(String name, int value) {}

        @Override
        public void addIntHeader(String name, int value) {}

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public java.util.Collection<String> getHeaders(String name) {
            return List.of();
        }

        @Override
        public java.util.Collection<String> getHeaderNames() {
            return List.of();
        }

        @Override
        public String getCharacterEncoding() {
            return "UTF-8";
        }

        @Override
        public void setCharacterEncoding(String charset) {}

        @Override
        public void setContentLength(int len) {}

        @Override
        public void setContentLengthLong(long len) {}

        @Override
        public void setBufferSize(int size) {}

        @Override
        public int getBufferSize() {
            return 0;
        }

        @Override
        public void flushBuffer() {}

        @Override
        public void resetBuffer() {}

        @Override
        public void reset() {}

        @Override
        public void setLocale(java.util.Locale loc) {}

        @Override
        public java.util.Locale getLocale() {
            return java.util.Locale.US;
        }
    }
}
