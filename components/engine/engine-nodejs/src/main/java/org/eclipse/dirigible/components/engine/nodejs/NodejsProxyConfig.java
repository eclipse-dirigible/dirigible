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
package org.eclipse.dirigible.components.engine.nodejs;

import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class NodejsProxyConfig {

    static final String RELATIVE_BASE_PATH = "services/nodejs";
    static final String ABSOLUTE_BASE_PATH = "/" + RELATIVE_BASE_PATH;
    static final String BASE_PATH_PATTERN = ABSOLUTE_BASE_PATH + "/**";

    @Bean
    RouterFunction<ServerResponse> configureProxy(NodejsRequestDispatcher requestDispatcher) {
        return GatewayRouterFunctions.route("nodejs-apps-proxy-route")
                                     .before(BeforeFilterFunctions.rewritePath(ABSOLUTE_BASE_PATH + "(.*)", "$1"))
                                     .before(requestDispatcher)

                                     .route(path(BASE_PATH_PATTERN), http())

                                     //                                     .after(AfterFilterFunctions.rewriteLocationResponseHeader(
                                     //                                             cfg -> cfg.setProtocolsRegex("https?|ftps?|http?")))
                                     .build();
    }
}
