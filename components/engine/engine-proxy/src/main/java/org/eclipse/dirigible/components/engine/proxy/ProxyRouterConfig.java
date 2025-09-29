/*
 * Copyright (c) 2022 codbex or an codbex affiliate company and contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 codbex or an codbex affiliate company and contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.proxy;

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

@Configuration
class ProxyRouterConfig {

    static final String PATH_PATTERN_REGEX = "^/([^/]+)(/.*)?$";

    private static final String RELATIVE_BASE_PATH = "services/proxy";
    private static final String ABSOLUTE_BASE_PATH = "/" + RELATIVE_BASE_PATH;
    private static final String BASE_PATH_PATTERN = ABSOLUTE_BASE_PATH + "/**";

    @Bean
    RouterFunction<ServerResponse> configureProxy(ProxyFilter proxyFilter, ProxyDispatcher proxyDispatcher) {
        return GatewayRouterFunctions.route("proxy-route")
                                     // methods order matters
                                     .before(rewritePath(ABSOLUTE_BASE_PATH + "(.*)", "$1")) // remove mapping base path
                                     .filter(proxyFilter)
                                     .before(proxyDispatcher)
                                     .before(rewritePath(PATH_PATTERN_REGEX, "$2")) // remove proxy name part
                                     .before(removeRequestHeader(HttpHeaders.COOKIE))// remove client cookies so that they are not send

                                     .route(path(BASE_PATH_PATTERN), http())

                                     .build();
    }
}
