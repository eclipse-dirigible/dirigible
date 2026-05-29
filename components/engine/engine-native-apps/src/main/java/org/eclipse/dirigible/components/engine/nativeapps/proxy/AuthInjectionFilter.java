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

import java.util.function.Function;

/**
 * If the resolved native app declares an authentication scheme, looks up the matching
 * {@link AuthenticationInjector} and lets it attach credentials to the outbound request. No-op if
 * no authentication is configured; fails fast (with a descriptive message naming the unknown type
 * and the affected app) if the configured scheme has no registered injector — otherwise an upstream
 * 401/403 would be the only signal that the native app is misconfigured.
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
        if (auth == null) {
            LOGGER.debug("Native app [{}] declares no security.authentication; forwarding request unchanged.", app.getName());
            return request;
        }
        if (auth.getType() == null) {
            LOGGER.debug("Native app [{}] has security.authentication present but type is null; forwarding request unchanged.",
                    app.getName());
            return request;
        }
        AuthenticationInjector injector = injectors.findByType(auth.getType())
                                                   .orElseThrow(() -> new IllegalStateException("Native app [" + app.getName()
                                                           + "] declares authentication type [" + auth.getType()
                                                           + "] in security.authentication.type, but no AuthenticationInjector is registered for that type."
                                                           + " Register a Spring @Component implementing AuthenticationInjector whose type() returns ["
                                                           + auth.getType() + "], or fix the .native-app file to use a supported type."));
        return injector.inject(request, auth);
    }
}
