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

import org.eclipse.dirigible.components.engine.nativeapps.auth.AuthenticationInjector;
import org.eclipse.dirigible.components.engine.nativeapps.auth.AuthenticationInjectorRegistry;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Authentication;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppConfig;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.Optional;
import java.util.function.Function;

/**
 * If the resolved native app declares an authentication scheme, looks up the matching
 * {@link AuthenticationInjector} and lets it attach credentials to the outbound request. No-op if
 * no authentication is configured.
 */
@Component
class AuthInjectionFilter implements Function<ServerRequest, ServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthInjectionFilter.class);

    private final AuthenticationInjectorRegistry injectors;

    AuthInjectionFilter(AuthenticationInjectorRegistry injectors) {
        this.injectors = injectors;
    }

    @Override
    public ServerRequest apply(ServerRequest request) {
        NativeApp app = NativeAppProxyAttributes.requireNativeApp(request);
        NativeAppConfig config = app.getConfig();
        Security security = config == null ? null : config.getSecurity();
        Authentication auth = security == null ? null : security.getAuthentication();
        if (auth == null || auth.getType() == null) {
            return request;
        }
        Optional<AuthenticationInjector> injector = injectors.findByType(auth.getType());
        if (injector.isEmpty()) {
            LOGGER.warn("No AuthenticationInjector registered for type [{}] (native app [{}]).", auth.getType(), app.getName());
            return request;
        }
        return injector.get()
                       .inject(request, auth);
    }
}
