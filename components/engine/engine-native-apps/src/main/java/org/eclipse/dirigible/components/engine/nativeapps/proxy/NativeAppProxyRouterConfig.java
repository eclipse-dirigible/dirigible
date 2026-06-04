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

import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.removeRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

/**
 * Spring Cloud Gateway WebMvc router that handles {@code /services/native-apps-proxy/v1/**}.
 *
 * <p>
 * Filter order is deliberate:
 * <ol>
 * <li>Strip the absolute base path so the rest of the chain sees {@code /<basePath>/<rest>}.</li>
 * <li>{@link NativeAppLookupFilter} resolves the app and rewrites the path to the upstream-relative
 * form (i.e. {@code /<rest>}).</li>
 * <li>{@link ExposedPathFilter} enforces whitelist + scope checks before doing anything expensive.
 * </li>
 * <li>{@link LazyStartFilter} starts the local process on demand if needed.</li>
 * <li>{@link NativeAppDispatcher} sets the downstream scheme/host/port.</li>
 * <li>{@link AuthInjectionFilter} attaches outbound auth.</li>
 * <li>Cookies are stripped, then {@code http()} forwards.</li>
 * </ol>
 */
@Configuration
class NativeAppProxyRouterConfig {

    private static final String RELATIVE_BASE_PATH = "services/native-apps-proxy/v1";
    private static final String ABSOLUTE_BASE_PATH = "/" + RELATIVE_BASE_PATH;
    private static final String BASE_PATH_PATTERN = ABSOLUTE_BASE_PATH + "/**";

    @Bean
    RouterFunction<ServerResponse> configureNativeAppProxy(NativeAppLookupFilter lookup, ExposedPathFilter exposedPaths,
            LazyStartFilter lazyStart, NativeAppDispatcher dispatcher, AuthInjectionFilter authInjection) {
        return GatewayRouterFunctions.route("native-app-proxy-route")
                                     .before(rewritePath(ABSOLUTE_BASE_PATH + "(.*)", "$1"))
                                     .filter(lookup)
                                     .filter(exposedPaths)
                                     .filter(lazyStart)
                                     .before(dispatcher)
                                     .before(authInjection)
                                     .before(removeRequestHeader(HttpHeaders.COOKIE))
                                     .route(path(BASE_PATH_PATTERN), http())
                                     .build();
    }
}
