/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.proxy;

import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.registry.NativeAppRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the {@link NativeApp} for an incoming request from its base-path segment, attaches the
 * resolved app as a request attribute, and rewrites the request path to the upstream-relative path
 * (i.e. everything after the base path). Supports both a non-empty {@code basePath} (matched
 * against the first path segment) and an empty {@code basePath} (catch-all that forwards the entire
 * path).
 */
@Component
class NativeAppLookupFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppLookupFilter.class);

    /**
     * Matches the post-strip path {@code /<basePath>/<rest>}. group(1) = basePath, group(2) = /rest.
     */
    private static final Pattern PATH_PATTERN = Pattern.compile("^/([^/]+)(/.*)?$");

    private final NativeAppRegistry registry;

    NativeAppLookupFilter(NativeAppRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        String path = request.uri()
                             .getPath();
        Matcher m = PATH_PATTERN.matcher(path);
        if (!m.matches()) {
            return notFound("Invalid native-app proxy path: " + path);
        }
        String basePathSegment = m.group(1);
        String rest = m.group(2);

        Optional<NativeApp> bySegment = registry.findByBasePath(basePathSegment);
        NativeApp app;
        String upstreamPath;
        if (bySegment.isPresent()) {
            app = bySegment.get();
            upstreamPath = rest == null ? "/" : rest;
        } else {
            Optional<NativeApp> emptyBase = registry.findByBasePath("");
            if (emptyBase.isEmpty()) {
                return notFound("No native app registered for base path [" + basePathSegment + "].");
            }
            app = emptyBase.get();
            upstreamPath = path;
        }

        URI rewritten = UriComponentsBuilder.fromUri(request.uri())
                                            .replacePath(upstreamPath)
                                            .encode()
                                            .build()
                                            .toUri();
        ServerRequest forwarded = ServerRequest.from(request)
                                               .uri(rewritten)
                                               .attribute(NativeAppProxyAttributes.NATIVE_APP, app)
                                               .build();
        return next.handle(forwarded);
    }

    private static ServerResponse notFound(String message) {
        LOGGER.debug(message);
        return ServerResponse.status(HttpStatus.NOT_FOUND)
                             .body(message);
    }
}
