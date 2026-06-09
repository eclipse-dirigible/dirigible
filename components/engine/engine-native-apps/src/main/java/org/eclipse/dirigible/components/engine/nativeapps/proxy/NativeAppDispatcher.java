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
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;
import org.eclipse.dirigible.components.engine.nativeapps.process.RuntimeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.net.URI;
import java.util.function.Function;

/**
 * Sets the downstream scheme/host/port on the request via
 * {@link MvcUtils#setRequestUrl(ServerRequest, URI)}. The current request path (already rewritten
 * by {@link NativeAppLookupFilter} to the upstream-relative path) is preserved and reused by Spring
 * Cloud Gateway's {@code http()} handler when it builds the final outbound URI.
 */
@Component
class NativeAppDispatcher implements Function<ServerRequest, ServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppDispatcher.class);

    private final NativeAppProcessManager processManager;

    NativeAppDispatcher(NativeAppProcessManager processManager) {
        this.processManager = processManager;
    }

    @Override
    public ServerRequest apply(ServerRequest request) {
        NativeApp app = NativeAppProxyAttributes.requireNativeApp(request);
        URI downstream = downstreamUri(app);
        LOGGER.debug("Dispatching to native app [{}] via [{}]", app.getName(), downstream);
        MvcUtils.setRequestUrl(request, downstream);
        return request;
    }

    private URI downstreamUri(NativeApp app) {
        if (app.getKind() == NativeAppKind.REMOTE) {
            String url = app.getRemoteUrl();
            if (url == null) {
                throw new IllegalStateException("Remote native app [" + app.getName() + "] has no URL configured.");
            }
            return URI.create(stripTrailingSlash(url));
        }
        RuntimeState state = processManager.getState(app)
                                           .orElseThrow(() -> new IllegalStateException(
                                                   "Local native app [" + app.getName() + "] is not running."));
        return URI.create("http://127.0.0.1:" + state.getPort());
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
