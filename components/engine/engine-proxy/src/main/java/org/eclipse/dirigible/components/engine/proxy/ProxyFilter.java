/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.proxy;

import org.eclipse.dirigible.components.engine.proxy.domain.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class ProxyFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    static final String PROXY_ATTRIBUTE_NAME = "proxy";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFilter.class);

    private static final Pattern PATH_PATTERN = Pattern.compile(ProxyRouterConfig.PATH_PATTERN_REGEX);

    private final ProxyRegistry proxyRegistry;

    ProxyFilter(ProxyRegistry proxyRegistry) {
        this.proxyRegistry = proxyRegistry;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> nextHandler) throws Exception {
        URI requestURI = request.uri();
        LOGGER.debug("Determining proxy for request with URI {}", requestURI);

        String path = requestURI.getPath();
        Matcher matcher = PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalStateException(
                    "The filter is mapped on an invalid path. Path [" + path + "] doesn't match [" + PATH_PATTERN + "]");
        }
        String proxyName = matcher.group(1);

        Optional<Proxy> proxy = proxyRegistry.findByName(proxyName);
        if (proxy.isEmpty()) {
            LOGGER.debug("There is no registered proxy with name [{}]. Request path [{}]", proxyName, path);
            String body = "Proxy with name [" + proxyName + "] is not registered.";
            return ServerResponse.status(HttpStatus.NOT_FOUND)
                                 .body(body);
        }

        ServerRequest modifiedRequest = ServerRequest.from(request)
                                                     .attribute(PROXY_ATTRIBUTE_NAME, proxy.get())
                                                     .build();
        return nextHandler.handle(modifiedRequest);
    }
}
